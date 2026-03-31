package com.zabora.subscription.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Scheduled para los jobs de expiracion y cancelacion diferida.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
