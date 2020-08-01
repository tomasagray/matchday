package self.me.matchday.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminDashboardConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {

    // Add admin dashboard
    registry
        .addResourceHandler("/admin/**")
        .addResourceLocations("classpath:/public/admin/");
  }
}
