package com.example.examplemod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record QTEResultPacket(
    UUID qteId,
    float score,
    boolean success,
    long completionTime
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<QTEResultPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "qte_result"));
    
    public static final StreamCodec<FriendlyByteBuf, QTEResultPacket> STREAM_CODEC = 
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.qteId());
                buf.writeFloat(packet.score());
                buf.writeBoolean(packet.success());
                buf.writeLong(packet.completionTime());
            },
            buf -> new QTEResultPacket(
                buf.readUUID(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readLong()
            )
        );
    
    @Override
    public CustomPacketPayload.Type<QTEResultPacket> type() {
        return TYPE;
    }
}