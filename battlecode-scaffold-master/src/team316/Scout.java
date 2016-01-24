package team316;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class Scout implements Player {

	private static final int BROADCAST_RADIUS = 80 * 80 * 2;
	private static final int BROADCAST_INTERVAL_MIN = 50;

	private final PotentialField field;
	private final MotionController mc;
	private final EnemyLocationModel elm;
	private final RCWrapper rcWrapper;

	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	private int nextFlowerSwitchTurn = 0;
	private int curFlowerStage = 0;
	private int lastBroadcast = -100;
	private int curDirection = 0;
	private Direction[] bordersYetToDiscover = {Direction.NORTH,
			Direction.SOUTH, Direction.EAST, Direction.WEST};

	public enum ScoutState {
		RUNAWAY, ROAM_AROUND, NEED_TO_BROADCAST
	}

	public Scout(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rcWrapper = new RCWrapper(rc);
		RobotPlayer.rcWrapper = rcWrapper;
		this.elm = new EnemyLocationModel();
		minX = Math.max(0, rc.getLocation().x - 80);
		minY = Math.max(0, rc.getLocation().y - 80);
		maxX = Math.min(580, rc.getLocation().x + 80);
		maxY = Math.max(580, rc.getLocation().y + 80);
	}

	public void initOnNewTurn(RobotController rc) throws GameActionException {
		elm.onNewTurn();
		rcWrapper.initOnNewTurn();
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		initOnNewTurn(rc);

		ScoutState state = assessSituation(rc);

		rc.setIndicatorString(0, "Currently in mode: " + state);

		switch (state) {
			case RUNAWAY :
				runaway(rc);
				break;
			case ROAM_AROUND :
				roamAround(rc);
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
		// System.out.println("messageA: " + messageA + ", messageB:" +
		// messageB);
		lastBroadcast = Turn.currentTurn();
		rc.broadcastMessageSignal(messageA, messageB, BROADCAST_RADIUS);
	}

	public void roamAround(RobotController rc) throws GameActionException {
		if (nextFlowerSwitchTurn <= Turn.currentTurn()) {
			nextFlowerSwitchTurn = Turn.currentTurn()
					+ (maxY - minY + maxX - minX + 2) / 4;
			curFlowerStage = (curFlowerStage + 1) & 15;

			int whichCorner = curFlowerStage / 2;

			final double TARGET_CHARGE = 1.0;
			final double DEVIATION_CHARGE = 0.02;
			final int midX = (maxX + minX) / 2;
			final int midY = (maxY + minY) / 2;
			MapLocation targetLocation;
			MapLocation deviationLocation;
			if (whichCorner == 0) {
				targetLocation = new MapLocation(midX, minY);
				deviationLocation = new MapLocation((minX + 3 * midX) / 4,
						(midY + minY) / 2);
			} else if (whichCorner == 1) {
				targetLocation = new MapLocation(maxX, minY);
				deviationLocation = new MapLocation((maxX + 2 * midX) / 3,
						((midY + minY) / 2 + minY) / 2);
			} else if (whichCorner == 2) {
				targetLocation = new MapLocation(maxX, midY);
				deviationLocation = new MapLocation((maxX + midX) / 2,
						(midY * 2 + minY) / 3);
			} else if (whichCorner == 3) {
				targetLocation = new MapLocation(maxX, maxY);
				deviationLocation = new MapLocation(
						((maxX + midX) / 2 + maxX) / 2, (midY * 2 + maxY) / 3);
			} else if (whichCorner == 4) {
				targetLocation = new MapLocation(midX, maxY);
				deviationLocation = new MapLocation((maxX + 3 * midX) / 4,
						(midY + maxY) / 2);
			} else if (whichCorner == 5) {
				targetLocation = new MapLocation(minX, maxY);
				deviationLocation = new MapLocation(
						((minX + midX) / 2 + minX) / 2, (midY * 2 + maxY) / 3);
			} else if (whichCorner == 6) {
				targetLocation = new MapLocation(minX, midY);
				deviationLocation = new MapLocation((minX + midX) / 2,
						(midY * 2 + maxY) / 3);
			} else if (whichCorner == 7) {
				targetLocation = new MapLocation(minX, minY);
				deviationLocation = new MapLocation((minX + 2 * midX) / 3,
						((midY + minY) / 2 + minY) / 2);
			} else {
				throw new RuntimeException(
						"Unknown whichCorner " + whichCorner);
			}

			if (curFlowerStage % 2 != 0) {
				targetLocation = new MapLocation(midX, midY);
			}

			field.addParticle(new ChargedParticle(DEVIATION_CHARGE,
					deviationLocation,
					(nextFlowerSwitchTurn - Turn.currentTurn() + 1) / 2));
			field.addParticle(new ChargedParticle(TARGET_CHARGE, targetLocation,
					nextFlowerSwitchTurn - Turn.currentTurn() + 1));
		}

		mc.tryToMove(rc);
	}

	public void runaway(RobotController rc) throws GameActionException {
		RobotInfo[] robotsWhoCanAttackMe = Battle.robotsWhoCanAttackLocation(
				rc.getLocation(), rcWrapper.enemyTeamRobotsNearby());
		for (RobotInfo r : robotsWhoCanAttackMe) {
			field.addParticle(new ChargedParticle(-1.0, r.location, 1));
		}

		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
		}
	}

	public ScoutState assessSituation(RobotController rc)
			throws GameActionException {
		inspectEnemiesWithinSightRange();
		inspectNeutralRobotswithinSightRange(rc);
		inspectBorders(rcWrapper);
		RobotInfo[] robotsWhoCanAttackMe = Battle.robotsWhoCanAttackLocation(
				rc.getLocation(), rcWrapper.enemyTeamRobotsNearby());
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
				bordersYetToDiscover[i] = Direction.NONE;
			}
		}
	}

}
