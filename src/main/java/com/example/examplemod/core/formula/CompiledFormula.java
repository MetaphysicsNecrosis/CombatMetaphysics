package com.example.examplemod.core.formula;

import java.util.Map;
import java.util.function.Function;

public final class CompiledFormula implements Formula {
    
    private final String expression;
    private final String[] requiredParameters;
    private final Function<Map<String, Double>, Double> compiledFunction;
    
    public CompiledFormula(String expression, String[] requiredParameters, 
                          Function<Map<String, Double>, Double> compiledFunction) {
        this.expression = expression;
        this.requiredParameters = requiredParameters.clone();
        this.compiledFunction = compiledFunction;
    }
    
    @Override
    public double evaluate(Map<String, Double> parameters) {
        for (String param : requiredParameters) {
            if (!parameters.containsKey(param)) {
                throw new IllegalArgumentException("Отсутствует параметр: " + param);
            }
        }
        return compiledFunction.apply(parameters);
    }
    
    @Override
    public boolean isCompiled() {
        return true;
    }
    
    @Override
    public String getExpression() {
        return expression;
    }
    
    @Override
    public String[] getRequiredParameters() {
        return requiredParameters.clone();
    }
}