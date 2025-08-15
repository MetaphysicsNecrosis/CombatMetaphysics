package com.example.examplemod.network.packets;

import com.example.examplemod.client.qte.QTEType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record QTEStartPacket(
    UUID qteId,
    QTEType qteType,
    long duration,
    List<Integer> expectedKeys,
    int chainPosition
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<QTEStartPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "qte_start"));
    
    public static final StreamCodec<FriendlyByteBuf, QTEStartPacket> STREAM_CODEC = 
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.qteId());
                buf.writeVarInt(packet.qteType().ordinal());
                buf.writeLong(packet.duration());
                buf.writeVarInt(packet.expectedKeys().size());
                for (Integer key : packet.expectedKeys()) {
                    buf.writeVarInt(key);
                }
                buf.writeVarInt(packet.chainPosition());
            },
            buf -> {
                UUID qteId = buf.readUUID();
                QTEType type = QTEType.values()[buf.readVarInt()];
                long duration = buf.readLong();
                int size = buf.readVarInt();
                List<Integer> keys = new java.util.ArrayList<>();
                for (int i = 0; i < size; i++) {
                    keys.add(buf.readVarInt());
                }
                int chainPosition = buf.readVarInt();
                return new QTEStartPacket(qteId, type, duration, keys, chainPosition);
            }
        );
    
    @Override
    public CustomPacketPayload.Type<QTEStartPacket> type() {
        return TYPE;
    }
}