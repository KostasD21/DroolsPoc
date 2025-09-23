package com.example.drools_poc;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import java.util.*;
import java.util.stream.Collectors;

import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class DMNXmlConverter {

    public static String constructXMLAndStoreInDb(RulesMapping rulesMapping) {
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

    public static String constructXMLAndStoreInDb(List<Map<String, String>> totalRules,
                                                  List<Map<String, Double>> totalResults,
                                                  Model model,
                                                  HitPolicy hitPolicy) {
        if (model != Model.DMN) {
            throw new IllegalArgumentException("Only DMN model is supported");
        }

        // Add validation for input parameters
        if (totalRules == null || totalRules.isEmpty()) {
            throw new IllegalArgumentException("totalRules cannot be null or empty");
        }

        if (totalResults == null || totalResults.isEmpty()) {
            throw new IllegalArgumentException("totalResults cannot be null or empty");
        }

        if (totalRules.size() != totalResults.size()) {
            throw new IllegalArgumentException("totalRules and totalResults must have the same size. Rules: " +
                    totalRules.size() + ", Results: " + totalResults.size());
        }

        try {
            // 1. Prepare XML document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            // 2. Root <definitions>
            Element definitions = doc.createElementNS("https://www.omg.org/spec/DMN/20191111/MODEL/", "definitions");
            definitions.setAttribute("xmlns:dmndi", "https://www.omg.org/spec/DMN/20191111/DMNDI/");
            definitions.setAttribute("xmlns:dc", "http://www.omg.org/spec/DMN/20180521/DC/");
            definitions.setAttribute("xmlns:di", "http://www.omg.org/spec/DMN/20180521/DI/");
            definitions.setAttribute("id", "generated-dmn");
            definitions.setAttribute("name", "Generated DMN");
            definitions.setAttribute("namespace", "http://example.com/dmn");
            doc.appendChild(definitions);

            // 3. Get consistent column names for inputs and outputs
            Set<String> inputColumnNames = new LinkedHashSet<>();
            Set<String> outputColumnNames = new LinkedHashSet<>();

            // Collect all possible input column names from all rules
            for (Map<String, String> rule : totalRules) {
                inputColumnNames.addAll(rule.keySet());
            }

            // Collect all possible output column names from all results
            for (Map<String, Double> result : totalResults) {
                outputColumnNames.addAll(result.keySet());
            }

            // 4. InputData elements (collect from rules keys)
            for (String inputName : inputColumnNames) {
                Element inputData = doc.createElement("inputData");
                inputData.setAttribute("id", "input_" + sanitizeId(inputName));
                inputData.setAttribute("name", inputName);

                Element variable = doc.createElement("variable");
                variable.setAttribute("id", "input_" + sanitizeId(inputName) + "_var");
                variable.setAttribute("name", inputName);
                variable.setAttribute("typeRef", "string");

                inputData.appendChild(variable);
                definitions.appendChild(inputData);
            }

            // 5. Decision element
            Element decision = doc.createElement("decision");
            decision.setAttribute("id", "decision_table");
            decision.setAttribute("name", "Business Rules Decision");

            Element decisionVar = doc.createElement("variable");
            decisionVar.setAttribute("id", "decision_var");
            decisionVar.setAttribute("name", "decision");
            decisionVar.setAttribute("typeRef", "Any");
            decision.appendChild(decisionVar);

            // Add information requirements
            for (String inputName : inputColumnNames) {
                Element infoReq = doc.createElement("informationRequirement");
                infoReq.setAttribute("id", "req_" + sanitizeId(inputName));

                Element requiredInput = doc.createElement("requiredInput");
                requiredInput.setAttribute("href", "#input_" + sanitizeId(inputName));

                infoReq.appendChild(requiredInput);
                decision.appendChild(infoReq);
            }

            // 6. DecisionTable
            Element decisionTable = doc.createElement("decisionTable");
            decisionTable.setAttribute("id", "decision_table_impl");
            decisionTable.setAttribute("hitPolicy", hitPolicy.name());

            // Input columns - use consistent ordering
            for (String inputName : inputColumnNames) {
                Element input = doc.createElement("input");
                input.setAttribute("id", "input_col_" + sanitizeId(inputName));

                Element inputExpr = doc.createElement("inputExpression");
                inputExpr.setAttribute("id", "input_expr_" + sanitizeId(inputName));
                inputExpr.setAttribute("typeRef", "string");

                Element text = doc.createElement("text");
                text.setTextContent(inputName);

                inputExpr.appendChild(text);
                input.appendChild(inputExpr);
                decisionTable.appendChild(input);
            }

            // Output columns - use consistent ordering
            for (String outputName : outputColumnNames) {
                Element output = doc.createElement("output");
                output.setAttribute("id", "output_col_" + sanitizeId(outputName));
                output.setAttribute("name", outputName);
                output.setAttribute("typeRef", "number");
                decisionTable.appendChild(output);
            }

            // Rules - ensure each rule has entries for ALL input and output columns
            for (int i = 0; i < totalRules.size(); i++) {
                Map<String, String> ruleInputs = totalRules.get(i);
                Map<String, Double> ruleOutputs = totalResults.get(i);

                Element rule = doc.createElement("rule");
                rule.setAttribute("id", "rule_" + (i + 1));

                // Input entries - must match the order of input columns defined above
                for (String inputName : inputColumnNames) {
                    Element inputEntry = doc.createElement("inputEntry");
                    inputEntry.setAttribute("id", "rule_" + (i + 1) + "_input_" + sanitizeId(inputName));

                    Element text = doc.createElement("text");
                    String inputValue = ruleInputs.get(inputName);
                    if (inputValue != null) {
                        text.setTextContent("\"" + inputValue + "\"");
                    } else {
                        text.setTextContent("-"); // No condition for this input
                    }
                    inputEntry.appendChild(text);

                    rule.appendChild(inputEntry);
                }

                // Output entries - must match the order of output columns defined above
                for (String outputName : outputColumnNames) {
                    Element outputEntry = doc.createElement("outputEntry");
                    outputEntry.setAttribute("id", "rule_" + (i + 1) + "_output_" + sanitizeId(outputName));

                    Element text = doc.createElement("text");
                    Double outputValue = ruleOutputs.get(outputName);
                    if (outputValue != null) {
                        text.setTextContent(outputValue.toString());
                    } else {
                        text.setTextContent("-"); // No output for this column in this rule
                    }
                    outputEntry.appendChild(text);

                    rule.appendChild(outputEntry);
                }

                decisionTable.appendChild(rule);
            }

            decision.appendChild(decisionTable);
            definitions.appendChild(decision);

            // 7. Convert Document → String
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error constructing DMN XML", e);
        }
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
