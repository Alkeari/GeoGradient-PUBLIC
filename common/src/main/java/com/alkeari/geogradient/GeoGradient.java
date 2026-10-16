package com.alkeari.geogradient;

import com.alkeari.geogradient.command.GeoGradientCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoGradient {
    public static final String MOD_ID = "geogradient";
    public static final Logger LOGGER = LoggerFactory.getLogger("GeoGradient");

    public static void init() {
        GeoGradientCommand.register();
        LOGGER.info("{} loaded.", MOD_ID);
    }
}
