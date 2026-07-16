package mtr.client;

import mtr.data.TrainType;
import mtr.data.TransportMode;
import mtr.mappings.Text;
import mtr.model.*;
import mtr.render.JonModelTrainRenderer;
import mtr.render.TrainRendererBase;
import mtr.sound.train.JonTrainSound;
import mtr.sound.train.TrainSoundBase;
import mtr.sound.train.bve.BveTrainSound;
import mtr.sound.train.bve.BveTrainSoundConfig;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiConsumer;

public class TrainClientRegistry {

	private static final Map<String, TrainProperties> REGISTRY = new HashMap<>();
	private static final Map<TransportMode, List<String>> KEY_ORDERS = new HashMap<>();

	public static void register(String key, TrainProperties properties) {
		final String keyLower = key.toLowerCase(Locale.ENGLISH);
		final TransportMode transportMode = TrainType.getTransportMode(properties.baseTrainType);
		if (!KEY_ORDERS.containsKey(transportMode)) {
			KEY_ORDERS.put(transportMode, new ArrayList<>());
		}
		if (!KEY_ORDERS.get(transportMode).contains(keyLower)) {
			KEY_ORDERS.get(transportMode).add(keyLower);
		}
		REGISTRY.put(keyLower, properties);
	}

	public static void register(String key, String baseTrainType, String name, String description, String wikipediaArticle, ModelTrainBase model, String textureId, int color, String gangwayConnectionId, String trainBarrierId, float riderOffset, float riderOffsetDismounting, float railSurfaceOffset, float bogiePosition, boolean isJacobsBogie, String soundId, JonTrainSound.JonTrainSoundConfig legacySoundConfig) {
		final TrainRendererBase renderer = new JonModelTrainRenderer(model, textureId, gangwayConnectionId, trainBarrierId);
		final TrainSoundBase sound = legacySoundConfig == null ? new BveTrainSound(new BveTrainSoundConfig(Minecraft.getInstance().getResourceManager(), soundId == null ? "" : soundId)) : new JonTrainSound(soundId, legacySoundConfig);
		register(key, new TrainProperties(baseTrainType, Text.translatable(name == null ? "train.mtr." + key.toLowerCase(Locale.ENGLISH) : name), description, wikipediaArticle, color, riderOffset, riderOffsetDismounting, railSurfaceOffset, bogiePosition, isJacobsBogie, !StringUtils.isEmpty(gangwayConnectionId), renderer, sound));
	}

	private static void register(TrainType defaultTrainType, String wikipediaArticle, ModelTrainBase model, String textureId, int color, String gangwayConnectionId, String trainBarrierId, float bogiePosition, boolean isJacobsBogie, String soundId, JonTrainSound.JonTrainSoundConfig legacySoundConfig) {
		register(defaultTrainType.toString(), defaultTrainType.baseTrainType, null, null, wikipediaArticle, model, textureId, color, gangwayConnectionId, trainBarrierId, 0, 0, -1, bogiePosition, isJacobsBogie, soundId, legacySoundConfig);
	}

	private static void register(TrainType defaultTrainType, String wikipediaArticle, ModelTrainBase model, String textureId, int color, float riderOffset, float riderOffsetDismounting) {
		register(defaultTrainType.toString(), defaultTrainType.baseTrainType, null, null, wikipediaArticle, model, textureId, color, "", "", riderOffset, riderOffsetDismounting,
				TrainType.getTransportMode(defaultTrainType.baseTrainType) == TransportMode.CABLE_CAR ? 5 : -1, 0, false, null, new JonTrainSound.JonTrainSoundConfig(null, 0, 0.5F, false));
	}

	public static void reset() {
		REGISTRY.clear();
		KEY_ORDERS.clear();
register(TrainType.M_TRAIN, "MTR_Metro_Cammell_EMU_(DC)", new ModelMTrain(), "mtr:textures/vehicle/m_train", 0x999999, "mtr:textures/vehicle/m_train", "", 8.5F, false, "m_train", new JonTrainSound.JonTrainSoundConfig("m_train", 90, 0.5F, false));
		register(TrainType.M_TRAIN_SMALL, "MTR_Metro_Cammell_EMU_(DC)", new ModelMTrainSmall(), "mtr:textures/vehicle/m_train", 0x999999, "mtr:textures/vehicle/m_train", "", 6F, false, "m_train", new JonTrainSound.JonTrainSoundConfig("m_train", 90, 0.5F, false));
		register(TrainType.M_TRAIN_MINI, "MTR_Metro_Cammell_EMU_(DC)", new ModelMTrainMini(), "mtr:textures/vehicle/m_train", 0x999999, "mtr:textures/vehicle/m_train", "", 2.5F, true, "m_train", new JonTrainSound.JonTrainSoundConfig("m_train", 90, 0.5F, false));
		register(TrainType.CM_STOCK, "MTR_Metro_Cammell_EMU_(DC)", new ModelCMStock(), "mtr:textures/vehicle/cm_stock", 0x999999, "mtr:textures/vehicle/m_train", "", 8.5F, false, "m_train", new JonTrainSound.JonTrainSoundConfig("m_train", 90, 0.5F, false));
		register(TrainType.CM_STOCK_SMALL, "MTR_Metro_Cammell_EMU_(DC)", new ModelCMStockSmall(), "mtr:textures/vehicle/cm_stock", 0x999999, "mtr:textures/vehicle/m_train", "", 6F, false, "m_train", new JonTrainSound.JonTrainSoundConfig("m_train", 90, 0.5F, false));
		register(TrainType.CM_STOCK_MINI, "MTR_Metro_Cammell_EMU_(DC)", new ModelCMStockMini(), "mtr:textures/vehicle/cm_stock", 0x999999, "mtr:textures/vehicle/m_train", "", 2.5F, true, "m_train", new JonTrainSound.JonTrainSoundConfig("m_train", 90, 0.5F, false));
		register(TrainType.K_TRAIN, "MTR_Rotem_EMU", new ModelKTrain(false), "mtr:textures/vehicle/k_train", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 8.5F, false, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_SMALL, "MTR_Rotem_EMU", new ModelKTrainSmall(false), "mtr:textures/vehicle/k_train", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 6F, false, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_MINI, "MTR_Rotem_EMU", new ModelKTrainMini(false), "mtr:textures/vehicle/k_train", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 2.5F, true, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_TCL, "MTR_Rotem_EMU", new ModelKTrain(true), "mtr:textures/vehicle/k_train_tcl", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 8.5F, false, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_TCL_SMALL, "MTR_Rotem_EMU", new ModelKTrainSmall(true), "mtr:textures/vehicle/k_train_tcl", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 6F, false, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_TCL_MINI, "MTR_Rotem_EMU", new ModelKTrainMini(true), "mtr:textures/vehicle/k_train_tcl", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 2.5F, true, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_AEL, "MTR_Rotem_EMU", new ModelKTrain(true), "mtr:textures/vehicle/k_train_ael", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 8.5F, false, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_AEL_SMALL, "MTR_Rotem_EMU", new ModelKTrainSmall(true), "mtr:textures/vehicle/k_train_ael", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 6F, false, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.K_TRAIN_AEL_MINI, "MTR_Rotem_EMU", new ModelKTrainMini(true), "mtr:textures/vehicle/k_train_ael", 0x0EAB52, "mtr:textures/vehicle/k_train", "", 2.5F, true, "k_train", new JonTrainSound.JonTrainSoundConfig("k_train", 66, 1, false));
		register(TrainType.C_TRAIN, "MTR_CNR_Changchun_EMU", new ModelCTrain(), "mtr:textures/vehicle/c_train", 0xFDD900, "mtr:textures/vehicle/s_train", "", 8.5F, false, "c_train", new JonTrainSound.JonTrainSoundConfig("sp1900", 69, 0.5F, false));
		register(TrainType.C_TRAIN_SMALL, "MTR_CNR_Changchun_EMU", new ModelCTrainSmall(), "mtr:textures/vehicle/c_train", 0xFDD900, "mtr:textures/vehicle/s_train", "", 6F, false, "c_train", new JonTrainSound.JonTrainSoundConfig("sp1900", 69, 0.5F, false));
		register(TrainType.C_TRAIN_MINI, "MTR_CNR_Changchun_EMU", new ModelCTrainMini(), "mtr:textures/vehicle/c_train", 0xFDD900, "mtr:textures/vehicle/s_train", "", 2.5F, true, "c_train", new JonTrainSound.JonTrainSoundConfig("sp1900", 69, 0.5F, false));
		register(TrainType.S_TRAIN, "MTR_Urban_Lines_Vision_Train", new ModelSTrain(), "mtr:textures/vehicle/s_train", 0xC1CD23, "mtr:textures/vehicle/s_train", "", 8.5F, false, "s_train", new JonTrainSound.JonTrainSoundConfig("sp1900", 42, 0.5F, false));
		register(TrainType.S_TRAIN_SMALL, "MTR_Urban_Lines_Vision_Train", new ModelSTrainSmall(), "mtr:textures/vehicle/s_train", 0xC1CD23, "mtr:textures/vehicle/s_train", "", 6F, false, "s_train", new JonTrainSound.JonTrainSoundConfig("sp1900", 42, 0.5F, false));
		register(TrainType.S_TRAIN_MINI, "MTR_Urban_Lines_Vision_Train", new ModelSTrainMini(), "mtr:textures/vehicle/s_train", 0xC1CD23, "mtr:textures/vehicle/s_train", "", 2.5F, true, "s_train", new JonTrainSound.JonTrainSoundConfig("sp1900", 42, 0.5F, false));
		register(TrainType.MINECART, "Minecart", null, "textures/vehicle/minecart", 0x666666, -0.5F, 0);
		register(TrainType.OAK_BOAT, "Boat", null, "textures/vehicle/boat/oak", 0x8F7748, -1.5F, 0);
		register(TrainType.SPRUCE_BOAT, "Boat", null, "textures/vehicle/boat/spruce", 0x8F7748, -1.5F, 0);
		register(TrainType.BIRCH_BOAT, "Boat", null, "textures/vehicle/boat/birch", 0x8F7748, -1.5F, 0);
		register(TrainType.JUNGLE_BOAT, "Boat", null, "textures/vehicle/boat/jungle", 0x8F7748, -1.5F, 0);
		register(TrainType.ACACIA_BOAT, "Boat", null, "textures/vehicle/boat/acacia", 0x8F7748, -1.5F, 0);
		register(TrainType.DARK_OAK_BOAT, "Boat", null, "textures/vehicle/boat/dark_oak", 0x8F7748, -1.5F, 0);
		register(TrainType.NGONG_PING_360_CRYSTAL, "Ngong_Ping_360", new ModelNgongPing360(false), "mtr:textures/vehicle/ngong_ping_360_crystal", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_CRYSTAL_RHT, "Ngong_Ping_360", new ModelNgongPing360(true), "mtr:textures/vehicle/ngong_ping_360_crystal", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_CRYSTAL_PLUS, "Ngong_Ping_360", new ModelNgongPing360(false), "mtr:textures/vehicle/ngong_ping_360_crystal_plus", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_CRYSTAL_PLUS_RHT, "Ngong_Ping_360", new ModelNgongPing360(true), "mtr:textures/vehicle/ngong_ping_360_crystal_plus", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_NORMAL_RED, "Ngong_Ping_360", new ModelNgongPing360(false), "mtr:textures/vehicle/ngong_ping_360_normal_red", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_NORMAL_RED_RHT, "Ngong_Ping_360", new ModelNgongPing360(true), "mtr:textures/vehicle/ngong_ping_360_normal_red", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_NORMAL_ORANGE, "Ngong_Ping_360", new ModelNgongPing360(false), "mtr:textures/vehicle/ngong_ping_360_normal_orange", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_NORMAL_ORANGE_RHT, "Ngong_Ping_360", new ModelNgongPing360(true), "mtr:textures/vehicle/ngong_ping_360_normal_orange", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_NORMAL_LIGHT_BLUE, "Ngong_Ping_360", new ModelNgongPing360(false), "mtr:textures/vehicle/ngong_ping_360_normal_light_blue", 0x062540, 0, 0);
		register(TrainType.NGONG_PING_360_NORMAL_LIGHT_BLUE_RHT, "Ngong_Ping_360", new ModelNgongPing360(true), "mtr:textures/vehicle/ngong_ping_360_normal_light_blue", 0x062540, 0, 0);
		register(TrainType.FLYING_MINECART, "Minecart", null, "textures/vehicle/minecart", 0x666666, -0.5F, 0);
	}

	public static TrainProperties getTrainProperties(String key) {
		final String keyLower = key.toLowerCase(Locale.ENGLISH);
		return REGISTRY.getOrDefault(keyLower, TrainProperties.getBlankProperties());
	}

	public static TrainProperties getTrainProperties(TransportMode transportMode, int index) {
		return index >= 0 && index < KEY_ORDERS.get(transportMode).size() ? REGISTRY.get(KEY_ORDERS.get(transportMode).get(index)) : TrainProperties.getBlankProperties();
	}

	public static String getTrainId(TransportMode transportMode, int index) {
		return KEY_ORDERS.get(transportMode).get(index >= 0 && index < KEY_ORDERS.get(transportMode).size() ? index : 0);
	}

	public static void forEach(TransportMode transportMode, BiConsumer<String, TrainProperties> biConsumer) {
		KEY_ORDERS.get(transportMode).forEach(key -> biConsumer.accept(key, REGISTRY.get(key)));
	}
}
