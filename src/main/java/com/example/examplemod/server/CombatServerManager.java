package com.example.examplemod.server;

import com.example.examplemod.core.PlayerStateMachine;
import com.example.examplemod.core.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatServerManager.class);
    private static CombatServerManager INSTANCE;
    
    private final Map<UUID, PlayerStateMachine> playerStates = new HashMap<>();
    private final Map<UUID, ResourceManager> playerResources = new HashMap<>();
    
    private CombatServerManager() {}
    
    public static CombatServerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CombatServerManager();
        }
        return INSTANCE;
    }
    
    public PlayerStateMachine getPlayerState(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, PlayerStateMachine::new);
    }
    
    public ResourceManager getPlayerResources(UUID playerId) {
        return playerResources.computeIfAbsent(playerId, id -> {
            LOGGER.debug("Creating new ResourceManager for player {}", id);
            return new ResourceManager(id, 100f, 100f); // Базовые значения
        });
    }
    
    public void removePlayer(UUID playerId) {
        playerStates.remove(playerId);
        playerResources.remove(playerId);
        LOGGER.debug("Removed player {} from combat system", playerId);
    }
    
    public void tick() {
        // Обновляем ресурсы всех игроков
        for (ResourceManager resourceManager : playerResources.values()) {
            resourceManager.tick();
        }
    }
    
    public void debugPrintState(UUID playerId) {
        PlayerStateMachine state = playerStates.get(playerId);
        ResourceManager resources = playerResources.get(playerId);
        
        LOGGER.info("=== Combat Server Debug for {} ===", playerId);
        if (state != null) {
            LOGGER.info("State: {} (time in state: {}ms)", 
                state.getCurrentState(), state.getTimeInCurrentState());
        } else {
            LOGGER.info("No state machine found");
        }
        if (resources != null) {
            LOGGER.info("Mana: {}/{} (reserved: {})", 
                resources.getCurrentMana(), resources.getMaxMana(), resources.getReservedMana());
            LOGGER.info("Stamina: {}/{}", 
                resources.getCurrentStamina(), resources.getMaxStamina());
        } else {
            LOGGER.info("No resource manager found");
        }
    }
}