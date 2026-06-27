package mtr.render;

import cn.zbx1425.mtrsteamloco.game.TrainVirtualDrive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.MTRClient;
import mtr.block.BlockNode;
import mtr.block.BlockPlatform;
import mtr.block.BlockSignalLightBase;
import mtr.block.BlockSignalSemaphoreBase;
import mtr.client.*;
import mtr.data.*;
import mtr.item.ItemNodeModifierBase;
import mtr.mappings.MatrixStackWrapper;
import mtr.mappings.Text;
import mtr.util.Utilities;
import mtr.util.UtilitiesClient;
import mtr.path.PathData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RenderTrains implements IGui {

	public static int maxTrainRenderDistance;
	public static ResourcePackCreatorProperties creatorProperties = new ResourcePackCreatorProperties();

	private static long lastFrameCounter;
	private static float newLastFrameDuration;

	private static int prevPlatformCount;
	private static int prevSidingCount;
	private static UUID renderedUuid;

	public static final int PLAYER_RENDER_OFFSET = 1000;

	public static final Set<String> AVAILABLE_TEXTURES = new HashSet<>();
	public static final Set<String> UNAVAILABLE_TEXTURES = new HashSet<>();

	public static final int DETAIL_RADIUS = 32;
	public static final int DETAIL_RADIUS_SQUARED = DETAIL_RADIUS * DETAIL_RADIUS;
	public static final int LIFT_LIGHT_COLOR = 0xFFFF0000;
	private static final int MAX_RADIUS_REPLAY_MOD = 64 * 16;
	private static final int TICKS_PER_SECOND = 20;
	private static final int DISMOUNT_PROGRESS_BAR_LENGTH = 30;
	private static final int TOTAL_RENDER_STAGES = 2;
	private static final List<List<Map<Identifier, Set<BiConsumer<PoseStack, VertexConsumer>>>>> RENDERS = new ArrayList<>(TOTAL_RENDER_STAGES);
	private static final List<List<Map<Identifier, Set<BiConsumer<PoseStack, VertexConsumer>>>>> CURRENT_RENDERS = new ArrayList<>(TOTAL_RENDER_STAGES);
	private static final Identifier LIFT_TEXTURE = Identifier.parse("mtr:textures/entity/lift_1.png");
	private static final Identifier ARROW_TEXTURE = Identifier.parse("mtr:textures/block/sign/lift_arrow.png");

	private static final List<Runnable> queuedTasks = new ArrayList<>();

	static {
		for (int i = 0; i < TOTAL_RENDER_STAGES; i++) {
			final int renderStageCount = QueuedRenderLayer.values().length;
			final List<Map<Identifier, Set<BiConsumer<PoseStack, VertexConsumer>>>> rendersList = new ArrayList<>(renderStageCount);
			final List<Map<Identifier, Set<BiConsumer<PoseStack, VertexConsumer>>>> currentRendersList = new ArrayList<>(renderStageCount);

			for (int j = 0; j < renderStageCount; j++) {
				rendersList.add(j, new HashMap<>());
				currentRendersList.add(j, new HashMap<>());
			}

			RENDERS.add(i, rendersList);
			CURRENT_RENDERS.add(i, currentRendersList);
		}
	}

	public static void simulate() {
		final Minecraft client = Minecraft.getInstance();
		final LocalPlayer player = client.player;
		final Level level = client.level;
		if (level == null) return;
		final float lastFrameDuration = MTRClient.getLastFrameDuration();
		newLastFrameDuration = lastFrameCounter == MTRClient.getGameTick() ? 0 : lastFrameDuration;
		final boolean useAnnouncements = Config.useTTSAnnouncements() || Config.showAnnouncementMessages();

		queuedTasks.forEach(Runnable::run);
		queuedTasks.clear();

		ClientData.TRAINS.forEach(train -> train.simulateTrain(level, newLastFrameDuration, (speed, stopIndex, routeIds) -> {
			final Route thisRoute = train.getThisRoute();
			final Station thisStation = train.getThisStation();
			final Station nextStation = train.getNextStation();
			final Station lastStation = train.getLastStation();

			if (showShiftProgressBar() && (!train.isCurrentlyManual() || !Train.isHoldingKey(player))) {
				if (train.getDoorValue() == 0 || thisRoute == null || thisStation == null || lastStation == null) {
					if (!(train instanceof TrainVirtualDrive)) { // TODO something more elegant
						player.sendOverlayMessage(Text.translatable("gui.mtr.vehicle_speed", RailwayData.round(speed, 1), RailwayData.round(speed * 3.6F, 1)));
					}
				} else {
					final Component text;
					switch ((int) ((System.currentTimeMillis() / 1000) % 3)) {
						default:
							text = getStationText(thisStation, "this");
							break;
						case 1:
							if (nextStation == null) {
								text = getStationText(thisStation, "this");
							} else {
								text = getStationText(nextStation, "next");
							}
							break;
						case 2:
							text = getStationText(lastStation, "last_" + thisRoute.transportMode.toString().toLowerCase(Locale.ENGLISH));
							break;
					}
					player.sendOverlayMessage(text);
				}
			}
		}, (stopIndex, routeIds) -> {
			final Route thisRoute = train.getThisRoute();
			final Route nextRoute = train.getNextRoute();
			final Station nextStation = train.getNextStation();
			final Station lastStation = train.getLastStation();

			if (useAnnouncements && thisRoute != null && nextStation != null && !thisRoute.disableNextStationAnnouncements) {
				final List<String> messages = new ArrayList<>();

				final boolean isLightRailRoute = thisRoute.isLightRailRoute;
				messages.add(IGui.insertTranslation(isLightRailRoute ? "gui.mtr.next_station_light_rail_announcement_cjk" : "gui.mtr.next_station_announcement_cjk", isLightRailRoute ? "gui.mtr.next_station_light_rail_announcement" : "gui.mtr.next_station_announcement", 1, nextStation.name));

				final String mergedInterchangeRoutes = getInterchangeRouteNames(nextStation, thisRoute, nextRoute);
				if (!mergedInterchangeRoutes.isEmpty()) {
					messages.add(IGui.insertTranslation("gui.mtr.interchange_announcement_cjk", "gui.mtr.interchange_announcement", 1, mergedInterchangeRoutes));
				}

				final List<String> connectingStationList = new ArrayList<>();
				ClientData.DATA_CACHE.stationIdToConnectingStations.get(nextStation).forEach(connectingStation -> {
					final String connectingStationMergedInterchangeRoutes = getInterchangeRouteNames(connectingStation, thisRoute, nextRoute);
					if (!connectingStationMergedInterchangeRoutes.isEmpty()) {
						connectingStationList.add(IGui.insertTranslation("gui.mtr.connecting_station_interchange_announcement_part_cjk", "gui.mtr.connecting_station_interchange_announcement_part", 2, connectingStationMergedInterchangeRoutes, connectingStation.name));
					}
				});
				if (!connectingStationList.isEmpty()) {
					messages.add(IGui.insertTranslation("gui.mtr.connecting_station_part_cjk", "gui.mtr.connecting_station_part", 1, IGui.mergeStationsWithCommas(connectingStationList)));
				}

				final String thisRouteSplit = thisRoute.name.split("\\|\\|")[0];
				final String nextRouteSplit = nextRoute == null ? null : nextRoute.name.split("\\|\\|")[0];
				if (lastStation != null && nextStation.id == lastStation.id && nextRoute != null && !nextRoute.platformIds.isEmpty() && !nextRouteSplit.equals(thisRouteSplit)) {
					final Station nextFinalStation = ClientData.DATA_CACHE.platformIdToStation.get(nextRoute.getLastPlatformId());
					if (nextFinalStation != null) {
						final String modeString = thisRoute.transportMode.toString().toLowerCase(Locale.ENGLISH);
						if (nextRoute.isLightRailRoute) {
							messages.add(IGui.insertTranslation("gui.mtr.next_route_" + modeString + "_light_rail_announcement_cjk", "gui.mtr.next_route_" + modeString + "_light_rail_announcement", nextRoute.lightRailRouteNumber, 1, nextFinalStation.name.split("\\|\\|")[0]));
						} else {
							messages.add(IGui.insertTranslation("gui.mtr.next_route_" + modeString + "_announcement_cjk", "gui.mtr.next_route_" + modeString + "_announcement", 2, nextRouteSplit, nextFinalStation.name.split("\\|\\|")[0]));
						}
					}
				}

				IDrawing.narrateOrAnnounce(IGui.mergeStations(messages, "", " "));
			}
		}, (stopIndex, routeIds) -> {
			final Route thisRoute = train.getThisRoute();
			final Station lastStation = train.getLastStation();

			if (useAnnouncements && thisRoute != null && thisRoute.isLightRailRoute && lastStation != null) {
				IDrawing.narrateOrAnnounce(IGui.insertTranslation("gui.mtr.light_rail_route_announcement_cjk", "gui.mtr.light_rail_route_announcement", thisRoute.lightRailRouteNumber, 1, lastStation.name));
			}
		}));

		ClientData.LIFTS.forEach(lift -> lift.tickClient(level, newLastFrameDuration));
	}

	public static void render(float tickDelta, PoseStack poseStack, MultiBufferSource vertexConsumers) {
		final Minecraft client = Minecraft.getInstance();

		final LocalPlayer player = client.player;
		final Level world = client.level;

		if (player == null || world == null) {
			return;
		}

		final int renderDistanceChunks = UtilitiesClient.getRenderDistance();
		maxTrainRenderDistance = renderDistanceChunks * (Config.trainRenderDistanceRatio() + 1);

		poseStack.pushPose();
		TrainRendererBase.setupStaticInfo(poseStack, vertexConsumers, tickDelta);
		TrainRendererBase.setBatch(false);
		ClientData.TRAINS.forEach(train -> train.renderTrain(world, newLastFrameDuration));
		if (!Config.hideTranslucentParts()) {
			TrainRendererBase.setBatch(true);
			ClientData.TRAINS.forEach(TrainClient::renderTranslucent);
		}

		ClientData.LIFTS.forEach(lift -> lift.render(world, (x, y, z, frontDoorValue, backDoorValue) -> {
			final BlockPos posAverage = TrainRendererBase.applyAverageTransform(x, y, z);
			if (posAverage == null) {
				return;
			}

			poseStack.translate(x, y, z);
			UtilitiesClient.rotateXDegrees(poseStack, 180);
			UtilitiesClient.rotateYDegrees(poseStack, 180 + lift.facing.toYRot());
			final int light = LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, posAverage), world.getBrightness(LightLayer.SKY, posAverage));
			lift.getModel().render(poseStack, vertexConsumers, lift, LIFT_TEXTURE, light, frontDoorValue, backDoorValue, false, 0, 1, false, true, false, false, false);

			for (int i = 0; i < (lift.isDoubleSided ? 2 : 1); i++) {
				UtilitiesClient.rotateYDegrees(poseStack, 180);
				poseStack.pushPose();
				poseStack.translate(0.875F, -1.5, lift.liftDepth / 2F - 0.25 - SMALL_OFFSET);
				renderLiftDisplay(poseStack, (renderType, callback) -> callback.accept(poseStack.last(), vertexConsumers.getBuffer(renderType)), posAverage, ClientData.DATA_CACHE.requestLiftFloorText(lift.getCurrentFloorBlockPos())[0], lift.getLiftDirection(), 0.1875F, 0.3125F);
				poseStack.popPose();
			}

			poseStack.popPose();
		}, newLastFrameDuration));

		final boolean renderColors = isHoldingRailRelated(player);
		final int maxRailDistance = renderDistanceChunks * 16;
		final Map<UUID, RailType> renderedRailMap = new HashMap<>();
		ClientData.RAILS.forEach((startPos, railMap) -> railMap.forEach((endPos, rail) -> {
			if (!RailwayData.isBetween(player.getX(), startPos.getX(), endPos.getX(), maxRailDistance) || !RailwayData.isBetween(player.getZ(), startPos.getZ(), endPos.getZ(), maxRailDistance)) {
				return;
			}

			final UUID railProduct = PathData.getRailProduct(startPos, endPos);
			if (renderedRailMap.containsKey(railProduct)) {
				if (renderedRailMap.get(railProduct) == rail.railType) {
					return;
				}
			} else {
				renderedRailMap.put(railProduct, rail.railType);
			}

			switch (rail.transportMode) {
				case TRAIN:
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, renderColors, 1);
					if (renderColors) {
						renderSignalsStandard(world, poseStack, vertexConsumers, rail, startPos, endPos);
					}
					break;
				case BOAT:
					if (renderColors) {
						renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, true, 0.5F);
						renderSignalsStandard(world, poseStack, vertexConsumers, rail, startPos, endPos);
					}
					break;
				case CABLE_CAR:
					if (rail.railType.hasSavedRail || rail.railType == RailType.CABLE_CAR_STATION) {
						renderRailStandard(world, rail, 0.25F + SMALL_OFFSET, renderColors, 0.25F, "mtr:textures/block/metal.png", 0.25F, 0, 0.75F, 1);
					}
					if (renderColors && !rail.railType.hasSavedRail) {
						renderRailStandard(world, rail, 0.5F + SMALL_OFFSET, true, 1, "mtr:textures/block/one_way_rail_arrow.png", 0, 0.75F, 1, 0.25F);
					}

					if (rail.railType != RailType.NONE) {
						rail.render((x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> {
							final int r = renderColors ? (rail.railType.color >> 16) & 0xFF : 0;
							final int g = renderColors ? (rail.railType.color >> 8) & 0xFF : 0;
							final int b = renderColors ? rail.railType.color & 0xFF : 0;
							IDrawing.drawLine((renderType, callback) -> callback.accept(poseStack.last(), vertexConsumers.getBuffer(renderType)), (float) x1, (float) y1 + 0.5F, (float) z1, (float) x3, (float) y2 + 0.5F, (float) z3, r, g, b);
						}, 0, 0);
					}

					break;
				case AIRPLANE:
					if (renderColors) {
						renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, true, 1);
						renderSignalsStandard(world, poseStack, vertexConsumers, rail, startPos, endPos);
					} else {
						renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, false, 0.25F, "textures/block/iron_block.png", 0.25F, 0, 0.75F, 1);
					}
					break;
			}
		}));

		poseStack.popPose();
		if (lastFrameCounter != MTRClient.getFrame()) { // Don't clear if still in the same frame, there might be other passes
			for (int i = 0; i < TOTAL_RENDER_STAGES; i++) {
				for (int j = 0; j < QueuedRenderLayer.values().length; j++) {
					CURRENT_RENDERS.get(i).get(j).clear();
					CURRENT_RENDERS.get(i).get(j).putAll(RENDERS.get(i).get(j));
					RENDERS.get(i).get(j).clear();
				}
			}
		}

		for (int i = 0; i < TOTAL_RENDER_STAGES; i++) {
			for (int j = 0; j < QueuedRenderLayer.values().length; j++) {
				final QueuedRenderLayer queuedRenderLayer = QueuedRenderLayer.values()[j];
				CURRENT_RENDERS.get(i).get(j).forEach((key, value) -> {
					final RenderType renderType;
					switch (queuedRenderLayer) {
						case LIGHT:
							renderType = MoreRenderLayers.getLight(key, false);
							break;
						case LIGHT_TRANSLUCENT:
							renderType = MoreRenderLayers.getLight(key, true);
							break;
						case INTERIOR:
							renderType = MoreRenderLayers.getInterior(key);
							break;
						default:
							renderType = MoreRenderLayers.getExterior(key);
							break;
					}
					final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderType);
					value.forEach(renderer -> renderer.accept(poseStack, vertexConsumer));
				});
			}
		}

		if (prevPlatformCount != ClientData.PLATFORMS.size() || prevSidingCount != ClientData.SIDINGS.size()) {
			ClientData.DATA_CACHE.sync();
		}
		prevPlatformCount = ClientData.PLATFORMS.size();
		prevSidingCount = ClientData.SIDINGS.size();
		ClientData.DATA_CACHE.clearDataIfNeeded();
		lastFrameCounter = MTRClient.getFrame();
	}

	public static boolean shouldNotRender(BlockPos pos, int maxDistance, Direction facing) {
		final Entity camera = Minecraft.getInstance().getCameraEntity();
		return shouldNotRender(camera == null ? null : camera.position(), pos, maxDistance, facing);
	}

	public static void clearTextureAvailability() {
		AVAILABLE_TEXTURES.clear();
		UNAVAILABLE_TEXTURES.clear();
	}

	public static void renderLiftDisplay(PoseStack matrices, RenderCallback renderCallback, BlockPos pos, String floorNumber, Lift.LiftDirection liftDirection, float maxWidth, float height) {
		if (RenderTrains.shouldNotRender(pos, Math.min(RenderPIDS.MAX_VIEW_DISTANCE, RenderTrains.maxTrainRenderDistance), null)) {
			return;
		}

		final MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
		IDrawing.drawStringWithFont(new MatrixStackWrapper.PoseStack(matrices), Minecraft.getInstance().font, immediate, floorNumber, IGui.HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM, 0, height, maxWidth, -1, 18 / maxWidth, LIFT_LIGHT_COLOR, false, MAX_LIGHT_GLOWING, null, new IDrawing.TextDrawingCallback.World());
		immediate.endBatch();

		if (liftDirection != Lift.LiftDirection.NONE) {
			renderCallback.invoke(MoreRenderLayers.getLight(ARROW_TEXTURE, true), (pose, vertexConsumer) -> {
				IDrawing.drawTexture(pose, vertexConsumer, -maxWidth / 6, 0, maxWidth / 3, maxWidth / 3, 0, liftDirection == Lift.LiftDirection.UP ? 0 : 1, 1, liftDirection == Lift.LiftDirection.UP ? 1 : 0, Direction.UP, LIFT_LIGHT_COLOR, MAX_LIGHT_GLOWING);
			});
		}
	}

	public static boolean isHoldingRailRelated(Player player) {
		return Utilities.isHolding(player,
				item -> item instanceof ItemNodeModifierBase ||
						Block.byItem(item) instanceof BlockSignalLightBase ||
						Block.byItem(item) instanceof BlockNode ||
						Block.byItem(item) instanceof BlockSignalSemaphoreBase ||
						Block.byItem(item) instanceof BlockPlatform
		);
	}

	public static boolean showShiftProgressBar() {
		final Minecraft client = Minecraft.getInstance();
		final LocalPlayer player = client.player;
		final float shiftHoldingTicks = ClientData.getShiftHoldingTicks();

		if (shiftHoldingTicks > 0 && player != null) {
			final int progressFilled = Mth.clamp((int) (shiftHoldingTicks * DISMOUNT_PROGRESS_BAR_LENGTH / RailwayDataCoolDownModule.SHIFT_ACTIVATE_TICKS), 0, DISMOUNT_PROGRESS_BAR_LENGTH);
			final String progressBar = String.format("§6%s§7%s", StringUtils.repeat('|', progressFilled), StringUtils.repeat('|', DISMOUNT_PROGRESS_BAR_LENGTH - progressFilled));
			player.sendOverlayMessage(Text.translatable("gui.mtr.dismount_hold", client.options.keyShift.getTranslatedKeyMessage(), progressBar));
			return false;
		} else {
			return true;
		}
	}

	public static void scheduleRender(Identifier resourceLocation, boolean priority, QueuedRenderLayer queuedRenderLayer, BiConsumer<PoseStack, VertexConsumer> callback) {
		final Map<Identifier, Set<BiConsumer<PoseStack, VertexConsumer>>> map = RENDERS.get(priority ? 1 : 0).get(queuedRenderLayer.ordinal());
		if (!map.containsKey(resourceLocation)) {
			map.put(resourceLocation, new HashSet<>());
		}
		map.get(resourceLocation).add(callback);
	}

	public static void scheduleBeforeNextSimulation(Runnable task) {
		queuedTasks.add(task);
	}

	public static String getInterchangeRouteNames(Station station, Route thisRoute, Route nextRoute) {
		final String thisRouteSplit = thisRoute.name.split("\\|\\|")[0];
		final String nextRouteSplit = nextRoute == null ? null : nextRoute.name.split("\\|\\|")[0];
		final Map<Integer, ClientCache.ColorNameTuple> routesInStation = ClientData.DATA_CACHE.stationIdToRoutes.get(station.id);
		if (routesInStation != null) {
			final List<String> interchangeRoutes = routesInStation.values().stream().filter(interchangeRoute -> {
				final String routeName = interchangeRoute.name.split("\\|\\|")[0];
				return !routeName.equals(thisRouteSplit) && !routeName.equals(nextRouteSplit);
			}).map(interchangeRoute -> interchangeRoute.name).collect(Collectors.toList());
			return IGui.mergeStationsWithCommas(interchangeRoutes);
		} else {
			return "";
		}
	}

	private static double maxDistanceXZ(Vec3 pos1, BlockPos pos2) {
		return Math.max(Math.abs(pos1.x - pos2.getX()), Math.abs(pos1.z - pos2.getZ()));
	}

	private static boolean shouldNotRender(Vec3 cameraPos, BlockPos pos, int maxDistance, Direction facing) {
		final boolean playerFacingAway;
		if (cameraPos == null || facing == null) {
			playerFacingAway = false;
		} else {
			if (facing.getAxis() == Direction.Axis.X) {
				final double playerXOffset = cameraPos.x - pos.getX() - 0.5;
				playerFacingAway = Math.signum(playerXOffset) == facing.getStepX() && Math.abs(playerXOffset) >= 0.5;
			} else {
				final double playerZOffset = cameraPos.z - pos.getZ() - 0.5;
				playerFacingAway = Math.signum(playerZOffset) == facing.getStepZ() && Math.abs(playerZOffset) >= 0.5;
			}
		}
		return cameraPos == null || playerFacingAway || maxDistanceXZ(cameraPos, pos) > (MTRClient.isReplayMod() ? MAX_RADIUS_REPLAY_MOD : maxDistance);
	}

	private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth) {
		renderRailStandard(world, rail, yOffset, renderColors, railWidth, renderColors && rail.railType == RailType.QUARTZ ? "mtr:textures/block/rail_preview.png" : "textures/block/rail.png", -1, -1, -1, -1);
	}

	private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth, String texture, float u1, float v1, float u2, float v2) {
		final int maxRailDistance = UtilitiesClient.getRenderDistance() * 16;

		rail.render((x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> {
			final BlockPos pos2 = RailwayData.newBlockPos(x1, y1, z1);
			if (shouldNotRender(pos2, maxRailDistance, null)) {
				return;
			}
			final int light2 = LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, pos2), world.getBrightness(LightLayer.SKY, pos2));

			if (rail.railType == RailType.NONE) {
				if (rail.transportMode != TransportMode.CABLE_CAR && renderColors) {
					scheduleRender(Identifier.parse("mtr:textures/block/one_way_rail_arrow.png"), false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer) -> {
						IDrawing.drawTexture(matrices.last(), vertexConsumer, (float) x1, (float) y1 + yOffset, (float) z1, (float) x2, (float) y1 + yOffset + SMALL_OFFSET, (float) z2, (float) x3, (float) y2 + yOffset, (float) z3, (float) x4, (float) y2 + yOffset + SMALL_OFFSET, (float) z4, 0, 0.25F, 1, 0.75F, Direction.UP, -1, light2);
						IDrawing.drawTexture(matrices.last(), vertexConsumer, (float) x2, (float) y1 + yOffset + SMALL_OFFSET, (float) z2, (float) x1, (float) y1 + yOffset, (float) z1, (float) x4, (float) y2 + yOffset + SMALL_OFFSET, (float) z4, (float) x3, (float) y2 + yOffset, (float) z3, 0, 0.25F, 1, 0.75F, Direction.UP, -1, light2);
					});
				}
			} else {
				final float textureOffset = (((int) (x1 + z1)) % 4) * 0.25F + (float) Config.trackTextureOffset() / Config.TRACK_OFFSET_COUNT;
				final int color = renderColors || !Config.hideSpecialRailColors() && rail.railType.hasSavedRail ? rail.railType.color : -1;
				scheduleRender(Identifier.parse(texture), false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer) -> {
					IDrawing.drawTexture(matrices.last(), vertexConsumer, (float) x1, (float) y1 + yOffset, (float) z1, (float) x2, (float) y1 + yOffset + SMALL_OFFSET, (float) z2, (float) x3, (float) y2 + yOffset, (float) z3, (float) x4, (float) y2 + yOffset + SMALL_OFFSET, (float) z4, u1 < 0 ? 0 : u1, v1 < 0 ? 0.1875F + textureOffset : v1, u2 < 0 ? 1 : u2, v2 < 0 ? 0.3125F + textureOffset : v2, Direction.UP, color, light2);
					IDrawing.drawTexture(matrices.last(), vertexConsumer, (float) x2, (float) y1 + yOffset + SMALL_OFFSET, (float) z2, (float) x1, (float) y1 + yOffset, (float) z1, (float) x4, (float) y2 + yOffset + SMALL_OFFSET, (float) z4, (float) x3, (float) y2 + yOffset, (float) z3, u1 < 0 ? 0 : u1, v1 < 0 ? 0.1875F + textureOffset : v1, u2 < 0 ? 1 : u2, v2 < 0 ? 0.3125F + textureOffset : v2, Direction.UP, color, light2);
				});
			}
		}, -railWidth, railWidth);
	}

	private static void renderSignalsStandard(Level world, PoseStack matrices, MultiBufferSource vertexConsumers, Rail rail, BlockPos startPos, BlockPos endPos) {
		final int maxRailDistance = UtilitiesClient.getRenderDistance() * 16;
		final List<SignalBlocks.SignalBlock> signalBlocks = ClientData.SIGNAL_BLOCKS.getSignalBlocksAtTrack(PathData.getRailProduct(startPos, endPos));
		final float width = 1F / DyeColor.values().length;

		for (int i = 0; i < signalBlocks.size(); i++) {
			final SignalBlocks.SignalBlock signalBlock = signalBlocks.get(i);
			final boolean shouldGlow = signalBlock.isOccupied() && (((int) Math.floor(MTRClient.getGameTick())) % TICKS_PER_SECOND) < TICKS_PER_SECOND / 2;
			final VertexConsumer vertexConsumer = shouldGlow ? vertexConsumers.getBuffer(MoreRenderLayers.getLight(Identifier.parse("mtr:textures/block/white.png"), false)) : vertexConsumers.getBuffer(MoreRenderLayers.getExterior(Identifier.parse("textures/block/white_wool.png")));
			final float u1 = width * i + 1 - width * signalBlocks.size() / 2;
			final float u2 = u1 + width;

			final int color = ARGB_BLACK | signalBlock.color.getMapColor().col;
			rail.render((x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> {
				final BlockPos pos2 = RailwayData.newBlockPos(x1, y1, z1);
				if (shouldNotRender(pos2, maxRailDistance, null)) {
					return;
				}
				final int light2 = shouldGlow ? MAX_LIGHT_GLOWING : LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, pos2), world.getBrightness(LightLayer.SKY, pos2));

				IDrawing.drawTexture(matrices.last(), vertexConsumer, (float) x1, (float) y1, (float) z1, (float) x2, (float) y1 + SMALL_OFFSET, (float) z2, (float) x3, (float) y2, (float) z3, (float) x4, (float) y2 + SMALL_OFFSET, (float) z4, u1, 0, u2, 1, Direction.UP, color, light2);
				IDrawing.drawTexture(matrices.last(), vertexConsumer, (float) x4, (float) y2 + SMALL_OFFSET, (float) z4, (float) x3, (float) y2, (float) z3, (float) x2, (float) y1 + SMALL_OFFSET, (float) z2, (float) x1, (float) y1, (float) z1, u1, 0, u2, 1, Direction.UP, color, light2);
			}, u1 - 1, u2 - 1);
		}
	}

	private static Component getStationText(Station station, String textKey) {
		if (station != null) {
			return Text.literal(IGui.formatStationName(IGui.insertTranslation("gui.mtr." + textKey + "_station_cjk", "gui.mtr." + textKey + "_station", 1, IGui.textOrUntitled(station.name))));
		} else {
			return Text.literal("");
		}
	}

	public enum QueuedRenderLayer {LIGHT, LIGHT_TRANSLUCENT, INTERIOR, EXTERIOR}

	// 26.1 Generic callback for VertexConsumers & SubmitNodeCollector
	@FunctionalInterface
	public interface RenderCallback {
		void invoke(RenderType renderType, BiConsumer<PoseStack.Pose, VertexConsumer> callback);
	}
}
