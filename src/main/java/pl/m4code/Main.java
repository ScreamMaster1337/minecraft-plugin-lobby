package pl.m4code;

import fr.minuskube.inv.InventoryManager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import pl.m4code.guis.LobbyGui;
import pl.m4code.listeners.*;
import pl.m4code.system.MessageManager;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public final class Main extends JavaPlugin {
    @Getter private static Main instance;

    @Getter private static InventoryManager invManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new MessageManager();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new LobbyGui(), this);



        try {
            registerCommands();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        registerListeners();
        registerTasks();


        invManager = new InventoryManager(this);
        invManager.init();

    }

    @SneakyThrows
    private void registerCommands() throws NoSuchFieldException, IllegalAccessException {
        final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);
        final CommandMap commandMap = (CommandMap) bukkitCommandMap.get(getServer());
        List.of(
        ).forEach(commands ->
                commandMap.register("m4code-lobby", (Command) commands)
        );
    }


    private void registerListeners() {
        new Listeners(this);

    }


    private void registerTasks() {

    }


}
