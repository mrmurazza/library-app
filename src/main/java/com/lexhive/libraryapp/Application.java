package com.lexhive.libraryapp;

import com.lexhive.libraryapp.config.LibraryConfig;
import com.lexhive.libraryapp.config.SecurityUsersProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LibraryConfig.class, SecurityUsersProperties.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
