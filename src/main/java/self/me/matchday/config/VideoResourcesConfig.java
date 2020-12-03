/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import self.me.matchday.api.service.VideoResourceInterceptor;
import self.me.matchday.util.Log;

@Configuration
@EnableWebMvc
@PropertySource("classpath:video-resources.properties")
@ConfigurationProperties(prefix = "video-resources")
@Data
public class VideoResourcesConfig implements WebMvcConfigurer {

  private VideoResourceInterceptor videoResourceInterceptor;
  private String videoStreamInterceptPattern;
  private String fileStorageLocation;

  public VideoResourcesConfig(@Autowired final VideoResourceInterceptor videoResourceInterceptor) {
    this.videoResourceInterceptor = videoResourceInterceptor;
  }

  @Override
  public void addInterceptors(@NotNull final InterceptorRegistry registry) {

    Log.i(
        "VideoResourcesConfig",
        "Adding Interceptor for path matching pattern:" + videoStreamInterceptPattern);

    registry.addInterceptor(videoResourceInterceptor).addPathPatterns(videoStreamInterceptPattern);
  }
}
