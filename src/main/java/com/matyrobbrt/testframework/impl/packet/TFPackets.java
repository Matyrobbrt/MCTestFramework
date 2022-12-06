package com.matyrobbrt.testframework.impl.packet;

import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;

@ApiStatus.Internal
public record TFPackets(SimpleChannel channel, TestFrameworkInternal framework) {
    @SubscribeEvent
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        class Registrar {
            private final SimpleChannel channel;
            int id = 0;

            Registrar(SimpleChannel channel) {
                this.channel = channel;
            }

            <P extends TFPacket> void register(Class<P> pkt, BiFunction<TestFrameworkInternal, FriendlyByteBuf, P> decoder) {
                channel.messageBuilder(pkt, id++)
                        .consumerMainThread((packet, contextSupplier) -> {
                            final var ctx = contextSupplier.get();
                            packet.handle(ctx);
                        })
                        .encoder(TFPacket::encode)
                        .decoder(buf -> decoder.apply(framework, buf))
                        .add();
            }
        }

        final Registrar registrar = new Registrar(channel);
        registrar.register(ChangeStatusPacket.class, ChangeStatusPacket::decode);
        registrar.register(ChangeEnabledPacket.class, ChangeEnabledPacket::decode);
    }
}
