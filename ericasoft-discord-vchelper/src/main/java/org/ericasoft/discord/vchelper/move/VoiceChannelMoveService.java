package org.ericasoft.discord.vchelper.move;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Entity;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.possible.Possible;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.ericasoft.discord.bot.BotService;
import org.ericasoft.discord.vchelper.config.VoiceChannelConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@AutoConfigureAfter({BotService.class, VoiceChannelConfigurationService.class})
@Getter
public class VoiceChannelMoveService extends AbstractVoiceChannelMoveService {

    @Autowired
    public VoiceChannelMoveService(BotService botService, VoiceChannelConfigurationService configService) {
        super(botService, configService);
    }

    @PostConstruct
    public void init() {
        getBotService().registerChatInputInteractionEventListener(
            ChatInputInteractionEvent.class,
            "move",
            this::handlePullCommand);
    }

    private Mono<Void> handlePullCommand(ChatInputInteractionEvent event) {
        Snowflake serverId = event.getInteraction().getGuildId().orElseThrow();

        log.debug("Looking for role");

        Snowflake roleId = extractRole(event);

        log.debug("Found role [{}]", roleId.asLong());

        Channel toChannel = event
            .getOption("to")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asChannel)
            .flatMap(Mono::blockOptional)
            .orElseThrow();

        Snowflake toChannelId = toChannel.getId();

        Optional<Long> fromChannelId = event
            .getOption("from")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asChannel)
            .flatMap(Mono::blockOptional)
            .map(Entity::getId)
            .map(Snowflake::asLong);

        List<Member> guildMembers;

        if (fromChannelId.isPresent()) {
            if (fromChannelId.get() == toChannelId.asLong()) {
                event.reply("From channel cannot be the same as the to channel!").block();
                return Mono.empty();
            }
            VoiceChannel fromChannel = findVoiceChannel(serverId, Snowflake.of(fromChannelId.get()));
            if (fromChannel == null) {
                event.reply("Could not find from channel").block();
                return Mono.empty();
            }
            log.info("Found from channel [{}] with id [{}]", fromChannel.getName(), fromChannel.getId().asLong());
            log.debug("Looking for guild members");
            guildMembers = findVoiceUsers(fromChannel, findGuildUsers(fromChannel.getGuildId()));
        } else {
            guildMembers = botService
                .getClient()
                .getGuildById(serverId)
                .flatMap(guild -> guild
                    .getVoiceStates()
                    // Only move users that are not already in the destination channel
                    .filter(voiceState -> voiceState
                        .getChannelId()
                        .map(channelId -> !Objects.equals(channelId, toChannelId))
                        .orElse(false))
                    .flatMap(VoiceState::getMember)
                    .collectList())
                .block();
        }

        if (guildMembers == null || guildMembers.isEmpty()) {
            event.reply("Could not find any voice users").block();
            return Mono.empty();
        }

        log.debug(
            "Found guild members: {}",
            guildMembers.stream().map(Member::getDisplayName).collect(Collectors.joining(", ")));
        log.debug("Filtering guild members");
        guildMembers = filterGuildMembers(guildMembers, roleId);

        if (guildMembers.isEmpty()) {
            event.reply("All voice users in lobby were filtered out").block();
            return Mono.empty();
        }

        String filteredNames = guildMembers.stream().map(Member::getDisplayName).collect(Collectors.joining(", "));
        log.debug(
            "The following guild members were not filtered out: {}",
            filteredNames);

        log.debug("Attempting to move guild members to the selected channel");
        Possible<Optional<Snowflake>> possibleBroadcastChannel = Possible.of(Optional.of(toChannelId));
        moveGuildMembersToVoiceChannel(guildMembers, possibleBroadcastChannel);

        event.reply(String.format("Moved users [%s] to <#%s>!", filteredNames, toChannelId.asLong())).block();
        return Mono.empty();
    }
}
