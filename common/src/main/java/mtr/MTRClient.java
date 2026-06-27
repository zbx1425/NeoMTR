package mtr;

import cn.zbx1425.mtrsteamloco.gui.RailEditorVisualScreen;
import com.lx862.tprobe3.packet.PacketTProbeRequester;
import mtr.block.*;
import mtr.client.ClientData;
import mtr.client.Config;
import mtr.client.IDrawing;
import mtr.client.VehiclePlayerMovementTracker;
import mtr.data.*;
import mtr.packet.IPacket;
import mtr.packet.PacketTrainDataGuiClient;
import com.lx862.tprobe3.packet.TProbePackets;
import mtr.render.*;
import mtr.servlet.Webserver;
import mtr.sound.train.LoopingSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class MTRClient implements IPacket {

	private static boolean isReplayMod;
	private static boolean isVivecraft;
	private static boolean isPehkui;
	private static double gameTick = 0;
	private static double lastPlayedTrainSoundsTick = 0;

	public static final int TICKS_PER_SPEED_SOUND = 4;
	public static final LoopingSoundInstance TACTILE_MAP_SOUND_INSTANCE = new LoopingSoundInstance("tactile_map_music");

	public static void init() {
		if (!Keys.LIFTS_ONLY) {
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ARRIVAL_PROJECTOR_1_SMALL_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, 12, 1, 1, 15, 16, 14, 14, false, false, PIDSType.ARRIVAL_PROJECTOR, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ARRIVAL_PROJECTOR_1_MEDIUM_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, 12, 1, -15, 15, 16, 30, 46, false, false, PIDSType.ARRIVAL_PROJECTOR, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ARRIVAL_PROJECTOR_1_LARGE_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, 16, 1, -15, 15, 16, 46, 46, false, false, PIDSType.ARRIVAL_PROJECTOR, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.BOAT_NODE_TILE_ENTITY.get(), RenderBoatNode::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.FREE_NODE_TILE_ENTITY.get(), RenderFreeNode::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.CLOCK_TILE_ENTITY.get(), RenderClock::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PSD_DOOR_1_TILE_ENTITY.get(), dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 0));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PSD_DOOR_2_TILE_ENTITY.get(), dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 1));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PSD_TOP_TILE_ENTITY.get(), RenderPSDTop::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.APG_GLASS_TILE_ENTITY.get(), RenderAPGGlass::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.APG_DOOR_TILE_ENTITY.get(), dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 2));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_1_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS1.TileEntityBlockPIDS1.MAX_ARRIVALS, BlockPIDS1.TileEntityBlockPIDS1.LINES_PER_ARRIVAL, 1, 3.25F, 6, 2.5F, 30, true, false, PIDSType.PIDS, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_2_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS2.TileEntityBlockPIDS2.MAX_ARRIVALS, BlockPIDS2.TileEntityBlockPIDS2.LINES_PER_ARRIVAL, 1.5F, 7.5F, 6, 6.5F, 29, true, true, PIDSType.PIDS, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_3_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS3.TileEntityBlockPIDS3.MAX_ARRIVALS, BlockPIDS3.TileEntityBlockPIDS3.LINES_PER_ARRIVAL, 2.5F, 7.5F, 6, 6.5F, 27, true, false, PIDSType.PIDS, 0xFF_FF9900, 0xFF_33CC00, 1.25F, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_4_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS4.TileEntityBlockPIDS4.MAX_ARRIVALS, BlockPIDS4.TileEntityBlockPIDS4.LINES_PER_ARRIVAL, 2F, 14F, 15, 28F, 12, false, false, PIDSType.PIDS_VERTICAL, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_SINGLE_ARRIVAL_1_TILE_ENTITY.get(), dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDSSingleArrival1.TileEntityBlockPIDSSingleArrival1.MAX_ARRIVALS, BlockPIDSSingleArrival1.TileEntityBlockPIDSSingleArrival1.LINES_PER_ARRIVAL, 2F, 14F, 15, 28F, 12, false, false, PIDSType.PIDS_SINGLE_ARRIVAL, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_2_EVEN_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_2_ODD_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_3_EVEN_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_3_ODD_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_4_EVEN_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_4_ODD_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_5_EVEN_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_5_ODD_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_6_EVEN_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_6_ODD_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_7_EVEN_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_7_ODD_TILE_ENTITY.get(), RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_STANDING_LIGHT_TILE_ENTITY.get(), RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_STANDING_METAL_TILE_ENTITY.get(), RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_WALL_LIGHT_TILE_ENTITY.get(), RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_WALL_METAL_TILE_ENTITY.get(), RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_1.get(), dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, true, false, 0xFF0000FF));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_2.get(), dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, false, false, 0xFF0000FF));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_3.get(), dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, true, true, 0xFF00FF00));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_4.get(), dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, false, true, 0xFF00FF00));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_3_ASPECT_1.get(), dispatcher -> new RenderSignalLight3Aspect<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_3_ASPECT_2.get(), dispatcher -> new RenderSignalLight3Aspect<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_4_ASPECT_1.get(), dispatcher -> new RenderSignalLight4Aspect<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_4_ASPECT_2.get(), dispatcher -> new RenderSignalLight4Aspect<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_SEMAPHORE_1.get(), dispatcher -> new RenderSignalSemaphore<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_SEMAPHORE_2.get(), dispatcher -> new RenderSignalSemaphore<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_ENTRANCE_TILE_ENTITY.get(), dispatcher -> new RenderStationNameTiled<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_TALL_BLOCK_TILE_ENTITY.get(), RenderStationNameTall::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_TALL_BLOCK_DOUBLE_SIDED_TILE_ENTITY.get(), RenderStationNameTall::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_TALL_WALL_TILE_ENTITY.get(), RenderStationNameTall::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_WALL_WHITE_TILE_ENTITY.get(), dispatcher -> new RenderStationNameTiled<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_WALL_GRAY_TILE_ENTITY.get(), dispatcher -> new RenderStationNameTiled<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_WALL_BLACK_TILE_ENTITY.get(), dispatcher -> new RenderStationNameTiled<>(dispatcher, false));

			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_ANDESITE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BEDROCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BIRCH_WOOD.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BONE_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_QUARTZ_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_STONE_BRICKS.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CLAY.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COAL_ORE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COBBLESTONE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE_POWDER.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CRACKED_STONE_BRICKS.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DARK_PRISMARINE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DIORITE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_GRAVEL.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_IRON_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_METAL.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_MOSAIC_TILE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PLANKS.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_ANDESITE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_DIORITE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_PILLAR.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BRICKS.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_PILLAR.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_QUARTZ.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_STONE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SNOW_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STAINED_GLASS.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE_BRICKS.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_WOOL.get());

			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_ANDESITE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BEDROCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BIRCH_WOOD_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BONE_BLOCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_QUARTZ_BLOCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_STONE_BRICKS_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CLAY_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COAL_ORE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COBBLESTONE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE_POWDER_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CRACKED_STONE_BRICKS_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DARK_PRISMARINE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DIORITE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_GRAVEL_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_IRON_BLOCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_METAL_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_MOSAIC_TILE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PLANKS_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_ANDESITE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_DIORITE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_BLOCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_PILLAR_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BLOCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BRICKS_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_PILLAR_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_QUARTZ_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_STONE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SNOW_BLOCK_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STAINED_GLASS_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE_BRICKS_SLAB.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_WOOL_SLAB.get());

			RegistryClient.registerBlockColors(Blocks.STATION_NAME_TALL_BLOCK.get());
			RegistryClient.registerBlockColors(Blocks.STATION_NAME_TALL_BLOCK_DOUBLE_SIDED.get());
			RegistryClient.registerBlockColors(Blocks.STATION_NAME_TALL_WALL.get());
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLE.get());
		}

		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_BUTTONS_1_TILE_ENTITY.get(), RenderLiftButtons::new);
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_EVEN_1_TILE_ENTITY.get(), dispatcher -> new RenderLiftPanel<>(dispatcher, false, false));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_ODD_1_TILE_ENTITY.get(), dispatcher -> new RenderLiftPanel<>(dispatcher, true, false));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_EVEN_2_TILE_ENTITY.get(), dispatcher -> new RenderLiftPanel<>(dispatcher, false, true));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_ODD_2_TILE_ENTITY.get(), dispatcher -> new RenderLiftPanel<>(dispatcher, true, true));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_DOOR_EVEN_1_TILE_ENTITY.get(), dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 3));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_DOOR_ODD_1_TILE_ENTITY.get(), dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 4));

		RegistryClient.registerNetworkReceiver(PACKET_VERSION_CHECK, packet -> PacketTrainDataGuiClient.openVersionCheckS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_CHUNK_S2C, packet -> PacketTrainDataGuiClient.receiveChunk(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_DASHBOARD_SCREEN, packet -> PacketTrainDataGuiClient.openDashboardScreenS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_PIDS_CONFIG_SCREEN, packet -> PacketTrainDataGuiClient.openPIDSConfigScreenS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_ARRIVAL_PROJECTOR_CONFIG_SCREEN, packet -> PacketTrainDataGuiClient.openArrivalProjectorConfigScreenS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_RAILWAY_SIGN_SCREEN, packet -> PacketTrainDataGuiClient.openRailwaySignScreenS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_TICKET_MACHINE_SCREEN, packet -> PacketTrainDataGuiClient.openTicketMachineScreenS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_TRAIN_SENSOR_SCREEN, packet -> PacketTrainDataGuiClient.openTrainSensorScreenS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_RESOURCE_PACK_CREATOR_SCREEN, packet -> PacketTrainDataGuiClient.openResourcePackCreatorScreen(Minecraft.getInstance()));
		RegistryClient.registerNetworkReceiver(PACKET_ANNOUNCE, packet -> PacketTrainDataGuiClient.announceS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_GENERATE_PATH, packet -> PacketTrainDataGuiClient.generatePathS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_CREATE_RAIL, packet -> PacketTrainDataGuiClient.createRailS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_CREATE_SIGNAL, packet -> PacketTrainDataGuiClient.createSignalS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_NODE, packet -> PacketTrainDataGuiClient.removeNodeS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_RAIL, packet -> PacketTrainDataGuiClient.removeRailConnectionS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_SIGNALS, packet -> PacketTrainDataGuiClient.removeSignalsS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_LIFT_FLOOR_TRACK, packet -> PacketTrainDataGuiClient.removeLiftFloorTrackS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_STATION, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.STATIONS, ClientData.DATA_CACHE.stationIdMap, (id, transportMode) -> new Station(id), false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_PLATFORM, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.PLATFORMS, ClientData.DATA_CACHE.platformIdMap, null, false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_SIDING, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.SIDINGS, ClientData.DATA_CACHE.sidingIdMap, null, false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_ROUTE, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.ROUTES, ClientData.DATA_CACHE.routeIdMap, Route::new, false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_DEPOT, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.DEPOTS, ClientData.DATA_CACHE.depotIdMap, Depot::new, false));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_STATION, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.STATIONS, ClientData.DATA_CACHE.stationIdMap, (id, transportMode) -> new Station(id), true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_PLATFORM, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.PLATFORMS, ClientData.DATA_CACHE.platformIdMap, null, true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_SIDING, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.SIDINGS, ClientData.DATA_CACHE.sidingIdMap, null, true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_ROUTE, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.ROUTES, ClientData.DATA_CACHE.routeIdMap, Route::new, true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_DEPOT, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.DEPOTS, ClientData.DATA_CACHE.depotIdMap, Depot::new, true));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFT, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(Minecraft.getInstance(), packet, ClientData.LIFTS, ClientData.DATA_CACHE.liftsClientIdMap, null, false));
		RegistryClient.registerNetworkReceiver(PACKET_WRITE_RAILS, packet -> ClientData.writeRails(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_TRAINS, packet -> ClientData.updateTrains(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFTS, packet -> ClientData.updateLifts(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_TRAINS, packet -> ClientData.deleteTrains(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_LIFTS, packet -> ClientData.deleteLifts(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_TRAIN_PASSENGERS, packet -> ClientData.updateTrainPassengers(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFT_PASSENGERS, packet -> ClientData.updateLiftPassengers(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_TRAIN_PASSENGER_POSITION, packet -> ClientData.updateTrainPassengerPosition(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFT_PASSENGER_POSITION, packet -> ClientData.updateLiftPassengerPosition(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_RAIL_ACTIONS, packet -> ClientData.updateRailActions(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_SCHEDULE, packet -> ClientData.updateSchedule(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_PROPAGATE_REPEATER_RESULT, packet -> RailEditorVisualScreen.receivePropagationResult(packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_LIFT_TRACK_FLOOR_SCREEN, packet -> PacketTrainDataGuiClient.openLiftTrackFloorS2C(Minecraft.getInstance(), packet));
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_LIFT_CUSTOMIZATION_SCREEN, packet -> PacketTrainDataGuiClient.openLiftCustomizationS2C(Minecraft.getInstance(), packet));

		RegistryClient.registerNetworkReceiver(TProbePackets.PACKET_REQUEST_PATH, packet -> PacketTProbeRequester.receivePathData(packet));
		RegistryClient.registerNetworkReceiver(TProbePackets.PACKET_REQUEST_VEHICLES, packet -> PacketTProbeRequester.receiveVehicles(packet));

		RegistryClient.registerKeyBinding(KeyMappings.LIFT_MENU);

		if (!Keys.LIFTS_ONLY) {
			RegistryClient.registerKeyBinding(KeyMappings.TRAIN_ACCELERATE);
			RegistryClient.registerKeyBinding(KeyMappings.TRAIN_BRAKE);
			RegistryClient.registerKeyBinding(KeyMappings.TRAIN_NEUTRAL);
			RegistryClient.registerKeyBinding(KeyMappings.TRAIN_TOGGLE_DOORS);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_1_NEGATIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_2_NEGATIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_3_NEGATIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_1_POSITIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_2_POSITIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_3_POSITIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_ROTATE_CATEGORY_NEGATIVE);
			RegistryClient.registerKeyBinding(KeyMappings.DEBUG_ROTATE_CATEGORY_POSITIVE);
		}

		Patreon.getPatreonList(Config.PATREON_LIST);
		Config.refreshProperties();

		RegistryClient.registerPostTickEvent(mc -> VehiclePlayerMovementTracker.tick());

		RegistryClient.registerPlayerJoinEvent(player -> {
			Config.refreshProperties();

			isReplayMod = player.getClass().toGenericString().toLowerCase(Locale.ENGLISH).contains("replaymod");
			try {
				Class.forName("org.vivecraft.main.VivecraftMain");
				isVivecraft = true;
			} catch (Exception ignored) {
				isVivecraft = false;
			}
			try {
				Class.forName("virtuoel.pehkui.Pehkui");
				isPehkui = true;
			} catch (Exception ignored) {
				isPehkui = false;
			}

			if(isReplayMod) {
				MTR.LOGGER.info("[NeoMTR] Running in Replay Mod mode");
			}
			if(isVivecraft) {
				MTR.LOGGER.info("[NeoMTR] Vivecraft detected");
			}
			if(isPehkui) {
				MTR.LOGGER.info("[NeoMTR] Pehkui detected");
			}

			if (!Keys.LIFTS_ONLY) {
				final Minecraft minecraft = Minecraft.getInstance();
				if (!minecraft.hasSingleplayerServer()) {
					Webserver.setMinecraftCallback(new Webserver.MinecraftCallback() {
						@Override
						public void runOnMainThread(Runnable runnable) {
							minecraft.execute(runnable);
						}

						@Override
						public @Nullable MinecraftServer getServer() {
							return null;
						}

						@Override
						public List<Level> getLevels() {
							return minecraft.level == null ? new ArrayList<>() : Collections.singletonList(minecraft.level);
						}

						@Override
						public List<Player> getLevelPlayers() {
							return List.of(minecraft.player);
						}

						@Override
						public Set<Route> getRoutes(RailwayData railwayData) {
							return ClientData.ROUTES;
						}

						@Override
						public DataCache getDataCache(RailwayData railwayData) {
							return ClientData.DATA_CACHE;
						}
					});
					Webserver.start(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("mtr_webserver_port.txt"));
				}
			}
		});

		if (!Keys.LIFTS_ONLY) {
			Webserver.init();
			Registry.registerPlayerQuitEvent(player -> Webserver.stop());

			BlockTactileMap.TileEntityTactileMap.updateSoundSource = TACTILE_MAP_SOUND_INSTANCE::setPos;
			BlockTactileMap.TileEntityTactileMap.onUse = pos -> {
				final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos);
				if (station != null) {
					IDrawing.narrateOrAnnounce(IGui.insertTranslation("gui.mtr.welcome_station_cjk", "gui.mtr.welcome_station", 1, IGui.textOrUntitled(station.name)));
				}
			};
		}
	}

	public static int getStationColor(BlockPos pos) {
		final int defaultColor = 0x7F7F7F;
		if (pos == null) {
			return defaultColor;
		} else {
			final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos);
			return station == null ? defaultColor : station.color;
		}
	}

	public static boolean isReplayMod() {
		return isReplayMod;
	}

	public static boolean isVivecraft() {
		return isVivecraft;
	}

	public static boolean isPehkui() {
		return isPehkui;
	}

	public static double getGameTick() {
		return gameTick;
	}

	public static void incrementGameTick() {
		gameTick += getLastFrameDuration();
		ClientData.tick();
	}

	public static float getLastFrameDuration() {
		if (Minecraft.getInstance().isPaused()) return 0;
		return isReplayMod ? 20F / 60 : Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
	}

	public static boolean canPlaySound() {
		if (gameTick - lastPlayedTrainSoundsTick >= TICKS_PER_SPEED_SOUND) {
			lastPlayedTrainSoundsTick = gameTick;
		}
		return gameTick == lastPlayedTrainSoundsTick && !Minecraft.getInstance().isPaused();
	}
}
