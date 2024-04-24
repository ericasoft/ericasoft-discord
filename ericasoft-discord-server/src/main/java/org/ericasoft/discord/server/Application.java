package org.ericasoft.discord.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.ericasoft.discord")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
