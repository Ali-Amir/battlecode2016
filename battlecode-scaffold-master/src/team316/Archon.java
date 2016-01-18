package team316;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.Encoding;
import team316.utils.Probability;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class Archon implements Player {

	private ArrayList<ArrayList<Integer>> toBroadcastNextTurnList = new ArrayList<>();
	private RobotType lastBuilt = RobotType.ARCHON;
	private Set<Integer> marriedScouts = new HashSet<>();
	private Set<Integer> marriedTurrets = new HashSet<>();
	private Map<RobotType, Double> buildDistribution = new HashMap<>();
	private RobotType toBuild = null;
	private boolean backupTurret = false;
	private RobotInfo chosenTurret = null;
	private int lastHealth = 1000;
	private int healthyArchonCount = 0;
	private final int ARCHON_UNHEALTHY_HP_THRESHOLD = 100;
	private int leaderID;
	private int buildAttempts = 0;
	private int successfulBuilds = 0;
	private boolean isDying = false;
	private final PotentialField field;
	private final MotionController mc;
	private boolean inDanger = false;
	private MapLocation myCurrentLocation = null;
	private Team myTeam = null;
	// maps Locations with parts to the turns they were added at.
	private Set<MapLocation> consideredPartsBeforeFrom = new HashSet<>();
	private Set<MapLocation> partsAdded = new HashSet<>();
	private int helpMessageDelay = 0;
	private final RCWrapper rcWrapper;
	// For archonRanl;
	// 1 is the leader.
	// 0 is unassigned
	// -1 is dead archon: an archon that can't build anymore;
	int archonRank = 0;
	Signal[] IncomingSignals;
	static int[] tryDirections = {0, -1, 1, -2, 2};
	final int MAX_RADIUS = GameConstants.MAP_MAX_HEIGHT
			* GameConstants.MAP_MAX_HEIGHT
			+ GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_WIDTH;
	Set<Integer> archonIDs = new HashSet<>();

	public Archon(PotentialField field, MotionController mc,
			RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rcWrapper = new RCWrapper(rc);
	}

	private boolean attemptBuild(RobotController rc)
			throws GameActionException {
		if (rc.isCoreReady()) {
			if (toBuild == null && lastBuilt.equals(RobotType.TURRET)) {
				RobotInfo[] alliesNearBy = rc.senseNearbyRobots(
						rc.getType().sensorRadiusSquared, myTeam);
				chosenTurret = getLonelyRobot(alliesNearBy, RobotType.TURRET,
						marriedTurrets);
				if (chosenTurret != null) {
					toBuild = RobotType.SCOUT;
					backupTurret = true;
				}
			}
			if (toBuild == null) {
				toBuild = (new Probability<RobotType>())
						.getRandomSample(buildDistribution);
			}
			if (rc.hasBuildRequirements(toBuild)) {
				Direction proposedBuildDirection;
				if (backupTurret && rc.canSenseRobot(chosenTurret.ID)) {
					proposedBuildDirection = myCurrentLocation.directionTo(
							rc.senseRobot(chosenTurret.ID).location);
				} else {
					proposedBuildDirection = RobotPlayer.randomDirection();
				}

				Direction buildDirection = closestToForwardToBuild(rc,
						proposedBuildDirection, toBuild);

				final double acceptProbability = 1.0
						/ (healthyArchonCount - archonRank + 1);

				rc.setIndicatorString(1,
						"Trying to build with prob:" + acceptProbability);
				if (buildDirection != null
						&& rc.canBuild(buildDirection, toBuild)) {
					buildAttempts++;
					boolean isFairResourcesDistribution = Probability
							.acceptWithProbability(acceptProbability)
							|| rc.getTeamParts() > 120;
					if (!isFairResourcesDistribution) {
						return false;
					}
					successfulBuilds++;
					rc.build(buildDirection, toBuild);
					lastBuilt = toBuild;
					toBuild = null;
					if (backupTurret) {
						declareTurretScoutMarriage(rc);
					}
					return true;
				}

			}
		}
		return false;
	}

	/**
	 * Sends a message next turn. Useful with communicating with a unit just
	 * built.
	 * 
	 * @param message1
	 * @param message2
	 * @param radius
	 */
	private void addNextTurnMessage(Integer firstInteger, Integer secondInteger,
			int radius) {
		ArrayList<Integer> message = new ArrayList<>();
		message.add(firstInteger);
		message.add(secondInteger);
		message.add(radius);
		toBroadcastNextTurnList.add(message);
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
			canBeAttacked = enemiesSensed.length > 0;
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
				rc.broadcastMessageSignal(RobotPlayer.MESSAGE_HELP_ARCHON, 0,
						1000);
				rc.setIndicatorString(1, "Seeking Help!");
				helpMessageDelay = 15;
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
		if (Turn.currentTurn() == 0) {
			buildDistribution.clear();
			// buildDistribution.put(RobotType.GUARD, 5.0);
			buildDistribution.put(RobotType.SOLDIER, 100.0);
			// buildDistribution.put(RobotType.TURRET, 5.0);
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

	private void declareTurretScoutMarriage(RobotController rc) {
		RobotInfo[] alliesVeryNear = rc.senseNearbyRobots(4, myTeam);
		RobotInfo chosenScout = getLonelyRobot(alliesVeryNear, RobotType.SCOUT,
				marriedScouts);
		addNextTurnMessage(chosenScout.ID, chosenTurret.ID, 8);
		marriedTurrets.add(chosenTurret.ID);
		marriedScouts.add(chosenScout.ID);
		backupTurret = false;
		chosenTurret = null;
	}

	private void attemptRepairingWeakest(RobotController rc)
			throws GameActionException {
		RobotInfo[] alliesToHelp = rc.senseNearbyRobots(
				RobotType.ARCHON.attackRadiusSquared, myTeam);
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
			if (s.getTeam().equals(myTeam) && s.getMessage() != null) {
				if (s.getMessage()[0] == RobotPlayer.MESSAGE_HELLO_ARCHON) {
					archonRank++;
					if (archonRank == 1) {
						leaderID = s.getID();
					}
				}
			}
		}
		archonRank++;

		// Find farthest archon from me and broadcast that I'm initialized.
		MapLocation[] archonLocations = rc.getInitialArchonLocations(myTeam);
		int furthestArchonDistance = 0;
		for (MapLocation location : archonLocations) {
			int distance = myCurrentLocation.distanceSquaredTo(location);
			if (distance > furthestArchonDistance) {
				furthestArchonDistance = distance;
			}
		}
		rc.broadcastMessageSignal(RobotPlayer.MESSAGE_HELLO_ARCHON, 0,
				furthestArchonDistance);

		rc.setIndicatorString(0, "My archon rank is: " + archonRank);

		if (archonRank == 1) {
			leaderID = rc.getID();
		}
	}

	private void checkInbox(RobotController rc) throws GameActionException {
		for (Signal s : IncomingSignals) {
			if (s.getTeam() == myTeam && s.getMessage() != null) {
				switch (s.getMessage()[0]) {
					case RobotPlayer.MESSAGE_BYE_ARCHON :
						if (archonRank > s.getMessage()[1]) {
							archonRank--;
							if (archonRank == 1) {
								rc.broadcastMessageSignal(
										RobotPlayer.MESSAGE_DECLARE_LEADER, 0,
										MAX_RADIUS);
							}
						}
						healthyArchonCount--;
						break;
					case RobotPlayer.MESSAGE_WELCOME_ACTIVATED_ARCHON :
						if (healthyArchonCount == 0) {
							healthyArchonCount = s.getMessage()[1];
							archonRank = healthyArchonCount;
						} else {
							healthyArchonCount++;
						}
						break;
					case RobotPlayer.MESSAGE_DECLARE_LEADER :
						leaderID = s.getID();
						break;
					default :
						break;
				}
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
			if (neutralRobotToActivate.type.equals(RobotType.ARCHON)) {
				int distanceToRobot = myCurrentLocation
						.distanceSquaredTo(neutralRobotToActivate.location);
				addNextTurnMessage(RobotPlayer.MESSAGE_WELCOME_ACTIVATED_ARCHON,
						healthyArchonCount + 1, distanceToRobot);
			}
		}

	}

	public void initializeArchon(RobotController rc)
			throws GameActionException {
		rcWrapper.initOnNewTurn();
		myCurrentLocation = rc.getLocation();
		myTeam = rc.getTeam();
		inDanger = false;
		rc.setIndicatorString(2,
				"Acceptance Rate: " + successfulBuilds + "/" + buildAttempts);
		healthyArchonCount = rc.getInitialArchonLocations(rc.getTeam()).length;
		IncomingSignals = rc.emptySignalQueue();
		field.removeParticleByID(
				Encoding.encodeLocationID(this.myCurrentLocation));
		if (helpMessageDelay > 0) {
			helpMessageDelay--;
		}

		if (Turn.currentTurn() == 0) {
			figureOutRank(rc);
		}
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		initializeArchon(rc);

		checkInbox(rc);

		seekHelpIfNeeded(rc);

		broadcastLateMessages(rc);

		figureOutDistribution();

		attemptActivateRobots(rc);
		if (!inDanger) {
			rc.setIndicatorString(1, "at least into attempBuild function.");
			attemptBuild(rc);
		}

		attemptRepairingWeakest(rc);

		adjustBattle(rc);
		attempMoving(rc);
	}

	private void attempMoving(RobotController rc) throws GameActionException {
		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
		}
	}

	private void adjustBattle(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(myCurrentLocation,
				RobotType.ARCHON.sensorRadiusSquared);
		Battle.addUniqueEnemyParticles(enemyArray, field, 2);
		RobotInfo[] allyArray = rc.senseNearbyRobots(
				RobotType.ARCHON.sensorRadiusSquared, myTeam);
		Battle.addUniqueAllyParticles(allyArray, field, 1);
		if (!consideredPartsBeforeFrom.contains(myCurrentLocation)) {
			MapLocation[] partsLocations = rc
					.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
			for (MapLocation partsLocation : partsLocations) {
				if (partsAdded.contains(partsLocations)) {
					double amount = rc.senseParts(partsLocation);
					field.addParticle(new ChargedParticle(
							Encoding.encodeLocationID(partsLocation),
							amount / 100.0, partsLocation, 30));
					partsAdded.add(partsLocation);
				}
			}
			consideredPartsBeforeFrom.add(myCurrentLocation);
		}
		if(inDanger){
			//Battle.addUniqueBorderParticles(rcWrapper, field);
		}

	}

	/**
	 * Gets an unmarried turret or scout or null if there is not.
	 * 
	 * @param robots
	 * @param targetType
	 * @param married
	 * @return
	 */
	private static RobotInfo getLonelyRobot(RobotInfo[] robots,
			RobotType targetType, Set<Integer> married) {
		for (RobotInfo robot : robots) {
			if (robot.type == targetType && !married.contains(robot.ID)) {
				return robot;
			}
		}
		return null;
	}

}
