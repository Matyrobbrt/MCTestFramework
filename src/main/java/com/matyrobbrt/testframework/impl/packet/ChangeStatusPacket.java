package com.matyrobbrt.testframework.impl.packet;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.conf.Feature;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public record ChangeStatusPacket(TestFrameworkInternal framework, String testId, Test.Status status) implements TFPacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(testId);
        buf.writeEnum(status.result());
        buf.writeUtf(status.message());
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        switch (context.getDirection()) {
            case PLAY_TO_CLIENT -> framework.tests().setStatus(testId, status);
            case PLAY_TO_SERVER -> {
                final ServerPlayer player = Objects.requireNonNull(context.getSender());
                if (framework.configuration().isEnabled(Feature.CLIENT_MODIFICATIONS) && Objects.requireNonNull(player.getServer()).getPlayerList().isOp(player.getGameProfile())) {
                    framework.tests().byId(testId).ifPresent(test -> framework.changeStatus(test, status, player));
                }
            }
        }
    }

    public static ChangeStatusPacket decode(TestFrameworkInternal framework, FriendlyByteBuf buf) {
        return new ChangeStatusPacket(framework, buf.readUtf(), new Test.Status(buf.readEnum(Test.Result.class), buf.readUtf()));
    }
}
