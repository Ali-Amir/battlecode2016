package team316;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.glass.ui.Robot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import team316.navigation.EnemyLocationModel;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.Probability;
import team316.utils.Turn;
import team316.utils.Vector;

public class Archon implements Player {

	private ArrayList<ArrayList<Integer>> toBroadcastNextTurnList = new ArrayList<>();
	private RobotType lastBuilt = RobotType.ARCHON;
	private Set<Integer> marriedScouts = new HashSet<>();
	private Set<Integer> marriedTurrets = new HashSet<>();
	private Map<RobotType, Double> buildDistribution = new HashMap<>();
	private RobotType toBuild = null;
	private boolean backupTurret = false;
	private RobotInfo choosenTurret = null;
	private int lastHealth = 1000;
	private int healthyArchonCount = 0;
	private final int ARCHON_UNHEALTHY_HP_THRESHOLD = 100;
	private int leaderID;
	private int buildAttempts = 0;
	private int successfulBuilds = 0;
	private boolean isDying = false;
	private final PotentialField field;
	private final MotionController mc;
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
	
	public Archon(PotentialField field,
			MotionController mc) {
		this.field = field;
		this.mc = mc;
	}
	
	private boolean attemptBuild(RobotController rc)
			throws GameActionException {
		if (rc.isCoreReady()) {
			if (toBuild == null && lastBuilt.equals(RobotType.TURRET)) {
				RobotInfo[] alliesNearBy = rc.senseNearbyRobots(
						rc.getType().sensorRadiusSquared, rc.getTeam());
				choosenTurret = getLonelyRobot(alliesNearBy, RobotType.TURRET,
						marriedTurrets);
				if (choosenTurret != null) {
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
				if (backupTurret && rc.canSenseRobot(choosenTurret.ID)) {
					proposedBuildDirection = rc.getLocation().directionTo(
							rc.senseRobot(choosenTurret.ID).location);
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
	private void seekHelpIfNeeded(RobotController rc, int lastHealth)
			throws GameActionException {
		boolean isAttacked = (int) rc.getHealth() < lastHealth;
		boolean canBeAttacked = false;
		RobotInfo[] enemiesSensed = rc.senseHostileRobots(rc.getLocation(),
				RobotType.ARCHON.sensorRadiusSquared);

		if (!isAttacked) {
			for (RobotInfo enemy : enemiesSensed) {
				if (rc.getLocation().distanceSquaredTo(
						enemy.location) <= enemy.type.attackRadiusSquared) {
					canBeAttacked = true;
					break;
				}
			}
		}
		if (isAttacked || canBeAttacked) {
			rc.broadcastMessageSignal(RobotPlayer.MESSAGE_HELP_ARCHON, 0, 1000);
			rc.setIndicatorString(1, "Seeking Help!");
		}
		isDying = rc.getHealth() < ARCHON_UNHEALTHY_HP_THRESHOLD;
		if (isDying) {
			rc.broadcastMessageSignal(RobotPlayer.MESSAGE_BYE_ARCHON,
					archonRank, MAX_RADIUS);
			archonRank = -1;
			healthyArchonCount--;
		}
	}

	public void figureOutDistribution() {
		if (Turn.currentTurn() == 1) {
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
		RobotInfo[] alliesVeryNear = rc.senseNearbyRobots(4, rc.getTeam());
		RobotInfo choosenScout = getLonelyRobot(alliesVeryNear, RobotType.SCOUT,
				marriedScouts);
		addNextTurnMessage(choosenScout.ID, choosenTurret.ID, 8);
		marriedTurrets.add(choosenTurret.ID);
		marriedScouts.add(choosenScout.ID);
		backupTurret = false;
		choosenTurret = null;
	}
	private void attemptRepairingWeakest(RobotController rc)
			throws GameActionException {
		RobotInfo[] alliesToHelp = rc.senseNearbyRobots(
				RobotType.ARCHON.attackRadiusSquared, rc.getTeam());
		MapLocation weakestOneLocation = null;
		double weakestWeakness = -(1e9);
		for (RobotInfo ally : alliesToHelp) {
			if (!ally.type.equals(RobotType.ARCHON)
					&& Battle.weakness(ally) > weakestWeakness) {
				weakestOneLocation = ally.location;
				weakestWeakness = Battle.weakness(ally);
			}
		}
		if (weakestOneLocation != null) {
			rc.repair(weakestOneLocation);
		}
	}
	private void figureOutRank(RobotController rc) throws GameActionException {
		for (Signal s : IncomingSignals) {
			if (s.getTeam() == rc.getTeam() && s.getMessage() != null) {
				if (s.getMessage()[0] == RobotPlayer.MESSAGE_HELLO_ARCHON) {
					archonRank++;
					if (archonRank == 1) {
						leaderID = s.getID();
					}
				}
			}
		}
		archonRank++;
		rc.broadcastMessageSignal(RobotPlayer.MESSAGE_HELLO_ARCHON, 0,
				MAX_RADIUS);
		rc.setIndicatorString(0, "My archon rank is: " + archonRank);
		if (archonRank == 1) {
			leaderID = rc.getID();
		}
	}

	private void checkInbox(RobotController rc) throws GameActionException {
		for (Signal s : IncomingSignals) {
			if (s.getTeam() == rc.getTeam() && s.getMessage() != null) {
				switch (s.getMessage()[0]) {
					case RobotPlayer.MESSAGE_BYE_ARCHON :
						if (archonRank > s.getMessage()[1]) {
							archonRank--;
							if(archonRank == 1){
								rc.broadcastMessageSignal(RobotPlayer.MESSAGE_DECLARE_LEADER, 0, MAX_RADIUS);
							}
						}
						healthyArchonCount--;
						break;
					case RobotPlayer.MESSAGE_WELCOME_ACTIVATED_ARCHON :
						if(healthyArchonCount == 0){
							healthyArchonCount = s.getMessage()[1];
							archonRank = healthyArchonCount;
						}else{
							healthyArchonCount++;
						}
						break;
					case RobotPlayer.MESSAGE_DECLARE_LEADER:
						leaderID = s.getID();
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
		RobotInfo[] neutralRobots = rc.senseNearbyRobots(
				RobotType.ARCHON.attackRadiusSquared, Team.NEUTRAL);
		int bestProfit = 0;
		RobotInfo neutralRobotToActivate = null;
		for (RobotInfo neutralRobot : neutralRobots) {
			if (activationProfit(neutralRobot.type) > bestProfit) {
				neutralRobotToActivate = neutralRobot;
				bestProfit = activationProfit(neutralRobot.type);
			}
		}
		if (neutralRobotToActivate != null) {
			rc.activate(neutralRobotToActivate.location);
		}
		if (neutralRobotToActivate.type.equals(RobotType.ARCHON)) {
			int distanceToRobot = rc.getLocation()
					.distanceSquaredTo(neutralRobotToActivate.location);
			addNextTurnMessage(RobotPlayer.MESSAGE_WELCOME_ACTIVATED_ARCHON,
					healthyArchonCount + 1, distanceToRobot);
		}

	}
	@Override
	public void play(RobotController rc) throws GameActionException {
		rc.setIndicatorString(2,
				"Acceptance Rate: " + successfulBuilds + "/" + buildAttempts);
		healthyArchonCount = rc.getInitialArchonLocations(rc.getTeam()).length;

		IncomingSignals = rc.emptySignalQueue();

		if (Turn.currentTurn() == 1) {
			figureOutRank(rc);
		}

		checkInbox(rc);

		seekHelpIfNeeded(rc, lastHealth);
		lastHealth = (int) rc.getHealth();

		broadcastLateMessages(rc);

		figureOutDistribution();

		attemptActivateRobots(rc);

		attemptBuild(rc);

		attemptRepairingWeakest(rc);
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