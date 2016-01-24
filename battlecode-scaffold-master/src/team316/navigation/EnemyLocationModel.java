package team316.navigation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.world.control.ZombieControlProvider;
import team316.RobotPlayer;
import team316.utils.EncodedMessage;
import team316.utils.EncodedMessage.MessageType;
import team316.utils.RCWrapper;

public class EnemyLocationModel {

	private final Set<MapLocation> knownZombieDens;
	private final Set<MapLocation> knownNeutrals;
	private final Set<MapLocation> knownEnemyArchonLocations;
	private final Set<Direction> knownBorders;
	private final RobotController rc;
	private final RCWrapper rcWrapper;
	public Queue<Integer> notificationsPending;

	public EnemyLocationModel() {
		knownZombieDens = new HashSet<>();
		knownNeutrals = new HashSet<>();
		knownEnemyArchonLocations = new HashSet<>();
		notificationsPending = new LinkedList<>();
		knownBorders = new HashSet<>();
		this.rc = RobotPlayer.rc;
		this.rcWrapper = RobotPlayer.rcWrapper;
	}

	public int numStrategicLocations() {
		return knownZombieDens.size();
	}

	public void pushStrategicLocationsToField(PotentialField field,
			int lifetime) {
		for (MapLocation zombieDen : knownZombieDens) {
			field.addParticle(ParticleType.DEN, zombieDen, 1);
		}
	}

	public void addEnemyArchonLocation(MapLocation loc) {
		if (!knownEnemyArchonLocations.contains(loc)) {
			knownEnemyArchonLocations.add(loc);
			notificationsPending.add(EncodedMessage
					.makeMessage(MessageType.ENEMY_ARCHON_LOCATION, loc));
		}
	}

	public void addZombieDenLocation(MapLocation loc) {
		if (!knownZombieDens.contains(loc)) {
			knownZombieDens.add(loc);
			notificationsPending.add(EncodedMessage.zombieDenLocation(loc));
		}
	}

	public void addZombieDenLocation(RobotInfo r) {
		addZombieDenLocation(r.location);
	}

	public void addNeutralArchon(MapLocation loc) {
		if (!knownNeutrals.contains(loc)) {
			knownNeutrals.add(loc);
			notificationsPending.add(EncodedMessage
					.makeMessage(MessageType.NEUTRAL_ARCHON_LOCATION, loc));
		}
	}

	public void addNeutralNonArchon(MapLocation loc) {
		if (!knownNeutrals.contains(loc)) {
			knownNeutrals.add(loc);
			notificationsPending.add(EncodedMessage
					.makeMessage(MessageType.NEUTRAL_NON_ARCHON_LOCATION, loc));
		}
	}

	public void addBorders(Direction direction, RCWrapper rcWrapper)
			throws GameActionException {
		int coordinateMin;
		int coordinateMax;
		boolean isYBorder = (direction.equals(Direction.NORTH)
				|| direction.equals(Direction.SOUTH));
		if (!knownBorders.contains(direction)) {
			knownBorders.add(direction);
			if (isYBorder) {
				coordinateMin = rcWrapper.getMaxCoordinate(Direction.NORTH);
				coordinateMax = rcWrapper.getMaxCoordinate(Direction.SOUTH);
				notificationsPending
						.add(EncodedMessage.makeMessage(MessageType.Y_BORDER,
								new MapLocation(coordinateMin, coordinateMax)));
			} else {
				coordinateMin = rcWrapper.getMaxCoordinate(Direction.WEST);
				coordinateMax = rcWrapper.getMaxCoordinate(Direction.EAST);
				notificationsPending
						.add(EncodedMessage.makeMessage(MessageType.X_BORDER,
								new MapLocation(coordinateMin, coordinateMax)));
			}

		}
	}

	public void onNewTurn() {
		List<MapLocation> locationsToDelete = new ArrayList<>();
		RobotInfo[] densNearby = null;
		for (MapLocation loc : knownZombieDens) {
			if (rc.getLocation().distanceSquaredTo(
					loc) <= rc.getType().sensorRadiusSquared) {
				if (densNearby == null) {
					densNearby = rcWrapper.zombieDensNearby();
				}
				boolean isStillAlive = false;
				for (RobotInfo r : densNearby) {
					if (r.location.equals(loc)) {
						isStillAlive = true;
					}
				}
				if (!isStillAlive) {
					locationsToDelete.add(loc);
				}
			}
		}

		for (MapLocation loc : locationsToDelete) {
			knownZombieDens.remove(loc);
		}
	}
}
