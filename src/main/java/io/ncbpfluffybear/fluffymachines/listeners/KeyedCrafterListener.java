package io.ncbpfluffybear.fluffymachines.listeners;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.ncbpfluffybear.fluffymachines.machines.AutoCraftingTable;
import io.ncbpfluffybear.fluffymachines.machines.SmartFactory;
import io.ncbpfluffybear.fluffymachines.utils.Utils;
import java.util.Optional;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class KeyedCrafterListener implements Listener {

    public KeyedCrafterListener() {
    }

    @EventHandler
    private void onSmartFactoryInteract(PlayerRightClickEvent e) {
        Optional<Block> clickedBlock = e.getClickedBlock();

        if (e.getHand() == EquipmentSlot.HAND && e.useBlock() != Event.Result.DENY && clickedBlock.isPresent() && e.getPlayer().isSneaking()) {
            Optional<SlimefunItem> slimefunBlock = e.getSlimefunBlock();

            if (!slimefunBlock.isPresent()) {
                return;
            }

            SlimefunItem sfBlock = slimefunBlock.get();
            ItemStack item = e.getItem();
            Player p = e.getPlayer();
            SlimefunItem key = SlimefunItem.getByItem(item);
            Block b = clickedBlock.get();

            // Handle SmartFactory recipe setting
            if (sfBlock instanceof SmartFactory) {

                if (isCargoNode(key)) {
                    return;
                }
                e.cancel();

                if (key == null) {
                    Utils.send(p, "&cYou can not use vanilla items with this machine!");
                    return;
                }

                // PERBAIKAN: Bandingkan berdasarkan ID atau gunakan method yang lebih aman
                // Opsi 1: Bandingkan berdasarkan ID (Rekomendasi)
                if (isItemAccepted(key, SmartFactory.getAcceptedItems())) {
                    BlockStorage.addBlockInfo(b, "recipe", key.getId());
                    BlockStorage.getInventory(b).replaceExistingItem(SmartFactory.RECIPE_SLOT,
                            SmartFactory.getDisplayItem(key, ((RecipeDisplayItem) sfBlock).getDisplayRecipes())
                    );
                    Utils.send(p, "&aTarget recipe set to " + key.getItemName());
                } else {
                    Utils.send(p, "&cThis item is not supported!");
                }

            } else if (sfBlock instanceof AutoCraftingTable) {

                if (isCargoNode(key)) {
                    return;
                }
                e.cancel();

                if (item.getType() == Material.AIR) {
                    Utils.send(p, "&cRight click the machine with an item to set the vanilla recipe");
                    return;
                }

                BlockStorage.getInventory(b).replaceExistingItem(AutoCraftingTable.KEY_SLOT,
                        AutoCraftingTable.createKeyItem(item.getType())
                );

                Utils.send(p, "&aTarget recipe set to "
                        + WordUtils.capitalizeFully(item.getType().name().replace("_", " "))
                );
            }
        }
    }

    /**
     * Check if a SlimefunItem is in the accepted items list
     *
     * @param item The SlimefunItem to check
     * @param acceptedItems The list of accepted SlimefunItemStacks
     * @return true if the item is accepted
     */
    private boolean isItemAccepted(SlimefunItem item, java.util.List<SlimefunItemStack> acceptedItems) {
        if (item == null || acceptedItems == null) {
            return false;
        }

        String itemId = item.getId();

        for (SlimefunItemStack acceptedItem : acceptedItems) {
            if (acceptedItem.getItemId().equals(itemId)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCargoNode(@Nullable SlimefunItem recipe) {
        if (recipe == null) {
            return false;
        }

        String recipeId = recipe.getId();
        return recipeId.equals("CARGO_NODE_INPUT")
                || recipeId.equals("CARGO_NODE")
                || recipeId.equals("CARGO_NODE_OUTPUT");
    }
}