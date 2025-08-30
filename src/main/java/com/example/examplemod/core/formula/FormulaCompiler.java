package com.example.examplemod.core.formula;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

public class FormulaCompiler {
    
    private static final Map<String, Function<Double, Double>> FUNCTIONS = new HashMap<>();
    
    static {
        FUNCTIONS.put("sin", Math::sin);
        FUNCTIONS.put("cos", Math::cos);
        FUNCTIONS.put("tan", Math::tan);
        FUNCTIONS.put("sqrt", Math::sqrt);
        FUNCTIONS.put("log", Math::log);
        FUNCTIONS.put("exp", Math::exp);
        FUNCTIONS.put("abs", Math::abs);
    }
    
    public CompiledFormula compile(FormulaParser.ParsedFormula parsed) {
        String normalized = parsed.getNormalizedExpression();
        String[] parameters = parsed.getRequiredParameters();
        
        Function<Map<String, Double>, Double> compiledFunction = createCompiledFunction(normalized, parameters);
        
        return new CompiledFormula(parsed.getOriginalExpression(), parameters, compiledFunction);
    }
    
    private Function<Map<String, Double>, Double> createCompiledFunction(String expression, String[] parameters) {
        return paramMap -> {
            String processedExpression = expression;
            
            for (String param : parameters) {
                String placeholder = "{" + param + "}";
                Double value = paramMap.get(param);
                if (value == null) {
                    throw new IllegalArgumentException("Отсутствует параметр: " + param);
                }
                processedExpression = processedExpression.replace(placeholder, value.toString());
            }
            
            return evaluateExpression(processedExpression);
        };
    }
    
    private double evaluateExpression(String expression) {
        try {
            return new ExpressionEvaluator().evaluate(expression);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка вычисления формулы: " + expression, e);
        }
    }
    
    private static class ExpressionEvaluator {
        
        private int pos = -1;
        private int ch;
        private String expression;
        
        public double evaluate(String expr) {
            this.expression = expr;
            this.pos = -1;
            nextChar();
            double result = parseExpression();
            if (pos < expression.length()) {
                throw new RuntimeException("Неожиданный символ: " + (char)ch);
            }
            return result;
        }
        
        private void nextChar() {
            ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
        }
        
        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }
        
        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }
        
        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else return x;
            }
        }
        
        private double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();
            
            double x;
            int startPos = this.pos;
            
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(expression.substring(startPos, this.pos));
            } else if (ch >= 'a' && ch <= 'z') {
                while (ch >= 'a' && ch <= 'z') nextChar();
                String func = expression.substring(startPos, this.pos);
                x = parseFactor();
                
                Function<Double, Double> function = FUNCTIONS.get(func);
                if (function != null) {
                    x = function.apply(x);
                } else {
                    throw new RuntimeException("Неизвестная функция: " + func);
                }
            } else {
                throw new RuntimeException("Неожиданный символ: " + (char)ch);
            }
            
            if (eat('^')) x = Math.pow(x, parseFactor());
            
            return x;
        }
    }
}