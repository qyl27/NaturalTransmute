package com.zg.natural_transmute.common.event;

import com.zg.natural_transmute.common.blocks.entity.GatheringPlatformBlockEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;

public class NTEventFactory {

    public static void onRegisterSpecialBiomeCatalystCraftingCondition(Map<Item, Boolean> conditionMaps, GatheringPlatformBlockEntity blockEntity) {
        NeoForge.EVENT_BUS.post(new RegisterSpecialBiomeCatalystCraftingConditionEvent(conditionMaps, blockEntity));
    }

}