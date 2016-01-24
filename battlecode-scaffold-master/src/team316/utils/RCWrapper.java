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
	private Map<Direction, Integer> maxSoFarCoordinate = new HashMap<>();
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
		// this.senseRadius = getSenseRaidus();
	}

	/**
	 * Should be called on beginning of each turn.
	 * 
	 * @throws GameActionException
	 */
	public void initOnNewTurn() throws GameActionException {
		robotsNearby = null;
		hostileNearby = null;
		enemyTeamNearby = null;
		attackableHostile = null;
		attackableEnemyTeam = null;
		archonNearby = null;
		this.previousHealth = this.currentHealth;
		this.currentHealth = rc.getHealth();
		this.currentLocation = null;
		String output = "Max so far in ";
		for (int i = 0; i < 4; i++) {
			Direction direction = Grid.mainDirections[i];
			//output += direction + " is " + "something";
			//output += direction + " is " + getMaxSoFarCoordinate(direction);
		}
		//output += "broadcast:" + maxBroadcastRadius();
		//rc.setIndicatorString(1, output);
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
		if (value == -1 || value == null) {
			return;
		}
		this.maxSoFarCoordinate.put(direction, value);
		this.maxCoordinate.put(direction, value);
		//this.rc.setIndicatorString(2,
			//	"I just knew about that " + direction + " border at " + value);
	}

	public Integer getMaxSoFarCoordinate(Direction direction)
			throws GameActionException {
		getMaxCoordinate(direction);
		return maxSoFarCoordinate.get(direction);
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
			MapLocation furthest = getCurrentLocation().add(direction,
					getSenseRaidus());
			int coordinate = Grid.getRelevantCoordinate(direction, furthest);
			coordinate = Grid.compareCoordinates(direction, coordinate,
					this.maxSoFarCoordinate.getOrDefault(direction, null));

			maxSoFarCoordinate.put(direction, coordinate);
			return null;
		}
		int coordinate = Grid.getRelevantCoordinate(direction, lastTile);
		maxCoordinate.put(direction, coordinate);
		maxSoFarCoordinate.put(direction, coordinate);
		return coordinate;
	}

	/**
	 * Returns the last tile in a certain direction starting from
	 * rcWrapper.getCurrentDirection()
	 * 
	 * Returns null for any direction other than those: NORTH, SOUTH, EAST, and
	 * WEST.
	 * 
	 * @param direction
	 * @return
	 * @throws GameActionException
	 */
	public MapLocation getLastTile(Direction direction)
			throws GameActionException {
		if (!Grid.isMainDirection(direction)) {
			return null;
		}
		if (rc.onTheMap(
				getCurrentLocation().add(direction, getSenseRaidus()))) {
			return null;
		}
		// System.out.println(this.getCurrentLocation());
		for (int d = getSenseRaidus() - 1; d > 0; d--) {
			MapLocation proposedLocation = getCurrentLocation().add(direction,
					d);
			if (rc.onTheMap(proposedLocation)) {
				// System.out.println("Direction:" + direction + "Location: " +
				// proposedLocation);
				return proposedLocation;
			}
		}
		// System.out.println("Direction:" + direction + "Location: " +
		// this.getCurrentLocation());
		return this.getCurrentLocation();
	}
	public Integer maxBroadcastRadius() throws GameActionException{
		int x = getCurrentLocation().x;
		int x1 = getMaxSoFarCoordinate(Direction.WEST);
		int x2 = getMaxSoFarCoordinate(Direction.EAST);
		int restx = 80 - (x2 - x1);
		int xComponent = restx + Math.max(x - x1, x2 - x);
		int y = getCurrentLocation().y;
		int y1 = getMaxSoFarCoordinate(Direction.NORTH);
		int y2 = getMaxSoFarCoordinate(Direction.SOUTH);
		int resty = 80 - (y2 - y1);
		int yComponent = resty + Math.max(y - y1, y2 - y);
		return xComponent*xComponent + yComponent*yComponent;
	}
}
