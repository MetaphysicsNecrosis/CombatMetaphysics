package com.example.examplemod.core.actions;

import com.example.examplemod.core.actions.composite.CompositeActionDefinition;
import com.example.examplemod.core.actions.core.*;
import com.example.examplemod.core.pipeline.ActionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Трехуровневый реестр действий по архитектуре Opus:
 * 1. Core Actions (Java) - базовые примитивы
 * 2. Composite Actions (JSON) - комбинации примитивов  
 * 3. Script Actions (KubeJS) - сложная логика
 */
public class ActionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionRegistry.class);
    private static final ActionRegistry INSTANCE = new ActionRegistry();
    
    // Уровень 1: Core Actions (оптимизированные Java классы)
    private final Map<String, CoreActionExecutor> coreActions = new ConcurrentHashMap<>();
    
    // Уровень 2: Composite Actions (JSON конфигурации)
    private final Map<String, CompositeActionExecutor> compositeActions = new ConcurrentHashMap<>();
    
    // Уровень 3: Script Actions (KubeJS/Rhino интеграция) 
    private final Map<String, ScriptActionExecutor> scriptActions = new ConcurrentHashMap<>();
    
    // Кэш для быстрого поиска
    private final Map<String, ActionExecutor> unifiedCache = new ConcurrentHashMap<>();
    
    private ActionRegistry() {
        registerCoreActions();
        loadCompositeActionsFromJSON();
        loadScriptActionsFromKubeJS();
    }
    
    public static ActionRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Регистрация Core Actions (уровень 1) - базовые примитивы
     */
    public void registerCoreAction(String actionType, CoreActionExecutor executor) {
        coreActions.put(actionType, executor);
        unifiedCache.put(actionType, executor);
        LOGGER.debug("Registered Core Action: {}", actionType);
    }
    
    /**
     * Регистрация Composite Actions (уровень 2) - JSON комбинации
     */
    public void registerCompositeAction(String actionType, CompositeActionDefinition definition) {
        CompositeActionExecutor executor = new CompositeActionExecutor(definition);
        compositeActions.put(actionType, executor);
        unifiedCache.put(actionType, executor);
        LOGGER.debug("Registered Composite Action: {}", actionType);
    }
    
    /**
     * Регистрация Script Actions (уровень 3) - KubeJS скрипты
     */
    public void registerScriptAction(String actionType, String scriptPath) {
        ScriptActionExecutor executor = new ScriptActionExecutor(actionType, scriptPath);
        scriptActions.put(actionType, executor);
        unifiedCache.put(actionType, executor);
        LOGGER.debug("Registered Script Action: {}", actionType);
    }
    
    /**
     * Получение исполнителя действия (любого уровня)
     */
    public ActionExecutor getExecutor(String actionType) {
        return unifiedCache.get(actionType);
    }
    
    /**
     * Проверка существования действия
     */
    public boolean hasAction(String actionType) {
        return unifiedCache.containsKey(actionType);
    }
    
    /**
     * Получение информации о действии
     */
    public ActionInfo getActionInfo(String actionType) {
        if (coreActions.containsKey(actionType)) {
            return new ActionInfo(actionType, ActionLevel.CORE, 
                    coreActions.get(actionType).getClass().getSimpleName());
        }
        if (compositeActions.containsKey(actionType)) {
            return new ActionInfo(actionType, ActionLevel.COMPOSITE,
                    compositeActions.get(actionType).getDefinition().getDescription());
        }
        if (scriptActions.containsKey(actionType)) {
            return new ActionInfo(actionType, ActionLevel.SCRIPT,
                    scriptActions.get(actionType).getScriptPath());
        }
        return null;
    }
    
    /**
     * Получение всех зарегистрированных действий по уровню
     */
    public Set<String> getActionsByLevel(ActionLevel level) {
        return switch (level) {
            case CORE -> new HashSet<>(coreActions.keySet());
            case COMPOSITE -> new HashSet<>(compositeActions.keySet());
            case SCRIPT -> new HashSet<>(scriptActions.keySet());
        };
    }
    
    /**
     * Перезагрузка Composite и Script действий (hot reload)
     */
    public void reloadDynamicActions() {
        // Очищаем только dynamic actions из кэша
        compositeActions.keySet().forEach(unifiedCache::remove);
        scriptActions.keySet().forEach(unifiedCache::remove);
        
        compositeActions.clear();
        scriptActions.clear();
        
        // Перезагружаем из файлов
        loadCompositeActionsFromJSON();
        loadScriptActionsFromKubeJS();
        
        LOGGER.info("Reloaded {} composite actions and {} script actions", 
                compositeActions.size(), scriptActions.size());
    }
    
    /**
     * Регистрация стандартных Core Actions
     */
    private void registerCoreActions() {
        // Базовые примитивы - оптимизированы и type-safe
        registerCoreAction("damage", new DamageAction());
        registerCoreAction("heal", new HealAction());
        registerCoreAction("teleport", new TeleportAction());
        registerCoreAction("area_scan", new AreaScanAction());
        registerCoreAction("block_destruction", new BlockDestructionAction());
        registerCoreAction("particle_effect", new ParticleEffectAction());
        registerCoreAction("sound_effect", new SoundEffectAction());
        registerCoreAction("knockback", new KnockbackAction());
        registerCoreAction("status_effect", new StatusEffectAction());
        registerCoreAction("resource_consume", new ResourceConsumeAction());
        
        LOGGER.info("Registered {} core actions", coreActions.size());
    }
    
    /**
     * Загрузка Composite Actions из JSON файлов
     */
    private void loadCompositeActionsFromJSON() {
        com.example.examplemod.core.actions.composite.CompositeActionLoader.loadCompositeActions(this);
    }
    
    /**
     * Загрузка Script Actions из KubeJS
     */
    private void loadScriptActionsFromKubeJS() {
        // TODO: Implement KubeJS integration
        // Горячая перезагрузка скриптов
    }
    
    /**
     * Debug информация о реестре
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalActions", unifiedCache.size());
        info.put("coreActions", coreActions.size());
        info.put("compositeActions", compositeActions.size());
        info.put("scriptActions", scriptActions.size());
        
        Map<String, ActionLevel> actionLevels = new HashMap<>();
        for (String actionType : unifiedCache.keySet()) {
            ActionInfo actionInfo = getActionInfo(actionType);
            if (actionInfo != null) {
                actionLevels.put(actionType, actionInfo.level());
            }
        }
        info.put("actionLevels", actionLevels);
        
        return info;
    }
    
    /**
     * Информация о действии
     */
    public record ActionInfo(String actionType, ActionLevel level, String description) {}
    
    /**
     * Уровни действий
     */
    public enum ActionLevel {
        CORE,       // Java примитивы
        COMPOSITE,  // JSON комбинации
        SCRIPT      // KubeJS скрипты
    }
}