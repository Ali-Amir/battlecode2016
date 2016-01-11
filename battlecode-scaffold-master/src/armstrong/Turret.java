package armstrong;

import java.util.ArrayList;

import com.sun.glass.ui.Robot;

import armstrong.navigation.ParticleType;
import armstrong.navigation.PotentialField;
import armstrong.navigation.motion.MotionController;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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
		//RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		//MapLocation[] enemyArray = combineThings(rc, visibleEnemyArray,incomingSignals);
		Direction recommendedDirection = null;
		for(Signal s: incomingSignals){
			MapLocation enemyLocation = getTurretEnemyMessage(rc, s);
			if(enemyLocation != null){
				int distanceToEnemy = rc.getLocation().distanceSquaredTo(enemyLocation);
				if(distanceToEnemy > 5 && distanceToEnemy <= 48){
					rc.unpack();
					return;
				}
			}
			recommendedDirection = getTurretRecommendedDirection(s);
			rc.setIndicatorString(0, "I was recommended to move in direction:" + RobotPlayer.directionToInt(recommendedDirection));
		}
		if(recommendedDirection != null){
			if(rc.isCoreReady()){
				RobotPlayer.tryToMove(rc, recommendedDirection);				
			}
			return;
		}
		rc.unpack();
		//TODO TTM should see if there are guards near by that it should escape from.
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
		boolean enemiesAround = false;
		Direction recommendedDirection = null;
		Signal[] incomingSignals = rc.emptySignalQueue();
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
		if(enemiesAround){
			return;			
		}
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		MapLocation[] enemyArray = combineThings(rc, visibleEnemyArray,incomingSignals);
		rc.setIndicatorString(0,"No enemy known using signal.");
		
		for(MapLocation oneEnemy:enemyArray){
			if(rc.canAttackLocation(oneEnemy)){
				enemiesAround = true;
				if(rc.isWeaponReady()){
					rc.setIndicatorString(1,"Attacking an enemy I can see.");
					rc.attackLocation(oneEnemy);
					break;
				}
			}
		}
		if(rc.isCoreReady() && recommendedDirection != null && enemiesAround == false){
				rc.pack();
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
