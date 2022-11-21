package com.matyrobbrt.testframework.impl.packet;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public record C2SChangeStatusPacket(TestFrameworkImpl framework, String testId, Test.Status status) implements TFPacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(testId);
        buf.writeEnum(status.result());
        buf.writeUtf(status.message());
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        final ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (Objects.requireNonNull(player.getServer()).getPlayerList().isOp(player.getGameProfile())) {
            framework.changeStatus(
                    framework.tests().byId(testId).orElseThrow(),
                    status, player
            );
        }
    }

    public static C2SChangeStatusPacket decode(TestFrameworkImpl framework, FriendlyByteBuf buf) {
        return new C2SChangeStatusPacket(framework, buf.readUtf(), new Test.Status(buf.readEnum(Test.Result.class), buf.readUtf()));
    }
}
