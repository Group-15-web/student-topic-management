package vn.edu.detaihub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.detaihub.repository.UserAccountRepository;

/** Adapts database accounts and roles to Spring Security's UserDetails contract. */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
  private final UserAccountRepository users;
  private final ActorService actors;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var account =
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));
    return User.withUsername(account.getEmail())
        .password(account.getPasswordHash())
        .disabled(!account.getEnabled())
        .roles(actors.roles(account.getId()).toArray(String[]::new))
        .build();
  }
}
