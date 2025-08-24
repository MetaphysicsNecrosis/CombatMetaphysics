package com.example.examplemod.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система управления выносливостью в стиле Gothic
 * Интегрируется с состояниями игрока для реалистичного расхода и восстановления
 */
public class StaminaManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaminaManager.class);
    
    // Данные по выносливости каждого игрока
    private final Map<UUID, StaminaData> playerStamina = new ConcurrentHashMap<>();
    
    // Константы системы
    private static final float DEFAULT_MAX_STAMINA = 100.0f;
    private static final float BASE_REGEN_RATE = 10.0f;      // Единиц в секунду в мирном состоянии
    private static final float EXHAUSTION_THRESHOLD = 10.0f;  // Порог истощения
    private static final float CRITICAL_THRESHOLD = 25.0f;    // Критический уровень
    
    /**
     * Данные о выносливости игрока
     */
    public static class StaminaData {
        private final UUID playerId;
        private float maxStamina;
        private float currentStamina;
        private long lastUpdateTime;
        private boolean isExhausted;
        private long exhaustionStartTime;
        
        // Модификаторы
        private float regenMultiplier;
        private float consumptionMultiplier;
        
        public StaminaData(UUID playerId) {
            this.playerId = playerId;
            this.maxStamina = DEFAULT_MAX_STAMINA;
            this.currentStamina = maxStamina;
            this.lastUpdateTime = System.currentTimeMillis();
            this.isExhausted = false;
            this.exhaustionStartTime = 0;
            this.regenMultiplier = 1.0f;
            this.consumptionMultiplier = 1.0f;
        }
        
        // Геттеры
        public UUID getPlayerId() { return playerId; }
        public float getMaxStamina() { return maxStamina; }
        public float getCurrentStamina() { return currentStamina; }
        public float getStaminaPercentage() { return currentStamina / maxStamina; }
        public boolean isExhausted() { return isExhausted; }
        public boolean isCritical() { return currentStamina <= CRITICAL_THRESHOLD; }
        public boolean isEmpty() { return currentStamina <= 0.1f; }
        
        // Сеттеры
        public void setMaxStamina(float maxStamina) { 
            this.maxStamina = Math.max(10f, maxStamina); 
            this.currentStamina = Math.min(this.currentStamina, this.maxStamina);
        }
        public void setRegenMultiplier(float multiplier) { this.regenMultiplier = multiplier; }
        public void setConsumptionMultiplier(float multiplier) { this.consumptionMultiplier = multiplier; }
        
        /**
         * Обновляет выносливость на основе состояния игрока
         */
        public void update(PlayerState playerState) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            
            if (deltaTime <= 0) return;
            
            float deltaSeconds = deltaTime / 1000.0f;
            float staminaChange = 0;
            
            // Получаем скорость изменения из состояния
            float stateRegenRate = playerState.getStaminaRegenRate();
            
            if (stateRegenRate > 0) {
                // Восстановление выносливости
                staminaChange = BASE_REGEN_RATE * stateRegenRate * regenMultiplier * deltaSeconds;
                
                // Бонус к регенерации в мирном состоянии
                if (playerState == PlayerState.PEACEFUL) {
                    staminaChange *= 1.5f;
                }
                
                // Штраф к регенерации при низкой выносливости
                if (isCritical()) {
                    staminaChange *= 0.7f;
                }
                
            } else if (stateRegenRate < 0) {
                // Расход выносливости
                staminaChange = BASE_REGEN_RATE * stateRegenRate * consumptionMultiplier * deltaSeconds;
            }
            
            // Применяем изменение
            modifyStamina(staminaChange);
            
            // Обновляем состояние истощения
            updateExhaustionState(playerState);
        }
        
        /**
         * Изменяет выносливость на указанную величину
         */
        public boolean modifyStamina(float amount) {
            float oldStamina = currentStamina;
            currentStamina = Math.max(0, Math.min(maxStamina, currentStamina + amount));
            
            boolean changed = Math.abs(oldStamina - currentStamina) > 0.1f;
            
            if (changed && amount < 0) {
                LOGGER.debug("Player {} stamina consumed: {:.1f} -> {:.1f}", 
                           playerId, oldStamina, currentStamina);
            }
            
            return changed;
        }
        
        /**
         * Проверяет, можно ли потратить указанное количество выносливости
         */
        public boolean canConsume(float amount) {
            return currentStamina >= amount;
        }
        
        /**
         * Пытается потратить выносливость
         */
        public boolean tryConsume(float amount, String reason) {
            if (!canConsume(amount)) {
                LOGGER.debug("Player {} insufficient stamina for {}: need {:.1f}, have {:.1f}", 
                           playerId, reason, amount, currentStamina);
                return false;
            }
            
            modifyStamina(-amount);
            LOGGER.debug("Player {} consumed {:.1f} stamina for {}", playerId, amount, reason);
            return true;
        }
        
        /**
         * Обновляет состояние истощения
         */
        private void updateExhaustionState(PlayerState currentState) {
            boolean wasExhausted = isExhausted;
            
            // Проверяем истощение
            if (currentStamina <= EXHAUSTION_THRESHOLD && !isExhausted) {
                isExhausted = true;
                exhaustionStartTime = System.currentTimeMillis();
                LOGGER.debug("Player {} became exhausted", playerId);
            }
            
            // Восстановление из истощения
            if (isExhausted && currentStamina > CRITICAL_THRESHOLD) {
                long exhaustionDuration = System.currentTimeMillis() - exhaustionStartTime;
                
                // Минимум 3 секунды истощения перед восстановлением
                if (exhaustionDuration > 3000) {
                    isExhausted = false;
                    LOGGER.debug("Player {} recovered from exhaustion after {}ms", playerId, exhaustionDuration);
                }
            }
        }
        
        /**
         * Принудительно восстанавливает выносливость
         */
        public void restore(float amount, String reason) {
            modifyStamina(amount);
            LOGGER.debug("Player {} stamina restored by {:.1f} due to {}", playerId, amount, reason);
        }
        
        /**
         * Полностью восстанавливает выносливость
         */
        public void fullRestore(String reason) {
            float restored = maxStamina - currentStamina;
            currentStamina = maxStamina;
            isExhausted = false;
            LOGGER.debug("Player {} stamina fully restored ({:.1f}) due to {}", playerId, restored, reason);
        }
    }
    
    /**
     * Уровни выносливости для UI и поведения
     */
    public enum StaminaLevel {
        FULL(80, 100),      // Полная выносливость
        HIGH(60, 80),       // Высокая
        MEDIUM(40, 60),     // Средняя
        LOW(20, 40),        // Низкая  
        CRITICAL(10, 20),   // Критическая
        EXHAUSTED(0, 10);   // Истощение
        
        public final float minPercentage;
        public final float maxPercentage;
        
        StaminaLevel(float min, float max) {
            this.minPercentage = min;
            this.maxPercentage = max;
        }
        
        public static StaminaLevel fromPercentage(float percentage) {
            float percent = percentage * 100;
            for (StaminaLevel level : values()) {
                if (percent >= level.minPercentage && percent <= level.maxPercentage) {
                    return level;
                }
            }
            return EXHAUSTED;
        }
    }
    
    /**
     * Получает или создает данные выносливости для игрока
     */
    public StaminaData getStaminaData(UUID playerId) {
        return playerStamina.computeIfAbsent(playerId, StaminaData::new);
    }
    
    /**
     * Получает текущую выносливость игрока
     */
    public float getCurrentStamina(UUID playerId) {
        return getStaminaData(playerId).getCurrentStamina();
    }
    
    /**
     * Получает максимальную выносливость игрока
     */
    public float getMaxStamina(UUID playerId) {
        return getStaminaData(playerId).getMaxStamina();
    }
    
    // Compatibility methods for single-player context
    private UUID currentPlayerId = null; // Set during state machine operations
    
    public void setCurrentPlayer(UUID playerId) {
        this.currentPlayerId = playerId;
    }
    
    public float getCurrentStamina() {
        if (currentPlayerId == null) return DEFAULT_MAX_STAMINA;
        return getCurrentStamina(currentPlayerId);
    }
    
    public float getMaxStamina() {
        if (currentPlayerId == null) return DEFAULT_MAX_STAMINA;
        return getMaxStamina(currentPlayerId);
    }
    
    public boolean hasStamina(int amount) {
        if (currentPlayerId == null) return true;
        return getCurrentStamina(currentPlayerId) >= amount;
    }
    
    /**
     * Получает процент выносливости
     */
    public float getStaminaPercentage(UUID playerId) {
        return getStaminaData(playerId).getStaminaPercentage();
    }
    
    /**
     * Получает уровень выносливости
     */
    public StaminaLevel getStaminaLevel(UUID playerId) {
        return StaminaLevel.fromPercentage(getStaminaPercentage(playerId));
    }
    
    /**
     * Проверяет, истощен ли игрок
     */
    public boolean isExhausted(UUID playerId) {
        return getStaminaData(playerId).isExhausted();
    }
    
    /**
     * Проверяет, можно ли потратить выносливость
     */
    public boolean canConsumeStamina(UUID playerId, float amount) {
        return getStaminaData(playerId).canConsume(amount);
    }
    
    /**
     * Пытается потратить выносливость
     */
    public boolean tryConsumeStamina(UUID playerId, float amount, String reason) {
        return getStaminaData(playerId).tryConsume(amount, reason);
    }
    
    /**
     * Восстанавливает выносливость
     */
    public void restoreStamina(UUID playerId, float amount, String reason) {
        getStaminaData(playerId).restore(amount, reason);
    }
    
    /**
     * Полностью восстанавливает выносливость
     */
    public void fullRestoreStamina(UUID playerId, String reason) {
        getStaminaData(playerId).fullRestore(reason);
    }
    
    /**
     * Устанавливает максимальную выносливость (для прогрессии)
     */
    public void setMaxStamina(UUID playerId, float maxStamina) {
        getStaminaData(playerId).setMaxStamina(maxStamina);
    }
    
    /**
     * Устанавливает модификаторы выносливости
     */
    public void setStaminaModifiers(UUID playerId, float regenMultiplier, float consumptionMultiplier) {
        StaminaData data = getStaminaData(playerId);
        data.setRegenMultiplier(regenMultiplier);
        data.setConsumptionMultiplier(consumptionMultiplier);
    }
    
    /**
     * Обновляет выносливость игрока на основе его текущего состояния
     */
    public void updatePlayer(UUID playerId, PlayerState playerState) {
        StaminaData data = getStaminaData(playerId);
        data.update(playerState);
    }
    
    /**
     * Обновляет всех игроков
     */
    public void tick(Map<UUID, PlayerState> playerStates) {
        for (Map.Entry<UUID, StaminaData> entry : playerStamina.entrySet()) {
            UUID playerId = entry.getKey();
            StaminaData data = entry.getValue();
            
            PlayerState state = playerStates.getOrDefault(playerId, PlayerState.PEACEFUL);
            data.update(state);
        }
    }
    
    /**
     * Удаляет данные игрока
     */
    public void removePlayer(UUID playerId) {
        StaminaData removed = playerStamina.remove(playerId);
        if (removed != null) {
            LOGGER.debug("Removed stamina data for player {}", playerId);
        }
    }
    
    /**
     * Очищает все данные
     */
    public void clearAll() {
        playerStamina.clear();
        LOGGER.info("Cleared all stamina data");
    }
    
    /**
     * Получает отладочную информацию
     */
    public Map<String, Object> getDebugInfo(UUID playerId) {
        StaminaData data = playerStamina.get(playerId);
        if (data == null) {
            return Map.of("hasData", false);
        }
        
        return Map.of(
            "hasData", true,
            "currentStamina", data.getCurrentStamina(),
            "maxStamina", data.getMaxStamina(),
            "percentage", data.getStaminaPercentage() * 100,
            "level", getStaminaLevel(playerId).name(),
            "isExhausted", data.isExhausted(),
            "isCritical", data.isCritical(),
            "isEmpty", data.isEmpty(),
            "regenMultiplier", data.regenMultiplier,
            "consumptionMultiplier", data.consumptionMultiplier
        );
    }
    
    /**
     * Проверяет совместимость действия с уровнем выносливости
     */
    public boolean canPerformAction(UUID playerId, String actionType, float staminaCost) {
        StaminaData data = getStaminaData(playerId);
        
        // Проверяем истощение
        if (data.isExhausted() && !actionType.equals("rest")) {
            return false;
        }
        
        // Проверяем достаточность выносливости
        if (!data.canConsume(staminaCost)) {
            return false;
        }
        
        // Дополнительные ограничения для критического уровня
        if (data.isCritical()) {
            return switch (actionType) {
                case "light_attack", "block", "walk" -> true;
                case "heavy_attack", "combo", "sprint", "dodge" -> false;
                default -> data.canConsume(staminaCost);
            };
        }
        
        return true;
    }
}