package io.ncbpfluffybear.fluffymachines.machines;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.ncbpfluffybear.fluffymachines.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A machine that constructs ingots
 * out of its raw materials
 *
 * @author NCBPFluffyBear
 */
public class SmartFactory extends SlimefunItem implements EnergyNetComponent, RecipeDisplayItem {

    private static final int[] BORDER = new int[]{5, 6, 7, 8, 41, 42, 44, 50, 51, 52, 53};
    private static final int[] BORDER_IN = new int[]{0, 1, 2, 3, 4, 9, 13, 18, 22, 27, 31, 36, 40, 45, 46, 47, 48, 49};
    private static final int[] BORDER_OUT = new int[]{14, 15, 16, 17, 23, 26, 32, 33, 34, 35};
    private static final int[] COAL_SLOTS = new int[]{10, 11, 12};
    private static final int[] MISC_SLOTS = new int[]{19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int[] INPUT_SLOTS = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int PROGRESS_SLOT = 42;
    public static final int RECIPE_SLOT = 43;
    private static final ItemStack PROGRESS_ITEM = CustomItemStack.create(Material.FLINT_AND_STEEL, "&aProgress");

    private static final Map<BlockPosition, Integer> progress = new HashMap<>();
    private static final int PROCESS_TIME_TICKS = 10;

    private static final List<SlimefunItemStack> ACCEPTED_ITEMS = new ArrayList<>(Arrays.asList(
            SlimefunItems.BILLON_INGOT, SlimefunItems.SOLDER_INGOT, SlimefunItems.NICKEL_INGOT,
            SlimefunItems.COBALT_INGOT, SlimefunItems.DURALUMIN_INGOT, SlimefunItems.BRONZE_INGOT,
            SlimefunItems.BRASS_INGOT, SlimefunItems.ALUMINUM_BRASS_INGOT, SlimefunItems.STEEL_INGOT,
            SlimefunItems.DAMASCUS_STEEL_INGOT, SlimefunItems.ALUMINUM_BRONZE_INGOT,
            SlimefunItems.CORINTHIAN_BRONZE_INGOT, SlimefunItems.GILDED_IRON, SlimefunItems.REDSTONE_ALLOY,
            SlimefunItems.HARDENED_METAL_INGOT, SlimefunItems.REINFORCED_ALLOY_INGOT, SlimefunItems.FERROSILICON,
            SlimefunItems.ELECTRO_MAGNET, SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.HEATING_COIL,
            SlimefunItems.SYNTHETIC_EMERALD, SlimefunItems.GOLD_4K, SlimefunItems.GOLD_6K, SlimefunItems.GOLD_8K,
            SlimefunItems.GOLD_10K, SlimefunItems.GOLD_12K, SlimefunItems.GOLD_14K, SlimefunItems.GOLD_16K,
            SlimefunItems.GOLD_18K, SlimefunItems.GOLD_20K, SlimefunItems.GOLD_22K, SlimefunItems.GOLD_24K
    ));

    private static final List<SlimefunItemStack> MAGMA_ALTERNATIVES = new ArrayList<>(Arrays.asList(
            SlimefunItems.ELECTRO_MAGNET, SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.HEATING_COIL
    ));

    private final Map<SlimefunItem, List<ItemStack[]>> ITEM_RECIPES = new HashMap<>();

    public SmartFactory(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        for (SlimefunItemStack sfItem : ACCEPTED_ITEMS) {
            List<ItemStack[]> variationList = new ArrayList<>();
            ItemStack[] rawRecipe = collectRawRecipe(sfItem.getItem());
            if (rawRecipe != null && rawRecipe.length > 0) {
                variationList.add(rawRecipe);
                ITEM_RECIPES.put(sfItem.getItem(), variationList);
            }
        }

        for (SlimefunItemStack alternative : MAGMA_ALTERNATIVES) {
            SlimefunItem altItem = alternative.getItem();
            if (altItem != null && ITEM_RECIPES.containsKey(altItem)) {
                ItemStack[] original = ITEM_RECIPES.get(altItem).get(0);
                ItemStack[] variation = new ItemStack[original.length];
                for (int i = 0; i < original.length; i++) {
                    if (original[i] != null && original[i].getType() == Material.NETHERRACK && original[i].getAmount() % 16 == 0) {
                        variation[i] = new ItemStack(Material.MAGMA_BLOCK, original[i].getAmount() / 16);
                    } else if (original[i] != null) {
                        variation[i] = original[i].clone();
                    }
                }

                List<ItemStack[]> variations = ITEM_RECIPES.get(altItem);
                variations.add(variation);
            }
        }

        buildPreset();
        addItemHandler(onBreak());
    }

    private void buildPreset() {
        new BlockMenuPreset(this.getId(), "&cSmart Factory") {
            @Override
            public void init() {
                ChestMenuUtils.drawBackground(this, BORDER);
                Utils.createBorder(this, ChestMenuUtils.getInputSlotTexture(), BORDER_IN);
                Utils.createBorder(this, ChestMenuUtils.getOutputSlotTexture(), BORDER_OUT);
                this.addItem(PROGRESS_SLOT, PROGRESS_ITEM);
                this.addItem(9, CustomItemStack.create(Material.BLACK_STAINED_GLASS_PANE, "&7Coal Slots",
                        "&eThis row is reserved for coal for cargo."
                ));
                this.addItem(18, CustomItemStack.create(Material.YELLOW_STAINED_GLASS_PANE, "&bMisc Slots",
                        "&eThe remaining rows accept any item.", "&eCargo will fill stacks after",
                        "&eat least one of each recipe", "&erequirement has been inserted.",
                        "&eNeed to keep one more item in", "&eeach stack as template."
                ));
                this.addMenuClickHandler(RECIPE_SLOT, ChestMenuUtils.getEmptyClickHandler());
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return Utils.canOpen(b, p);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                String recipeId = BlockStorage.getLocationInfo(b.getLocation(), "recipe");
                SlimefunItem recipe = null;

                if (recipeId != null) {
                    recipe = SlimefunItem.getById(recipeId);
                }

                if (recipe == null) {
                    menu.replaceExistingItem(RECIPE_SLOT, CustomItemStack.create(Material.BARRIER, "&bRecipe",
                            "&cSneak and Right Click the", "&cfactory with a supported resource", "&cto set the recipe"
                    ));
                } else {
                    boolean isAccepted = false;
                    for (SlimefunItemStack acceptedItem : ACCEPTED_ITEMS) {
                        if (acceptedItem.getItem() != null && acceptedItem.getItem().equals(recipe)) {
                            isAccepted = true;
                            break;
                        }
                    }

                    if (isAccepted) {
                        menu.replaceExistingItem(RECIPE_SLOT, getDisplayItem(recipe, getDisplayRecipes()));
                    } else {
                        menu.replaceExistingItem(RECIPE_SLOT, CustomItemStack.create(Material.BARRIER, "&bRecipe",
                                "&cSneak and Right Click the", "&cfactory with a supported resource", "&cto set the recipe"
                        ));
                        BlockStorage.addBlockInfo(b, "recipe", null);
                    }
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return getOutputSlots();
                }

                if (item != null && item.getType() == Material.COAL) {
                    return COAL_SLOTS;
                }

                List<Integer> slots = new ArrayList<>();
                for (int slot : MISC_SLOTS) {
                    if (menu.getItemInSlot(slot) != null) {
                        slots.add(slot);
                    }
                }

                slots.sort(compareSlots(menu));

                int[] array = new int[slots.size()];
                for (int i = 0; i < slots.size(); i++) {
                    array[i] = slots.get(i);
                }

                return array;
            }
        };
    }

    private BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(@Nonnull Block b) {
                BlockMenu inv = BlockStorage.getInventory(b);

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }

                progress.remove(new BlockPosition(b.getWorld(), b.getX(), b.getY(), b.getZ()));
            }
        };
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {
            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                SmartFactory.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
    }

    protected void tick(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);
        if (inv == null) {
            return;
        }

        // Check if power is sufficient
        if (getCharge(b.getLocation()) < getEnergyConsumption()) {
            return;
        }

        final BlockPosition pos = new BlockPosition(b.getWorld(), b.getX(), b.getY(), b.getZ());

        // Check if we have valid ingredients
        HashMap<Integer, Integer> ingredients = getIngredientSlots(inv, b);
        if (ingredients == null || ingredients.isEmpty()) {
            // Reset progress if no valid ingredients
            if (progress.containsKey(pos)) {
                progress.remove(pos);
                ChestMenuUtils.updateProgressbar(inv, PROGRESS_SLOT, PROCESS_TIME_TICKS,
                        PROCESS_TIME_TICKS, PROGRESS_ITEM);
            }
            return;
        }

        // Get current progress
        int currentProgress = progress.getOrDefault(pos, 0);

        // Increment progress
        if (currentProgress < PROCESS_TIME_TICKS) {
            progress.put(pos, currentProgress + 1);
            ChestMenuUtils.updateProgressbar(inv, PROGRESS_SLOT, PROCESS_TIME_TICKS - currentProgress - 1,
                    PROCESS_TIME_TICKS, PROGRESS_ITEM);
            removeCharge(b.getLocation(), getEnergyConsumption());
            return;
        }

        // Craft the item
        if (craft(b, inv)) {
            // Consume items only if crafting was successful
            for (Map.Entry<Integer, Integer> ingredient : ingredients.entrySet()) {
                inv.consumeItem(ingredient.getKey(), ingredient.getValue());
            }
        }

        // Reset progress after crafting
        progress.remove(pos);
        ChestMenuUtils.updateProgressbar(inv, PROGRESS_SLOT, PROCESS_TIME_TICKS,
                PROCESS_TIME_TICKS, PROGRESS_ITEM);
    }

    private HashMap<Integer, Integer> getIngredientSlots(BlockMenu inv, Block b) {
        String recipeId = BlockStorage.getLocationInfo(b.getLocation(), "recipe");
        if (recipeId == null || recipeId.isEmpty()) {
            return null;
        }

        SlimefunItem key = SlimefunItem.getById(recipeId);
        if (key == null) {
            return null;
        }

        // Check if the recipe is in the accepted items list
        boolean isAccepted = false;
        for (SlimefunItemStack acceptedItem : ACCEPTED_ITEMS) {
            if (acceptedItem.getItem() != null && acceptedItem.getItem().equals(key)) {
                isAccepted = true;
                break;
            }
        }

        if (!isAccepted) {
            return null;
        }

        // Check if we have recipes for this item
        if (!ITEM_RECIPES.containsKey(key)) {
            return null;
        }

        // Check if output slot has space
        ItemStack outputItem = key.getItem();
        if (outputItem == null || !inv.fits(outputItem, getOutputSlots())) {
            return null;
        }

        // Try each recipe variation
        List<ItemStack[]> recipeVariations = ITEM_RECIPES.get(key);
        if (recipeVariations == null || recipeVariations.isEmpty()) {
            return null;
        }

        for (ItemStack[] variation : recipeVariations) {
            if (variation == null) {
                continue;
            }

            HashMap<Integer, Integer> ingredientSlots = new HashMap<>();
            boolean validVariation = true;

            for (ItemStack recipeItem : variation) {
                if (recipeItem == null) {
                    continue;
                }

                boolean found = false;
                int requiredAmount = recipeItem.getAmount();

                for (int slot : getInputSlots()) {
                    ItemStack slotItem = inv.getItemInSlot(slot);

                    if (slotItem == null || slotItem.getType() == Material.AIR) {
                        continue;
                    }

                    // Check if the item matches (for both vanilla and Slimefun items)
                    boolean matches = false;
                    SlimefunItem sfRecipeItem = SlimefunItem.getByItem(recipeItem);
                    SlimefunItem sfSlotItem = SlimefunItem.getByItem(slotItem);

                    if (sfRecipeItem != null && sfSlotItem != null) {
                        // Both are Slimefun items
                        matches = sfRecipeItem.getId().equals(sfSlotItem.getId());
                    } else if (sfRecipeItem == null && sfSlotItem == null) {
                        // Both are vanilla items
                        matches = slotItem.getType() == recipeItem.getType();
                    }

                    if (matches) {
                        int availableAmount = slotItem.getAmount();

                        // For coal slots, consume all; for misc items, keep 1 as template
                        boolean isCoalSlot = false;
                        for (int coalSlot : COAL_SLOTS) {
                            if (slot == coalSlot) {
                                isCoalSlot = true;
                                break;
                            }
                        }

                        if (isCoalSlot) {
                            if (availableAmount >= requiredAmount) {
                                ingredientSlots.put(slot, requiredAmount);
                                found = true;
                                break;
                            }
                        } else {
                            // Misc slots - need +1 for template
                            if (availableAmount >= requiredAmount + 1) {
                                ingredientSlots.put(slot, requiredAmount);
                                found = true;
                                break;
                            }
                        }
                    }
                }

                if (!found) {
                    validVariation = false;
                    break;
                }
            }

            if (validVariation && !ingredientSlots.isEmpty()) {
                return ingredientSlots;
            }
        }

        return null;
    }

    private boolean craft(Block b, BlockMenu inv) {
        String recipeId = BlockStorage.getLocationInfo(b.getLocation(), "recipe");
        if (recipeId == null || recipeId.isEmpty()) {
            return false;
        }

        SlimefunItem key = SlimefunItem.getById(recipeId);
        if (key == null) {
            return false;
        }

        // Check if the recipe is accepted
        boolean isAccepted = false;
        for (SlimefunItemStack acceptedItem : ACCEPTED_ITEMS) {
            if (acceptedItem.getItem() != null && acceptedItem.getItem().equals(key)) {
                isAccepted = true;
                break;
            }
        }

        if (!isAccepted || !ITEM_RECIPES.containsKey(key)) {
            return false;
        }

        ItemStack output = key.getItem();
        if (output == null) {
            return false;
        }

        inv.pushItem(output.clone(), getOutputSlots());
        return true;
    }

    private ItemStack[] collectRawRecipe(SlimefunItem key) {
        if (key == null) {
            return new ItemStack[0];
        }

        Pair<HashMap<Material, Integer>, HashMap<SlimefunItem, Integer>> ingredients = reduceRecipe(key);
        int totalSize = ingredients.getFirstValue().size() + ingredients.getSecondValue().size();

        if (totalSize == 0) {
            return new ItemStack[0];
        }

        ItemStack[] rawRecipe = new ItemStack[totalSize];
        int index = 0;

        for (Map.Entry<Material, Integer> materialEntry : ingredients.getFirstValue().entrySet()) {
            rawRecipe[index] = new ItemStack(materialEntry.getKey(), materialEntry.getValue());
            index++;
        }

        for (Map.Entry<SlimefunItem, Integer> sfItemEntry : ingredients.getSecondValue().entrySet()) {
            ItemStack sfItem = sfItemEntry.getKey().getItem();
            if (sfItem != null) {
                rawRecipe[index] = CustomItemStack.create(sfItem, sfItemEntry.getValue());
                index++;
            }
        }

        return rawRecipe;
    }

    private Pair<HashMap<Material, Integer>, HashMap<SlimefunItem, Integer>> reduceRecipe(SlimefunItem key) {
        HashMap<Material, Integer> rawVanilla = new HashMap<>();
        HashMap<SlimefunItem, Integer> rawSlimefun = new HashMap<>();

        if (key == null) {
            return new Pair<>(rawVanilla, rawSlimefun);
        }

        ItemStack[] recipe = key.getRecipe();
        if (key.equals(SlimefunItems.COPPER_WIRE.getItem())) {
            recipe = new ItemStack[] {SlimefunItems.COPPER_DUST.item()};
        }

        if (recipe == null) {
            return new Pair<>(rawVanilla, rawSlimefun);
        }

        for (ItemStack item : recipe) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!isReduced(item)) {
                SlimefunItem itemAsSlimefunItem = SlimefunItem.getByItem(item);
                if (itemAsSlimefunItem != null) {
                    Pair<HashMap<Material, Integer>, HashMap<SlimefunItem, Integer>> reduced = reduceRecipe(itemAsSlimefunItem);
                    reduced.getFirstValue().forEach((recipeItem, amt) -> {
                        rawVanilla.put(recipeItem, rawVanilla.getOrDefault(recipeItem, 0) + amt * item.getAmount());
                    });

                    reduced.getSecondValue().forEach((recipeItem, amt) -> {
                        rawSlimefun.put(recipeItem, rawSlimefun.getOrDefault(recipeItem, 0) + amt * item.getAmount());
                    });
                } else {
                    rawVanilla.put(item.getType(), rawVanilla.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            } else {
                SlimefunItem sfItem = SlimefunItem.getByItem(item);
                if (sfItem != null) {
                    rawSlimefun.put(sfItem, rawSlimefun.getOrDefault(sfItem, 0) + item.getAmount());
                } else {
                    switch (item.getType()) {
                        case IRON_INGOT:
                            SlimefunItem ironDust = SlimefunItems.IRON_DUST.getItem();
                            if (ironDust != null) {
                                rawSlimefun.put(ironDust, rawSlimefun.getOrDefault(ironDust, 0) + item.getAmount());
                            }
                            break;
                        case QUARTZ_BLOCK:
                            rawVanilla.put(Material.QUARTZ, rawVanilla.getOrDefault(Material.QUARTZ, 0) + item.getAmount() * 4);
                            break;
                        case REDSTONE_BLOCK:
                            rawVanilla.put(Material.REDSTONE, rawVanilla.getOrDefault(Material.REDSTONE, 0) + item.getAmount() * 9);
                            break;
                        default:
                            rawVanilla.put(item.getType(), rawVanilla.getOrDefault(item.getType(), 0) + item.getAmount());
                    }
                }
            }
        }

        return new Pair<>(rawVanilla, rawSlimefun);
    }

    private boolean isReduced(ItemStack test) {
        if (test == null) {
            return true;
        }
        SlimefunItem sfTest = SlimefunItem.getByItem(test);
        return sfTest == null || sfTest.getId().endsWith("_DUST");
    }

    public static ItemStack getDisplayItem(SlimefunItem key, List<ItemStack> displayRecipes) {
        if (key == null) {
            return new ItemStack(Material.BARRIER);
        }

        int index = -1;
        for (int i = 0; i < ACCEPTED_ITEMS.size(); i++) {
            if (ACCEPTED_ITEMS.get(i).getItem() != null && ACCEPTED_ITEMS.get(i).getItem().equals(key)) {
                index = i;
                break;
            }
        }

        ItemStack item;
        if (index != -1 && index < displayRecipes.size()) {
            item = displayRecipes.get(index).clone();
        } else {
            ItemStack keyItem = key.getItem();
            item = keyItem != null ? keyItem.clone() : new ItemStack(Material.BARRIER);
        }

        ItemMeta displayMeta = item.getItemMeta();
        if (displayMeta != null) {
            List<String> lore = displayMeta.getLore() != null ? displayMeta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(Utils.color("&eSneak and Right Click the factory with a"));
            lore.add(Utils.color("&ecompatible resource to change the recipe"));
            displayMeta.setLore(lore);
            item.setItemMeta(displayMeta);
        }

        return item;
    }

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> recipes = new ArrayList<>();

        for (SlimefunItemStack sfStack : ACCEPTED_ITEMS) {
            SlimefunItem sfItem = sfStack.getItem();
            if (sfItem == null) {
                continue;
            }

            ItemStack display = sfStack.item();
            if (display == null) {
                continue;
            }

            ItemMeta displayMeta = display.getItemMeta();
            if (displayMeta == null) {
                recipes.add(display);
                continue;
            }

            List<String> lore = new ArrayList<>();
            List<ItemStack[]> recipeVariations = ITEM_RECIPES.get(sfItem);

            if (recipeVariations != null && !recipeVariations.isEmpty() && recipeVariations.get(0) != null) {
                for (ItemStack item : recipeVariations.get(0)) {
                    if (item != null && item.getType() != Material.AIR) {
                        lore.add(Utils.color("&e" + item.getAmount() + "x " + Utils.getViewableName(item)));
                    }
                }

                if (recipeVariations.size() > 1) {
                    lore.add("");
                    lore.add(Utils.color("&7This recipe has a Magma Block alternative"));
                }
            }

            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
            recipes.add(display);
        }

        return recipes;
    }

    private Comparator<Integer> compareSlots(DirtyChestMenu menu) {
        return Comparator.comparingInt(slot -> {
            ItemStack item = menu.getItemInSlot(slot);
            return item != null ? item.getAmount() : 0;
        });
    }

    public static int getEnergyConsumption() {
        return 1024;
    }

    public static int getEnergyCapacity() {
        return getEnergyConsumption() * 3;
    }

    private int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    private int[] getOutputSlots() {
        return new int[]{24, 25};
    }

    public static List<SlimefunItemStack> getAcceptedItems() {
        return ACCEPTED_ITEMS;
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return getEnergyCapacity();
    }
}