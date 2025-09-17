package com.example.drools_poc;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collection;

@SpringBootApplication
public class TaxRuleRunner {
    public static void main(String[] args) {
        try {
            // Load Drools services
            KieServices kieServices = KieServices.Factory.get();
            KieContainer kieContainer = kieServices.getKieClasspathContainer();
            KieSession kieSession = kieContainer.newKieSession("ksession-rules");

            // Example: Reservation for PortA inbound
            Reservation reservation = new Reservation("PortB", "INBOUND");
            kieSession.insert(reservation);

            // Fire rules
            int fired = kieSession.fireAllRules();
            System.out.println("Number of rules fired: " + fired);

            // Retrieve results (all facts of type TaxResult)
            Collection<?> results = kieSession.getObjects(obj -> obj instanceof TaxResult);
            for (Object obj : results) {
                TaxResult tax = (TaxResult) obj;
                System.out.println("Final tax result: " + tax);
            }

            kieSession.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
