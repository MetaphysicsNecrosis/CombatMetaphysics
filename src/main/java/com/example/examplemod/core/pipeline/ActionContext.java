package com.example.examplemod.core.pipeline;

import com.example.examplemod.core.state.PlayerStateComposition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Контекст выполнения действия в Pipeline
 * Содержит все необходимые данные для обработки действия
 */
public class ActionContext {
    private final UUID id;
    private final ActionEvent event;
    private final PlayerStateComposition state;
    private final Player player;
    private final Level world;
    private final long creationTime;
    
    // Промежуточные данные между этапами pipeline
    private final Map<String, Object> pipelineData = new HashMap<>();
    private boolean cancelled = false;
    private String cancellationReason = "";
    
    public ActionContext(ActionEvent event, PlayerStateComposition state, Player player) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.state = state;
        this.player = player;
        this.world = player.level();
        this.creationTime = System.currentTimeMillis();
    }
    
    // Основные getters
    public UUID getId() { return id; }
    public ActionEvent getEvent() { return event; }
    public PlayerStateComposition getState() { return state; }
    public Player getPlayer() { return player; }
    public Level getWorld() { return world; }
    public long getCreationTime() { return creationTime; }
    public long getAge() { return System.currentTimeMillis() - creationTime; }
    
    // Управление pipeline данными
    public void setPipelineData(String key, Object value) {
        pipelineData.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getPipelineData(String key, Class<T> type) {
        Object value = pipelineData.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
    
    public boolean hasPipelineData(String key) {
        return pipelineData.containsKey(key);
    }
    
    public void removePipelineData(String key) {
        pipelineData.remove(key);
    }
    
    // Отмена действия
    public void cancel(String reason) {
        this.cancelled = true;
        this.cancellationReason = reason;
    }
    
    public boolean isCancelled() { return cancelled; }
    public String getCancellationReason() { return cancellationReason; }
    
    // Удобные методы для часто используемых данных
    public void setModifiedDamage(float damage) {
        setPipelineData("modifiedDamage", damage);
    }
    
    public float getModifiedDamage() {
        Float damage = getPipelineData("modifiedDamage", Float.class);
        return damage != null ? damage : event.getFloatParameter("damage");
    }
    
    public void setModifiedManaCost(float manaCost) {
        setPipelineData("modifiedManaCost", manaCost);
    }
    
    public float getModifiedManaCost() {
        Float cost = getPipelineData("modifiedManaCost", Float.class);
        return cost != null ? cost : event.getFloatParameter("manaCost");
    }
    
    public void setModifiedRange(float range) {
        setPipelineData("modifiedRange", range);
    }
    
    public float getModifiedRange() {
        Float range = getPipelineData("modifiedRange", Float.class);
        return range != null ? range : event.getFloatParameter("range");
    }
    
    // Маркеры для особых состояний
    public void markAsCriticalHit() {
        setPipelineData("isCriticalHit", true);
    }
    
    public boolean isCriticalHit() {
        Boolean critical = getPipelineData("isCriticalHit", Boolean.class);
        return critical != null && critical;
    }
    
    public void markAsBlocked() {
        setPipelineData("isBlocked", true);
    }
    
    public boolean isBlocked() {
        Boolean blocked = getPipelineData("isBlocked", Boolean.class);
        return blocked != null && blocked;
    }
    
    // Debug информация
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("id", id.toString());
        info.put("actionType", event.getActionType());
        info.put("playerId", player.getUUID().toString());
        info.put("age", getAge());
        info.put("cancelled", cancelled);
        info.put("cancellationReason", cancellationReason);
        info.put("pipelineDataKeys", pipelineData.keySet());
        info.put("eventParameters", event.getParameters().keySet());
        info.put("stateCapabilities", state.getActiveCapabilities());
        return info;
    }
    
    /**
     * Очистка ресурсов контекста
     * ВАЖНО: вызывать для предотвращения memory leaks
     */
    public void cleanup() {
        pipelineData.clear();
        // Очищаем другие references если они есть
    }
    
    @Override
    public String toString() {
        return String.format("ActionContext{id=%s, action=%s, player=%s, age=%dms}", 
                id, event.getActionType(), player.getName().getString(), getAge());
    }
}