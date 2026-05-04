package com.dway.dwaybackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {
            ClassPathResource serviceAccountResource =
                    new ClassPathResource("firebase-service-account.json");

            try (InputStream serviceAccount = serviceAccountResource.getInputStream()) {
                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(serviceAccount)
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);

                log.info("Firebase initialized successfully");
            }
        } else {
            log.info("Firebase already initialized — skipping");
        }

        return FirebaseMessaging.getInstance();
    }
}