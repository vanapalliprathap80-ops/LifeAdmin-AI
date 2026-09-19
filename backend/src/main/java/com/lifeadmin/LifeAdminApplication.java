package com.lifeadmin;

import com.lifeadmin.ai.GeminiProperties;
import com.lifeadmin.storage.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, GeminiProperties.class})
@EnableScheduling
public class LifeAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeAdminApplication.class, args);
    }
}
