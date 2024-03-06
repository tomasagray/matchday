package self.me.matchday.plugin.datasource.forum;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@Configuration
@PropertySource("classpath:plugins/forum/forum.properties")
@ConfigurationProperties(prefix = "plugin.forum")
@Getter
@Setter
public class ForumPluginProperties extends PluginProperties {

    private String linkSelector;
    private String matchPattern;
    private String dateFormatter;
    private int scrapeSteps;
}
