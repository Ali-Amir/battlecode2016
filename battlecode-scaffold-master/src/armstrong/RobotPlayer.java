package armstrong;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.hibernate.search.analyzer.Discriminator;

//import com.sun.xml.internal.bind.v2.runtime.Location;

import battlecode.common.*;

public class RobotPlayer{
	 
	static Random rnd;
	static RobotController rc;
	static int[] tryDirections = {0,-1,1,-2,2};
	static RobotType[] buildList = new RobotType[]{RobotType.GUARD,RobotType.TURRET};
	static RobotType lastBuilt = RobotType.ARCHON;
	static Set<Integer> marriedScouts = new HashSet<>();
	static Set<Integer> marriedTurrets = new HashSet<>();
	static int husbandTurretID = -1;
	static boolean broadcastNextTurn = false;
	static int[] toBroadcastNextTurn = new int[3];
	static int MESSAGE_MARRIAGE = 0;
	static int MESSAGE_ENEMY = 1;
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		rnd = new Random(rc.getID());
		while(true){
			try{
				if(rc.getType()==RobotType.ARCHON){
					archonCode();
				}else if(rc.getType()==RobotType.TURRET){
					turretCode();
				}else if(rc.getType()==RobotType.TTM){
					ttmCode();
				}else if(rc.getType()==RobotType.GUARD){
					guardCode();
				}else if(rc.getType()==RobotType.SOLDIER){
					guardCode();
				}else if(rc.getType()==RobotType.SCOUT){
					scoutCode();
				}
			}catch(Exception e){
				e.printStackTrace();
			}

			Clock.yield();
		}
	}
	private static MapLocation getTurretEnemyMessage(Signal s){
		int[] message = s.getMessage(); 
		if(s.getTeam().equals(rc.getTeam()) && message != null && s.getLocation().distanceSquaredTo(rc.getLocation()) <= 2){
			if(message[0] == MESSAGE_ENEMY){
				return decodeLocation(message[1]);
			}
		}
		return null;
	}
	private static void turretCode() throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(visibleEnemyArray,incomingSignals);
		boolean enemiesAround = false;
		for(Signal s: incomingSignals){
			MapLocation enemyLocation = getTurretEnemyMessage(s);
			if(enemyLocation != null && rc.canAttackLocation(enemyLocation)){
				rc.setIndicatorString(0,"Discovered an enemy around using signal");
				enemiesAround = true;
				if(rc.isWeaponReady()){
					rc.setIndicatorString(0,"Attemping attacking an enemy known using signal");
					rc.attackLocation(enemyLocation);
				}

			}
		}
		if(enemyArray.length>0){
			enemiesAround = true;
			if(rc.isWeaponReady()){
				//look for adjacent enemies to attack
				for(MapLocation oneEnemy:enemyArray){
					if(rc.canAttackLocation(oneEnemy)){
						rc.setIndicatorString(1,"trying to attack");
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
				if(nearbyFriends.length>3 && enemiesAround == false){
					rc.pack();
				}
			}
		}
	}
	private static int encodeLocation(MapLocation lc){
		final int maxOffset = 16000;
		final int range = 2 * maxOffset;
		int x = lc.x;
		int y = lc.y;
		x += maxOffset;
		y += maxOffset;
		return (range + 1) * x + y;
	}
	private static MapLocation decodeLocation(int code){
		final int maxOffset = 16000;
		final int range = 2 * maxOffset;
		int x = code/(range + 1);
		int y = code%(range + 1);
		return new MapLocation(x, y);
	}
	private static void ttmCode() throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(visibleEnemyArray,incomingSignals);
		for(Signal s: incomingSignals){
			MapLocation enemyLocation = getTurretEnemyMessage(s);
			if(enemyLocation != null && rc.getLocation().distanceSquaredTo(enemyLocation) > 5){
				rc.unpack();
				return;
			}		
		}
		if(enemyArray.length>0){
			rc.unpack();
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0];
				Direction toEnemy = rc.getLocation().directionTo(goal);
				tryToMove(toEnemy);
			}
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = randomDirection();
					tryToMove(away);
				}else{//maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000,rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if(weakestOne!=null){//found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						tryToMove(towardFriend);
					}
				}
			}
		}
	}
	
	private static void guardCode() throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		if(enemyArray.length>0){
			if(rc.isWeaponReady()){
				//look for adjacent enemies to attack
				for(RobotInfo oneEnemy:enemyArray){
					if(rc.canAttackLocation(oneEnemy.location)){
						rc.setIndicatorString(0,"trying to attack");
						rc.attackLocation(oneEnemy.location);
						break;
					}
				}
			}
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0].location;
				Direction toEnemy = rc.getLocation().directionTo(goal);
				tryToMove(toEnemy);
			}
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = randomDirection();
					tryToMove(away);
				}else{//maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000,rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if(weakestOne!=null){//found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						tryToMove(towardFriend);
					}
				}
			}
		}
	}
	
	private static MapLocation[] combineThings(RobotInfo[] visibleEnemyArray, Signal[] incomingSignals) {
		ArrayList<MapLocation> attackableEnemyArray = new ArrayList<MapLocation>();
		for(RobotInfo r:visibleEnemyArray){
			attackableEnemyArray.add(r.location);
		}
		for(Signal s:incomingSignals){
			if(s.getTeam()==rc.getTeam().opponent()){
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

	public static void tryToMove(Direction forward) throws GameActionException{
		if(rc.isCoreReady()){
			for(int deltaD:tryDirections){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
			if(rc.getType().canClearRubble()){
				//failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
					rc.clearRubble(forward);
				}
			}
		}
	}
	private static RobotInfo findWeakestRobot(RobotInfo[] listOfRobots){
		double weakestSoFar = 0;
		RobotInfo weakest = null;
		int c = 4;
		for(RobotInfo r:listOfRobots){
			rc.setIndicatorString(c, "Enemy at location (" + r.location.x + ", " + r.location.y + ")");
			c++;
			double weakness = r.maxHealth-r.health;
			//double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
			if(weakness>weakestSoFar){
				weakest = r;
				weakestSoFar=weakness;
			}
		}
		return weakest;
	}	
	private static MapLocation findWeakest(RobotInfo[] listOfRobots){
		double weakestSoFar = 0;
		MapLocation weakestLocation = null;
		for(RobotInfo r:listOfRobots){
			double weakness = r.maxHealth-r.health;
			//double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
			if(weakness>weakestSoFar){
				weakestLocation = r.location;
				weakestSoFar=weakness;
			}
		}
		return weakestLocation;
	}
	//Gets an unmarried turret or scout
	private static RobotInfo getLonelyRobot(RobotInfo[] robots,RobotType targetType,Set<Integer> married){
		for(RobotInfo robot: robots){
			if(robot.type == targetType && !married.contains(robot.ID)){
				return robot;
			}
		}
		return null;
	}
	//Checks 
	private static int getHusbandTurretID(Signal s){
		if(s.getTeam().equals(rc.getTeam()) && s.getMessage() != null){
			if(s.getMessage()[0] == rc.getID()){
				return s.getMessage()[1];
			}
		}
		return -1;
	}
	private static void scoutCode() throws GameActionException{		
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), -1);
		Signal[] incomingSignals = rc.emptySignalQueue();
		//MapLocation[] enemyArray = combineThings(visibleEnemyArray,incomingSignals);
		for(Signal s: incomingSignals){
			husbandTurretID = getHusbandTurretID(s);
			if(husbandTurretID != -1){
				break;
			}
		}
		RobotInfo[] visibleAlliesArray = rc.senseNearbyRobots();
		RobotInfo husband = null;
		for(int i = 0; i < visibleAlliesArray.length; i++){
			if(visibleAlliesArray[i].ID ==  husbandTurretID){
				husband = visibleAlliesArray[i];
			}
		}
		if(husband != null){
			RobotInfo targetRobot = getTurretTarget(husband.location, visibleEnemyArray);
			MapLocation target = null;
			if(targetRobot != null){
				target = targetRobot.location;
				rc.setIndicatorDot(target, 255, 0, 0);
				rc.setIndicatorString(2, "Target is at location (" + target.x + "," + target.y + ")");
			}
			Direction toHusband = rc.getLocation().directionTo(husband.location);
			if(rc.getLocation().distanceSquaredTo(husband.location)<= 2){
				if(target != null){
					rc.broadcastMessageSignal(MESSAGE_ENEMY, encodeLocation(target),rc.getLocation().distanceSquaredTo(husband.location));
				}
			}else{
				if(rc.isCoreReady()){
					tryToMove(toHusband);						
				}
			}

		}

	}
	private static RobotInfo getTurretTarget(MapLocation turretLocatoin, RobotInfo[] listOfRobots){
		double weakestSoFar = -1;
		RobotInfo weakest = null;
		for(int i = 0; i < 3; i++){
			rc.setIndicatorString(i, "");			
		}
		int c = 0;
		for(RobotInfo r:listOfRobots){
			int distanceSquared = r.location.distanceSquaredTo(turretLocatoin);
			if(r.team == rc.getTeam() || distanceSquared <=5 || distanceSquared > 48){
				continue;
			}
			rc.setIndicatorString(c, "can reach this enemy at location:(" + r.location.x + ", " + r.location.y + ")");
			c++;
			double weakness = r.maxHealth-r.health;
			//double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
			if(weakness>weakestSoFar){
				weakest = r;
				weakestSoFar=weakness;
			}
		}
		return weakest;
	}
	private static RobotInfo[] getArray(ArrayList<RobotInfo> selectedList){
		RobotInfo[] selectedArray = new RobotInfo[selectedList.size()];
		for(int i = 0;i < selectedList.size();i ++){
			selectedArray[i] = selectedList.get(i);
		}
		return selectedArray;
		
	}
	private static void archonCode() throws GameActionException {
		if(broadcastNextTurn){
			rc.broadcastMessageSignal(toBroadcastNextTurn[0], toBroadcastNextTurn[1], toBroadcastNextTurn[2]);
			broadcastNextTurn = false;
		}
		if(rc.isCoreReady()){
			Direction randomDir = randomDirection();
			boolean backupTurret = false;
			RobotInfo choosenTurret = null;
			RobotType toBuild;
			if(lastBuilt == RobotType.TURRET){
				//We can improve on this
				//for instance we can combine the two sense statements we have.
				RobotInfo[] alliesNearBy = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared,rc.getTeam()); 
				//turretsNearBy = filterRobotsbyType(alliesNearBy, targetType)
				choosenTurret = getLonelyRobot(alliesNearBy,RobotType.TURRET,marriedTurrets);
				if(choosenTurret != null){
					toBuild = RobotType.SCOUT;
					backupTurret = true;
				}else{
					toBuild = buildList[rnd.nextInt(buildList.length)];
				}
			}else{
				toBuild = buildList[rnd.nextInt(buildList.length)];
			}
			if(rc.getTeamParts()>100){
				if(rc.canBuild(randomDir, toBuild)){
					rc.build(randomDir,toBuild);
					lastBuilt = toBuild;
					if(backupTurret){
						RobotInfo[] alliesVeryNear = rc.senseNearbyRobots(2,rc.getTeam());
						RobotInfo choosenScout = getLonelyRobot(alliesVeryNear, RobotType.SCOUT, marriedScouts);
						if(choosenTurret == null){
							rc.disintegrate();
						}
						//rc.broadcastMessageSignal(choosenScout.ID,choosenTurret.ID, choosenTurret.location.distanceSquaredTo(rc.getLocation()));
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
			

			RobotInfo[] alliesToHelp = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared,rc.getTeam());
			MapLocation weakestOne = findWeakest(alliesToHelp);
			if(weakestOne!=null){
				rc.repair(weakestOne);
				return;
			}
		}
		
	}

	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	//filters robots by type
	private static RobotInfo[] filterRobotsbyType(RobotInfo[] robots, RobotType targetType){
		ArrayList<RobotInfo> selectedList = new ArrayList<>();
		for(RobotInfo r:robots){
			if(r.type.equals(targetType)){
				selectedList.add(r);				
			}
		}
		return getArray(selectedList);
	}
}