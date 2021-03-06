package nagginghammer;

import java.util.Arrays;

import nagginghammer.navigation.PotentialField;
import nagginghammer.navigation.motion.MotionController;
import nagginghammer.utils.Battle;
import nagginghammer.utils.Turn;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Guard implements Player {

	private final PotentialField field;
	private final MotionController mc;

	public Guard(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), rc.getType().sensorRadiusSquared);
		Battle.addEnemyParticles(enemyArray, field, 5);

		// rc.setIndicatorString(2, "Enemies around: " + enemyArray.length + "
		// turn: " + Turn.currentTurn());
		if (enemyArray.length > 0) {
			if (rc.isWeaponReady()) {
				// look for adjacent enemies to attack
				Arrays.sort(enemyArray, (a, b) -> {
					double weaknessDiff = Battle.weakness(a) - Battle.weakness(b);
					return weaknessDiff < 0 ? 1 : weaknessDiff > 0 ? -1 : 0;
				});
				for (RobotInfo oneEnemy : enemyArray) {
					if (rc.canAttackLocation(oneEnemy.location)) {
						rc.setIndicatorString(0, "trying to attack " + Turn.currentTurn());
						rc.attackLocation(oneEnemy.location);
						return;
					}
				}
			} else {
				return;
			}

			// could not find any enemies adjacent to attack
			// try to move toward them
			if (rc.isCoreReady()) {
				//MapLocation goal = enemyArray[0].location;
				//Direction toEnemy = rc.getLocation().directionTo(goal);
				//RobotPlayer.tryToMove(rc, toEnemy);
				mc.tryToMove(rc);
				return;
			}
		}

		// rc.setIndicatorString(1, "Got here " + Turn.currentTurn());
		// there are no enemies nearby
		// check to see if we are in the way of friends
		// we are obstructing them
		if (rc.isCoreReady()) {
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
			Battle.addAllyParticles(nearbyFriends, field, 2);
			if (field.particles().size() == 0 && nearbyFriends.length > 2) {
				mc.tryToMoveRandom(rc);
			} else {
				if (!field.particles().isEmpty()) {
					rc.setIndicatorString(2, "Turn: " + Turn.currentTurn() + " Field: " + field.toString());
				}
				mc.tryToMove(rc);
			}
			//
			// if (nearbyFriends.length > 3) {
			// Direction away = RobotPlayer.randomDirection();
			// mc.tryToMove(rc);
			// // RobotPlayer.tryToMove(rc, away);
			// } else {// maybe a friend is in need!
			// RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000,
			// rc.getTeam());
			// MapLocation weakestOne = RobotPlayer.findWeakest(alliesToHelp);
			// if (weakestOne != null) {// found a friend most in need
			// mc.tryToMove(rc);
			// // Direction towardFriend =
			// // rc.getLocation().directionTo(weakestOne);
			// // RobotPlayer.tryToMove(rc, towardFriend);
			// }
			// }
		}
	}

}
