package com.example.drools_poc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

public class DMNXmlConverter {

    public static String constructXMLAndStoreInDb(RulesMapping rulesMapping) throws ParserConfigurationException, TransformerException, URISyntaxException, IOException {
        // Build DMN XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element definitions = doc.createElement("definitions");
        definitions.setAttribute("xmlns", "https://www.omg.org/spec/DMN/20191111/MODEL/");
        definitions.setAttribute("id", "definitions_1");
        definitions.setAttribute("name", "TaxDecision");
        definitions.setAttribute("namespace", "http://example.com/dmn");
        doc.appendChild(definitions);

        Element variable = doc.createElement("variable");
        variable.setAttribute("name", "Tax Decision");
        variable.setAttribute("typeRef", "number"); // or string/boolean depending on output type
        definitions.appendChild(variable);

        Element decision = doc.createElement("decision");
        decision.setAttribute("id", "decision_1");
        decision.setAttribute("name", "Tax Decision");
        definitions.appendChild(decision);

        Element decisionTable = doc.createElement("decisionTable");
        decisionTable.setAttribute("id", "decisionTable_1");
        decisionTable.setAttribute("hitPolicy", "FIRST");
        decision.appendChild(decisionTable);

        // Collect all input entities
        Set<String> inputEntities = new LinkedHashSet<>();
        for (Rule rule : rulesMapping.getRules()) {
            collectEntities(rule.getConditions(), inputEntities);
        }

        // Add input columns
        for (String entity : inputEntities) {
            Element input = doc.createElement("input");
            input.setAttribute("id", "input_" + entity);

            Element inputExpression = doc.createElement("inputExpression");
            inputExpression.setAttribute("id", "inputExpr_" + entity);
            inputExpression.setAttribute("typeRef", "string");

            Element text = doc.createElement("text");
            text.setTextContent(entity);

            inputExpression.appendChild(text);
            input.appendChild(inputExpression);
            decisionTable.appendChild(input);
        }

        // Add output column (from result)
        String outputEntity = rulesMapping.getRules().get(0).getResult().getEntity();
        Element output = doc.createElement("output");
        output.setAttribute("id", "output_" + outputEntity);
        output.setAttribute("name", outputEntity);
        output.setAttribute("typeRef", "number");
        decisionTable.appendChild(output);

        // Build rules
        for (Rule rule : rulesMapping.getRules()) {
            expandConditions(doc, decisionTable, rule.getConditions(), rule.getResult(), inputEntities, rule.getId());
        }

        // Write XML file
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    // Collect all unique entities for input columns
    private static void collectEntities(ConditionGroup group, Set<String> inputs) {
        if (group.getEntity() != null) {
            inputs.add(group.getEntity());
        }
        if (group.getChildren() != null) {
            for (ConditionGroup child : group.getChildren()) {
                collectEntities(child, inputs);
            }
        }
    }

    // Expand conditions into rules
    private static void expandConditions(Document doc, Element decisionTable,
                                         ConditionGroup group, Result result,
                                         Set<String> inputEntities, String ruleId) {

        if ("OR".equalsIgnoreCase(group.getOperator())) {
            for (ConditionGroup child : group.getChildren()) {
                createRule(doc, decisionTable, child, result, inputEntities, ruleId + "_or");
            }
        } else {
            createRule(doc, decisionTable, group, result, inputEntities, ruleId);
        }
    }

    private static void createRule(Document doc, Element decisionTable,
                                   ConditionGroup group, Result result,
                                   Set<String> inputEntities, String ruleId) {

        Element ruleElem = doc.createElement("rule");
        ruleElem.setAttribute("id", "rule_" + ruleId);

        // Add one inputEntry for each column
        for (String entity : inputEntities) {
            Element inputEntry = doc.createElement("inputEntry");

            String value = extractValueForEntity(group, entity);
            Element text = doc.createElement("text");
            text.setTextContent(value != null ? "\"" + value + "\"" : "-"); // "-" = don't care
            inputEntry.appendChild(text);

            ruleElem.appendChild(inputEntry);
        }

        // Add result
        Element outputEntry = doc.createElement("outputEntry");
        Element text = doc.createElement("text");
        text.setTextContent(result.getValue().toString());
        outputEntry.appendChild(text);
        ruleElem.appendChild(outputEntry);

        decisionTable.appendChild(ruleElem);
    }

    private static String extractValueForEntity(ConditionGroup group, String entity) {
        if (entity.equals(group.getEntity())) {
            return group.getValue();
        }
        if (group.getChildren() != null) {
            for (ConditionGroup child : group.getChildren()) {
                String val = extractValueForEntity(child, entity);
                if (val != null) return val;
            }
        }
        return null;
    }
}
