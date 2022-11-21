package com.matyrobbrt.testframework.impl.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface TFPacket {
    void encode(FriendlyByteBuf buf);
    void handle(NetworkEvent.Context context);
}
