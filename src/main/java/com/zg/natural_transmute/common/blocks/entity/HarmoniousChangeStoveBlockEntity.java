package com.zg.natural_transmute.common.blocks.entity;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.zg.natural_transmute.client.inventory.HarmoniousChangeStoveMenu;
import com.zg.natural_transmute.common.items.crafting.HarmoniousChangeRecipe;
import com.zg.natural_transmute.common.items.crafting.HarmoniousChangeRecipeInput;
import com.zg.natural_transmute.registry.NTBlockEntityTypes;
import com.zg.natural_transmute.registry.NTItems;
import com.zg.natural_transmute.registry.NTRecipes;
import com.zg.natural_transmute.utils.NTCommonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ObjIntConsumer;

public class HarmoniousChangeStoveBlockEntity extends SimpleContainerBlockEntity {
    public static final int INPUT_A_SLOT = 0;
    public static final int INPUT_B_SLOT = 1;
    public static final int INPUT_C_SLOT = 2;
    public static final int FUEL_SLOT = 3;
    public static final int BIOME_CATALYST_SLOT = 4;
    public static final int OUTPUT_A_SLOT = 5;
    public static final int OUTPUT_B_SLOT = 6;
    public static final int OUTPUT_C_SLOT = 7;

    public static final int IDLING_STATE = 0;
    public static final int WORKING_STATE = 1;

    private int time;
    private int totalTime;
    private int currentState;

    /**
     * Remaining burn time for the current fuel.
     */
    private int litTime;

    /**
     * Maximum burn time for the current fuel.
     * Used by the menu to render the fuel bar.
     */
    private int litDuration;

    @Nullable
    public BlockPos mainPos;
    private final ContainerData containerData = new Data();
    private final RecipeManager.CachedCheck<HarmoniousChangeRecipeInput, ? extends HarmoniousChangeRecipe> quickCheck;

    public HarmoniousChangeStoveBlockEntity(BlockPos pos, BlockState blockState) {
        super(NTBlockEntityTypes.HARMONIOUS_CHANGE_STOVE.get(), pos, blockState);
        this.quickCheck = RecipeManager.createCheck(NTRecipes.HARMONIOUS_CHANGE_RECIPE.get());
        this.handler = new Handler(8);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HarmoniousChangeStoveBlockEntity blockEntity) {
        boolean shouldReset = false;
        boolean changed = false;

        if (blockEntity.hasInput()) {
            HarmoniousChangeRecipe recipe = blockEntity.checkHarmoniousChangeRecipe();
            if (recipe != null && blockEntity.canWork(recipe)) {    // Tick working
                if (!blockEntity.isLit()) { // Consumes fuel
                    var fuel = blockEntity.getItem(FUEL_SLOT);
                    var fuelValue = getFuel(fuel.getItem());
                    if (fuelValue != null) {
                        fuel.shrink(1);
                        blockEntity.litTime = fuelValue;
                        blockEntity.litDuration = fuelValue;
                    } else {    // Fail to lit, reset
                        shouldReset = true;
                    }
                }

                var isRecipeDone = blockEntity.processRecipe(recipe);
                if (isRecipeDone) {
                    blockEntity.currentState = IDLING_STATE;
                } else {
                    blockEntity.currentState = WORKING_STATE;
                }
                changed = true;
            } else {
                shouldReset = true;
            }
        } else if (blockEntity.time > 0) {
            shouldReset = true;
        }

        if (shouldReset) {
            blockEntity.time = 0;
            // XXX: totalTime is not using here, fill it in processRecipe :(
            blockEntity.currentState = IDLING_STATE;
        }

        if (changed || shouldReset) {
            setChanged(level, pos, state);
        }
    }


    /**
     * Tick process recipe.
     * @param recipe Recipe.
     * @return true for the recipe process was done, false for still processing.
     */
    private boolean processRecipe(HarmoniousChangeRecipe recipe) {
        if (this.level == null) {
            return false;
        }

        if (this.litTime > 0) { // Lit time -= 1, only in working.
            this.litTime -= 1;
        }

        ++this.time;
        this.totalTime = recipe.getTime();
        if (this.time < this.totalTime) {
            return false;
        }

        this.time = 0;
        ItemStack resultStack = recipe.assemble(this.getRecipeInput(), this.level.registryAccess());
        ItemStack outStack = this.handler.getStackInSlot(5);
        ItemStack extraStack = this.handler.getStackInSlot(6);
        ItemStack resultExtraStack = recipe.getResults().size() > 1 ?
                recipe.getResults().get(1) : ItemStack.EMPTY;
        if (outStack.isEmpty()) {
            this.handler.setStackInSlot(5, resultStack.copy());
        } else if (ItemStack.isSameItemSameComponents(outStack, resultStack)) {
            outStack.grow(resultStack.getCount());
        }

        if (!resultExtraStack.isEmpty()) {
            if (extraStack.isEmpty()) {
                this.handler.setStackInSlot(6, resultExtraStack.copy());
            } else if (ItemStack.isSameItemSameComponents(extraStack, resultExtraStack)) {
                extraStack.grow(resultExtraStack.getCount());
            }
        }

        if (recipe.isConsume()) {
            NTCommonUtils.consumeIngredients(this);
        }

        return true;
    }

    @Nullable
    private HarmoniousChangeRecipe checkHarmoniousChangeRecipe() {
        if (this.level != null) {
            RecipeHolder<? extends HarmoniousChangeRecipe> holder = this.quickCheck.getRecipeFor(this.getRecipeInput(), this.level).orElse(null);
            return holder != null ? holder.value() : null;
        }

        return null;
    }

    private HarmoniousChangeRecipeInput getRecipeInput() {
        ItemStack input1 = this.getItem(INPUT_A_SLOT);
        ItemStack input2 = this.getItem(INPUT_B_SLOT);
        ItemStack input3 = this.getItem(INPUT_C_SLOT);
        ItemStack biome_catalyst = this.getItem(BIOME_CATALYST_SLOT);

        List<ItemStack> ingredients = java.util.stream.Stream.of(input1, input2, input3)
                .filter(stack -> !stack.isEmpty())
                .collect(java.util.stream.Collectors.toList());

        return new HarmoniousChangeRecipeInput(ingredients, biome_catalyst);
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    private boolean hasInput() {
        for (int i = 0; i < 4; ++i) {
            if (!this.handler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    protected boolean canWork(HarmoniousChangeRecipe recipe) {
        if (this.level != null && recipe.matches(this.getRecipeInput(), this.level)) {
            boolean checkExtra;
            ItemStack resultStack = recipe.getResults().getFirst();
            if (resultStack.isEmpty()) {
                return false;
            } else {
                ItemStack outStack = this.handler.getStackInSlot(5);
                if (outStack.isEmpty()) {
                    checkExtra = true;
                } else if (!ItemStack.isSameItemSameComponents(outStack, resultStack)) {
                    return false;
                } else {
                    checkExtra = outStack.getCount() + resultStack.getCount() <= resultStack.getMaxStackSize();
                }

                ItemStack resultExtraStack1 = recipe.getResults().size() > 1 ? recipe.getResults().get(1) : ItemStack.EMPTY;
                if (resultExtraStack1.isEmpty()) {
                    return checkExtra;
                } else {
                    ItemStack extraStack1 = this.handler.getStackInSlot(6);
                    if (extraStack1.isEmpty()) {
                        checkExtra = true;
                    } else if (!ItemStack.isSameItemSameComponents(extraStack1, resultExtraStack1)) {
                        return false;
                    } else {
                        checkExtra = extraStack1.getCount() + resultExtraStack1.getCount() <= resultExtraStack1.getMaxStackSize();
                    }

                    ItemStack resultExtraStack2 = recipe.getResults().size() > 2 ? recipe.getResults().get(2) : ItemStack.EMPTY;
                    if (resultExtraStack2.isEmpty()) {
                        return checkExtra;
                    } else if (checkExtra) {
                        ItemStack extraStack2 = this.handler.getStackInSlot(7);
                        if (extraStack2.isEmpty()) {
                            return true;
                        } else if (!ItemStack.isSameItemSameComponents(extraStack2, resultExtraStack2)) {
                            return false;
                        } else {
                            return extraStack2.getCount() + resultExtraStack2.getCount() <= resultExtraStack2.getMaxStackSize();
                        }
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.time = tag.getInt("Time");
        this.totalTime = tag.getInt("TotalTime");
        this.litTime = tag.getInt("LitTime");
        this.litDuration = tag.getInt("LitDuration");
        this.currentState = tag.getInt("CurrentState");
        this.mainPos = NbtUtils.readBlockPos(tag, "MainPos").orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Time", this.time);
        tag.putInt("TotalTime", this.totalTime);
        tag.putInt("LitTime", this.litTime);
        tag.putInt("LitDuration", this.litDuration);
        tag.putInt("CurrentState", this.currentState);
        if (this.mainPos != null) {
            tag.put("MainPos", NbtUtils.writeBlockPos(this.mainPos));
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        ContainerLevelAccess access = ContainerLevelAccess.create(Objects.requireNonNull(this.level), this.worldPosition);
        return new HarmoniousChangeStoveMenu(containerId, inventory, access, this.handler, this.containerData);
    }

    // region Fuel

    @Nullable
    private static volatile Map<Item, Integer> fuelCache;

    public static void invalidateCache() {
        fuelCache = null;
    }

    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> cacheMap = fuelCache;
        if (cacheMap != null) {
            return cacheMap;
        } else {
            Map<Item, Integer> map = Maps.newLinkedHashMap();
            buildFuels((e, amount) -> e.ifRight(tag -> add(map, tag, amount))
                    .ifLeft(item -> map.put(item.asItem(), amount)));
            fuelCache = map;
            return map;
        }
    }

    private static void add(ObjIntConsumer<Either<Item, TagKey<Item>>> consumer, ItemLike item, int amount) {
        consumer.accept(Either.left(item.asItem()), amount);
    }

    private static void add(ObjIntConsumer<Either<Item, TagKey<Item>>> consumer, TagKey<Item> tag, int amount) {
        consumer.accept(Either.right(tag), amount);
    }

    public static void buildFuels(ObjIntConsumer<Either<Item, TagKey<Item>>> map) {
        add(map, Items.AMETHYST_SHARD, 200);
        add(map, Items.LAPIS_LAZULI, 400);
        add(map, NTItems.HARMONIOUS_CHANGE_FUEL.get(), 800);
    }

    private static void add(Map<Item, Integer> map, TagKey<Item> itemTag, int amount) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(itemTag)) {
            map.put(holder.value(), amount);
        }
    }

    /**
     * Get lit time for item.
     * @param item Item.
     * @return Lit time in ticks, or null for not a fuel.
     */
    public static @Nullable Integer getFuel(Item item) {
        return getFuel().get(item);
    }

    // endregion

    private class Data implements ContainerData {

        @Override
        public int get(int index) {
            if (index == 0) {
                return time;
            } else if (index == 1) {
                return totalTime;
            } else if (index == 2) {
                return currentState;
            } else if (index == 3) {
                if (litDuration > Short.MAX_VALUE) {
                    return Mth.floor(((double) litTime / litDuration) * Short.MAX_VALUE);
                }

                return litTime;
            } else if (index == 4) {
                return Math.min(litDuration, Short.MAX_VALUE);
            } else {
                return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                time = value;
            } else if (index == 1) {
                totalTime = value;
            } else if (index == 2) {
                currentState = value;
            } else if (index == 3) {
                litTime = value;
            } else if (index == 4) {
                litDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

    }

}