package com.example.examplemod.core.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Event-Driven State Machine: состояние как композиция способностей
 * Заменяет старую enum-based систему на гибкую capability-based
 */
public class PlayerStateComposition {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStateComposition.class);
    
    private final Set<StateCapability> activeCapabilities = ConcurrentHashMap.newKeySet();
    private final Map<StateCapability, Long> capabilityTimestamps = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    
    public PlayerStateComposition() {
        // По умолчанию только IDLE
        addCapability(StateCapability.IDLE);
    }
    
    /**
     * Добавляет способность с проверками на совместимость
     */
    public boolean addCapability(StateCapability capability) {
        synchronized (lock) {
            // Проверяем совместимость с существующими способностями
            for (StateCapability existing : activeCapabilities) {
                if (!capability.canCoexistWith(existing)) {
                    // Попытка прерывания
                    if (capability.canInterrupt(existing)) {
                        removeCapability(existing);
                        LOGGER.debug("Capability {} interrupted {}", capability, existing);
                    } else {
                        LOGGER.debug("Cannot add capability {} - conflicts with {}", capability, existing);
                        return false;
                    }
                }
            }
            
            activeCapabilities.add(capability);
            capabilityTimestamps.put(capability, System.currentTimeMillis());
            
            // Убираем IDLE если добавили что-то активное
            if (capability != StateCapability.IDLE && activeCapabilities.contains(StateCapability.IDLE)) {
                removeCapability(StateCapability.IDLE);
            }
            
            LOGGER.debug("Added capability: {}. Active: {}", capability, activeCapabilities);
            return true;
        }
    }
    
    /**
     * Убирает способность
     */
    public boolean removeCapability(StateCapability capability) {
        synchronized (lock) {
            boolean removed = activeCapabilities.remove(capability);
            capabilityTimestamps.remove(capability);
            
            // Если убрали все активные способности - добавляем IDLE
            if (activeCapabilities.isEmpty()) {
                addCapability(StateCapability.IDLE);
            }
            
            if (removed) {
                LOGGER.debug("Removed capability: {}. Active: {}", capability, activeCapabilities);
            }
            return removed;
        }
    }
    
    /**
     * Проверяет наличие способности
     */
    public boolean hasCapability(StateCapability capability) {
        return activeCapabilities.contains(capability);
    }
    
    /**
     * Получает все активные способности
     */
    public Set<StateCapability> getActiveCapabilities() {
        return new HashSet<>(activeCapabilities);
    }
    
    /**
     * Получает доминирующую способность (с наивысшим приоритетом)
     */
    public StateCapability getDominantCapability() {
        return activeCapabilities.stream()
                .max(Comparator.comparingInt(StateCapability::getPriority))
                .orElse(StateCapability.IDLE);
    }
    
    /**
     * Проверяет разрешение на действие
     */
    public boolean canPerformAction(StateCapability.ActionType actionType) {
        // Если хотя бы одна способность запрещает - запрещаем
        for (StateCapability capability : activeCapabilities) {
            if (!capability.allowsAction(actionType)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Принудительно устанавливает единственную способность
     */
    public void setExclusiveCapability(StateCapability capability) {
        synchronized (lock) {
            activeCapabilities.clear();
            capabilityTimestamps.clear();
            addCapability(capability);
            LOGGER.debug("Set exclusive capability: {}", capability);
        }
    }
    
    /**
     * Очищает все способности кроме IDLE
     */
    public void reset() {
        synchronized (lock) {
            activeCapabilities.clear();
            capabilityTimestamps.clear();
            addCapability(StateCapability.IDLE);
            LOGGER.debug("Reset to IDLE state");
        }
    }
    
    /**
     * Получает время активности способности
     */
    public long getCapabilityDuration(StateCapability capability) {
        Long timestamp = capabilityTimestamps.get(capability);
        return timestamp != null ? System.currentTimeMillis() - timestamp : 0;
    }
    
    /**
     * Проверяет можно ли прервать текущее состояние
     */
    public boolean canBeInterrupted() {
        return activeCapabilities.stream().allMatch(StateCapability::isInterruptible);
    }
    
    /**
     * Прерывает все прерываемые способности и устанавливает новую
     */
    public boolean interrupt(StateCapability newCapability, String reason) {
        synchronized (lock) {
            // Убираем все прерываемые способности
            Set<StateCapability> toRemove = activeCapabilities.stream()
                    .filter(StateCapability::isInterruptible)
                    .collect(Collectors.toSet());
            
            for (StateCapability cap : toRemove) {
                removeCapability(cap);
            }
            
            boolean success = addCapability(newCapability);
            if (success) {
                LOGGER.warn("Interrupted state with {}: {}", newCapability, reason);
            }
            return success;
        }
    }
    
    /**
     * Debug информация для отладки
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeCapabilities", activeCapabilities.stream()
                .map(StateCapability::getId)
                .collect(Collectors.toList()));
        info.put("dominantCapability", getDominantCapability().getId());
        info.put("canBeInterrupted", canBeInterrupted());
        info.put("capabilityCount", activeCapabilities.size());
        
        // Времена активности
        Map<String, Long> durations = new HashMap<>();
        for (StateCapability cap : activeCapabilities) {
            durations.put(cap.getId(), getCapabilityDuration(cap));
        }
        info.put("capabilityDurations", durations);
        
        return info;
    }
    
    /**
     * Для совместимости со старой системой - эмулируем главное состояние
     */
    public String getLegacyStateName() {
        StateCapability dominant = getDominantCapability();
        return switch (dominant) {
            case CASTING -> "MAGIC_CASTING";
            case CHANNELING -> "MAGIC_PREPARING";
            case QTE_ACTIVE -> "QTE_TRANSITION";
            case ATTACKING -> "MELEE_ATTACKING";
            case DEFENDING -> "BLOCKING";
            case DODGING -> "DODGING";
            case STUNNED -> "INTERRUPTED";
            case INTERRUPTED -> "INTERRUPTED";
            default -> "IDLE";
        };
    }
    
    @Override
    public String toString() {
        return "PlayerState{" + activeCapabilities.stream()
                .map(StateCapability::getId)
                .collect(Collectors.joining(", ")) + "}";
    }
}