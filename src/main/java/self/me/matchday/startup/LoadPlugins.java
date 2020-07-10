package self.me.matchday.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadPlugins {

  @Bean
  CommandLineRunner initPlugins() {

    return args -> {

      /*
      - Get data source plugins
      - submit SnapshotRequests
      - Save & print responses
       */
    };
  }
}
