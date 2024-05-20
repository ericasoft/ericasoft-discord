package org.ericasoft.discord.vchelper.move;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.possible.Possible;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ericasoft.discord.bot.BotService;
import org.ericasoft.discord.vchelper.config.VoiceChannelConfiguration;
import org.ericasoft.discord.vchelper.config.VoiceChannelConfigurationService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
@AutoConfigureAfter({BotService.class, VoiceChannelConfigurationService.class})
@Getter
public class VoiceChannelPullService {

    private final BotService botService;
    private final VoiceChannelConfigurationService configService;

    @PostConstruct
    public void init() {
        botService.registerChatInputInteractionEventListener(
            ChatInputInteractionEvent.class,
            "pull",
            this::handlePullCommand);
    }

    private Mono<Void> handlePullCommand(ChatInputInteractionEvent event) {
        Snowflake serverId = event.getInteraction().getGuildId().orElseThrow();
        Optional<VoiceChannelConfiguration> config = configService.getConfiguration(serverId.asLong());

        if (config.isEmpty()) {
            event.reply("This command is not configured for this server.").block();
            return Mono.empty();
        }

        log.debug("Looking for role");

        Snowflake roleId = event
            .getOption("role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asRole)
            .map(role -> role.map(Role::getId).block())
            .orElseThrow();

        log.debug("Found role [{}]", roleId.asLong());
        log.debug("Looking for lobby");

        VoiceChannel lobby = botService
            .getClient()
            .getGuildById(serverId)
            .flatMap(guild -> guild.getChannelById(Snowflake.of(config.get().getLobbyChannelId())))
            .filter(channel -> Channel.Type.GUILD_VOICE.equals(channel.getType()))
            .ofType(VoiceChannel.class)
            .block();

        if (lobby == null) {
            event.reply("Could not find lobby").block();
            return Mono.empty();
        }

        log.info("Found lobby [{}] with id [{}]", lobby.getName(), lobby.getId().asLong());
        log.debug("Looking for guild members");
        List<Member> guildMembers = lobby.getVoiceStates()
            .flatMap(VoiceState::getUser)
            .flatMap(user -> event.getInteraction().getGuild().flatMap(guild -> guild.getMemberById(user.getId())))
            .collectList()
            .block();

        if (guildMembers == null) {
            event.reply("Could not find guild members").block();
            return Mono.empty();
        }

        log.debug(
            "Found guild members: {}",
            guildMembers.stream().map(Member::getDisplayName).collect(Collectors.joining(", ")));
        log.debug("Filtering guild members");
        guildMembers = guildMembers
            .stream()
            .filter(member -> !CollectionUtils.isEmpty(member.getRoles().filter(role -> roleId.equals(role.getId())).collectList().block()))
            .toList();

        if (guildMembers.isEmpty()) {
            event.reply("All guild members were filtered out").block();
            return Mono.empty();
        }

        String filteredNames = guildMembers.stream().map(Member::getDisplayName).collect(Collectors.joining(", "));
        log.debug(
            "The following guild members were not filtered out: {}",
            filteredNames);

        log.debug("Attempting to move guild members to broadcast channel");
        Possible<Optional<Snowflake>> possibleBroadcastChannel = Possible.of(
            Optional.of(
                Snowflake.of(
                    config.get().getBroadcastChannelId())));
        Flux
            .fromIterable(guildMembers)
            .map(Member::edit)
            .flatMap(edit -> edit.withNewVoiceChannel(possibleBroadcastChannel))
            .delayElements(Duration.ofMillis(250))
            .collectList()
            .block();

        event.reply(String.format("Moved users [%s] to broadcast!", filteredNames)).block();
        return Mono.empty();
    }
}
