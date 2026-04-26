package com.zg.natural_transmute.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.zg.natural_transmute.registry.NTItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class HarmoniousChangeFuelUtils {
    private static final Map<Predicate<ItemStack>, Integer> fuels = new HashMap<>();
    private static final List<Item> fuelsItemView = new ArrayList<>();

    public static Map<Predicate<ItemStack>, Integer> getFuels() {
        return ImmutableMap.copyOf(fuels);
    }

    public static List<Item> getFuelsItemView() {
        return ImmutableList.copyOf(fuelsItemView);
    }

    public static boolean isFuel(ItemStack stack) {
        for (var entry : fuels.entrySet()) {
            if (entry.getKey().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static Integer getFuel(ItemStack stack) {
        for (var entry : fuels.entrySet()) {
            if (entry.getKey().test(stack)) {
                return entry.getValue();
            }
        }
        return null;
    }

    static {
        rebuildFuels();
    }

    public static void rebuildFuels() {
        fuels.clear();
        fuelsItemView.clear();

        add(NTItems.HARMONIOUS_CHANGE_FUEL.get(), 8);
        add(NTItems.ETERNAL_HARMONIOUS_CHANGE_LAVA_BUCKET.get(), Integer.MAX_VALUE);
    }

    private static void add(ItemLike itemLike, int fuelDuration) {
        var item = itemLike.asItem();
        add(stack -> stack.is(item), fuelDuration);
        fuelsItemView.add(item);
    }

    private static void add(TagKey<Item> tag, int fuelDuration) {
        add(stack -> stack.is(tag), fuelDuration);
        // Todo: add all items in tag to fuelsItemView
    }

    private static void add(Predicate<ItemStack> predicate, int fuelDuration) {
        fuels.put(predicate, fuelDuration);
    }

    public static boolean isEternalFuel(ItemStack stack) {
        return stack.is(NTItems.ETERNAL_HARMONIOUS_CHANGE_LAVA_BUCKET.get());
    }
}
