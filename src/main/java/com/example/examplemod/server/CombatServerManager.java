package com.example.examplemod.server;

import com.example.examplemod.core.PlayerStateMachine;
import com.example.examplemod.core.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Серверный менеджер для интегрированной combat системы
 * Управляет PlayerStateMachine экземплярами и координирует все подсистемы
 */
public class CombatServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatServerManager.class);
    private static CombatServerManager INSTANCE;
    
    // Интегрированные state machines (содержат все подсистемы)
    private final Map<UUID, PlayerStateMachine> playerStateMachines = new HashMap<>();
    
    // Отдельные resource managers для обратной совместимости с командами
    private final Map<UUID, ResourceManager> standaloneResources = new HashMap<>();
    
    private CombatServerManager() {}
    
    public static CombatServerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CombatServerManager();
        }
        return INSTANCE;
    }
    
    /**
     * Получает интегрированную state machine для игрока
     */
    public PlayerStateMachine getPlayerStateMachine(UUID playerId) {
        return playerStateMachines.computeIfAbsent(playerId, id -> {
            LOGGER.debug("Creating new integrated PlayerStateMachine for player {}", id);
            ResourceManager resourceManager = getPlayerResources(id);
            return PlayerStateMachine.getInstance(id, resourceManager);
        });
    }
    
    /**
     * Получает standalone resource manager (для обратной совместимости)
     */
    public ResourceManager getPlayerResources(UUID playerId) {
        return standaloneResources.computeIfAbsent(playerId, id -> {
            LOGGER.debug("Creating new ResourceManager for player {}", id);
            return new ResourceManager(id, 100f, 100f); // Базовые значения
        });
    }
    
    /**
     * Получает resource manager из интегрированной state machine
     */
    public ResourceManager getIntegratedResources(UUID playerId) {
        PlayerStateMachine stateMachine = getPlayerStateMachine(playerId);
        return stateMachine.getResourceManager();
    }
    
    /**
     * Удаляет игрока из всех систем
     */
    public void removePlayer(UUID playerId) {
        PlayerStateMachine stateMachine = playerStateMachines.remove(playerId);
        if (stateMachine != null) {
            PlayerStateMachine.removeInstance(playerId);
        }
        standaloneResources.remove(playerId);
        LOGGER.debug("Removed player {} from combat system", playerId);
    }
    
    /**
     * Обновляет все системы
     */
    public void tick() {
        // Обновляем интегрированные state machines
        for (PlayerStateMachine stateMachine : playerStateMachines.values()) {
            stateMachine.tick();
        }
        
        // Обновляем standalone resource managers (для обратной совместимости)
        for (ResourceManager resourceManager : standaloneResources.values()) {
            resourceManager.tick();
        }
    }
    
    /**
     * Выводит отладочную информацию о состоянии игрока
     */
    public void debugPrintState(UUID playerId) {
        PlayerStateMachine stateMachine = playerStateMachines.get(playerId);
        ResourceManager resources = standaloneResources.get(playerId);
        
        LOGGER.info("=== Combat Server Debug for {} ===", playerId);
        
        if (stateMachine != null) {
            LOGGER.info("Integrated State Machine:");
            LOGGER.info("  Current State: {} (time: {}ms)", 
                stateMachine.getCurrentState(), stateMachine.getTimeInCurrentState());
            LOGGER.info("  Current Action: {}", stateMachine.getCurrentAction());
            LOGGER.info("  Combat Type: {}", stateMachine.getCurrentState().getCombatType());
            
            // Информация о ресурсах из state machine
            ResourceManager integratedResources = stateMachine.getResourceManager();
            LOGGER.info("  Integrated Resources:");
            LOGGER.info("    Mana: {}/{} (reserved: {})", 
                integratedResources.getCurrentMana(), integratedResources.getMaxMana(), 
                integratedResources.getReservedMana());
            LOGGER.info("    Stamina: {}/{}", 
                integratedResources.getCurrentStamina(), integratedResources.getMaxStamina());
                
            // Информация о подсистемах
            if (stateMachine.getAttackSystem().isCharging(playerId)) {
                LOGGER.info("  Attack System: Charging attack");
                var attackData = stateMachine.getAttackSystem().getCurrentAttack(playerId);
                if (attackData != null) {
                    LOGGER.info("    Direction: {}, Charge Time: {}ms", 
                        attackData.getDirection(), attackData.getChargeDuration());
                }
            }
            
            if (stateMachine.getDefenseSystem().isDefending(playerId)) {
                LOGGER.info("  Defense System: Active defense");
                var defenseData = stateMachine.getDefenseSystem().getCurrentDefense(playerId);
                if (defenseData != null) {
                    LOGGER.info("    Type: {}, Remaining: {}ms", 
                        defenseData.getType(), stateMachine.getDefenseSystem().getRemainingTime(playerId));
                }
            }
            
            if (stateMachine.getInterruptionSystem().isInterrupted(playerId)) {
                LOGGER.info("  Interruption System: Player is interrupted");
                var interruption = stateMachine.getInterruptionSystem().getActiveInterruption(playerId);
                if (interruption != null) {
                    LOGGER.info("    Type: {}, Reason: {}", 
                        interruption.getType(), interruption.getReason());
                }
            }
        } else {
            LOGGER.info("No integrated state machine found");
        }
        
        if (resources != null) {
            LOGGER.info("Standalone Resources:");
            LOGGER.info("  Mana: {}/{} (reserved: {})", 
                resources.getCurrentMana(), resources.getMaxMana(), resources.getReservedMana());
            LOGGER.info("  Stamina: {}/{}", 
                resources.getCurrentStamina(), resources.getMaxStamina());
        } else {
            LOGGER.info("No standalone resource manager found");
        }
    }
    
    /**
     * Получает количество активных игроков в combat системе
     */
    public int getActivePlayersCount() {
        return playerStateMachines.size();
    }
    
    /**
     * Получает статистику состояний игроков
     */
    public Map<String, Integer> getStateStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        for (PlayerStateMachine stateMachine : playerStateMachines.values()) {
            String state = stateMachine.getCurrentState().name();
            stats.merge(state, 1, Integer::sum);
        }
        
        return stats;
    }
    
    /**
     * Принудительно сбрасывает всех игроков в IDLE состояние
     */
    public void resetAllPlayers(String reason) {
        LOGGER.warn("Resetting all {} players to IDLE state. Reason: {}", 
            playerStateMachines.size(), reason);
            
        for (PlayerStateMachine stateMachine : playerStateMachines.values()) {
            stateMachine.forceTransition(com.example.examplemod.core.PlayerState.IDLE, 
                "Server reset: " + reason);
        }
    }
}