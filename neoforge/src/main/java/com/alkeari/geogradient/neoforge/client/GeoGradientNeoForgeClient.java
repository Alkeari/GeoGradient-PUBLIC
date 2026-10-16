package com.alkeari.geogradient.neoforge.client;

import net.neoforged.fml.ModContainer;
//? if >=1.21 {
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//?}

/** Client-only, so a dedicated server never loads the screen classes it references. */
public final class GeoGradientNeoForgeClient {

    private GeoGradientNeoForgeClient() {}

    public static void registerConfigScreen(ModContainer modContainer) {
        // NeoForge's built-in config screen arrived with 21.0; earlier versions edit the file only.
        //? if >=1.21 {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        //?}
    }
}
