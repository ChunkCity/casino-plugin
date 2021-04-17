package eu.hiddenite.casino.helpers;

import org.bukkit.Location;

public class BlockHelper {
    public static boolean areBlockLocationsEqual(Location locationA, Location locationB) {
        return locationA.getBlockX() == locationB.getBlockX()
                && locationA.getBlockY() == locationB.getBlockY()
                && locationA.getBlockZ() == locationB.getBlockZ();
    }
}