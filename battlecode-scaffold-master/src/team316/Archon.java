package team316;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Archon implements Player {
	
	boolean broadcastNextTurn = false;
	int[] toBroadcastNextTurn = new int[3];
	RobotType[] buildList = new RobotType[] { RobotType.GUARD, RobotType.TURRET };
	RobotType lastBuilt = RobotType.ARCHON;
	Set<Integer> marriedScouts = new HashSet<>();
	Set<Integer> marriedTurrets = new HashSet<>();
	Map<RobotType,Integer> buildDistribution = new HashMap<>();
	RobotType buildNext = null;
	RobotType toBuild = null;
	int guardCounter = 0;
	int turretCounter = 0;
	boolean backupTurret = false;
	RobotInfo choosenTurret = null;
	static int lastHealth = 1000;
	static int[] tryDirections = { 0, -1, 1, -2, 2 };
	private static final int BROADCAST_RADIUSSQR = 200;
	/**
	 * Returns a random unit to build according to buildDistribution.  
	 */
	private RobotType getRandomUntiToBuild(){
		RobotType[] myTypes = RobotType.values();
		int n = myTypes.length;
		double sum = 0;
		for(int i = 0; i < n; i++){
			sum += buildDistribution.getOrDefault(myTypes[i], 0);
		}
		double acc = 0;
		double x = (RobotPlayer.rnd.nextDouble() * sum);
		RobotType lastRobotType = null;
		for(int i = 0; i < n; i++){
			if(!buildDistribution.containsKey(myTypes[i]))
				continue;
			lastRobotType = myTypes[i];
			acc += buildDistribution.get(myTypes[i]);

			if(x < acc){
				return myTypes[i];
			}
		}
		return lastRobotType;
	}
	@Override
	public void play(RobotController rc) throws GameActionException {
		if((int)rc.getHealth() < lastHealth){
			rc.broadcastMessageSignal(RobotPlayer.MESSAGE_HELP_ARCHON, 0, 1000);
			rc.setIndicatorString(1, "Seeking Help!");
		}
		lastHealth = (int) rc.getHealth();
		if (broadcastNextTurn) {
			System.out.println("BroadCasting.." + toBroadcastNextTurn[0] + "," + toBroadcastNextTurn[1]);
			rc.broadcastMessageSignal(toBroadcastNextTurn[0], toBroadcastNextTurn[1], toBroadcastNextTurn[2]);
			broadcastNextTurn = false;
		}
		if(buildDistribution.isEmpty()){
			buildDistribution.put(RobotType.GUARD, 50);
			buildDistribution.put(RobotType.SOLDIER, 45);
			buildDistribution.put(RobotType.TURRET, 5);
		}
		rc.setIndicatorString(2, "Guard Count:" + guardCounter + ", Turret Count:" + turretCounter);
		if (rc.isCoreReady()) {
			Direction randomDir = RobotPlayer.randomDirection();
			if(toBuild == null){
				if (lastBuilt.equals(RobotType.TURRET)) {
					// We can improve on this
					// for instance we can combine the two sense statements we have.
					RobotInfo[] alliesNearBy = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
					// turretsNearBy = filterRobotsbyType(alliesNearBy, targetType)
					choosenTurret = getLonelyRobot(alliesNearBy, RobotType.TURRET, marriedTurrets);
					if (choosenTurret != null) {
						toBuild = RobotType.SCOUT;
						backupTurret = true;
					} else {
						toBuild = getRandomUntiToBuild();
					}
				} else {
					toBuild = getRandomUntiToBuild();
				}				
			}
			rc.setIndicatorString(0, "We have " + rc.getTeamParts() + "parts");
			rc.setIndicatorString(1, "We need " + RobotPlayer.getPrice(toBuild) + "parts");
			if (rc.getTeamParts() >= RobotPlayer.getPrice(toBuild)) {
				/*for (int deltaD : tryDirections) {
					Direction maybeForward = Direction.values()[(forward.ordinal() + deltaD + 8) % 8];
					if (rc.canMove(maybeForward)) {
						rc.move(maybeForward);
						return;
					}
				}*/
				if (backupTurret && rc.canSenseRobot(choosenTurret.ID)) {
					Direction forward = rc.getLocation().directionTo(rc.senseRobot(choosenTurret.ID).location);
					for (int deltaD : tryDirections) {
						Direction maybeForward = Direction.values()[(forward.ordinal() + deltaD + 8) % 8];
						if (rc.canBuild(maybeForward, toBuild)) {
							randomDir = maybeForward;
							break;
						}
					}
				}
				if (rc.canBuild(randomDir, toBuild)) {
					rc.build(randomDir, toBuild);
					lastBuilt = toBuild;
					if(lastBuilt.equals(RobotType.TURRET)){
						turretCounter ++;
					}
					if(lastBuilt.equals(RobotType.GUARD)){
						guardCounter ++;
					}
					toBuild = null;
					if(backupTurret){
						RobotInfo[] alliesVeryNear = rc.senseNearbyRobots(4, rc.getTeam());
						RobotInfo choosenScout = getLonelyRobot(alliesVeryNear, RobotType.SCOUT, marriedScouts);
						toBroadcastNextTurn[0] = choosenScout.ID;
						toBroadcastNextTurn[1] = choosenTurret.ID;
						toBroadcastNextTurn[2] = 8;
						broadcastNextTurn = true;
						marriedTurrets.add(choosenTurret.ID);
						marriedScouts.add(choosenScout.ID);
						backupTurret = false;
						choosenTurret = null;
					}

					return;
				}

			}

			RobotInfo[] alliesToHelp = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, rc.getTeam());
			MapLocation weakestOne = RobotPlayer.findWeakest(alliesToHelp);
			if (weakestOne != null) {
				rc.repair(weakestOne);
				return;
			}
		}
	}
	
	// Gets an unmarried turret or scout
	private static RobotInfo getLonelyRobot(RobotInfo[] robots, RobotType targetType, Set<Integer> married) {
		for (RobotInfo robot : robots) {
			if (robot.type == targetType && !married.contains(robot.ID)) {
				return robot;
			}
		}
		return null;
	}

}
