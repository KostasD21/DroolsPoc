package com.example.drools_poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineAdapter {

    @Autowired
    private DMNRuleEngine dmnRuleEngine;

    public void process(RulesMapping rulesMapping) {
        if (rulesMapping.getRules() != null && rulesMapping.getModel().equals(Model.DMN)) {
            dmnRuleEngine.process(rulesMapping);
        }
    }
}
