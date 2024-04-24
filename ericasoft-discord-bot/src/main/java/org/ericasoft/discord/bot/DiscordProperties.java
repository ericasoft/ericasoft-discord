package org.ericasoft.discord.bot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.discord")
@Data
public class DiscordProperties {

    private String token;
}
