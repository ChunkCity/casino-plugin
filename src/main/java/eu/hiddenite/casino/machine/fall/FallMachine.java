package eu.hiddenite.casino.machine.fall;

import eu.hiddenite.casino.CasinoPlugin;
import eu.hiddenite.casino.Economy;
import eu.hiddenite.casino.machine.AMachine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FallMachine extends AMachine {
    protected ArrayList<FallMachineRow> rows = new ArrayList<>();

    public FallMachine(CasinoPlugin plugin, int id, Location leverLocation, Location screenLocation,
                       BlockFace leverFacing, BlockFace screenFacing, int inputPrice) throws Exception {
        super(plugin, id, leverLocation, screenLocation, leverFacing, screenFacing, inputPrice);
        createLever();
        createScreen();
    }

    @Override
    public MachineType getType() {
        return MachineType.FALL;
    }

    public void destroy() throws Exception {
        leverLocation.getBlock().breakNaturally();
        for (var row : rows) {
            row.destroy();
        }
    }

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
        screenLocation.getWorld().playSound(screenLocation.toCenterLocation(), Sound.BLOCK_LAVA_POP, 1.0f, 0.5f);
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
        this.result = new FallMachineResult(inputPrice);
        int id = 0;
        for (var row : rows) {
            try {
                row.play(player, Arrays.asList(
                        this.result.getRows().get(0).get(id),
                        this.result.getRows().get(1).get(id),
                        this.result.getRows().get(2).get(id)
                ));
                id += 1;
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
        if (this.result.getFinalScore() > 0) {
            world.playSound(screenLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);
            plugin.sendMessage(player, "casino.messages.fall-machine-gain",
                    "{GAIN}",
                    this.result.getGain(),
                    "{COMBINATIONS}",
                    this.result.getMessage());
            plugin.getEconomy().addMoney(player.getUniqueId(), this.result.getGain());
        } else {
            world.playSound(screenLocation, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.01f);
        }
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


    protected void createScreen() throws Exception {
        var anchorBlock = screenLocation.getBlock();
        rows.add(new FallMachineRow(plugin, anchorBlock, 0, screenFacing, false));
        rows.add(new FallMachineRow(plugin, anchorBlock, 1, screenFacing, true));
        rows.add(new FallMachineRow(plugin, anchorBlock, 2, screenFacing, false));
    }
}
