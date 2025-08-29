package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.instances.SpellInstance;
import com.example.examplemod.core.spells.instances.SpellState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Collection;

/**
 * Управляет жизненным циклом всех активных заклинаний
 * Thread-safe менеджер с автоматической очисткой завершённых заклинаний
 */
public class SpellLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellLifecycleManager.class);
    private final Map<UUID, SpellInstance> activeSpells = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    
    private static final long CLEANUP_INTERVAL_SECONDS = 30;
    private static final long FINISHED_SPELL_RETENTION_MS = 60000; // 1 minute

    public SpellLifecycleManager() {
        cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "SpellLifecycle-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        startCleanupTask();
    }

    /**
     * Зарегистрировать новое заклинание
     */
    public void registerSpell(SpellInstance spell) {
        activeSpells.put(spell.getId(), spell);
        LOGGER.debug("Registered spell {} in lifecycle manager", spell.getId());
    }

    /**
     * Получить заклинание по ID
     */
    public SpellInstance getSpell(UUID spellId) {
        return activeSpells.get(spellId);
    }

    /**
     * Обновить состояние заклинания
     */
    public boolean updateSpellState(UUID spellId, SpellState newState) {
        SpellInstance spell = activeSpells.get(spellId);
        if (spell == null) {
            LOGGER.warn("Attempted to update state of non-existent spell {}", spellId);
            return false;
        }

        SpellState oldState = spell.getState();
        if (!oldState.canTransitionTo(newState)) {
            LOGGER.warn("Invalid state transition for spell {}: {} -> {}", 
                       spellId, oldState, newState);
            return false;
        }

        if (spell.compareAndSetState(oldState, newState)) {
            spell.updateLastUpdateTime();
            LOGGER.debug("Updated spell {} state: {} -> {}", spellId, oldState, newState);
            return true;
        }

        return false;
    }

    /**
     * Принудительно завершить заклинание
     */
    public void terminateSpell(UUID spellId, SpellState terminalState) {
        if (!terminalState.isTerminal()) {
            throw new IllegalArgumentException("State must be terminal: " + terminalState);
        }

        SpellInstance spell = activeSpells.get(spellId);
        if (spell != null) {
            spell.compareAndSetState(spell.getState(), terminalState);
            spell.updateLastUpdateTime();
            LOGGER.debug("Terminated spell {} with state {}", spellId, terminalState);
        }
    }

    /**
     * Получить все активные заклинания
     */
    public Collection<SpellInstance> getActiveSpells() {
        return activeSpells.values().stream()
                .filter(SpellInstance::isActive)
                .toList();
    }

    /**
     * Получить количество активных заклинаний
     */
    public int getActiveSpellCount() {
        return (int) activeSpells.values().stream()
                .filter(SpellInstance::isActive)
                .count();
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupFinishedSpells, 
                                          CLEANUP_INTERVAL_SECONDS, 
                                          CLEANUP_INTERVAL_SECONDS, 
                                          TimeUnit.SECONDS);
    }

    private void cleanupFinishedSpells() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        var iterator = activeSpells.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            SpellInstance spell = entry.getValue();
            
            if (spell.isFinished() && 
                (currentTime - spell.getLastUpdateTime()) > FINISHED_SPELL_RETENTION_MS) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            LOGGER.debug("Cleaned up {} finished spells", removedCount);
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        activeSpells.clear();
        LOGGER.info("SpellLifecycleManager shut down");
    }
}