package com.rebataur.restifyml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

 @SpringBootApplication 
 @EnableAutoConfiguration
 @ComponentScan(basePackages={"com.rebataur.restifyml"})
 @EnableJpaRepositories(basePackages="com.rebataur.restifyml.repositories")
 @EnableTransactionManagement
 @EntityScan(basePackages="com.rebataur.restifyml.entities")
public class RestifyMLApplication {
	public static void main(String[] args) {	
		SpringApplication.run(RestifyMLApplication.class, args);
	}

}
