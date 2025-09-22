package com.example.drools_poc;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RuleFlattener {


    /**
     * Flatten a condition group into a list of rows (maps).
     */
    public static List<Map<String, String>> flattenConditions(ConditionGroup group) {
        // Leaf condition
        if (group.getEntity() != null && group.getValue() != null) {
            Map<String, String> row = new HashMap<>();
            row.put(group.getEntity(), group.getValue());
            return Collections.singletonList(row);
        }

        // Operator logic
        if ("AND".equalsIgnoreCase(group.getOperator())) {
            List<Map<String, String>> rows = new ArrayList<>();
            rows.add(new HashMap<>()); // start with an empty row

            for (ConditionGroup child : group.getChildren()) {
                List<Map<String, String>> childRows = flattenConditions(child);
                rows = combine(rows, childRows);
            }
            return rows;
        }

        if ("OR".equalsIgnoreCase(group.getOperator())) {
            List<Map<String, String>> allRows = new ArrayList<>();
            for (ConditionGroup child : group.getChildren()) {
                allRows.addAll(flattenConditions(child));
            }
            return allRows;
        }

        return Collections.emptyList();
    }

    /**
     * Cross-join two row lists (used for AND).
     */
    private static List<Map<String, String>> combine(List<Map<String, String>> rows1,
                                                     List<Map<String, String>> rows2) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> r1 : rows1) {
            for (Map<String, String> r2 : rows2) {
                Map<String, String> merged = new HashMap<>(r1);
                merged.putAll(r2);
                result.add(merged);
            }
        }
        return result;
    }
}
