package rimon.chunkhopper;

import org.bukkit.plugin.java.JavaPlugin;

public final class MCSchunkhoppers extends JavaPlugin {

    private HopperManager hopperManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.hopperManager = new HopperManager(this);

        getCommand("chunkhopper").setExecutor(new GiveHopperCommand(this));

        getServer().getPluginManager().registerEvents(new HopperListener(this, hopperManager), this);

        getLogger().info("ChunkHopper plugin enabled successfully!");
    }

    @Override
    public void onDisable() {

        getLogger().info("ChunkHopper plugin disabled.");
    }
}
