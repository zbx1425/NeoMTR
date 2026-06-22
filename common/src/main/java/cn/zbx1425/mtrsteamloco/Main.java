package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import cn.zbx1425.mtrsteamloco.block.BlockOneWayGate;
import cn.zbx1425.mtrsteamloco.network.*;
import com.google.gson.JsonParser;
import mtr.*;
import mtr.item.ItemBridgeCreator;
import mtr.item.ItemWithCreativeTabBase;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Main {

	public static final String MOD_ID = "mtrsteamloco";

	public static final Logger LOGGER = LoggerFactory.getLogger("MTR-NTE");
	public static final JsonParser JSON_PARSER = new JsonParser();

	public static final boolean enableRegistry;
	static {
		boolean enableRegistry1;
		try {
			String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI().getPath().toLowerCase(Locale.ROOT);
			enableRegistry1 = !jarPath.endsWith("-client.jar");
		} catch (URISyntaxException ignored) {
			enableRegistry1 = true;
		}
		enableRegistry = enableRegistry1;
	}

	public static final BrandNewEpicRegistryObject<Block> BLOCK_EYE_CANDY = new BrandNewEpicRegistryObject<>(resourceKey -> new BlockEyeCandy(BlockBehaviour.Properties.of().setId(resourceKey)));
	public static final RegistryObject<BlockEntityType<BlockEyeCandy.BlockEntityEyeCandy>>
			BLOCK_ENTITY_TYPE_EYE_CANDY = new RegistryObject<>(() -> Registry.getBlockEntityType(BlockEyeCandy.BlockEntityEyeCandy::new, BLOCK_EYE_CANDY.get()));
	public static final BrandNewEpicRegistryObject<Block> BLOCK_ONE_WAY_GATE = new BrandNewEpicRegistryObject<>(resourceKey -> new BlockOneWayGate(BlockBehaviour.Properties.of().setId(resourceKey)));

	public static final BrandNewEpicRegistryObject<Item> BRIDGE_CREATOR_1 = new BrandNewEpicRegistryObject<>((resourceKey) -> new ItemBridgeCreator(new Item.Properties().setId(resourceKey), 1));
	public static final BrandNewEpicRegistryObject<Item> RAIL_EDITOR_VISUAL = new BrandNewEpicRegistryObject<>((resourceKey) ->
		new ItemWithCreativeTabBase(new Item.Properties().setId(resourceKey), CreativeModeTabs.CORE, propModifier -> propModifier.stacksTo(1)));
	public static final BrandNewEpicRegistryObject<Item> RAIL_EDITOR_GEOMETRY = new BrandNewEpicRegistryObject<>((resourceKey) ->
		new ItemWithCreativeTabBase(new Item.Properties().setId(resourceKey), CreativeModeTabs.CORE, propModifier -> propModifier.stacksTo(1)));

	public static final RegistryObject<DataComponentType<CompoundTag>> TOOL_TAG = new RegistryObject<>(() -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).build());

	public static final SoundEvent SOUND_EVENT_BELL = SoundEvent.createVariableRangeEvent(Main.id("bell"));

	public static SimpleParticleType PARTICLE_STEAM_SMOKE;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void init(RegistriesWrapper registries) {
        LOGGER.info("MTR-NTE " + BuildConfig.MOD_VERSION + " built at {}", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault()).format(BuildConfig.BUILD_TIME));
		if (enableRegistry) {
			registries.registerBlockAndItem("eye_candy", BLOCK_EYE_CANDY, CreativeModeTabs.STATION_BUILDING_BLOCKS);
			registries.registerBlockEntityType("eye_candy", BLOCK_ENTITY_TYPE_EYE_CANDY);
			registries.registerBlockAndItem("one_way_gate_1", BLOCK_ONE_WAY_GATE, CreativeModeTabs.RAILWAY_FACILITIES);
			registries.registerItem("bridge_creator_1", BRIDGE_CREATOR_1);
			registries.registerItem("rail_editor_visual", RAIL_EDITOR_VISUAL);
			registries.registerItem("rail_editor_geometry", RAIL_EDITOR_GEOMETRY);
			registries.registerSoundEvent("bell", SOUND_EVENT_BELL);
			registries.registerDataComponents("tool_tag", TOOL_TAG);
			PARTICLE_STEAM_SMOKE = registries.createParticleType(true);
			registries.registerParticleType("steam_smoke", PARTICLE_STEAM_SMOKE);


			mtr.Registry.registerNetworkReceiver(PacketUpdateBlockEntity.PACKET_UPDATE_BLOCK_ENTITY,
					PacketUpdateBlockEntity::receiveUpdateC2S);
			mtr.Registry.registerNetworkReceiver(PacketUpdateRail.PACKET_UPDATE_RAIL,
					PacketUpdateRail::receiveUpdateC2S);
			mtr.Registry.registerNetworkReceiver(PacketUpdateHoldingItem.PACKET_UPDATE_HOLDING_ITEM,
					PacketUpdateHoldingItem::receiveUpdateC2S);
			mtr.Registry.registerNetworkReceiver(PacketVirtualDrive.PACKET_VIRTUAL_DRIVE,
					PacketVirtualDrive::receiveVirtualDriveC2S);

			mtr.Registry.registerNetworkPacket(PacketVersionCheck.PACKET_VERSION_CHECK);
			mtr.Registry.registerNetworkPacket(PacketScreen.PACKET_SHOW_SCREEN);
			mtr.Registry.registerNetworkPacket(PacketVirtualDrivingPlayers.PACKET_VIRTUAL_DRIVING_PLAYERS);

			mtr.Registry.registerPlayerJoinEvent(PacketVersionCheck::sendVersionCheckS2C);
		}

		Registry.registerPlayerJoinEvent(PacketVirtualDrivingPlayers::sendVirtualDrivingPlayersS2C);
	}

	@FunctionalInterface
	public interface RegisterBlockItem {
		void accept(String string, RegistryObject<Block> block, CreativeModeTabs.Wrapper tab);
	}
}
