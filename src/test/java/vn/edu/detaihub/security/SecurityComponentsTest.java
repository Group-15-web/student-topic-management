package vn.edu.detaihub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import vn.edu.detaihub.model.UserAccount;
import vn.edu.detaihub.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class SecurityComponentsTest {
  private static final String EMAIL = "student@example.edu";
  private static final String PASSWORD_HASH = "$2a$10$stored-password-hash";
  private static final int SECURITY_VERSION = 7;

  @Mock private UserAccountRepository users;
  @Mock private ActorService actors;
  @Mock private UserDetailsService userDetailsService;

  @BeforeEach
  void clearContextBeforeTest() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void clearContextAfterTest() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void userDetailsMapStoredEmailPasswordHashAndRoles() {
    var account = account(true);
    when(users.findByEmailIgnoreCase("STUDENT@example.edu")).thenReturn(Optional.of(account));
    when(actors.roles(account.getId())).thenReturn(Set.of("STUDENT", "LECTURER"));

    var details = new DatabaseUserDetailsService(users, actors)
        .loadUserByUsername("STUDENT@example.edu");

    assertThat(details.getUsername()).isEqualTo(EMAIL);
    assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
    assertThat(details.isEnabled()).isTrue();
    assertThat(details.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_LECTURER");
    verify(users).findByEmailIgnoreCase("STUDENT@example.edu");
    verify(actors).roles(account.getId());
  }

  @Test
  void disabledAccountProducesDisabledUserDetails() {
    var account = account(false);
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));
    when(actors.roles(account.getId())).thenReturn(Set.of("STUDENT"));

    var details = new DatabaseUserDetailsService(users, actors).loadUserByUsername(EMAIL);

    assertThat(details.isEnabled()).isFalse();
    assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
  }

  @Test
  void missingAccountThrowsUsernameNotFoundWithoutLoadingRoles() {
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> new DatabaseUserDetailsService(users, actors).loadUserByUsername(EMAIL))
        .isInstanceOf(UsernameNotFoundException.class);

    verifyNoInteractions(actors);
  }

  @Test
  void successfulLoginCreatesSessionWithCurrentSecurityVersionAndRedirectsHome() throws Exception {
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    assertThat(request.getSession(false)).isNull();

    new LoginSuccessHandler(users).onAuthenticationSuccess(request, response, authenticate());

    assertThat(request.getSession(false)).isNotNull();
    assertThat(request.getSession(false).getAttribute("securityVersion")).isEqualTo(SECURITY_VERSION);
    assertThat(response.getRedirectedUrl()).isEqualTo("/");
    verify(users).findByEmailIgnoreCase(EMAIL);
  }

  @Test
  void successfulLoginUpdatesTheExistingSessionSecurityVersion() throws Exception {
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION - 1, null);
    request.setSession(session);

    new LoginSuccessHandler(users)
        .onAuthenticationSuccess(request, new MockHttpServletResponse(), authenticate());

    assertThat(request.getSession(false)).isSameAs(session);
    assertThat(session.getAttribute("securityVersion")).isEqualTo(SECURITY_VERSION);
  }

  @Test
  void apiAuthenticationFailureReturnsTheExistingJsonContract() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/topics");
    var response = new MockHttpServletResponse();

    new AppAuthenticationEntryPoint()
        .commence(request, response, new InsufficientAuthenticationException("Login required"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"UNAUTHENTICATED\"}");
    assertThat(response.getRedirectedUrl()).isNull();
  }

  @Test
  void browserAuthenticationFailureRedirectsToLogin() throws Exception {
    var request = new MockHttpServletRequest("GET", "/topics");
    var response = new MockHttpServletResponse();

    new AppAuthenticationEntryPoint()
        .commence(request, response, new InsufficientAuthenticationException("Login required"));

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("/login");
  }

  @Test
  void bareApiPathStillUsesTheBrowserRedirect() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api");
    var response = new MockHttpServletResponse();

    new AppAuthenticationEntryPoint()
        .commence(request, response, new InsufficientAuthenticationException("Login required"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/login");
  }

  @Test
  void filterWithoutAuthenticationDelegatesWithoutLookingUpAnAccount() throws Exception {
    var request = new MockHttpServletRequest();

    runFilter(request);

    assertThat(request.getSession(false)).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(users, userDetailsService);
  }

  @Test
  void filterWithNonUserDetailsPrincipalLeavesAuthenticationAndSessionAlone() throws Exception {
    var authentication = new UsernamePasswordAuthenticationToken("anonymousUser", null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION, null);
    request.setSession(session);

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    assertThat(session.isInvalid()).isFalse();
    verifyNoInteractions(users, userDetailsService);
  }

  @Test
  void deletedAccountClearsAuthenticationAndInvalidatesSession() throws Exception {
    authenticate();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION, SECURITY_VERSION);
    request.setSession(session);

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(session.isInvalid()).isTrue();
    verifyNoInteractions(userDetailsService);
  }

  @Test
  void disabledAccountClearsAuthenticationAndInvalidatesSession() throws Exception {
    authenticate();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(false)));
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION, SECURITY_VERSION);
    request.setSession(session);

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(session.isInvalid()).isTrue();
    verifyNoInteractions(userDetailsService);
  }

  @Test
  void mismatchedSecurityVersionClearsAuthenticationAndInvalidatesSession() throws Exception {
    authenticate();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION - 1, SECURITY_VERSION);
    request.setSession(session);

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(session.isInvalid()).isTrue();
    verifyNoInteractions(userDetailsService);
  }

  @Test
  void invalidAccountWithoutSessionClearsAuthenticationWithoutCreatingSession() throws Exception {
    authenticate();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    var request = new MockHttpServletRequest();

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(request.getSession(false)).isNull();
    verifyNoInteractions(userDetailsService);
  }

  @Test
  void missingLoadedVersionRefreshesAuthoritiesAndCachesTheVersion() throws Exception {
    var previous = authenticate();
    var fresh = freshUserDetails();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(fresh);
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION, null);
    request.setSession(session);

    runFilter(request);

    var refreshed = SecurityContextHolder.getContext().getAuthentication();
    assertThat(refreshed).isNotSameAs(previous);
    assertThat(refreshed.getPrincipal()).isSameAs(fresh);
    assertThat(refreshed.getCredentials()).isEqualTo(previous.getCredentials());
    assertThat(refreshed.isAuthenticated()).isTrue();
    assertThat(refreshed.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_LECTURER");
    assertThat(session.getAttribute("loadedSecurityVersion")).isEqualTo(SECURITY_VERSION);
    assertThat(session.getAttribute("securityVersion")).isEqualTo(SECURITY_VERSION);
    assertThat(session.isInvalid()).isFalse();
    verify(userDetailsService).loadUserByUsername(EMAIL);
  }

  @Test
  void staleLoadedVersionRefreshesAuthorities() throws Exception {
    authenticate();
    var fresh = freshUserDetails();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(fresh);
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION, SECURITY_VERSION - 1);
    request.setSession(session);

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(fresh);
    assertThat(session.getAttribute("loadedSecurityVersion")).isEqualTo(SECURITY_VERSION);
    assertThat(session.isInvalid()).isFalse();
    verify(userDetailsService).loadUserByUsername(EMAIL);
  }

  @Test
  void matchingLoadedVersionKeepsAuthenticationButChecksTheAccountOnEveryRequest() throws Exception {
    var previous = authenticate();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    var request = new MockHttpServletRequest();
    var session = session(SECURITY_VERSION, SECURITY_VERSION);
    request.setSession(session);
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);
    var filter = new SessionVersionFilter(users, userDetailsService);

    filter.doFilter(request, response, chain);
    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(previous);
    assertThat(session.isInvalid()).isFalse();
    verify(users, times(2)).findByEmailIgnoreCase(EMAIL);
    verifyNoInteractions(userDetailsService);
    verify(chain, times(2)).doFilter(request, response);
  }

  @Test
  void absentSecurityVersionDoesNotInvalidateAnOtherwiseValidSession() throws Exception {
    authenticate();
    var fresh = freshUserDetails();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(fresh);
    var request = new MockHttpServletRequest();
    var session = new MockHttpSession();
    request.setSession(session);

    runFilter(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(fresh);
    assertThat(session.isInvalid()).isFalse();
    assertThat(session.getAttribute("securityVersion")).isNull();
    assertThat(session.getAttribute("loadedSecurityVersion")).isEqualTo(SECURITY_VERSION);
  }

  @Test
  void requestsWithoutSessionReloadAuthoritiesEachTimeWithoutCreatingSession() throws Exception {
    authenticate();
    var fresh = freshUserDetails();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account(true)));
    when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(fresh);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);
    var filter = new SessionVersionFilter(users, userDetailsService);

    filter.doFilter(request, response, chain);
    filter.doFilter(request, response, chain);

    assertThat(request.getSession(false)).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(fresh);
    verify(users, times(2)).findByEmailIgnoreCase(EMAIL);
    verify(userDetailsService, times(2)).loadUserByUsername(EMAIL);
    verify(chain, times(2)).doFilter(request, response);
  }

  private void runFilter(MockHttpServletRequest request) throws Exception {
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);

    new SessionVersionFilter(users, userDetailsService).doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  private static Authentication authenticate() {
    var principal = User.withUsername(EMAIL).password("old-password-hash").roles("STUDENT").build();
    var authentication = new UsernamePasswordAuthenticationToken(
        principal, "session-credentials", principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    return authentication;
  }

  private static UserDetails freshUserDetails() {
    return User.withUsername(EMAIL).password(PASSWORD_HASH).roles("LECTURER").build();
  }

  private static MockHttpSession session(int securityVersion, Integer loadedSecurityVersion) {
    var session = new MockHttpSession();
    session.setAttribute("securityVersion", securityVersion);
    if (loadedSecurityVersion != null) {
      session.setAttribute("loadedSecurityVersion", loadedSecurityVersion);
    }
    return session;
  }

  private static UserAccount account(boolean enabled) {
    var account = new UserAccount();
    account.setId(42L);
    account.setEmail(EMAIL);
    account.setPasswordHash(PASSWORD_HASH);
    account.setEnabled(enabled);
    account.setSecurityVersion(SECURITY_VERSION);
    return account;
  }
}
