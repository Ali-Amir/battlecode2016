package team316;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import team316.navigation.ChargedParticle;
import team316.navigation.EnemyLocationModel;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.EncodedMessage;
import team316.utils.EncodedMessage.MessageType;
import team316.utils.Message;
import team316.utils.Probability;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class ArchonNew implements Player {

	public enum WalkReason {
		FREEPLAY, GATHER, ACTIVATE, ATTACK
	}

	public enum ActionIntent {
		I_AM_BORN, OMG_OMG_IM_ATTACKED, USUAL_ROUTINE, WANNA_GO_FOR_A_WALK, DEFENSE
	}

	private final static int MAX_RADIUS = GameConstants.MAP_MAX_HEIGHT
			* GameConstants.MAP_MAX_HEIGHT
			+ GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_WIDTH;
	private final static int HELP_MESSAGE_MAX_DELAY = 30;
	private final static int GATHER_MESSAGE_MAX_DELAY = 15;
	private final static int MESSAGE_BROADCAST_ATTEMPT_FREQUENCY = 10;

	private final RobotController rc;
	private final int birthTurn;
	private final EnemyLocationModel elm;
	private final PotentialField field;
	private final MotionController mc;
	private final RCWrapper rcWrapper;
	private final List<Message> messageQueue;

	// For archonRank;
	// 1 is the leader.
	// 0 is unassigned
	// -1 is dead archon: an archon that can't build anymore;
	private int archonRank = 0;
	private int healthyArchonCount = 0;
	private Map<RobotType, Double> buildDistribution = new HashMap<>();

	private RobotType toBuild = null;
	private WalkReason walkReason = null;
	private MapLocation targetLocation = null;
	private ArrayList<ArrayList<Integer>> toBroadcastNextTurnList = new ArrayList<>();
	private LinkedList<MapLocation> neutralArchonLocations = new LinkedList<>();

	// Messaging timings.
	private int lastHelpAskedTurn = -1000;
	private int lastBroadcastAttemptTurn = -1000;

	public ArchonNew(PotentialField field, MotionController mc,
			RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rc = rc;
		this.rcWrapper = new RCWrapper(rc);
		this.birthTurn = Turn.currentTurn();
		this.elm = new EnemyLocationModel();
		this.messageQueue = new ArrayList<>();
	}

	private boolean attemptBuild(RobotController rc)
			throws GameActionException {
		Direction proposedBuildDirection = null;
		if (rc.isCoreReady()) {
			if (rc.hasBuildRequirements(toBuild)) {
				if (proposedBuildDirection == null) {
					proposedBuildDirection = RobotPlayer.randomDirection();
				}

				Direction buildDirection = buildDirectionClosestTo(rc,
						proposedBuildDirection, toBuild);
				final double acceptProbability = 1.0
						/ (healthyArchonCount - archonRank + 1);
				// will equal to 1.0 if healthyArchonCount = 0 and archonRank =
				// 0 (not assigned).
				if (buildDirection != null
						&& rc.canBuild(buildDirection, toBuild)) {
					boolean isFairResourcesDistribution = Probability
							.acceptWithProbability(acceptProbability)
							|| rc.getTeamParts() > 120;
					if (!isFairResourcesDistribution) {
						return false;
					}
					rc.build(buildDirection, toBuild);
					toBuild = null;
					return true;
				}
			}
		}
		return false;
	}

	private void addToMessageQueue(int message, int radiusSqr) {
		messageQueue.add(new Message(message, radiusSqr));
	}

	/**
	 * Broadcasts all messages that are on the queue.
	 */
	private void trySendingMessages(RobotController rc)
			throws GameActionException {
		// Try sending one message at a time.
		if (messageQueue.isEmpty()) {
			return;
		}

		Message message0 = messageQueue.get(0);
		messageQueue.remove(0);
		Message message1 = new Message(0, 0);
		if (!messageQueue.isEmpty()) {
			message1 = messageQueue.get(0);
			messageQueue.remove(0);
		}
		rc.setIndicatorString(2,
				"Sending messages: "
						+ EncodedMessage.getMessageType(message0.message) + ","
						+ EncodedMessage.getMessageType(message1.message));
		rc.broadcastMessageSignal(message0.message, message1.message,
				Math.max(message0.radiusSqr, message1.radiusSqr));
	}

	/**
	 * Returns the build direction closest to a given direction Returns null if
	 * it can't build anywhere.
	 */
	private Direction buildDirectionClosestTo(RobotController rc,
			Direction forward, RobotType toBuild) {
		final int[] tryDirections = {0, -1, 1, -2, 2};
		for (int deltaD : tryDirections) {
			Direction currentDirection = Direction
					.values()[(forward.ordinal() + deltaD + 8) % 8];
			if (rc.canBuild(currentDirection, toBuild)) {
				return currentDirection;
			}
		}
		return null;
	}

	private void attemptRepairingWeakest(RobotController rc)
			throws GameActionException {
		RobotInfo[] alliesToHelp = rc.senseNearbyRobots(
				RobotType.ARCHON.attackRadiusSquared, rcWrapper.myTeam);
		MapLocation weakestOneLocation = null;
		double weakestWeakness = -(1e9);
		for (RobotInfo ally : alliesToHelp) {
			if (!ally.type.equals(RobotType.ARCHON)
					&& Battle.weakness(ally) > weakestWeakness
					&& ally.health < ally.maxHealth) {
				weakestOneLocation = ally.location;
				weakestWeakness = Battle.weakness(ally);
			}
		}
		if (weakestOneLocation != null) {
			rc.repair(weakestOneLocation);
		}
	}

	private void figureOutRank(RobotController rc) throws GameActionException {
		// Get all incoming archon signals who were initialized before me.
		final Signal[] incomingSignals = rcWrapper.incomingSignals();
		for (Signal s : incomingSignals) {
			if (s.getTeam().equals(rcWrapper.myTeam)
					&& s.getMessage() != null) {
				if (EncodedMessage.getMessageType(s.getMessage()[0])
						.equals(MessageType.MESSAGE_HELLO_ARCHON)) {
					archonRank++;
				}
			}
		}
		archonRank++;

		// Find farthest archon from me and broadcast that I'm initialized.
		MapLocation[] archonLocations = rc
				.getInitialArchonLocations(rcWrapper.myTeam);
		int furthestArchonDistance = 0;
		if (Turn.currentTurn() == 0) {
			for (MapLocation location : archonLocations) {
				int distance = rc.getLocation().distanceSquaredTo(location);
				if (distance > furthestArchonDistance) {
					furthestArchonDistance = distance;
				}
			}
		} else {
			furthestArchonDistance = MAX_RADIUS;
		}
		// TODO furthestArchonDistance doesn't work.
		int message = EncodedMessage.makeMessage(
				MessageType.MESSAGE_HELLO_ARCHON,
				rcWrapper.getCurrentLocation());
		rc.broadcastMessageSignal(message, 0, MAX_RADIUS);

		rc.setIndicatorString(0, "My archon rank is: " + archonRank);
	}

	private void processMessage(int message) throws GameActionException {
		MapLocation location = EncodedMessage.getMessageLocation(message);
		switch (EncodedMessage.getMessageType(message)) {
			case EMPTY_MESSAGE :
				break;

			case ZOMBIE_DEN_LOCATION :
				elm.addZombieDenLocation(location);
				break;

			case MESSAGE_HELP_ARCHON :
				field.addParticle(ParticleType.ARCHON_ATTACKED, location, 5);
				break;

			case NEUTRAL_ARCHON_LOCATION :
				neutralArchonLocations.add(location);
				// field.addParticle(new ChargedParticle(1000, location, 500));
				break;

			case NEUTRAL_NON_ARCHON_LOCATION :
				field.addParticle(new ChargedParticle(30, location, 100));
				break;

			case Y_BORDER :
				int coordinateY = location.y;
				if (coordinateY <= rcWrapper.getCurrentLocation().y) {
					rcWrapper.setMaxCoordinate(Direction.NORTH, coordinateY);
				} else {
					rcWrapper.setMaxCoordinate(Direction.SOUTH, coordinateY);
				}
				break;

			case X_BORDER :
				int coordinateX = location.x;
				if (coordinateX <= rcWrapper.getCurrentLocation().x) {
					rcWrapper.setMaxCoordinate(Direction.WEST, coordinateX);
				} else {
					rcWrapper.setMaxCoordinate(Direction.EAST, coordinateX);
				}

				break;

			default :
				System.out.println(EncodedMessage.getMessageType(message));
				break;
		}
	}

	private void checkInbox(RobotController rc) throws GameActionException {
		Signal[] incomingSignals = rcWrapper.incomingSignals();
		for (Signal s : incomingSignals) {
			if (s.getTeam() == rcWrapper.myTeam && s.getMessage() != null) {
				processMessage(s.getMessage()[0]);
				processMessage(s.getMessage()[1]);
			}
		}
	}

	private int activationProfit(RobotType type) {
		switch (type) {
			case ARCHON :
				return 100;
			case GUARD :
				return 5;
			case SOLDIER :
				return 50;
			case SCOUT :
				return 1;
			case VIPER :
				return 20;
			case TTM :
				return 10;
			case TURRET :
				return 11;
			default :
				throw new RuntimeException("UNKNOWN ROBOT TYPE!");
		}
	}

	private void tryActivateNearbyNeutrals(RobotController rc)
			throws GameActionException {
		if (!rc.isCoreReady())
			return;
		RobotInfo[] neutralRobots = rc.senseNearbyRobots(2, Team.NEUTRAL);
		int bestProfit = 0;
		RobotInfo neutralRobotToActivate = null;
		for (RobotInfo neutralRobot : neutralRobots) {
			if (activationProfit(neutralRobot.type) > bestProfit
					&& rc.getLocation().isAdjacentTo(neutralRobot.location)) {
				neutralRobotToActivate = neutralRobot;
				bestProfit = activationProfit(neutralRobot.type);
			}
		}
		if (neutralRobotToActivate != null) {
			rc.activate(neutralRobotToActivate.location);
		}
	}

	private void figureOutWalkTarget(RobotController rc)
			throws GameActionException {
		if (targetLocation != null) {
			return;
		}

		MapLocation closestNeutralArchonLocation = rcWrapper
				.getClosestLocation(neutralArchonLocations);
		if (closestNeutralArchonLocation != null) {
			targetLocation = closestNeutralArchonLocation;
			walkReason = WalkReason.ACTIVATE;
		}
	}

	// High level logic here.
	private void checkIfTargetWasReached(RobotController rc)
			throws GameActionException {
		if (targetLocation == null) {
			return;
		}

		int distance = rc.getLocation().distanceSquaredTo(targetLocation);
		switch (walkReason) {
			case ACTIVATE :
				if (distance > 2) {
					return;
				}

				RobotInfo targetRobot = rc.senseRobotAtLocation(targetLocation);
				if (targetRobot == null
						|| !targetRobot.team.equals(Team.NEUTRAL)) {
					// Successfully completed the mission.
					targetLocation = null;
				}
				break;
		}
	}

	private void addParticlesToField(RobotController rc)
			throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(),
				RobotType.ARCHON.sensorRadiusSquared);
		Battle.addEnemyParticles(enemyArray, field, 1);
		RobotInfo[] allyArray = rc.senseNearbyRobots(
				RobotType.ARCHON.sensorRadiusSquared, rcWrapper.myTeam);
		Battle.addAllyParticles(allyArray, field, 1);
		MapLocation[] partsLocations = rc
				.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
		for (MapLocation partsLocation : partsLocations) {
			double amount = rc.senseParts(partsLocation);
			field.addParticle(
					new ChargedParticle(amount / 100.0, partsLocation, 1));
		}
	}

	private void initOnNewTurn(RobotController rc) throws GameActionException {
		rcWrapper.initOnNewTurn();
		elm.onNewTurn();

		// set build intentions if they are not set yet.
		if (toBuild == null) {
			toBuild = (new Probability<RobotType>())
					.getRandomSample(buildDistribution);
		}

		checkIfTargetWasReached(rc);
	}

	private ActionIntent assessSitutation() throws GameActionException {
		// If I was just spawned do leader election.
		if (Turn.currentTurn() == birthTurn) {
			return ActionIntent.I_AM_BORN;
		}

		// Check incoming messages to possibly change strategy.
		checkInbox(rc);

		if (rcWrapper.isUnderAttack()) {
			return ActionIntent.OMG_OMG_IM_ATTACKED;
		}

		if (Turn.currentTurn() > 1000) {
			return ActionIntent.DEFENSE;
		}

		if (Turn.currentTurn() > 200) {
			figureOutWalkTarget(rc);
		}

		return ActionIntent.USUAL_ROUTINE;
	}

	private void usualRoutineCode() throws GameActionException {
		tryActivateNearbyNeutrals(rc);
		attemptBuild(rc);
		attemptRepairingWeakest(rc);
		addParticlesToField(rc);
		mc.tryToMove(rc);
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		initOnNewTurn(rc);

		if (Turn.turnsSince(
				lastBroadcastAttemptTurn) >= MESSAGE_BROADCAST_ATTEMPT_FREQUENCY) {
			lastBroadcastAttemptTurn = Turn.currentTurn();
			trySendingMessages(rc);
		}

		ActionIntent mode = assessSitutation();
		switch (mode) {
			case I_AM_BORN :
				rc.setIndicatorString(1, "I AM BORN");
				// Init distribution on birth
				buildDistribution.clear();
				buildDistribution.put(RobotType.SCOUT, 10.0);
				buildDistribution.put(RobotType.SOLDIER, 90.0);

				figureOutRank(rc);
				healthyArchonCount = rc
						.getInitialArchonLocations(rcWrapper.myTeam).length;

				usualRoutineCode();
				break;

			case OMG_OMG_IM_ATTACKED :
				tryActivateNearbyNeutrals(rc); // TODO only if not chased by
												// zombies.
				// Add border particles.
				Battle.addBorderParticles(rcWrapper, field);
				Battle.addEnemyParticles(rcWrapper.hostileRobotsNearby(), field,
						1);

				if (Turn.turnsSince(
						lastHelpAskedTurn) >= HELP_MESSAGE_MAX_DELAY) {
					lastHelpAskedTurn = Turn.currentTurn();
					addToMessageQueue(EncodedMessage.makeMessage(
							MessageType.MESSAGE_HELP_ARCHON, rc.getLocation()),
							1000);
				}

				rc.setIndicatorString(1,
						"OMG_OMG_IM_ATTACKED. Particles: " + field.particles());
				mc.tryToMove(rc);
				break;

			case USUAL_ROUTINE :
				rc.setIndicatorString(1, "USUAL ROUTINE");
				usualRoutineCode();
				break;
		}

		checkIfTargetWasReached(rc);
	}

}
