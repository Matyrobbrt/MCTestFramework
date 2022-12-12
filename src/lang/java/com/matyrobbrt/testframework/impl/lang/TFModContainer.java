package com.matyrobbrt.testframework.impl.lang;

import net.minecraftforge.eventbus.EventBusErrorMessage;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.AutomaticEventSubscriber;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TFModContainer extends ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();
    private final IEventBus eventBus;
    private Object mod;
    private Class<?> clazz;

    TFModContainer(IModInfo info) {
        super(info);
        this.mod = new Mod(info.getModId());
        this.eventBus = BusBuilder.builder().setExceptionHandler(this::onEventFailed).setTrackPhases(false).markerType(IModBusEvent.class).useModLauncher().build();
        contextExtension = () -> null;
    }

    TFModContainer(IModInfo info, String clazz, ModuleLayer gameLayer) {
        super(info);
        this.mod = new Mod(info.getModId());
        contextExtension = () -> null;
        this.eventBus = BusBuilder.builder().setExceptionHandler(this::onEventFailed).setTrackPhases(false).markerType(IModBusEvent.class).useModLauncher().build();
        activityMap.put(ModLoadingStage.CONSTRUCT, this::constructMod);

        try {
            var layer = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
            this.clazz = Class.forName(layer, clazz);
        } catch (Throwable e) {
            LOGGER.error("Failed to load class {}", clazz, e);
            throw new ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmodclass", e);
        }
    }

    private void constructMod() {
        try {
            this.mod = clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e, clazz.getName());
        }

        // TODO - EBS
    }


    private void onEventFailed(IEventBus iEventBus, Event event, IEventListener[] iEventListeners, int i, Throwable throwable) {
        LOGGER.error(new EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    @Override
    public boolean matches(Object mod) {
        return this.mod == mod;
    }

    @Override
    public Object getMod() {
        return mod;
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(final T e) {
        try {
            this.eventBus.post(e);
        } catch (Throwable t) {
            LOGGER.error("Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new ModLoadingException(modInfo, modLoadingStage, "fml.modloading.errorduringevent", t);
        }
    }

    public IEventBus getBus() {
        return eventBus;
    }

    private record Mod(String modId) {
    }
}
