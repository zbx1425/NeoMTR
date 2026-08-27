package mtr.packet;

import mtr.MTR;
import net.minecraft.resources.Identifier;

public interface IPacket {

	Identifier PACKET_VERSION_CHECK = MTR.id("packet_version_check");

	Identifier PACKET_OPEN_DASHBOARD_SCREEN = MTR.id("packet_open_dashboard_screen");
	Identifier PACKET_OPEN_PIDS_CONFIG_SCREEN = MTR.id("packet_open_pids_config_screen");
	Identifier PACKET_OPEN_ARRIVAL_PROJECTOR_CONFIG_SCREEN = MTR.id("packet_open_arrival_projector_config_screen");
	Identifier PACKET_OPEN_RAILWAY_SIGN_SCREEN = MTR.id("packet_open_railway_sign_screen");
	Identifier PACKET_OPEN_TICKET_MACHINE_SCREEN = MTR.id("packet_open_ticket_machine_screen");
	Identifier PACKET_OPEN_TRAIN_SENSOR_SCREEN = MTR.id("packet_open_train_sensor_screen");
	Identifier PACKET_OPEN_LIFT_TRACK_FLOOR_SCREEN = MTR.id("packet_open_lift_track_floor_screen");
	Identifier PACKET_OPEN_LIFT_CUSTOMIZATION_SCREEN = MTR.id("packet_open_lift_customization_screen");
	Identifier PACKET_OPEN_RESOURCE_PACK_CREATOR_SCREEN = MTR.id("packet_open_resource_pack_creator_screen");

	Identifier PACKET_ANNOUNCE = MTR.id("packet_announce");

	Identifier PACKET_CREATE_RAIL = MTR.id("packet_create_rail");
	Identifier PACKET_CREATE_SIGNAL = MTR.id("packet_create_signal");
	Identifier PACKET_REMOVE_NODE = MTR.id("packet_remove_node");
	Identifier PACKET_REMOVE_LIFT_FLOOR_TRACK = MTR.id("packet_remove_lift_floor_track");
	Identifier PACKET_REMOVE_RAIL = MTR.id("packet_remove_rail");
	Identifier PACKET_REMOVE_SIGNALS = MTR.id("packet_remove_signals");
	Identifier PACKET_REMOVE_RAIL_ACTION = MTR.id("packet_remove_rail_action");

	Identifier PACKET_GENERATE_PATH = MTR.id("packet_generate_path");
	Identifier PACKET_CLEAR_TRAINS = MTR.id("packet_clear_trains");
	Identifier PACKET_SIGN_TYPES = MTR.id("packet_sign_types");
	Identifier PACKET_DRIVE_TRAIN = MTR.id("packet_drive_train");
	Identifier PACKET_PRESS_LIFT_BUTTON = MTR.id("packet_press_lift_button");
	Identifier PACKET_ADD_BALANCE = MTR.id("packet_add_balance");
	Identifier PACKET_PIDS_UPDATE = MTR.id("packet_pids_update");
	Identifier PACKET_ARRIVAL_PROJECTOR_UPDATE = MTR.id("packet_arrival_projector_update");
	Identifier PACKET_CHUNK_S2C = MTR.id("packet_chunk_s2c");

	Identifier PACKET_UPDATE_STATION = MTR.id("packet_update_station");
	Identifier PACKET_UPDATE_PLATFORM = MTR.id("packet_update_platform");
	Identifier PACKET_UPDATE_SIDING = MTR.id("packet_update_siding");
	Identifier PACKET_UPDATE_ROUTE = MTR.id("packet_update_route");
	Identifier PACKET_UPDATE_DEPOT = MTR.id("packet_update_depot");
	Identifier PACKET_UPDATE_LIFT = MTR.id("packet_update_lift");

	Identifier PACKET_DELETE_STATION = MTR.id("packet_delete_station");
	Identifier PACKET_DELETE_PLATFORM = MTR.id("packet_delete_platform");
	Identifier PACKET_DELETE_SIDING = MTR.id("packet_delete_siding");
	Identifier PACKET_DELETE_ROUTE = MTR.id("packet_delete_route");
	Identifier PACKET_DELETE_DEPOT = MTR.id("packet_delete_depot");

	Identifier PACKET_WRITE_RAILS = MTR.id("write_rails");
	Identifier PACKET_UPDATE_TRAINS = MTR.id("update_trains");
	Identifier PACKET_DELETE_TRAINS = MTR.id("delete_trains");
	Identifier PACKET_UPDATE_LIFTS = MTR.id("update_lifts");
	Identifier PACKET_DELETE_LIFTS = MTR.id("delete_lifts");
	Identifier PACKET_UPDATE_TRAIN_PASSENGERS = MTR.id("update_train_passengers");
	Identifier PACKET_UPDATE_TRAIN_PASSENGER_POSITION = MTR.id("update_train_passenger_position");
	Identifier PACKET_UPDATE_LIFT_PASSENGERS = MTR.id("update_lift_passengers");
	Identifier PACKET_UPDATE_LIFT_PASSENGER_POSITION = MTR.id("update_lift_passenger_position");
	Identifier PACKET_UPDATE_ENTITY_SEAT_POSITION = MTR.id("update_entity_seat_position");
	Identifier PACKET_UPDATE_RAIL_ACTIONS = MTR.id("update_rail_actions");
	Identifier PACKET_UPDATE_SCHEDULE = MTR.id("update_schedule");
	Identifier PACKET_UPDATE_TRAIN_SENSOR = MTR.id("packet_update_train_announcer");
	Identifier PACKET_UPDATE_FREE_NODE = MTR.id("packet_update_free_node");
	Identifier PACKET_UPDATE_LIFT_TRACK_FLOOR = MTR.id("packet_update_lift_track_floor");

	Identifier PACKET_PROPAGATE_REPEATER_OFFSET = MTR.id("packet_propagate_repeater_offset");
	Identifier PACKET_PROPAGATE_REPEATER_RESULT = MTR.id("packet_propagate_repeater_result");

	int MAX_PACKET_BYTES = 1048576;
}
