package eu.hiddenite.casino.commands;

import eu.hiddenite.casino.CasinoPlugin;

import eu.hiddenite.casino.machine.IMachine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CasinoCommand implements CommandExecutor, TabCompleter {
    private final CasinoPlugin plugin;

    public CasinoCommand(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    private final String[] commands = {
            "machine",
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
            commandMachine(player, args);
        } else {
            plugin.sendMessage(player, "casino.messages.command-unknown", "{COMMAND}", args[0]);
        }
        return true;
    }

    private final String[] machineCommands = {
            "create",
            "delete",
    };

    private final List<String> casinoCreateCommands = Stream.of(IMachine.MachineType.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    private void commandMachine(Player player, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(player, "casino.messages.casino-machine-usage", "{SLOTCOMMANDS}",
                    String.join(", ", machineCommands));
            return;
        }
        if (args[1].equalsIgnoreCase(machineCommands[0])) {
            if (args.length < 4) {
                plugin.sendMessage(player, "casino.messages.casino-machine-create-usage");
                return;
            }
            try {
                var machineType = IMachine.MachineType.valueOf(args[2]);
                try {
                    var inputPrice = Integer.parseInt(args[3]);
                    plugin.getSlotMachineManager().setupSlotMachine(player, machineType, inputPrice);
                } catch (NumberFormatException error) {
                    plugin.sendMessage(player, "casino.messages.casino-machine-create-usage");
                    return;
                }
            } catch (IllegalArgumentException error) {
                plugin.sendMessage(player, "casino.messages.casino-machine-create-usage");
            }
        } else if (args[1].equalsIgnoreCase(machineCommands[1])) {
            if (args.length < 3) {
                plugin.sendMessage(player, "casino.messages.casino-machine-delete-usage");
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
        }  else if (args.length >= 2 && args[0].equalsIgnoreCase(commands[0])) {
            if (args.length == 2) {
                return Arrays.asList(machineCommands);
            }  else if (args.length == 3) {
                if (args[1].equalsIgnoreCase(machineCommands[0])) {
                    return casinoCreateCommands;
                }
                return Collections.emptyList();
            } else if (args.length == 4) {
                return Arrays.asList("10", "100", "1000", "10000");
            }
        }
        return Collections.emptyList();
    }
}
