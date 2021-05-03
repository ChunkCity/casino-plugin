package eu.hiddenite.casino.machine.fall;

import eu.hiddenite.casino.CasinoPlugin;
import eu.hiddenite.casino.helpers.BlockHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FallMachineRow {
    private final int rowSize = 3;

    private final CasinoPlugin plugin;
    private final Block anchor;
    private final int position;
    private final BlockFace screenFacing;

    private final Material[] materialValue = {
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.NETHERITE_BLOCK,
    };

    public FallMachineRow(CasinoPlugin plugin, Block anchor, int position, BlockFace screenFacing) {
        this.plugin = plugin;
        this.anchor = anchor;
        this.position = position;
        this.screenFacing = screenFacing;
    }

    private final ArrayList row = new ArrayList();

    private void clearEntities() throws Exception {
        var world = anchor.getWorld();
        for (var i = 0; i < rowSize + 1; i += 1) {
            var location = getLocation();
            location.setY(location.getY() - i);
            var block = world.getBlockAt(location);
            BlockHelper.breakNaturallyNoDrop(block);
            var entities = world.getNearbyEntities(location, 1, 1, 1);
            for (var entity : entities) {
                entity.remove();
            }
        }
    }

    public void createRow() throws Exception {
        clearEntities();
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> blockFallRec(0), position * 10L);
    }

    private void blockFallRec(int id) {
        playing = true;
        try {
            var world = anchor.getWorld();
            var location = getLocation();
            location.setX(location.getX() + 0.5);
            location.setZ(location.getZ() + 0.5);
            world.playSound(location, Sound.BLOCK_BELL_USE, 0.05f, 2.5f);
            var value = rowValues.get(id) - 1;
            var block = createCube(value, location);
            row.add(block);
        } catch (Exception error) {
            error.printStackTrace();
        }
        if (id == rowSize - 1) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                    plugin, () -> { playing = false; }, 10L);
        } else  {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                plugin, () -> blockFallRec(id + 1), 30L + (long)id * 10 * (long)(position + 1));
        }
    }

    private Entity createCube(int id, Location location) {
        var world = anchor.getWorld();
        var data = Bukkit.createBlockData(materialValue[id]);
        var cube = (FallingBlock)world.spawnFallingBlock(location, data);
        cube.setVelocity(new Vector(0,-0.20,0));
        cube.setGravity(false);
        cube.setDropItem(true);
        return cube;
    }

    private Location getLocation() throws Exception {
        var world = anchor.getWorld();
        var iteration = getIteration();
        return new Location(world,
                (double)anchor.getX() + iteration[0] * (position - 1),
                (double)anchor.getY() + rowSize,
                (double)anchor.getZ() + iteration[1] * (position - 1)
        );
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
        clearEntities();
    }

    private boolean playing = false;
    private List<Integer> rowValues = Arrays.asList(getRandomValue(), getRandomValue(), getRandomValue());

    private int getRandomValue() {
        return (int)(Math.random() * materialValue.length % materialValue.length + 1);
    }

    public void play(Player player, List<Integer> rowValues) throws Exception {
        if (playing) {
            return;
        }
        this.rowValues = rowValues;
        playing = true;
        createRow();
    }

    public boolean isPlaying() {
        return playing;
    }
}
