package com.alkeari.geogradient;

import com.alkeari.geogradient.command.GeoGradientCommand;
import com.alkeari.modtrix.api.Events;
import com.alkeari.modtrix.api.Updates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class GeoGradient {
    public static final String MOD_ID = "geogradient";
    public static final Logger LOGGER = LoggerFactory.getLogger("GeoGradient");

    public static final String UPDATE_URL = "https://alkeari.com/updates/geogradient.json";
    private static final AtomicBoolean UPDATE_CHECK_DECIDED = new AtomicBoolean();

    /** {@code checkForUpdates} is read on the first tick, since Forge and NeoForge load their config only after mod construction. */
    public static void init(BooleanSupplier checkForUpdates) {
        GeoGradientCommand.register();
        Runnable decide = () -> {
            if (UPDATE_CHECK_DECIDED.compareAndSet(false, true) && checkForUpdates.getAsBoolean()) {
                Updates.check(MOD_ID, UPDATE_URL);
            }
        };
        Events.onClientTick(decide);
        Events.onServerTick(server -> decide.run());
        LOGGER.info("{} loaded.", MOD_ID);
    }
}
