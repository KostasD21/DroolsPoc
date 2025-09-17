package com.example.drools_poc;

import org.mvel2.MVEL;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.*;

public class CsvRuleEngine {

    static class Rule {
        String expression;
        double taxRate;
        Serializable compiledExpression;

        Rule(String expression, double taxRate) {
            this.expression = expression;
            this.taxRate = taxRate;
            this.compiledExpression = MVEL.compileExpression(expression);
        }
    }

    private final List<Rule> rules = new ArrayList<>();

    // Load CSV into rules list
    public void loadRules(String filePath) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String expr = parts[1].trim();
                double tax = Double.parseDouble(parts[2].trim());
                rules.add(new Rule(expr, tax));
            }
        }
    }

    // Evaluate reservation against rules
    public double evaluate(Reservation reservation) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("port", reservation.getPort());
        vars.put("direction", reservation.getDirection());
        vars.put("travelDate", reservation.getTravelDate());

        for (Rule r : rules) {
            Boolean match = (Boolean) MVEL.executeExpression(r.compiledExpression, vars);
            if (Boolean.TRUE.equals(match)) {
                return r.taxRate;
            }
        }
        throw new IllegalStateException("No matching rule found!");
    }
}

