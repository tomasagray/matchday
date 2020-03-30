package self.me.matchday._DEVFIXTURES;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

// TODO: DELETE THIS!! ONLY FOR DEVELOPMENT!
@Configuration
@EnableWebMvc
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