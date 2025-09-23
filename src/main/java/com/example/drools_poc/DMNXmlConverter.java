package com.example.drools_poc;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class DMNXmlConverter {

    public static String constructXMLAndStoreInDb(RulesMapping rulesMapping) throws ParserConfigurationException, TransformerException, URISyntaxException, IOException {
        if (rulesMapping.getModel() != Model.DMN) {
            throw new IllegalArgumentException("Only DMN model is supported");
        }

        StringBuilder xml = new StringBuilder();

        // DMN Header
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\" ");
        xml.append("xmlns:dmndi=\"https://www.omg.org/spec/DMN/20191111/DMNDI/\" ");
        xml.append("xmlns:dc=\"http://www.omg.org/spec/DMN/20180521/DC/\" ");
        xml.append("xmlns:di=\"http://www.omg.org/spec/DMN/20180521/DI/\" ");
        xml.append("id=\"generated-dmn\" name=\"Generated DMN\" namespace=\"http://example.com/dmn\">\n\n");

        // Collect all entities used in conditions and results
        Set<String> inputEntities = collectInputEntities(rulesMapping.getRules());
        Set<String> outputEntities = collectOutputEntities(rulesMapping.getRules());

        // Generate Input Data elements
        for (String entity : inputEntities) {
            xml.append("  <inputData id=\"input_").append(sanitizeId(entity)).append("\" name=\"").append(entity).append("\">\n");
            xml.append("    <variable id=\"input_").append(sanitizeId(entity)).append("_var\" name=\"").append(entity).append("\" typeRef=\"string\"/>\n");
            xml.append("  </inputData>\n\n");
        }

        // Generate Decision Table
        xml.append("  <decision id=\"decision_table\" name=\"Business Rules Decision\">\n");
        xml.append("    <variable id=\"decision_var\" name=\"decision\" typeRef=\"Any\"/>\n");

        // Information Requirements (inputs)
        for (String entity : inputEntities) {
            xml.append("    <informationRequirement id=\"req_").append(sanitizeId(entity)).append("\">\n");
            xml.append("      <requiredInput href=\"#input_").append(sanitizeId(entity)).append("\"/>\n");
            xml.append("    </informationRequirement>\n");
        }

        // Decision Table
        xml.append("    <decisionTable id=\"decision_table_impl\" hitPolicy=\"").append(sanitizeId(rulesMapping.getHitPolicy().name())).append("\">\n");

        // Input columns
        for (String entity : inputEntities) {
            xml.append("      <input id=\"input_col_").append(sanitizeId(entity)).append("\">\n");
            xml.append("        <inputExpression id=\"input_expr_").append(sanitizeId(entity)).append("\" typeRef=\"string\">\n");
            xml.append("          <text>").append(entity).append("</text>\n");
            xml.append("        </inputExpression>\n");
            xml.append("      </input>\n");
        }

        // Output columns
        for (String entity : outputEntities) {
            xml.append("      <output id=\"output_col_").append(sanitizeId(entity)).append("\" name=\"").append(entity).append("\" typeRef=\"number\"/>\n");
        }

        // Rules
        for (int i = 0; i < rulesMapping.getRules().size(); i++) {
            Rule rule = rulesMapping.getRules().get(i);
            xml.append("      <rule id=\"rule_").append(i + 1).append("\">\n");

            // Input entries for each input entity
            for (String entity : inputEntities) {
                String condition = generateConditionForEntity(rule.getConditions(), entity);
                xml.append("        <inputEntry id=\"rule_").append(i + 1).append("_input_").append(sanitizeId(entity)).append("\">\n");
                xml.append("          <text>").append(escapeXml(condition)).append("</text>\n");
                xml.append("        </inputEntry>\n");
            }

            // Output entries
            for (String entity : outputEntities) {
                xml.append("        <outputEntry id=\"rule_").append(i + 1).append("_output_").append(sanitizeId(entity)).append("\">\n");
                if (rule.getResult().getEntity().equals(entity)) {
                    xml.append("          <text>").append(rule.getResult().getValue()).append("</text>\n");
                } else {
                    xml.append("          <text>-</text>\n"); // No output for this entity in this rule
                }
                xml.append("        </outputEntry>\n");
            }

            xml.append("      </rule>\n");
        }

        xml.append("    </decisionTable>\n");
        xml.append("  </decision>\n\n");
        xml.append("</definitions>");

        return xml.toString();
    }

    private static Set<String> collectInputEntities(List<Rule> rules) {
        Set<String> entities = new HashSet<>();
        for (Rule rule : rules) {
            collectEntitiesFromConditionGroup(rule.getConditions(), entities);
        }
        return entities.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> collectOutputEntities(List<Rule> rules) {
        return rules.stream()
                .map(rule -> rule.getResult().getEntity())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static void collectEntitiesFromConditionGroup(ConditionGroup group, Set<String> entities) {
        if (group == null) return;

        if (group.getEntity() != null) {
            entities.add(group.getEntity());
        }

        if (group.getChildren() != null) {
            for (ConditionGroup child : group.getChildren()) {
                collectEntitiesFromConditionGroup(child, entities);
            }
        }
    }

    private static String generateConditionForEntity(ConditionGroup conditionGroup, String targetEntity) {
        if (conditionGroup == null) {
            return "-"; // No condition for this entity
        }

        List<String> conditions = collectConditionsForEntity(conditionGroup, targetEntity);

        if (conditions.isEmpty()) {
            return "-"; // No condition for this entity
        }

        if (conditions.size() == 1) {
            return conditions.get(0);
        }

        // Multiple conditions for the same entity - combine them with OR logic
        return conditions.stream().collect(Collectors.joining(" or "));
    }

    private static List<String> collectConditionsForEntity(ConditionGroup group, String targetEntity) {
        List<String> conditions = new ArrayList<>();

        if (group == null) return conditions;

        // If this is a leaf condition for our target entity
        if (group.getEntity() != null && group.getEntity().equals(targetEntity)) {
            if (group.getValue() != null) {
                conditions.add("\"" + group.getValue() + "\"");
            }
            return conditions;
        }

        // If this group has children, process them
        if (group.getChildren() != null && !group.getChildren().isEmpty()) {
            List<String> childConditions = new ArrayList<>();

            for (ConditionGroup child : group.getChildren()) {
                List<String> childResults = collectConditionsForEntity(child, targetEntity);
                childConditions.addAll(childResults);
            }

            if (!childConditions.isEmpty()) {
                if (childConditions.size() == 1) {
                    conditions.addAll(childConditions);
                } else {
                    // Group child conditions based on the operator
                    String operator = group.getOperator();
                    if ("OR".equalsIgnoreCase(operator)) {
                        conditions.add("(" + String.join(" or ", childConditions) + ")");
                    } else { // AND or default
                        conditions.add("(" + String.join(" and ", childConditions) + ")");
                    }
                }
            }
        }

        return conditions;
    }

    private static String sanitizeId(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
