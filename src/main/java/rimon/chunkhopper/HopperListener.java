package rimon.chunkhopper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.meta.ItemMeta;
import rimon.chunkhopper.HopperManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class HopperListener implements Listener {
    private final MCSchunkhoppers plugin;

    private final HopperManager hopperManager;

    public HopperListener(MCSchunkhoppers plugin, HopperManager hopperManager) {
        this.hopperManager = hopperManager;
        this.plugin = plugin;
    }

    /**
     * Event: Player places a block.
     * Checks if a hopper is placed and registers it as a chunk hopper.
     * For this simple implementation, every hopper placed becomes a chunk hopper.
     * A more advanced version would check item NBT or player permissions here.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.HOPPER) {
            ItemMeta meta = event.getItemInHand().getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(plugin.chunkHopperKey, PersistentDataType.BYTE)) {

                hopperManager.addChunkHopper(event.getBlockPlaced());
                event.getPlayer().sendMessage("§aChunk Hopper created!");
            }
        }
    }

    /**
     * Event: Player breaks a block.
     * Checks if the broken block was a registered chunk hopper and removes it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) return;
        Bukkit.broadcastMessage("This is a hopper");
        if (hopperManager.isChunkHopper(block)) {
            Bukkit.broadcastMessage("This is a Chunk hopper");
            event.setDropItems(false);
            ItemStack item = GiveHopperCommand.createChunkHopperItem(plugin,1);
            block.getWorld().dropItemNaturally(block.getLocation(), item);
            Bukkit.broadcastMessage("should've given you chunk hopper");
            hopperManager.removeChunkHopper(block.getLocation());
            event.getPlayer().sendMessage("§cChunk Hopper removed.");

        }
            }

    /**
     * Event: Item spawns in the world (from block breaks, mob deaths, etc.).
     * This is the core logic for collecting items.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack itemStack = event.getEntity().getItemStack();

        // 1. Check if the item type matches the filter from config.yml.
        if (!hopperManager.itemPassesFilter(itemStack.getType())) {
            return;
        }

        // 2. Attempt to pick up the item.
        boolean pickedUp = hopperManager.attemptPickup(itemStack, event.getLocation().getChunk());

        // 3. If successfully picked up, remove the item entity from the world.
        if (pickedUp) {
            event.getEntity().remove();
        }
        // If not picked up (hopper full or no hopper in chunk), do nothing and let the item fall normally.
    }

    /**
     * Event: Chunk loads into memory.
     * Scans the loaded chunk to find any existing chunk hoppers for persistence.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        hopperManager.scanChunkForHopper(event.getChunk());
    }

    /**
     * Event: Chunk unloads from memory.
     * Removes the chunk's hopper from the active map to free up resources.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        hopperManager.unloadChunk(event.getChunk());
    }
}
