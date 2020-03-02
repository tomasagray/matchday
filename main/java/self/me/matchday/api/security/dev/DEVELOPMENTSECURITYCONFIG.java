package self.me.matchday.api.security.dev;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.firewall.StrictHttpFirewall;

// TODO: DELETE THIS!! ONLY FOR DEVELOPMENT!
@Configuration
public class DEVELOPMENTSECURITYCONFIG extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(@NotNull HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers("/").permitAll()
        .antMatchers("/h2-console/**").permitAll();

    http.csrf().disable();
    http.headers().frameOptions().disable();
  }

  @Bean
  public StrictHttpFirewall httpFirewall() {

    // set allowed HTTP verbs
    final StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall();
    strictHttpFirewall.setAllowedHttpMethods(
        Arrays.asList("GET", "POST", "HEAD", "OPTIONS", "PATCH", "PUT", "PROPFIND"));
    return strictHttpFirewall;
  }
}