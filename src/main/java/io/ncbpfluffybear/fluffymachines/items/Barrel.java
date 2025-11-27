package io.ncbpfluffybear.fluffymachines.items;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import io.ncbpfluffybear.fluffymachines.objects.DoubleHologramOwner;
import io.ncbpfluffybear.fluffymachines.objects.NonHopperableBlock;
import io.ncbpfluffybear.fluffymachines.utils.Utils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/**
 * A Remake of Barrels by John000708
 * FIXED: Dupe bug prevention with strict Slimefun vs Vanilla item validation
 *
 * @author NCBPFluffyBear
 */

public class Barrel extends NonHopperableBlock implements DoubleHologramOwner {

    private final int[] inputBorder = {9, 10, 11, 12, 18, 21, 27, 28, 29, 30};
    private final int[] outputBorder = {14, 15, 16, 17, 23, 26, 32, 33, 34, 35};
    private final int[] plainBorder = {0, 1, 2, 3, 4, 5, 6, 7, 8, 13, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    protected final int[] INPUT_SLOTS = {19, 20};
    protected final int[] OUTPUT_SLOTS = {24, 25};

    private final int STATUS_SLOT = 22;
    private final int DISPLAY_SLOT = 31;
    private final int HOLOGRAM_TOGGLE_SLOT = 36;
    private final int TRASH_TOGGLE_SLOT = 37;

    private final int OVERFLOW_AMOUNT = 3240;
    public static final DecimalFormat STORAGE_INDICATOR_FORMAT = new DecimalFormat("###,###.####",
            DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final ItemStack HOLOGRAM_OFF_ITEM = CustomItemStack.create(Material.QUARTZ_SLAB, "&3Toggle Hologram &c(Off)");
    private final ItemStack HOLOGRAM_ON_ITEM = CustomItemStack.create(Material.QUARTZ_SLAB, "&3Toggle Hologram &a(On)");
    private final ItemStack TRASH_ON_ITEM = CustomItemStack.create(SlimefunItems.TRASH_CAN.item(), "&3Toggle Overfill Trash &a(On)",
            "&7Turn on to delete unstorable items");
    private final ItemStack TRASH_OFF_ITEM = CustomItemStack.create(SlimefunItems.TRASH_CAN.item(), "&3Toggle Overfill Trash &c(Off)",
            "&7Turn on to delete unstorable items"
    );

    private final ItemSetting<Boolean> showHologram = new ItemSetting<>(this, "show-hologram", true);
    private final ItemSetting<Boolean> breakOnlyWhenEmpty = new ItemSetting<>(this, "break-only-when-empty", false);

    protected final ItemSetting<Integer> barrelCapacity;

    public Barrel(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                  int MAX_STORAGE) {
        super(category, item, recipeType, recipe);

        this.barrelCapacity = new IntRangeSetting(this, "capacity", 0, MAX_STORAGE, Integer.MAX_VALUE);

        addItemSetting(barrelCapacity);

        new BlockMenuPreset(getId(), getItemName()) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                buildMenu(menu, b);
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                if (Utils.canOpen(b, p)) {
                    updateMenu(b, BlockStorage.getInventory(b), true, getCapacity(b));
                    return true;
                }

                return false;
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                return new int[0];
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT) {
                    return INPUT_SLOTS;
                } else if (flow == ItemTransportFlow.WITHDRAW) {
                    return OUTPUT_SLOTS;
                } else {
                    return new int[0];
                }
            }
        };

        addItemHandler(onBreak());
        addItemSetting(showHologram, breakOnlyWhenEmpty);

    }

    private ItemHandler onBreak() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(@Nonnull BlockBreakEvent e, @Nonnull ItemStack item, @Nonnull List<ItemStack> drops) {
                Block b = e.getBlock();
                Player p = e.getPlayer();
                BlockMenu inv = BlockStorage.getInventory(b);
                int capacity = getCapacity(b);
                int stored = getStored(b);

                // Flag to track if hologram should be removed
                boolean shouldRemoveHologram = false;

                if (inv != null) {

                    int itemCount = 0;

                    if (breakOnlyWhenEmpty.getValue() && stored != 0) {
                        Utils.send(p, "&cThis barrel can't be broken since it has items inside it!");
                        e.setCancelled(true);
                        return;
                    }

                    for (Entity en : p.getNearbyEntities(5, 5, 5)) {
                        if (en instanceof Item) {
                            itemCount++;
                        }
                    }

                    if (itemCount > 5) {
                        Utils.send(p, "&cPlease remove nearby items before breaking this barrel!");
                        e.setCancelled(true);
                        return;
                    }

                    inv.dropItems(b.getLocation(), INPUT_SLOTS);
                    inv.dropItems(b.getLocation(), OUTPUT_SLOTS);

                    if (stored > 0) {
                        int stackSize = inv.getItemInSlot(DISPLAY_SLOT).getMaxStackSize();
                        ItemStack unKeyed = getStoredItem(b);

                        if (unKeyed.getType() == Material.BARRIER) {
                            setStored(b, 0);
                            updateMenu(b, inv, true, capacity);
                            shouldRemoveHologram = true; // Remove hologram when emptying a barrier item
                        } else if (stored > OVERFLOW_AMOUNT) {

                            Utils.send(p, "&eThere are more than " + OVERFLOW_AMOUNT + " items in this barrel! " +
                                    "Dropping " + OVERFLOW_AMOUNT + " items instead!");
                            int toRemove = OVERFLOW_AMOUNT;
                            while (toRemove >= stackSize) {

                                b.getWorld().dropItemNaturally(b.getLocation(), CustomItemStack.create(unKeyed, stackSize));

                                toRemove = toRemove - stackSize;
                            }

                            if (toRemove > 0) {
                                b.getWorld().dropItemNaturally(b.getLocation(), CustomItemStack.create(unKeyed, toRemove));
                            }

                            setStored(b, stored - OVERFLOW_AMOUNT);
                            updateMenu(b, inv, true, capacity);

                            e.setCancelled(true);
                        } else {

                            // Everything greater than 1 stack
                            while (stored >= stackSize) {

                                b.getWorld().dropItemNaturally(b.getLocation(), CustomItemStack.create(unKeyed, stackSize));

                                stored = stored - stackSize;
                            }

                            // Drop remaining, if there is any
                            if (stored > 0) {
                                b.getWorld().dropItemNaturally(b.getLocation(), CustomItemStack.create(unKeyed, stored));
                            }

                            // In case they use an explosive pick
                            setStored(b, 0);
                            updateMenu(b, inv, true, capacity);
                            shouldRemoveHologram = true; // Mark for hologram removal
                        }
                    } else {
                        shouldRemoveHologram = true; // Mark for hologram removal when empty
                    }

                    // Remove hologram if the block is going to be broken (not cancelled)
                    if (!e.isCancelled() && shouldRemoveHologram) {
                        removeHologram(b);
                    }
                } else if (stored == 0) {
                    // Even if no inventory, remove hologram if stored is 0 (empty barrel)
                    shouldRemoveHologram = true;
                    if (!e.isCancelled()) {
                        removeHologram(b);
                    }
                }
            }
        };
    }

    private void constructMenu(BlockMenuPreset preset) {
        Utils.createBorder(preset, ChestMenuUtils.getOutputSlotTexture(), outputBorder);
        Utils.createBorder(preset, ChestMenuUtils.getInputSlotTexture(), inputBorder);
        ChestMenuUtils.drawBackground(preset, plainBorder);
    }

    protected void buildMenu(BlockMenu menu, Block b) {
        int capacity = getCapacity(b);

        // Initialize an empty barrel
        if (BlockStorage.getLocationInfo(b.getLocation(), "stored") == null) {

            menu.replaceExistingItem(STATUS_SLOT, CustomItemStack.create(
                    Material.LIME_STAINED_GLASS_PANE, "&6Items Stored: &e0" + " / " + capacity, "&70%"));
            menu.replaceExistingItem(DISPLAY_SLOT, CustomItemStack.create(Material.BARRIER, "&cEmpty"));

            setStored(b, 0);

            if (showHologram.getValue()) {
                updateHologram(b, null, "&cEmpty");
            }

            // Change hologram settings
        } else if (!showHologram.getValue()) {
            removeHologram(b);
        }

        // Every time setup
        menu.addMenuClickHandler(STATUS_SLOT, ChestMenuUtils.getEmptyClickHandler());
        menu.addMenuClickHandler(DISPLAY_SLOT, ChestMenuUtils.getEmptyClickHandler());

        // Toggle hologram (Dynamic button)
        String holo = BlockStorage.getLocationInfo(b.getLocation(), "holo");
        if (holo == null || holo.equals("true")) {
            menu.replaceExistingItem(HOLOGRAM_TOGGLE_SLOT, HOLOGRAM_ON_ITEM);
        } else {
            menu.replaceExistingItem(HOLOGRAM_TOGGLE_SLOT, HOLOGRAM_OFF_ITEM);
        }
        menu.addMenuClickHandler(HOLOGRAM_TOGGLE_SLOT, (pl, slot, item, action) -> {
            toggleHolo(b, capacity);
            return false;
        });

        // Toggle trash (Dynamic button)
        String trash = BlockStorage.getLocationInfo(b.getLocation(), "trash");
        if (trash == null || trash.equals("false")) {
            menu.replaceExistingItem(TRASH_TOGGLE_SLOT, TRASH_OFF_ITEM);
        } else {
            menu.replaceExistingItem(TRASH_TOGGLE_SLOT, TRASH_ON_ITEM);
        }
        menu.addMenuClickHandler(TRASH_TOGGLE_SLOT, (pl, slot, item, action) -> {
            toggleTrash(b);
            return false;
        });

        // Insert all
        int INSERT_ALL_SLOT = 43;
        menu.replaceExistingItem(INSERT_ALL_SLOT,
                CustomItemStack.create(Material.LIME_STAINED_GLASS_PANE, "&bInsert All",
                        "&7> Click here to insert all", "&7compatible items into the barrel"));
        menu.addMenuClickHandler(INSERT_ALL_SLOT, (pl, slot, item, action) -> {
            insertAll(pl, menu, b);
            return false;
        });

        // Extract all
        int EXTRACT_SLOT = 44;
        menu.replaceExistingItem(EXTRACT_SLOT,
                CustomItemStack.create(Material.RED_STAINED_GLASS_PANE, "&6Extract All",
                        "&7> Left click to extract", "&7all items to your inventory",
                        "&7> Right click to extract 1 item"
                ));
        menu.addMenuClickHandler(EXTRACT_SLOT, (pl, slot, item, action) -> {
            extract(pl, menu, b, action);
            return false;
        });
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                Barrel.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return true;
            }
        });
    }

    protected void tick(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);

        // If inventory is null, skip ticking to prevent errors
        if (inv == null) {
            return;
        }

        int capacity = getCapacity(b);

        for (int slot : INPUT_SLOTS) {
            acceptInput(inv, b, slot, capacity);
        }

        for (int ignored : OUTPUT_SLOTS) {
            pushOutput(inv, b, capacity);
        }
    }

    void acceptInput(BlockMenu inv, Block b, int slot, int capacity) {
        // If inventory is null, skip processing
        if (inv == null) {
            return;
        }

        ItemStack inputItem = inv.getItemInSlot(slot);
        if (inputItem == null) {
            return;
        }

        int stored = getStored(b);
        ItemStack displayItem = inv.getItemInSlot(DISPLAY_SLOT);

        if (stored == 0) {
            registerItem(b, inv, slot, inputItem, capacity, stored);
        } else if (stored > 0 && displayItem != null) {

            // CRITICAL FIX: Validasi ketat sebelum menerima item
            if (!matchMeta(displayItem, inputItem)) {
                // Item tidak cocok, tolak
                String useTrash = BlockStorage.getLocationInfo(b.getLocation(), "trash");
                if (useTrash != null && useTrash.equals("true")) {
                    inv.replaceExistingItem(slot, null);
                }
                return;
            }

            if (stored < capacity) {

                // Can fit entire itemstack
                if (stored + inputItem.getAmount() <= capacity) {
                    storeItem(b, inv, slot, inputItem, capacity, stored);

                    // Split itemstack
                } else {
                    int amount = capacity - stored;
                    inv.consumeItem(slot, amount);

                    setStored(b, stored + amount);
                    updateMenu(b, inv, false, capacity);
                }
            } else {
                String useTrash = BlockStorage.getLocationInfo(b.getLocation(), "trash");

                if (useTrash != null && useTrash.equals("true")) {
                    inv.replaceExistingItem(slot, null);
                }

            }
        }
    }

    // DirtyChestMenu#fits() does not check for nbt data...
    public boolean fits(DirtyChestMenu inv, @Nonnull ItemStack item, int... slots) {
        // If inventory is null, return false to prevent insertion
        if (inv == null) {
            return false;
        }

        int metaMismatches = 0;
        for (int slot : slots) {
            ItemStack slotItem = inv.getItemInSlot(slot);
            // A small optimization for empty slots
            if (inv.getItemInSlot(slot) == null) {
                return true;
            }

            // Heavy but foolproof check
            if (!matchMeta(item, slotItem)) {
                metaMismatches++;
            }
        }

        if (metaMismatches == slots.length) {
            return false;
        }

        try {
            // This still performs other config based checks to see if insertion is valid...
            return InvUtils.fits(inv.toInventory(), ItemStackWrapper.wrap(item), slots);
        } catch (Exception e) {
            // If there's an exception in the fits check, return false to prevent insertion
            return false;
        }
    }

    // DirtyChestMenu#pushItem() does not check for nbt data...
    @Nullable
    public ItemStack pushItem(DirtyChestMenu inv, ItemStack item, int... slots) {
        if (item == null || item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot push null or AIR");
        }

        // If inventory is null, return the item to indicate nothing was pushed
        if (inv == null) {
            return item;
        }

        ItemStackWrapper wrapper = null;
        int amount = item.getAmount();

        for (int slot : slots) {
            if (amount <= 0) {
                break;
            }

            ItemStack stack = inv.getItemInSlot(slot);

            if (stack == null) {
                inv.replaceExistingItem(slot, item);
                return null;
            } else {
                int maxStackSize = Math.min(stack.getMaxStackSize(), inv.toInventory().getMaxStackSize());
                if (stack.getAmount() < maxStackSize) {
                    if (wrapper == null) {
                        wrapper = ItemStackWrapper.wrap(item);
                    }

                    // Patched with a meta check
                    if (ItemUtils.canStack(wrapper, stack) && matchMeta(wrapper, stack)) {
                        amount -= (maxStackSize - stack.getAmount());
                        // Make sure we don't exceed max stack size
                        int newAmount = Math.min(stack.getAmount() + item.getAmount(), maxStackSize);
                        stack.setAmount(newAmount);
                        item.setAmount(amount);
                    }
                }
            }
        }

        if (amount > 0) {
            return CustomItemStack.create(item, amount);
        } else {
            return null;
        }
    }

    void pushOutput(BlockMenu inv, Block b, int capacity) {
        // If inventory is null, skip processing
        if (inv == null) {
            return;
        }

        ItemStack displayItem = inv.getItemInSlot(DISPLAY_SLOT);
        if (displayItem != null && displayItem.getType() != Material.BARRIER) {

            int stored = getStored(b);

            // Output stack
            if (stored > displayItem.getMaxStackSize()) {

                ItemStack clone = CustomItemStack.create(Utils.unKeyItem(displayItem), displayItem.getMaxStackSize());


                if (fits(inv, clone, OUTPUT_SLOTS)) {
                    int amount = clone.getMaxStackSize();

                    setStored(b, stored - amount);
                    pushItem(inv, clone, OUTPUT_SLOTS);
                    updateMenu(b, inv, false, capacity);
                }

            } else if (stored != 0) {   // Output remaining

                ItemStack clone = CustomItemStack.create(Utils.unKeyItem(displayItem), stored);

                if (fits(inv, clone, OUTPUT_SLOTS)) {
                    setStored(b, 0);
                    pushItem(inv, clone, OUTPUT_SLOTS);
                    updateMenu(b, inv, false, capacity);
                }
            }
        }
    }

    private void registerItem(Block b, BlockMenu inv, int slot, ItemStack item, int capacity, int stored) {
        int amount = item.getAmount();

        // SECURITY FIX: Clone item untuk mencegah reference manipulation
        ItemStack clonedItem = item.clone();
        clonedItem.setAmount(1);

        // Set the display slot immediately with the first item to lock the barrel type
        // This "settings" ensures that only items matching this exact type (material + keyed status)
        // can be added to this barrel going forward
        inv.replaceExistingItem(DISPLAY_SLOT, clonedItem);

        // Fit all
        if (amount <= capacity) {
            storeItem(b, inv, slot, item, capacity, stored);
        } else {
            amount = capacity;
            inv.consumeItem(slot, amount);

            setStored(b, stored + amount);
            updateMenu(b, inv, false, capacity);
        }
    }

    private void storeItem(Block b, BlockMenu inv, int slot, ItemStack item, int capacity, int stored) {
        int amount = item.getAmount();
        inv.consumeItem(slot, amount);

        setStored(b, stored + amount);
        updateMenu(b, inv, false, capacity);
    }

    /**
     * This method checks if two items have the same metadata
     * FIXED: Mencegah dupe bug dengan validasi Slimefun item yang lebih ketat
     * FIXED: Proper handling of PersistentDataContainer types to prevent IllegalArgumentException
     *
     * @param item1 is the first item to compare
     * @param item2 is the second item to compare
     * @return if the items have the same meta
     */
    private boolean matchMeta(ItemStack item1, ItemStack item2) {
        // Type check first (lightest operation)
        if (!item1.getType().equals(item2.getType())) {
            return false;
        }

        // Check durability for non-block items
        if (item1.getDurability() != item2.getDurability()) {
            return false;
        }

        // If one has meta and the other doesn't, they're different
        if (!item1.hasItemMeta() || !item2.hasItemMeta()) {
            return item1.hasItemMeta() == item2.hasItemMeta(); // Must both have or not have meta
        }

        // CRITICAL FIX: Check if one is a Slimefun item and the other isn't
        SlimefunItem sfItem1 = SlimefunItem.getByItem(item1);
        SlimefunItem sfItem2 = SlimefunItem.getByItem(item2);

        // If one is a SF item and the other isn't, they're different
        if ((sfItem1 != null) != (sfItem2 != null)) {
            return false;
        }

        // If both are SF items, must be the exact same SF item
        if (sfItem1 != null && sfItem2 != null) {
            if (!sfItem1.getId().equals(sfItem2.getId())) {
                return false;
            }
        }

        // Get metadata for both items
        var item1Meta = item1.getItemMeta();
        var item2Meta = item2.getItemMeta();

        // Compare the persistent data container to distinguish between different item variants
        var item1Container = item1Meta.getPersistentDataContainer();
        var item2Container = item2Meta.getPersistentDataContainer();

        // Check if one has the fluffykey and the other doesn't
        boolean item1HasKey = item1Container.has(Utils.getFluffyKey(), PersistentDataType.INTEGER);
        boolean item2HasKey = item2Container.has(Utils.getFluffyKey(), PersistentDataType.INTEGER);

        // If one has the key and the other doesn't, they're different items
        if (item1HasKey != item2HasKey) {
            return false;
        }

        // If both have the key, check if they have the same value
        if (item1HasKey && item2HasKey) {
            Integer item1KeyValue = item1Container.get(Utils.getFluffyKey(), PersistentDataType.INTEGER);
            Integer item2KeyValue = item2Container.get(Utils.getFluffyKey(), PersistentDataType.INTEGER);

            if (item1KeyValue == null || item2KeyValue == null) {
                if (item1KeyValue != item2KeyValue) {
                    return false;
                }
            } else if (!item1KeyValue.equals(item2KeyValue)) {
                return false;
            }
        }

        // Check all NamespacedKeys in persistent data containers match
        var keys1 = item1Container.getKeys();
        var keys2 = item2Container.getKeys();

        if (!keys1.equals(keys2)) {
            return false;
        }

        // FIXED: Properly check each key with appropriate data type handling
        // We iterate through keys and try different data types safely
        for (NamespacedKey key : keys1) {
            // Try to compare as different types, wrapped in try-catch to handle type mismatches
            boolean matched = false;

            try {
                // Try INTEGER
                if (item1Container.has(key, PersistentDataType.INTEGER) &&
                        item2Container.has(key, PersistentDataType.INTEGER)) {
                    Integer val1 = item1Container.get(key, PersistentDataType.INTEGER);
                    Integer val2 = item2Container.get(key, PersistentDataType.INTEGER);
                    matched = (val1 == null && val2 == null) || (val1 != null && val1.equals(val2));
                    if (!matched) return false;
                    continue;
                }
            } catch (Exception ignored) {
                // Type mismatch, try next type
            }

            try {
                // Try STRING
                if (item1Container.has(key, PersistentDataType.STRING) &&
                        item2Container.has(key, PersistentDataType.STRING)) {
                    String val1 = item1Container.get(key, PersistentDataType.STRING);
                    String val2 = item2Container.get(key, PersistentDataType.STRING);
                    matched = (val1 == null && val2 == null) || (val1 != null && val1.equals(val2));
                    if (!matched) return false;
                    continue;
                }
            } catch (Exception ignored) {
                // Type mismatch, try next type
            }

            try {
                // Try BYTE
                if (item1Container.has(key, PersistentDataType.BYTE) &&
                        item2Container.has(key, PersistentDataType.BYTE)) {
                    Byte val1 = item1Container.get(key, PersistentDataType.BYTE);
                    Byte val2 = item2Container.get(key, PersistentDataType.BYTE);
                    matched = (val1 == null && val2 == null) || (val1 != null && val1.equals(val2));
                    if (!matched) return false;
                    continue;
                }
            } catch (Exception ignored) {
                // Type mismatch, try next type
            }

            try {
                // Try LONG
                if (item1Container.has(key, PersistentDataType.LONG) &&
                        item2Container.has(key, PersistentDataType.LONG)) {
                    Long val1 = item1Container.get(key, PersistentDataType.LONG);
                    Long val2 = item2Container.get(key, PersistentDataType.LONG);
                    matched = (val1 == null && val2 == null) || (val1 != null && val1.equals(val2));
                    if (!matched) return false;
                    continue;
                }
            } catch (Exception ignored) {
                // Type mismatch, try next type
            }

            try {
                // Try DOUBLE
                if (item1Container.has(key, PersistentDataType.DOUBLE) &&
                        item2Container.has(key, PersistentDataType.DOUBLE)) {
                    Double val1 = item1Container.get(key, PersistentDataType.DOUBLE);
                    Double val2 = item2Container.get(key, PersistentDataType.DOUBLE);
                    matched = (val1 == null && val2 == null) || (val1 != null && val1.equals(val2));
                    if (!matched) return false;
                    continue;
                }
            } catch (Exception ignored) {
                // Type mismatch, continue
            }

            // If no type matched, check if both don't have the key (which shouldn't happen since we're iterating keys1)
            if (!matched) {
                // If we couldn't match any type, assume they're different
                return false;
            }
        }

        // Check enchantments
        if (!item1Meta.getEnchants().equals(item2Meta.getEnchants())) {
            return false;
        }

        // Check display names
        if (item1Meta.hasDisplayName() != item2Meta.hasDisplayName()) {
            return false;
        }

        if (item1Meta.hasDisplayName() && item2Meta.hasDisplayName()) {
            if (!item1Meta.getDisplayName().equals(item2Meta.getDisplayName())) {
                return false;
            }
        }

        // Check lore
        if (item1Meta.hasLore() != item2Meta.hasLore()) {
            return false;
        }

        if (item1Meta.hasLore() && item2Meta.hasLore()) {
            var lore1 = item1Meta.getLore();
            var lore2 = item2Meta.getLore();

            if (lore1 == null && lore2 == null) {
                // Both are null, continue
            } else if (lore1 == null || lore2 == null) {
                return false;
            } else if (!lore1.equals(lore2)) {
                return false;
            }
        }

        // Compare other meta properties
        if (item1Meta.isUnbreakable() != item2Meta.isUnbreakable()) {
            return false;
        }

        // Final comprehensive check
        // This prevents VNL items from matching with SF items of the same type
        return item1Meta.equals(item2Meta);
    }

    /**
     * This method updates the barrel's menu and hologram displays
     *
     * @param b   is the barrel block
     * @param inv is the barrel's inventory
     */
    public void updateMenu(Block b, BlockMenu inv, boolean force, int capacity) {
        String hasHolo = BlockStorage.getLocationInfo(b.getLocation(), "holo");
        int stored = getStored(b);
        String itemName;

        String storedPercent = doubleRoundAndFade((double) stored / (double) capacity * 100);

        // Check for null display slot item to prevent NullPointerException
        ItemStack displayItem = inv.getItemInSlot(DISPLAY_SLOT);
        String storedStacks;

        if (displayItem != null && displayItem.getType() != Material.BARRIER) {
            storedStacks = doubleRoundAndFade((double) stored / (double) displayItem.getMaxStackSize());
        } else {
            // Default to 64 if display item is null or BARRIER (empty barrel)
            storedStacks = doubleRoundAndFade((double) stored / 64.0);
        }

        // This helps a bit with lag, but may have visual impacts
        if (inv.hasViewer() || force) {
            inv.replaceExistingItem(STATUS_SLOT, CustomItemStack.create(
                    Material.LIME_STAINED_GLASS_PANE, "&6Items Stored: &e" + stored + " / " + capacity,
                    "&b" + storedStacks + " Stacks &8| &7" + storedPercent + "&7%"));
        }

        if (displayItem != null) {
            if (displayItem.getItemMeta() != null && displayItem.getItemMeta().hasDisplayName()) {
                itemName = displayItem.getItemMeta().getDisplayName();
            } else {
                itemName = WordUtils.capitalizeFully(displayItem.getType().name().replace("_", " "));
            }
        } else {
            // Use a default display name when display slot is null
            itemName = "Unknown Item";
        }

        if (showHologram.getValue() && (hasHolo == null || hasHolo.equals("true"))) {
            updateHologram(b, itemName, " &9x" + stored + " &7(" + storedPercent + "&7%)");
        }

        if (stored == 0) {
            inv.replaceExistingItem(DISPLAY_SLOT, CustomItemStack.create(Material.BARRIER, "&cEmpty"));
            if (showHologram.getValue() && (hasHolo == null || hasHolo.equals("true"))) {
                updateHologram(b, null, "&cEmpty");
            }
        }
    }

    /**
     * This method toggles if a hologram is present above the barrel.
     *
     * @param b is the block the hologram is linked to
     */
    private void toggleHolo(Block b, int capacity) {
        String toggle = BlockStorage.getLocationInfo(b.getLocation(), "holo");
        if (toggle == null || toggle.equals("true")) {
            putBlockData(b, HOLOGRAM_TOGGLE_SLOT, "holo", HOLOGRAM_OFF_ITEM, false);
            removeHologram(b);
        } else {
            putBlockData(b, HOLOGRAM_TOGGLE_SLOT, "holo", HOLOGRAM_ON_ITEM, true);
            updateMenu(b, BlockStorage.getInventory(b), false, capacity);
        }
    }

    /**
     * Toggle auto dispose status of barrel
     */
    private void toggleTrash(Block b) {
        String toggle = BlockStorage.getLocationInfo(b.getLocation(), "trash");
        if (toggle == null || toggle.equals("false")) {
            putBlockData(b, TRASH_TOGGLE_SLOT, "trash", TRASH_ON_ITEM, true);
        } else {
            putBlockData(b, TRASH_TOGGLE_SLOT, "trash", TRASH_OFF_ITEM, false);
        }
    }

    /**
     * Sets a key in BlockStorage and replaces an item
     */
    private void putBlockData(Block b, int slot, String key, ItemStack displayItem, boolean data) {
        BlockStorage.addBlockInfo(b.getLocation(), key, String.valueOf(data));
        BlockStorage.getInventory(b).replaceExistingItem(slot, displayItem);
    }

    public void insertAll(Player p, BlockMenu menu, Block b) {
        ItemStack keyedStoredItem = menu.getItemInSlot(DISPLAY_SLOT);
        PlayerInventory inv = p.getInventory();
        int capacity = getCapacity(b);

        int stored = getStored(b);

        for (int i = 0; i < inv.getContents().length; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) {
                continue;
            }
            int amount = item.getAmount();
            if (matchMeta(item, keyedStoredItem) && stored + amount <= capacity) {
                inv.setItem(i, null);
                stored += amount;
            }
        }

        BlockStorage.addBlockInfo(b.getLocation(), "stored", String.valueOf(stored));
        updateMenu(b, menu, false, capacity);
    }

    public void extract(Player p, BlockMenu menu, Block b, ClickAction action) {
        ItemStack storedItem = getStoredItem(b);
        int capacity = getCapacity(b);

        PlayerInventory inv = p.getInventory();
        int stored = getStored(b);

        // Extract single
        if (action.isRightClicked()) {
            if (stored > 0) { // Extract from stored
                Utils.giveOrDropItem(p, new ItemStack(storedItem.getType(), 1, storedItem.getDurability()));
                setStored(b, --stored);
                updateMenu(b, menu, false, capacity);
                return;
            } else {
                for (int slot : OUTPUT_SLOTS) { // Extract from slot
                    if (menu.getItemInSlot(slot) != null) {
                        Utils.giveOrDropItem(p, CustomItemStack.create(menu.getItemInSlot(slot), 1));
                        menu.consumeItem(slot);
                        return;
                    }
                }
            }
            Utils.send(p, "&cThis barrel is empty!");
            return;
        }

        if (storedItem.getType() == Material.BARRIER) {
            Utils.send(p, "&cThis barrel is empty!");
            return;
        }

        // Extract all - with better state validation
        ItemStack[] contents = inv.getStorageContents().clone();
        int maxStackSize = storedItem.getMaxStackSize();
        int outI = 0;

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                if (stored >= maxStackSize) {
                    inv.setItem(i, CustomItemStack.create(storedItem, maxStackSize));
                    stored -= maxStackSize;
                } else if (stored > 0) {
                    inv.setItem(i, CustomItemStack.create(storedItem, stored));
                    stored = 0;
                } else {
                    // Check output slots only if we haven't exceeded the limit
                    if (outI >= OUTPUT_SLOTS.length) {
                        break;
                    }

                    ItemStack item = menu.getItemInSlot(OUTPUT_SLOTS[outI]);

                    if (item == null) {
                        outI++; // Move to next output slot even if this one is empty
                        continue;
                    }

                    inv.setItem(i, item.clone());
                    menu.replaceExistingItem(OUTPUT_SLOTS[outI], null);
                    outI++;
                }
            }
        }

        // Ensure stored count doesn't go negative due to potential race conditions
        if (stored < 0) {
            stored = 0;
        }

        setStored(b, stored);
        updateMenu(b, menu, false, capacity);
    }

    public static String doubleRoundAndFade(double num) {
        // Using same format that is used on lore power
        String formattedString = STORAGE_INDICATOR_FORMAT.format(num);
        if (formattedString.indexOf('.') != -1) {
            return formattedString.substring(0, formattedString.indexOf('.')) + ChatColor.DARK_GRAY
                    + formattedString.substring(formattedString.indexOf('.')) + ChatColor.GRAY;
        } else {
            return formattedString;
        }
    }

    public int getStored(Block b) {
        String storedStr = BlockStorage.getLocationInfo(b.getLocation(), "stored");
        if (storedStr == null) {
            // If no stored value is found, initialize to 0
            return 0;
        }
        try {
            return Integer.parseInt(storedStr);
        } catch (NumberFormatException e) {
            // If the stored value is not a valid number, return 0 and log issue
            return 0;
        }
    }

    public void setStored(Block b, int amount) {
        BlockStorage.addBlockInfo(b.getLocation(), "stored", String.valueOf(Math.max(0, amount)));
    }

    public ItemStack getStoredItem(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);
        if (inv == null) {
            // Return a barrier item if inventory is null to indicate empty state
            return new ItemStack(Material.BARRIER);
        }
        ItemStack displayItem = inv.getItemInSlot(DISPLAY_SLOT);
        if (displayItem == null) {
            // Return a barrier item if display slot is null
            return new ItemStack(Material.BARRIER);
        }
        return Utils.unKeyItem(displayItem);
    }

    /**
     * Gets capacity of barrel
     * Includes Block parameter for MiniBarrel
     */
    public int getCapacity(Block b) {
        return barrelCapacity.getValue();
    }

    public static int getDisplayCapacity(Barrel.BarrelType barrel) {
        int capacity = Slimefun.getItemCfg().getInt(barrel.getKey() + ".capacity");
        if (capacity == 0) {
            capacity = barrel.getDefaultSize();
        }

        return capacity;
    }

    @Nonnull
    @Override
    public Vector getHologramOffset(@Nonnull Block b) {
        return new Vector(0.5, 0.9, 0.5);
    }

    public enum BarrelType {

        SMALL(17280000, "&eSmall Fluffy Barrel", Material.BEEHIVE, SlimefunItems.REINFORCED_PLATE.item(), new ItemStack(Material.OAK_LOG)),
        MEDIUM(34560000, "&6Medium Fluffy Barrel", Material.BARREL, SlimefunItems.REINFORCED_PLATE.item(), new ItemStack(Material.SMOOTH_STONE)),
        BIG(69120000, "&bBig Fluffy Barrel", Material.SMOKER, SlimefunItems.REINFORCED_PLATE.item(), new ItemStack(Material.BRICKS)),
        LARGE(138240000, "&aLarge Fluffy Barrel", Material.LODESTONE, SlimefunItems.REINFORCED_PLATE.item(), new ItemStack(Material.IRON_BLOCK)),
        MASSIVE(276480000, "&5Massive Fluffy Barrel", Material.CRYING_OBSIDIAN, SlimefunItems.REINFORCED_PLATE.item(), new ItemStack(Material.OBSIDIAN)),
        BOTTOMLESS(1728000000, "&cBottomless Fluffy Barrel", Material.RESPAWN_ANCHOR, SlimefunItems.BLISTERING_INGOT_3.item(), SlimefunItems.REINFORCED_PLATE.item());

        private final int defaultSize;
        private final String displayName;
        private final Material itemMaterial;
        private final ItemStack reinforcement;
        private final ItemStack border;

        BarrelType(int defaultSize, String displayName, Material itemMaterial, ItemStack reinforcement, ItemStack border) {
            this.defaultSize = defaultSize;
            this.displayName = displayName;
            this.itemMaterial = itemMaterial;
            this.reinforcement = reinforcement;
            this.border = border;
        }

        public int getDefaultSize() {
            return defaultSize;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getType() {
            return itemMaterial;
        }

        public String getKey() {
            return this.name().toUpperCase() + "_FLUFFY_BARREL";
        }

        public ItemStack getReinforcement() {
            return reinforcement;
        }

        public ItemStack getBorder() {
            return border;
        }
    }

}