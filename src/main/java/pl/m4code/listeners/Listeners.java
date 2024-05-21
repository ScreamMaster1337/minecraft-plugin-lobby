package pl.m4code.listeners;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.m4code.Main;
import pl.m4code.guis.LobbyGui;
import pl.m4code.system.MessageManager;
import pl.m4code.utils.TextUtil;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.*;
import java.util.List;

public class Listeners implements Listener {

    private final Main plugin;
    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private final HashMap<UUID, Long> flyCooldowns = new HashMap<>();
    private final HashMap<UUID, Long> hidePlayersCooldowns = new HashMap<>();

    public Listeners(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();


        World world = Bukkit.getWorld("world");
        if (world == null) {
            player.sendMessage("Nie odnaleziono takiego świata!");
            return;
        }
        if (!player.hasPlayedBefore()) {

            event.getPlayer().teleport(new Location(world, Main.getInstance().getConfig().getDouble("lobby.spawn.x"), Main.getInstance().getConfig().getDouble("lobby.spawn.y"), Main.getInstance().getConfig().getDouble("lobby.spawn.z"), Main.getInstance().getConfig().getInt("lobby.spawn.yaw"), Main.getInstance().getConfig().getInt("lobby.spawn.pitch")));
        }
        event.getPlayer().teleport(new Location(world, Main.getInstance().getConfig().getDouble("lobby.spawn.x"), Main.getInstance().getConfig().getDouble("lobby.spawn.y"), Main.getInstance().getConfig().getDouble("lobby.spawn.z"), Main.getInstance().getConfig().getInt("lobby.spawn.yaw"), Main.getInstance().getConfig().getInt("lobby.spawn.pitch")));

        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttribute.getValue();
        player.setHealth(maxHealth);
        event.setJoinMessage(null);
        player.getInventory().clear();

        ItemStack compassItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(Main.getInstance().getConfig().getString("items.lobby.material")))));
        ItemMeta compassMeta = compassItem.getItemMeta();
        compassMeta.setDisplayName(TextUtil.fixColor(String.valueOf(Main.getInstance().getConfig().getString("items.lobby.name"))));
        List<String> lore1 = new ArrayList<>();
        List<String> loreFromConfig = Main.getInstance().getConfig().getStringList("items.lobby.lore");
        for (String loreLine : loreFromConfig) {
            lore1.add(TextUtil.fixColor(loreLine));
        }
        compassMeta.setLore(lore1);
        compassItem.setItemMeta(compassMeta);

        ItemStack featherItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(Main.getInstance().getConfig().getString("items.fly.material")))));
        ItemMeta featherMeta = featherItem.getItemMeta();
        featherMeta.setDisplayName(TextUtil.fixColor(String.valueOf(Main.getInstance().getConfig().getString("items.fly.name"))));
        List<String> lore3 = new ArrayList<>();
        List<String> loreFromConfig3 = Main.getInstance().getConfig().getStringList("items.fly.lore");
        for (String loreLine : loreFromConfig3) {
            lore3.add(TextUtil.fixColor(loreLine));
        }
        featherMeta.setLore(lore3);
        featherItem.setItemMeta(featherMeta);

        ItemStack hidePlayersItem = new ItemStack(hiddenPlayers.contains(player.getUniqueId()) ? Material.GRAY_DYE : Material.LIME_DYE);
        ItemMeta hidePlayersMeta = hidePlayersItem.getItemMeta();
        hidePlayersMeta.setDisplayName(TextUtil.fixColor(String.valueOf(Main.getInstance().getConfig().getString("items.hide_players.name"))));
        List<String> lore4 = new ArrayList<>();
        List<String> loreFromConfig4 = Main.getInstance().getConfig().getStringList("items.hide_players.lore");
        for (String loreLine : loreFromConfig4) {
            lore4.add(TextUtil.fixColor(loreLine));
        }
        hidePlayersMeta.setLore(lore4);
        hidePlayersItem.setItemMeta(hidePlayersMeta);

        player.getInventory().setItem(7, featherItem);
        player.getInventory().setItem(4, compassItem);
        player.getInventory().setItem(8, hidePlayersItem);
    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null) {
            if (item.getType() == Material.getMaterial(Objects.requireNonNull(Main.getInstance().getConfig().getString("items.fly.material"))) && event.getAction().name().contains("RIGHT_CLICK")) {
                if (player.hasPermission("m4code.lobby.fly")) {
                    long cooldown = getCooldownRemaining(player, flyCooldowns);
                    int cooldown_fly = MessageManager.config.getInt("cooldown_fly");
                    if (cooldown <= 0) {
                        toggleFly(player);
                        setCooldown(player, flyCooldowns, cooldown_fly);
                    } else {
                        String cooldownMessage = MessageManager.config.getString("cooldown_message_fly");
                        cooldownMessage = cooldownMessage.replace("{cooldown}", String.valueOf(cooldown));
                        player.sendMessage(TextUtil.fixColor(cooldownMessage));
                    }
                } else {
                    toggleFly(player);
                }
            } else if (item.getType() == Material.getMaterial(Objects.requireNonNull(Main.getInstance().getConfig().getString("items.lobby.material"))) && event.getAction().name().contains("RIGHT_CLICK")) {
                new LobbyGui().openMenu(player);
            } else if ((item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE) && event.getAction().name().contains("RIGHT_CLICK")) {
                long cooldown = getCooldownRemaining(player, hidePlayersCooldowns);
                int cooldown_hide_players = MessageManager.config.getInt("cooldown_hide_players");
                if (cooldown <= 0) {
                    toggleHiddenPlayers(player);
                    setCooldown(player, hidePlayersCooldowns, cooldown_hide_players);
                    ItemStack updatedHidePlayersItem = new ItemStack(hiddenPlayers.contains(player.getUniqueId()) ? Material.GRAY_DYE : Material.LIME_DYE);
                    ItemMeta updatedHidePlayersMeta = updatedHidePlayersItem.getItemMeta();
                    updatedHidePlayersMeta.setDisplayName(TextUtil.fixColor(String.valueOf(Main.getInstance().getConfig().getString("items.hide_players.name"))));
                    List<String> loreFromConfig = Main.getInstance().getConfig().getStringList("items.hide_players.lore");
                    List<String> updatedLore = new ArrayList<>();
                    for (String loreLine : loreFromConfig) {
                        updatedLore.add(TextUtil.fixColor(loreLine));
                    }
                    updatedHidePlayersMeta.setLore(updatedLore);
                    updatedHidePlayersItem.setItemMeta(updatedHidePlayersMeta);
                    player.getInventory().setItem(8, updatedHidePlayersItem);
                } else {
                    String cooldownMessage = MessageManager.config.getString("cooldown_message_hide_players");
                    cooldownMessage = cooldownMessage.replace("{cooldown}", String.valueOf(cooldown));
                    player.sendMessage(TextUtil.fixColor(cooldownMessage));
                }
            }
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        event.quitMessage(null);
        flyCooldowns.remove(player.getUniqueId());
        hidePlayersCooldowns.remove(player.getUniqueId());

        if (hiddenPlayers.contains(player.getUniqueId())) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(plugin, player);
            }
            hiddenPlayers.remove(player.getUniqueId());
        }
    }

    private void setCooldown(Player player, HashMap<UUID, Long> cooldowns, int seconds) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000));
    }

    private long getCooldownRemaining(Player player, HashMap<UUID, Long> cooldowns) {
        return Math.max(0, (cooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis()) / 1000);
    }

    private void toggleHiddenPlayers(Player player) {
        if (hiddenPlayers.contains(player.getUniqueId())) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                player.showPlayer(plugin, onlinePlayer);
            }
            hiddenPlayers.remove(player.getUniqueId());
            player.sendMessage(TextUtil.fixColor(Objects.requireNonNull(MessageManager.config.getString("show_players"))));
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                player.hidePlayer(plugin, onlinePlayer);
            }
            hiddenPlayers.add(player.getUniqueId());player.sendMessage(TextUtil.fixColor(Objects.requireNonNull(MessageManager.config.getString("hide_players"))));
        }
    }
    private void toggleFly(Player player) {
        if (player.hasPermission("m4code.lobby.fly")) {
            player.setAllowFlight(!player.getAllowFlight());
            if (player.getAllowFlight()) {
                player.sendMessage(TextUtil.fixColor(Objects.requireNonNull(MessageManager.config.getString("fly_enabled"))));
            } else {
                player.sendMessage(TextUtil.fixColor(Objects.requireNonNull(MessageManager.config.getString("fly_disabled"))));

            }
        } else {
            player.sendMessage(TextUtil.fixColor(Objects.requireNonNull(MessageManager.config.getString("no_permission"))));
        }
    }

    @EventHandler
    public void blockerBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void blockerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void blockerPlayerHit(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void blockerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerDragEvent(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("m4code.lobby.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        event.getInventory();
        event.getInventory().getType();
        if (!player.hasPermission("m4code.lobby.access")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}