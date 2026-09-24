package com.lexhive.libraryapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.lexhive.libraryapp.config.LibraryConfig;
import com.lexhive.libraryapp.config.StaticUsers;

@SpringBootApplication
@EnableConfigurationProperties({LibraryConfig.class, StaticUsers.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
