package com.example.drools_poc;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/rules", consumes = "application/json", produces = "application/json")
public class RulesController {

    private final RuleEngineAdapter ruleEngineAdapter;

    public RulesController(RuleEngineAdapter ruleEngineAdapter) {
        this.ruleEngineAdapter = ruleEngineAdapter;
    }

    @PostMapping(consumes = "application/json")
    public String createRules(@RequestBody RulesMapping rulesMapping) {
        ruleEngineAdapter.process(rulesMapping);
        return "Successfully added rule!";
    }

}
