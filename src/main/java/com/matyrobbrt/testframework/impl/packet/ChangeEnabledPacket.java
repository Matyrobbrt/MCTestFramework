package com.matyrobbrt.testframework.impl.packet;

import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Consumer;

public record ChangeEnabledPacket(TestFrameworkInternal framework, String testId, boolean enabled) implements TFPacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(testId);
        buf.writeBoolean(enabled);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        switch (context.getDirection()) {
            case PLAY_TO_CLIENT -> {
                final Consumer<String> enablerer = enabled ? id -> framework.tests().enable(id) : id -> framework.tests().disable(id);
                enablerer.accept(testId);
            }
            case PLAY_TO_SERVER -> {
                final ServerPlayer player = Objects.requireNonNull(context.getSender());
                if (framework.configuration().modifiableByClients() && Objects.requireNonNull(player.getServer()).getPlayerList().isOp(player.getGameProfile())) {
                    framework.setEnabled(
                            framework.tests().byId(testId).orElseThrow(),
                            enabled, player
                    );
                }
            }
        }
    }

    public static ChangeEnabledPacket decode(TestFrameworkInternal framework, FriendlyByteBuf buf) {
        return new ChangeEnabledPacket(framework, buf.readUtf(), buf.readBoolean());
    }
}
