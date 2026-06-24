package com.ecoomerce.sportscenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SportscenterApplication — Spring Boot entry point for the SportsCenter e-commerce API.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration      : marks this class as a source of Spring bean definitions
 *   - @EnableAutoConfiguration : tells Spring Boot to auto-configure beans based on classpath dependencies
 *   - @ComponentScan      : scans this package and sub-packages for @Component, @Service, @Repository, @Controller beans
 *
 * When the application starts it bootstraps the embedded Tomcat server on port 8080
 * and registers all REST controllers, Spring Security filters, and JPA repositories.
 */
@SpringBootApplication
public class SportscenterApplication {

	/**
	 * Main method — JVM entry point.
	 * SpringApplication.run() bootstraps the entire Spring context,
	 * starts the embedded web server, and begins listening for HTTP requests.
	 *
	 * @param args command-line arguments (not used here but forwarded to Spring for profile activation etc.)
	 */
	public static void main(String[] args) {
		SpringApplication.run(SportscenterApplication.class, args);
	}

}
