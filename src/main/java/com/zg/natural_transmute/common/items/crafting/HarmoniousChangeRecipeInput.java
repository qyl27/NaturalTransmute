package com.zg.natural_transmute.common.items.crafting;

import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public class HarmoniousChangeRecipeInput implements RecipeInput {
    public static final int INPUT_A_SLOT = 0;
    public static final int INPUT_B_SLOT = 1;
    public static final int INPUT_C_SLOT = 2;
    public static final int BIOME_CATALYST_SLOT = 4;

    private final List<ItemStack> items;
    private final ItemStack biome_catalyst;
    private final StackedContents stackedContents = new StackedContents();
    private final int ingredientCount;

    public HarmoniousChangeRecipeInput(List<ItemStack> items, ItemStack biome_catalyst) {
        this.items = items;
        this.biome_catalyst = biome_catalyst;
        int i = 0;
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                i++;
                this.stackedContents.accountStack(itemStack, 1);
            }
        }

        this.ingredientCount = i;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 3) {
            return this.items.get(index);
        } else if (index == 4) {
            return this.biome_catalyst;
        } else {
            throw new IllegalArgumentException("No item for index " + index);
        }
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.ingredientCount == 0;
    }

    public StackedContents stackedContents() {
        return this.stackedContents;
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public int ingredientCount() {
        return this.ingredientCount;
    }

}