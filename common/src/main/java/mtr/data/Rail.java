package mtr.data;

import mtr.block.BlockNode;
import mtr.mappings.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class Rail extends SerializedDataBase {

	public final RailType railType;
	public final TransportMode transportMode;
	public final RailAngle facingStart;
	public final RailAngle facingEnd;
	private final double h1, k1, r1, tStart1, tEnd1;
	private final double h2, k2, r2, tStart2, tEnd2;
	private final int yStart, yEnd;
	private final boolean reverseT1, isStraight1, reverseT2, isStraight2;

	private static final double ACCEPT_THRESHOLD = 1E-4;
	private static final int MIN_RADIUS = 2;
	private static final int CABLE_CURVATURE_SCALE = 1000;
	private static final int MAX_CABLE_DIP = 8;

	private static final String KEY_H_1 = "h_1";
	private static final String KEY_K_1 = "k_1";
	private static final String KEY_H_2 = "h_2";
	private static final String KEY_K_2 = "k_2";
	private static final String KEY_R_1 = "r_1";
	private static final String KEY_R_2 = "r_2";
	private static final String KEY_T_START_1 = "t_start_1";
	private static final String KEY_T_END_1 = "t_end_1";
	private static final String KEY_T_START_2 = "t_start_2";
	private static final String KEY_T_END_2 = "t_end_2";
	private static final String KEY_Y_START = "y_start";
	private static final String KEY_Y_END = "y_end";
	private static final String KEY_REVERSE_T_1 = "reverse_t_1";
	private static final String KEY_IS_STRAIGHT_1 = "is_straight_1";
	private static final String KEY_REVERSE_T_2 = "reverse_t_2";
	private static final String KEY_IS_STRAIGHT_2 = "is_straight_2";
	private static final String KEY_RAIL_TYPE = "rail_type";
	private static final String KEY_TRANSPORT_MODE = "transport_mode";

	// for curves:
	// x = h + r*cos(T)
	// z = k + r*sin(T)
	// for straight lines (both k and r >= 0.5):
	// x = h*T
	// z = k*T + h*r
	// for straight lines (otherwise):
	// x = h*T + k*r
	// z = k*T + h*r

	public Rail(BlockPos posStart, RailAngle facingStart, BlockPos posEnd, RailAngle facingEnd, RailType railType, TransportMode transportMode) {
		this.facingStart = facingStart;
		this.facingEnd = facingEnd;
		this.railType = railType;
		this.transportMode = transportMode;
		yStart = posStart.getY();
		yEnd = posEnd.getY();

		final RailCalculator.Group group = RailCalculator.calculate(
				posStart.getX(), posStart.getZ(), posEnd.getX(), posEnd.getZ(),
				facingStart.angleRadians, facingEnd.angleRadians
		);

		if (group != null) {
			h1 = group.first.h;
			k1 = group.first.k;
			r1 = group.first.r;
			tStart1 = group.first.tStart;
			tEnd1 = group.first.tEnd;
			reverseT1 = group.first.reverseT;
			isStraight1 = group.first.isStraight;
			h2 = group.second.h;
			k2 = group.second.k;
			r2 = group.second.r;
			tStart2 = group.second.tStart;
			tEnd2 = group.second.tEnd;
			reverseT2 = group.second.reverseT;
			isStraight2 = group.second.isStraight;
		} else {
			h1 = k1 = h2 = k2 = r1 = r2 = 0;
			tStart1 = tStart2 = tEnd1 = tEnd2 = 0;
			reverseT1 = reverseT2 = false;
			isStraight1 = isStraight2 = true;
		}
	}

	public Rail(Map<String, Value> map) {
		final MessagePackHelper messagePackHelper = new MessagePackHelper(map);
		h1 = messagePackHelper.getDouble(KEY_H_1);
		k1 = messagePackHelper.getDouble(KEY_K_1);
		h2 = messagePackHelper.getDouble(KEY_H_2);
		k2 = messagePackHelper.getDouble(KEY_K_2);
		r1 = messagePackHelper.getDouble(KEY_R_1);
		r2 = messagePackHelper.getDouble(KEY_R_2);
		tStart1 = messagePackHelper.getDouble(KEY_T_START_1);
		tEnd1 = messagePackHelper.getDouble(KEY_T_END_1);
		tStart2 = messagePackHelper.getDouble(KEY_T_START_2);
		tEnd2 = messagePackHelper.getDouble(KEY_T_END_2);
		yStart = messagePackHelper.getInt(KEY_Y_START);
		yEnd = messagePackHelper.getInt(KEY_Y_END);
		reverseT1 = messagePackHelper.getBoolean(KEY_REVERSE_T_1);
		isStraight1 = messagePackHelper.getBoolean(KEY_IS_STRAIGHT_1);
		reverseT2 = messagePackHelper.getBoolean(KEY_REVERSE_T_2);
		isStraight2 = messagePackHelper.getBoolean(KEY_IS_STRAIGHT_2);
		railType = EnumHelper.valueOf(RailType.IRON, messagePackHelper.getString(KEY_RAIL_TYPE));
		transportMode = EnumHelper.valueOf(TransportMode.TRAIN, messagePackHelper.getString(KEY_TRANSPORT_MODE));

		facingStart = getRailAngle(false);
		facingEnd = getRailAngle(true);
	}

	@Deprecated
	public Rail(CompoundTag compoundTag) {
		h1 = compoundTag.getDoubleOr(KEY_H_1, 0);
		k1 = compoundTag.getDoubleOr(KEY_K_1, 0);
		h2 = compoundTag.getDoubleOr(KEY_H_2, 0);
		k2 = compoundTag.getDoubleOr(KEY_K_2, 0);
		r1 = compoundTag.getDoubleOr(KEY_R_1, 0);
		r2 = compoundTag.getDoubleOr(KEY_R_2, 0);
		tStart1 = compoundTag.getDoubleOr(KEY_T_START_1, 0);
		tEnd1 = compoundTag.getDoubleOr(KEY_T_END_1, 0);
		tStart2 = compoundTag.getDoubleOr(KEY_T_START_2, 0);
		tEnd2 = compoundTag.getDoubleOr(KEY_T_END_2, 0);
		yStart = compoundTag.getIntOr(KEY_Y_START, 0);
		yEnd = compoundTag.getIntOr(KEY_Y_END, 0);
		reverseT1 = compoundTag.getBooleanOr(KEY_REVERSE_T_1, false);
		isStraight1 = compoundTag.getBooleanOr(KEY_IS_STRAIGHT_1, false);
		reverseT2 = compoundTag.getBooleanOr(KEY_REVERSE_T_2, false);
		isStraight2 = compoundTag.getBooleanOr(KEY_IS_STRAIGHT_2, false);
		railType = EnumHelper.valueOf(RailType.IRON, compoundTag.getStringOr(KEY_RAIL_TYPE, ""));
		transportMode = EnumHelper.valueOf(TransportMode.TRAIN, compoundTag.getStringOr(KEY_TRANSPORT_MODE, "TRAIN"));

		facingStart = getRailAngle(false);
		facingEnd = getRailAngle(true);
	}

	public Rail(FriendlyByteBuf packet) {
		h1 = packet.readFloat();
		k1 = packet.readFloat();
		h2 = packet.readFloat();
		k2 = packet.readFloat();
		r1 = packet.readFloat();
		r2 = packet.readFloat();
		tStart1 = packet.readFloat();
		tEnd1 = packet.readFloat();
		tStart2 = packet.readFloat();
		tEnd2 = packet.readFloat();
		yStart = packet.readVarInt();
		yEnd = packet.readVarInt();
		reverseT1 = packet.readBoolean();
		isStraight1 = packet.readBoolean();
		reverseT2 = packet.readBoolean();
		isStraight2 = packet.readBoolean();
		railType = EnumHelper.valueOf(RailType.IRON, packet.readUtf(PACKET_STRING_READ_LENGTH));
		transportMode = EnumHelper.valueOf(TransportMode.TRAIN, packet.readUtf(PACKET_STRING_READ_LENGTH));

		facingStart = getRailAngle(false);
		facingEnd = getRailAngle(true);
	}

	@Override
	public void toMessagePack(MessagePacker messagePacker) throws IOException {
		messagePacker.packString(KEY_H_1).packDouble(h1);
		messagePacker.packString(KEY_K_1).packDouble(k1);
		messagePacker.packString(KEY_H_2).packDouble(h2);
		messagePacker.packString(KEY_K_2).packDouble(k2);
		messagePacker.packString(KEY_R_1).packDouble(r1);
		messagePacker.packString(KEY_R_2).packDouble(r2);
		messagePacker.packString(KEY_T_START_1).packDouble(tStart1);
		messagePacker.packString(KEY_T_END_1).packDouble(tEnd1);
		messagePacker.packString(KEY_T_START_2).packDouble(tStart2);
		messagePacker.packString(KEY_T_END_2).packDouble(tEnd2);
		messagePacker.packString(KEY_Y_START).packInt(yStart);
		messagePacker.packString(KEY_Y_END).packInt(yEnd);
		messagePacker.packString(KEY_REVERSE_T_1).packBoolean(reverseT1);
		messagePacker.packString(KEY_IS_STRAIGHT_1).packBoolean(isStraight1);
		messagePacker.packString(KEY_REVERSE_T_2).packBoolean(reverseT2);
		messagePacker.packString(KEY_IS_STRAIGHT_2).packBoolean(isStraight2);
		messagePacker.packString(KEY_RAIL_TYPE).packString(railType.toString());
		messagePacker.packString(KEY_TRANSPORT_MODE).packString(transportMode.toString());
	}

	@Override
	public int messagePackLength() {
		return 18;
	}

	@Override
	public void writePacket(FriendlyByteBuf packet) {
		packet.writeFloat((float) h1);
		packet.writeFloat((float) k1);
		packet.writeFloat((float) h2);
		packet.writeFloat((float) k2);
		packet.writeFloat((float) r1);
		packet.writeFloat((float) r2);
		packet.writeFloat((float) tStart1);
		packet.writeFloat((float) tEnd1);
		packet.writeFloat((float) tStart2);
		packet.writeFloat((float) tEnd2);
		packet.writeVarInt(yStart);
		packet.writeVarInt(yEnd);
		packet.writeBoolean(reverseT1);
		packet.writeBoolean(isStraight1);
		packet.writeBoolean(reverseT2);
		packet.writeBoolean(isStraight2);
		packet.writeUtf(railType.toString());
		packet.writeUtf(transportMode.toString());
	}

	public Vec3 getPosition(double rawValue) {
		final double count1 = Math.abs(tEnd1 - tStart1);
		final double count2 = Math.abs(tEnd2 - tStart2);
		final double value = Mth.clamp(rawValue, 0, count1 + count2);
		final double y = getPositionY(value);

		if (value <= count1) {
			return getPositionXZ(h1, k1, r1, (reverseT1 ? -1 : 1) * value + tStart1, 0, isStraight1).add(0, y, 0);
		} else {
			return getPositionXZ(h2, k2, r2, (reverseT2 ? -1 : 1) * (value - count1) + tStart2, 0, isStraight2).add(0, y, 0);
		}
	}

	public double getLength() {
		return Math.abs(tEnd2 - tStart2) + Math.abs(tEnd1 - tStart1);
	}

	public void render(RenderRail callback, float offsetRadius1, float offsetRadius2) {
		renderSegment(h1, k1, r1, tStart1, tEnd1, 0, offsetRadius1, offsetRadius2, reverseT1, isStraight1, callback);
		renderSegment(h2, k2, r2, tStart2, tEnd2, Math.abs(tEnd1 - tStart1), offsetRadius1, offsetRadius2, reverseT2, isStraight2, callback);
	}

	public boolean goodRadius() {
		return (isStraight1 || r1 > MIN_RADIUS - ACCEPT_THRESHOLD) && (isStraight2 || r2 > MIN_RADIUS - ACCEPT_THRESHOLD);
	}

	public boolean isValid() {
		return (h1 != 0 || k1 != 0 || h2 != 0 || k2 != 0 || r1 != 0 || r2 != 0 || tStart1 != 0 || tStart2 != 0 || tEnd1 != 0 || tEnd2 != 0) && facingStart.isParallel(getRailAngle(false)) && facingEnd.isParallel(getRailAngle(true));
	}

	private double getPositionY(double value) {
		final double length = getLength();

		if (railType.railSlopeStyle == RailType.RailSlopeStyle.CABLE) {
			if (value < 0.5) {
				return yStart;
			} else if (value > length - 0.5) {
				return yEnd;
			}

			final double offsetValue = value - 0.5;
			final double offsetLength = length - 1;
			final double posY = yStart + (yEnd - yStart) * offsetValue / offsetLength;
			final double dip = offsetLength * offsetLength / 4 / CABLE_CURVATURE_SCALE;
			return posY + (dip > MAX_CABLE_DIP ? MAX_CABLE_DIP / dip : 1) * (offsetValue - offsetLength) * offsetValue / CABLE_CURVATURE_SCALE;
		} else {
			final double intercept = length / 2;
			final double yChange;
			final double yInitial;
			final double offsetValue;

			if (value < intercept) {
				yChange = (yEnd - yStart) / 2D;
				yInitial = yStart;
				offsetValue = value;
			} else {
				yChange = (yStart - yEnd) / 2D;
				yInitial = yEnd;
				offsetValue = length - value;
			}

			return yChange * offsetValue * offsetValue / (intercept * intercept) + yInitial;
		}
	}

	private static Vec3 getPositionXZ(double h, double k, double r, double t, double radiusOffset, boolean isStraight) {
		if (isStraight) {
			return new Vec3(h * t + k * ((Math.abs(h) >= 0.5 && Math.abs(k) >= 0.5 ? 0 : r) + radiusOffset) + 0.5, 0, k * t + h * (r - radiusOffset) + 0.5);
		} else {
			return new Vec3(h + (r + radiusOffset) * Math.cos(t / r) + 0.5, 0, k + (r + radiusOffset) * Math.sin(t / r) + 0.5);
		}
	}

	private void renderSegment(double h, double k, double r, double tStart, double tEnd, double rawValueOffset, float offsetRadius1, float offsetRadius2, boolean reverseT, boolean isStraight, RenderRail callback) {
		final double count = Math.abs(tEnd - tStart);
		final double increment = count / Math.round(count);

		for (double i = 0; i < count - 0.1; i += increment) {
			final double t1 = (reverseT ? -1 : 1) * i + tStart;
			final double t2 = (reverseT ? -1 : 1) * (i + increment) + tStart;
			final Vec3 corner1 = getPositionXZ(h, k, r, t1, offsetRadius1, isStraight);
			final Vec3 corner2 = getPositionXZ(h, k, r, t1, offsetRadius2, isStraight);
			final Vec3 corner3 = getPositionXZ(h, k, r, t2, offsetRadius2, isStraight);
			final Vec3 corner4 = getPositionXZ(h, k, r, t2, offsetRadius1, isStraight);

			final double y1 = getPositionY(i + rawValueOffset);
			final double y2 = getPositionY(i + increment + rawValueOffset);

			callback.renderRail(corner1.x, corner1.z, corner2.x, corner2.z, corner3.x, corner3.z, corner4.x, corner4.z, y1, y2);
		}
	}

	private RailAngle getRailAngle(boolean getEnd) {
		final double start;
		final double end;
		if (getEnd) {
			start = getLength();
			end = start - ACCEPT_THRESHOLD;
		} else {
			start = 0;
			end = ACCEPT_THRESHOLD;
		}
		final Vec3 pos1 = getPosition(start);
		final Vec3 pos2 = getPosition(end);
		return RailAngle.fromExactAngle((float) Math.toDegrees(Math.atan2(pos2.z - pos1.z, pos2.x - pos1.x)));
	}


	public static class RailActions {

		private double distance;

		public final long id;
		private final Level world;
		private final UUID uuid;
		private final String playerName;
		private final RailActionType railActionType;
		private final Rail rail;
		private final int radius;
		private final int height;
		private final double length;
		private final BlockState state;
		private final boolean isSlab;
		private final Set<BlockPos> blacklistedPos = new HashSet<>();

		private static final double INCREMENT = 0.01;

		public RailActions(Level world, Player player, RailActionType railActionType, Rail rail, int radius, int height, BlockState state) {
			id = new Random().nextLong();
			this.world = world;
			uuid = player.getUUID();
			playerName = player.getName().getString();
			this.railActionType = railActionType;
			this.rail = rail;
			this.radius = radius;
			this.height = height;
			this.state = state;
			isSlab = state != null && state.getBlock() instanceof SlabBlock;
			length = rail.getLength();
			distance = 0;
		}

		public boolean build() {
			switch (railActionType) {
				case BRIDGE:
					return createBridge();
				case TUNNEL:
					return createTunnel();
				case TUNNEL_WALL:
					return createTunnelWall();
				default:
					return true;
			}
		}

		public void writePacket(FriendlyByteBuf packet) {
			packet.writeLong(id);
			packet.writeUtf(playerName);
			packet.writeFloat(RailwayData.round(length, 1));
			packet.writeUtf(state == null ? "" : state.getBlock().getDescriptionId());
			packet.writeUtf(railActionType.nameTranslation);
			packet.writeInt(railActionType.color);
		}

		private boolean createTunnel() {
			return create(true, editPos -> {
				final BlockPos pos = RailwayData.newBlockPos(editPos);
				if (!blacklistedPos.contains(pos) && canPlace(world, pos)) {
					world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
					blacklistedPos.add(pos);
				}
			});
		}

		private boolean createTunnelWall() {
			return create(false, editPos -> {
				final BlockPos pos = RailwayData.newBlockPos(editPos);
				if (!blacklistedPos.contains(pos) && canPlace(world, pos)) {
					world.setBlockAndUpdate(pos, state);
					blacklistedPos.add(pos);
				}
			});
		}

		private boolean createBridge() {
			return create(false, editPos -> {
				final BlockPos pos = RailwayData.newBlockPos(editPos);
				final boolean isTopHalf = editPos.y - Math.floor(editPos.y) >= 0.5;
				blacklistedPos.add(getHalfPos(pos, isTopHalf));

				final BlockPos placePos;
				final BlockState placeState;
				final boolean placeHalf;

				if (isSlab && isTopHalf) {
					placePos = pos;
					placeState = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
					placeHalf = false;
				} else {
					placePos = pos.below();
					placeState = isSlab ? state.setValue(SlabBlock.TYPE, SlabType.TOP) : state;
					placeHalf = true;
				}

				if (placePos != pos && canPlace(world, pos)) {
					world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				}
				if (!blacklistedPos.contains(getHalfPos(placePos, placeHalf)) && canPlace(world, placePos)) {
					world.setBlockAndUpdate(placePos, placeState);
				}
			});
		}

		private boolean create(boolean includeMiddle, Consumer<Vec3> consumer) {
			final long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < 2) {
				final Vec3 pos1 = rail.getPosition(distance);
				distance += INCREMENT;
				final Vec3 pos2 = rail.getPosition(distance);
				final Vec3 vec3 = new Vec3(pos2.x - pos1.x, 0, pos2.z - pos1.z).normalize().yRot((float) Math.PI / 2);

				for (double x = -radius; x <= radius; x += INCREMENT) {
					final Vec3 editPos = pos1.add(vec3.multiply(x, 0, x));
					final boolean wholeNumber = Math.floor(editPos.y) == Math.ceil(editPos.y);
					if (includeMiddle || Math.abs(x) > radius - INCREMENT) {
						for (int y = 0; y <= height; y++) {
							if (y < height || !wholeNumber) {
								consumer.accept(editPos.add(0, y, 0));
							}
						}
					} else {
						consumer.accept(editPos.add(0, Math.max(0, wholeNumber ? height - 1 : height), 0));
					}
				}

				if (length - distance < INCREMENT) {
					showProgressMessage(100);
					return true;
				}
			}

			showProgressMessage(RailwayData.round(100 * distance / length, 1));
			return false;
		}

		private void showProgressMessage(float percentage) {
			final Player player = world.getPlayerByUUID(uuid);
			if (player != null) {
				player.sendOverlayMessage(Text.translatable("gui.mtr." + railActionType.progressTranslation, percentage));
			}
		}

		private static boolean canPlace(Level world, BlockPos pos) {
			return world.getBlockEntity(pos) == null && !(world.getBlockState(pos).getBlock() instanceof BlockNode);
		}

		private static BlockPos getHalfPos(BlockPos pos, boolean isTopHalf) {
			return RailwayData.newBlockPos(pos.getX(), pos.getY() * 2 + (isTopHalf ? 1 : 0), pos.getZ());
		}
	}

	@FunctionalInterface
	public interface RenderRail {
		void renderRail(double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4, double y1, double y2);
	}

	public enum RailActionType {
		BRIDGE("percentage_complete_bridge", "rail_action_bridge", 0xCCCCCC),
		TUNNEL("percentage_complete_tunnel", "rail_action_tunnel", 0x663300),
		TUNNEL_WALL("percentage_complete_tunnel_wall", "rail_action_tunnel_wall", 0x999999);

		private final String progressTranslation;
		private final String nameTranslation;
		private final int color;

		RailActionType(String progressTranslation, String nameTranslation, int color) {
			this.progressTranslation = progressTranslation;
			this.nameTranslation = nameTranslation;
			this.color = color;
		}
	}
}
