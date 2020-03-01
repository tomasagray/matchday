/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main attachment point for the SpringBoot application.
 */
@SpringBootApplication
@EnableJpaRepositories
public class MatchdayApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchdayApplication.class, args);
	}

}
