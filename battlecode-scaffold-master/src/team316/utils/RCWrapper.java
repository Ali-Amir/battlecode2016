package team316.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team316.SoldierPF;

/**
 * Convenience class for getting things from RobotController without extra
 * overhead. Like getting enemy locations without having to do sensing again.
 * 
 * @author aliamir
 *
 */
public class RCWrapper {
	private RobotController rc;
	private List<RobotInfo> robotsNearby = null;
	private List<RobotInfo> hostileNearby = null;
	private List<RobotInfo> enemyTeamNearby = null;
	private List<RobotInfo> attackableHostile = null;
	private List<RobotInfo> attackableEnemyTeam = null;
	private Map<Direction, Integer> maxCoordinate = new HashMap<>();
	private Integer senseRadius = null;
	public RobotInfo archonNearby = null;
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
			enemyTeam = Team.B;
		} else {
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
	 * Calls RobotController to get all robots nearby.
	 */
	private void getRobotsNearby() {
		assert robotsNearby == null;

		RobotInfo[] robots = rc.senseNearbyRobots();
		// look for adjacent enemies to attack
		Arrays.sort(robots, (a, b) -> {
			/*
			 * int priorityDiff = SoldierPF.getAttakTypePriority(b.type) -
			 * SoldierPF.getAttakTypePriority(a.type); if (priorityDiff != 0) {
			 * return priorityDiff; }
			 */

			double weaknessDiff = Battle.weakness(a) - Battle.weakness(b);
			if (weaknessDiff != 0.0) {
				return weaknessDiff < 0 ? 1 : weaknessDiff > 0 ? -1 : 0;
			}
			return a.ID - b.ID;
		});
		robotsNearby = Collections.unmodifiableList(Arrays.asList(robots));

		// Get closest archon.
		for (RobotInfo r : robots) {
			if (r.type.equals(RobotType.ARCHON)
					&& r.team.equals(rc.getTeam())) {
				if (archonNearby == null || archonNearby.location
						.distanceSquaredTo(getCurrentLocation()) > r.location
								.distanceSquaredTo(getCurrentLocation())) {
					archonNearby = r;
				}
			}
		}
	}

	/**
	 * @return Hostile robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public List<RobotInfo> hostileRobotsNearby() {
		if (robotsNearby == null) {
			getRobotsNearby();
		}

		if (hostileNearby != null) {
			return hostileNearby;
		}

		// Get the actual hostile robots.
		hostileNearby = new ArrayList<>();
		for (RobotInfo r : robotsNearby) {
			if (r.team.equals(enemyTeam) || r.team.equals(Team.ZOMBIE)) {
				hostileNearby.add(r);
			}
		}
		hostileNearby = Collections.unmodifiableList(hostileNearby);
		return hostileNearby;
	}

	/**
	 * @return Enemy team's robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public List<RobotInfo> enemyTeamRobotsNearby() {
		if (robotsNearby == null) {
			getRobotsNearby();
		}

		if (enemyTeamNearby != null) {
			return enemyTeamNearby;
		}

		// Get the enemy team robots.
		enemyTeamNearby = new ArrayList<>();
		for (RobotInfo r : robotsNearby) {
			if (r.team.equals(enemyTeam)) {
				enemyTeamNearby.add(r);
			}
		}
		enemyTeamNearby = Collections.unmodifiableList(enemyTeamNearby);
		return enemyTeamNearby;
	}

	/**
	 * @return Hostile robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public List<RobotInfo> attackableHostileRobots() {
		if (robotsNearby == null) {
			getRobotsNearby();
		}

		if (attackableHostile != null) {
			return attackableHostile;
		}

		if (hostileNearby == null) {
			hostileRobotsNearby();
		}

		// Get the actual hostile robots.
		attackableHostile = new ArrayList<>();
		for (RobotInfo r : hostileNearby) {
			if (rc.canAttackLocation(r.location)) {
				attackableHostile.add(r);
			}
		}
		attackableHostile = Collections.unmodifiableList(attackableHostile);
		return attackableHostile;
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
	public MapLocation getLastTile(Direction direction)
			throws GameActionException {
		if (rc.onTheMap(
				getCurrentLocation().add(direction, getSenseRaidus()))) {
			return null;
		}
		for (int d = getSenseRaidus() - 1; d > 0; d--) {
			MapLocation proposedLocation = getCurrentLocation().add(direction,
					-d);
			if (rc.onTheMap(proposedLocation)) {
				return proposedLocation;
			}
		}
		return this.currentLocation;
	}
	/**
	 * @return Enemy team's robots nearby sorted by attack priority (first has
	 *         highest priority). Caches results to avoid overhead.
	 */
	public List<RobotInfo> attackableEnemyTeamRobots() {
		if (robotsNearby == null) {
			getRobotsNearby();
		}

		if (attackableEnemyTeam != null) {
			return attackableEnemyTeam;
		}

		if (enemyTeamNearby == null) {
			enemyTeamRobotsNearby();
		}

		// Get the enemy team robots.
		attackableEnemyTeam = new ArrayList<>();
		for (RobotInfo r : enemyTeamNearby) {
			if (rc.canAttackLocation(r.location)) {
				attackableEnemyTeam.add(r);
			}
		}
		attackableEnemyTeam = Collections.unmodifiableList(attackableEnemyTeam);
		return attackableEnemyTeam;
	}
}
