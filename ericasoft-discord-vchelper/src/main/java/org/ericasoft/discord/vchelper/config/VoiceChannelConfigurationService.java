package org.ericasoft.discord.vchelper.config;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Entity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ericasoft.discord.bot.BotService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
@AutoConfigureAfter({BotService.class, VoiceChannelRepository.class})
@EnableJpaRepositories
@EntityScan
@Getter
public class VoiceChannelConfigurationService {

    private final BotService botService;
    private final VoiceChannelRepository voiceChannelRepository;

    @PostConstruct
    public void init() {
        botService.registerChatInputInteractionEventListener(
            ChatInputInteractionEvent.class,
            "config",
            this::handleConfigCommand);
    }

    public Optional<VoiceChannelConfiguration> getConfiguration(long serverId) {
        return voiceChannelRepository.findById(serverId);
    }

    private Mono<Void> handleConfigCommand(ChatInputInteractionEvent event) {
        VoiceChannelConfiguration.VoiceChannelConfigurationBuilder configBuilder = VoiceChannelConfiguration
            .builder();
        event.getOptions().forEach(option ->
        {
            if (option.getName().equalsIgnoreCase("lobby")) {
                configBuilder.lobbyChannelId(option
                    .getValue()
                    .map(ApplicationCommandInteractionOptionValue::asChannel)
                    .flatMap(Mono::blockOptional)
                    .map(Entity::getId)
                    .map(Snowflake::asLong)
                    .orElseThrow());
            }

            if (option.getName().equalsIgnoreCase("broadcast")) {
                configBuilder.broadcastChannelId(option
                    .getValue()
                    .map(ApplicationCommandInteractionOptionValue::asChannel)
                    .flatMap(Mono::blockOptional)
                    .map(Entity::getId)
                    .map(Snowflake::asLong)
                    .orElseThrow());
            }
        });

        configBuilder.serverId(event.getInteraction().getGuildId().orElseThrow().asLong());
        voiceChannelRepository.save(configBuilder.build());
        event.reply("Configuration has been saved!").block();

        return Mono.empty();
    }

}
