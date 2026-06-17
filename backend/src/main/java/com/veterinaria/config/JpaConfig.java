package com.veterinaria.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.veterinaria.domain.repository",
        "com.veterinaria.ai.audit"
})
public class JpaConfig {

}
