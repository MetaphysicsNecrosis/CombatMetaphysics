package com.example.examplemod.core;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PlayerStateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStateMachine.class);
    
    private final UUID playerId;
    private PlayerState currentState;
    private long stateChangeTime;
    private String currentAction;
    
    public PlayerStateMachine(UUID playerId) {
        this.playerId = playerId;
        this.currentState = PlayerState.IDLE;
        this.stateChangeTime = System.currentTimeMillis();
        this.currentAction = "";
    }
    
    public boolean tryTransition(PlayerState newState, String action) {
        if (!currentState.canTransitionTo(newState)) {
            LOGGER.debug("Invalid state transition for player {}: {} -> {}", 
                playerId, currentState, newState);
            return false;
        }
        
        PlayerState oldState = currentState;
        currentState = newState;
        stateChangeTime = System.currentTimeMillis();
        currentAction = action != null ? action : "";
        
        LOGGER.debug("Player {} state transition: {} -> {} (action: {})", 
            playerId, oldState, newState, action);
        
        return true;
    }
    
    public void forceTransition(PlayerState newState, String reason) {
        PlayerState oldState = currentState;
        currentState = newState;
        stateChangeTime = System.currentTimeMillis();
        currentAction = reason != null ? reason : "";
        
        LOGGER.warn("Forced state transition for player {}: {} -> {} (reason: {})", 
            playerId, oldState, newState, reason);
    }
    
    public boolean interrupt(String reason) {
        if (!currentState.canBeInterrupted()) {
            return false;
        }
        
        forceTransition(PlayerState.INTERRUPTED, reason);
        return true;
    }
    
    public PlayerState getCurrentState() {
        return currentState;
    }
    
    public long getStateChangeTime() {
        return stateChangeTime;
    }
    
    public long getTimeInCurrentState() {
        return System.currentTimeMillis() - stateChangeTime;
    }
    
    public String getCurrentAction() {
        return currentAction;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isActive() {
        return currentState.isActive();
    }
    
    public boolean canStartNewAction() {
        return currentState == PlayerState.IDLE;
    }
}