package net.tomasbot.matchday.plugin.datasource.forum;

import lombok.Getter;
import lombok.Setter;
import net.tomasbot.matchday.plugin.PluginProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:plugins/forum/forum.properties")
@ConfigurationProperties(prefix = "plugin.forum")
@Getter
@Setter
public class ForumPluginProperties extends PluginProperties {

  private int scrapeSteps;
}
