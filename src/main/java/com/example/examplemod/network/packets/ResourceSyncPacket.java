package com.example.examplemod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ResourceSyncPacket(
    UUID playerId,
    float currentMana,
    float reservedMana,
    float maxMana,
    float currentStamina,
    float maxStamina
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ResourceSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "resource_sync"));
    
    public static final StreamCodec<FriendlyByteBuf, ResourceSyncPacket> STREAM_CODEC = 
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.playerId());
                buf.writeFloat(packet.currentMana());
                buf.writeFloat(packet.reservedMana());
                buf.writeFloat(packet.maxMana());
                buf.writeFloat(packet.currentStamina());
                buf.writeFloat(packet.maxStamina());
            },
            buf -> new ResourceSyncPacket(
                buf.readUUID(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
            )
        );
    
    @Override
    public CustomPacketPayload.Type<ResourceSyncPacket> type() {
        return TYPE;
    }
}