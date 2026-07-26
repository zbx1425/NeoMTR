package mtr.data;

import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import mtr.Items;
import mtr.Keys;
import mtr.MTR;
import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPlatform;
import mtr.block.IBlock;
import mtr.packet.IPacket;
import mtr.path.PathData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;

import java.io.*;
import java.util.*;

public abstract class Train extends NameColorDataBase implements IPacket {

	protected float speed;
	protected double railProgress;
	protected boolean doorTarget;
	protected float doorValue;
	protected float elapsedDwellTicks;
	protected int nextStoppingIndex;
	protected int nextPlatformIndex;
	protected boolean reversed;
	protected boolean isOnRoute = false;
	protected boolean isCurrentlyManual;
	protected int manualNotch;

	public final long sidingId;
	public final String trainId;
	public final String baseTrainType;
	public final TransportMode transportMode;
	public final int spacing;
	public final int width;
	public final int trainCars;
	public final float accelerationConstant;
	public final boolean isManualAllowed;
	public final int maxManualSpeed;
	public final int manualToAutomaticTime;
	public final List<PathData> path;

	protected final List<Double> distances;
	protected final int repeatIndex1;
	protected final int repeatIndex2;
	protected final Set<UUID> ridingEntities = new HashSet<>();

	protected final float railLength;

	public static final float ACCELERATION_DEFAULT = 0.01F; // m/tick^2
	public static final float MAX_ACCELERATION = 0.02F; // m/tick^2
	public static final float MIN_ACCELERATION = 0.001F; // m/tick^2
	public static final int DOOR_MOVE_TIME = 64;
	protected static final int MAX_CHECK_DISTANCE = 32;
	public static final int DOOR_DELAY = 60;

	private static final String KEY_SPEED = "speed";
	private static final String KEY_RAIL_PROGRESS = "rail_progress";
	private static final String KEY_ELAPSED_DWELL_TICKS = "stop_counter";
	private static final String KEY_NEXT_STOPPING_INDEX = "next_stopping_index";
	private static final String KEY_NEXT_PLATFORM_INDEX = "next_platform_index";
	private static final String KEY_REVERSED = "reversed";
	private static final String KEY_IS_CURRENTLY_MANUAL = "is_currently_manual";
	private static final String KEY_IS_ON_ROUTE = "is_on_route";
	private static final String KEY_TRAIN_TYPE = "train_type";
	private static final String KEY_TRAIN_CUSTOM_ID = "train_custom_id";
	private static final String KEY_RIDING_ENTITIES = "riding_entities";
	private static final String KEY_CARGO = "cargo";

	public Train(long id, long sidingId, float railLength, String trainId, String baseTrainType, int trainCars, List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2, float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime) {
		super(id);
		this.sidingId = sidingId;
		this.railLength = RailwayData.round(railLength, 3);
		this.trainId = trainId;
		// TODO temporary code for backwards compatibility
		baseTrainType = baseTrainType.startsWith("base_") ? baseTrainType.replace("base_", "train_") : baseTrainType;
		// TODO temporary code end
		this.baseTrainType = baseTrainType;
		transportMode = TrainType.getTransportMode(baseTrainType);
		spacing = TrainType.getSpacing(baseTrainType);
		width = TrainType.getWidth(baseTrainType);
		this.trainCars = trainCars;
		this.isManualAllowed = isManualAllowed;
		isCurrentlyManual = isManualAllowed;
		this.maxManualSpeed = maxManualSpeed;
		this.manualToAutomaticTime = manualToAutomaticTime;
		this.path = path;
		this.distances = distances;
		this.repeatIndex1 = repeatIndex1;
		this.repeatIndex2 = repeatIndex2;
		final float tempAccelerationConstant = RailwayData.round(accelerationConstant, 4);
		this.accelerationConstant = tempAccelerationConstant <= 0 ? ACCELERATION_DEFAULT : tempAccelerationConstant;
	}

	public Train(
			long sidingId, float railLength,
			List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
			float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
			Map<String, Value> map
	) {
		super(map);
		final MessagePackHelper messagePackHelper = new MessagePackHelper(map);

		this.sidingId = sidingId;
		this.railLength = RailwayData.round(railLength, 3);
		this.path = path;
		this.distances = distances;
		this.repeatIndex1 = repeatIndex1;
		this.repeatIndex2 = repeatIndex2;
		this.accelerationConstant = accelerationConstant;
		this.isManualAllowed = isManualAllowed;
		this.maxManualSpeed = maxManualSpeed;
		this.manualToAutomaticTime = manualToAutomaticTime;

		speed = messagePackHelper.getFloat(KEY_SPEED);
		railProgress = messagePackHelper.getDouble(KEY_RAIL_PROGRESS);
		elapsedDwellTicks = messagePackHelper.getFloat(KEY_ELAPSED_DWELL_TICKS);
		nextStoppingIndex = messagePackHelper.getInt(KEY_NEXT_STOPPING_INDEX);
		nextPlatformIndex = messagePackHelper.getInt(KEY_NEXT_PLATFORM_INDEX);
		reversed = messagePackHelper.getBoolean(KEY_REVERSED);

		final String tempTrainId = messagePackHelper.getString(KEY_TRAIN_CUSTOM_ID).toLowerCase(Locale.ENGLISH);
		// TODO temporary code for backwards compatibility
		String tempBaseTrainType = messagePackHelper.getString(KEY_TRAIN_TYPE).toLowerCase(Locale.ENGLISH);
		baseTrainType = tempBaseTrainType.startsWith("base_") ? tempBaseTrainType.replace("base_", "train_") : tempBaseTrainType;
		// TODO temporary code end
		trainId = tempTrainId.isEmpty() ? baseTrainType : tempTrainId;
		transportMode = TrainType.getTransportMode(baseTrainType);
		spacing = TrainType.getSpacing(baseTrainType);
		width = TrainType.getWidth(baseTrainType);
		trainCars = Math.min(transportMode.maxLength, (int) Math.floor(railLength / spacing));
		isCurrentlyManual = messagePackHelper.getBoolean(KEY_IS_CURRENTLY_MANUAL);

		isOnRoute = messagePackHelper.getBoolean(KEY_IS_ON_ROUTE);
		messagePackHelper.iterateArrayValue(KEY_RIDING_ENTITIES, value -> ridingEntities.add(UUID.fromString(value.asStringValue().asString())));

		SimpleContainer inventory1 = new SimpleContainer(trainCars);
		if (map.containsKey(KEY_CARGO) && !map.get(KEY_CARGO).isNilValue()) {
			final byte[] rawNbt = map.get(KEY_CARGO).asBinaryValue().asByteArray();
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(rawNbt);
			try {
				final CompoundTag compoundTag = NbtIo.read(new DataInputStream(inputStream));
				final NonNullList<ItemStack> stacks = NonNullList.withSize(trainCars, ItemStack.EMPTY);
//				ContainerHelper.loadAllItems(compoundTag.getCompound(KEY_CARGO), stacks);
				inventory1 = new SimpleContainer(stacks.toArray(new ItemStack[0]));
			} catch (IOException e) {
				MTR.LOGGER.error("", e);
			}
		}
	}

	@Deprecated
	public Train(
			long sidingId, float railLength,
			List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
			float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
			CompoundTag compoundTag
	) {
		super(compoundTag);

		this.sidingId = sidingId;
		this.railLength = RailwayData.round(railLength, 3);
		this.path = path;
		this.distances = distances;
		this.repeatIndex1 = repeatIndex1;
		this.repeatIndex2 = repeatIndex2;
		this.accelerationConstant = accelerationConstant;
		this.isManualAllowed = isManualAllowed;
		this.maxManualSpeed = maxManualSpeed;
		this.manualToAutomaticTime = manualToAutomaticTime;

		speed = compoundTag.getFloatOr(KEY_SPEED, 0);
		railProgress = compoundTag.getDoubleOr(KEY_RAIL_PROGRESS, 0);
		elapsedDwellTicks = compoundTag.getFloatOr(KEY_ELAPSED_DWELL_TICKS, 0);
		nextStoppingIndex = compoundTag.getIntOr(KEY_NEXT_STOPPING_INDEX, 0);
		nextPlatformIndex = compoundTag.getIntOr(KEY_NEXT_PLATFORM_INDEX, 0);
		reversed = compoundTag.getBooleanOr(KEY_REVERSED, false);

		trainId = compoundTag.getStringOr(KEY_TRAIN_CUSTOM_ID, "");
		baseTrainType = compoundTag.getStringOr(KEY_TRAIN_TYPE, "");
		transportMode = TrainType.getTransportMode(baseTrainType);
		spacing = TrainType.getSpacing(baseTrainType);
		width = TrainType.getWidth(baseTrainType);
		trainCars = Math.min(transportMode.maxLength, (int) Math.floor(railLength / spacing));
		isCurrentlyManual = compoundTag.getBooleanOr(KEY_IS_CURRENTLY_MANUAL, false);

		isOnRoute = compoundTag.getBooleanOr(KEY_IS_ON_ROUTE, false);
		final CompoundTag tagRidingEntities = compoundTag.getCompoundOrEmpty(KEY_RIDING_ENTITIES);
		tagRidingEntities.keySet().forEach(key -> ridingEntities.add(UUID.fromString(tagRidingEntities.getStringOr(key, ""))));

		final NonNullList<ItemStack> stacks = NonNullList.withSize(trainCars, ItemStack.EMPTY);
	}

	public Train(FriendlyByteBuf packet) {
		super(packet);

		path = new ArrayList<>();
		distances = new ArrayList<>();
		final int pathSize = packet.readVarInt();
		for (int i = 0; i < pathSize; i++) {
			path.add(new PathData(packet));
			distances.add(packet.readDouble());
		}
		repeatIndex1 = packet.readVarInt();
		repeatIndex2 = packet.readVarInt();

		sidingId = packet.readLong();
		railLength = RailwayData.round(packet.readFloat(), 3);
		speed = packet.readFloat();
		final float tempAccelerationConstant = RailwayData.round(packet.readFloat(), 4);
		accelerationConstant = tempAccelerationConstant <= 0 ? ACCELERATION_DEFAULT : tempAccelerationConstant;
		railProgress = packet.readDouble();
		elapsedDwellTicks = packet.readFloat();
		nextStoppingIndex = packet.readVarInt();
		nextPlatformIndex = packet.readVarInt();
		reversed = packet.readBoolean();
		trainId = packet.readUtf(PACKET_STRING_READ_LENGTH);
		baseTrainType = packet.readUtf(PACKET_STRING_READ_LENGTH);
		transportMode = TrainType.getTransportMode(baseTrainType);
		spacing = TrainType.getSpacing(baseTrainType);
		width = TrainType.getWidth(baseTrainType);
		trainCars = Math.min(transportMode.maxLength, (int) Math.floor(railLength / spacing));
		isManualAllowed = packet.readBoolean();
		isCurrentlyManual = packet.readBoolean();
		maxManualSpeed = packet.readVarInt();
		manualToAutomaticTime = packet.readVarInt();
		isOnRoute = packet.readBoolean();
		manualNotch = packet.readVarInt();
		doorTarget = packet.readBoolean();

		final int ridingEntitiesCount = packet.readVarInt();
		for (int i = 0; i < ridingEntitiesCount; i++) {
			ridingEntities.add(packet.readUUID());
		}

	}

	@Override
	public void toMessagePack(MessagePacker messagePacker) throws IOException {
		super.toMessagePack(messagePacker);

		messagePacker.packString(KEY_SPEED).packFloat(speed);
		messagePacker.packString(KEY_RAIL_PROGRESS).packDouble(railProgress);
		messagePacker.packString(KEY_ELAPSED_DWELL_TICKS).packFloat(elapsedDwellTicks);
		messagePacker.packString(KEY_NEXT_STOPPING_INDEX).packLong(nextStoppingIndex);
		messagePacker.packString(KEY_NEXT_PLATFORM_INDEX).packLong(nextPlatformIndex);
		messagePacker.packString(KEY_REVERSED).packBoolean(reversed);
		messagePacker.packString(KEY_TRAIN_CUSTOM_ID).packString(trainId);
		messagePacker.packString(KEY_TRAIN_TYPE).packString(baseTrainType);
		messagePacker.packString(KEY_IS_CURRENTLY_MANUAL).packBoolean(isCurrentlyManual);
		messagePacker.packString(KEY_IS_ON_ROUTE).packBoolean(isOnRoute);

		messagePacker.packString(KEY_RIDING_ENTITIES).packArrayHeader(ridingEntities.size());
		for (final UUID uuid : ridingEntities) {
			messagePacker.packString(uuid.toString());
		}

		messagePacker.packString(KEY_CARGO);
		messagePacker.packNil();
	}

	@Override
	public int messagePackLength() {
		return super.messagePackLength() + 12;
	}

	@Override
	public void writePacket(FriendlyByteBuf packet) {
		super.writePacket(packet);

		final int pathSize = Math.min(path.size(), distances.size());
		packet.writeVarInt(pathSize);
		for (int i = 0; i < pathSize; i++) {
			path.get(i).writePacket(packet);
			packet.writeDouble(distances.get(i));
		}
		packet.writeVarInt(repeatIndex1);
		packet.writeVarInt(repeatIndex2);

		packet.writeLong(sidingId);
		packet.writeFloat(railLength);
		packet.writeFloat(speed);
		packet.writeFloat(accelerationConstant);
		packet.writeDouble(railProgress);
		packet.writeFloat(elapsedDwellTicks);
		packet.writeVarInt(nextStoppingIndex);
		packet.writeVarInt(nextPlatformIndex);
		packet.writeBoolean(reversed);
		packet.writeUtf(trainId);
		packet.writeUtf(baseTrainType);
		packet.writeBoolean(isManualAllowed);
		packet.writeBoolean(isCurrentlyManual);
		packet.writeVarInt(maxManualSpeed);
		packet.writeVarInt(manualToAutomaticTime);
		packet.writeBoolean(isOnRoute);
		packet.writeVarInt(manualNotch);
		packet.writeBoolean(doorTarget);
		packet.writeVarInt(ridingEntities.size());
		ridingEntities.forEach(packet::writeUUID);
	}

	@Override
	protected final boolean hasTransportMode() {
		return false;
	}

	public final boolean getIsOnRoute() {
		return isOnRoute;
	}

	public final double getRailProgress() {
		return railProgress;
	}

	public final boolean closeToDepot(int trainDistance) {
		return !isOnRoute || railProgress < trainDistance + railLength;
	}

	public final boolean isCurrentlyManual() {
		return isCurrentlyManual;
	}

	public boolean changeManualSpeed(boolean isAccelerate) {
		if (doorValue == 0 && isAccelerate && manualNotch >= -2 && manualNotch < 2) {
			manualNotch++;
			return true;
		} else if (!isAccelerate && manualNotch > -2) {
			manualNotch--;
			return true;
		} else {
			return false;
		}
	}

	public boolean toggleDoors() {
		if (speed == 0) {
			doorTarget = !doorTarget;
			manualNotch = -2;
			return true;
		} else {
			doorTarget = false;
			return false;
		}
	}

	public final int getIndex(int car, int trainSpacing, boolean roundDown) {
		return getIndex(getRailProgress(car, trainSpacing), roundDown);
	}

	public final int getIndex(double targetDistance, boolean roundDown) {
		int left = 0;
    int right = distances.size() - 1;
    int result = -1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        double midVal = distances.get(mid);
        if (roundDown ? (midVal >= targetDistance) : (midVal > targetDistance)) {
            result = mid;
            right = mid - 1;
        } else {
            left = mid + 1;
        }
    }
    return (result == -1) ? distances.size() - 1 : result;
	}

	public final float getRailSpeed(int railIndex) {
		final RailType thisRail = path.get(railIndex).rail.railType;
		final float railSpeed;
		if (thisRail.canAccelerate) {
			railSpeed = thisRail.maxBlocksPerTick;
		} else {
			final RailType lastRail = railIndex > 0 ? path.get(railIndex - 1).rail.railType : thisRail;
			railSpeed = Math.max(lastRail.canAccelerate ? lastRail.maxBlocksPerTick : RailType.getDefaultMaxBlocksPerTick(transportMode), speed);
		}
		return railSpeed;
	}

	public final boolean isPlayerRiding(Player player) {
		return ridingEntities.contains(player.getUUID());
	}

	public int getNextStopIndex() {
		return nextStoppingIndex;
	}

	public final float getSpeed() {
		return speed;
	}

	public final float getDoorValue() {
		return doorValue;
	}

	public final float getElapsedDwellTicks() {
		return elapsedDwellTicks;
	}

	public final boolean isReversed() {
		return reversed;
	}

	public final boolean isOnRoute() {
		return isOnRoute;
	}

	public int getTotalDwellTicks() {
		return path.get(nextStoppingIndex).dwellTime * 10;
	}

	protected Vec3[] keyPointsPositions;

	protected final void simulateTrain(Level world, float ticksElapsed, Depot depot) {
		if (world == null) {
			return;
		}

		try {
			if (nextStoppingIndex >= path.size()) {
				return;
			}

			final boolean tempDoorOpen;
			final float tempDoorValue;
			final int totalDwellTicks = getTotalDwellTicks();

			if (!isOnRoute) {
				railProgress = (railLength + trainCars * spacing) / 2;
				reversed = false;
				tempDoorOpen = false;
				tempDoorValue = 0;
				speed = 0;
				nextStoppingIndex = 0;

				if (!isCurrentlyManual && canDeploy(depot) || isCurrentlyManual && manualNotch > 0) {
					startUp(world, trainCars, spacing, isOppositeRail());
				}
			} else {
				final float newAcceleration = accelerationConstant * ticksElapsed;

				if (railProgress >= distances.get(distances.size() - 1) - (railLength - trainCars * spacing) / 2) {
					isOnRoute = false;
					manualNotch = -2;
					ridingEntities.clear();
					tempDoorOpen = false;
					tempDoorValue = 0;
				} else {
					if (speed <= 0) {
						speed = 0;

						final boolean isOppositeRail = isOppositeRail();
						final boolean railBlocked = isRailBlocked(getIndex(0, spacing, true) + (isOppositeRail ? 2 : 1));

						if (totalDwellTicks == 0) {
							tempDoorOpen = false;
						} else {
							if (elapsedDwellTicks == 0 && isRepeat() && getIndex(railProgress, false) >= repeatIndex2 && distances.size() > repeatIndex1) {
								if (path.get(repeatIndex2).isOppositeRail(path.get(repeatIndex1))) {
									railProgress = distances.get(repeatIndex1 - 1) + trainCars * spacing;
									reversed = !reversed;
								} else {
									railProgress = distances.get(repeatIndex1);
								}
							}

							if (elapsedDwellTicks < totalDwellTicks - DOOR_MOVE_TIME - DOOR_DELAY - ticksElapsed || !railBlocked) {
								elapsedDwellTicks += ticksElapsed;
							}

							tempDoorOpen = openDoors();
						}

						if (!world.isClientSide() && (isCurrentlyManual || elapsedDwellTicks >= totalDwellTicks) && !railBlocked && (!isCurrentlyManual || manualNotch > 0)) {
							startUp(world, trainCars, spacing, isOppositeRail);
						}
					} else {
						if (!world.isClientSide()) {
							final int checkIndex = getIndex(0, spacing, true) + 1;
							if (isRailBlocked(checkIndex)) {
								nextStoppingIndex = checkIndex - 1;
							} else if (nextPlatformIndex > 0 && nextPlatformIndex < path.size()) {
								nextStoppingIndex = nextPlatformIndex;
								if (manualNotch < -2) {
									manualNotch = 0;
								}
							}
						}

						if (transportMode.continuousMovement || isCurrentlyManual) {
							final double stoppingDistance = distances.get(nextStoppingIndex) - railProgress;
							if (!transportMode.continuousMovement && stoppingDistance < 0.5 * speed * speed / accelerationConstant) {
								speed = stoppingDistance <= 0 ? Train.ACCELERATION_DEFAULT : (float) Math.max(speed - (0.5 * speed * speed / stoppingDistance) * ticksElapsed, Train.ACCELERATION_DEFAULT);
								manualNotch = -3;
							} else {
								if (isCurrentlyManual) {
									if (manualNotch >= -2 && manualNotch <= 2) {
										final RailType railType = convertMaxManualSpeed(maxManualSpeed);
										speed = Mth.clamp(speed + manualNotch * newAcceleration / 2, 0, railType == null ? RailType.IRON.maxBlocksPerTick : railType.maxBlocksPerTick);
									}
								} else {
									final float railSpeed = getRailSpeed(getIndex(0, spacing, false));
									if (speed < railSpeed) {
										speed = Math.min(speed + newAcceleration, railSpeed);
										manualNotch = 2;
									} else if (speed > railSpeed) {
										speed = Math.max(speed - newAcceleration, railSpeed);
										manualNotch = -2;
									} else {
										manualNotch = 0;
									}
								}
							}
						} else {
							double lookAheadDistance = Math.max(300, Math.pow(speed, 2) / (2 * accelerationConstant));
							float atpYellowSpeed = Float.POSITIVE_INFINITY;
							for (int i = getIndex(railProgress - spacing * trainCars, true);
									i < path.size() && distances.get(i) < railProgress + lookAheadDistance; i++) {
									PathData pathSeg = path.get(i);
									if (i > 0 && distances.get(i - 1) < railProgress && distances.get(i) > railProgress - spacing * trainCars) {
											// Persisting speed limit
											atpYellowSpeed = Math.min(atpYellowSpeed, pathSeg.rail.railType.maxBlocksPerTick);
									}
									// Stop point pattern based on stoppingDistance
									final double stoppingDistance = distances.get(nextStoppingIndex) - railProgress;
									atpYellowSpeed = Math.min(atpYellowSpeed, (float)Math.sqrt(2 * accelerationConstant * stoppingDistance));
									if (pathSeg.dwellTime > 0) {
										// This is already handled by stoppingDistance
									} else {
											if (i > 0 && distances.get(i - 1) > railProgress) {
													// Decelerate to start of speed limit
													float yellowPatternSpeed = (float) Math.sqrt(2 * accelerationConstant * (distances.get(i - 1) - railProgress)
																					+ Math.pow(pathSeg.rail.railType.maxBlocksPerTick, 2));
													if (yellowPatternSpeed < atpYellowSpeed) {
															atpYellowSpeed = yellowPatternSpeed;
													}
											}
									}
							}
							if (speed < atpYellowSpeed) {
								speed = Math.min(speed + newAcceleration, atpYellowSpeed);
								manualNotch = 2;
							} else if (speed > atpYellowSpeed) {
								speed = atpYellowSpeed;
								manualNotch = -2;
							} else {
								manualNotch = 0;
							}
						}
						tempDoorOpen = transportMode.continuousMovement && openDoors();
					}

					railProgress += speed * ticksElapsed;
					if (!transportMode.continuousMovement && railProgress > distances.get(nextStoppingIndex)) {
						railProgress = distances.get(nextStoppingIndex);
						speed = 0;
						manualNotch = -2;
					}

					tempDoorValue = Mth.clamp(doorValue + ticksElapsed * (doorTarget ? 1 : -1) / DOOR_MOVE_TIME, 0, 1);
				}
			}

			doorTarget = tempDoorOpen;
			doorValue = tempDoorValue;
			if (doorTarget || doorValue != 0) {
				manualNotch = -2;
			}

			if (!path.isEmpty()) {
				keyPointsPositions = getPositions();
				if (handlePositions(world, keyPointsPositions, ticksElapsed, false)) {
					final double[] prevX = {0};
					final double[] prevY = {0};
					final double[] prevZ = {0};
					final float[] prevYaw = {0};
					final float[] prevPitch = {0};
					final float[] prevRoll = {0};

					for (int i = 0; i < trainCars; i++) {
						final int ridingCar = i;
						calculateCar(world, keyPointsPositions, i, totalDwellTicks, false, (x, y, z, yaw, pitch, roll, realSpacing, doorLeftOpen, doorRightOpen) -> {
							simulateCar(
									world, ridingCar, ticksElapsed,
									x, y, z,
									yaw, pitch, roll,
									prevX[0], prevY[0], prevZ[0],
									prevYaw[0], prevPitch[0], prevRoll[0],
									doorLeftOpen, doorRightOpen, realSpacing
							);
							prevX[0] = x;
							prevY[0] = y;
							prevZ[0] = z;
							prevYaw[0] = yaw;
							prevPitch[0] = pitch;
							prevRoll[0] = roll;
						});
					}
				}
			}
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	/**
	 * @param isRendering If it's the renderTrain pass on client (Door logic should only be handled there)
	 */
	protected void calculateCar(Level world, Vec3[] positions, int index, int dwellTicks, boolean isRendering, CalculateCarCallback calculateCarCallback) {
		final Vec3 pos1 = positions[index * 2];
		final Vec3 pos2 = positions[index * 2 + 1];

		if (pos1 != null && pos2 != null) {
			final double x = getAverage(pos1.x, pos2.x);
			final double y = getAverage(pos1.y, pos2.y) + 1;
			final double z = getAverage(pos1.z, pos2.z);

			final double realSpacing = spacing;
			final float yaw = (float) Mth.atan2(pos2.x - pos1.x, pos2.z - pos1.z);
			final float pitch = realSpacing == 0 ? 0 : (float) asin((pos2.y - pos1.y) / realSpacing);
			final boolean doorLeftOpen = scanDoors(world, x, y, z, (float) Math.PI + yaw, pitch, realSpacing / 2, dwellTicks, isRendering) && doorValue > 0;
			final boolean doorRightOpen = scanDoors(world, x, y, z, yaw, pitch, realSpacing / 2, dwellTicks, isRendering) && doorValue > 0;

			calculateCarCallback.calculateCarCallback(x, y, z, yaw, pitch, 0, realSpacing, doorLeftOpen, doorRightOpen);
		}
	}

	protected Vec3[] getPositions() {
		final Vec3[] positions = new Vec3[trainCars * 2];
		for (int i = 0; i < trainCars; i++) {
			double centerOffset = i * spacing + spacing / 2.0;
			centerOffset = reversed ? trainCars * spacing - centerOffset : centerOffset;
			double bogiePosition = (getBogiePosition() == 0 || getIsJacobsBogie()) ? (spacing - 1) / 2.0 : getBogiePosition();
			double kr = reversed ? -1 : 1;
			double bogieFOffset = centerOffset - kr * bogiePosition;
			double bogieBOffset = centerOffset + kr * bogiePosition;
//					if (getIsJacobsBogie() && i != 0) bogieFOffset = centerOffset - kr * spacing / 2.0;
//					if (getIsJacobsBogie() && i != trainCars - 1) bogieBOffset = centerOffset + kr * spacing / 2.0;
			positions[i * 2] = getRoutePosition(bogieFOffset);
			positions[i * 2 + 1] = getRoutePosition(bogieBOffset);
		}
		return positions;
	}

	protected void startUp(Level world, int trainCars, int trainSpacing, boolean isOppositeRail) {
		doorTarget = false;
		doorValue = 0;
		nextPlatformIndex = nextStoppingIndex;
	}

	protected boolean openDoors() {
		return doorTarget;
	}

	protected float getModelZOffset() {
		return 0;
	}

	protected float getBogiePosition() {
		return 0;
	}

	protected boolean getIsJacobsBogie() {
		return false;
	}

	protected boolean isRepeat() {
		return repeatIndex1 > 0 && repeatIndex2 > 0;
	}

	protected abstract void simulateCar(
			Level world, int ridingCar, float ticksElapsed,
			double carX, double carY, double carZ, float carYaw, float carPitch, float carRoll,
			double prevCarX, double prevCarY, double prevCarZ, float prevCarYaw, float prevCarPitch, float prevCarRoll,
			boolean doorLeftOpen, boolean doorRightOpen, double realSpacing
	);

	protected abstract boolean handlePositions(Level world, Vec3[] positions, float ticksElapsed, boolean shouldOpenDoors);

	protected abstract boolean canDeploy(Depot depot);

	protected abstract boolean isRailBlocked(int checkIndex);

	protected abstract boolean skipScanBlocks(Level world, double trainX, double trainY, double trainZ);

	protected abstract void openDoors(Level world, Block block, BlockPos checkPos, int dwellTicks);

	protected abstract double asin(double value);

	private boolean isOppositeRail() {
		return path.size() > nextStoppingIndex + 1 && railProgress == distances.get(nextStoppingIndex) && path.get(nextStoppingIndex).isOppositeRail(path.get(nextStoppingIndex + 1));
	}

	private double getRailProgress(int car, int trainSpacing) {
		return railProgress - car * trainSpacing;
	}

	private Vec3 getRoutePosition(double offset) {
		final double tempRailProgress = Math.max(railProgress - offset - getModelZOffset(), 0);
		final int index = getIndex(tempRailProgress, false);
		return path.get(index).rail.getPosition(tempRailProgress - (index == 0 ? 0 : distances.get(index - 1))).add(0, transportMode.railOffset, 0);
	}

	protected boolean scanDoors(Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks, boolean shouldOpenDoors) {
		if (skipScanBlocks(world, trainX, trainY, trainZ)) {
			return false;
		}

		boolean hasPlatform = false;
		final Vec3 offsetVec = new Vec3(1, 0, 0).yRot(checkYaw).xRot(pitch);
		final Vec3 traverseVec = new Vec3(0, 0, 1).yRot(checkYaw).xRot(pitch);


		for (double checkZ = -halfSpacing; checkZ <= halfSpacing; checkZ++) {
			for (int checkX = 1; checkX <= 3; checkX++) {
				for (int checkY = 3; checkY >= -2; checkY--) {
					final BlockPos checkPos = RailwayData.newBlockPos(trainX + offsetVec.x * checkX + traverseVec.x * checkZ, trainY + checkY, trainZ + offsetVec.z * checkX + traverseVec.z * checkZ);
					final BlockState state = world.getBlockState(checkPos);
					// TODO: TEMP Code for custom PSD
					if(state.getBlock() instanceof BlockEyeCandy) {
						if (shouldOpenDoors) openDoors(world, state.getBlock(), checkPos, dwellTicks);
						hasPlatform = true;
					}

					if (state.getBlock() instanceof BlockPSDAPGBase) {
						// PSD/APG glass are not considered platform since you can't exit from there
						if (state.getBlock() instanceof BlockPSDAPGDoorBase) {
							if (IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.UNLOCKED)) {
								if (shouldOpenDoors) openDoors(world, state.getBlock(), checkPos, dwellTicks);
								hasPlatform = true;
							}
						}
						// When the platform has PSD/APG, use them to determine if the doors can open
						// Thus, skip checking the platform block below them
						break;
					}
					if (state.getBlock() instanceof BlockPlatform) {
						// The platform has no PSD/APG, and platform blocks are present, doors can open
						hasPlatform = true;
					}
				}
				// On server we only care about platform presence, no need to check further to open doors
				if (hasPlatform && !shouldOpenDoors) return true;
			}
		}

		return hasPlatform;
	}

	public static boolean isHoldingKey(Player player) {
		return player != null && !Keys.LIFTS_ONLY && player.isHolding(Items.DRIVER_KEY.get());
	}

	public static double getAverage(double a, double b) {
		return (a + b) / 2;
	}

	public static RailType convertMaxManualSpeed(int maxManualSpeed) {
		if (maxManualSpeed >= 0 && maxManualSpeed <= RailType.DIAMOND.ordinal()) {
			return RailType.values()[maxManualSpeed];
		} else {
			return null;
		}
	}

	@FunctionalInterface
	protected interface CalculateCarCallback {
		void calculateCarCallback(double x, double y, double z, float yaw, float pitch, float roll, double realSpacing, boolean doorLeftOpen, boolean doorRightOpen);
	}
}
