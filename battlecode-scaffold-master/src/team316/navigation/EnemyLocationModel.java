package team316.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import team316.RobotPlayer;
import team316.utils.EncodedMessage;
import team316.utils.EncodedMessage.MessageType;
import team316.utils.Grid;
import team316.utils.RCWrapper;

public class EnemyLocationModel {

	public final Set<MapLocation> knownZombieDens;
	private final Set<MapLocation> knownNeutrals;
	private final Set<MapLocation> knownEnemyArchonLocations;
	private final Set<Direction> knownBorders;
	private final RobotController rc;
	private final RCWrapper rcWrapper;
	private final Map<Direction, Integer> maxCoordinateSofar;
	private Map<Direction, Integer> maxSoFarCoordinate = new HashMap<>();
	
	public Queue<Integer> notificationsPending;
	
	public EnemyLocationModel() {
		knownZombieDens = new HashSet<>();
		knownNeutrals = new HashSet<>();
		knownEnemyArchonLocations = new HashSet<>();
		notificationsPending = new LinkedList<>();
		knownBorders = new HashSet<>();
		this.rc = RobotPlayer.rc;
		this.rcWrapper = RobotPlayer.rcWrapper;
		maxCoordinateSofar = new HashMap<>();
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

	public void addEnemyBaseLocation(MapLocation loc) {
		notificationsPending.add(EncodedMessage
				.makeMessage(MessageType.ENEMY_BASE_LOCATION, loc));
	}

	public void addBorders(Direction direction, int value)
			throws GameActionException {
		if (!knownBorders.contains(direction)) {
			knownBorders.add(direction);
			if (Grid.isVertical(direction)) {
				notificationsPending.add(EncodedMessage.makeMessage(
						MessageType.Y_BORDER, new MapLocation(0, value)));
			} else {
				notificationsPending.add(EncodedMessage.makeMessage(
						MessageType.X_BORDER, new MapLocation(value, 0)));
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
