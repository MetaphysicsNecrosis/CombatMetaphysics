package com.example.examplemod.core.formula;

import java.util.Map;

public record FormulaContext(
    Map<String, Double> parameters,
    long timestamp,
    String contextId
) {
    
    public static FormulaContext create(Map<String, Double> parameters) {
        return new FormulaContext(
            Map.copyOf(parameters),
            System.currentTimeMillis(),
            generateContextId()
        );
    }
    
    private static String generateContextId() {
        return "ctx_" + System.nanoTime();
    }
    
    public double getParameter(String key, double defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }
    
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
}