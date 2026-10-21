package com.example.demo;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUname("admin").isPresent()) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUname("admin");
        admin.setUpass(passwordEncoder.encode("1234"));
        userRepository.save(admin);
    }
}
