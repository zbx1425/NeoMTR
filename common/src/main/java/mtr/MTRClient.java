package mtr;

import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import cn.zbx1425.mtrsteamloco.gui.RailEditorVisualScreen;
import com.lx862.tprobe3.data.CompiledTrainData;
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
	private static long frameCounter = 0;
	private static double lastPlayedTrainSoundsTick = 0;

	public static final int TICKS_PER_SPEED_SOUND = 4;
	public static final LoopingSoundInstance TACTILE_MAP_SOUND_INSTANCE = new LoopingSoundInstance("tactile_map_music");

	public static void init() {
		if (!Keys.LIFTS_ONLY) {
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ARRIVAL_PROJECTOR_1_SMALL_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, 12, 1, 1, 15, 16, 14, 14, false, false, PIDSType.ARRIVAL_PROJECTOR, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ARRIVAL_PROJECTOR_1_MEDIUM_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, 12, 1, -15, 15, 16, 30, 46, false, false, PIDSType.ARRIVAL_PROJECTOR, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ARRIVAL_PROJECTOR_1_LARGE_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, 16, 1, -15, 15, 16, 46, 46, false, false, PIDSType.ARRIVAL_PROJECTOR, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.BOAT_NODE_TILE_ENTITY, RenderBoatNode::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.FREE_NODE_TILE_ENTITY, RenderFreeNode::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.CLOCK_TILE_ENTITY, RenderClock::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PSD_DOOR_1_TILE_ENTITY, dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 0));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PSD_DOOR_2_TILE_ENTITY, dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 1));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PSD_TOP_TILE_ENTITY, RenderPSDTop::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.APG_GLASS_TILE_ENTITY, RenderAPGGlass::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.APG_DOOR_TILE_ENTITY, dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 2));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_1_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS1.TileEntityBlockPIDS1.MAX_ARRIVALS, BlockPIDS1.TileEntityBlockPIDS1.LINES_PER_ARRIVAL, 1, 3.25F, 6, 2.5F, 30, true, false, PIDSType.PIDS, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_2_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS2.TileEntityBlockPIDS2.MAX_ARRIVALS, BlockPIDS2.TileEntityBlockPIDS2.LINES_PER_ARRIVAL, 1.5F, 7.5F, 6, 6.5F, 29, true, true, PIDSType.PIDS, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_3_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS3.TileEntityBlockPIDS3.MAX_ARRIVALS, BlockPIDS3.TileEntityBlockPIDS3.LINES_PER_ARRIVAL, 2.5F, 7.5F, 6, 6.5F, 27, true, false, PIDSType.PIDS, 0xFF_FF9900, 0xFF_33CC00, 1.25F, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_4_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDS4.TileEntityBlockPIDS4.MAX_ARRIVALS, BlockPIDS4.TileEntityBlockPIDS4.LINES_PER_ARRIVAL, 2F, 14F, 15, 28F, 12, false, false, PIDSType.PIDS_VERTICAL, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.PIDS_SINGLE_ARRIVAL_1_TILE_ENTITY, dispatcher -> new RenderPIDS<>(dispatcher, BlockPIDSSingleArrival1.TileEntityBlockPIDSSingleArrival1.MAX_ARRIVALS, BlockPIDSSingleArrival1.TileEntityBlockPIDSSingleArrival1.LINES_PER_ARRIVAL, 2F, 14F, 15, 28F, 12, false, false, PIDSType.PIDS_SINGLE_ARRIVAL, 0xFF_FF9900, 0xFF_FF9900));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_2_EVEN_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_2_ODD_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_3_EVEN_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_3_ODD_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_4_EVEN_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_4_ODD_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_5_EVEN_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_5_ODD_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_6_EVEN_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_6_ODD_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_7_EVEN_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.RAILWAY_SIGN_7_ODD_TILE_ENTITY, RenderRailwaySign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_STANDING_LIGHT_TILE_ENTITY, RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_STANDING_METAL_TILE_ENTITY, RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_WALL_LIGHT_TILE_ENTITY, RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.ROUTE_SIGN_WALL_METAL_TILE_ENTITY, RenderRouteSign::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_1, dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, true, false, 0xFF0000FF));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_2, dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, false, false, 0xFF0000FF));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_3, dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, true, true, 0xFF00FF00));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_2_ASPECT_4, dispatcher -> new RenderSignalLight2Aspect<>(dispatcher, false, true, 0xFF00FF00));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_3_ASPECT_1, dispatcher -> new RenderSignalLight3Aspect<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_3_ASPECT_2, dispatcher -> new RenderSignalLight3Aspect<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_4_ASPECT_1, dispatcher -> new RenderSignalLight4Aspect<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_LIGHT_4_ASPECT_2, dispatcher -> new RenderSignalLight4Aspect<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_SEMAPHORE_1, dispatcher -> new RenderSignalSemaphore<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.SIGNAL_SEMAPHORE_2, dispatcher -> new RenderSignalSemaphore<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_ENTRANCE_TILE_ENTITY, dispatcher -> new RenderStationNameTiled<>(dispatcher, true));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_TALL_BLOCK_TILE_ENTITY, RenderStationNameTall::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_TALL_BLOCK_DOUBLE_SIDED_TILE_ENTITY, RenderStationNameTall::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_TALL_WALL_TILE_ENTITY, RenderStationNameTall::new);
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_WALL_WHITE_TILE_ENTITY, dispatcher -> new RenderStationNameTiled<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_WALL_GRAY_TILE_ENTITY, dispatcher -> new RenderStationNameTiled<>(dispatcher, false));
			RegistryClient.registerTileEntityRenderer(BlockEntityTypes.STATION_NAME_WALL_BLACK_TILE_ENTITY, dispatcher -> new RenderStationNameTiled<>(dispatcher, false));

			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_ANDESITE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BEDROCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BIRCH_WOOD);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BONE_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_QUARTZ_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_STONE_BRICKS);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CLAY);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COAL_ORE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COBBLESTONE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE_POWDER);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CRACKED_STONE_BRICKS);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DARK_PRISMARINE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DIORITE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_GRAVEL);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_IRON_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_METAL);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_MOSAIC_TILE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PLANKS);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_ANDESITE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_DIORITE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_PILLAR);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BRICKS);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_PILLAR);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_QUARTZ);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_STONE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SNOW_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STAINED_GLASS);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE_BRICKS);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_WOOL);

			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_ANDESITE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BEDROCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BIRCH_WOOD_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_BONE_BLOCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_QUARTZ_BLOCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CHISELED_STONE_BRICKS_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CLAY_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COAL_ORE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_COBBLESTONE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CONCRETE_POWDER_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_CRACKED_STONE_BRICKS_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DARK_PRISMARINE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_DIORITE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_GRAVEL_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_IRON_BLOCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_METAL_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_MOSAIC_TILE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PLANKS_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_ANDESITE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLISHED_DIORITE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_BLOCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_PURPUR_PILLAR_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BLOCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_BRICKS_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_QUARTZ_PILLAR_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_QUARTZ_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SMOOTH_STONE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_SNOW_BLOCK_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STAINED_GLASS_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_STONE_BRICKS_SLAB);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_WOOL_SLAB);

			RegistryClient.registerBlockColors(Blocks.STATION_NAME_TALL_BLOCK);
			RegistryClient.registerBlockColors(Blocks.STATION_NAME_TALL_BLOCK_DOUBLE_SIDED);
			RegistryClient.registerBlockColors(Blocks.STATION_NAME_TALL_WALL);
			RegistryClient.registerBlockColors(Blocks.STATION_COLOR_POLE);
		}

		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_BUTTONS_1_TILE_ENTITY, RenderLiftButtons::new);
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_EVEN_1_TILE_ENTITY, dispatcher -> new RenderLiftPanel<>(dispatcher, false, false));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_ODD_1_TILE_ENTITY, dispatcher -> new RenderLiftPanel<>(dispatcher, true, false));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_EVEN_2_TILE_ENTITY, dispatcher -> new RenderLiftPanel<>(dispatcher, false, true));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_PANEL_ODD_2_TILE_ENTITY, dispatcher -> new RenderLiftPanel<>(dispatcher, true, true));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_DOOR_EVEN_1_TILE_ENTITY, dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 3));
		RegistryClient.registerTileEntityRenderer(BlockEntityTypes.LIFT_DOOR_ODD_1_TILE_ENTITY, dispatcher -> new RenderPSDAPGDoor<>(dispatcher, 4));

		RegistryClient.registerNetworkReceiver(PACKET_VERSION_CHECK, PacketTrainDataGuiClient::openVersionCheckS2C);
		RegistryClient.registerNetworkReceiver(PACKET_CHUNK_S2C, PacketTrainDataGuiClient::receiveChunk);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_DASHBOARD_SCREEN, PacketTrainDataGuiClient::openDashboardScreenS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_PIDS_CONFIG_SCREEN, PacketTrainDataGuiClient::openPIDSConfigScreenS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_ARRIVAL_PROJECTOR_CONFIG_SCREEN, PacketTrainDataGuiClient::openArrivalProjectorConfigScreenS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_RAILWAY_SIGN_SCREEN, PacketTrainDataGuiClient::openRailwaySignScreenS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_TICKET_MACHINE_SCREEN, PacketTrainDataGuiClient::openTicketMachineScreenS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_TRAIN_SENSOR_SCREEN, PacketTrainDataGuiClient::openTrainSensorScreenS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_RESOURCE_PACK_CREATOR_SCREEN, packet -> PacketTrainDataGuiClient.openResourcePackCreatorScreen());
		RegistryClient.registerNetworkReceiver(PACKET_ANNOUNCE, PacketTrainDataGuiClient::announceS2C);
		RegistryClient.registerNetworkReceiver(PACKET_GENERATE_PATH, PacketTrainDataGuiClient::generatePathS2C);
		RegistryClient.registerNetworkReceiver(PACKET_CREATE_RAIL, PacketTrainDataGuiClient::createRailS2C);
		RegistryClient.registerNetworkReceiver(PACKET_CREATE_SIGNAL, PacketTrainDataGuiClient::createSignalS2C);
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_NODE, PacketTrainDataGuiClient::removeNodeS2C);
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_RAIL, PacketTrainDataGuiClient::removeRailConnectionS2C);
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_SIGNALS, PacketTrainDataGuiClient::removeSignalsS2C);
		RegistryClient.registerNetworkReceiver(PACKET_REMOVE_LIFT_FLOOR_TRACK, PacketTrainDataGuiClient::removeLiftFloorTrackS2C);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_STATION, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.STATIONS, ClientData.DATA_CACHE.stationIdMap, (id, transportMode) -> new Station(id), false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_PLATFORM, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.PLATFORMS, ClientData.DATA_CACHE.platformIdMap, null, false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_SIDING, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.SIDINGS, ClientData.DATA_CACHE.sidingIdMap, null, false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_ROUTE, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.ROUTES, ClientData.DATA_CACHE.routeIdMap, Route::new, false));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_DEPOT, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.DEPOTS, ClientData.DATA_CACHE.depotIdMap, Depot::new, false));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_STATION, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.STATIONS, ClientData.DATA_CACHE.stationIdMap, (id, transportMode) -> new Station(id), true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_PLATFORM, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.PLATFORMS, ClientData.DATA_CACHE.platformIdMap, null, true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_SIDING, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.SIDINGS, ClientData.DATA_CACHE.sidingIdMap, null, true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_ROUTE, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.ROUTES, ClientData.DATA_CACHE.routeIdMap, Route::new, true));
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_DEPOT, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.DEPOTS, ClientData.DATA_CACHE.depotIdMap, Depot::new, true));
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFT, packet -> PacketTrainDataGuiClient.receiveUpdateOrDeleteS2C(packet, ClientData.LIFTS, ClientData.DATA_CACHE.liftsClientIdMap, null, false));
		RegistryClient.registerNetworkReceiver(PACKET_WRITE_RAILS, ClientData::writeRails);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_TRAINS, ClientData::updateTrains);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFTS, ClientData::updateLifts);
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_TRAINS, ClientData::deleteTrains);
		RegistryClient.registerNetworkReceiver(PACKET_DELETE_LIFTS, ClientData::deleteLifts);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_TRAIN_PASSENGERS, ClientData::updateTrainPassengers);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFT_PASSENGERS, ClientData::updateLiftPassengers);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_TRAIN_PASSENGER_POSITION, ClientData::updateTrainPassengerPosition);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_LIFT_PASSENGER_POSITION, ClientData::updateLiftPassengerPosition);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_RAIL_ACTIONS, ClientData::updateRailActions);
		RegistryClient.registerNetworkReceiver(PACKET_UPDATE_SCHEDULE, ClientData::updateSchedule);
		RegistryClient.registerNetworkReceiver(PACKET_PROPAGATE_REPEATER_RESULT, RailEditorVisualScreen::receivePropagationResult);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_LIFT_TRACK_FLOOR_SCREEN, PacketTrainDataGuiClient::openLiftTrackFloorS2C);
		RegistryClient.registerNetworkReceiver(PACKET_OPEN_LIFT_CUSTOMIZATION_SCREEN, PacketTrainDataGuiClient::openLiftCustomizationS2C);

		RegistryClient.registerNetworkReceiver(TProbePackets.PACKET_REQUEST_PATH, PacketTProbeRequester::receivePathData);
		RegistryClient.registerNetworkReceiver(TProbePackets.PACKET_REQUEST_VEHICLES, PacketTProbeRequester::receiveVehicles);

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

						@Override
						public List<CompiledTrainData> getExtraTrains() {
							// NeoMTR: Add Virtual Driving train on the map
							for(TrainClient trainClient : ClientData.TRAINS) {
								if(trainClient instanceof TrainVirtualDrive trainVirtualDrive) {
									CompiledTrainData compiledTrainData = CompiledTrainData.fromTrainClient(trainVirtualDrive);
									return List.of(compiledTrainData);
								}
							}
							return List.of();
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

	public static void onClientSetup() {
		Patreon.getPatreonList(Config.PATREON_LIST);
		Config.refreshProperties();
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

	public static long getFrame() {
		return frameCounter;
	}

	public static void onNewRenderFrame() {
		gameTick += getLastFrameDuration();
		frameCounter++;
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
