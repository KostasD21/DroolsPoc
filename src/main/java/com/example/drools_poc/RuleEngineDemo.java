package com.example.drools_poc;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.time.LocalDate;
import java.util.Collection;

public class RuleEngineDemo {
    public static void main(String[] args) throws Exception {
//        CsvRuleEngine engine = new CsvRuleEngine();
//        engine.loadRules("src/main/resources/tax-rules.csv");
//
//        Reservation summerTrip = new Reservation("PortA", "INBOUND", LocalDate.of(2025, 7, 15));
//        Reservation winterTrip = new Reservation("PortA", "INBOUND", LocalDate.of(2025, 12, 20));
//        Reservation marchTrip = new Reservation("PortA", "INBOUND", LocalDate.of(2025, 3, 20));
//        Reservation outboundTrip = new Reservation("PortA", "OUTBOUND", LocalDate.of(2025, 3, 10));
//        Reservation noDirection = new Reservation("PortA", LocalDate.of(2025, 3, 10));
//
//        Reservation portBInbounda = new Reservation("PortB", "INBOUND", LocalDate.of(2025, 3, 20));
//        Reservation portBNoDirection = new Reservation("PortB", LocalDate.of(2025, 6, 20));
//
//        System.out.println("--------------PortA----------------------------");
//        System.out.println("Summer trip tax: " + engine.evaluate(summerTrip));
//        System.out.println("Winter trip tax: " + engine.evaluate(winterTrip));
//        System.out.println("March trip tax: " + engine.evaluate(marchTrip));
//        System.out.println("Outbound trip tax: " + engine.evaluate(outboundTrip));
//        System.out.println("No direction trip tax: " + engine.evaluate(noDirection));
//
//        System.out.println("--------------PortB----------------------------");
//        System.out.println("Inbound port B tax: " + engine.evaluate(portBInbounda));
//        System.out.println("No direction port B tax: " + engine.evaluate(portBNoDirection));

        KieServices kieServices = KieServices.Factory.get();
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        KieSession kieSession = kieContainer.newKieSession("ksession-rules");

// Example reservation
        Reservation r = new Reservation("PortA", "INBOUND", LocalDate.of(2025, 7, 15));
        kieSession.insert(r);

        kieSession.fireAllRules();

        Collection<?> results = kieSession.getObjects(obj -> obj instanceof TaxResult);
        for (Object obj : results) {
            System.out.println(obj);
        }
    }
}

