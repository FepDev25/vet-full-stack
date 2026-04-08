package com.veterinaria.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.veterinaria.domain.repository")
public class JpaConfig {

}
