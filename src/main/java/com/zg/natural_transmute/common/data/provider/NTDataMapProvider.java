package com.zg.natural_transmute.common.data.provider;

import com.zg.natural_transmute.registry.NTItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

/** @noinspection deprecation*/
public class NTDataMapProvider extends DataMapProvider {

    public NTDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.addCompostable(0.65F, NTItems.WARPED_WART.get());
    }

    public void addCompostable(float chance, ItemLike item) {
        Builder<Compostable, Item> builder = this.builder(NeoForgeDataMaps.COMPOSTABLES);
        builder.add(item.asItem().builtInRegistryHolder(), new Compostable(chance), false);
    }

}