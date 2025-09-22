package com.example.drools_poc;

import java.util.List;

public class RulesMapping {
    private List<Rule> rules;
    private Model model;

    public RulesMapping() {}

    public List<Rule> getRules() {
        return rules;
    }
    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }
}

enum Model {
    DMN,
    DRL
}

class Rule {
    private String id;
    private ConditionGroup conditions;
    private Result result;

    public Rule() {}

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public ConditionGroup getConditions() {
        return conditions;
    }
    public void setConditions(ConditionGroup conditions) {
        this.conditions = conditions;
    }

    public Result getResult() {
        return result;
    }
    public void setResult(Result result) {
        this.result = result;
    }
}

class ConditionGroup {
    private String operator; // AND / OR
    private List<ConditionGroup> children; // can hold groups or conditions
    private String entity;
    private String value;

    public ConditionGroup() {}

    public String getOperator() {
        return operator;
    }
    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<ConditionGroup> getChildren() {
        return children;
    }
    public void setChildren(List<ConditionGroup> children) {
        this.children = children;
    }

    public String getEntity() {
        return entity;
    }
    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}

class Result {
    private String entity;
    private Double value;

    public Result() {}

    public String getEntity() {
        return entity;
    }
    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Double getValue() {
        return value;
    }
    public void setValue(Double value) {
        this.value = value;
    }
}

