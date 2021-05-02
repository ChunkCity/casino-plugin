package eu.hiddenite.casino;

import java.util.*;
import java.util.stream.Collectors;

import eu.hiddenite.casino.machine.fall.FallMachineResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import eu.hiddenite.casino.commands.CasinoCommand;
import eu.hiddenite.casino.machine.MachineManager;

public class CasinoPlugin extends JavaPlugin {
    private MachineManager machineManager;
    private Database database;
    private Economy economy;

    public MachineManager getSlotMachineManager() {
        return machineManager;
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
            machineManager = new MachineManager(this);
        } catch (Exception e) {
            getLogger().warning(String.format("Failed to load the plugin: %s.", e.getMessage()));
            e.printStackTrace();
            return;
        }
        var casinoCommand = getCommand("casino");
        if (casinoCommand != null) {
            casinoCommand.setExecutor(new CasinoCommand(this));
        }
    }

    void testScore() {
        var results = new HashMap<Integer, Integer>();
        for (var i = 0; i < 100000000; i += 1) {
            var rows = buildRows();
            var result = new FallMachineResult(rows, 1);
            results.put(result.getFinalScore(), results.getOrDefault(result.getFinalScore(), 0) + 1);
        }
        for (var result : results.entrySet()) {
            var score = result.getKey();
            var count = result.getValue();
            getLogger().warning(String.format("%d : %d", score, count));
        }
    }

    private List<List<Integer>> buildRows() {
        List<List<Integer>> rows = new ArrayList<>();
        for (var i = 0; i < 3; i += 1) {
            List<Integer> row = new ArrayList<>();
            for (var j = 0; j < 3; j += 1) {
                var score = (Math.random() * 5 % 5) + 1;
                row.add((int)score);
            }
            rows.add(row);
        }
        return rows;
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

    public Component formatComponent(String key, Object... parameters) {
        return LegacyComponentSerializer.legacySection().deserialize(formatMessage(key, parameters))
                .decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> formatComponents(String key, Object... parameters) {
        String message = formatMessage(key, parameters);
        return Arrays.stream(message.split("\n"))
                .map(x -> LegacyComponentSerializer.legacySection().deserialize(x)
                        .decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
    }

    private Component actionBarMessage = null;

    private void reSendActionBar(Player player) {
        if (actionBarMessage != null) {
            player.sendActionBar(actionBarMessage);
        }
    }

    public void sendActionBarLong(Player player, String key, Object... parameters) {
        Component component = formatComponent(key, parameters);
        actionBarMessage = component;
        player.sendActionBar(actionBarMessage);
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> reSendActionBar(player), 40);
    }

    public void sendActionBar(Player player, String key, Object... parameters) {
        Component component = formatComponent(key, parameters);
        actionBarMessage = null;
        player.sendActionBar(component);
    }
}