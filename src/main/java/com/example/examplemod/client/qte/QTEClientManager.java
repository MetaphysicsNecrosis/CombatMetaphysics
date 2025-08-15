package com.example.examplemod.client.qte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QTEClientManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QTEClientManager.class);
    private static QTEClientManager INSTANCE;
    
    private final Map<UUID, QTEEvent> activeEvents = new ConcurrentHashMap<>();
    private final List<QTEEventListener> listeners = new ArrayList<>();
    
    private QTEClientManager() {}
    
    public static QTEClientManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new QTEClientManager();
        }
        return INSTANCE;
    }
    
    public void tick() {
        // Проверяем истекшие QTE
        Iterator<Map.Entry<UUID, QTEEvent>> iterator = activeEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, QTEEvent> entry = iterator.next();
            QTEEvent event = entry.getValue();
            
            if (event.isExpired() && event.isActive()) {
                event.forceTimeout();
                notifyEventCompleted(event);
                iterator.remove();
                LOGGER.debug("QTE {} timed out", entry.getKey());
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
    
    public boolean processKeyInput(int keyCode) {
        boolean handled = false;
        
        for (QTEEvent event : activeEvents.values()) {
            if (event.processKeyInput(keyCode)) {
                handled = true;
                
                if (event.isCompleted()) {
                    notifyEventCompleted(event);
                    LOGGER.debug("QTE {} completed with score: {}", event.getId(), event.getScore());
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
        for (QTEEvent event : activeEvents.values()) {
            event.forceTimeout();
            notifyEventCancelled(event);
        }
        activeEvents.clear();
        LOGGER.debug("Cancelled all active QTE events");
    }
    
    public QTEEvent getActiveQTE(UUID id) {
        return activeEvents.get(id);
    }
    
    public Collection<QTEEvent> getAllActiveQTE() {
        return new ArrayList<>(activeEvents.values());
    }
    
    public boolean hasActiveQTE() {
        return !activeEvents.isEmpty();
    }
    
    public int getActiveQTECount() {
        return activeEvents.size();
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
    
    public interface QTEEventListener {
        void onQTEStarted(QTEEvent event);
        void onQTECompleted(QTEEvent event);
        void onQTECancelled(QTEEvent event);
    }
}