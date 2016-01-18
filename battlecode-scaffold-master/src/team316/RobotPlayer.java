package team316;

import java.util.ArrayList;
import java.util.Random;

//import com.sun.xml.internal.bind.v2.runtime.Location;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class RobotPlayer {

	public static Random rnd;
	public static RobotController rc;
	public static RCWrapper rcWrapper;
	static int[] tryDirections = {0, -1, 1, -2, 2};
	public final static int MESSAGE_MARRIAGE = 0;
	public final static int MESSAGE_ENEMY = 1;
	public final static int MESSAGE_TURRET_RECOMMENDED_DIRECTION = 2;
	public final static int MESSAGE_HELP_ARCHON = 3;
	public final static int MESSAGE_HELLO_ARCHON = 4;
	public final static int MESSAGE_BYE_ARCHON = 5;
	public final static int MESSAGE_WELCOME_ACTIVATED_ARCHON = 6;
	public final static int MESSAGE_DECLARE_LEADER = 7;
	static Player player = null;
	static MotionController mc = null;
	static PotentialField field = null;

	public static void run(RobotController rcIn) {
		MapLocation archonLoc = null;
		if (!rcIn.getType().equals(RobotType.ARCHON)) {
			RobotInfo[] robots = rcIn.senseNearbyRobots(2, rcIn.getTeam());
			for (RobotInfo r : robots) {
				if (r.type.equals(RobotType.ARCHON)) {
					archonLoc = r.location;
				}
			}
		}

		if (player == null) {
			switch (rcIn.getType()) {
				case ARCHON :
					field = PotentialField.archon();
					mc = new MotionController(field);
					player = new Archon(field, mc, rcIn);
					break;
				case GUARD :
					field = PotentialField.guard();
					mc = new MotionController(field);
					player = new Guard(archonLoc, field, mc);
					break;
				case SOLDIER :
					field = PotentialField.soldier();
					mc = new MotionController(field);
					player = new SoldierPF(archonLoc, field, mc, rcIn);
					break;
				case SCOUT :
					field = PotentialField.scout();
					mc = new MotionController(field);
					player = new Scout(archonLoc, field, mc);
					break;
				case VIPER :
					field = PotentialField.viper();
					mc = new MotionController(field);
					player = new Viper(archonLoc, field, mc);
					break;
				case TTM :
				case TURRET :
					field = PotentialField.turret();
					mc = new MotionController(field);
					player = new Turret(archonLoc, field, mc);
					break;
				default :
					throw new RuntimeException("UNKNOWN ROBOT TYPE!");
			}
		}

		rc = rcIn;
		rnd = new Random(rc.getID());
		while (true) {
			try {
				player.play(rc);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	public static int getPrice(RobotType t) {
		switch (t) {

			case GUARD :
				return 30;

			case SOLDIER :
				return 30;

			case SCOUT :
				return 40;

			case VIPER :
				return 100;

			case TURRET :
				return 125;

			default :
				throw new RuntimeException("This Robot Type cannot be built!");
		}
	}

	public static int directionToInt(Direction d) {
		Direction[] directions = Direction.values();
		for (int i = 0; i < 8; i++) {
			if (directions[i].equals(d))
				return i;
		}
		return -1;
	}

	private static MapLocation[] combineThings(RobotInfo[] visibleEnemyArray,
			Signal[] incomingSignals) {
		ArrayList<MapLocation> attackableEnemyArray = new ArrayList<MapLocation>();
		for (RobotInfo r : visibleEnemyArray) {
			attackableEnemyArray.add(r.location);
		}
		for (Signal s : incomingSignals) {
			if (s.getTeam() == rc.getTeam().opponent()) {
				MapLocation enemySignalLocation = s.getLocation();
				int distanceToSignalingEnemy = rc.getLocation()
						.distanceSquaredTo(enemySignalLocation);
				if (distanceToSignalingEnemy <= rc
						.getType().attackRadiusSquared) {
					attackableEnemyArray.add(enemySignalLocation);
				}
			}
		}
		MapLocation[] finishedArray = new MapLocation[attackableEnemyArray
				.size()];
		for (int i = 0; i < attackableEnemyArray.size(); i++) {
			finishedArray[i] = attackableEnemyArray.get(i);
		}
		return finishedArray;
	}

	public static void tryToMove(RobotController rc, Direction forward)
			throws GameActionException {
		if (rc.isCoreReady()) {
			for (int deltaD : tryDirections) {
				Direction maybeForward = Direction
						.values()[(forward.ordinal() + deltaD + 8) % 8];
				if (rc.canMove(maybeForward)) {
					rc.move(maybeForward);
					return;
				}
			}
			if (rc.getType().canClearRubble()) {
				// failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if (rc.senseRubble(
						ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(forward);
				}
			}
		}
	}

	public static void tryToMove(Direction forward) throws GameActionException {
		if (rc.isCoreReady()) {
			for (int deltaD : tryDirections) {
				Direction maybeForward = Direction
						.values()[(forward.ordinal() + deltaD + 8) % 8];
				if (rc.canMove(maybeForward)) {
					rc.move(maybeForward);
					return;
				}
			}
			if (rc.getType().canClearRubble()) {
				// failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if (rc.senseRubble(
						ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
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
			rc.setIndicatorString(c, "Enemy at location (" + r.location.x + ", "
					+ r.location.y + ")");
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

	public static RobotInfo findWeakestRobot(RobotController rc,
			RobotInfo[] listOfRobots) {
		double weakestSoFar = 0;
		RobotInfo weakest = null;
		int c = 4;
		for (RobotInfo r : listOfRobots) {
			rc.setIndicatorString(c, "Enemy at location (" + r.location.x + ", "
					+ r.location.y + ")");
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

	public static MapLocation findWeakestNonArchon(RobotInfo[] listOfRobots) {
		double weakestSoFar = 0;
		MapLocation weakestLocation = null;
		for (RobotInfo r : listOfRobots) {
			if (r.type.equals(RobotType.ARCHON)) {
				if (weakestLocation == null) {
					weakestLocation = r.location;
				}
				continue;
			}
			double weakness = Battle.weakness(r);
			if (weakness > weakestSoFar) {
				weakestLocation = r.location;
				weakestSoFar = weakness;
			}
		}
		return weakestLocation;
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