package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Episode 12 demos. One Spring Boot application that runs every demo on
 * startup, makes real HTTP calls to itself, prints what happened and exits.
 *
 *   mvn -o spring-boot:run
 */
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(App.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }
}
