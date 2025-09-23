package com.example.drools_poc;
import com.example.drools_poc.entity.TaxRule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        List<Map<String, String>> totalRules = new ArrayList<>();
        for (Rule rule : rulesMapping.getRules()) {
            System.out.println("Rule ID: " + rule.getId());

            //One rule from the API can produce multiple smaller rules
            List<Map<String, String>> rulesProduced = RuleFlattener.flattenConditions(rule.getConditions());
            totalRules.addAll(rulesProduced);

//            int rowNum = 1;
//            for (Map<String, String> row : rulesProduced) {
//                System.out.println("Row " + rowNum++ + ": " + row + " => " +
//                        rule.getResult().getEntity() + " = " + rule.getResult().getValue());
//            }
        }
        String xmlContent = null;
        try {
            xmlContent = constructXMLAndStoreInDb(rulesMapping);
            TaxRule taxRule = new TaxRule(xmlContent);
            taxRuleRepository.save(taxRule);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        if (xmlContent != null) {
            executeRules(xmlContent);
        }
    }

    private void executeRules(String dmnXml) {
        // 2. Create DMN runtime from XML string
        DMNRuntime dmnRuntime = createDmnRuntimeFromXml(dmnXml);

        // 3. Load the DMN model
        String namespace = "http://example.com/dmn";
        String modelName = "Generated DMN";
        DMNModel dmnModel = dmnRuntime.getModel(namespace, modelName);

        if (dmnModel == null) {
            throw new RuntimeException("DMN model not found!");
        }

        // 4. Create context and set input values (like from your DB row)
        DMNContext context = dmnRuntime.newContext();
        context.set("Port", "Piraeus");
        context.set("Direction", "INBOUND");
        context.set("CargoType", "Containers");

        // 5. Evaluate decision
        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, context);

        // 6. Extract result
        Object taxValue = dmnResult.getDecisionResultByName("Business Rules Decision").getResult();

        System.out.println("Calculated Tax = " + taxValue);
    }

    private DMNRuntime createDmnRuntimeFromXml(String dmnXml) {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // Add the DMN file to the file system
            kieFileSystem.write("src/main/resources/rules.dmn", dmnXml);

            // Build the KIE module
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            // Create KIE container
            KieContainer kieContainer = kieServices.newKieContainer(
                    kieServices.getRepository().getDefaultReleaseId());

            // Get DMN runtime
            return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create DMN runtime from XML", e);
        }
    }
}
