package mtr.data;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import mtr.MTR;
import mtr.MTRClient;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.client.*;
import mtr.render.RenderDrivingOverlay;
import mtr.render.TrainRendererBase;
import mtr.sound.train.TrainSoundBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TrainClient extends Train implements IGui {

	public boolean isRemoved = false;
	private boolean justMounted;
	private float oldSpeed;
	private double oldRailProgress;
	private float oldDoorValue;
	private boolean doorOpening;
	private boolean isSitting;
	private boolean previousShifting;

	private int currentStationIndex;
	private Route thisRoute;
	private Route nextRoute;
	private Station thisStation;
	private Station nextStation;
	private Station lastStation;

	private SpeedCallback speedCallback;
	private AnnouncementCallback announcementCallback;
	private AnnouncementCallback lightRailAnnouncementCallback;
	private Depot depot;
	private List<Long> routeIds = new ArrayList<>();

	public final TrainRendererBase trainRenderer;
	public final TrainSoundBase trainSound;
	public final VehicleRidingClient vehicleRidingClient = new VehicleRidingClient(ridingEntities, PACKET_UPDATE_TRAIN_PASSENGER_POSITION);
	public final List<ScrollingText> scrollingTexts = new ArrayList<>();

	private final Set<Runnable> trainTranslucentRenders = new HashSet<>();

	private static final float CONNECTION_HEIGHT = 2.25F;
	private static final float CONNECTION_Z_OFFSET = 0.5F;
	private static final float CONNECTION_X_OFFSET = 0.25F;

	public TrainClient(FriendlyByteBuf packet) {
		super(packet);
		final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(trainId);
		trainRenderer = trainProperties.renderer.createTrainInstance(this);
		trainSound = trainProperties.sound.createTrainInstance(this);
	}

	@Override
	protected void simulateCar(
			Level world, int ridingCar, float ticksElapsed,
			double carX, double carY, double carZ, float carYaw, float carPitch, float carRoll,
			double prevCarX, double prevCarY, double prevCarZ, float prevCarYaw, float prevCarPitch, float prevCarRoll,
			boolean doorLeftOpen, boolean doorRightOpen, double realSpacing
	) {
	}

	protected void renderCar(
			Level world, int ridingCar, float ticksElapsed,
			double carX, double carY, double carZ, float carYaw, float carPitch, float carRoll,
			double prevCarX, double prevCarY, double prevCarZ, float prevCarYaw, float prevCarPitch, float prevCarRoll,
			boolean doorLeftOpen, boolean doorRightOpen, double realSpacing
	) {

		final LocalPlayer clientPlayer = Minecraft.getInstance().player;
		if (clientPlayer == null) {
			return;
		}

		final BlockPos soundPos = RailwayData.newBlockPos(carX, carY, carZ);
		if (ticksElapsed > 0) trainSound.playAllCars(world, soundPos, ridingCar);
		if (doorLeftOpen || doorRightOpen) {
			if (ticksElapsed > 0) trainSound.playAllCarsDoorOpening(world, soundPos, ridingCar);
		}

		final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(trainId);
		final float railSurfaceOffset = trainProperties.railSurfaceOffset;
		vehicleRidingClient.renderPlayers();

		doorOpening = doorValue > oldDoorValue;
		trainRenderer.renderCar(ridingCar, carX, carY, carZ, carYaw, carPitch, carRoll, doorLeftOpen, doorRightOpen);
		trainTranslucentRenders.add(() -> trainRenderer.renderCar(ridingCar, carX, carY, carZ, carYaw, carPitch, carRoll, doorLeftOpen, doorRightOpen));

		if (ridingCar > 0) {
			final Vec3 prevPos0 = withCarTransform(new Vec3(0, railSurfaceOffset, spacing / 2D - 1), prevCarX, prevCarY, prevCarZ, prevCarYaw, prevCarPitch, prevCarRoll, railSurfaceOffset);
			final Vec3 thisPos0 = withCarTransform(new Vec3(0, railSurfaceOffset, -(spacing / 2D - 1)), carX, carY, carZ, carYaw, carPitch, carRoll, railSurfaceOffset);
			final Vec3 connectPos = prevPos0.add(thisPos0).scale(0.5).add(0, -railSurfaceOffset, 0);
			final float connectYaw = (float) Mth.atan2(thisPos0.x - prevPos0.x, thisPos0.z - prevPos0.z);
			final double connectRealSpacing = thisPos0.distanceTo(prevPos0);
			final float connectPitch = (float) asin((thisPos0.y - prevPos0.y) / connectRealSpacing);
			final float connectRoll = (carRoll + prevCarRoll) / 2;

			for (int i = 0; i < 2; i++) {
				final double xStart = width / 2D + (i == 0 ? -1 : 0.5) * CONNECTION_X_OFFSET;
				final double zStart = spacing / 2D - (i == 0 ? 1 : 2) * CONNECTION_Z_OFFSET;

				final float SMALL_OFFSET = 0.05f;
				final Vec3 prevPos1 = withCarTransform(new Vec3(xStart, SMALL_OFFSET, zStart), prevCarX, prevCarY, prevCarZ, prevCarYaw, prevCarPitch, prevCarRoll, railSurfaceOffset);
				final Vec3 prevPos2 = withCarTransform(new Vec3(xStart, CONNECTION_HEIGHT + SMALL_OFFSET, zStart), prevCarX, prevCarY, prevCarZ, prevCarYaw, prevCarPitch, prevCarRoll, railSurfaceOffset);
				final Vec3 prevPos3 = withCarTransform(new Vec3(-xStart, CONNECTION_HEIGHT + SMALL_OFFSET, zStart), prevCarX, prevCarY, prevCarZ, prevCarYaw, prevCarPitch, prevCarRoll, railSurfaceOffset);
				final Vec3 prevPos4 = withCarTransform(new Vec3(-xStart, SMALL_OFFSET, zStart), prevCarX, prevCarY, prevCarZ, prevCarYaw, prevCarPitch, prevCarRoll, railSurfaceOffset);

				final Vec3 thisPos1 = withCarTransform(new Vec3(-xStart, SMALL_OFFSET, -zStart), carX, carY, carZ, carYaw, carPitch, carRoll, railSurfaceOffset);
				final Vec3 thisPos2 = withCarTransform(new Vec3(-xStart, CONNECTION_HEIGHT + SMALL_OFFSET, -zStart), carX, carY, carZ, carYaw, carPitch, carRoll, railSurfaceOffset);
				final Vec3 thisPos3 = withCarTransform(new Vec3(xStart, CONNECTION_HEIGHT + SMALL_OFFSET, -zStart), carX, carY, carZ, carYaw, carPitch, carRoll, railSurfaceOffset);
				final Vec3 thisPos4 = withCarTransform(new Vec3(xStart, SMALL_OFFSET, -zStart), carX, carY, carZ, carYaw, carPitch, carRoll, railSurfaceOffset);

				if (i == 0) {
					trainRenderer.renderConnection(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, connectPos.x, connectPos.y, connectPos.z, connectYaw, connectPitch, connectRoll);
				} else {
					trainRenderer.renderBarrier(prevPos1, prevPos2, prevPos3, prevPos4, thisPos1, thisPos2, thisPos3, thisPos4, connectPos.x, connectPos.y, connectPos.z, connectYaw, connectPitch, connectRoll);
				}
			}
		}
	}

	public void renderTrain(Level world, float ticksElapsed) {
		if (world == null) {
			return;
		}

		// Hide trains near TrainVirtualDrive
		if (TrainVirtualDrive.activeTrain != null && TrainVirtualDrive.activeTrain != this) {
			for (int i = getIndex(railProgress - spacing * trainCars, true);
				 i < path.size() && distances.get(i) < railProgress + 300; i++) {
				if (TrainVirtualDrive.activeTrain.railAheadLookup.contains(path.get(i).startingPos.asLong())) {
					trainSound.stopAll();
					return;
				}
			}
		}

		try {
			final int totalDwellTicks = getTotalDwellTicks();
			final double[] prevX = {0};
			final double[] prevY = {0};
			final double[] prevZ = {0};
			final float[] prevYaw = {0};
			final float[] prevPitch = {0};
			final float[] prevRoll = {0};

			for (int i = 0; i < trainCars; i++) {
				final int ridingCar = i;
				calculateCar(world, keyPointsPositions, i, totalDwellTicks, true, (x, y, z, yaw, pitch, roll, realSpacing, doorLeftOpen, doorRightOpen) -> {
					renderCar(
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
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	private static Vec3 withCarTransform(Vec3 child, double x, double y, double z, float yaw, float pitch, float roll, float railSurfaceOffset) {
		return child.add(0, -railSurfaceOffset, 0).zRot(roll).xRot(pitch).yRot(yaw).add(0, railSurfaceOffset, 0).add(x, y, z);
	}

	@Override
	protected boolean handlePositions(Level world, Vec3[] positions, float ticksElapsed, boolean isRendering) {
		final Minecraft client = Minecraft.getInstance();
		final LocalPlayer clientPlayer = client.player;
		if (clientPlayer == null) {
			return false;
		}

		vehicleRidingClient.begin();

		if (ticksElapsed > 0) {
			if (isPlayerRiding(clientPlayer)) {
				final int headIndex = getIndex(0, spacing, false);
				final int stopIndex = path.get(headIndex).stopIndex - 1;

				if (speedCallback != null) {
					speedCallback.speedCallback(speed * 20, stopIndex, routeIds);
				}

				if (announcementCallback != null) {
					final double targetProgress = distances.get(getPreviousStoppingIndex(headIndex)) + (trainCars + 1) * spacing;
					if (oldRailProgress < targetProgress && railProgress >= targetProgress) {
						announcementCallback.announcementCallback(stopIndex, routeIds);
					}
				}

				if (lightRailAnnouncementCallback != null && (justOpening() || justMounted)) {
					lightRailAnnouncementCallback.announcementCallback(stopIndex, routeIds);
				}
			}

			final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(trainId);
			vehicleRidingClient.movePlayer(uuid -> {
				final CalculateCarCallback calculateCarCallback = (x, y, z, yaw, pitch, roll, realSpacingRender, doorLeftOpenRender, doorRightOpenRender) -> vehicleRidingClient.setOffsets(uuid, x, y, z, yaw, pitch, transportMode.maxLength == 1 ? spacing : realSpacingRender, width, doorLeftOpenRender, doorRightOpenRender, transportMode.hasPitchAscending, transportMode.hasPitchDescending, trainProperties.riderOffset, trainProperties.riderOffsetDismounting, false, doorValue == 0, () -> {
					final boolean isShifting = clientPlayer.isShiftKeyDown();
					if (Config.shiftToToggleSitting() && !MTRClient.isVivecraft()) {
						if (isShifting && !previousShifting) {
							isSitting = !isSitting;
						}
						clientPlayer.setPose(isSitting && !client.gameRenderer.getMainCamera().isDetached() ? Pose.CROUCHING : Pose.STANDING);
					}
					previousShifting = isShifting;
				});

				final int currentRidingCar = Mth.clamp((int) Math.floor(vehicleRidingClient.getPercentageZ(uuid)), 0, trainCars - 1);
				calculateCar(world, positions, currentRidingCar, 0, isRendering, (x, y, z, yaw, pitch, roll, realSpacingRender, doorLeftOpenRender, doorRightOpenRender) -> {
					vehicleRidingClient.moveSelf(id, uuid, realSpacingRender, width, yaw, currentRidingCar, trainCars, doorLeftOpenRender, doorRightOpenRender, !trainProperties.hasGangwayConnection, ticksElapsed);

					final int newRidingCar = Mth.clamp((int) Math.floor(vehicleRidingClient.getPercentageZ(uuid)), 0, trainCars - 1);
					if (currentRidingCar == newRidingCar) {
						calculateCarCallback.calculateCarCallback(x, y, z, yaw, pitch, roll, realSpacingRender, doorLeftOpenRender, doorRightOpenRender);
					} else {
						calculateCar(world, positions, newRidingCar, 0, isRendering, calculateCarCallback);
					}
				});
			});
		}

		vehicleRidingClient.end();
		justMounted = false;

		return true;
	}

	private final PerlinNoise1D irregX = new PerlinNoise1D(1 / 8f, 3, 0.0200);
	private final PerlinNoise1D irregY = new PerlinNoise1D(1 / 8f, 3, 0.0100);
	private final PerlinNoise1D irregR = new PerlinNoise1D(1 / 8f, 3, 0.0100);

	@Override
	protected void calculateCar(Level world, Vec3[] positions, int index, int dwellTicks, boolean isRendering, CalculateCarCallback calculateCarCallback) {
		final Vec3 pos1 = positions[index * 2];
		final Vec3 pos2 = positions[index * 2 + 1];

		if (pos1 != null && pos2 != null) {
			double centerOffset = index * spacing + spacing / 2.0;
			centerOffset = reversed ? trainCars * spacing - centerOffset : centerOffset;
			double railProgress = this.railProgress - 10.0;
			double bogiePosition = getBogiePosition() == 0 ? spacing / 2.0 : getBogiePosition();
			double bogieFOffset = centerOffset - bogiePosition;
			double bogieBOffset = centerOffset + bogiePosition;
			if (getIsJacobsBogie() && index != 0) bogieFOffset = centerOffset - spacing / 2.0;
			if (getIsJacobsBogie() && index != trainCars - 1) bogieBOffset = centerOffset + spacing / 2.0;

			final float irregRatio = ClientConfig.hideRidingTrain ? 0
					: Mth.clamp(speed / (5.56f * 0.05f), 0, 1);

			final float roll = (float)(irregR.getAt(railProgress - bogieFOffset) + irregR.getAt(railProgress - bogieBOffset)) / 2 * irregRatio;
//			final float roll = (float)Math.toRadians(30);

			double irregY1 = irregY.getAt(railProgress - bogieFOffset) * irregRatio, irregY2 = irregY.getAt(railProgress - bogieBOffset) * irregRatio;
			final float yaw = (float) Mth.atan2(pos2.x - pos1.x, pos2.z - pos1.z);
			final Vec3 latIrreg = new Vec3((irregX.getAt(railProgress - bogieFOffset) + irregX.getAt(railProgress - bogieBOffset)) / 2 * irregRatio, 0, 0)
					.yRot(yaw);

			final double x = getAverage(pos1.x, pos2.x) + latIrreg.x;
			final double y = getAverage(pos1.y + irregY1, pos2.y + irregY2) + 1;
			final double z = getAverage(pos1.z, pos2.z) + latIrreg.z;

			final double realSpacing = spacing;
			final float pitch = realSpacing == 0 ? 0 : (float) asin((pos2.y + irregY2 - pos1.y - irregY1) / realSpacing);
			final boolean doorLeftOpen = scanDoors(world, x, y, z, (float) Math.PI + yaw, pitch, realSpacing / 2, dwellTicks, isRendering) && doorValue > 0;
			final boolean doorRightOpen = scanDoors(world, x, y, z, yaw, pitch, realSpacing / 2, dwellTicks, isRendering) && doorValue > 0;

			calculateCarCallback.calculateCarCallback(x, y, z, yaw, pitch, roll, realSpacing, doorLeftOpen, doorRightOpen);
		}
	}

	@Override
	protected boolean canDeploy(Depot depot) {
		return false;
	}

	@Override
	protected boolean isRailBlocked(int checkIndex) {
		return false;
	}

	@Override
	protected boolean skipScanBlocks(Level world, double trainX, double trainY, double trainZ) {
		return false;
	}

	@Override
	protected void openDoors(Level world, Block block, BlockPos checkPos, int dwellTicks) {
		for (int i = -1; i <= 1; i++) {
			final BlockPos doorPos = checkPos.above(i);
			final BlockState state = world.getBlockState(doorPos);
			final Block doorBlock = state.getBlock();
			final BlockEntity entity = world.getBlockEntity(doorPos);
			if (doorBlock instanceof BlockPSDAPGDoorBase && entity instanceof BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase && IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.UNLOCKED)) {
				final float doorStateValue = Mth.clamp(doorValue * DOOR_MOVE_TIME / BlockPSDAPGDoorBase.MAX_OPEN_VALUE, 0, 1);
				((BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase) entity).setOpen(doorStateValue);
			}
		}
	}

	@Override
	protected float getModelZOffset() {
		return baseTrainType.startsWith("london_underground_199")
				|| trainId.startsWith("london_underground_199")
				|| baseTrainType.equals("mpl_85")
				|| trainId.equals("mpl_85")
				|| baseTrainType.equals("br_423")
				|| trainId.equals("br_423") ?
				reversed ? -0.5F : 0.5F : 0; // TODO integrate this into TrainClientRegistry
	}

	@Override
	protected double asin(double value) {
		return Math.asin(value);
	}

	public void simulateTrain(Level world, float ticksElapsed, SpeedCallback speedCallback, AnnouncementCallback announcementCallback, AnnouncementCallback lightRailAnnouncementCallback) {
		trainTranslucentRenders.clear();
		this.speedCallback = speedCallback;
		this.announcementCallback = announcementCallback;
		this.lightRailAnnouncementCallback = lightRailAnnouncementCallback;
		if (ticksElapsed > 0) {
			oldSpeed = speed;
			oldRailProgress = railProgress;
			oldDoorValue = doorValue;
		}

		if (ticksElapsed != 0) {
			final int stopIndex = path.get(getIndex(0, spacing, false)).stopIndex - 1;
			if (!RailwayData.useRoutesAndStationsFromIndex(stopIndex, routeIds, ClientData.DATA_CACHE, (currentStationIndex, thisRoute1, nextRoute1, thisStation1, nextStation1, lastStation1) -> {
				this.currentStationIndex = currentStationIndex;
				thisRoute = thisRoute1;
				nextRoute = nextRoute1;
				thisStation = thisStation1;
				nextStation = nextStation1;
				lastStation = lastStation1;
			})) {
				currentStationIndex = 0;
				thisRoute = null;
				nextRoute = null;
				thisStation = null;
				nextStation = null;
				lastStation = null;
			}
		}

		simulateTrain(world, ticksElapsed, null);

		if (depot == null || routeIds.isEmpty()) {
			final Siding siding = ClientData.DATA_CACHE.sidingIdMap.get(sidingId);
			depot = siding == null ? null : ClientData.DATA_CACHE.sidingIdToDepot.get(siding.id);
			routeIds = depot == null ? new ArrayList<>() : depot.routeIds;
			if (depot != null) {
				depot.lastDeployedMillis = System.currentTimeMillis();
			}
		}

		irregX.tick(railProgress);
		irregY.tick(railProgress);
		irregR.tick(railProgress);

		final LocalPlayer player = Minecraft.getInstance().player;
		if (isManualAllowed && Train.isHoldingKey(player) && isPlayerRiding(player)) {
			RenderDrivingOverlay.setData(manualNotch, this);
		}

		this.speedCallback = null;
		this.announcementCallback = null;



		// Update sound
		if (path.isEmpty()) {
			trainSound.stopAll();
			return;
		}
		if (!handlePositions(world, keyPointsPositions, ticksElapsed, true)) {
			trainSound.stopAll();
			return;
		}

		final Entity camera = Minecraft.getInstance().getCameraEntity();
		final Vec3 cameraPos = camera == null ? Vec3.ZERO : camera.position();
		Vec3 nearestPoint = keyPointsPositions[0];
		double nearestDistance = Double.POSITIVE_INFINITY;
		int nearestCar = 0;
		for (int i = 0; i < trainCars; i++) {
			Vec3 v = keyPointsPositions[i * 2 + 1].subtract(keyPointsPositions[i * 2]);
			Vec3 w = cameraPos.subtract(keyPointsPositions[i * 2]);

			double c1 = w.dot(v);
			if ( c1 <= 0 ) {
				final double checkDistance = keyPointsPositions[i * 2].distanceToSqr(cameraPos);
				if (checkDistance < nearestDistance) {
					nearestCar = i;
					nearestDistance = checkDistance;
					nearestPoint = keyPointsPositions[i * 2];
				}
				continue;
			}

			double c2 = v.dot(v);
			if ( c2 <= c1 ) {
				final double checkDistance = keyPointsPositions[i * 2 + 1].distanceToSqr(cameraPos);
				if (checkDistance < nearestDistance) {
					nearestCar = i;
					nearestDistance = checkDistance;
					nearestPoint = keyPointsPositions[i * 2 + 1];
				}
				continue;
			}

			double b = c1 / c2;
			Vec3 Pb = keyPointsPositions[i * 2].add(v.scale(b));
			final double checkDistance = Pb.distanceToSqr(cameraPos);
			if (checkDistance < nearestDistance) {
				nearestCar = i;
				nearestDistance = checkDistance;
				nearestPoint = Pb;
			}
		}
		final BlockPos soundPos = RailwayData.newBlockPos(nearestPoint.x, nearestPoint.y, nearestPoint.z);
		if (ticksElapsed > 0) trainSound.playNearestCar(world, soundPos, nearestCar);
	}

	@Override
	protected float getBogiePosition() {
		final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(trainId);
		return trainProperties.bogiePosition;
	}

	@Override
	protected boolean getIsJacobsBogie() {
		final TrainProperties trainProperties = TrainClientRegistry.getTrainProperties(trainId);
		return trainProperties.isJacobsBogie;
	}

	public void renderTranslucent() {
		trainTranslucentRenders.forEach(Runnable::run);
		trainTranslucentRenders.clear();
	}

	public int getCurrentStationIndex() {
		return currentStationIndex;
	}

	public Route getThisRoute() {
		return thisRoute;
	}

	public Route getNextRoute() {
		return nextRoute;
	}

	public Station getThisStation() {
		return thisStation;
	}

	public Station getNextStation() {
		return nextStation;
	}

	public Station getLastStation() {
		return lastStation;
	}

	public void startRidingClient(UUID uuid, float percentageX, float percentageZ) {
		final LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && player.getUUID().equals(uuid)) {
			justMounted = true;
			isSitting = false;
		}
		vehicleRidingClient.startRiding(uuid, percentageX, percentageZ);
	}

	public void updateRiderPercentages(UUID uuid, float percentageX, float percentageZ) {
		vehicleRidingClient.updateRiderPercentages(uuid, percentageX, percentageZ);
	}

	public void copyFromTrain(Train train) {
		path.clear();
		distances.clear();
		ridingEntities.clear();

		path.addAll(train.path);
		distances.addAll(train.distances);
		ridingEntities.addAll(train.ridingEntities);

		speed = train.speed;
		railProgress = train.railProgress;
		doorTarget = train.doorTarget;
		elapsedDwellTicks = train.elapsedDwellTicks;
		nextStoppingIndex = train.nextStoppingIndex;
		nextPlatformIndex = train.nextPlatformIndex;
		reversed = train.reversed;
		isOnRoute = train.isOnRoute;
		isCurrentlyManual = train.isCurrentlyManual;
		manualNotch = train.manualNotch;
	}

	public final float speedChange() {
		return speed - oldSpeed;
	}

	public boolean justOpening() {
		return oldDoorValue == 0 && doorValue > 0;
	}

	public boolean justClosing(float doorCloseTime) {
		return oldDoorValue >= doorCloseTime && doorValue < doorCloseTime;
	}

	public final boolean isDoorOpening() {
		return doorOpening;
	}

	public final List<Long> getRouteIds() {
		return routeIds;
	}

	private int getPreviousStoppingIndex(int headIndex) {
		for (int i = headIndex; i >= 0; i--) {
			if (path.get(i).dwellTime > 0 && path.get(i).rail.railType == RailType.PLATFORM) {
				return i;
			}
		}
		return 0;
	}

	@FunctionalInterface
	public interface SpeedCallback {
		void speedCallback(float speed, int stopIndex, List<Long> routeIds);
	}

	@FunctionalInterface
	public interface AnnouncementCallback {
		void announcementCallback(int stopIndex, List<Long> routeIds);
	}
}
