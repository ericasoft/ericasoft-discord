package org.ericasoft.discord.vchelper.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "voice_config")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VoiceChannelConfiguration {

    @Id
    @NonNull
    private Long serverId;
    @NonNull
    @Column(nullable = false)
    private Long lobbyChannelId;
    @NonNull
    @Column(nullable = false)
    private Long broadcastChannelId;


}
