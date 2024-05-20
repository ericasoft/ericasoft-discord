package org.ericasoft.discord.vchelper.move;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.possible.Possible;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.ericasoft.discord.bot.BotService;
import org.ericasoft.discord.vchelper.config.VoiceChannelConfiguration;
import org.ericasoft.discord.vchelper.config.VoiceChannelConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@AutoConfigureAfter({BotService.class, VoiceChannelConfigurationService.class})
@Getter
public class VoiceChannelReturnService extends VoiceChannelMoveService {

    @Autowired
    public VoiceChannelReturnService(BotService botService, VoiceChannelConfigurationService configService) {
        super(botService, configService);
    }

    @PostConstruct
    public void init() {
        getBotService().registerChatInputInteractionEventListener(
            ChatInputInteractionEvent.class,
            "return",
            this::handlePullCommand);
    }

    private Mono<Void> handlePullCommand(ChatInputInteractionEvent event) {
        Snowflake serverId = event.getInteraction().getGuildId().orElseThrow();
        Optional<VoiceChannelConfiguration> config = getConfigService().getConfiguration(serverId.asLong());

        if (config.isEmpty()) {
            event.reply("This command is not configured for this server.").block();
            return Mono.empty();
        }

        log.debug("Looking for role");

        Snowflake roleId = extractRole(event);

        log.debug("Found role [{}]", roleId.asLong());
        log.debug("Looking for broadcast");

        VoiceChannel broadcast = findVoiceChannel(serverId, Snowflake.of(config.get().getBroadcastChannelId()));

        if (broadcast == null) {
            event.reply("Could not find broadcast").block();
            return Mono.empty();
        }

        log.info("Found broadcast [{}] with id [{}]", broadcast.getName(), broadcast.getId().asLong());
        log.debug("Looking for guild members");
        List<Member> guildMembers = findVoiceUsers(event, broadcast);

        if (guildMembers == null) {
            event.reply("Could not find voice users in broadcast").block();
            return Mono.empty();
        }

        log.debug(
            "Found guild members: {}",
            guildMembers.stream().map(Member::getDisplayName).collect(Collectors.joining(", ")));
        log.debug("Filtering guild members");
        guildMembers = filterGuildMembers(guildMembers, roleId);

        if (guildMembers.isEmpty()) {
            event.reply("All voice users in broadcast were filtered out").block();
            return Mono.empty();
        }

        String filteredNames = guildMembers.stream().map(Member::getDisplayName).collect(Collectors.joining(", "));
        log.debug(
            "The following guild members were not filtered out: {}",
            filteredNames);

        log.debug("Attempting to move guild members to broadcast channel");
        Possible<Optional<Snowflake>> possibleLobbyChannel = Possible.of(
            Optional.of(
                Snowflake.of(
                    config.get().getLobbyChannelId())));
        moveGuildMembersToVoiceChannel(guildMembers, possibleLobbyChannel);

        event.reply(String.format("Return users [%s] to lobby!", filteredNames)).block();
        return Mono.empty();
    }
}
