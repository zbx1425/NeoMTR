package mtr.data;

import mtr.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RailwayDataCoolDownModule extends RailwayDataModuleBase {

	private final Map<Player, Integer> playerRidingCoolDown = new HashMap<>();
	private final Map<Player, Long> playerRidingRoute = new HashMap<>();
	private final Map<Player, Integer> playerShiftCoolDowns = new HashMap<>();

	public final Set<Player> playerInVirtualDrive = new HashSet<>();

	public static final int SHIFT_ACTIVATE_TICKS = 30;

	public RailwayDataCoolDownModule(RailwayData railwayData, Level world, Map<BlockPos, Map<BlockPos, Rail>> rails) {
		super(railwayData, world, rails);
	}

	public void tick() {
		world.players().forEach(player -> {
			final int oldShiftCoolDown = playerShiftCoolDowns.getOrDefault(player, 0);
			final int shiftCoolDown;
			if (player.isShiftKeyDown()) {
				shiftCoolDown = Math.min(SHIFT_ACTIVATE_TICKS, oldShiftCoolDown + 1);
			} else {
				shiftCoolDown = 0;
			}
			if (shiftCoolDown != oldShiftCoolDown) {
				playerShiftCoolDowns.put(player, shiftCoolDown);
			}
		});

		playerInVirtualDrive.forEach(player -> {
			playerRidingCoolDown.put(player, 2);
			Registry.setInTeleportationState(player, true);
		});

		final Set<Player> playersToRemove = new HashSet<>();
		playerRidingCoolDown.forEach((player, coolDown) -> {
			if (coolDown <= 0) {
				updatePlayerRiding(player, 0);
				playersToRemove.add(player);
				player.stopRiding();
			}
			playerRidingCoolDown.put(player, coolDown - 1);
		});
		playersToRemove.forEach(player -> {
			playerRidingCoolDown.remove(player);
			playerRidingRoute.remove(player);
		});
	}

	public void onPlayerJoin(ServerPlayer serverPlayer) {
		playerRidingCoolDown.put(serverPlayer, 2);
		playerShiftCoolDowns.put(serverPlayer, 0);
	}

	public void onPlayerDisconnect(Player player) {
		playerShiftCoolDowns.remove(player);
		playerInVirtualDrive.remove(player);
	}

	public void updatePlayerRiding(Player player, long routeId) {
		final boolean isRiding = routeId != 0;
		player.fallDistance = 0;
		player.setNoGravity(isRiding);
		player.noPhysics = isRiding;
		if (isRiding) {
			player.getAbilities().mayfly = true;
			playerRidingCoolDown.put(player, 2);
			playerRidingRoute.put(player, routeId);
		} else {
			playerInVirtualDrive.remove(player);
			((ServerPlayer) player).gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
		}
		Registry.setInTeleportationState(player, isRiding);
	}

	public boolean canRide(Player player) {
		return !playerRidingCoolDown.containsKey(player);
	}

	public void updatePlayerInVirtualDrive(Player player, boolean isRiding) {
		player.fallDistance = 0;
		player.setNoGravity(isRiding);
		player.noPhysics = isRiding;
		if (isRiding) {
			player.getAbilities().mayfly = true;
			playerInVirtualDrive.add(player);
		} else {
			playerInVirtualDrive.remove(player);
			((ServerPlayer) player).gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
		}
		Registry.setInTeleportationState(player, isRiding);
	}

	public Route getRidingRoute(Player player) {
		if (playerRidingRoute.containsKey(player)) {
			return railwayData.dataCache.routeIdMap.get(playerRidingRoute.get(player));
		} else {
			return null;
		}
	}

	public boolean shouldDismount(Player player) {
		return playerShiftCoolDowns.getOrDefault(player, 0) == SHIFT_ACTIVATE_TICKS;
	}

	public void dismountPlayer(Player player) {
		if (!playerRidingCoolDown.containsKey(player)) {
			return;
		}
		railwayData.sidings.forEach(siding -> siding.unmountPlayer(player));
		railwayData.lifts.forEach(lift -> lift.ridingEntities.remove(player.getUUID()));
		updatePlayerRiding(player, 0);
		playerRidingCoolDown.remove(player);
		playerRidingRoute.remove(player);
	}
}
