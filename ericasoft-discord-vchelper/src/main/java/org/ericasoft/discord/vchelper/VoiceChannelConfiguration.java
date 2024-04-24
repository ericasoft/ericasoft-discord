package org.ericasoft.discord.vchelper;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "voice_config")
@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@EqualsAndHashCode
public class VoiceChannelConfiguration {

    @Id
    private final long serverId;
    private final long lobbyId;
    private final long broadcastId;


}
