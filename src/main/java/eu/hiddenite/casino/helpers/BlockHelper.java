package eu.hiddenite.casino.helpers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;

public class BlockHelper {
    public static boolean areBlockLocationsEqual(Location locationA, Location locationB) {
        return locationA.getBlockX() == locationB.getBlockX()
                && locationA.getBlockY() == locationB.getBlockY()
                && locationA.getBlockZ() == locationB.getBlockZ();
    }

    public static void breakNaturallyNoDrop(Block block) {
        block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 256, block.getBlockData());
        block.setType(Material.AIR);
    }
}