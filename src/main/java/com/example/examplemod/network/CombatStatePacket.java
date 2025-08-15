package com.example.examplemod.network;

import com.example.examplemod.core.PlayerState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Пакет для синхронизации состояния combat системы между клиентом и сервером
 */
public record CombatStatePacket(
    UUID playerId,
    PlayerState currentState,
    String actionDescription,
    long stateChangeTime,
    float currentMana,
    float maxMana,
    float reservedMana,
    float currentStamina,
    float maxStamina
) implements CustomPacketPayload {
    
    public static final Type<CombatStatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "combat_state")
    );
    
    public static final StreamCodec<ByteBuf, CombatStatePacket> STREAM_CODEC = StreamCodec.composite(
        // UUID как строка
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        CombatStatePacket::playerId,
        
        // PlayerState как строка enum
        ByteBufCodecs.STRING_UTF8.map(PlayerState::valueOf, PlayerState::name),
        CombatStatePacket::currentState,
        
        // Описание действия
        ByteBufCodecs.STRING_UTF8,
        CombatStatePacket::actionDescription,
        
        // Время изменения состояния
        ByteBufCodecs.VAR_LONG,
        CombatStatePacket::stateChangeTime,
        
        // Ресурсы
        ByteBufCodecs.FLOAT,
        CombatStatePacket::currentMana,
        
        ByteBufCodecs.FLOAT,
        CombatStatePacket::maxMana,
        
        ByteBufCodecs.FLOAT,
        CombatStatePacket::reservedMana,
        
        ByteBufCodecs.FLOAT,
        CombatStatePacket::currentStamina,
        
        ByteBufCodecs.FLOAT,
        CombatStatePacket::maxStamina,
        
        CombatStatePacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}