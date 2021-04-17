package eu.hiddenite.casino.machine;

import eu.hiddenite.casino.CasinoPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import static java.lang.Math.exp;
import static java.lang.Math.log;

@SuppressWarnings("unused")
public class SlotMachineRow {
    private final int rowSize = 5;

    private final CasinoPlugin plugin;
    private final Block anchor;
    private final int height;
    private final BlockFace screenFacing;
    private final boolean reverse;

    public SlotMachineRow(CasinoPlugin plugin, Block anchor, int height, BlockFace screenFacing, boolean reverse) throws Exception {
        this.plugin = plugin;
        this.anchor = anchor;
        this.height = height;
        this.screenFacing = screenFacing;
        this.reverse = reverse;
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
        return world.getBlockAt(anchor.getX() + iteration[0], anchor.getY() + height, anchor.getZ() + iteration[1]);
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
            block.breakNaturally();
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
    private int totalRound = 0;

    public void play(Player player) {
        playing = true;
        var randomInt = 100 + (int)(Math.random() * 10) % 10;
        round = 10 + randomInt;
        totalRound = round;
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
                anchor.getY() + height,
                anchor.getZ()
        ).getLocation();
        world.playSound(location, Sound.BLOCK_LANTERN_PLACE, 0.05f, 2.5f);
        round -= 1;
        if (round == 0) {
            result = getRowValue();
            playing = false;
        } else {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                    plugin, () -> roundChecked(player),
                    (long) exp(log((double)(totalRound - round + 1) / 35)));
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
