package org.angbyte.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportEvent;
import org.springframework.boot.autoconfigure.AutoConfigurationImportListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class AutoConfigurationDiagnostic implements AutoConfigurationImportListener {

    @Override
    public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
        List<String> candidates = event.getCandidateConfigurations();

        Set<String> exclusions = event.getExclusions();

        System.out.println("\n=== EVENTO DE AUTOCONFIGURACIÓN ===");
        System.out.println("Candidatas: " + candidates);
        System.out.println("Exclusiones: " + exclusions);
        System.out.println("==================================\n");
    }
}
