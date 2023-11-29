/*
 * Copyright (c) 2022.
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

import static self.me.matchday.config.Properties.API_PREFIX;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StatusWebSocketConfigurer implements WebSocketMessageBrokerConfigurer {

  private static final String WEBSOCKET_ROOT = API_PREFIX + "/ws";
  public static final String STOMP_ENDPOINT = WEBSOCKET_ROOT + "/stomp";
  public static final String BROKER_ROOT = WEBSOCKET_ROOT + "/subscription";
  public static final String SUBSCRIPTION_PREFIX = WEBSOCKET_ROOT + "/subscribe";

  @Override
  public void registerStompEndpoints(@NotNull StompEndpointRegistry registry) {
    registry.addEndpoint(STOMP_ENDPOINT).setAllowedOriginPatterns("*").withSockJS();
  }

  @Override
  public void configureMessageBroker(@NotNull MessageBrokerRegistry registry) {
    registry.enableSimpleBroker(BROKER_ROOT);
    registry.setApplicationDestinationPrefixes(SUBSCRIPTION_PREFIX);
  }
}
