package com.kalsym.flowcore;

import com.kalsym.flowcore.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class FlowcoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowcoreApplication.class, args);

    }

    @Value("${build.version:not-known}")
    String version;

    @Bean
    CommandLineRunner lookup(ApplicationContext context) {
        return args -> {
            VersionHolder.VERSION = version;
            Logger.info("", "", "\n"
                    + "  ______ _                  _____               \n"
                    + " |  ____| |                / ____|              \n"
                    + " | |__  | | _____      __ | |     ___  _ __ ___ \n"
                    + " |  __| | |/ _ \\ \\ /\\ / / | |    / _ \\| '__/ _ \\\n"
                    + " | |    | | (_) \\ V  V /  | |___| (_) | | |  __/\n"
                    + " |_|    |_|\\___/ \\_/\\_/    \\_____\\___/|_|  \\___|\n"
                    + " :: com.kalsym ::              (v" + VersionHolder.VERSION + ")", "");

        };
    }

}
