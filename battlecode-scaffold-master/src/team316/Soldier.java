package team316;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
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

	private static final int MESSAGE_DELAY_TURNS = 30;
	private static final int BROADCAST_RADIUSSQR = 150;
	private static final int ARMY_MARCH_SIZE = 4;
	private static final int TURNS_BEFORE_AMBUSH = 300;
	private static final int BUDDY_HELP_FREQUENCY_TURNS = 10;
	private static final int ELM_AMNESIA_PERIOD_TURNS = 500;
	private static final int KUCHKUEM_DURATION_TURNS = 50;

	private final PotentialField field;
	private final MotionController mc;
	private final EnemyLocationModel elm;
	private final RCWrapper rcWrapper;
	private final RobotController rc;

	private int lastElmAmnesia = -1000;
	private int lastBroadcastTurn = -100;
	private int lastTimeEnemySeen = -100;
	private int maxParticlesSoFar = 0;
	private int startByteCodes;
	private int maxPartAByteCodes = 0;
	private int maxPartBByteCodes = 0;
	private int maxPartCByteCodes = 0;
	private int maxPartDByteCodes = 0;
	private int maxPartEByteCodes = 0;
	private int maxPartInitByteCodes = 0;
	private int maxPartSigByteCodes = 0;
	private boolean gatherMode = false;
	private MapLocation gatherLocation = null;

	private MapLocation kuchkuemLocation = null;
	private boolean isBlitzkriegActivated = false;
	private MapLocation enemyBaseLocation = null;

	private static int nearestFollowerDistance;
	private boolean archonAttacked;

	private int lastHelpedABuddy = -1000;
	private int lastKuchkuemTurn = -1000;

	public Soldier(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rc = rc;
		this.rcWrapper = new RCWrapper(rc);
		RobotPlayer.rcWrapper = rcWrapper;
		this.elm = new EnemyLocationModel();
	}

	public void processMessage(MapLocation senderLocation, int message,
			RobotController rc) throws GameActionException {
		Direction movementDirection;
		MapLocation location = EncodedMessage.getMessageLocation(message);
		int distanceFromSender = rcWrapper.getCurrentLocation()
				.distanceSquaredTo(senderLocation);

		if (EncodedMessage.isEmptyMessage(message)) {
			return;
		}
		switch (EncodedMessage.getMessageType(message)) {

			case ZOMBIE_DEN_LOCATION :
				rc.setIndicatorString(2,
						"Added a den location at turn " + Turn.currentTurn()
								+ " and location (" + location.x + ","
								+ location.y + ")");
				elm.addZombieDenLocation(location);
				break;

			case ENEMY_ARCHON_LOCATION :
				// field.addParticle(ParticleType.OPPOSITE_ARCHON, location,
				// 10);
				break;

			case MESSAGE_HELP_ARCHON :
				archonAttacked = true;
				field.addParticle(ParticleType.ARCHON_ATTACKED, location, 5);
				break;

			case NEUTRAL_ARCHON_LOCATION :
				field.addParticle(new ChargedParticle(50, location, 500));
				break;

			case NEUTRAL_NON_ARCHON_LOCATION :
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
				if (distanceFromSender < nearestFollowerDistance) {
					gatherMode = true;
					movementDirection = senderLocation.directionTo(location);
					gatherLocation = senderLocation.add(
							movementDirection.dx * 4, movementDirection.dy * 4);
					nearestFollowerDistance = distanceFromSender;
				}
				break;

			case ATTACK :
				if (Turn.turnsSince(
						lastKuchkuemTurn) > KUCHKUEM_DURATION_TURNS) {
					lastKuchkuemTurn = Turn.currentTurn();
					kuchkuemLocation = location;
				}
				// if (distanceFromSender < nearestFollowerDistance) {
				// gatherMode = true;
				// movementDirection = senderLocation.directionTo(location);
				// gatherLocation = senderLocation.add(
				// movementDirection.dx * 4, movementDirection.dy * 4);
				// nearestFollowerDistance = distanceFromSender;
				// }
				break;

			case ACTIVATE :
				if (distanceFromSender < nearestFollowerDistance) {
					gatherMode = true;
					movementDirection = senderLocation.directionTo(location);
					gatherLocation = senderLocation.add(
							movementDirection.dx * 4, movementDirection.dy * 4);
					nearestFollowerDistance = distanceFromSender;
				}
				break;

			case ENEMY_BASE_LOCATION :
				enemyBaseLocation = location;
				break;

			case BLITZKRIEG :
				isBlitzkriegActivated = true;
				enemyBaseLocation = location;
				break;

			default :
				break;
		}
		// return success;
	}

	public void receiveIncomingSignals(RobotController rc)
			throws GameActionException {
		// Do message signaling stuff.
		if (Turn.turnsSince(lastBroadcastTurn) > MESSAGE_DELAY_TURNS
				&& lastTimeEnemySeen + 1 == Turn.currentTurn()) {
			rc.broadcastSignal(BROADCAST_RADIUSSQR);
			lastBroadcastTurn = Turn.currentTurn();
		}

		final Signal[] signals = rc.emptySignalQueue();
		for (Signal signal : signals) {
			// If ally. Then ally is reporting enemies.
			if (signal.getMessage() == null
					&& signal.getTeam().equals(rcWrapper.myTeam)
					&& Turn.turnsSince(
							lastHelpedABuddy) > BUDDY_HELP_FREQUENCY_TURNS
					&& rcWrapper.attackableHostileRobots().length == 0) {
				lastHelpedABuddy = Turn.currentTurn();
				field.addParticle(ParticleType.FIGHTING_ALLY,
						signal.getLocation(), 3);
			} else if (signal.getTeam().equals(rcWrapper.myTeam)
					&& signal.getMessage() != null) {
				processMessage(signal.getLocation(), signal.getMessage()[0],
						rc);
				processMessage(signal.getLocation(), signal.getMessage()[1],
						rc);
			}
		}
	}

	public void initOnNewTurn(RobotController rc) throws GameActionException {
		// Attract towards closest enemy base location prediction.
		// field.addParticle(elm.predictEnemyBase(rc));
		if (Turn.turnsSince(lastElmAmnesia) >= ELM_AMNESIA_PERIOD_TURNS) {
			lastElmAmnesia = Turn.currentTurn();
			elm.zombieDenAmnesia();
		}
		elm.onNewTurn();
		rcWrapper.initOnNewTurn();
		if (gatherMode) {
			if (gatherLocation.distanceSquaredTo(
					rc.getLocation()) <= rc.getType().attackRadiusSquared
					|| archonAttacked) {
				// field.addParticle(new ChargedParticle(1000, gatherLocation,
				// 5));
				// gatherMode = false;
				// field.addParticle(new ChargedParticle(1000, gatherLocation,
				// 1));
			} else {
				field.addParticle(new ChargedParticle(1000, gatherLocation, 1));
			}
		}

		// Try to avoid enemy base location until blitzkrieg is activated.
		if (enemyBaseLocation != null) {
			field.addParticle(new ChargedParticle(-1.0, enemyBaseLocation, 1));
		}

		archonAttacked = false;
		nearestFollowerDistance = (int) 1e9;
	}

	/**
	 * Implements logic for walking when there are no enemy robots around.
	 * 
	 * @param rc
	 * @throws GameActionException
	 */
	public void walkingModeCode(RobotController rc) throws GameActionException {
		startByteCodes = Clock.getBytecodeNum();

		if (rc.getHealth() < rc.getType().maxHealth
				&& rcWrapper.archonNearby != null) {
			field.addParticle(new ChargedParticle(100,
					rcWrapper.archonNearby.location, 2));
		}

		maxPartAByteCodes = Math.max(maxPartAByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
		startByteCodes = Clock.getBytecodeNum();

		// rc.setIndicatorString(1, "Got here " + Turn.currentTurn());
		// there are no enemies nearby
		// check to see if we are in the way of friends
		// we are obstructing them
		if (rc.isCoreReady()) {
			debug_indicator0();
			if (Turn.turnsSince(lastKuchkuemTurn) < KUCHKUEM_DURATION_TURNS) {
				field.addParticle(
						new ChargedParticle(1.0, kuchkuemLocation, 1));
			}

			if (rcWrapper.allyRobotsNearby().length > ARMY_MARCH_SIZE
					&& elm.numStrategicLocations() > 0 && !(Turn.turnsSince(
							lastKuchkuemTurn) < KUCHKUEM_DURATION_TURNS)) {
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
				maxPartBByteCodes = Math.max(maxPartBByteCodes,
						Clock.getBytecodeNum() - startByteCodes); // TODO
				startByteCodes = Clock.getBytecodeNum();
				if (!field.particles().isEmpty()) {
					mc.tryToMove(rc);
				} else if (nearbyFriends.length > 2) {
					mc.tryToMoveRandom(rc);
				} else {
					if (rc.isCoreReady() && rc.senseRubble(rc
							.getLocation()) >= GameConstants.RUBBLE_SLOW_THRESH) {
						rc.clearRubble(Direction.NONE);
					}
					for (int i = 0; i < 8; ++i) {
						MapLocation ahead = rc.getLocation()
								.add(Direction.values()[i]);
						if (rc.isCoreReady() && rc.senseRubble(
								ahead) >= GameConstants.RUBBLE_SLOW_THRESH) {
							rc.clearRubble(Direction.values()[i]);
						}
					}
				}
				maxPartCByteCodes = Math.max(maxPartCByteCodes,
						Clock.getBytecodeNum() - startByteCodes); // TODO
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

		if (rcWrapper.attackableHostileRobots().length == 0) {
			// Try to go towards the enemies.
			Battle.addEnemyParticles(rcWrapper.hostileRobotsNearby(), field, 2);
			mc.tryToMove(rc);
			return;
		}

		boolean somethingIsScary = Battle
				.addScaryParticles(rcWrapper.hostileRobotsNearby(), field, 1);

		lastTimeEnemySeen = Turn.currentTurn();

		maxPartAByteCodes = Math.max(maxPartAByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
		startByteCodes = Clock.getBytecodeNum();

		maxPartBByteCodes = Math.max(maxPartBByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
		startByteCodes = Clock.getBytecodeNum();

		if (somethingIsScary && rc.isCoreReady()) {
			mc.fallBack(rc);
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

	private void debug_indicator0() {
		String status = "Kuchkuem: "
				+ (Turn.turnsSince(lastKuchkuemTurn) < KUCHKUEM_DURATION_TURNS
						? "true"
						: "false")
				+ " Location: " + kuchkuemLocation;
		rc.setIndicatorString(0, status);
	}

	private void debug_indicator1(String str) {
		maxParticlesSoFar = Math.max(field.particles().size(),
				maxParticlesSoFar);
		String status = "Currently have " + field.particles().size()
				+ " particles in store. Max so far: " + maxParticlesSoFar
				+ " maxA(" + maxPartAByteCodes + ") maxB(" + maxPartBByteCodes
				+ ") maxC(" + maxPartCByteCodes + ") maxD(" + maxPartDByteCodes
				+ ") maxE(" + maxPartEByteCodes + ")" + " maxInit("
				+ maxPartInitByteCodes + ") maxSig(" + maxPartSigByteCodes
				+ ")";
		rc.setIndicatorString(1, str + status);
	}

	private void debug_indicator2() {
		rc.setIndicatorString(2, "" + gatherMode + " Location: "
				+ gatherLocation + "field:" + field.particles());
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		startByteCodes = Clock.getBytecodeNum();
		// Initialize all we can.
		initOnNewTurn(rc);
		maxPartInitByteCodes = Math.max(maxPartInitByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO
		startByteCodes = Clock.getBytecodeNum();
		// Receive signals and update field based on the contents.
		receiveIncomingSignals(rc);
		maxPartSigByteCodes = Math.max(maxPartSigByteCodes,
				Clock.getBytecodeNum() - startByteCodes); // TODO

		startByteCodes = Clock.getBytecodeNum();

		// Decide on mode: Walking vs. Fighting.
		if (rcWrapper.hostileRobotsNearby().length != 0) { // Fighting.
			debug_indicator1("MODE: FIGHTING. ");

			maxPartEByteCodes = Math.max(maxPartEByteCodes,
					Clock.getBytecodeNum() - startByteCodes); // TODO
			fightingModeCode(rc);
		} else if (isBlitzkriegActivated) { // Blitzkrieg.
			debug_indicator1("MODE: BLITZKRIEG. ");

			maxPartEByteCodes = Math.max(maxPartEByteCodes,
					Clock.getBytecodeNum() - startByteCodes); // TODO

			blitzkriegModeCode(rc);
		} else { // Walking.
			debug_indicator1("MODE: WALKING. ");

			maxPartEByteCodes = Math.max(maxPartEByteCodes,
					Clock.getBytecodeNum() - startByteCodes); // TODO

			walkingModeCode(rc);
		}

		debug_indicator2();
	}

}
