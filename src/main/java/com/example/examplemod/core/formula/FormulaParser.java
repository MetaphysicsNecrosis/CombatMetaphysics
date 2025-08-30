package com.example.examplemod.core.formula;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaParser {
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("[+\\-*/()^]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(sin|cos|tan|sqrt|log|exp|abs|min|max|pow)\\(");
    
    public ParsedFormula parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Пустая формула");
        }
        
        String normalized = normalizeExpression(expression);
        Set<String> parameters = extractParameters(normalized);
        validateSyntax(normalized);
        
        return new ParsedFormula(expression, normalized, parameters.toArray(new String[0]));
    }
    
    private String normalizeExpression(String expression) {
        return expression.trim()
            .replaceAll("\\s+", "")
            .replace("**", "^");
    }
    
    private Set<String> extractParameters(String expression) {
        Set<String> parameters = new HashSet<>();
        Matcher matcher = PARAMETER_PATTERN.matcher(expression);
        
        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }
        
        return parameters;
    }
    
    private void validateSyntax(String expression) {
        if (!isValidSyntax(expression)) {
            throw new IllegalArgumentException("Неверный синтаксис формулы: " + expression);
        }
    }
    
    private boolean isValidSyntax(String expression) {
        int parenthesesCount = 0;
        
        for (char c : expression.toCharArray()) {
            if (c == '(') parenthesesCount++;
            else if (c == ')') parenthesesCount--;
            
            if (parenthesesCount < 0) return false;
        }
        
        return parenthesesCount == 0;
    }
    
    public static class ParsedFormula {
        private final String originalExpression;
        private final String normalizedExpression;
        private final String[] requiredParameters;
        
        public ParsedFormula(String original, String normalized, String[] parameters) {
            this.originalExpression = original;
            this.normalizedExpression = normalized;
            this.requiredParameters = parameters.clone();
        }
        
        public String getOriginalExpression() { return originalExpression; }
        public String getNormalizedExpression() { return normalizedExpression; }
        public String[] getRequiredParameters() { return requiredParameters.clone(); }
    }
}