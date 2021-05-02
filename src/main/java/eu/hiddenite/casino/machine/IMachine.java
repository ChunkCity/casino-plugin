package eu.hiddenite.casino.machine;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public interface IMachine {
    boolean isPlaying();
    void play(Player player);
    void destroy() throws Exception;
    int getId();
    Location getLeverLocation();
    Location getScreenLocation();
    BlockFace getLeverFacing();
    BlockFace getScreenFacing();
    int getInputPrice();
    void initScreen() throws Exception;

    enum MachineType {
        SLOT,
        FALL
    }
    MachineType getType();
}
