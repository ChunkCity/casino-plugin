package eu.hiddenite.casino.machine.slot;

import eu.hiddenite.casino.CasinoPlugin;
import eu.hiddenite.casino.helpers.BlockHelper;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import static java.lang.Math.*;

@SuppressWarnings("unused")
public class SlotMachineRow {
    private final int rowSize = 5;

    private final CasinoPlugin plugin;
    private final Block anchor;
    private final int position;
    private final BlockFace screenFacing;
    private final boolean reverse;

    public SlotMachineRow(
            CasinoPlugin plugin, Block anchor, int position, BlockFace screenFacing, boolean reverse) throws Exception {
        this.plugin = plugin;
        this.anchor = anchor;
        this.position = position;
        this.screenFacing = screenFacing;
        this.reverse = reverse;
    }

    public void createRow() throws Exception {
        iterate();
    }

    private Block getBlock(int id) throws Exception {
        var world = anchor.getWorld();
        var iteration = getIteration();
        if (reverse) {
            id = rowSize - id - 1;
        }
        iteration[0] *= (id - rowSize / 2);
        iteration[1] *= (id - rowSize / 2);
        return world.getBlockAt(anchor.getX() + iteration[0], anchor.getY() + position, anchor.getZ() + iteration[1]);
    }

    private int[] getIteration() throws Exception {
        switch (screenFacing) {
            case NORTH:
                return new int[]{1, 0};
            case SOUTH:
                return new int[]{-1, 0};
            case WEST:
                return new int[]{0, 1};
            case EAST:
                return new int[]{0, -1};
            default:
                throw new Exception(String.format("Unknown block face: %s", screenFacing));
        }
    }

    public void destroy() throws Exception {
        var world = anchor.getWorld();
        var start = anchor.getX() - (rowSize / 2);
        for (var i = 0; i < rowSize; i += 1) {
            var block = getBlock(i);
            BlockHelper.breakNaturallyNoDrop(block);
        }
    }

    private final Material[] materialValue = {
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.NETHERITE_BLOCK,
    };

    private int getRowValue() throws Exception {
        var type = getBlock(rowSize/2).getType();
        for (var i = 0; i < materialValue.length; i += 1) {
            if (materialValue[i] == type) {
                return i;
            }
        }
        throw new Exception("Block value not found");
    }

    private final Material[] materials = {
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.EMERALD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.NETHERITE_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.IRON_BLOCK,
    };

    private boolean playing = false;
    private int round = 0;

    public void play(Player player) {
        playing = true;
        round = 50;
        roundChecked(player);
    }

    private void roundChecked(Player player) {
        try {
            round(player);
        } catch (Exception e) {
            plugin.sendMessage(player, "casino.messages.slot-machine-user-error");
            e.printStackTrace();
        }
    }

    private int result = 0;
    private void round(Player player) throws Exception {
        if (!playing) {
            return;
        }
        iterate();
        var world = anchor.getWorld();
        var location = world.getBlockAt(
                anchor.getX(),
                anchor.getY() + position,
                anchor.getZ()
        ).getLocation();
        world.playSound(location, Sound.BLOCK_LANTERN_PLACE, 0.05f, 2.5f);
        round -= 1;
        if (round == 0) {
            result = getRowValue();
            playing = false;
        } else {
            var ticks = exp(-((round)) / (10.0 + position * 10.0)) * 10.0 + 1.0;
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                    plugin, () -> roundChecked(player),
                    (long) ticks);
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    private int iteration = 0;
    public void iterate() throws Exception {
        iteration += 1;
        if (iteration == materials.length) {
            iteration = 0;
        }
        for (var id = 0; id < rowSize; id += 1) {
            var offset = reverse ? rowSize - id - 1 : id;
            var materialId = reverse ? materials.length - iteration - 1 : iteration;
            var material = materials[(id + materialId) % materials.length];
            var block = getBlock(offset);
            block.setType(material);

        }
    }

    public int resultId() {
        return result;
    }
}
