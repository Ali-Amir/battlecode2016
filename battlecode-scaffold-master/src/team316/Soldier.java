package team316;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import team316.navigation.ChargedParticle;
import team316.navigation.EnemyLocationModel;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.EncodedMessage;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class Soldier implements Player {

	private static final int MESSAGE_DELAY_TURNS = 5000;
	private static final int BROADCAST_RADIUSSQR = 200;
	private static final int ARMY_MARCH_SIZE = 4;
	private static final int TURNS_BEFORE_AMBUSH = 300;

	private final PotentialField field;
	private final MotionController mc;
	private final EnemyLocationModel elm;
	private final RCWrapper rcWrapper;

	private int lastBroadcastTurn = -100;
	private int lastTimeEnemySeen = -100;
	private int maxParticlesSoFar = 0;
	private int startByteCodes;
	private int maxPartAByteCodes = 0;
	private int maxPartBByteCodes = 0;
	private int maxPartCByteCodes = 0;
	private int maxPartDByteCodes = 0;
	private int maxPartEByteCodes = 0;
	private boolean gatherMode = false;
	private MapLocation gatherLocation = null;

	private boolean isBlitzkriegActivated = false;
	private MapLocation enemyBaseLocation = null;

	public Soldier(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rcWrapper = new RCWrapper(rc);
		RobotPlayer.rcWrapper = rcWrapper;
		this.elm = new EnemyLocationModel();
	}

	/*
	 * Smallest number here must be non-negative.
	 */
	public static int getAttakTypePriority(RobotType t) {
		switch (t) {
			case ARCHON :
				return 100;
			case GUARD :
				return 102;
			case SOLDIER :
				return 104;
			case SCOUT :
				return 101;
			case VIPER :
				return 103;
			case TTM :
				return 105;
			case TURRET :
				return 106;

			case BIGZOMBIE :
				return 5;
			case RANGEDZOMBIE :
				return 4;
			case STANDARDZOMBIE :
				return 2;
			case FASTZOMBIE :
				return 3;
			case ZOMBIEDEN :
				return 1;
			default :
				throw new RuntimeException("UNKNOWN ROBOT TYPE!");
		}
	}

	public boolean processMessage(int message, RobotController rc)
			throws GameActionException {
		boolean success = true;
		MapLocation location = EncodedMessage.getMessageLocation(message);
		switch (EncodedMessage.getMessageType(message)) {
			case EMPTY_MESSAGE :
				return false;
			case ZOMBIE_DEN_LOCATION :
				rc.setIndicatorString(2,
						"Added a den location at turn " + Turn.currentTurn()
								+ " and location (" + location.x + ","
								+ location.y + ")");
				elm.addZombieDenLocation(location);
				break;
			case ENEMY_ARCHON_LOCATION :
				field.addParticle(ParticleType.OPPOSITE_ARCHON, location, 10);
				break;
			case MESSAGE_HELP_ARCHON :
				field.addParticle(ParticleType.ARCHON_ATTACKED, location, 5);
				break;
			case NEUTRAL_ARCHON_LOCATION :
				field.addParticle(new ChargedParticle(50, location, 500));
				break;
			case NEUTRAL_NON_ARCHON_LOCATION :
				// field.addParticle(new ChargedParticle(1, location, 500));
				break;
			case Y_BORDER :
				int minCoordinateY = location.x;
				int maxCoordinateY = location.y;
				rcWrapper.setMaxCoordinate(Direction.NORTH, minCoordinateY);
				rcWrapper.setMaxCoordinate(Direction.SOUTH, maxCoordinateY);
				break;
			case X_BORDER :
				int minCoordinateX = location.x;
				int maxCoordinateX = location.y;
				rcWrapper.setMaxCoordinate(Direction.WEST, minCoordinateX);
				rcWrapper.setMaxCoordinate(Direction.EAST, maxCoordinateX);
				break;

			case GATHER :
				gatherMode = true;
				gatherLocation = location;
				break;

			case ENEMY_BASE_LOCATION :
				enemyBaseLocation = location;
				break;

			case BLITZKRIEG :
				isBlitzkriegActivated = true;
				enemyBaseLocation = location;
				break;

			default :
				success = false;
				break;
		}
		return success;
	}

	public void receiveIncomingSignals(RobotController rc)
			throws GameActionException {
		// Do message signaling stuff.
		if (Turn.currentTurn() - lastBroadcastTurn > MESSAGE_DELAY_TURNS
				&& lastTimeEnemySeen > lastBroadcastTurn) {
			rc.broadcastSignal(BROADCAST_RADIUSSQR);
			lastBroadcastTurn = Turn.currentTurn();
			rc.setIndicatorString(1,
					"Relay message at turn: " + Turn.currentTurn());
		}

		final Signal[] signals = rc.emptySignalQueue();
		for (Signal signal : signals) {
			// If ally. Then ally is reporting enemies.
			if (signal.getMessage() == null
					&& signal.getTeam().equals(rcWrapper.myTeam)
					&& lastBroadcastTurn + MESSAGE_DELAY_TURNS < Turn
							.currentTurn()
					&& rcWrapper.attackableHostileRobots().length == 0) {
				field.addParticle(ParticleType.FIGHTING_ALLY,
						signal.getLocation(), 2);
			} else if (signal.getTeam().equals(rcWrapper.myTeam)
					&& signal.getMessage() != null) {
				boolean processMessage1 = processMessage(signal.getMessage()[0],
						rc);
				boolean processMessage2 = processMessage(signal.getMessage()[1],
						rc);
				// if (!processMessage1 && !processMessage2) {
				// field.addParticle(ParticleType.FIGHTING_ALLY,
				// signal.getLocation(), 5);
				// }
			}
		}
	}

	public void initOnNewTurn(RobotController rc) throws GameActionException {
		// Attract towards closest enemy base location prediction.
		// field.addParticle(elm.predictEnemyBase(rc));
		elm.onNewTurn();
		rcWrapper.initOnNewTurn();
		if (gatherMode) {
			if (gatherLocation.distanceSquaredTo(
					rc.getLocation()) <= rc.getType().attackRadiusSquared) {
				field.addParticle(new ChargedParticle(1000, gatherLocation, 5));
				gatherMode = false;
			} else {
				field.addParticle(new ChargedParticle(1000, gatherLocation, 1));
			}
		}

		// Try to avoid enemy base location until blitzkrieg is activated.
		if (enemyBaseLocation != null) {
			field.addParticle(new ChargedParticle(-1.0, enemyBaseLocation, 1));
		}
	}

	/**
	 * Implements logic for walking when there are no enemy robots around.
	 * 
	 * @param rc
	 * @throws GameActionException
	 */
	public void walkingModeCode(RobotController rc) throws GameActionException {
		if (rc.getHealth() < rc.getType().maxHealth
				&& rcWrapper.archonNearby != null) {
			field.addParticle(new ChargedParticle(100,
					rcWrapper.archonNearby.location, 2));
		}

		// rc.setIndicatorString(1, "Got here " + Turn.currentTurn());
		// there are no enemies nearby
		// check to see if we are in the way of friends
		// we are obstructing them
		if (rc.isCoreReady()) {
			if (rcWrapper.allyRobotsNearby().length > ARMY_MARCH_SIZE
					&& elm.numStrategicLocations() > 0) {
				elm.pushStrategicLocationsToField(field, 1);
				mc.tryToMove(rc, Battle.centerOfMassPlusPoint(
						rcWrapper.allyRobotsNearby(), rc.getLocation()));
				rc.setIndicatorString(1, "Following elm!");
			} else {
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2,
						rcWrapper.myTeam);
				rc.setIndicatorString(1,
						"Currently close allies: " + nearbyFriends.length
								+ ". Neighbors in total: "
								+ rcWrapper.allyRobotsNearby().length);
				// Battle.addAllyParticles(nearbyFriends, field, 2);
				// if (field.particles().size() == 0 || nearbyFriends.length >
				// 2) {
				if (nearbyFriends.length > 2) {
					mc.tryToMoveRandom(rc);
				}
				// } else {
				// mc.tryToMove(rc);
				// }
			}
		} else {
			rc.setIndicatorString(1, "Core not ready!");
		}
	}

	/**
	 * Implements logic for fighting, that is when an enemy robot is visible.
	 * 
	 * @param rc
	 * @throws GameActionException
	 */
	public void fightingModeCode(RobotController rc)
			throws GameActionException {
		// Add two types of particles for each hostile:
		// 1. Repelling that lasts only for one turn (so that we walk away as we
		// shoot).
		// 2. And attracting that lasts for 5 turns (so that when the enemy out
		// of sight we try to go back).
		startByteCodes = Clock.getBytecodeNum();
		Battle.addEnemyParticles(rcWrapper.hostileRobotsNearby(), field, 3);
		boolean somethingIsScary = Battle
				.addScaryParticles(rcWrapper.hostileRobotsNearby(), field, 1);

		lastTimeEnemySeen = Turn.currentTurn();

		maxPartAByteCodes = Math.max(maxPartAByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
		startByteCodes = Clock.getBytecodeNum();

		if (rcWrapper.attackableHostileRobots().length == 0) {
			mc.tryToMove(rc);
			return;
		}

		maxPartBByteCodes = Math.max(maxPartBByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
		startByteCodes = Clock.getBytecodeNum();

		if (somethingIsScary && rc.isCoreReady()) {
			mc.tryToMove(rc);
			return;
		}

		maxPartCByteCodes = Math.max(maxPartCByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO

		if (rc.isWeaponReady()) {
			rc.attackLocation(rcWrapper.attackableHostileRobots()[0].location);
		}

		maxPartDByteCodes = Math.max(maxPartDByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
	}

	/**
	 * Implements logic for walking when there are no enemy robots around and
	 * blitzkrieg is activated.
	 * 
	 * @param rc
	 * @throws GameActionException
	 */
	public void blitzkriegModeCode(RobotController rc)
			throws GameActionException {
		if (!rc.isCoreReady()) {
			return;
		}

		field.addParticle(new ChargedParticle(100.0, enemyBaseLocation, 1));
		if (Turn.currentTurn() < 3000 - TURNS_BEFORE_AMBUSH && rc.getLocation()
				.distanceSquaredTo(enemyBaseLocation) <= 40 * 6) {
			field.addParticle(
					new ChargedParticle(-200.0, enemyBaseLocation, 1));
		}
		
		mc.tryToMove(rc);
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		// Initialize all we can.
		initOnNewTurn(rc);

		// Receive signals and update field based on the contents.
		receiveIncomingSignals(rc);

		String statusString;
		{
			maxParticlesSoFar = Math.max(field.particles().size(),
					maxParticlesSoFar);
			statusString = "Currently have " + field.particles().size()
					+ " particles in store. Max so far: " + maxParticlesSoFar
					+ " maxA(" + maxPartAByteCodes + ") maxB("
					+ maxPartBByteCodes + ") maxC(" + maxPartCByteCodes
					+ ") maxD(" + maxPartDByteCodes + ") maxE("
					+ maxPartEByteCodes + ")";
		}
		startByteCodes = Clock.getBytecodeNum();

		// Decide on mode: Walking vs. Fighting.
		if (rcWrapper.hostileRobotsNearby().length != 0) { // Fighting.
			rc.setIndicatorString(0, "MODE: FIGHTING. " + statusString);

			maxPartEByteCodes = Math.max(maxPartEByteCodes,
					Clock.getBytecodeNum() - startByteCodes); // TODO
			fightingModeCode(rc);
		} else if (isBlitzkriegActivated) { // Blitzkrieg.
			rc.setIndicatorString(0, "MODE: BLITZKRIEG. " + statusString);

			maxPartEByteCodes = Math.max(maxPartEByteCodes,
					Clock.getBytecodeNum() - startByteCodes); // TODO

			blitzkriegModeCode(rc);
		} else { // Walking.
			rc.setIndicatorString(0, "MODE: WALKING. " + statusString);

			maxPartEByteCodes = Math.max(maxPartEByteCodes,
					Clock.getBytecodeNum() - startByteCodes); // TODO

			walkingModeCode(rc);
		}
		// rc.setIndicatorString(2, "" + field.particles());
	}

}
