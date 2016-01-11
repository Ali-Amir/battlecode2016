package armstrong;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.hibernate.search.analyzer.Discriminator;

import armstrong.navigation.PotentialField;
import armstrong.navigation.motion.MotionController;
import armstrong.utils.Turn;

//import com.sun.xml.internal.bind.v2.runtime.Location;

import battlecode.common.*;

public class RobotPlayer {

	public static Random rnd;
	static RobotController rc;
	static int[] tryDirections = { 0, -1, 1, -2, 2 };
	public static int MESSAGE_MARRIAGE = 0;
	public static int MESSAGE_ENEMY = 1;
	public static int MESSAGE_TURRET_RECOMMENDED_DIRECTION = 2;
	
	static Player player = null;
	static MotionController mc = null;
	static PotentialField field = null;

	public static void run(RobotController rcIn) {

		if (player == null) {
			switch (rcIn.getType()) {
			case ARCHON:
				player = new Archon();
				break;
			case GUARD:
				field = PotentialField.guard();
				mc = new MotionController(field);
				player = new Guard(field, mc);
				break;
			case SOLDIER:
				field = PotentialField.soldier();
				mc = new MotionController(field);
				player = new Soldier(field, mc);
				break;
			case SCOUT:
				field = PotentialField.scout();
				mc = new MotionController(field);
				player = new Scout(field, mc);
				break;
			case VIPER:
				field = PotentialField.viper();
				mc = new MotionController(field);
				player = new Viper(field, mc);
				break;
			case TTM:
			case TURRET:
				field = PotentialField.turret();
				mc = new MotionController(field);
				player = new Turret(field, mc);
				break;
			default:
				throw new RuntimeException("UNKNOWN ROBOT TYPE!");
			}
		}

		rc = rcIn;
		rnd = new Random(rc.getID());
		while (true) {
			try {
				player.play(rc);
				Turn.increaseTurn();
			} catch (Exception e) {
				e.printStackTrace();
			}

			Clock.yield();
		}
	}

	private static MapLocation getTurretEnemyMessage(Signal s) {
		int[] message = s.getMessage();
		if (s.getTeam().equals(rc.getTeam()) && message != null
				&& s.getLocation().distanceSquaredTo(rc.getLocation()) <= 2) {
			if (message[0] == MESSAGE_ENEMY) {
				return decodeLocation(message[1]);
			}
		}
		return null;
	}
	public static int directionToInt(Direction d){
		Direction[] directions = Direction.values();
		for(int i = 0;i < 8;i++){
			if(directions[i].equals(d))
				return i;
		}
		return-1;
	}
	private static void turretCode() throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(visibleEnemyArray, incomingSignals);
		boolean enemiesAround = false;
		for (Signal s : incomingSignals) {
			MapLocation enemyLocation = getTurretEnemyMessage(s);
			if (enemyLocation != null && rc.canAttackLocation(enemyLocation)) {
				rc.setIndicatorString(0, "Discovered an enemy around using signal");
				enemiesAround = true;
				if (rc.isWeaponReady()) {
					rc.setIndicatorString(0, "Attemping attacking an enemy known using signal");
					rc.attackLocation(enemyLocation);
				}

			}
		}
		if (enemyArray.length > 0) {
			enemiesAround = true;
			if (rc.isWeaponReady()) {
				// look for adjacent enemies to attack
				for (MapLocation oneEnemy : enemyArray) {
					if (rc.canAttackLocation(oneEnemy)) {
						rc.setIndicatorString(1, "trying to attack");
						rc.attackLocation(oneEnemy);
						break;
					}
				}
			}

			// could not find any enemies adjacent to attack
			// try to move toward them
			// TODO make sure that this if statement actually doesn't make any
			// sense.
			/*
			 * if(rc.isCoreReady()){ rc.pack(); }
			 */
		} else {// there are no enemies nearby
				// check to see if we are in the way of friends
				// we are obstructing them
			if (rc.isCoreReady()) {
				// TODO Fix logic here choose the condition for packing
				// rc.pack();
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if (nearbyFriends.length > 3 && enemiesAround == false) {
					rc.pack();
				}
			}
		}
	}

	public static int encodeLocation(MapLocation lc) {
		final int maxOffset = 16000;
		final int range = 2 * maxOffset;
		int x = lc.x;
		int y = lc.y;
		x += maxOffset;
		y += maxOffset;
		return (range + 1) * x + y;
	}

	public static MapLocation decodeLocation(int code) {
		final int maxOffset = 16000;
		final int range = 2 * maxOffset;
		int x = code / (range + 1);
		int y = code % (range + 1);
		return new MapLocation(x - maxOffset, y - maxOffset);
	}

	private static void ttmCode() throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(visibleEnemyArray, incomingSignals);
		for (Signal s : incomingSignals) {
			MapLocation enemyLocation = getTurretEnemyMessage(s);
			if (enemyLocation != null && rc.getLocation().distanceSquaredTo(enemyLocation) > 5) {
				rc.unpack();
				return;
			}
		}
		if (enemyArray.length > 0) {
			rc.unpack();
			// could not find any enemies adjacent to attack
			// try to move toward them
			if (rc.isCoreReady()) {
				MapLocation goal = enemyArray[0];
				Direction toEnemy = rc.getLocation().directionTo(goal);
				tryToMove(toEnemy);
			}
		} else {// there are no enemies nearby
				// check to see if we are in the way of friends
				// we are obstructing them
			if (rc.isCoreReady()) {
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if (nearbyFriends.length > 3) {
					Direction away = randomDirection();
					tryToMove(away);
				} else {// maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000, rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if (weakestOne != null) {// found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						tryToMove(towardFriend);
					}
				}
			}
		}
	}

	private static void guardCode() throws GameActionException {
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
				tryToMove(toEnemy);
			}
		} else {// there are no enemies nearby
				// check to see if we are in the way of friends
				// we are obstructing them
			if (rc.isCoreReady()) {
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if (nearbyFriends.length > 3) {
					Direction away = randomDirection();
					tryToMove(away);
				} else {// maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000, rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if (weakestOne != null) {// found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						tryToMove(towardFriend);
					}
				}
			}
		}
	}

	private static MapLocation[] combineThings(RobotInfo[] visibleEnemyArray, Signal[] incomingSignals) {
		ArrayList<MapLocation> attackableEnemyArray = new ArrayList<MapLocation>();
		for (RobotInfo r : visibleEnemyArray) {
			attackableEnemyArray.add(r.location);
		}
		for (Signal s : incomingSignals) {
			if (s.getTeam() == rc.getTeam().opponent()) {
				MapLocation enemySignalLocation = s.getLocation();
				int distanceToSignalingEnemy = rc.getLocation().distanceSquaredTo(enemySignalLocation);
				if (distanceToSignalingEnemy <= rc.getType().attackRadiusSquared) {
					attackableEnemyArray.add(enemySignalLocation);
				}
			}
		}
		MapLocation[] finishedArray = new MapLocation[attackableEnemyArray.size()];
		for (int i = 0; i < attackableEnemyArray.size(); i++) {
			finishedArray[i] = attackableEnemyArray.get(i);
		}
		return finishedArray;
	}

	public static void tryToMove(RobotController rc, Direction forward) throws GameActionException {
		if (rc.isCoreReady()) {
			for (int deltaD : tryDirections) {
				Direction maybeForward = Direction.values()[(forward.ordinal() + deltaD + 8) % 8];
				if (rc.canMove(maybeForward)) {
					rc.move(maybeForward);
					return;
				}
			}
			if (rc.getType().canClearRubble()) {
				// failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(forward);
				}
			}
		}
	}

	public static void tryToMove(Direction forward) throws GameActionException {
		if (rc.isCoreReady()) {
			for (int deltaD : tryDirections) {
				Direction maybeForward = Direction.values()[(forward.ordinal() + deltaD + 8) % 8];
				if (rc.canMove(maybeForward)) {
					rc.move(maybeForward);
					return;
				}
			}
			if (rc.getType().canClearRubble()) {
				// failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(forward);
				}
			}
		}
	}

	public static RobotInfo findWeakestRobot(RobotInfo[] listOfRobots) {
		double weakestSoFar = 0;
		RobotInfo weakest = null;
		int c = 4;
		for (RobotInfo r : listOfRobots) {
			rc.setIndicatorString(c, "Enemy at location (" + r.location.x + ", " + r.location.y + ")");
			c++;
			double weakness = r.maxHealth - r.health;
			// double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
			if (weakness > weakestSoFar) {
				weakest = r;
				weakestSoFar = weakness;
			}
		}
		return weakest;
	}

	public static RobotInfo findWeakestRobot(RobotController rc, RobotInfo[] listOfRobots) {
		double weakestSoFar = 0;
		RobotInfo weakest = null;
		int c = 4;
		for (RobotInfo r : listOfRobots) {
			rc.setIndicatorString(c, "Enemy at location (" + r.location.x + ", " + r.location.y + ")");
			c++;
			double weakness = r.maxHealth - r.health;
			// double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
			if (weakness > weakestSoFar) {
				weakest = r;
				weakestSoFar = weakness;
			}
		}
		return weakest;
	}

	public static MapLocation findWeakest(RobotInfo[] listOfRobots) {
		double weakestSoFar = 0;
		MapLocation weakestLocation = null;
		for (RobotInfo r : listOfRobots) {
			double weakness = r.maxHealth - r.health;
			// double weakness = (r.maxHealth-r.health)*1.0/r.maxHealth;
			if (weakness > weakestSoFar) {
				weakestLocation = r.location;
				weakestSoFar = weakness;
			}
		}
		return weakestLocation;
	}

	public static int getHusbandTurretID(RobotController rc, Signal s) {
		if (s.getTeam().equals(rc.getTeam()) && s.getMessage() != null) {
			if (s.getMessage()[0] == rc.getID()) {
				return s.getMessage()[1];
			}
		}
		return -1;
	}

	public static int getHusbandTurretID(Signal s) {
		if (s.getTeam().equals(rc.getTeam()) && s.getMessage() != null) {
			if (s.getMessage()[0] == rc.getID()) {
				return s.getMessage()[1];
			}
		}
		return -1;
	}

	public static Direction randomDirection() {
		return Direction.values()[(int) (rnd.nextDouble() * 8)];
	}
}