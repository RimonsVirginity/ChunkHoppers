package rimon.chunkhopper;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HopperManager {

    private final MCSchunkhoppers plugin;
    private final Map<Chunk, Location> activeHoppers = new ConcurrentHashMap<>();
    private Set<Material> pickupFilter = new HashSet<>();
    private boolean filterEnabled = false;

    public HopperManager(MCSchunkhoppers plugin) {
        this.plugin = plugin;
        loadConfigSettings();
    }

    /**
     * Loads settings from config.yml.
     */
    public void loadConfigSettings() {
        List<String> filterList = plugin.getConfig().getStringList("pickup-filter");
        if (!filterList.isEmpty()) {
            filterEnabled = true;
            pickupFilter.clear();
            for (String materialName : filterList) {
                try {
                    pickupFilter.add(Material.valueOf(materialName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material name in config.yml: " + materialName);
                }
            }
            plugin.getLogger().info("Loaded " + pickupFilter.size() + " items into pickup filter.");
        } else {
            filterEnabled = false;
            plugin.getLogger().info("Pickup filter is empty. Chunk hoppers will collect all items.");
        }
    }

    /**
     * Checks if an item's material should be picked up based on the config filter.
     * @param material The material to check.
     * @return True if the item should be picked up, false otherwise.
     */
    public boolean itemPassesFilter(Material material) {
        if (!filterEnabled) {
            return true; // No filter, pick up everything.
        }
        return pickupFilter.contains(material);
    }

    /**
     * Marks a hopper as a chunk hopper by adding persistent data.
     * @param block The hopper block to mark.
     */
    public void addChunkHopper(Block block) {
        if (block.getState() instanceof TileState) {
            TileState tileState = (TileState) block.getState();
            PersistentDataContainer container = tileState.getPersistentDataContainer();
            container.set(plugin.chunkHopperKey, PersistentDataType.BYTE, (byte) 1);
            tileState.update(); // Save the data to the block state

            activeHoppers.put(block.getChunk(), block.getLocation());
            plugin.getLogger().info("Chunk Hopper created at: " + block.getLocation().toString());
        }
    }

    /**
     * Removes a chunk hopper from tracking.
     * @param location The location of the hopper to remove.
     */
    public void removeChunkHopper(Location location) {
        if (activeHoppers.remove(location.getChunk(), location)) {
            plugin.getLogger().info("Chunk Hopper removed at: " + location.toString());
        }
    }

    /**
     * Checks if a block at a given location is a chunk hopper by reading its persistent data.
     * @param block The block to check.
     * @return True if it is a chunk hopper, false otherwise.
     */
    public boolean isChunkHopper(Block block) {
        if (block.getType() == Material.HOPPER && block.getState() instanceof TileState) {
            TileState tileState = (TileState) block.getState();
            PersistentDataContainer container = tileState.getPersistentDataContainer();
            return container.has(plugin.chunkHopperKey, PersistentDataType.BYTE);
        }
        return false;
    }

    /**
     * Attempts to find a chunk hopper in the given chunk and add an item to it.
     * @param itemStack The item to add.
     * @param chunk The chunk where the item spawned.
     * @return True if the item was successfully collected, false otherwise.
     */
    public boolean attemptPickup(ItemStack itemStack, Chunk chunk) {
        Location hopperLocation = activeHoppers.get(chunk);
        if (hopperLocation == null) {
            return false;
        }

        Block hopperBlock = hopperLocation.getBlock();
        if (hopperBlock.getType() != Material.HOPPER) {
            // Hopper was replaced by another block unexpectedly. Clean up.
            activeHoppers.remove(chunk);
            return false;
        }

        Hopper hopperState = (Hopper) hopperBlock.getState();

        // Check inventory full condition: firstEmpty() returns -1 if no empty slots.
        // We also need to check if existing stacks can accept more items.
        // The addItem method handles both checks efficiently.
        HashMap<Integer, ItemStack> remainingItems = hopperState.getInventory().addItem(itemStack);

        // If remainingItems is empty, the item was fully added.
        return remainingItems.isEmpty();
    }

    /**
     * Scans all loaded chunks on server startup to populate the active hopper map.
     */
    public void loadExistingHoppers() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunkForHopper(chunk);
            }
        }
        plugin.getLogger().info("Found " + activeHoppers.size() + " existing chunk hoppers on startup.");
    }

    /**
     * Scans a single chunk for tile entities and checks if they are chunk hoppers.
     * @param chunk The chunk to scan.
     */
    public void scanChunkForHopper(Chunk chunk) {
        for (BlockState tileEntity : chunk.getTileEntities()) {
            if (isChunkHopper(tileEntity.getBlock())) {
                activeHoppers.put(chunk, tileEntity.getLocation());
                break; // Only one chunk hopper per chunk as per this logic.
            }
        }
    }

    /**
     * Removes a potentially cached hopper location when its chunk unloads.
     * @param chunk The chunk being unloaded.
     */
    public void unloadChunk(Chunk chunk) {
        activeHoppers.remove(chunk);
    }
}
