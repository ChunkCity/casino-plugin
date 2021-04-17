package eu.hiddenite.casino;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import eu.hiddenite.casino.commands.CasinoCommand;
import eu.hiddenite.casino.machine.SlotMachineManager;

@SuppressWarnings("unused")
public class CasinoPlugin extends JavaPlugin {
    private SlotMachineManager slotMachineManager;
    private Database database;
    private Economy economy;

    public SlotMachineManager getSlotMachineManager() {
        return slotMachineManager;
    }

    public Database getDatabase() {
        return database;
    }
    public Economy getEconomy() {
        return economy;
    }

    @Override
    public void onEnable() {
        database = new Database(getConfig(), getLogger());
        economy = new Economy(database, getLogger());
        if (!database.open()) {
            getLogger().warning("Could not connect to the database. Plugin not enabled.");
            return;
        }
        try {
            slotMachineManager = new SlotMachineManager(this);
        } catch (Exception e) {
            getLogger().warning(String.format("Failed to load the plugin: %s.", e.getMessage()));
            return;
        }
        var casinoCommand = getCommand("casino");
        if (casinoCommand != null) {
            casinoCommand.setExecutor(new CasinoCommand(this));
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
    }

    public String getMessage(String configPath) {
        return Objects.toString(getConfig().getString(configPath), "");
    }

    public String formatMessage(String key, Object... parameters) {
        String msg = getMessage(key);
        for (int i = 0; i < parameters.length - 1; i += 2) {
            msg = msg.replace(parameters[i].toString(), parameters[i + 1].toString());
        }
        return msg;
    }

    public void sendMessage(Player player, String key, Object... parameters) {
        player.sendMessage(formatMessage(key, parameters));
    }
}