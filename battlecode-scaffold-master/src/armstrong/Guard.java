package armstrong;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import navigation.PotentialField;
import navigation.motion.MotionController;

public class Guard implements Player {
	
	private final PotentialField field;
	private final MotionController mc;
	
	public Guard(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		if (enemyArray.length > 0) {
			if (rc.isWeaponReady()) {
				// look for adjacent enemies to attack
				for (RobotInfo oneEnemy : enemyArray) {
					if (rc.canAttackLocation(oneEnemy.location)) {
						rc.setIndicatorString(0, "trying to attack");
						rc.attackLocation(oneEnemy.location);
						break;
					}
				}
			}
			// could not find any enemies adjacent to attack
			// try to move toward them
			if (rc.isCoreReady()) {
				MapLocation goal = enemyArray[0].location;
				Direction toEnemy = rc.getLocation().directionTo(goal);
				RobotPlayer.tryToMove(rc, toEnemy);
			}
		} else {// there are no enemies nearby
				// check to see if we are in the way of friends
				// we are obstructing them
			if (rc.isCoreReady()) {
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if (nearbyFriends.length > 3) {
					Direction away = RobotPlayer.randomDirection();
					RobotPlayer.tryToMove(rc, away);
				} else {// maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000, rc.getTeam());
					MapLocation weakestOne = RobotPlayer.findWeakest(alliesToHelp);
					if (weakestOne != null) {// found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						RobotPlayer.tryToMove(rc, towardFriend);
					}
				}
			}
		}
	}

}
