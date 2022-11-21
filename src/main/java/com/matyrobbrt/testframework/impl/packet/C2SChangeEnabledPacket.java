package com.matyrobbrt.testframework.impl.packet;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public record C2SChangeEnabledPacket(TestFrameworkImpl framework, String testId, boolean enabled) implements TFPacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(testId);
        buf.writeBoolean(enabled);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        final ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (Objects.requireNonNull(player.getServer()).getPlayerList().isOp(player.getGameProfile())) {
            framework.setEnabled(
                    framework.tests().byId(testId).orElseThrow(),
                    enabled, player
            );
        }
    }

    public static C2SChangeEnabledPacket decode(TestFrameworkImpl framework, FriendlyByteBuf buf) {
        return new C2SChangeEnabledPacket(framework, buf.readUtf(), buf.readBoolean());
    }
}
