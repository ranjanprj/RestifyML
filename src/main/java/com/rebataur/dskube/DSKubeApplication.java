package com.rebataur.dskube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

 @SpringBootApplication 
 @EnableAutoConfiguration
 @ComponentScan(basePackages={"com.rebataur.dskube"})
 @EnableJpaRepositories(basePackages="com.rebataur.dskube.repositories")
 @EnableTransactionManagement
 @EntityScan(basePackages="com.rebataur.dskube.entities")
public class DSKubeApplication {
	public static void main(String[] args) {	
		SpringApplication.run(DSKubeApplication.class, args);
	}

}
