package armstrong;

import java.util.ArrayList;

import armstrong.navigation.PotentialField;
import armstrong.navigation.motion.MotionController;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Soldier implements Player {

	private final PotentialField field;
	private final MotionController mc;

	public Soldier(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}
	/*
	 * Smallest number here must be non-negative.
	 */
	private int getAttakTypePriority(RobotType t){
		switch (t) {
		case ARCHON:
			return 5;
		
		case GUARD:
			return 15;

		case SOLDIER:
			return 100;

		case SCOUT:
			return 1;
			
		case VIPER:
			return 10;
			
		case TTM:
			return 5;

		case TURRET:
			return 5;
		case BIGZOMBIE:
			return 10;
		case RANGEDZOMBIE:
			return 10;
		case STANDARDZOMBIE:
			return 5;
		case FASTZOMBIE:
			return 10;
		case ZOMBIEDEN:
			return 2;
		default:
			throw new RuntimeException("UNKNOWN ROBOT TYPE!");
		}

	}
	private double getWeakness(RobotInfo r){
		return (r.maxHealth - r.health);
	}
	private RobotInfo chooseTarget(RobotController rc, ArrayList<RobotInfo> attackableEnemies){
		RobotInfo bestTarget = null;
		int bestTypePriority = -1;
		double currentWeakness = -10000000;
		for(RobotInfo enemy: attackableEnemies){
			if(getAttakTypePriority(enemy.type) == bestTypePriority && getWeakness(enemy) > currentWeakness){
				bestTarget = enemy;
				currentWeakness = getWeakness(enemy);				
			}
			if(getAttakTypePriority(enemy.type) > bestTypePriority){
				bestTarget = enemy;
				bestTypePriority = getAttakTypePriority(enemy.type);
				currentWeakness = getWeakness(enemy);;
			}
		}
		return bestTarget;
	}
	@Override
	public void play(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		boolean enemyInAttackRange = false;
		ArrayList<RobotInfo> attackableEnemies = new ArrayList<>(); 
		if (enemyArray.length > 0) {
				rc.broadcastSignal(24);
				// look for adjacent enemies to attack
				for (RobotInfo oneEnemy : enemyArray) {
					if (rc.canAttackLocation(oneEnemy.location)) {
						enemyInAttackRange = true;
						attackableEnemies.add(oneEnemy);
					}
				}
				if (rc.isWeaponReady() && enemyInAttackRange) {
					RobotInfo targetEnemy = chooseTarget(rc, attackableEnemies);
					rc.attackLocation(targetEnemy.location);
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
