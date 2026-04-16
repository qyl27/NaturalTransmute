package com.zg.natural_transmute.common.event;

import com.zg.natural_transmute.common.blocks.entity.GatheringPlatformBlockEntity;
import com.zg.natural_transmute.registry.NTDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.Event;

import java.util.Map;

public class RegisterSpecialBiomeCatalystCraftingConditionEvent extends Event {

    private final Map<Item, Boolean> conditionMaps;
    private final GatheringPlatformBlockEntity blockEntity;

    public RegisterSpecialBiomeCatalystCraftingConditionEvent(
            Map<Item, Boolean> conditionMaps,
            GatheringPlatformBlockEntity blockEntity) {
        this.conditionMaps = conditionMaps;
        this.blockEntity = blockEntity;
    }

    public void register(Item BiomeCatalyst, boolean condition) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(BiomeCatalyst);
        if (this.conditionMaps.containsKey(BiomeCatalyst)) {
            throw new IllegalStateException("Duplicate item key: '" + key + "'");
        } else if (!BiomeCatalyst.components().has(NTDataComponents.ASSOCIATED_BIOMES.get())) {
            throw new IllegalStateException("The item '" + key + "' is not a valid Fu Xiang!");
        } else {
            this.conditionMaps.put(BiomeCatalyst, condition);
        }
    }

    public GatheringPlatformBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

}