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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ericasoft.discord.bot.BotService;
import org.ericasoft.discord.vchelper.config.VoiceChannelConfigurationService;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
public abstract class VoiceChannelMoveService {

    private final BotService botService;
    private final VoiceChannelConfigurationService configService;

    protected static Snowflake extractRole(ChatInputInteractionEvent event) {
        return event
            .getOption("role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asRole)
            .map(role -> role.map(Role::getId).block())
            .orElseThrow();
    }

    protected static List<Member> findVoiceUsers(ChatInputInteractionEvent event, VoiceChannel voiceChannel) {
        return voiceChannel.getVoiceStates()
            .flatMap(VoiceState::getUser)
            .flatMap(user -> event.getInteraction().getGuild().flatMap(guild -> guild.getMemberById(user.getId())))
            .collectList()
            .block();
    }

    protected static List<Member> filterGuildMembers(List<Member> guildMembers, Snowflake roleId) {
        return guildMembers
            .stream()
            .filter(member -> !CollectionUtils.isEmpty(member.getRoles().filter(role -> roleId.equals(role.getId())).collectList().block()))
            .toList();
    }

    protected static void moveGuildMembersToVoiceChannel(List<Member> guildMembers, Possible<Optional<Snowflake>> possibleBroadcastChannel) {
        Flux
            .fromIterable(guildMembers)
            .map(Member::edit)
            .flatMap(edit -> edit.withNewVoiceChannel(possibleBroadcastChannel))
            .delayElements(Duration.ofMillis(250))
            .collectList()
            .block();
    }

    protected VoiceChannel findVoiceChannel(Snowflake serverId, Snowflake id) {
        return botService
            .getClient()
            .getGuildById(serverId)
            .flatMap(guild -> guild.getChannelById(id))
            .filter(channel -> Channel.Type.GUILD_VOICE.equals(channel.getType()))
            .ofType(VoiceChannel.class)
            .block();
    }
}
