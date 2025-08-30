package com.example.examplemod.core.formula;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FormulaLoader {
    
    private static final Gson GSON = new Gson();
    
    public Map<String, String> loadFormulasFromJson(String filePath) throws IOException {
        Map<String, String> formulas = new HashMap<>();
        
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            if (root.has("formulas")) {
                JsonObject formulasObj = root.getAsJsonObject("formulas");
                for (String key : formulasObj.keySet()) {
                    String expression = formulasObj.get(key).getAsString();
                    formulas.put(key, expression);
                }
            }
        }
        
        return formulas;
    }
    
    public FormulaConfig loadConfigFromJson(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            return GSON.fromJson(reader, FormulaConfig.class);
        }
    }
    
    public static class FormulaConfig {
        public Map<String, String> formulas = new HashMap<>();
        public Map<String, Double> constants = new HashMap<>();
        public String version = "1.0";
        
        public boolean isValid() {
            return formulas != null && !formulas.isEmpty();
        }
    }
}