package pl.m4code.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.m4code.Main;
import pl.m4code.utils.SendPlayer;
import pl.m4code.utils.TextUtil;

import java.util.List;
import java.util.stream.Collectors;

public class LobbyGui implements Listener {

    private final Main plugin = Main.getInstance();

    public void openMenu(Player player) {
        int rows = plugin.getConfig().getInt("gui.config.rows") * 9;
        Inventory inventory = Bukkit.createInventory(null, rows, TextUtil.fixColor(plugin.getConfig().getString("gui.config.title")));

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("gui.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                Material material = Material.getMaterial(itemsSection.getString(key + ".material"));
                String name = itemsSection.getString(key + ".name");
                List<String> lore = itemsSection.getStringList(key + ".lore");
                int slot = itemsSection.getInt(key + ".slot");

                ItemStack itemStack = new ItemStack(material);
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(TextUtil.fixColor(name));
                meta.setLore(lore.stream().map(TextUtil::fixColor).collect(Collectors.toList()));
                itemStack.setItemMeta(meta);

                inventory.setItem(slot, itemStack);
            }
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("gui.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                Material material = Material.getMaterial(itemsSection.getString(key + ".material"));
                String server = itemsSection.getString(key + ".server");

                if (clickedItem.getType() == material) {
                    player.closeInventory();
                    SendPlayer.toServer(player, server);
                    break;
                }
            }
        }
    }
}