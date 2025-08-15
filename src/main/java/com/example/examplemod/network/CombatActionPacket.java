package com.example.examplemod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Пакет для отправки combat действий от клиента к серверу
 */
public record CombatActionPacket(
    UUID playerId,
    ActionType actionType,
    String actionData,
    long timestamp
) implements CustomPacketPayload {
    
    public enum ActionType {
        MAGIC_PREPARE,
        MAGIC_CAST,
        MELEE_START,
        MELEE_EXECUTE,
        MELEE_DIRECTION_CHANGE,
        DEFENSE_ACTIVATE,
        ACTION_CANCEL,
        INTERRUPT_REQUEST
    }
    
    public static final Type<CombatActionPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "combat_action")
    );
    
    public static final StreamCodec<ByteBuf, CombatActionPacket> STREAM_CODEC = StreamCodec.composite(
        // UUID как строка
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        CombatActionPacket::playerId,
        
        // ActionType как строка enum
        ByteBufCodecs.STRING_UTF8.map(ActionType::valueOf, ActionType::name),
        CombatActionPacket::actionType,
        
        // Данные действия (JSON или простая строка)
        ByteBufCodecs.STRING_UTF8,
        CombatActionPacket::actionData,
        
        // Временная метка
        ByteBufCodecs.VAR_LONG,
        CombatActionPacket::timestamp,
        
        CombatActionPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    // Фабричные методы для создания различных типов пакетов
    
    public static CombatActionPacket createMagicPrepare(UUID playerId, String spellName, float manaCost) {
        return new CombatActionPacket(playerId, ActionType.MAGIC_PREPARE, 
            String.format("{\"spell\":\"%s\",\"cost\":%.1f}", spellName, manaCost),
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createMagicCast(UUID playerId, String spellName) {
        return new CombatActionPacket(playerId, ActionType.MAGIC_CAST,
            String.format("{\"spell\":\"%s\"}", spellName),
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createMeleeStart(UUID playerId, String direction) {
        return new CombatActionPacket(playerId, ActionType.MELEE_START,
            String.format("{\"direction\":\"%s\"}", direction),
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createMeleeExecute(UUID playerId) {
        return new CombatActionPacket(playerId, ActionType.MELEE_EXECUTE,
            "{}",
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createMeleeDirectionChange(UUID playerId, String newDirection) {
        return new CombatActionPacket(playerId, ActionType.MELEE_DIRECTION_CHANGE,
            String.format("{\"direction\":\"%s\"}", newDirection),
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createDefenseActivate(UUID playerId, String defenseType) {
        return new CombatActionPacket(playerId, ActionType.DEFENSE_ACTIVATE,
            String.format("{\"type\":\"%s\"}", defenseType),
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createActionCancel(UUID playerId, String reason) {
        return new CombatActionPacket(playerId, ActionType.ACTION_CANCEL,
            String.format("{\"reason\":\"%s\"}", reason),
            System.currentTimeMillis());
    }
    
    public static CombatActionPacket createInterruptRequest(UUID playerId, String interruptType, String reason) {
        return new CombatActionPacket(playerId, ActionType.INTERRUPT_REQUEST,
            String.format("{\"type\":\"%s\",\"reason\":\"%s\"}", interruptType, reason),
            System.currentTimeMillis());
    }
}