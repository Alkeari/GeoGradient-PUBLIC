package com.alkeari.geogradient.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import com.alkeari.geogradient.config.fabric.GeoGradientFabricConfig;

public class GeoGradientModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(GeoGradientFabricConfig.class, parent).get();
    }
}
