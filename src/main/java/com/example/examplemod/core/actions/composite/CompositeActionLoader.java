package com.example.examplemod.core.actions.composite;

import com.example.examplemod.core.actions.ActionRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Загрузчик Composite Actions из JSON файлов
 */
public class CompositeActionLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeActionLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Загружает все Composite Actions из ресурсов
     */
    public static void loadCompositeActions(ActionRegistry registry) {
        try {
            // Загружаем конкретный файл meteor_strike.json для тестирования
            loadCompositeAction(registry, "/data/combatmetaphysics/actions/meteor_strike.json");
            
            // TODO: Implement automatic discovery of all JSON files in actions directory
            
        } catch (Exception e) {
            LOGGER.error("Failed to load composite actions", e);
        }
    }
    
    /**
     * Загружает одно Composite Action из JSON файла
     */
    public static void loadCompositeAction(ActionRegistry registry, String resourcePath) {
        try {
            InputStream stream = CompositeActionLoader.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                LOGGER.error("Composite action file not found: {}", resourcePath);
                return;
            }
            
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                CompositeActionDefinition definition = parseCompositeAction(json);
                
                if (definition != null) {
                    registry.registerCompositeAction(definition.getActionType(), definition);
                    LOGGER.info("Loaded composite action: {} from {}", definition.getActionType(), resourcePath);
                } else {
                    LOGGER.error("Failed to parse composite action from: {}", resourcePath);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading composite action from {}: {}", resourcePath, e.getMessage(), e);
        }
    }
    
    /**
     * Парсит JSON в CompositeActionDefinition
     */
    private static CompositeActionDefinition parseCompositeAction(JsonObject json) {
        try {
            String actionType = json.get("actionType").getAsString();
            String description = json.get("description").getAsString();
            
            // Парсим шаги
            List<CompositeActionStep> steps = parseSteps(json.getAsJsonArray("steps"));
            
            // Парсим условия (пока пустые)
            List<CompositeCondition> conditions = new ArrayList<>();
            
            // Парсим параметры по умолчанию
            Map<String, Object> defaultParameters = new HashMap<>();
            if (json.has("defaultParameters")) {
                JsonObject params = json.getAsJsonObject("defaultParameters");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : params.entrySet()) {
                    defaultParameters.put(entry.getKey(), parseJsonValue(entry.getValue()));
                }
            }
            
            return new CompositeActionDefinition(actionType, description, steps, conditions, defaultParameters);
            
        } catch (Exception e) {
            LOGGER.error("Error parsing composite action JSON", e);
            return null;
        }
    }
    
    /**
     * Парсит шаги из JSON массива
     */
    private static List<CompositeActionStep> parseSteps(com.google.gson.JsonArray stepsArray) {
        List<CompositeActionStep> steps = new ArrayList<>();
        
        for (com.google.gson.JsonElement stepElement : stepsArray) {
            JsonObject stepJson = stepElement.getAsJsonObject();
            
            String actionType = stepJson.get("actionType").getAsString();
            boolean required = stepJson.has("required") ? stepJson.get("required").getAsBoolean() : true;
            boolean breakOnSuccess = stepJson.has("breakOnSuccess") ? stepJson.get("breakOnSuccess").getAsBoolean() : false;
            
            Map<String, Object> parameters = new HashMap<>();
            if (stepJson.has("parameters")) {
                JsonObject params = stepJson.getAsJsonObject("parameters");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : params.entrySet()) {
                    parameters.put(entry.getKey(), parseJsonValue(entry.getValue()));
                }
            }
            
            // Пока без условий шага
            List<CompositeCondition> stepConditions = new ArrayList<>();
            
            steps.add(new CompositeActionStep(actionType, parameters, required, breakOnSuccess, stepConditions));
        }
        
        return steps;
    }
    
    /**
     * Парсит JSON значение в Java объект
     */
    private static Object parseJsonValue(com.google.gson.JsonElement element) {
        if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isNumber()) {
                // Попробуем определить тип числа
                Number number = primitive.getAsNumber();
                if (number.toString().contains(".")) {
                    return number.floatValue();
                } else {
                    return number.intValue();
                }
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
        } else if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (com.google.gson.JsonElement arrayElement : element.getAsJsonArray()) {
                list.add(parseJsonValue(arrayElement));
            }
            return list;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), parseJsonValue(entry.getValue()));
            }
            return map;
        }
        
        return element.getAsString(); // Fallback
    }
}