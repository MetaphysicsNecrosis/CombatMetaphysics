package com.example.examplemod.client.qte;

import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * OSU-style QTE Event с множественными hit points и точными timing windows
 * SINGLEPLAYER: Вся логика выполняется локально без сетевой синхронизации
 */
public class OSUStyleQTEEvent {
    
    private final UUID eventId;
    private final QTEType type;
    private final String spellName;              // Имя заклинания для магических комбо
    private final long startTime;               // Время начала QTE
    private final List<QTEHitPoint> hitPoints;  // Точки попадания OSU-style
    
    // Состояние события
    private boolean isActive = true;
    private boolean isCompleted = false;
    private QTEResult finalResult = null;
    
    // Статистика
    private int perfectHits = 0;
    private int greatHits = 0;
    private int goodHits = 0;
    private int okHits = 0;
    private int missedHits = 0;
    
    public enum QTEType {
        SEQUENCE,       // Последовательные нажатия (W -> A -> S -> D)
        TIMING,         // Одно точное нажатие в момент совпадения кругов
        RAPID,          // Быстрые множественные нажатия одной клавиши
        PRECISION       // Очень точное попадание в узкое окно
    }
    
    public OSUStyleQTEEvent(UUID eventId, QTEType type, String spellName) {
        this.eventId = eventId;
        this.type = type;
        this.spellName = spellName;
        this.startTime = System.currentTimeMillis();
        this.hitPoints = generateHitPoints(type);
        // totalDuration вычисляется динамически через метод getTotalDuration()
        
        LOGGER.debug("Created OSU-style QTE: {} of type {} with {} hit points", 
                   eventId, type, hitPoints.size());
    }
    
    /**
     * Генерирует hit points в зависимости от типа QTE
     */
    private List<QTEHitPoint> generateHitPoints(QTEType type) {
        List<QTEHitPoint> points = new ArrayList<>();
        long baseTime = startTime + 2000; // Начинаем через 2 секунды (время для приближения первого круга)
        
        switch (type) {
            case SEQUENCE -> {
                // Последовательность из 4 нажатий с интервалом 800ms
                int[] keys = {GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D};
                for (int i = 0; i < keys.length; i++) {
                    long targetTime = baseTime + i * 800;
                    points.add(new QTEHitPoint(targetTime, keys[i]));
                }
            }
            case TIMING -> {
                // Одно точное нажатие
                points.add(new QTEHitPoint(baseTime, GLFW.GLFW_KEY_SPACE));
            }
            case RAPID -> {
                // 5 быстрых нажатий с интервалом 300ms
                for (int i = 0; i < 5; i++) {
                    long targetTime = baseTime + i * 300;
                    points.add(new QTEHitPoint(targetTime, GLFW.GLFW_KEY_SPACE));
                }
            }
            case PRECISION -> {
                // 3 точных нажатия с увеличивающейся сложностью
                int[] keys = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R};
                for (int i = 0; i < keys.length; i++) {
                    long targetTime = baseTime + i * 1200; // Больше времени между точными попаданиями
                    points.add(new QTEHitPoint(targetTime, keys[i]));
                }
            }
        }
        
        return points;
    }
    
    /**
     * SINGLEPLAYER: Обрабатывает нажатие клавиши локально
     * @param keyCode код нажатой клавиши
     * @param pressTime время нажатия
     * @return true если input был обработан
     */
    public boolean processKeyInput(int keyCode, long pressTime) {
        if (!isActive || isCompleted) {
            return false;
        }
        
        boolean inputProcessed = false;
        
        // Обрабатываем все ожидающие hit points
        for (QTEHitPoint hitPoint : hitPoints) {
            if (hitPoint.getResult() == QTEHitPoint.HitResult.WAITING) {
                QTEHitPoint.HitResult result = hitPoint.processInput(pressTime, keyCode);
                
                if (result != QTEHitPoint.HitResult.WAITING) {
                    inputProcessed = true;
                    updateStatistics(result);
                    
                    LOGGER.debug("QTE {} hit processed: {} (efficiency: {:.2f})", 
                               eventId, result, hitPoint.getEfficiency());
                    break; // Обрабатываем только одно попадание за раз
                }
            }
        }
        
        // Проверяем завершение QTE
        checkCompletion();
        
        return inputProcessed;
    }
    
    /**
     * Обновляет статистику попаданий
     */
    private void updateStatistics(QTEHitPoint.HitResult result) {
        switch (result) {
            case PERFECT -> perfectHits++;
            case GREAT -> greatHits++;
            case GOOD -> goodHits++;
            case OK -> okHits++;
            case MISS, TOO_EARLY -> missedHits++;
        }
    }
    
    /**
     * Проверяет завершение QTE и вычисляет финальный результат
     */
    private void checkCompletion() {
        // Проверяем, все ли hit points обработаны
        boolean allProcessed = hitPoints.stream()
                .allMatch(QTEHitPoint::isProcessed);
        
        if (allProcessed || hasExpired()) {
            completeQTE();
        }
    }
    
    /**
     * Завершает QTE и вычисляет финальный результат
     */
    private void completeQTE() {
        if (isCompleted) return;
        
        this.isCompleted = true;
        this.isActive = false;
        
        // Принудительно истекаем все необработанные hit points
        long currentTime = System.currentTimeMillis();
        for (QTEHitPoint hitPoint : hitPoints) {
            hitPoint.forceExpireIfNeeded(currentTime);
            if (hitPoint.getResult() == QTEHitPoint.HitResult.MISS && 
                !hitPoint.isProcessed()) {
                missedHits++;
            }
        }
        
        this.finalResult = calculateFinalResult();
        
        LOGGER.info("QTE {} completed: {} (P:{} G:{} G:{} O:{} M:{})", 
                  eventId, finalResult.getEfficiency(), 
                  perfectHits, greatHits, goodHits, okHits, missedHits);
    }
    
    /**
     * Вычисляет финальный результат QTE на основе статистики попаданий
     */
    private QTEResult calculateFinalResult() {
        int totalHits = hitPoints.size();
        if (totalHits == 0) {
            return QTEResult.failure(0f, "No hit points");
        }
        
        // Weighted scoring: PERFECT=100, GREAT=90, GOOD=70, OK=50, MISS=0
        float totalScore = perfectHits * 100f + greatHits * 90f + 
                          goodHits * 70f + okHits * 50f;
        float maxPossibleScore = totalHits * 100f;
        float finalScore = totalScore / maxPossibleScore;
        
        // Бонус за полное комбо (без промахов)
        boolean fullCombo = missedHits == 0;
        if (fullCombo && totalHits > 1) {
            finalScore *= 1.2f; // +20% за полное комбо
            finalScore = Math.min(1.0f, finalScore);
        }
        
        // Определяем уровень эффективности
        QTEResult.EfficiencyLevel efficiency;
        if (finalScore >= 0.9f) {
            efficiency = QTEResult.EfficiencyLevel.BONUS;
        } else if (finalScore >= 0.7f) {
            efficiency = QTEResult.EfficiencyLevel.NORMAL;
        } else if (finalScore >= 0.5f) {
            efficiency = QTEResult.EfficiencyLevel.REDUCED;
        } else {
            efficiency = QTEResult.EfficiencyLevel.FAILED;
        }
        
        String message = String.format("Score: %.1f%% - %s %s", 
                                     finalScore * 100, 
                                     efficiency.getDescription(),
                                     fullCombo ? "(FULL COMBO!)" : "");
        
        return new QTEResult(finalScore >= 0.5f, finalScore, efficiency, 
                           spellName, efficiency.getMultiplier(), message);
    }
    
    /**
     * Проверяет, истекло ли время QTE
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() > startTime + getTotalDuration();
    }
    
    /**
     * Принудительно завершает QTE (при прерывании)
     */
    public void forceComplete() {
        if (!isCompleted) {
            completeQTE();
        }
    }
    
    /**
     * Получает прогресс QTE (0.0 - 1.0)
     */
    public float getProgress() {
        if (hitPoints.isEmpty()) return 1.0f;
        
        int processedCount = (int) hitPoints.stream()
                .mapToInt(hp -> hp.isProcessed() ? 1 : 0)
                .sum();
        
        return (float) processedCount / hitPoints.size();
    }
    
    /**
     * Возвращает следующий ожидающий hit point для preview
     */
    public QTEHitPoint getNextWaitingHitPoint() {
        long currentTime = System.currentTimeMillis();
        return hitPoints.stream()
                .filter(hp -> hp.getResult() == QTEHitPoint.HitResult.WAITING)
                .filter(hp -> currentTime >= hp.getTargetTime() - 2000) // Показываем за 2 сек до попадания
                .findFirst()
                .orElse(null);
    }
    
    // === ГЕТТЕРЫ ===
    
    public UUID getEventId() { return eventId; }
    public QTEType getType() { return type; }
    public QTEType getQTEType() { return type; } // Alias для CombatClientManager
    public String getSpellName() { return spellName; }
    public long getStartTime() { return startTime; }
    public List<QTEHitPoint> getHitPoints() { return new ArrayList<>(hitPoints); }
    public boolean isActive() { return isActive; }
    public boolean isCompleted() { return isCompleted; }
    public QTEResult getFinalResult() { return finalResult; }
    
    
    /**
     * Возвращает общую продолжительность QTE
     */
    public long getTotalDuration() {
        if (hitPoints.isEmpty()) return 5000; // Базовая длительность
        
        long lastHitTime = hitPoints.get(hitPoints.size() - 1).getTargetTime();
        return lastHitTime - startTime + 1000; // +1 секунда после последнего hit
    }
    
    public int getPerfectHits() { return perfectHits; }
    public int getGreatHits() { return greatHits; }
    public int getGoodHits() { return goodHits; }
    public int getOkHits() { return okHits; }
    public int getMissedHits() { return missedHits; }
    
    private static final org.slf4j.Logger LOGGER = 
            org.slf4j.LoggerFactory.getLogger(OSUStyleQTEEvent.class);
}