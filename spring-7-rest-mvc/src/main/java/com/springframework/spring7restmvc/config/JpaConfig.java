package com.springframework.spring7restmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for JPA Auditing.
 *
 * Enables automatic population of @CreatedDate and @LastModifiedDate fields.
 * This eliminates the need for manual timestamp management in service layer.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}