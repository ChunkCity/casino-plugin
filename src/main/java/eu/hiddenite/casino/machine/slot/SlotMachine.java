package eu.hiddenite.casino.machine.slot;

import eu.hiddenite.casino.CasinoPlugin;
import eu.hiddenite.casino.Economy;
import eu.hiddenite.casino.machine.AMachine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SlotMachine extends AMachine {
    protected ArrayList<SlotMachineRow> rows = new ArrayList<>();

    public SlotMachine(CasinoPlugin plugin, int id, Location leverLocation, Location screenLocation,
                       BlockFace leverFacing, BlockFace screenFacing, int inputPrice) throws Exception {
        super(plugin, id, leverLocation, screenLocation, leverFacing, screenFacing, inputPrice);
        createLever();
        createScreen();
    }

    @Override
    public MachineType getType() {
        return MachineType.SLOT;
    }

    @Override
    public void play(Player player) {
        if (playing) {
            if (playerPlaying == null) {
                plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
                plugin.getLogger().warning(
                        "Tried to play the slot machine but it is already playing without a player");
            } else if (player != playerPlaying) {
                plugin.sendActionBar(player, "casino.messages.slot-machine-already-playing", "{PLAYER}",
                        playerPlaying.getName());
            } else {
                plugin.sendActionBar(player, "casino.messages.slot-machine-already-playing-self");
            }
            return;
        }

        var result = plugin.getEconomy().removeMoney(player.getUniqueId(), inputPrice);
        if (result == Economy.ResultType.NOT_ENOUGH_MONEY) {
            plugin.sendActionBar(player, "casino.messages.slot-machine-player-too-poor", "{PRICE}",
                    inputPrice);
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
        plugin.sendActionBar(player, "casino.messages.slot-machine-debit", "{PRICE}", inputPrice);
        playing = true;
        playerPlaying = player;
        waitForRows(player);
    }

    private Player playerPlaying = null;
    private void waitForRows(Player player) {
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

    public void initScreen() throws Exception {
        for (var row : rows) {
            row.createRow();
        }
    }

    private void processResult(Player player) {
        var world = screenLocation.getWorld();
        var results = new ArrayList<Integer>();
        var money = 0;
        for (var row : rows) {
            results.add(row.resultId());
        }

        if (results.get(0).equals(results.get(1)) && results.get(0).equals(results.get(2))) {
            switch (results.get(0)) {
                case 0:
                    money = 5 * inputPrice;
                    break;
                case 1:
                    money = 125 * inputPrice / 10;
                    break;
                case 2:
                    money = 25 * inputPrice;
                    break;
                case 3:
                    money = 50 * inputPrice;
                    break;
                default:
                    money = inputPrice;
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

    private void resetLever() {
        var block = leverLocation.getBlock();
        if (block.getType() != Material.LEVER) {
            return;
        }
        var leverData = (Switch) block.getBlockData();
        leverData.setPowered(false);
        block.setBlockData(leverData);
    }

    @Override
    protected void createScreen() throws Exception {
        var anchorBlock = screenLocation.getBlock();
        rows.add(new SlotMachineRow(plugin, anchorBlock, 0, screenFacing, false));
        rows.add(new SlotMachineRow(plugin, anchorBlock, 1, screenFacing, true));
        rows.add(new SlotMachineRow(plugin, anchorBlock, 2, screenFacing, false));
    }

    @Override
    public void destroy() throws Exception {
        leverLocation.getBlock().breakNaturally();
        for (var row : rows) {
            row.destroy();
        }
    }
}
