package org.angbyte.config;

import jakarta.servlet.http.HttpServletRequest;
import org.angbyte.repositories.BuhoPersistable;
import org.angbyte.repositories.BuhoPersistableImpl;
import org.angbyte.resources.BuhoApi;
import org.angbyte.service.BuhoService;
import org.angbyte.utils.BuhoCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.RestController;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ RestController.class, HttpServletRequest.class })
@EnableConfigurationProperties(BuhoProperties.class)
@EnableJpaRepositories(basePackages = "org.angbyte.repositories")

public class BuhoAutoConfiguration {

    private static final Logger log = LogManager.getLogger(BuhoAutoConfiguration.class);

    public BuhoAutoConfiguration(BuhoProperties properties) {
        System.out.println("Properties: " + properties);
    }


    @Bean
    @ConditionalOnMissingBean
    public BuhoApi busquedaController(BuhoService busquedaService) {
        log.info("Iniciando controlador de busquedas");
        return new BuhoApi(busquedaService);
    }

    @Bean
    @ConditionalOnMissingBean
    public BuhoService busquedaService(BuhoProperties properties, BuhoPersistable repository) {
        log.info("Iniciando servicio de busquedas");
        return new BuhoService(properties, repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public BuhoPersistable persistableRepository(BuhoProperties properties, BuhoCache buhoCache) {
        log.info("Iniciando repositorio de persistencia de busquedas");
        return new BuhoPersistableImpl<>(buhoCache, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BuhoCache buhoCache() {
        return new BuhoCache();
    }


}
