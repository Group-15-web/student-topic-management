package vn.edu.detaihub.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.detaihub.repository.UserAccountRepository;
import vn.edu.detaihub.security.ActorService;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final UserAccountRepository users;
  private final ActorService actors;

  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService details() {
    return email -> {
      var u =
          users
              .findByEmailIgnoreCase(email)
              .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));
      return User.withUsername(u.getEmail())
          .password(u.getPasswordHash())
          .disabled(!u.getEnabled())
          .roles(actors.roles(u.getId()).toArray(String[]::new))
          .build();
    };
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/css/**", "/js/**", "/error", "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/login")
                    .successHandler(
                        (req, res, auth) -> {
                          var u = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
                          req.getSession().setAttribute("securityVersion", u.getSecurityVersion());
                          res.sendRedirect("/");
                        })
                    .permitAll())
        .logout(
            l ->
                l.logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    (req, res, ex) -> {
                      if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(401);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\":\"UNAUTHENTICATED\"}");
                      } else res.sendRedirect("/login");
                    }))
        .addFilterAfter(
            new OncePerRequestFilter() {
              @Override
              protected void doFilterInternal(
                  HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                  throws ServletException, IOException {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserDetails old) {
                  var u = users.findByEmailIgnoreCase(auth.getName()).orElse(null);
                  var session = req.getSession(false);
                  var version = session == null ? null : session.getAttribute("securityVersion");
                  if (u == null
                      || !u.getEnabled()
                      || (version != null && !version.equals(u.getSecurityVersion()))) {
                    SecurityContextHolder.clearContext();
                    if (req.getSession(false) != null) req.getSession(false).invalidate();
                  } else {
                    var fresh = details().loadUserByUsername(auth.getName());
                    SecurityContextHolder.getContext()
                        .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                fresh, auth.getCredentials(), fresh.getAuthorities()));
                  }
                }
                chain.doFilter(req, res);
              }
            },
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
