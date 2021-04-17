package eu.hiddenite.casino.machine;

import eu.hiddenite.casino.CasinoPlugin;
import eu.hiddenite.casino.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class SlotMachine {
    private final CasinoPlugin plugin;
    private final int id;
    private final Location leverLocation;
    private final Location screenLocation;
    private final BlockFace leverFacing;
    private final BlockFace screenFacing;
    private final ArrayList<SlotMachineRow> rows = new ArrayList<>();

    public SlotMachine(CasinoPlugin plugin, int id, Location leverLocation, Location screenLocation,
                       BlockFace leverFacing, BlockFace screenFacing) throws Exception {
        this.plugin = plugin;
        this.id = id;
        this.leverLocation = leverLocation;
        this.screenLocation = screenLocation;
        this.leverFacing = leverFacing;
        this.screenFacing = screenFacing;
        createLever();
        createScreen();
    }

    public void destroy() throws Exception {
        leverLocation.getBlock().breakNaturally();
        for (var row : rows) {
            row.destroy();
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void play(Player player) {
        if (playing) {
            if (playerPlaying == null) {
                plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
                plugin.getLogger().warning(
                        "Tried to play the slot machine but it is already playing without a player");
            } else if (player != playerPlaying) {
                plugin.sendMessage(player, "casino.messages.slot-machine-already-playing", "{PLAYER}",
                        playerPlaying.getName());
            } else {
                plugin.sendMessage(player, "casino.messages.slot-machine-already-playing-self");
            }
            return;
        }

        long slotMachineCost = 20000;
        var result = plugin.getEconomy().removeMoney(player.getUniqueId(), slotMachineCost);
        if (result == Economy.ResultType.NOT_ENOUGH_MONEY) {
            plugin.sendMessage(player, "casino.messages.slot-machine-player-too-poor", "{PRICE}",
                    slotMachineCost);
            return;
        } else if (result != Economy.ResultType.SUCCESS) {
            plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
            plugin.getLogger().warning(String.format("error: while %s tried to pay for the slot machine: %s",
                    player.getName(), result.toString()));
            return;
        }

        for (var row : rows) {
            try {
                row.play(player);
            } catch (Exception e) {
                plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
                plugin.getLogger().warning(
                        String.format("error: failed to play slot machine %d: %s", id, e.getMessage()));
                e.printStackTrace();
                playing = false;
                playerPlaying = null;
                return;
            }
        }

        var leverData = (Switch) leverLocation.getBlock().getBlockData();
        leverData.setPowered(true);
        leverLocation.getBlock().setBlockData(leverData);
        plugin.sendMessage(player, "casino.messages.slot-machine-debit", "{PRICE}", slotMachineCost);
        playing = true;
        playerPlaying = player;
        waitForRows(player);
    }

    private boolean playing = false;
    private Player playerPlaying = null;
    public void waitForRows(Player player) {
        if (!playing || playerPlaying == null || player != playerPlaying) {
            plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
            plugin.getLogger().warning("error: tried to play the slot machine without initializing it beforehand");
            return;
        }
        playing = false;
        for (var row : rows) {
            if (row.isPlaying()) {
                playing = true;
                break;
            }
        }
        if (playing) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> waitForRows(player), 10L);
        } else {
            processResult(player);
            resetLever();
            playerPlaying = null;
        }
    }

    private void processResult(Player player) {
        var world = screenLocation.getWorld();
        var results = new ArrayList<Integer>();
        var money = 0;
        for (var row : rows) {
            results.add(row.resultId());
        }

        var duplicate = duplicates(results);
        if (results.get(0).equals(results.get(1)) && results.get(0).equals(results.get(2))) {
            switch (results.get(0)) {
                case 0:
                    money = 100000;
                    break;
                case 1:
                    money = 250000;
                    break;
                case 2:
                    money = 500000;
                    break;
                case 3:
                    money = 1000000;
                    break;
                default:
                    money = 100000;
                    plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
                    plugin.getLogger().warning(String.format("error: unknown row result id %d", results.get(0)));
            }
            world.playSound(screenLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);
        } else {
            world.playSound(screenLocation, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.01f);
            return;
        }
        plugin.getEconomy().addMoney(player.getUniqueId(), money);
        plugin.sendMessage(player, "casino.messages.slot-machine-gain", "{GAIN}", money);

    }

    int duplicates(final ArrayList<Integer> values)
    {
        Set<Integer> lump = new HashSet<>();
        for (int i : values)
        {
            if (lump.contains(i)) return i;
            lump.add(i);
        }
        return -1;
    }

    private void resetLever() {
        var block = leverLocation.getBlock();
        if (block.getType() != Material.LEVER) {
            return;
        }
        var leverData = (Switch) block.getBlockData();
        leverData.setPowered(false);
        block.setBlockData(leverData);
    }

    private void createLever() {
        if (leverLocation.getBlock().getType() == Material.LEVER
                && ((Switch)leverLocation.getBlock().getBlockData()).getFacing() == this.leverFacing) {
            return;
        }
        var block = leverLocation.getBlock();
        block.setType(Material.LEVER);
        var lever = (Switch)block.getBlockData();
        lever.setFacing(leverFacing);
        block.setBlockData(lever);
    }

    private void createScreen() throws Exception {
        var anchorBlock = screenLocation.getBlock();
        rows.add(new SlotMachineRow(plugin, anchorBlock, 0, screenFacing, false));
        rows.add(new SlotMachineRow(plugin, anchorBlock, 1, screenFacing, true));
        rows.add(new SlotMachineRow(plugin, anchorBlock, 2, screenFacing, false));
    }

    public int getId() {
        return id;
    }

    public Location getLeverLocation() {
        return leverLocation;
    }

    public Location getScreenLocation() {
        return screenLocation;
    }

    public BlockFace getLeverFacing() { return leverFacing; }
    public BlockFace getScreenFacing() { return screenFacing; }
}
