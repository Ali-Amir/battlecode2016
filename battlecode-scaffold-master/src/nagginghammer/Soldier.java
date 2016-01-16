package nagginghammer;

import nagginghammer.navigation.PotentialField;
import nagginghammer.navigation.motion.MotionController;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class Soldier implements Player {

	private final PotentialField field;
	private final MotionController mc;

	public Soldier(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}
	private static int messageDelay = 0;
	@Override
	public void play(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		boolean isWeaponReady = rc.isWeaponReady();
		boolean enemyInAttackRange = false;
		if (enemyArray.length > 0) {
				rc.broadcastSignal(16);
				// look for adjacent enemies to attack
				for (RobotInfo oneEnemy : enemyArray) {
					if (rc.canAttackLocation(oneEnemy.location)) {
						enemyInAttackRange = true;
						if (isWeaponReady) {
							rc.attackLocation(oneEnemy.location);
						}
						break;
					}
				}
			if (rc.isCoreReady() && !enemyInAttackRange) {
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
					//RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000, rc.getTeam());
					//MapLocation weakestOne = RobotPlayer.findWeakest(alliesToHelp);
					MapLocation weakestOne = null;
					while(true){
						Signal s = rc.readSignal();
						if(s == null)
							break;
						if(s.getMessage() == null && s.getTeam().equals(rc.getTeam()))
							weakestOne = s.getLocation();
					}

					if (weakestOne != null) {// found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						RobotPlayer.tryToMove(rc, towardFriend);
					}
				}
			}
		}
	}

}
