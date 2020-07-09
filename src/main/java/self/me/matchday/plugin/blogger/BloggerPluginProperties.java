package self.me.matchday.plugin.blogger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@Configuration
@PropertySource("classpath:plugins/blogger/blogger.properties")
@ConfigurationProperties(prefix = "plugin.blogger")
public class BloggerPluginProperties extends PluginProperties {

}
