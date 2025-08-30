package com.example.examplemod.core.formula;

import java.util.Map;

public interface Formula {
    
    double evaluate(Map<String, Double> parameters);
    
    boolean isCompiled();
    
    String getExpression();
    
    String[] getRequiredParameters();
    
    default boolean hasParameter(String paramName) {
        for (String param : getRequiredParameters()) {
            if (param.equals(paramName)) {
                return true;
            }
        }
        return false;
    }
}