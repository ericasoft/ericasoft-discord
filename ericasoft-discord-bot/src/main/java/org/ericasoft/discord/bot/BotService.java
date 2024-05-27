package org.ericasoft.discord.bot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.GatewayBootstrap;
import discord4j.gateway.GatewayOptions;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(DiscordProperties.class)
public class BotService implements DisposableBean {
    private final DiscordProperties properties;
    @Getter
    private GatewayDiscordClient client;
    private IntentSet intents;

    @Autowired(required = false)
    public void setIntents(IntentSet intents) {
        this.intents = intents;
    }

    @PostConstruct
    public void init() {
        GatewayBootstrap<GatewayOptions> bootstrap = DiscordClientBuilder
            .create(properties.getToken())
            .build()
            .gateway();

        if (intents != null) {
            log.info(
                "Creating client with intents [{}]",
                intents.stream().map(Intent::name).collect(Collectors.joining(", ")));
            bootstrap.setEnabledIntents(intents);
        }

        client = bootstrap
            .login()
            .block();

        if (client == null) {
            throw new BeanCreationException("Discord client could not be created");
        }

        client.updatePresence(ClientPresence.online()).block();

        if (intents.contains(Intent.GUILD_MEMBERS)) {
            initializeGuildMemberCache();
        }

        if (intents.contains(Intent.GUILD_VOICE_STATES)) {
            initializeVoiceStateCache();
        }
    }

    private void initializeGuildMemberCache() {
        client
            .getGuilds()
            .flatMap(guild -> client.requestMembers(guild.getId()))
            .collectList()
            .block();

        client
            .on(GuildCreateEvent.class)
            .flatMap(event -> client.requestMembers(event.getGuild().getId()))
            .collectList()
            .subscribe();
    }

    private void initializeVoiceStateCache() {
        client
            .getGuilds()
            .flatMap(Guild::getVoiceStates)
            .collectList()
            .block();

        client
            .on(GuildCreateEvent.class)
            .flatMap(event -> event.getGuild().getVoiceStates())
            .collectList()
            .subscribe();
    }

    public <T extends Event> void registerEventListener(
            final Class<T> eventType,
            final Function<T, Mono<Void>> eventListener) {
        registerEventListener(eventType, eventListener, this::defaultErrorHandler);
    }

    public <T extends Event> void registerEventListener(
            final Class<T> eventType,
            final Function<T, Mono<Void>> eventListener,
            final Function<Throwable, Mono<Void>> errorHandler) {
        client
            .on(eventType)
            .flatMap(eventListener)
            .onErrorResume(errorHandler)
            .subscribe();
    }

    public <T extends ChatInputInteractionEvent> void registerChatInputInteractionEventListener(
        final Class<T> eventType,
        final String commandName,
        final Function<T, Mono<Void>> eventListener) {
        registerChatInputInteractionEventListener(eventType, commandName, eventListener, this::defaultErrorHandler);
    }

    public <T extends ChatInputInteractionEvent> void registerChatInputInteractionEventListener(
        final Class<T> eventType,
        final String commandName,
        final Function<T, Mono<Void>> eventListener,
        final Function<Throwable, Mono<Void>> errorHandler) {
        client
            .on(eventType)
            .flatMap(event -> {
                Mono<Void> result;
                if (event.getCommandName().equalsIgnoreCase(commandName)) {
                    log.info("Command [{}] received", event.getCommandName());
                    try {
                        result = eventListener.apply(event);
                    } catch (RuntimeException e) {
                        log.warn("Error occurred while processing command [{}]", event.getCommandName(), e);
                        event.reply("Error occurred while processing command").block();
                        return Mono.empty();
                    }
                    log.info("Finished processed command [{}]", event.getCommandName());
                } else {
                    result = Mono.empty();
                }

                return result;
            })
            .onErrorResume(errorHandler)
            .subscribe();
    }

    private Mono<Void> defaultErrorHandler(Throwable throwable) {
        log.warn("Error occured while processing Discord event", throwable);
        return Mono.empty();
    }

    @Override
    public void destroy() {
        log.info("Shutting down - going invisible");
        client.updatePresence(ClientPresence.invisible()).doOnError(log::error).block();
        log.info("Shutting down - logging out");
        client.logout().doOnError(log::error).block();
    }
}
