package com.zg.natural_transmute.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zg.natural_transmute.registry.NTItems;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.*;
import java.util.function.Predicate;

public class HarmoniousChangeFuelUtils {
    private static final Set<Predicate<ItemStack>> fuels = new HashSet<>();
    private static final List<Item> fuelsItemView = new ArrayList<>();

    public static Set<Predicate<ItemStack>> getFuels() {
        return ImmutableSet.copyOf(fuels);
    }

    public static List<Item> getFuelsItemView() {
        return ImmutableList.copyOf(fuelsItemView);
    }

    public static boolean isFuel(ItemStack stack) {
        for (var entry : fuels) {
            if (entry.test(stack)) {
                return true;
            }
        }
        return false;
    }

    static {
        rebuildFuels();
    }

    public static void rebuildFuels() {
        fuels.clear();
        fuelsItemView.clear();

        add(NTItems.HARMONIOUS_CHANGE_FUEL.get());
        add(NTItems.ETERNAL_HARMONIOUS_CHANGE_LAVA_BUCKET.get());
    }

    private static void add(ItemLike itemLike) {
        var item = itemLike.asItem();
        add(stack -> stack.is(item));
        fuelsItemView.add(item);
    }

    private static void add(TagKey<Item> tag) {
        add(stack -> stack.is(tag));
        // Todo: add all items in tag to fuelsItemView
    }

    private static void add(Predicate<ItemStack> predicate) {
        fuels.add(predicate);
    }

    public static boolean isEternalFuel(ItemStack stack) {
        return stack.is(NTItems.ETERNAL_HARMONIOUS_CHANGE_LAVA_BUCKET.get());
    }
}
