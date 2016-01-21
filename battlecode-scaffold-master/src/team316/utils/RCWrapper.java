package team316.utils;

import java.util.HashMap;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

/**
 * Convenience class for getting things from RobotController without extra
 * overhead. Like getting enemy locations without having to do sensing again.
 * 
 * @author aliamir
 *
 */
public class RCWrapper {
	private RobotController rc;
	private RobotInfo[] robotsNearby = null;
	private RobotInfo[] hostileNearby = null;
	private RobotInfo[] enemyTeamNearby = null;
	private RobotInfo[] attackableHostile = null;
	private RobotInfo[] attackableEnemyTeam = null;
	private Map<Direction, Integer> maxCoordinate = new HashMap<>();
	private Integer senseRadius = null;
	public RobotInfo archonNearby = null;
	public final Team myTeam;
	public final Team enemyTeam;
	private double previousHealth;
	private double currentHealth;
	private MapLocation currentLocation;
	private RobotType type;

	/**
	 * Creates a new instance of RobotController wrapper class with given robot
	 * controller.
	 * 
	 * @param rc
	 *            Controller to wrap.
	 */
	public RCWrapper(RobotController rc) {
		this.rc = rc;
		if (rc.getTeam().equals(Team.A)) {
			myTeam = Team.A;
			enemyTeam = Team.B;
		} else {
			myTeam = Team.B;
			enemyTeam = Team.A;
		}
		this.currentHealth = rc.getHealth();
		this.previousHealth = this.currentHealth;
		this.type = rc.getType();
	}

	/**
	 * Should be called on beginning of each turn.
	 */
	public void initOnNewTurn() {
		robotsNearby = null;
		hostileNearby = null;
		enemyTeamNearby = null;
		attackableHostile = null;
		attackableEnemyTeam = null;
		archonNearby = null;
		this.previousHealth = this.currentHealth;
		this.currentHealth = rc.getHealth();
		this.currentLocation = null;
	}

	/**
	 * Returns the senseRadius (not squared) of the robot.
	 * 
	 * @return
	 */
	public Integer getSenseRaidus() {
		if (senseRadius != null)
			return senseRadius;
		senseRadius = 0;
		while (senseRadius * senseRadius <= this.type.sensorRadiusSquared) {
			senseRadius++;
		}
		senseRadius -= 1;
		return senseRadius;
	}

	public MapLocation getCurrentLocation() {
		if (this.currentLocation == null) {
			this.currentLocation = rc.getLocation();
		}
		return this.currentLocation;
	}

	/**
	 * @return Whether current robot is under attack (with reference to previous
	 *         turn).
	 */
	public boolean isUnderAttack() {
		return currentHealth < previousHealth;
	}

	/**
	 * @return Hostile robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public RobotInfo[] hostileRobotsNearby() {
		if (hostileNearby != null) {
			return hostileNearby;
		}

		// Get the actual hostile robots.
		hostileNearby = rc.senseHostileRobots(rc.getLocation(),
				rc.getType().sensorRadiusSquared);
		putWeakestInFront(hostileNearby);
		return hostileNearby;
	}

	/**
	 * @return Enemy team's robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public RobotInfo[] enemyTeamRobotsNearby() {
		if (enemyTeamNearby != null) {
			return enemyTeamNearby;
		}

		// Get the enemy team robots.
		enemyTeamNearby = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared,
				enemyTeam);
		putWeakestInFront(enemyTeamNearby);
		return enemyTeamNearby;
	}

	/**
	 * @return Hostile robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public RobotInfo[] attackableHostileRobots() {
		if (attackableHostile != null) {
			return attackableHostile;
		}

		// Get the actual hostile robots.
		attackableHostile = rc.senseHostileRobots(rc.getLocation(),
				rc.getType().attackRadiusSquared);
		putWeakestInFront(attackableHostile);
		return attackableHostile;
	}

	/**
	 * @return Enemy team's robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public RobotInfo[] attackableEnemyTeamRobots() {
		if (attackableEnemyTeam != null) {
			return attackableEnemyTeam;
		}

		// Get the enemy team robots.
		attackableEnemyTeam = rc
				.senseNearbyRobots(rc.getType().attackRadiusSquared, enemyTeam);
		putWeakestInFront(attackableEnemyTeam);
		return attackableEnemyTeam;
	}

	public static void putWeakestInFront(RobotInfo[] robots) {
		if (robots.length == 0) {
			return;
		}
		
		int weakestId = 0;
		for (int i = 1; i < robots.length; ++i) {
			double weaknessCur = Battle.weakness(robots[i]);
			double weaknessBest = Battle.weakness(robots[weakestId]);
			if (weaknessCur > weaknessBest || (weaknessCur == weaknessBest
					&& robots[i].ID < robots[weakestId].ID)) {
				weakestId = i;
			}
		}

		RobotInfo tmp = robots[weakestId];
		robots[weakestId] = robots[0];
		robots[0] = tmp;
	}

	public void setMaxCoordinate(Direction direction, Integer value)
			throws GameActionException {
		if(value == -1 || value == null){
			return;
		}
		this.maxCoordinate.put(direction, value);
		this.rc.setIndicatorString(2, "I just knew about that " + direction + " border at " + value);
	}

	/**
	 * Gets the max coordinate in a certain direction.
	 * 
	 * @param direction
	 *            has to be NORTH, SOUTH, EAST, or WEST.
	 * @return
	 * @throws GameActionException
	 */
	public Integer getMaxCoordinate(Direction direction)
			throws GameActionException {
		if (this.maxCoordinate.containsKey(direction)) {
			return this.maxCoordinate.get(direction);
		}
		MapLocation lastTile = getLastTile(direction);
		if (lastTile == null) {
			return null;
		}
		if (direction.equals(Direction.WEST)
				|| direction.equals(Direction.EAST)) {
			System.out.println("Direction: " + direction + "coordinate" + lastTile.x);
			maxCoordinate.put(direction, lastTile.x);
		} else {
			if (direction.equals(Direction.NORTH)
					|| direction.equals(Direction.SOUTH)) {
				maxCoordinate.put(direction, lastTile.y);
			} else {
				return null;
			}
		}
		return this.maxCoordinate.get(direction);
	}

	/**
	 * Returns the last tile in a certain direction  
	 * starting from rcWrapper.getCurrentDirection()
	 * 
	 * Returns null for any direction other than those:
	 * NORTH, SOUTH, EAST, and WEST.
	 * 
	 * @param direction 
	 * @return
	 * @throws GameActionException
	 */
	public MapLocation getLastTile(Direction direction)
			throws GameActionException {
		boolean validDirection = direction.equals(Direction.NORTH) || direction.equals(Direction.SOUTH)
				||direction.equals(Direction.EAST) || direction.equals(Direction.WEST);
		if(!validDirection){
			return null;
		}
		if (rc.onTheMap(
				getCurrentLocation().add(direction, getSenseRaidus()))) {
			return null;
		}
		System.out.println(this.getCurrentLocation());
		for (int d = getSenseRaidus() - 1; d > 0; d--) {
			MapLocation proposedLocation = getCurrentLocation().add(direction,
					d);
			if (rc.onTheMap(proposedLocation)) {
				System.out.println("Direction:" + direction + "Location: " + proposedLocation);
				return proposedLocation;
			}
		}
		System.out.println("Direction:" + direction + "Location: " + this.getCurrentLocation());
		return this.getCurrentLocation();
	}
}
