package team316;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team316.navigation.ChargedParticle;
import team316.navigation.EnemyLocationModel;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.EncodedMessage;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class Scout implements Player {

	// ======= Helper enum's =======
	public enum ScoutState {
		RUNAWAY, ROAM_AROUND, NEED_TO_BROADCAST
	}

	// ======= Static fields =======
	private static final int BROADCAST_RADIUS = 80 * 80 * 2;
	private static final int BROADCAST_INTERVAL_MIN = 50;
	private static final int ENEMY_BASE_NOTIFICATION_PERIOD_TURNS = 200;
	private static final int WALK_ORDER_RESET_PERIOD_TURNS = 800;
	private static final int BLOCK_SIZE = 10;

	// ======= Main fields =======
	private final PotentialField field;
	private final MotionController mc;
	private final EnemyLocationModel elm;
	private final RCWrapper rcWrapper;
	private final RobotController rc;

	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	private int nextFlowerSwitchTurn = 0;
	private int lastEnemyBaseNotificationTurn = -1000;
	private int curFlowerStage = 0;
	private int lastBroadcast = -100;

	private MapLocation initExploreLocation;
	private int lastWalkOrderInitTurn;
	private int nextExploreLocationExpiryTurn = -1000;
	private MapLocation nextExploreLocation = null;
	private int preWalkOrderNum;
	private int walkOrderNum;
	private MapLocation[] preWalkOrder;

	private Direction[] bordersYetToDiscover = {Direction.NORTH,
			Direction.SOUTH, Direction.EAST, Direction.WEST};

	// ======= Constructors and initialization =======

	public Scout(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rc = rc;
		this.rcWrapper = new RCWrapper(rc);
		RobotPlayer.rcWrapper = rcWrapper;
		this.elm = new EnemyLocationModel();
		minX = Math.max(0, rc.getLocation().x - 80);
		minY = Math.max(0, rc.getLocation().y - 80);
		maxX = Math.min(580, rc.getLocation().x + 80);
		maxY = Math.min(580, rc.getLocation().y + 80);
		preWalkOrder = new MapLocation[1024];
		initializeWalkingOrder();
	}

	private void initializeWalkingOrder() {
		lastWalkOrderInitTurn = Turn.currentTurn();
		preWalkOrderNum = 0;
		initExploreLocation = rc.getLocation();
		for (int x = minX + BLOCK_SIZE - 1; x <= maxX; x += BLOCK_SIZE) {
			for (int y = minY + BLOCK_SIZE - 1; y <= maxY; y += BLOCK_SIZE) {
				preWalkOrder[preWalkOrderNum++] = new MapLocation(x, y);
			}
		}
	}

	// ======= Main methods =======

	private void discardThingsOutside() {
		for (int i = 0; i < preWalkOrderNum; ++i) {
			MapLocation curLoc = preWalkOrder[i];
			if (curLoc.x < minX || curLoc.y < minY || curLoc.x > maxX
					|| curLoc.y > maxY) {
				preWalkOrder[i] = preWalkOrder[preWalkOrderNum - 1];
				--preWalkOrderNum;
			}
		}
	}

	private void figureOutExploreLocation() {
		// If nothing is set or it is expired.
		if (nextExploreLocation == null
				|| nextExploreLocationExpiryTurn <= Turn.currentTurn()) {
			int closestDist = 1000000000;
			int closestLoc = -1;
			for (int i = 0; i < preWalkOrderNum; ++i) {
				int curDist = initExploreLocation
						.distanceSquaredTo(preWalkOrder[i])
						+ rc.getLocation().distanceSquaredTo(preWalkOrder[i])
								/ 2;
				if (curDist < closestDist) {
					closestDist = curDist;
					closestLoc = i;
				}
			}
			nextExploreLocation = preWalkOrder[closestLoc];
			nextExploreLocationExpiryTurn = (Math
					.abs(nextExploreLocation.x - rc.getLocation().x)
					+ Math.abs(nextExploreLocation.y - rc.getLocation().y)) / 2
					+ Turn.currentTurn();
			preWalkOrder[closestLoc] = preWalkOrder[preWalkOrderNum - 1];
			--preWalkOrderNum;
			if (preWalkOrderNum == 0) {
				initializeWalkingOrder();
			}
		}
	}

	public void initOnNewTurn(RobotController rc) throws GameActionException {
		elm.onNewTurn();
		rcWrapper.initOnNewTurn();
		if (Turn.turnsSince(
				lastWalkOrderInitTurn) >= WALK_ORDER_RESET_PERIOD_TURNS) {
			initializeWalkingOrder();
		}
		figureOutExploreLocation();
	}

	private void debug_indicator0(ScoutState state) {
		rc.setIndicatorString(0, "Currently in mode: " + state);
		String statusString = "minX: " + minX + " maxX: " + maxX + " minY: "
				+ minY + " maxY: " + maxY + " nextTurn: " + nextFlowerSwitchTurn
				+ " particles: " + field.particles();
		rc.setIndicatorString(1, statusString);
	}

	private void debug_indicator2() {
		String dens = "dens: ";
		for (MapLocation l : elm.knownZombieDens) {
			dens += l + " ";
		}
		rc.setIndicatorString(2, dens);
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		initOnNewTurn(rc);

		ScoutState state = assessSituation(rc);

		debug_indicator0(state);

		switch (state) {
			case RUNAWAY :
				runaway(rc);
				debug_indicator2();
				break;
			case ROAM_AROUND :
				roamAround(rc);
				debug_indicator2();
				break;
			case NEED_TO_BROADCAST :
				publishData(rc);
				roamAround(rc);
				break;

			default :
				assert (0 == 1) : "One state case was missed!";
		}
	}

	public void publishData(RobotController rc) throws GameActionException {
		int messageA = elm.notificationsPending.poll();
		int messageB = elm.notificationsPending.isEmpty()
				? 0
				: elm.notificationsPending.poll();
		rc.setIndicatorString(2,
				"Sending messages: " + EncodedMessage.getMessageType(messageA)
						+ " loc: " + EncodedMessage.getMessageLocation(messageA)
						+ " " + EncodedMessage.getMessageType(messageB));
		lastBroadcast = Turn.currentTurn();
		rc.broadcastMessageSignal(messageA, messageB,
				rcWrapper.getMaxBroadcastRadius());
	}

	public void roamAround(RobotController rc) throws GameActionException {
		// if (nextFlowerSwitchTurn <= Turn.currentTurn()) {
		// nextFlowerSwitchTurn = Turn.currentTurn()
		// + (maxY - minY + maxX - minX + 2) / 4;
		// curFlowerStage = (curFlowerStage + 1) & 15;
		//
		// int whichCorner = curFlowerStage / 2;
		//
		// final double TARGET_CHARGE = 1.0 / 100.0;
		// final double DEVIATION_CHARGE = 0.02 / 100.0;
		// final int midX = (maxX + minX) / 2;
		// final int midY = (maxY + minY) / 2;
		// MapLocation targetLocation;
		// MapLocation deviationLocation;
		// if (whichCorner == 0) {
		// targetLocation = new MapLocation(midX, minY);
		// deviationLocation = new MapLocation((minX + 3 * midX) / 4,
		// (midY + minY) / 2);
		// } else if (whichCorner == 1) {
		// targetLocation = new MapLocation(maxX, minY);
		// deviationLocation = new MapLocation((maxX + 2 * midX) / 3,
		// ((midY + minY) / 2 + minY) / 2);
		// } else if (whichCorner == 2) {
		// targetLocation = new MapLocation(maxX, midY);
		// deviationLocation = new MapLocation((maxX + midX) / 2,
		// (midY * 2 + minY) / 3);
		// } else if (whichCorner == 3) {
		// targetLocation = new MapLocation(maxX, maxY);
		// deviationLocation = new MapLocation(
		// ((maxX + midX) / 2 + maxX) / 2, (midY * 2 + maxY) / 3);
		// } else if (whichCorner == 4) {
		// targetLocation = new MapLocation(midX, maxY);
		// deviationLocation = new MapLocation((maxX + 3 * midX) / 4,
		// (midY + maxY) / 2);
		// } else if (whichCorner == 5) {
		// targetLocation = new MapLocation(minX, maxY);
		// deviationLocation = new MapLocation(
		// ((minX + midX) / 2 + minX) / 2, (midY * 2 + maxY) / 3);
		// } else if (whichCorner == 6) {
		// targetLocation = new MapLocation(minX, midY);
		// deviationLocation = new MapLocation((minX + midX) / 2,
		// (midY * 2 + maxY) / 3);
		// } else if (whichCorner == 7) {
		// targetLocation = new MapLocation(minX, minY);
		// deviationLocation = new MapLocation((minX + 2 * midX) / 3,
		// ((midY + minY) / 2 + minY) / 2);
		// } else {
		// throw new RuntimeException(
		// "Unknown whichCorner " + whichCorner);
		// }
		//
		// if (curFlowerStage % 2 != 0) {
		// targetLocation = new MapLocation(midX, midY);
		// }
		//
		// field.addParticle(new ChargedParticle(DEVIATION_CHARGE,
		// deviationLocation,
		// (nextFlowerSwitchTurn - Turn.currentTurn() + 1) / 2));
		// field.addParticle(new ChargedParticle(TARGET_CHARGE, targetLocation,
		// nextFlowerSwitchTurn - Turn.currentTurn() + 1));
		// }

		if (nextExploreLocation != null) {
			field.addParticle(new ChargedParticle(0.1, nextExploreLocation, 1));
		}
		mc.tryToMove(rc);
	}

	public void runaway(RobotController rc) throws GameActionException {
		RobotInfo[] robotsWhoCanAttackMe = Battle
				.robotsWhoCanAttackLocationPlusDelta(rc.getLocation(),
						rcWrapper.hostileRobotsNearby(), 10);
		for (RobotInfo r : robotsWhoCanAttackMe) {
			field.addParticle(new ChargedParticle(-1.0, r.location, 2));
		}

		debug_indicator0(ScoutState.RUNAWAY);

		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
		}
	}

	public ScoutState assessSituation(RobotController rc)
			throws GameActionException {
		inspectEnemiesWithinSightRange();
		inspectNeutralRobotswithinSightRange(rc);
		inspectBorders(rcWrapper);
		inspectEnemyBase();
		RobotInfo[] robotsWhoCanAttackMe = Battle.robotsWhoCanAttackLocation(
				rc.getLocation(), rcWrapper.hostileRobotsNearby());
		if (robotsWhoCanAttackMe.length > 0) {
			return ScoutState.RUNAWAY;
		} else {
			if (elm.notificationsPending.size() > 0 && Turn.currentTurn()
					- lastBroadcast >= BROADCAST_INTERVAL_MIN) {
				return ScoutState.NEED_TO_BROADCAST;
			} else {
				return ScoutState.ROAM_AROUND;
			}
		}
	}

	private void inspectEnemiesWithinSightRange() {
		RobotInfo[] robotsISee = rcWrapper.hostileRobotsNearby();
		for (RobotInfo r : robotsISee) {
			if (r.type.equals(RobotType.ZOMBIEDEN)) {
				elm.addZombieDenLocation(r);
			}
			if (r.type.equals(RobotType.ARCHON)) {
				elm.addEnemyArchonLocation(r.location);
			}
		}
	}

	private void inspectNeutralRobotswithinSightRange(RobotController rc) {
		RobotInfo[] neutralIsee = rc.senseNearbyRobots(
				RobotType.SCOUT.sensorRadiusSquared, Team.NEUTRAL);
		for (RobotInfo r : neutralIsee) {
			if (r.type.equals(RobotType.ARCHON)) {
				elm.addNeutralArchon(r.location);
			} else {
				elm.addNeutralNonArchon(r.location);
			}
		}
	}

	private void inspectBorders(RCWrapper rcWrapper)
			throws GameActionException {
		for (int i = 0; i < 4; i++) {
			Direction direction = bordersYetToDiscover[i];
			if (!direction.equals(Direction.NONE)
					&& rcWrapper.getMaxCoordinate(direction) != null) {
				elm.addBorders(direction,
						rcWrapper.getMaxCoordinate(direction));
				if (direction.equals(Direction.NORTH)) {
					minY = rcWrapper.getMaxCoordinate(direction);
				}
				if (direction.equals(Direction.SOUTH)) {
					maxY = rcWrapper.getMaxCoordinate(direction);
				}
				if (direction.equals(Direction.EAST)) {
					maxX = rcWrapper.getMaxCoordinate(direction);
				}
				if (direction.equals(Direction.WEST)) {
					minX = rcWrapper.getMaxCoordinate(direction);
				}
				discardThingsOutside();
				nextFlowerSwitchTurn = Turn.currentTurn();
				bordersYetToDiscover[i] = Direction.NONE;
			}
		}
	}

	private void inspectEnemyBase() throws GameActionException {
		// Theorem: enemy has a base if there is a bunch of turrets set up
		// together or there is an archon with at least one turret in sight.s
		RobotInfo[] enemyRobots = rcWrapper.enemyTeamRobotsNearby();
		MapLocation archonLocation = null;
		int sumXTurrets = 0, sumYTurrets = 0, numTurrets = 0;
		for (RobotInfo r : enemyRobots) {
			switch (r.type) {
				case ARCHON :
					archonLocation = r.location;
					break;
				case TURRET :
					++numTurrets;
					sumXTurrets += r.location.x;
					sumYTurrets += r.location.y;
					break;
				default :
					break;
			}
		}

		boolean enemyHasBase = (archonLocation != null && numTurrets > 0)
				|| (numTurrets > 1);
		if (enemyHasBase) {
			MapLocation enemyBaseLoc = archonLocation != null
					? archonLocation
					: new MapLocation(sumXTurrets / numTurrets,
							sumYTurrets / numTurrets);
			if (Turn.currentTurn()
					- lastEnemyBaseNotificationTurn >= ENEMY_BASE_NOTIFICATION_PERIOD_TURNS) {
				lastEnemyBaseNotificationTurn = Turn.currentTurn();
				elm.addEnemyBaseLocation(enemyBaseLoc);
			}
		}
	}

}
