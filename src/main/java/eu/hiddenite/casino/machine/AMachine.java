package eu.hiddenite.casino.machine;

import eu.hiddenite.casino.CasinoPlugin;
import eu.hiddenite.casino.machine.fall.FallMachineResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;

@SuppressWarnings("unused")
public abstract class AMachine implements IMachine {
    protected final CasinoPlugin plugin;
    protected final int id;
    protected final Location leverLocation;
    protected final Location screenLocation;
    protected final BlockFace leverFacing;
    protected final BlockFace screenFacing;
    protected int inputPrice;
    protected boolean playing = false;
    protected FallMachineResult result = null;

    public abstract MachineType getType();

    protected AMachine(CasinoPlugin plugin, int id, Location leverLocation, Location screenLocation,
                       BlockFace leverFacing, BlockFace screenFacing, int inputPrice) throws Exception {
        this.plugin = plugin;
        this.id = id;
        this.leverLocation = leverLocation;
        this.screenLocation = screenLocation;
        this.leverFacing = leverFacing;
        this.screenFacing = screenFacing;
        this.inputPrice = inputPrice;
    }

    protected abstract void createScreen() throws Exception;

    public Location getLeverLocation() {
        return leverLocation;
    }

    public Location getScreenLocation() {
        return screenLocation;
    }

    public BlockFace getLeverFacing() { return leverFacing; }
    public BlockFace getScreenFacing() { return screenFacing; }

    public int getId() {
        return id;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getInputPrice() { return inputPrice; }

    public abstract void initScreen() throws Exception;

    protected void createLever() {
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
}
