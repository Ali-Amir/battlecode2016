package armstrong;

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

public class Scout implements Player {

	private final PotentialField field;
	private final MotionController mc;

	private static int husbandTurretID = -1;

	public Scout(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), -1);
		Signal[] incomingSignals = rc.emptySignalQueue();
		//MapLocation[] enemyArray =  combineThings(visibleEnemyArray,incomingSignals);
		for (Signal s : incomingSignals) {
			husbandTurretID = RobotPlayer.getHusbandTurretID(rc, s);
			if (husbandTurretID != -1) {		
				break;
			}
		}
		rc.setIndicatorString(0, "My husband has ID: " + husbandTurretID);
		RobotInfo[] visibleAlliesArray = rc.senseNearbyRobots();
		RobotInfo husband = null;
		for (int i = 0; i < visibleAlliesArray.length; i++) {
			if (visibleAlliesArray[i].ID == husbandTurretID) {
				husband = visibleAlliesArray[i];
			}
		}
		/*
		if(husbandTurretID != -1 && rc.canSenseRobot(husbandTurretID)){
			husband = rc.senseRobot(husbandTurretID);
		}*/

		if (husband != null) {
			RobotInfo targetRobot = getTurretTarget(rc, husband.location, visibleEnemyArray);
			MapLocation target = null;
			if (targetRobot != null) {
				target = targetRobot.location;
				rc.setIndicatorDot(target, 255, 0, 0);
				rc.setIndicatorString(2, "Target is at location (" + target.x + "," + target.y + ")");
			}
			Direction toHusband = rc.getLocation().directionTo(husband.location);
			if (rc.getLocation().distanceSquaredTo(husband.location) <= 2) {
				if (target != null) {
					rc.broadcastMessageSignal(RobotPlayer.MESSAGE_ENEMY, RobotPlayer.encodeLocation(target),
							rc.getLocation().distanceSquaredTo(husband.location));
				}
			} else {
				if (rc.isCoreReady()) {
					RobotPlayer.tryToMove(rc, toHusband);
				}
			}

		}
	}

	private static RobotInfo getTurretTarget(RobotController rc, MapLocation turretLocation, RobotInfo[] listOfRobots) throws GameActionException {
		PotentialField field = PotentialField.turret();		
		double weakestSoFar = -1;
		RobotInfo weakest = null;
		/*
		for (int i = 0; i < 3; i++) {
			rc.setIndicatorString(i, "");
		}*/
		int c = 0;
		boolean farEnemies = false;
		for (RobotInfo r : listOfRobots) {
			int distanceSquared = r.location.distanceSquaredTo(turretLocation);
			if (r.team == rc.getTeam()) {
				continue;
			}
			if(distanceSquared <= 5){
				
			}else if(distanceSquared <= 48){
				//rc.setIndicatorString(c, "can reach this enemy at location:(" + r.location.x + ", " + r.location.y + ")");
				c++;
				if(r.type == RobotType.ARCHON){
					return r;
				}
				double weakness = r.maxHealth - r.health;
				// double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
				if (weakness > weakestSoFar) {
					weakest = r;
					weakestSoFar = weakness;
				}				
			}else if(distanceSquared > 48){
				farEnemies = true;
				if(r.type.equals(RobotType.ARCHON)){
					field.addParticle(ParticleType.ALLY_ARCHON, r.location, 10);
				}
			}
		}
		if(weakest == null && farEnemies){
			Direction turretRecommendedDirection = field.strongetAttractionDirection(turretLocation);
			//rc.setIndicatorString(0, "I am recommending direction: " + RobotPlayer.directionToInt(turretRecommendedDirection));
			rc.broadcastMessageSignal(RobotPlayer.MESSAGE_TURRET_RECOMMENDED_DIRECTION, RobotPlayer.directionToInt(turretRecommendedDirection), rc.getLocation().distanceSquaredTo(turretLocation));
		}
		return weakest;
	}

}
