package self.me.matchday.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import self.me.matchday.api.service.VideoResourceInterceptor;

@Configuration
@EnableWebMvc
@PropertySource("classpath:video-resources.properties")
@ConfigurationProperties(prefix = "video-resources")
@Data
public class VideoResourcesConfig implements WebMvcConfigurer {

  private String fileStorageLocation;
  private String videoStorageDirname;

  @Override
  public void addInterceptors(@NotNull final InterceptorRegistry registry) {

    registry
        .addInterceptor(createVideoResourceInterceptor())
        .addPathPatterns(String.format("/%s/**/playlist.m3u8", videoStorageDirname));
  }

  /**
   * Ensure instance of interceptor is managed by Spring
   *
   * @return A VideoResourceInterceptor instance
   */
  @Bean
  public VideoResourceInterceptor createVideoResourceInterceptor() {
    return new VideoResourceInterceptor(fileStorageLocation);
  }
}
