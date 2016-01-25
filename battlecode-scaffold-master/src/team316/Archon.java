package team316;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
import team316.utils.Grid;
import team316.utils.Probability;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class Archon implements Player {

	private final static int BLITZKRIEG_NOTIFICATION_PERIOD_TURNS = 100;
	private final static int DEN_GATHER_PERIOD_TURNS = 200;

	// For archonRank;
	// 1 is the leader.
	// 0 is unassigned
	// -1 is dead archon: an archon that can't build anymore;
	private int archonRank = 0;
	private ArrayList<ArrayList<Integer>> toBroadcastNextTurnList = new ArrayList<>();
	// private RobotType lastBuilt = RobotType.ARCHON;
	private Map<RobotType, Double> buildDistribution = new HashMap<>();
	private RobotType toBuild = null;
	private int healthyArchonCount = 0;
	private final int ARCHON_UNHEALTHY_HP_THRESHOLD = 100;
	private int leaderID;
	private boolean isDying = false;
	private final PotentialField field;
	private final MotionController mc;
	private boolean inDanger = false;
	private MapLocation myCurrentLocation = null;
	// maps Locations with parts to the turns they were added at.
	private Set<MapLocation> consideredPartsBeforeFrom = new HashSet<>();
	private Set<MapLocation> partsAdded = new HashSet<>();
	private int helpMessageDelay = 0;
	private int gatherMessageDelay = 0;
	private final RCWrapper rcWrapper;

	private MapLocation targetLocation = null;
	private int HELP_MESSASGE_MAX_DELAY = 15;
	private int GATHER_MESSASGE_MAX_DELAY = 15;
	private LinkedList<MapLocation> densLocations = new LinkedList<>();
	private LinkedList<MapLocation> neutralArchonLocations = new LinkedList<>();
	private Signal[] IncomingSignals;

	static int[] tryDirections = {0, -1, 1, -2, 2};
	private final static int MAX_RADIUS = GameConstants.MAP_MAX_HEIGHT
			* GameConstants.MAP_MAX_HEIGHT
			+ GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_WIDTH;

	private MapLocation enemyBaseLocation = null;
	private int lastBlitzkriegNotification = -1000;
	private final EnemyLocationModel elm;
	private int lastDenGatherTurn = -1000;

	private boolean isTopArchon = false;
	public enum GameMode {
		FREEPLAY, GATHER, ACTIVATE, ATTACK, DEFENSE
	}
	private GameMode myMode = GameMode.FREEPLAY;
	private boolean reachedTarget;
	private static int emptyMessage = EncodedMessage
			.makeMessage(MessageType.EMPTY_MESSAGE, new MapLocation(0, 0));
	// private boolean defenseMode = false;
	// private boolean gatherMode = false;
	// private boolean activationMode = false;
	// private boolean attackMode = false;
	// private MapLocation gatherLocation = null;
	// private MapLocation attackLocation = null;

	public Archon(PotentialField field, MotionController mc,
			RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rcWrapper = new RCWrapper(rc);
		this.elm = new EnemyLocationModel();
	}

	private boolean attemptBuild(RobotController rc)
			throws GameActionException {
		Direction proposedBuildDirection = null;
		if (rc.isCoreReady()) {
			if (myMode.equals(GameMode.DEFENSE)) {
				toBuild = RobotType.TURRET;
				for (int i = 0; i < 4; i++) {
					proposedBuildDirection = Grid.mainDirections[i]
							.rotateRight();
					if (rc.canBuild(proposedBuildDirection, RobotType.TURRET)) {
						break;
					}
				}
			}
			if (toBuild == null) {
				toBuild = (new Probability<RobotType>())
						.getRandomSample(buildDistribution);
				if (toBuild == null) {
					return false;
				}
			}
			if (rc.hasBuildRequirements(toBuild)) {
				if (proposedBuildDirection == null) {
					proposedBuildDirection = RobotPlayer.randomDirection();
				}
				Direction buildDirection = closestToForwardToBuild(rc,
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
					// lastBuilt = toBuild;
					toBuild = null;
					return true;
				}

			}
		}
		return false;
	}

	/**
	 * Broadcasts all messages that are on the queue.
	 */
	private void broadcastLateMessages(RobotController rc)
			throws GameActionException {
		for (ArrayList<Integer> message : toBroadcastNextTurnList) {
			if (message.get(0) == null) {
				rc.broadcastSignal(message.get(2));
			} else {
				rc.broadcastMessageSignal(message.get(0), message.get(1),
						message.get(2));
			}
		}
		toBroadcastNextTurnList.clear();
	}

	private void seekHelpIfNeeded(RobotController rc)
			throws GameActionException {
		boolean isAttacked = rcWrapper.isUnderAttack();
		boolean canBeAttacked = false;
		RobotInfo[] enemiesSensed = rc.senseHostileRobots(myCurrentLocation,
				RobotType.ARCHON.sensorRadiusSquared);

		if (!isAttacked) {
			for (RobotInfo enemy : enemiesSensed) {
				/*
				 * if (myCurrentLocation.distanceSquaredTo( enemy.location) <=
				 * enemy.type.attackRadiusSquared) { canBeAttacked = true;
				 * break; }
				 */
				if (enemy.type != RobotType.ARCHON
						&& enemy.type != RobotType.SCOUT) {
					canBeAttacked = true;
					break;
				}
			}
		}
		if (isAttacked || canBeAttacked) {
			inDanger = true;
			if (helpMessageDelay == 0 && rc.getRobotCount() > 1) {
				int message = EncodedMessage.makeMessage(
						MessageType.MESSAGE_HELP_ARCHON,
						rcWrapper.getCurrentLocation());
				rc.broadcastMessageSignal(message, emptyMessage, 1000);
				helpMessageDelay = this.HELP_MESSASGE_MAX_DELAY;
			}
		}
		isDying = rc.getHealth() < ARCHON_UNHEALTHY_HP_THRESHOLD;
		// TODO fix this:
		/*
		 * if (isDying && !isAttacked && !canBeAttacked) {
		 * rc.broadcastMessageSignal(RobotPlayer.MESSAGE_BYE_ARCHON, archonRank,
		 * MAX_RADIUS); archonRank = -1; healthyArchonCount--; }
		 */
	}

	public void figureOutDistribution() {
		if (Turn.currentTurn() == 1) {
			buildDistribution.clear();
			// buildDistribution.put(RobotType.GUARD, 5.0);
			buildDistribution.put(RobotType.SCOUT, 10.0);
			buildDistribution.put(RobotType.SOLDIER, 90.0);
		}
	}

	/**
	 * Returns the build direction closest to a given direction Returns null if
	 * it can't build anywhere.
	 */
	private Direction closestToForwardToBuild(RobotController rc,
			Direction forward, RobotType toBuild) {
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
		for (Signal s : IncomingSignals) {
			if (s.getTeam().equals(rcWrapper.myTeam)
					&& s.getMessage() != null) {
				if (EncodedMessage.getMessageType(s.getMessage()[0])
						.equals(MessageType.MESSAGE_HELLO_ARCHON)) {
					archonRank++;
					if (archonRank == 1) {
						leaderID = s.getID();
					}
				}
			}
		}
		archonRank++;

		// Find farthest archon from me and broadcast that I'm initialized.
		MapLocation[] archonLocations = rc
				.getInitialArchonLocations(rcWrapper.myTeam);
		int furthestArchonDistance = 0;
		if (Turn.currentTurn() == 1) {
			for (MapLocation location : archonLocations) {
				int distance = myCurrentLocation.distanceSquaredTo(location);
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

		if (archonRank == 1) {
			leaderID = rc.getID();
		}
	}

	private void startGather(MapLocation location) {
		myMode = GameMode.GATHER;
		targetLocation = location;
	}

	private boolean processMessage(int message) throws GameActionException {
		MapLocation location = EncodedMessage.getMessageLocation(message);
		boolean success = false;
		switch (EncodedMessage.getMessageType(message)) {
			case EMPTY_MESSAGE :
				break;

			case ZOMBIE_DEN_LOCATION :
				elm.addZombieDenLocation(location);
				field.addParticle(ParticleType.DEN, location, 100);
				if (!densLocations.contains(location)) {
					densLocations.add(location);
				}

				// field.addParticle(ParticleType.DEN, location, 100);
				break;

			case MESSAGE_HELP_ARCHON :
				field.addParticle(ParticleType.ARCHON_ATTACKED, location, 5);
				break;

			case NEUTRAL_ARCHON_LOCATION :
				neutralArchonLocations.add(location);
				// field.addParticle(new ChargedParticle(1000, location, 500));
				break;

			case NEUTRAL_NON_ARCHON_LOCATION :
				field.addParticle(new ChargedParticle(9, location, 100));
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

			case DEFENSE_MODE_ON :
				myMode = GameMode.DEFENSE;
				targetLocation = location;
				// isTopArchon = false;
				break;

			case ACTIVATE :
				myMode = GameMode.ACTIVATE;
				targetLocation = location;
				// isTopArchon = false;
				break;

			case ATTACK :
				myMode = GameMode.ATTACK;
				targetLocation = location;
				// isTopArchon = false;
				break;

			case GATHER :
				System.out.println("Gather Message received!");
				if (elm.knownZombieDens.contains(location)) {
					lastDenGatherTurn = Turn.currentTurn();
					elm.knownZombieDens.remove(location);
				}
				startGather(location);
				// isTopArchon = false;
				break;

			case ENEMY_BASE_LOCATION :
				System.out.println("Enemy base location received: " + location);
				enemyBaseLocation = location;
				break;

			default :
				success = false;
				break;
		}
		return success;
	}

	private void checkInbox(RobotController rc) throws GameActionException {
		for (Signal s : IncomingSignals) {
			if (s.getTeam() == rcWrapper.myTeam && s.getMessage() != null) {
				processMessage(s.getMessage()[0]);
				processMessage(s.getMessage()[1]);
				/*
				 * if (!processMessage(s.getMessage()[0]) &&
				 * processMessage(s.getMessage()[1])) { /// switch
				 * (s.getMessage()[0]) { case RobotPlayer.MESSAGE_BYE_ARCHON :
				 * if (archonRank > s.getMessage()[1]) { archonRank--; if
				 * (archonRank == 1) { rc.broadcastMessageSignal(
				 * RobotPlayer.MESSAGE_DECLARE_LEADER, 0, MAX_RADIUS); } }
				 * healthyArchonCount--; break; case
				 * RobotPlayer.MESSAGE_WELCOME_ACTIVATED_ARCHON : if
				 * (healthyArchonCount == 0) { healthyArchonCount =
				 * s.getMessage()[1]; archonRank = healthyArchonCount; } else {
				 * healthyArchonCount++; } break; case
				 * RobotPlayer.MESSAGE_DECLARE_LEADER : leaderID = s.getID();
				 * break; default : break; } /// }
				 */
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

	private void attemptActivateRobots(RobotController rc)
			throws GameActionException {
		if (!rc.isCoreReady())
			return;
		RobotInfo[] neutralRobots = rc.senseNearbyRobots(2, Team.NEUTRAL);
		int bestProfit = 0;
		RobotInfo neutralRobotToActivate = null;
		for (RobotInfo neutralRobot : neutralRobots) {
			if (activationProfit(neutralRobot.type) > bestProfit
					&& myCurrentLocation.isAdjacentTo(neutralRobot.location)) {
				neutralRobotToActivate = neutralRobot;
				bestProfit = activationProfit(neutralRobot.type);
			}
		}
		if (neutralRobotToActivate != null) {
			rc.activate(neutralRobotToActivate.location);
			/*
			 * if (neutralRobotToActivate.type.equals(RobotType.ARCHON)) { int
			 * distanceToRobot = myCurrentLocation
			 * .distanceSquaredTo(neutralRobotToActivate.location);
			 * addNextTurnMessage(RobotPlayer.MESSAGE_WELCOME_ACTIVATED_ARCHON,
			 * healthyArchonCount + 1, distanceToRobot); }
			 */
		}

	}

	private void switchModes(RobotController rc) throws GameActionException {
		MapLocation closestArchonLocation = rcWrapper
				.getClosestLocation(neutralArchonLocations);
		int emptyMessage = EncodedMessage.makeMessage(MessageType.EMPTY_MESSAGE,
				new MapLocation(0, 0));
		if (closestArchonLocation != null) {
			targetLocation = closestArchonLocation;
			myMode = GameMode.ACTIVATE;
			int activateMessage = EncodedMessage
					.makeMessage(MessageType.ACTIVATE, targetLocation);
			// rc.broadcastMessageSignal(activateMessage, emptyMessage,
			// MAX_RADIUS);
			return;
		}
		MapLocation closestDenLocation = rcWrapper
				.getClosestLocation(densLocations);
		if (closestDenLocation != null && Turn.currentTurn() > 600) {
			targetLocation = closestDenLocation;
			myMode = GameMode.ATTACK;
			int attackMessage = EncodedMessage.makeMessage(MessageType.ATTACK,
					targetLocation);
			System.out.println("Den message!");
			// rc.broadcastMessageSignal(attackMessage, emptyMessage,
			// MAX_RADIUS);
			return;
		}
		if (Turn.currentTurn() > 1000) {
			targetLocation = rcWrapper.getCurrentLocation();
			int defenseMessage = EncodedMessage
					.makeMessage(MessageType.DEFENSE_MODE_ON, targetLocation);
			myMode = GameMode.DEFENSE;
			System.out.println("Defense message!");
			// rc.broadcastMessageSignal(defenseMessage, emptyMessage,
			// MAX_RADIUS);
		}
	}
	// High level logic here.
	private void checkMode(RobotController rc) throws GameActionException {
		boolean finishedMission = false;
		reachedTarget = false;
		Integer distance = null;
		if (targetLocation != null) {
			distance = rcWrapper.getCurrentLocation()
					.distanceSquaredTo(targetLocation);
		}
		MessageType messageType = null;
		switch (myMode) {
			case FREEPLAY :
				rc.setIndicatorString(1, "FreePlay");
				reachedTarget = true;
				// finishedMission = Turn.currentTurn() > 200;
				break;

			case GATHER :
				rc.setIndicatorString(1, "Gather");
				if (distance <= 10) {
					reachedTarget = true;
					finishedMission = true;
				}
				messageType = MessageType.GATHER;
				break;

			case ACTIVATE :
				rc.setIndicatorString(1, "Activate");
				if (distance <= 1) {
					reachedTarget = true;
				}
				if (rc.senseNearbyRobots(targetLocation, 0,
						Team.NEUTRAL).length == 0) {
					finishedMission = true;
				}
				messageType = MessageType.ACTIVATE;
				break;

			case ATTACK :
				rc.setIndicatorString(1, "Attack: " + densLocations);
				if (distance <= 35) {
					reachedTarget = true;
					if (rc.senseNearbyRobots(targetLocation, 0,
							Team.ZOMBIE).length == 0) {
						finishedMission = true;
						densLocations.remove(targetLocation);
					}
				}
				messageType = MessageType.ATTACK;
				break;

			case DEFENSE :
				rc.setIndicatorString(1, "Defense");
				reachedTarget = true;
				messageType = MessageType.DEFENSE_MODE_ON;
				break;

			default :
				break;
		}

		if (finishedMission && Turn.currentTurn() > 500) {
			switchModes(rc);
		}
		if (!myMode.equals(GameMode.FREEPLAY) && gatherMessageDelay == 0
				&& !inDanger) {
			int message = EncodedMessage.makeMessage(messageType,
					targetLocation);
			rc.broadcastMessageSignal(message, emptyMessage, 1000);
			gatherMessageDelay = GATHER_MESSASGE_MAX_DELAY;
		}
		if (!reachedTarget && !inDanger) {
			field.addParticle(new ChargedParticle(1000, targetLocation, 1));
			return;
		}
	}

	private void initializeArchon(RobotController rc)
			throws GameActionException {
		rcWrapper.initOnNewTurn();
		myCurrentLocation = rcWrapper.getCurrentLocation();
		inDanger = false;
		IncomingSignals = rc.emptySignalQueue();
		if (helpMessageDelay > 0) {
			helpMessageDelay--;
		}
		// if (gatherMode) { // TODO
		// field.addParticle(new ChargedParticle(1000, gatherLocation, 1));
		// }
		if (gatherMessageDelay > 0) {
			gatherMessageDelay--;
		}

		checkInbox(rc);

		seekHelpIfNeeded(rc);

		checkMode(rc);

		if (Turn.currentTurn() == 1) {
			figureOutRank(rc);
			healthyArchonCount = rc
					.getInitialArchonLocations(rcWrapper.myTeam).length;
		}

		if (Turn.currentTurn() - lastDenGatherTurn >= DEN_GATHER_PERIOD_TURNS
				&& !elm.knownZombieDens.isEmpty()) {
			lastDenGatherTurn = Turn.currentTurn();
			MapLocation closestDen = null;
			for (MapLocation l : elm.knownZombieDens) {
				if (closestDen == null
						|| closestDen.distanceSquaredTo(rc.getLocation()) > l
								.distanceSquaredTo(rc.getLocation())) {
					closestDen = l;
				}
			}
			elm.knownZombieDens.remove(closestDen);
			rc.broadcastMessageSignal(
					EncodedMessage.makeMessage(MessageType.GATHER, closestDen),
					0, MAX_RADIUS);
		}

		// Start producing vipers if 1/3 of the game has passed. Getting ready
		// for ambush.
		if (enemyBaseLocation != null) {
			field.addParticle(new ChargedParticle(-0.1, enemyBaseLocation, 1));

			if (Turn.currentTurn() >= 2000) {
				buildDistribution.clear();
				buildDistribution.put(RobotType.VIPER, 100.0);
				buildDistribution.put(RobotType.SOLDIER, 2.0);
			}

			if (Turn.currentTurn() >= 2500 && Turn.currentTurn()
					- lastBlitzkriegNotification >= BLITZKRIEG_NOTIFICATION_PERIOD_TURNS) {
				lastBlitzkriegNotification = Turn.currentTurn();
				rc.broadcastMessageSignal(EncodedMessage
						.makeMessage(MessageType.BLITZKRIEG, enemyBaseLocation),
						0, MAX_RADIUS);
			}
		}

		rc.setIndicatorString(1, "Zombie den locations known: ");
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		initializeArchon(rc);
		broadcastLateMessages(rc);
		figureOutDistribution();

		attemptActivateRobots(rc);
		if (!inDanger) {
			attemptBuild(rc);
		}
		attemptRepairingWeakest(rc);
		adjustBattle(rc);
		// If not necessary move with a probability.
		if (inDanger || myMode.equals(GameMode.GATHER)
				|| (Probability.acceptWithProbability(.10)
						&& !myMode.equals(GameMode.DEFENSE))) {
			attempMoving(rc);
		}
		if (!inDanger && myMode.equals(GameMode.FREEPLAY)
				&& Turn.currentTurn() > 200) {
			int gatherMessage = EncodedMessage.makeMessage(MessageType.GATHER,
					rcWrapper.getCurrentLocation());
			int emptyMessage = EncodedMessage.makeMessage(
					MessageType.EMPTY_MESSAGE, new MapLocation(0, 0));
			rc.broadcastMessageSignal(gatherMessage, emptyMessage, MAX_RADIUS);
			// System.out.println("Gather Message sent");
			startGather(rcWrapper.getCurrentLocation());
		}
	}

	private void attempMoving(RobotController rc) throws GameActionException {
		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
		}
	}

	private void adjustBattle(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(myCurrentLocation,
				RobotType.ARCHON.sensorRadiusSquared);
		Battle.addEnemyParticles(enemyArray, field, 1);
		RobotInfo[] allyArray = rc.senseNearbyRobots(
				RobotType.ARCHON.sensorRadiusSquared, rcWrapper.myTeam);
		Battle.addAllyParticles(allyArray, field, 1);
		// if (!consideredPartsBeforeFrom.contains(myCurrentLocation)) {
		MapLocation[] partsLocations = rc
				.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
		for (MapLocation partsLocation : partsLocations) {
			// if (!partsAdded.contains(partsLocations)) {
			double amount = rc.senseParts(partsLocation);
			field.addParticle(
					new ChargedParticle(amount / 100.0, partsLocation, 1));
			// partsAdded.add(partsLocation);
			// }
		}
		// consideredPartsBeforeFrom.add(myCurrentLocation);
		// }
		if (inDanger) {
			Battle.addBorderParticles(rcWrapper, field);
		}

	}

}
