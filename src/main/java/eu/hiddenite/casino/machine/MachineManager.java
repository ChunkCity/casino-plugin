package eu.hiddenite.casino.machine;

import eu.hiddenite.casino.machine.fall.FallMachine;
import eu.hiddenite.casino.machine.slot.SlotMachine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import eu.hiddenite.casino.helpers.BlockHelper;
import eu.hiddenite.casino.CasinoPlugin;

import java.sql.SQLException;
import java.util.*;

public class MachineManager implements Listener {
    private final CasinoPlugin plugin;
    private final HashMap<Integer, IMachine> slotMachines = new HashMap<>();

    public MachineManager(CasinoPlugin plugin) throws Exception {
        this.plugin = plugin;
        loadSlotMachinesFromDB();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void buildSlotMachine(Player player, Block block) throws Exception {
        if (block.getType() == Material.LEVER) {
            if (temporarySlotMachineConfig.lever == null) {
                registerSlotMachineLever(player, block);
            }
        } else {
            if (temporarySlotMachineConfig.screen == null) {
                registerSlotMachineScreen(player, block);
            }
        }
        if (temporarySlotMachineConfig.lever != null && temporarySlotMachineConfig.screen != null) {
            var id = createSlotMachineInDB(temporarySlotMachineConfig);
            try {
                loadSlotMachineFromDB(id);
                plugin.sendMessage(player, "casino.messages.slot-machine-created", "{SLOTMACHINEID}",
                        id);
            } catch (Exception error) {
                deleteSlotMachineFromDB(id);
                plugin.sendMessage(player, "casino.messages.casino-slot-machine-creation-error");
                plugin.getLogger().warning(String.format("error: slot machine not created: %s",
                        error.getMessage()));
                error.printStackTrace();
            }
            temporarySlotMachineConfig = null;
        }
    }

    private void registerSlotMachineScreen(Player player, Block block) {
        temporarySlotMachineConfig.screen = block.getLocation();
        temporarySlotMachineConfig.screenFacing = player.getTargetBlockFace(50);
        plugin.sendMessage(player, "casino.messages.slot-machine-screen-built");
    }

    private void registerSlotMachineLever(Player player, Block block) {
        var id = 0;

        if (block.getType() != Material.LEVER) {
            plugin.sendMessage(player, "casino.messages.slot-machine-lever-not-found");
            return;
        }
        var alreadyAssignedId = getLeverIdAtLocation(block.getLocation());
        if (alreadyAssignedId.isPresent()) {
            plugin.sendMessage(player, "casino.messages.slot-machine-lever-already-registered",
                    "{SLOTMACHINEID}", id);
            return;
        }
        temporarySlotMachineConfig.lever = block.getLocation();
        temporarySlotMachineConfig.leverFacing = ((Switch)block.getBlockData()).getFacing();
        plugin.sendMessage(player, "casino.messages.slot-machine-lever-built");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlaceEvent(final BlockPlaceEvent event) {
        var player = event.getPlayer();
        if (temporarySlotMachineConfig == null || temporarySlotMachineConfig.player != player) {
            return;
        }
        var block = event.getBlock();
        try {
            buildSlotMachine(player, block);
        } catch (Exception error) {
            plugin.sendMessage(player, "casino.messages.slot-machine-creation-error");
            plugin.getLogger().warning(String.format("error: Failed to build slot machine: %s", error.getMessage()));
            error.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakEvent(final BlockBreakEvent event) {
        var block = event.getBlock();
        var player = event.getPlayer();
        if (block.getType() == Material.LEVER) {
            var id = getLeverIdAtLocation(block.getLocation());
            if (id.isPresent()) {
                try {
                    deleteSlotMachine(id.getAsInt());
                    plugin.sendMessage(player, "casino.messages.slot-machine-lever-deleted", "{SLOTMACHINEID}",
                            id.getAsInt());
                    plugin.getLogger().info(String.format("Slot machine %d deleted", id.getAsInt()));
                } catch (Exception error) {
                    plugin.sendMessage(player, "casino.messages.slot-machine-admin-error");
                    plugin.getLogger().warning(String.format("error: %s", error.getMessage()));
                    error.printStackTrace();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LEVER) {
            return;
        }
        var slotMachineId = getLeverIdAtLocation(block.getLocation());
        if (slotMachineId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        try {
            var slotMachine = slotMachines.get(slotMachineId.getAsInt());
            slotMachine.play(player);
            plugin.getLogger().warning(String.format("%s is playing %s machine %d",
                    player.getName(), slotMachine.getType().toString(), slotMachine.getId()));
        } catch (Exception error) {
            plugin.sendMessage(player,"casino.messages.slot-machine-user-error");
            plugin.getLogger().warning(String.format("error: slot machine %d: %s",
                    slotMachineId.getAsInt(), error.getMessage()));
        }
    }

    private OptionalInt getLeverIdAtLocation(Location location) {
        for (var slotMachine : slotMachines.values()) {
            if (BlockHelper.areBlockLocationsEqual(location, slotMachine.getLeverLocation())) {
                return OptionalInt.of(slotMachine.getId());
            }
        }
        return OptionalInt.empty();
    }

    private int createSlotMachineInDB(TemporarySlotMachineConfig slotMachine) throws Exception {
        var database = plugin.getDatabase();
        database.disableAutocommit();
        var insert_request =
                "INSERT INTO slot_machines " +
                        "(lever_x, lever_y, lever_z, screen_x, screen_y, screen_z, lever_facing, screen_facing, type, input_price) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        var insert_statement = database.prepareStatement(insert_request);
        insert_statement.setInt(1, slotMachine.lever.getBlockX());
        insert_statement.setInt(2, slotMachine.lever.getBlockY());
        insert_statement.setInt(3, slotMachine.lever.getBlockZ());
        insert_statement.setInt(4, slotMachine.screen.getBlockX());
        insert_statement.setInt(5, slotMachine.screen.getBlockY());
        insert_statement.setInt(6, slotMachine.screen.getBlockZ());
        insert_statement.setString(7, blockFaceToString(slotMachine.leverFacing));
        insert_statement.setString(8, blockFaceToString(slotMachine.screenFacing));
        insert_statement.setString(9, slotMachine.machineType.toString());
        insert_statement.setInt(10, slotMachine.inputPrice);

        var select_request = "SELECT LAST_INSERT_ID()";
        var select_statement = database.prepareStatement(select_request);

        insert_statement.executeUpdate();
        var select_result = select_statement.executeQuery();
        database.commit();
        database.enableAutocommit();

        if (!select_result.next()) {
            throw new Exception(
                    "Expected a new ID after inserting the new slot machine in the Database but none was found");
        }
        return select_result.getInt(1);
    }

    private void updateSlotMachine(SlotMachine slotMachine) throws SQLException {
        var request =
                "REPLACE INTO slot_machines " +
                        "(slot_machine_id, lever_x, lever_y, lever_z, screen_x, screen_y, screen_z, " +
                        "lever_facing, screen_facing, type, input_price) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        var statement = plugin.getDatabase().prepareStatement(request);
        statement.setInt(1, slotMachine.getId());
        statement.setInt(2, slotMachine.getLeverLocation().getBlockY());
        statement.setInt(3, slotMachine.getLeverLocation().getBlockZ());
        statement.setInt(4, slotMachine.getLeverLocation().getBlockX());
        statement.setInt(5, slotMachine.getScreenLocation().getBlockX());
        statement.setInt(6, slotMachine.getScreenLocation().getBlockY());
        statement.setInt(7, slotMachine.getScreenLocation().getBlockZ());
        statement.setString(8, blockFaceToString(slotMachine.getLeverFacing()));
        statement.setString(9, blockFaceToString(slotMachine.getScreenFacing()));
        statement.setString(10, slotMachine.getType().toString());
        statement.setInt(11, slotMachine.getInputPrice());
        statement.executeQuery();
    }

    private void deleteSlotMachine(int id) throws Exception {
        if (!slotMachines.containsKey(id)) {
            throw new Exception(String.format("Could not delete slot machine %d as it does not exist", id));
        }
        deleteSlotMachineFromDB(id);
        slotMachines.get(id).destroy();
        slotMachines.remove(id);
    }

    private void deleteSlotMachineFromDB(int id) throws Exception {
        var request = "DELETE FROM slot_machines WHERE slot_machine_id = ?";
        var statement = plugin.getDatabase().prepareStatement(request);
        statement.setInt(1, id);
        var row_deleted = statement.executeUpdate();
        if (row_deleted != 1) {
            throw new Exception(
                    String.format("%d row deleted in slot_machines tables where only 1 was expected", row_deleted));
        }
    }

    private void loadSlotMachinesFromDB() throws Exception {
        var worldName = plugin.getConfig().getString("world-name");
        if (worldName == null) {
            throw new Exception("Missing world-name field in config");
        }
        var world = Bukkit.getWorld(worldName);
        var ps = plugin.getDatabase().prepareStatement(
                "SELECT slot_machine_id, lever_x, lever_y, lever_z, screen_x, screen_y, screen_z, " +
                        "lever_facing, screen_facing, type, input_price " +
                        "FROM slot_machines");
        try (var rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("slot_machine_id");
                loadMachineFromRequest(rs, world, id);
            }
        }
        plugin.getLogger().info("Loaded slot machines");
    }

    private void loadSlotMachineFromDB(int id) throws Exception {
        var worldName = plugin.getConfig().getString("world-name");
        if (worldName == null) {
            throw new Exception("Missing world-name field in config");
        }
        var world = Bukkit.getWorld(worldName);
        var ps = plugin.getDatabase().prepareStatement(
                "SELECT slot_machine_id, lever_x, lever_y, lever_z, screen_x, screen_y, screen_z, " +
                        "lever_facing, screen_facing, type, input_price " +
                "FROM slot_machines WHERE slot_machine_id = ?");
        ps.setInt(1, id);
        var rs = ps.executeQuery();
        if (!rs.next()) {
            throw new Exception(String.format("Slot Machine %d not found in DB", id));
        }
        int id_from_rs = rs.getInt("slot_machine_id");
        if (id_from_rs != id) {
            throw new Exception(String.format("Looking for Slot Machine id %d but received id %d", id, id_from_rs));
        }
        loadMachineFromRequest(rs, world, id);
    }

    private void loadMachineFromRequest(java.sql.ResultSet rs, World world, int id) throws Exception {
        var leverPosition = new Location(
                world,
                rs.getInt("lever_x"),
                rs.getInt("lever_y"),
                rs.getInt("lever_z")
        );
        var screenPosition = new Location(
                world,
                rs.getInt("screen_x"),
                rs.getInt("screen_y"),
                rs.getInt("screen_z")
        );
        var leverFacing = stringToBlockFace(rs.getString("lever_facing"));
        var screenFacing = stringToBlockFace(rs.getString("screen_facing"));
        var typeString = rs.getString("type");
        var type =
                typeString.isEmpty() ? IMachine.MachineType.SLOT : IMachine.MachineType.valueOf(typeString);
        var inputPrice = rs.getInt("input_price");
        var machine = instantiateMachine(
                type, id, leverPosition, screenPosition, leverFacing, screenFacing, inputPrice);
        slotMachines.put(id, machine);
        plugin.getLogger().info(String.format("[SlotMachine] Loaded slot machine %d", id));
    }

    private IMachine instantiateMachine(IMachine.MachineType machineType, int id,
                                        Location leverPosition, Location screenPosition,
                                        BlockFace leverFacing, BlockFace screenFacing, int inputPrice) throws Exception {
        switch (machineType) {
            case FALL:
                return new FallMachine(
                        plugin, id, leverPosition, screenPosition, leverFacing, screenFacing, inputPrice);
            case SLOT:
                return new SlotMachine(
                        plugin, id, leverPosition, screenPosition, leverFacing, screenFacing, inputPrice);
            default:
                throw new IllegalArgumentException(String.format("Unsupported machine type %s", machineType));
        }
    }

    private String blockFaceToString(BlockFace blockFace) {
        return blockFace.toString();
    }

    private BlockFace stringToBlockFace(String face) throws Exception {
        switch (face) {
            case "NORTH": return BlockFace.NORTH;
            case "SOUTH": return BlockFace.SOUTH;
            case "WEST": return BlockFace.WEST;
            case "EAST": return BlockFace.EAST;
            default: throw new Exception(String.format("Found unexpected face: %s", face));
        }
    }

    private static class TemporarySlotMachineConfig {
        public Location screen = null;
        public Location lever = null;
        public BlockFace screenFacing = null;
        public BlockFace leverFacing = null;
        public Player player = null;
        public IMachine.MachineType machineType = null;
        public int inputPrice = 0;
    }

    private TemporarySlotMachineConfig temporarySlotMachineConfig = null;

    public void setupSlotMachine(Player player, IMachine.MachineType machineType, int inputPrice) {
        temporarySlotMachineConfig = new TemporarySlotMachineConfig();
        temporarySlotMachineConfig.player = player;
        temporarySlotMachineConfig.machineType = machineType;
        temporarySlotMachineConfig.inputPrice = inputPrice;
        plugin.sendMessage(player, "casino.messages.slot-machine-screen-initiate-building");
    }

    public void removeSlotMachine(Player player, int id) throws Exception {
        deleteSlotMachine(id);
        plugin.sendMessage(player, "casino.messages.slot-machine-deleted", "{SLOTMACHINEID}", id);
    }
}
