package com.yugabyte.app.messenger.data.generator;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.service.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringComponent
public class DataGenerator {

    @Bean
    public CommandLineRunner loadData(PasswordEncoder passwordEncoder, ProfileRepository userRepository) {
        return args -> {
            Logger logger = LoggerFactory.getLogger(getClass());
            if (userRepository.count() != 0L) {
                logger.info("Using existing database");
                logger.info("Number of entities " + userRepository.count());
                return;
            }
            int seed = 123;

            logger.info("Generating demo data");

            logger.info("... generating 2 User entities...");
            Profile firstUser = new Profile();
            firstUser.setCountryCode("US");
            firstUser.setFullName("John Normal");
            firstUser.setEmail("john@gmail.com");
            firstUser.setHashedPassword(passwordEncoder.encode("user"));
            firstUser.setUserPictureUrl(
                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=128&h=128&q=80");
            firstUser.setPhone("65023403435");
            userRepository.save(firstUser);

            Profile secondUser = new Profile();
            secondUser.setCountryCode("UK");
            secondUser.setFullName("Emma Powerful");
            secondUser.setEmail("emma@gmail.com");
            secondUser.setHashedPassword(passwordEncoder.encode("admin"));
            secondUser.setUserPictureUrl(
                    "https://images.unsplash.com/photo-1607746882042-944635dfe10e?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=128&h=128&q=80");
            secondUser.setPhone("65034534543");
            userRepository.save(secondUser);

            logger.info("Generated demo data");
        };
    }

}