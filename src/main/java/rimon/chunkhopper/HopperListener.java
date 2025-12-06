package rimon.chunkhopper;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

public class HopperListener implements Listener {

    private final HopperManager hopperManager;

    public HopperListener(MCSchunkhoppers plugin, HopperManager hopperManager) {
        this.hopperManager = hopperManager;
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
            // Note: A check should be added here to prevent multiple chunk hoppers in one chunk.
            // For simplicity, we assume one hopper per chunk. If another is placed, it overwrites the previous one in the map.
            hopperManager.addChunkHopper(event.getBlockPlaced());
            event.getPlayer().sendMessage("§aChunk Hopper created!");
        }
    }

    /**
     * Event: Player breaks a block.
     * Checks if the broken block was a registered chunk hopper and removes it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
            // Check if it was a chunk hopper before removing it from tracking.
            if (hopperManager.isChunkHopper(event.getBlock())) {
                hopperManager.removeChunkHopper(event.getBlock().getLocation());
                event.getPlayer().sendMessage("§cChunk Hopper removed.");
            }
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
