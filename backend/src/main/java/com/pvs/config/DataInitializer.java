package com.pvs.config;

import com.pvs.entity.Role;
import com.pvs.entity.User;
import com.pvs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.existsByUsername("admin")) {
            log.info("Admin user already exists.");
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);

        log.info("======================================");
        log.info("Default admin user created");
        log.info("Username : admin");
        log.info("Password : admin123");
        log.info("======================================");
    }
}