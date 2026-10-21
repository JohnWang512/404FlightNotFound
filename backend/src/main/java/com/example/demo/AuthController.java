package com.example.demo;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        String username = normalize(request.username());
        String password = request.password();

        if (username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(error("Username and password are required."));
        }

        if (userRepository.findByUname(username).isPresent()) {
            return ResponseEntity.status(409).body(error("Username already exists."));
        }

        AppUser user = new AppUser();
        user.setUname(username);
        user.setUpass(passwordEncoder.encode(password));
        AppUser saved = userRepository.save(user);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("user", sanitizeUser(saved));
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
        String username = normalize(request.username());
        String password = request.password();

        if (username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(error("Username and password are required."));
        }

        Optional<AppUser> existing = userRepository.findByUname(username);
        if (existing.isEmpty() || !verifyAndUpgradePassword(existing.get(), password)) {
            return ResponseEntity.status(401).body(error("Invalid username or password."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("user", sanitizeUser(existing.get()));
        return ResponseEntity.ok(payload);
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }

    private boolean verifyAndUpgradePassword(AppUser user, String rawPassword) {
        String stored = user.getUpass();
        if (stored == null || stored.isBlank()) {
            return false;
        }

        // Preferred path: BCrypt-hashed passwords.
        if (passwordEncoder.matches(rawPassword, stored)) {
            return true;
        }

        // Backward compatibility for legacy/plaintext demo seed rows.
        if (rawPassword.equals(stored)) {
            user.setUpass(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    private Map<String, Object> sanitizeUser(AppUser user) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("uId", user.getUId());
        safe.put("uname", user.getUname());
        return safe;
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody UpdateRequest request) {
        if (request.userId() == null || request.currentPassword() == null || request.currentPassword().isBlank()) {
            return ResponseEntity.badRequest().body(error("User ID and current password are required."));
        }
        boolean hasNewUsername = request.newUsername() != null && !request.newUsername().isBlank();
        boolean hasNewPassword = request.newPassword() != null && !request.newPassword().isBlank();
        if (!hasNewUsername && !hasNewPassword) {
            return ResponseEntity.badRequest().body(error("Provide a new username or new password."));
        }

        Optional<AppUser> found = userRepository.findById(request.userId());
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(error("User not found."));
        }
        AppUser user = found.get();
        if (!passwordEncoder.matches(request.currentPassword(), user.getUpass())) {
            return ResponseEntity.status(401).body(error("Current password is incorrect."));
        }

        if (hasNewUsername) {
            String newUname = normalize(request.newUsername());
            Optional<AppUser> taken = userRepository.findByUname(newUname);
            if (taken.isPresent() && !taken.get().getUId().equals(user.getUId())) {
                return ResponseEntity.status(409).body(error("Username already taken."));
            }
            user.setUname(newUname);
        }
        if (hasNewPassword) {
            user.setUpass(passwordEncoder.encode(request.newPassword()));
        }

        AppUser saved = userRepository.save(user);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("user", sanitizeUser(saved));
        return ResponseEntity.ok(payload);
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", false);
        payload.put("error", message);
        return payload;
    }

    public record AuthRequest(String username, String password) {}
    public record UpdateRequest(Long userId, String currentPassword, String newUsername, String newPassword) {}
}
