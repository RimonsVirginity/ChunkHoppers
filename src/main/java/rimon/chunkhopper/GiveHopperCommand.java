package rimon.chunkhopper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class GiveHopperCommand implements CommandExecutor {

    private final MCSchunkhoppers plugin;

    public GiveHopperCommand(MCSchunkhoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /chunkhopper give <player> [amount]");
            return false;
        }

        if ("give".equalsIgnoreCase(args[0])) {
            return handleGiveCommand(sender, args);
        }

        // Future subcommands can be added here (e.g., /chunkhopper reload)

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /chunkhopper give <player> [amount]");
        return false;
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /chunkhopper give <player> [amount]");
            return false;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return false;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
                    return false;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount specified.");
                return false;
            }
        }

        ItemStack chunkHopperItem = createChunkHopperItem(this.plugin, amount);
        target.getInventory().addItem(chunkHopperItem);

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " Chunk Hopper(s) to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "You received " + amount + " Chunk Hopper(s)!");
        return true;
    }

    public static ItemStack createChunkHopperItem(MCSchunkhoppers plugin, int amount) {
        ItemStack item = new ItemStack(Material.HOPPER, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta ==null) return item;

        String displayName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("hopper-item.name", "&6&lChunk Hopper"));
        List<String> lore = plugin.getConfig().getStringList("hopper-item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        meta.setDisplayName(displayName);
        meta.setLore(lore);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(plugin.chunkHopperKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }
}