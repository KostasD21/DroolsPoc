package com.example.drools_poc;
import com.example.drools_poc.entity.TaxRule;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.example.drools_poc.DMNXmlConverter.constructXMLAndStoreInDb;

@Service
public class DMNRuleEngine implements RuleEngine {

    private final TaxRuleRepository taxRuleRepository;

    public DMNRuleEngine(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    @Override
    public void process(RulesMapping rulesMapping) {
//        Double tax;
//        for (Rule rule : rulesMapping.getRules()) {
//            System.out.println("Rule ID: " + rule.getId());
//
//            List<Map<String, String>> rows = RuleFlattener.flattenConditions(rule.getConditions());
//
//            int rowNum = 1;
//            for (Map<String, String> row : rows) {
//                System.out.println("Row " + rowNum++ + ": " + row + " => " +
//                        rule.getResult().getEntity() + " = " + rule.getResult().getValue());
//                tax= rule.getResult().getValue();
//                TaxRule taxRule = new TaxRule(row.get("Port"), row.get("Direction"), row.get("CargoType"), tax);
//                taxRuleRepository.save(taxRule);
//            }
//        }

        try {
            String xmlContent = constructXMLAndStoreInDb(rulesMapping);
            TaxRule taxRule = new TaxRule(xmlContent);
            taxRuleRepository.save(taxRule);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        executeRules();
    }

    private void executeRules() {
        // 1. Load KIE container
        KieServices ks = KieServices.Factory.get();
        KieContainer kieContainer = ks.getKieClasspathContainer();

        // 2. Get DMN runtime
        DMNRuntime dmnRuntime =
                KieRuntimeFactory.of(kieContainer.getKieBase("defaultKieBase"))
                        .get(DMNRuntime.class);

        // 3. Load your DMN model
        String namespace = "http://www.example.com/dmn"; // adjust to your DMN namespace
        String modelName = "TaxDecision";               // your DMN model name
        DMNModel dmnModel = dmnRuntime.getModel(namespace, modelName);

        if (dmnModel == null) {
            throw new RuntimeException("DMN model not found!");
        }

        // 4. Create context and set input values (like from your DB row)
        DMNContext context = dmnRuntime.newContext();
        context.set("Port", "Piraeus");
        context.set("Direction", null);
        context.set("CargoType", null);

        // 5. Evaluate decision
        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, context);

        // 6. Extract result
        Object taxValue = dmnResult.getDecisionResultByName("Tax").getResult();

        System.out.println("Calculated Tax = " + taxValue);
    }
}
