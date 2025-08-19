package com.example.examplemod.core.pipeline;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Событие действия в Action Pipeline
 * Содержит всю информацию для обработки действия
 */
public class ActionEvent {
    private final UUID id;
    private final String actionType;
    private final UUID playerId;
    private final long timestamp;
    private final Map<String, Object> parameters;
    private final EventSource source;
    
    private ActionEvent(Builder builder) {
        this.id = UUID.randomUUID();
        this.actionType = builder.actionType;
        this.playerId = builder.playerId;
        this.timestamp = System.currentTimeMillis();
        this.parameters = new HashMap<>(builder.parameters);
        this.source = builder.source;
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getActionType() { return actionType; }
    public UUID getPlayerId() { return playerId; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
    public EventSource getSource() { return source; }
    
    // Удобные методы для получения параметров
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
    
    public String getStringParameter(String key) {
        return getParameter(key, String.class);
    }
    
    public Integer getIntParameter(String key) {
        return getParameter(key, Integer.class);
    }
    
    public Float getFloatParameter(String key) {
        return getParameter(key, Float.class);
    }
    
    public Boolean getBooleanParameter(String key) {
        return getParameter(key, Boolean.class);
    }
    
    public BlockPos getBlockPosParameter(String key) {
        return getParameter(key, BlockPos.class);
    }
    
    public Entity getEntityParameter(String key) {
        return getParameter(key, Entity.class);
    }
    
    // Builder pattern
    public static class Builder {
        private String actionType;
        private UUID playerId;
        private final Map<String, Object> parameters = new HashMap<>();
        private EventSource source = EventSource.UNKNOWN;
        
        public Builder(String actionType, UUID playerId) {
            this.actionType = actionType;
            this.playerId = playerId;
        }
        
        public Builder(String actionType, Player player) {
            this.actionType = actionType;
            this.playerId = player.getUUID();
        }
        
        public Builder parameter(String key, Object value) {
            parameters.put(key, value);
            return this;
        }
        
        public Builder source(EventSource source) {
            this.source = source;
            return this;
        }
        
        // Удобные методы для частых параметров
        public Builder target(Entity entity) {
            return parameter("target", entity);
        }
        
        public Builder position(BlockPos pos) {
            return parameter("position", pos);
        }
        
        public Builder damage(float damage) {
            return parameter("damage", damage);
        }
        
        public Builder range(float range) {
            return parameter("range", range);
        }
        
        public Builder spellId(String spellId) {
            return parameter("spellId", spellId);
        }
        
        public Builder manaCost(float manaCost) {
            return parameter("manaCost", manaCost);
        }
        
        public ActionEvent build() {
            if (actionType == null || playerId == null) {
                throw new IllegalStateException("ActionType and PlayerId are required");
            }
            return new ActionEvent(this);
        }
    }
    
    // Источник события
    public enum EventSource {
        PLAYER_INPUT,    // Действие игрока
        SERVER_COMMAND,  // Серверная команда
        MOD_API,        // Вызов через API мода
        AUTOMATIC,      // Автоматическое действие (таймер, триггер)
        NETWORK,        // Сетевой пакет
        UNKNOWN
    }
    
    // Предопределенные типы действий
    public static class ActionTypes {
        // Магические действия
        public static final String SPELL_CAST = "spell_cast";
        public static final String SPELL_PREPARE = "spell_prepare";
        public static final String SPELL_CHANNEL = "spell_channel";
        public static final String QTE_START = "qte_start";
        
        // Боевые действия  
        public static final String MELEE_ATTACK = "melee_attack";
        public static final String MELEE_PREPARE = "melee_prepare";
        public static final String MELEE_CHARGE = "melee_charge";
        
        // Защитные действия
        public static final String BLOCK_START = "block_start";
        public static final String PARRY_ATTEMPT = "parry_attempt";
        public static final String DODGE_ROLL = "dodge_roll";
        
        // Прерывания
        public static final String INTERRUPT = "interrupt";
        public static final String STUN = "stun";
        public static final String FORCE_STOP = "force_stop";
        
        // Системные действия
        public static final String STATE_RESET = "state_reset";
        public static final String CLEANUP = "cleanup";
    }
    
    // Фабричные методы для частых событий
    public static ActionEvent spellCast(Player player, String spellId, float manaCost) {
        return new Builder(ActionTypes.SPELL_CAST, player)
                .spellId(spellId)
                .manaCost(manaCost)
                .source(EventSource.PLAYER_INPUT)
                .build();
    }
    
    public static ActionEvent meleeAttack(Player player, Entity target, float damage) {
        return new Builder(ActionTypes.MELEE_ATTACK, player)
                .target(target)
                .damage(damage)
                .source(EventSource.PLAYER_INPUT)
                .build();
    }
    
    public static ActionEvent interrupt(Player player, String reason) {
        return new Builder(ActionTypes.INTERRUPT, player)
                .parameter("reason", reason)
                .source(EventSource.AUTOMATIC)
                .build();
    }
    
    public static ActionEvent qteStart(Player player, String qteType) {
        return new Builder(ActionTypes.QTE_START, player)
                .parameter("qteType", qteType)
                .source(EventSource.MOD_API)
                .build();
    }
    
    @Override
    public String toString() {
        return String.format("ActionEvent{type=%s, player=%s, params=%s}", 
                actionType, playerId, parameters.keySet());
    }
}