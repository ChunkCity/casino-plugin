package eu.hiddenite.casino.commands;

import eu.hiddenite.casino.CasinoPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class CasinoCommand implements CommandExecutor, TabCompleter {
    private final CasinoPlugin plugin;

    public CasinoCommand(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    private final String[] commands = {
            "slot",
    };

    @Override
    public boolean onCommand(@Nonnull final CommandSender sender,
                             @Nonnull final Command command,
                             @Nonnull final String alias,
                             @Nonnull final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player)sender;

        if (args.length < 1) {
            plugin.sendMessage(player, "casino.messages.casino-usage", "{SUBCOMMANDS}",
                    String.join(", ", commands));
            return true;
        }
        if (args[0].equalsIgnoreCase(commands[0])) {
            commandSlot(player, args);
        } else {
            plugin.sendMessage(player, "casino.messages.command-unknown", "{COMMAND}", args[0]);
        }
        return true;
    }

    private final String[] slotCommands = {
            "create",
            "delete",
    };

    private void commandSlot(Player player, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(player, "casino.messages.casino-slot-usage", "{SLOTCOMMANDS}",
                    String.join(", ", slotCommands));
            return;
        }
        if (args[1].equalsIgnoreCase(slotCommands[0])) {
            plugin.getSlotMachineManager().setupSlotMachine(player);
        } else if (args[1].equalsIgnoreCase(slotCommands[1])) {
            if (args.length < 3) {
                plugin.sendMessage(player, "casino.messages.casino-slot-delete-usage");
                return;
            }
            var id = Integer.parseInt(args[2]);
            try {
                plugin.getSlotMachineManager().removeSlotMachine(player, id);
            } catch (Exception error) {
                var error_message = String.format("error: %s", error.getMessage());
                plugin.sendMessage(player, "casino.messages.casino-command-error");
                plugin.getLogger().warning(error_message);
            }
        }
    }

    @Override
    public List<String> onTabComplete(@Nonnull final CommandSender sender,
                                      @Nonnull final Command command,
                                      @Nonnull final String alias,
                                      @Nonnull final String[] args) {
        if (args.length == 1) {
            return Arrays.asList(commands);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase(commands[0])) {
            return Arrays.asList(slotCommands);
        }
        return Collections.emptyList();
    }
}
