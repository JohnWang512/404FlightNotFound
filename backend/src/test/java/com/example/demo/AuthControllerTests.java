package com.example.demo;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-slice tests for {@link AuthController}.
 *
 * Why a separate file:
 * - AuthController depends on UserRepository directly (no DbService proxy), so it needs its
 *   own @WebMvcTest slice with UserRepository mocked — not the DbService mock used by the
 *   rest of ManualBackendControllerTests.
 * - Isolating auth tests here makes it easy to extend auth (e.g., JWT, roles) later without
 *   touching the large flight/db test suite.
 *
 * Coverage:
 *   register — success, blank username, blank password, duplicate username
 *   login    — success (sign-in), wrong password, user not found, blank username, blank password
 */
@Tag("manual_backend")
@WebMvcTest(AuthController.class)
@Import(PasswordConfig.class)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    // ------------------------------------------------------------------ register

    @Test
    void registerSuccessReturnsOkAndSanitizedUser() throws Exception {
        AppUser saved = new AppUser();
        saved.setUId(1L);
        saved.setUname("alice");
        saved.setUpass("secret");

        when(userRepository.findByUname("alice")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.user.uId").value(1))
                .andExpect(jsonPath("$.user.uname").value("alice"))
                // password must NOT be returned
                .andExpect(jsonPath("$.user.upass").doesNotExist());
    }

    @Test
    void registerStoresProvidedUsernameAndPassword() throws Exception {
        AppUser saved = new AppUser();
        saved.setUId(2L);
        saved.setUname("bob");

        when(userRepository.findByUname("bob")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"bob\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk());

        verify(userRepository).save(argThat(u ->
                "bob".equals(u.getUname()) && passwordEncoder.matches("pass123", u.getUpass())));
    }

    @Test
    void registerBlankUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").isString());

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerBlankPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").isString());

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerNullPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerDuplicateUsernameReturnsConflict() throws Exception {
        AppUser existing = new AppUser();
        existing.setUId(99L);
        existing.setUname("alice");

        when(userRepository.findByUname("alice")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"newpass\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").isString());

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerTrimsLeadingAndTrailingWhitespaceFromUsername() throws Exception {
        AppUser saved = new AppUser();
        saved.setUId(3L);
        saved.setUname("trimmed");

        when(userRepository.findByUname("trimmed")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"  trimmed  \",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.uname").value("trimmed"));
    }

    // ------------------------------------------------------------------ login (sign-in)

    @Test
    void loginWithValidCredentialsReturnsOkAndSanitizedUser() throws Exception {
        AppUser user = new AppUser();
        user.setUId(5L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("secret"));

        when(userRepository.findByUname("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.user.uId").value(5))
                .andExpect(jsonPath("$.user.uname").value("alice"))
                // password must NOT appear in the login response
                .andExpect(jsonPath("$.user.upass").doesNotExist());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        AppUser user = new AppUser();
        user.setUId(5L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("correct"));

        when(userRepository.findByUname("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void loginWithUnknownUsernameReturnsUnauthorized() throws Exception {
        when(userRepository.findByUname("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"ghost\",\"password\":\"anypass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void loginBlankUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).findByUname(any());
    }

    @Test
    void loginBlankPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).findByUname(any());
    }

    @Test
    void loginNullPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void loginTrimsUsernameBeforeLookup() throws Exception {
        AppUser user = new AppUser();
        user.setUId(7L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("secret"));

        when(userRepository.findByUname("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"  alice  \",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(userRepository).findByUname("alice");
    }

    // ------------------------------------------------------------------ normalize (via endpoints)

    @Test
    void registerNullUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":null,\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginNullUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":null,\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).findByUname(any());
    }

    // ------------------------------------------------------------------ verifyAndUpgradePassword

    @Test
    void loginWithNullStoredPasswordReturnsUnauthorized() throws Exception {
        AppUser user = new AppUser();
        user.setUId(8L);
        user.setUname("alice");
        user.setUpass(null); // null stored password → verifyAndUpgradePassword returns false

        when(userRepository.findByUname("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"anything\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void loginWithBlankStoredPasswordReturnsUnauthorized() throws Exception {
        AppUser user = new AppUser();
        user.setUId(9L);
        user.setUname("alice");
        user.setUpass("   "); // blank stored password → verifyAndUpgradePassword returns false

        when(userRepository.findByUname("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"anything\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void loginWithPlaintextPasswordUpgradesToBcryptAndReturnsOk() throws Exception {
        // Legacy seed row: password stored as plaintext, not bcrypt
        AppUser user = new AppUser();
        user.setUId(10L);
        user.setUname("legacy");
        user.setUpass("plaintextpass"); // stored as plaintext

        AppUser saved = new AppUser();
        saved.setUId(10L);
        saved.setUname("legacy");
        saved.setUpass(passwordEncoder.encode("plaintextpass"));

        when(userRepository.findByUname("legacy")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"legacy\",\"password\":\"plaintextpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        // Password must have been re-encoded and persisted
        verify(userRepository).save(argThat(u ->
                passwordEncoder.matches("plaintextpass", u.getUpass())));
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateNullUserIdReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":null,\"currentPassword\":\"pw\",\"newPassword\":\"newpw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNullCurrentPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":null,\"newPassword\":\"newpw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateBlankCurrentPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"\",\"newPassword\":\"newpw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNoNewUsernameOrPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"pw\",\"newUsername\":null,\"newPassword\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserNotFoundReturns404() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":99,\"currentPassword\":\"pw\",\"newPassword\":\"newpw\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void updateWrongCurrentPasswordReturnsUnauthorized() throws Exception {
        AppUser user = new AppUser();
        user.setUId(1L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("correct"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"wrong\",\"newPassword\":\"newpw\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNewUsernameTakenByOtherUserReturnsConflict() throws Exception {
        AppUser user = new AppUser();
        user.setUId(1L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("pw"));

        AppUser other = new AppUser();
        other.setUId(2L);
        other.setUname("bob");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUname("bob")).thenReturn(Optional.of(other));

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"pw\",\"newUsername\":\"bob\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNewUsernameSameAsOwnUsernameSucceeds() throws Exception {
        AppUser user = new AppUser();
        user.setUId(1L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("pw"));

        AppUser saved = new AppUser();
        saved.setUId(1L);
        saved.setUname("alice");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUname("alice")).thenReturn(Optional.of(user)); // same user
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"pw\",\"newUsername\":\"alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void updatePasswordSuccessReturnsOkAndSanitizedUser() throws Exception {
        AppUser user = new AppUser();
        user.setUId(1L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("oldpw"));

        AppUser saved = new AppUser();
        saved.setUId(1L);
        saved.setUname("alice");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"oldpw\",\"newPassword\":\"newpw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.user.uId").value(1))
                .andExpect(jsonPath("$.user.uname").value("alice"))
                .andExpect(jsonPath("$.user.upass").doesNotExist());

        verify(userRepository).save(argThat(u -> passwordEncoder.matches("newpw", u.getUpass())));
    }

    @Test
    void updateUsernameSuccessReturnsOkAndSanitizedUser() throws Exception {
        AppUser user = new AppUser();
        user.setUId(1L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("pw"));

        AppUser saved = new AppUser();
        saved.setUId(1L);
        saved.setUname("alicia");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUname("alicia")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"pw\",\"newUsername\":\"alicia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.user.uname").value("alicia"));
    }

    @Test
    void updateBothUsernameAndPasswordSucceeds() throws Exception {
        AppUser user = new AppUser();
        user.setUId(1L);
        user.setUname("alice");
        user.setUpass(passwordEncoder.encode("oldpw"));

        AppUser saved = new AppUser();
        saved.setUId(1L);
        saved.setUname("alicia");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUname("alicia")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        mockMvc.perform(put("/api/auth/update")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"currentPassword\":\"oldpw\",\"newUsername\":\"alicia\",\"newPassword\":\"newpw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(userRepository).save(argThat(u ->
                "alicia".equals(u.getUname()) && passwordEncoder.matches("newpw", u.getUpass())));
    }
}
