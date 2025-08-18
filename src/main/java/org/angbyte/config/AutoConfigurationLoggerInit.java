package org.angbyte.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class AutoConfigurationLoggerInit implements
        ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(AutoConfigurationLoggerInit.class);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        log.info("Iniciando contexto de aplicación para la buho-api");

        context.addBeanFactoryPostProcessor(beanFactory -> {
            log.debug("Post-procesamiento de BeanFactory iniciado");
        });
    }
}
