package com.example.examplemod.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система Quick Time Events для магических комбо-переходов
 * Согласно CLAUDE.md: QTE для переходов между заклинаниями в комбо
 * 90-100% = бонус эффективности, 50-69% = пониженная эффективность, <50% = провал
 */
public class QTESystem {
    
    public enum QTEType {
        TIMING_SEQUENCE(500, "Sequential timing inputs"),
        RHYTHM_PATTERN(800, "Rhythmic button presses"),
        PRECISION_TARGET(300, "Precise timing window"),
        COMBO_CHAIN(1000, "Multi-spell combo chain");
        
        private final long timeWindow; // Время на выполнение QTE в миллисекундах
        private final String description;
        
        QTEType(long timeWindow, String description) {
            this.timeWindow = timeWindow;
            this.description = description;
        }
        
        public long getTimeWindow() { return timeWindow; }
        public String getDescription() { return description; }
    }
    
    public static class QTEEvent {
        private final UUID playerId;
        private final QTEType type;
        private final String currentSpell;
        private final String nextSpell;
        private final long startTime;
        private final long[] targetTimings; // Времена для нажатий
        private final boolean[] inputReceived; // Получены ли нажатия
        private final long[] actualTimings; // Фактические времена нажатий
        private boolean isActive;
        private boolean isCompleted;
        private float finalScore;
        
        public QTEEvent(UUID playerId, QTEType type, String currentSpell, String nextSpell, 
                       long[] targetTimings) {
            this.playerId = playerId;
            this.type = type;
            this.currentSpell = currentSpell;
            this.nextSpell = nextSpell;
            this.startTime = System.currentTimeMillis();
            this.targetTimings = targetTimings;
            this.inputReceived = new boolean[targetTimings.length];
            this.actualTimings = new long[targetTimings.length];
            this.isActive = true;
            this.isCompleted = false;
            this.finalScore = 0f;
        }
        
        public UUID getPlayerId() { return playerId; }
        public QTEType getType() { return type; }
        public String getCurrentSpell() { return currentSpell; }
        public String getNextSpell() { return nextSpell; }
        public long getStartTime() { return startTime; }
        public boolean isActive() { return isActive; }
        public boolean isCompleted() { return isCompleted; }
        public float getFinalScore() { return finalScore; }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public boolean hasExpired() {
            return getElapsedTime() > type.getTimeWindow();
        }
        
        public int getExpectedInputCount() {
            return targetTimings.length;
        }
        
        public int getReceivedInputCount() {
            int count = 0;
            for (boolean received : inputReceived) {
                if (received) count++;
            }
            return count;
        }
        
        public boolean processInput(long inputTime) {
            if (!isActive || isCompleted || hasExpired()) {
                return false;
            }
            
            long relativeTime = inputTime - startTime;
            
            // Находим ближайший ожидаемый тайминг
            int closestIndex = -1;
            long closestDifference = Long.MAX_VALUE;
            
            for (int i = 0; i < targetTimings.length; i++) {
                if (inputReceived[i]) continue; // Уже получен ввод для этого тайминга
                
                long difference = Math.abs(relativeTime - targetTimings[i]);
                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestIndex = i;
                }
            }
            
            if (closestIndex != -1 && closestDifference <= 200) { // 200ms tolerance
                inputReceived[closestIndex] = true;
                actualTimings[closestIndex] = relativeTime;
                
                // Проверяем, все ли нажатия получены
                if (getReceivedInputCount() == targetTimings.length) {
                    completeQTE();
                }
                return true;
            }
            
            return false; // Нажатие не в нужное время
        }
        
        private void completeQTE() {
            isCompleted = true;
            isActive = false;
            finalScore = calculateScore();
        }
        
        public void forceExpire() {
            isActive = false;
            if (!isCompleted) {
                finalScore = calculateScore(); // Частичный счет
            }
        }
        
        private float calculateScore() {
            if (getReceivedInputCount() == 0) {
                return 0f;
            }
            
            float totalScore = 0f;
            int validInputs = 0;
            
            for (int i = 0; i < targetTimings.length; i++) {
                if (inputReceived[i]) {
                    long difference = Math.abs(actualTimings[i] - targetTimings[i]);
                    float inputScore = calculateInputScore(difference);
                    totalScore += inputScore;
                    validInputs++;
                }
            }
            
            if (validInputs == 0) return 0f;
            
            float averageScore = totalScore / validInputs;
            
            // Бонус за завершение всех нажатий
            if (validInputs == targetTimings.length) {
                averageScore *= 1.2f; // +20% бонус
            }
            
            return Math.min(1.0f, averageScore);
        }
        
        private float calculateInputScore(long timingDifference) {
            if (timingDifference <= 50) return 1.0f;   // Идеальный тайминг
            if (timingDifference <= 100) return 0.9f;  // Отличный тайминг
            if (timingDifference <= 150) return 0.7f;  // Хороший тайминг
            if (timingDifference <= 200) return 0.5f;  // Приемлемый тайминг
            return 0.2f; // Плохой тайминг
        }
        
        public String getScoreDescription() {
            if (finalScore >= 0.9f) return "Perfect!";
            if (finalScore >= 0.7f) return "Excellent";
            if (finalScore >= 0.5f) return "Good";
            if (finalScore >= 0.3f) return "Fair";
            return "Poor";
        }
        
        public QTEResult.EfficiencyLevel getEfficiencyLevel() {
            if (finalScore >= 0.9f) return QTEResult.EfficiencyLevel.BONUS;
            if (finalScore >= 0.7f) return QTEResult.EfficiencyLevel.NORMAL;
            if (finalScore >= 0.5f) return QTEResult.EfficiencyLevel.REDUCED;
            return QTEResult.EfficiencyLevel.FAILED;
        }
    }
    
    public static class QTEResult {
        public enum EfficiencyLevel {
            BONUS(1.5f, "Bonus efficiency (+50%)"),
            NORMAL(1.0f, "Normal efficiency"),
            REDUCED(0.7f, "Reduced efficiency (-30%)"),
            FAILED(0f, "Combo failed");
            
            private final float multiplier;
            private final String description;
            
            EfficiencyLevel(float multiplier, String description) {
                this.multiplier = multiplier;
                this.description = description;
            }
            
            public float getMultiplier() { return multiplier; }
            public String getDescription() { return description; }
        }
        
        private final boolean success;
        private final float score;
        private final EfficiencyLevel efficiency;
        private final String nextSpell;
        private final float manaCostMultiplier;
        private final String message;
        
        public QTEResult(boolean success, float score, EfficiencyLevel efficiency, 
                        String nextSpell, float manaCostMultiplier, String message) {
            this.success = success;
            this.score = score;
            this.efficiency = efficiency;
            this.nextSpell = nextSpell;
            this.manaCostMultiplier = manaCostMultiplier;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public float getScore() { return score; }
        public EfficiencyLevel getEfficiency() { return efficiency; }
        public String getNextSpell() { return nextSpell; }
        public float getManaCostMultiplier() { return manaCostMultiplier; }
        public String getMessage() { return message; }
        
        public static QTEResult success(float score, EfficiencyLevel efficiency, String nextSpell) {
            float manaCost = efficiency == EfficiencyLevel.BONUS ? 0.8f : 
                           efficiency == EfficiencyLevel.REDUCED ? 1.2f : 1.0f;
            return new QTEResult(true, score, efficiency, nextSpell, manaCost,
                String.format("QTE Success! Score: %.0f%% - %s", score * 100, efficiency.getDescription()));
        }
        
        public static QTEResult failure(float score, String message) {
            return new QTEResult(false, score, EfficiencyLevel.FAILED, null, 0f, message);
        }
    }
    
    private final Map<UUID, QTEEvent> activeQTEs = new ConcurrentHashMap<>();
    private final Map<String, String[]> spellComboChains = new HashMap<>();
    private final Random random = new Random();
    
    public QTESystem() {
        initializeSpellChains();
    }
    
    private void initializeSpellChains() {
        // Предопределенные магические комбо-цепочки
        spellComboChains.put("fireball", new String[]{"flame_burst", "inferno", "meteor"});
        spellComboChains.put("ice_shard", new String[]{"frost_bolt", "blizzard", "absolute_zero"});
        spellComboChains.put("lightning", new String[]{"chain_lightning", "thunder_storm", "divine_wrath"});
        spellComboChains.put("heal", new String[]{"greater_heal", "mass_heal", "resurrection"});
        spellComboChains.put("shield", new String[]{"barrier", "fortress", "invulnerability"});
    }
    
    /**
     * Начинает QTE для перехода к следующему заклинанию в комбо
     */
    public boolean startQTE(UUID playerId, String currentSpell, QTEType type) {
        // Проверяем, есть ли уже активное QTE
        if (activeQTEs.containsKey(playerId)) {
            return false;
        }
        
        // Получаем возможные следующие заклинания
        String[] availableSpells = spellComboChains.get(currentSpell.toLowerCase());
        if (availableSpells == null || availableSpells.length == 0) {
            return false; // Нет доступных комбо для этого заклинания
        }
        
        // Выбираем случайное следующее заклинание
        String nextSpell = availableSpells[random.nextInt(availableSpells.length)];
        
        // Генерируем таймиnги для QTE
        long[] targetTimings = generateTargetTimings(type);
        
        QTEEvent qteEvent = new QTEEvent(playerId, type, currentSpell, nextSpell, targetTimings);
        activeQTEs.put(playerId, qteEvent);
        
        return true;
    }
    
    private long[] generateTargetTimings(QTEType type) {
        return switch (type) {
            case TIMING_SEQUENCE -> new long[]{200, 400}; // 2 нажатия через 200мс
            case RHYTHM_PATTERN -> new long[]{150, 300, 450, 600}; // 4 ритмичных нажатия
            case PRECISION_TARGET -> new long[]{250}; // 1 точное нажатие
            case COMBO_CHAIN -> new long[]{180, 360, 540, 720, 900}; // 5 нажатий для сложного комбо
        };
    }
    
    /**
     * Обрабатывает ввод игрока для активного QTE
     */
    public QTEResult processQTEInput(UUID playerId, long inputTime) {
        QTEEvent qte = activeQTEs.get(playerId);
        if (qte == null) {
            return QTEResult.failure(0f, "No active QTE found");
        }
        
        if (qte.hasExpired()) {
            QTEResult result = completeQTE(playerId);
            return result != null ? result : QTEResult.failure(0f, "QTE expired");
        }
        
        boolean inputAccepted = qte.processInput(inputTime);
        
        if (qte.isCompleted()) {
            return completeQTE(playerId);
        }
        
        if (!inputAccepted) {
            return QTEResult.failure(qte.getFinalScore(), "Input timing was off");
        }
        
        // QTE продолжается
        return null;
    }
    
    /**
     * Завершает QTE и возвращает результат
     */
    public QTEResult completeQTE(UUID playerId) {
        QTEEvent qte = activeQTEs.remove(playerId);
        if (qte == null) {
            return QTEResult.failure(0f, "No QTE to complete");
        }
        
        if (!qte.isCompleted()) {
            qte.forceExpire();
        }
        
        float score = qte.getFinalScore();
        QTEResult.EfficiencyLevel efficiency = qte.getEfficiencyLevel();
        
        if (efficiency == QTEResult.EfficiencyLevel.FAILED) {
            return QTEResult.failure(score, "QTE failed - combo broken");
        }
        
        return QTEResult.success(score, efficiency, qte.getNextSpell());
    }
    
    /**
     * Отменяет активное QTE (при прерывании)
     */
    public void cancelQTE(UUID playerId) {
        QTEEvent qte = activeQTEs.remove(playerId);
        if (qte != null) {
            qte.forceExpire();
        }
    }
    
    /**
     * Проверяет, есть ли активное QTE у игрока
     */
    public boolean hasActiveQTE(UUID playerId) {
        QTEEvent qte = activeQTEs.get(playerId);
        return qte != null && qte.isActive() && !qte.hasExpired();
    }
    
    /**
     * Получает информацию о текущем QTE
     */
    public QTEEvent getCurrentQTE(UUID playerId) {
        return activeQTEs.get(playerId);
    }
    
    /**
     * Получает прогресс текущего QTE (0.0 - 1.0)
     */
    public float getQTEProgress(UUID playerId) {
        QTEEvent qte = activeQTEs.get(playerId);
        if (qte == null) return 0f;
        
        return (float) qte.getReceivedInputCount() / qte.getExpectedInputCount();
    }
    
    /**
     * Получает оставшееся время QTE
     */
    public long getRemainingTime(UUID playerId) {
        QTEEvent qte = activeQTEs.get(playerId);
        if (qte == null) return 0L;
        
        long elapsed = qte.getElapsedTime();
        return Math.max(0, qte.getType().getTimeWindow() - elapsed);
    }
    
    /**
     * Очищает истекшие QTE
     */
    public void tick() {
        List<UUID> expiredQTEs = new ArrayList<>();
        
        for (Map.Entry<UUID, QTEEvent> entry : activeQTEs.entrySet()) {
            QTEEvent qte = entry.getValue();
            if (qte.hasExpired()) {
                qte.forceExpire();
                expiredQTEs.add(entry.getKey());
            }
        }
        
        // Удаляем истекшие QTE
        for (UUID playerId : expiredQTEs) {
            activeQTEs.remove(playerId);
        }
    }
    
    /**
     * Проверяет, может ли заклинание быть частью комбо
     */
    public boolean canStartCombo(String spellName) {
        return spellComboChains.containsKey(spellName.toLowerCase());
    }
    
    /**
     * Получает возможные следующие заклинания для комбо
     */
    public String[] getAvailableComboSpells(String currentSpell) {
        return spellComboChains.get(currentSpell.toLowerCase());
    }
    
    /**
     * Добавляет новую комбо-цепочку заклинаний
     */
    public void addSpellComboChain(String baseSpell, String... comboSpells) {
        spellComboChains.put(baseSpell.toLowerCase(), comboSpells);
    }
    
    /**
     * Получает статистику QTE для отладки
     */
    public Map<String, Object> getQTEStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeQTEs", activeQTEs.size());
        stats.put("availableComboChains", spellComboChains.size());
        
        if (!activeQTEs.isEmpty()) {
            stats.put("activeQTEDetails", activeQTEs.entrySet().stream()
                .collect(HashMap::new, 
                    (map, entry) -> map.put(entry.getKey().toString(), 
                        Map.of(
                            "type", entry.getValue().getType(),
                            "progress", getQTEProgress(entry.getKey()),
                            "remainingTime", getRemainingTime(entry.getKey())
                        )), 
                    HashMap::putAll));
        }
        
        return stats;
    }
    
    // ============== ИНТЕГРАЦИЯ С ДРУГИМИ СИСТЕМАМИ ==============
    
    /**
     * Интеграция с InterruptionSystem - QTE могут быть прерваны
     */
    public boolean canBeInterrupted(UUID playerId, InterruptionSystem.InterruptionType interruptionType) {
        QTEEvent qte = activeQTEs.get(playerId);
        if (qte == null) return true;
        
        // QTE имеют частичную защиту от слабых прерываний
        return interruptionType.getPriority() >= InterruptionSystem.InterruptionType.MAGICAL_DISRUPTION.getPriority();
    }
    
    /**
     * Принудительно прерывает QTE
     */
    public void forceInterruptQTE(UUID playerId, String reason) {
        QTEEvent qte = activeQTEs.remove(playerId);
        if (qte != null) {
            qte.forceExpire();
        }
    }
    
    /**
     * Вычисляет бонус эффективности от QTE для следующего заклинания
     */
    public float calculateSpellEfficiencyBonus(QTEResult.EfficiencyLevel efficiency) {
        return switch (efficiency) {
            case BONUS -> 1.5f;      // +50% эффективность
            case NORMAL -> 1.0f;     // Базовая эффективность
            case REDUCED -> 0.7f;    // -30% эффективность
            case FAILED -> 0f;       // Комбо прервано
        };
    }
    
    /**
     * Динамически адаптирует сложность QTE на основе производительности игрока
     */
    public QTEType adaptQTEDifficulty(UUID playerId, float recentSuccessRate) {
        if (recentSuccessRate >= 0.8f) {
            return QTEType.COMBO_CHAIN; // Сложные комбо для опытных игроков
        } else if (recentSuccessRate >= 0.6f) {
            return QTEType.RHYTHM_PATTERN; // Средняя сложность
        } else if (recentSuccessRate >= 0.4f) {
            return QTEType.TIMING_SEQUENCE; // Простые последовательности
        } else {
            return QTEType.PRECISION_TARGET; // Самый простой вариант
        }
    }
}