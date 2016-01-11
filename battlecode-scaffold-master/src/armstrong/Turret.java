package armstrong;

import java.util.ArrayList;

import com.sun.glass.ui.Robot;

import armstrong.navigation.ParticleType;
import armstrong.navigation.PotentialField;
import armstrong.navigation.motion.MotionController;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Turret implements Player {
	
	private final PotentialField field;
	private final MotionController mc;
	
	public Turret(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		if (rc.getType().equals(RobotType.TURRET)) {
			turretCode(rc);
		} else {
			ttmCode(rc);
		}
	}
	
	public void ttmCode(RobotController rc) throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(rc, visibleEnemyArray,incomingSignals);
		Direction recommendedDirection = null;
		for(Signal s: incomingSignals){
			MapLocation enemyLocation = getTurretEnemyMessage(rc, s);
			if(enemyLocation != null && rc.getLocation().distanceSquaredTo(enemyLocation) > 5){
				rc.unpack();
				return;
			}
			recommendedDirection = getTurretRecommendedDirection(s);
			rc.setIndicatorString(0, "I was recommended to move in direction:" + RobotPlayer.directionToInt(recommendedDirection));
		}
		if(rc.isCoreReady() && recommendedDirection != null){
			RobotPlayer.tryToMove(rc, recommendedDirection);
		}
		if(enemyArray.length>0){
			rc.unpack();
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0];
				Direction toEnemy = rc.getLocation().directionTo(goal);
				RobotPlayer.tryToMove(rc, toEnemy);
			}
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = RobotPlayer.randomDirection();
					RobotPlayer.tryToMove(rc, away);
				}else{//maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000,rc.getTeam());
					MapLocation weakestOne = RobotPlayer.findWeakest(alliesToHelp);
					if(weakestOne!=null){//found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						RobotPlayer.tryToMove(rc, towardFriend);
					}
				}
			}
		}
	}
	
	private Direction getTurretRecommendedDirection(Signal s) {
		int[] message = s.getMessage(); 
		if(message != null){
			if(message[0] == RobotPlayer.MESSAGE_TURRET_RECOMMENDED_DIRECTION){
				return Direction.values()[message[1]];
			}
		}
		return null;
	}

	public void turretCode(RobotController rc) throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(rc, visibleEnemyArray,incomingSignals);
		boolean enemiesAround = false;
		Direction recommendedDirection = null;
		for(Signal s: incomingSignals){
			MapLocation enemyLocation = getTurretEnemyMessage(rc, s);
			if(enemyLocation != null && rc.canAttackLocation(enemyLocation)){
				rc.setIndicatorString(0,"Discovered an enemy around using signal.");
				enemiesAround = true;
				if(rc.isWeaponReady()){
					rc.setIndicatorString(0,"Attacking an enemy known using signal.");
					rc.attackLocation(enemyLocation);
				}

			}
			recommendedDirection = getTurretRecommendedDirection(s);
		}
		if(!enemiesAround){
			rc.setIndicatorString(0,"No enemy known using signal.");
		}
		if(enemyArray.length>0){
			enemiesAround = true;
			if(rc.isWeaponReady()){
				//look for adjacent enemies to attack
				for(MapLocation oneEnemy:enemyArray){
					if(rc.canAttackLocation(oneEnemy)){
						rc.setIndicatorString(1,"Attacking an enemy I can see.");
						rc.attackLocation(oneEnemy);
						break;
					}
				}
			}
			
			//could not find any enemies adjacent to attack
			//try to move toward them
			//TODO make sure that this if statement actually doesn't make any sense.
			/*
			if(rc.isCoreReady()){
				rc.pack();
			}
			*/
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				//TODO Fix logic here choose the condition for packing
				//rc.pack();
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(recommendedDirection != null && enemiesAround == false){
					rc.pack();
				}
			}
		}
	}

	private static MapLocation[] combineThings(RobotController rc, RobotInfo[] visibleEnemyArray, Signal[] incomingSignals) {
		ArrayList<MapLocation> attackableEnemyArray = new ArrayList<MapLocation>();
		for(RobotInfo r:visibleEnemyArray){
			attackableEnemyArray.add(r.location);
		}
		for(Signal s:incomingSignals){
			if (s.getTeam()==rc.getTeam().opponent()){
				MapLocation enemySignalLocation = s.getLocation();
				int distanceToSignalingEnemy = rc.getLocation().distanceSquaredTo(enemySignalLocation);
				if(distanceToSignalingEnemy<=rc.getType().attackRadiusSquared){
					attackableEnemyArray.add(enemySignalLocation);
				}
			}
		}
		MapLocation[] finishedArray = new MapLocation[attackableEnemyArray.size()];
		for(int i=0;i<attackableEnemyArray.size();i++){
			finishedArray[i]=attackableEnemyArray.get(i);
		}
		return finishedArray;
	}
	
	private static MapLocation getTurretEnemyMessage(RobotController rc, Signal s){
		int[] message = s.getMessage(); 
		if(s.getTeam().equals(rc.getTeam()) && message != null && s.getLocation().distanceSquaredTo(rc.getLocation()) <= 2){
			if(message[0] == RobotPlayer.MESSAGE_ENEMY){
				return RobotPlayer.decodeLocation(message[1]);
			}
		}
		return null;
	}
	
}
