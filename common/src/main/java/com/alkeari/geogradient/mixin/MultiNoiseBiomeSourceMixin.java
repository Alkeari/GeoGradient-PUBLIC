package com.alkeari.geogradient.mixin;

import com.alkeari.geogradient.config.GeoGradientConfig;
import com.alkeari.geogradient.worldgen.GeoGradientClimate;
import com.alkeari.geogradient.worldgen.GeoGradientSampler;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GeoGradient swaps the sampler handed to the biome lookup for one whose temperature and humidity follow latitude,
 * and lets the lookup run: Biolith's replacements and TerraBlender's regions are hooks on this same method and
 * answered nothing when GeoGradient cancelled it. Priority 100 puts the swap ahead of their hooks, which read the
 * sampler argument.
 */
@Mixin(value = MultiNoiseBiomeSource.class, priority = 100)
public abstract class MultiNoiseBiomeSourceMixin {

    @Unique
    private static final boolean TERRABLENDER = classExists("terrablender.worldgen.IExtendedParameterList");

    @Shadow
    protected abstract Climate.ParameterList<Holder<Biome>> parameters();

    @ModifyVariable(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), argsOnly = true)
    private Climate.Sampler geogradient$latitudeSampler(Climate.Sampler sampler) {
        return GeoGradientClimate.isInitialized() ? GeoGradientSampler.wrap(sampler) : sampler;
    }

    /** With use_terrablender_regions off, the lookup skips TerraBlender's regions and reads vanilla's parameter list. */
    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), cancellable = true)
    private void geogradient$vanillaLookup(int i, int j, int k, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!TERRABLENDER || GeoGradientConfig.useTerraBlenderRegions || !GeoGradientClimate.isInitialized()) return;
        cir.setReturnValue(this.parameters().findValue(GeoGradientSampler.wrap(sampler).sample(i, j, k)));
    }

    @Unique
    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, MultiNoiseBiomeSourceMixin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
