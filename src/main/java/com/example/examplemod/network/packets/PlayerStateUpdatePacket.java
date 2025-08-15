package com.example.examplemod.network.packets;

import com.example.examplemod.core.PlayerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PlayerStateUpdatePacket(
    UUID playerId,
    PlayerState state,
    String action,
    long stateChangeTime
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PlayerStateUpdatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "player_state_update"));
    
    public static final StreamCodec<FriendlyByteBuf, PlayerStateUpdatePacket> STREAM_CODEC = 
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.playerId());
                buf.writeEnum(packet.state());
                buf.writeUtf(packet.action(), 256);
                buf.writeLong(packet.stateChangeTime());
            },
            buf -> new PlayerStateUpdatePacket(
                buf.readUUID(),
                buf.readEnum(PlayerState.class),
                buf.readUtf(256),
                buf.readLong()
            )
        );
    
    @Override
    public CustomPacketPayload.Type<PlayerStateUpdatePacket> type() {
        return TYPE;
    }
}