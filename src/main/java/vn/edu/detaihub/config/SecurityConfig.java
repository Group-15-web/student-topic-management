package vn.edu.detaihub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.detaihub.repository.UserAccountRepository;
import vn.edu.detaihub.security.AppAuthenticationEntryPoint;
import vn.edu.detaihub.security.LoginSuccessHandler;
import vn.edu.detaihub.security.SessionVersionFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final UserAccountRepository users;
  private final UserDetailsService userDetailsService;
  private final LoginSuccessHandler loginSuccessHandler;
  private final AppAuthenticationEntryPoint authenticationEntryPoint;

  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error", "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/login")
                    .successHandler(loginSuccessHandler)
                    .permitAll())
        .logout(
            l ->
                l.logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
        // Keep this filter inside Spring Security only, not as a servlet Filter bean.
        .addFilterAfter(
            new SessionVersionFilter(users, userDetailsService),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
