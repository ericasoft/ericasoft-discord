package org.ericasoft.discord.vchelper.config;


import jakarta.validation.constraints.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface VoiceChannelRepository extends CrudRepository<VoiceChannelConfiguration, Long> {

    <S extends VoiceChannelConfiguration> S save(@NotNull S vcConfig);

    @Override
    Optional<VoiceChannelConfiguration> findById(Long aLong);
}
