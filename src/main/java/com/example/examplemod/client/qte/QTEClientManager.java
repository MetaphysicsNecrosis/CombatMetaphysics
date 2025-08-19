package com.example.examplemod.client.qte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SINGLEPLAYER QTE Client Manager с OSU-style визуализацией
 * Управляет активными QTE событиями локально, без сетевой синхронизации
 */
public class QTEClientManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QTEClientManager.class);
    private static QTEClientManager INSTANCE;
    
    // OSU-style QTE система
    private final Map<UUID, OSUStyleQTEEvent> activeQTEs = new ConcurrentHashMap<>();
    private final QTEVisualizer visualizer = new QTEVisualizer();
    private final List<QTEEventListener> listeners = new ArrayList<>();
    
    // Backward compatibility для старой системы
    private final Map<UUID, QTEEvent> activeEvents = new ConcurrentHashMap<>();
    
    private QTEClientManager() {}
    
    public static QTEClientManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new QTEClientManager();
        }
        return INSTANCE;
    }
    
    public void tick() {
        // Обновляем новую OSU-style QTE систему
        tickOSUStyleQTEs();
        
        // Backward compatibility: обновляем старую систему
        tickLegacyQTEs();
    }
    
    /**
     * SINGLEPLAYER: Обновление OSU-style QTE системы
     */
    private void tickOSUStyleQTEs() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, OSUStyleQTEEvent>> iterator = activeQTEs.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, OSUStyleQTEEvent> entry = iterator.next();
            OSUStyleQTEEvent qteEvent = entry.getValue();
            
            // Принудительно истекаем hit points которые пропустили
            for (QTEHitPoint hitPoint : qteEvent.getHitPoints()) {
                hitPoint.forceExpireIfNeeded(currentTime);
            }
            
            // Проверяем завершение QTE
            if (qteEvent.hasExpired() && qteEvent.isActive()) {
                qteEvent.forceComplete();
                notifyOSUQTECompleted(qteEvent);
                LOGGER.debug("OSU QTE {} timed out", entry.getKey());
            }
            
            // Удаляем завершенные QTE
            if (qteEvent.isCompleted()) {
                iterator.remove();
                LOGGER.debug("OSU QTE {} removed (completed)", entry.getKey());
            }
        }
    }
    
    /**
     * Backward compatibility: обновление старой QTE системы
     */
    private void tickLegacyQTEs() {
        Iterator<Map.Entry<UUID, QTEEvent>> iterator = activeEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, QTEEvent> entry = iterator.next();
            QTEEvent event = entry.getValue();
            
            if (event.isExpired() && event.isActive()) {
                event.forceTimeout();
                notifyEventCompleted(event);
                iterator.remove();
                LOGGER.debug("Legacy QTE {} timed out", entry.getKey());
            } else if (event.isCompleted()) {
                iterator.remove();
            }
        }
    }
    
    public void startQTE(UUID id, QTEType type, long duration, List<Integer> expectedKeys, int chainPosition) {
        QTEEvent event = new QTEEvent(id, type, duration, expectedKeys, chainPosition);
        activeEvents.put(id, event);
        
        notifyEventStarted(event);
        
        LOGGER.debug("Started QTE {} of type {} (chain position: {}, duration: {}ms)", 
            id, type, chainPosition, duration);
    }
    
    /**
     * SINGLEPLAYER: Создание нового OSU-style QTE
     */
    public UUID startOSUStyleQTE(OSUStyleQTEEvent.QTEType type, String spellName) {
        UUID qteId = UUID.randomUUID();
        OSUStyleQTEEvent qteEvent = new OSUStyleQTEEvent(qteId, type, spellName);
        
        activeQTEs.put(qteId, qteEvent);
        notifyOSUQTEStarted(qteEvent);
        
        LOGGER.info("Started OSU-style QTE: {} of type {} for spell '{}'", 
                  qteId, type, spellName);
        
        return qteId;
    }
    
    /**
     * SINGLEPLAYER: Обработка нажатий клавиш для OSU-style QTE
     */
    public boolean processKeyInput(int keyCode) {
        boolean handled = false;
        long pressTime = System.currentTimeMillis();
        
        // Обрабатываем новую OSU-style систему
        for (OSUStyleQTEEvent qteEvent : activeQTEs.values()) {
            if (qteEvent.processKeyInput(keyCode, pressTime)) {
                handled = true;
                
                if (qteEvent.isCompleted()) {
                    notifyOSUQTECompleted(qteEvent);
                    LOGGER.debug("OSU QTE {} completed: {}", 
                               qteEvent.getEventId(), qteEvent.getFinalResult());
                }
                break; // Обрабатываем только одно QTE за раз
            }
        }
        
        // Backward compatibility: обрабатываем старую систему
        if (!handled) {
            for (QTEEvent event : activeEvents.values()) {
                if (event.processKeyInput(keyCode)) {
                    handled = true;
                    
                    if (event.isCompleted()) {
                        notifyEventCompleted(event);
                        LOGGER.debug("Legacy QTE {} completed with score: {}", 
                                   event.getId(), event.getScore());
                    }
                }
            }
        }
        
        return handled;
    }
    
    public void cancelQTE(UUID id) {
        QTEEvent event = activeEvents.remove(id);
        if (event != null) {
            event.forceTimeout();
            notifyEventCancelled(event);
            LOGGER.debug("Cancelled QTE {}", id);
        }
    }
    
    public void cancelAllQTE() {
        // Отменяем OSU-style QTE
        for (OSUStyleQTEEvent qteEvent : activeQTEs.values()) {
            qteEvent.forceComplete();
            notifyOSUQTECancelled(qteEvent);
        }
        activeQTEs.clear();
        
        // Legacy QTE
        for (QTEEvent event : activeEvents.values()) {
            event.forceTimeout();
            notifyEventCancelled(event);
        }
        activeEvents.clear();
        LOGGER.debug("Cancelled all QTE events (OSU + Legacy)");
    }
    
    public QTEEvent getActiveQTE(UUID id) {
        return activeEvents.get(id);
    }
    
    public Collection<QTEEvent> getAllActiveQTE() {
        return new ArrayList<>(activeEvents.values());
    }
    
    public boolean hasActiveQTE() {
        return !activeEvents.isEmpty() || !activeQTEs.isEmpty();
    }
    
    public int getActiveQTECount() {
        return activeEvents.size() + activeQTEs.size();
    }
    
    // Event listeners
    public void addListener(QTEEventListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(QTEEventListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyEventStarted(QTEEvent event) {
        for (QTEEventListener listener : listeners) {
            try {
                listener.onQTEStarted(event);
            } catch (Exception e) {
                LOGGER.error("Error in QTE event listener", e);
            }
        }
    }
    
    private void notifyEventCompleted(QTEEvent event) {
        for (QTEEventListener listener : listeners) {
            try {
                listener.onQTECompleted(event);
            } catch (Exception e) {
                LOGGER.error("Error in QTE event listener", e);
            }
        }
    }
    
    private void notifyEventCancelled(QTEEvent event) {
        for (QTEEventListener listener : listeners) {
            try {
                listener.onQTECancelled(event);
            } catch (Exception e) {
                LOGGER.error("Error in QTE event listener", e);
            }
        }
    }
    
    // === OSU-STYLE QTE МЕТОДЫ ===
    
    /**
     * Получает активные OSU-style QTE для отрисовки
     */
    public Collection<OSUStyleQTEEvent> getActiveOSUQTEs() {
        return new ArrayList<>(activeQTEs.values());
    }
    
    /**
     * Получает визуализатор для рендеринга
     */
    public QTEVisualizer getVisualizer() {
        return visualizer;
    }
    
    /**
     * Отменяет OSU-style QTE
     */
    public void cancelOSUQTE(UUID qteId) {
        OSUStyleQTEEvent qteEvent = activeQTEs.remove(qteId);
        if (qteEvent != null) {
            qteEvent.forceComplete();
            notifyOSUQTECancelled(qteEvent);
            LOGGER.debug("Cancelled OSU QTE {}", qteId);
        }
    }
    
    /**
     * Расширенная версия cancelAllQTE для OSU QTE
     */
    public void cancelAllOSUQTE() {
        // Отменяем OSU-style QTE
        for (OSUStyleQTEEvent qteEvent : activeQTEs.values()) {
            qteEvent.forceComplete();
            notifyOSUQTECancelled(qteEvent);
        }
        activeQTEs.clear();
        LOGGER.debug("Cancelled all OSU QTE events");
    }
    
    // === УВЕДОМЛЕНИЯ ДЛЯ OSU-STYLE QTE ===
    
    private void notifyOSUQTEStarted(OSUStyleQTEEvent qteEvent) {
        for (QTEEventListener listener : listeners) {
            try {
                if (listener instanceof OSUQTEEventListener) {
                    ((OSUQTEEventListener) listener).onOSUQTEStarted(qteEvent);
                }
            } catch (Exception e) {
                LOGGER.error("Error in OSU QTE event listener", e);
            }
        }
    }
    
    private void notifyOSUQTECompleted(OSUStyleQTEEvent qteEvent) {
        for (QTEEventListener listener : listeners) {
            try {
                if (listener instanceof OSUQTEEventListener) {
                    ((OSUQTEEventListener) listener).onOSUQTECompleted(qteEvent);
                }
            } catch (Exception e) {
                LOGGER.error("Error in OSU QTE event listener", e);
            }
        }
    }
    
    private void notifyOSUQTECancelled(OSUStyleQTEEvent qteEvent) {
        for (QTEEventListener listener : listeners) {
            try {
                if (listener instanceof OSUQTEEventListener) {
                    ((OSUQTEEventListener) listener).onOSUQTECancelled(qteEvent);
                }
            } catch (Exception e) {
                LOGGER.error("Error in OSU QTE event listener", e);
            }
        }
    }
    
    // === ИНТЕРФЕЙСЫ ДЛЯ LISTENERS ===
    
    public interface QTEEventListener {
        void onQTEStarted(QTEEvent event);
        void onQTECompleted(QTEEvent event);
        void onQTECancelled(QTEEvent event);
    }
    
    /**
     * Расширенный интерфейс для OSU-style QTE событий
     */
    public interface OSUQTEEventListener extends QTEEventListener {
        default void onOSUQTEStarted(OSUStyleQTEEvent qteEvent) {}
        default void onOSUQTECompleted(OSUStyleQTEEvent qteEvent) {}
        default void onOSUQTECancelled(OSUStyleQTEEvent qteEvent) {}
    }
}