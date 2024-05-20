package org.ericasoft.discord.bot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Log4j2
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(DiscordProperties.class)
public class BotService {
    private final DiscordProperties properties;
    @Getter
    private GatewayDiscordClient client;

    @PostConstruct
    public void init() {
        client = DiscordClientBuilder
            .create(properties.getToken())
            .build()
            .gateway()
            .login()
            .block();
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

    private Mono<Void> defaultErrorHandler(Throwable throwable) {
        log.warn("Error occured while processing Discord event", throwable);
        return Mono.empty();
    }

}
