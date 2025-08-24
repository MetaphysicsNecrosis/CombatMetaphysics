package com.example.examplemod.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);
    
    private final UUID playerId;
    
    // Mana system (двухслойная)
    private float maxMana;
    private float currentMana;
    private float reservedMana;
    private float manaRegenRate;
    private long lastManaUpdate;
    
    // Stamina system
    private float maxStamina;
    private float currentStamina;
    private float staminaRegenRate;
    private long lastStaminaUpdate;
    
    public ResourceManager(UUID playerId, float maxMana, float maxStamina) {
        this.playerId = playerId;
        this.maxMana = maxMana;
        this.currentMana = maxMana;
        this.reservedMana = 0f;
        this.manaRegenRate = 1.0f; // per second
        
        this.maxStamina = maxStamina;
        this.currentStamina = maxStamina;
        this.staminaRegenRate = 2.0f; // per second
        
        long currentTime = System.currentTimeMillis();
        this.lastManaUpdate = currentTime;
        this.lastStaminaUpdate = currentTime;
    }
    
    public void tick() {
        long currentTime = System.currentTimeMillis();
        updateManaRegen(currentTime);
        updateStaminaRegen(currentTime);
    }
    
    private void updateManaRegen(long currentTime) {
        float deltaSeconds = (currentTime - lastManaUpdate) / 1000.0f;
        float availableSpace = maxMana - (currentMana + reservedMana);
        
        if (availableSpace > 0) {
            float regenAmount = manaRegenRate * deltaSeconds;
            currentMana = Math.min(maxMana - reservedMana, currentMana + regenAmount);
        }
        
        lastManaUpdate = currentTime;
    }
    
    private void updateStaminaRegen(long currentTime) {
        float deltaSeconds = (currentTime - lastStaminaUpdate) / 1000.0f;
        
        if (currentStamina < maxStamina) {
            float regenAmount = staminaRegenRate * deltaSeconds;
            currentStamina = Math.min(maxStamina, currentStamina + regenAmount);
        }
        
        lastStaminaUpdate = currentTime;
    }
    
    // Mana operations (двухслойная система)
    public boolean canReserveMana(float amount) {
        return (currentMana >= amount) && (currentMana + reservedMana <= maxMana);
    }
    
    public boolean tryReserveMana(float amount, String reason) {
        if (!canReserveMana(amount)) {
            LOGGER.debug("Cannot reserve {} mana for player {} (available: {}, reserved: {})", 
                amount, playerId, currentMana, reservedMana);
            return false;
        }
        
        reservedMana += amount;
        currentMana -= amount;
        
        LOGGER.debug("Reserved {} mana for player {} (reason: {}, remaining: {}, reserved: {})", 
            amount, playerId, reason, currentMana, reservedMana);
        
        return true;
    }
    
    public void consumeReservedMana(float amount) {
        if (amount > reservedMana) {
            LOGGER.warn("Trying to consume more reserved mana than available for player {} ({} > {})", 
                playerId, amount, reservedMana);
            reservedMana = 0f;
        } else {
            reservedMana -= amount;
        }
        
        LOGGER.debug("Consumed {} reserved mana for player {} (remaining reserved: {})", 
            amount, playerId, reservedMana);
    }
    
    public void releaseReservedMana(float amount) {
        if (amount > reservedMana) {
            LOGGER.warn("Trying to release more reserved mana than available for player {} ({} > {})", 
                playerId, amount, reservedMana);
            amount = reservedMana;
        }
        
        reservedMana -= amount;
        currentMana += amount;
        
        LOGGER.debug("Released {} reserved mana for player {} (current: {}, reserved: {})", 
            amount, playerId, currentMana, reservedMana);
    }
    
    // Compatibility methods for new state machine
    public boolean canSpendMana(int amount) {
        return currentMana >= amount;
    }
    
    public void loseReservedMana(String reason) {
        LOGGER.debug("Lost {} reserved mana for player {} (reason: {})", reservedMana, playerId, reason);
        reservedMana = 0f; // Lose all reserved mana
    }
    
    // Stamina operations
    public boolean canUseStamina(float amount) {
        return currentStamina >= amount;
    }
    
    public boolean tryUseStamina(float amount, String reason) {
        if (!canUseStamina(amount)) {
            LOGGER.debug("Cannot use {} stamina for player {} (available: {})", 
                amount, playerId, currentStamina);
            return false;
        }
        
        currentStamina -= amount;
        
        LOGGER.debug("Used {} stamina for player {} (reason: {}, remaining: {})", 
            amount, playerId, reason, currentStamina);
        
        return true;
    }
    
    public void restoreStamina(float amount) {
        currentStamina = Math.min(maxStamina, currentStamina + amount);
    }
    
    // Getters
    public float getCurrentMana() { return currentMana; }
    public float getReservedMana() { return reservedMana; }
    public float getAvailableMana() { return currentMana; }
    public float getTotalMana() { return currentMana + reservedMana; }
    public float getMaxMana() { return maxMana; }
    
    public float getCurrentStamina() { return currentStamina; }
    public float getMaxStamina() { return maxStamina; }
    
    // ActionResolver compatibility methods
    public boolean hasMana(UUID playerId, float amount) {
        return canReserveMana(amount);
    }
    
    public boolean reserveMana(UUID playerId, float amount) {
        return tryReserveMana(amount, "action resolver");
    }
    
    public boolean hasStamina(UUID playerId, float amount) {
        return canUseStamina(amount);
    }
    
    public float getCurrentStamina(UUID playerId) {
        return getCurrentStamina();
    }
    
    public float getCurrentMana(UUID playerId) {
        return getCurrentMana();
    }
    
    public float getManaPercentage() { return getTotalMana() / maxMana; }
    public float getStaminaPercentage() { return currentStamina / maxStamina; }
    
    // Setters (for synchronization)
    public void setManaRegenRate(float rate) { this.manaRegenRate = rate; }
    public void setStaminaRegenRate(float rate) { this.staminaRegenRate = rate; }
    
    public void syncMana(float current, float reserved) {
        this.currentMana = current;
        this.reservedMana = reserved;
    }
    
    public void syncStamina(float current) {
        this.currentStamina = current;
    }
}