package armstrong;

import java.util.HashSet;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
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

	@Override
	public void play(RobotController rc) throws GameActionException {
		if (broadcastNextTurn) {
			rc.broadcastMessageSignal(toBroadcastNextTurn[0], toBroadcastNextTurn[1], toBroadcastNextTurn[2]);
			broadcastNextTurn = false;
		}
		if (rc.isCoreReady()) {
			Direction randomDir = RobotPlayer.randomDirection();
			boolean backupTurret = false;
			RobotInfo choosenTurret = null;
			RobotType toBuild;
			if (lastBuilt == RobotType.TURRET) {
				// We can improve on this
				// for instance we can combine the two sense statements we have.
				RobotInfo[] alliesNearBy = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
				// turretsNearBy = filterRobotsbyType(alliesNearBy, targetType)
				choosenTurret = getLonelyRobot(alliesNearBy, RobotType.TURRET, marriedTurrets);
				if (choosenTurret != null) {
					toBuild = RobotType.SCOUT;
					backupTurret = true;
				} else {
					toBuild = buildList[RobotPlayer.rnd.nextInt(buildList.length)];
				}
			} else {
				toBuild = buildList[RobotPlayer.rnd.nextInt(buildList.length)];
			}
			if (rc.getTeamParts() > 100) {
				if (rc.canBuild(randomDir, toBuild)) {
					rc.build(randomDir, toBuild);
					lastBuilt = toBuild;
					if (backupTurret) {
						RobotInfo[] alliesVeryNear = rc.senseNearbyRobots(2, rc.getTeam());
						RobotInfo choosenScout = getLonelyRobot(alliesVeryNear, RobotType.SCOUT, marriedScouts);
						if (choosenTurret == null) {
							rc.disintegrate();
						}
						// rc.broadcastMessageSignal(choosenScout.ID,choosenTurret.ID,
						// choosenTurret.location.distanceSquaredTo(rc.getLocation()));
						toBroadcastNextTurn[0] = choosenScout.ID;
						toBroadcastNextTurn[1] = choosenTurret.ID;
						toBroadcastNextTurn[2] = 8;
						broadcastNextTurn = true;
						marriedTurrets.add(choosenTurret.ID);
						marriedScouts.add(choosenScout.ID);
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
