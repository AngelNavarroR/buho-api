package org.angbyte.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "buho")
public class BuhoProperties {

    private static final Logger log = LoggerFactory.getLogger(BuhoProperties.class);
    private boolean debug;
    @Pattern(regexp = "^/.*", message = "El path debe comenzar con '/'")
    private String path;

    @PostConstruct
    public void init() {
        log.info("Configuración de BuhoProperties cargada");
        log.debug("Debug: {}", debug);
        log.debug("Path base: {}", path);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getPath() {
        if (path == null || path.trim().isEmpty()) {
            this.path = "/filters";
        } else if (!path.startsWith("/")) {
            this.path = "/" + path;
        }
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "BuhoProperties{" + "debug=" + debug + ", path='" + path + '\'' + '}';
    }
}