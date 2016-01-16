package team316.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
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
	private List<RobotInfo> robotsNearby = null;
	private List<RobotInfo> hostileNearby = null;
	private List<RobotInfo> enemyTeamNearby = null;
	private final Team enemyTeam;

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
	}

	/**
	 * Should be called on beginning of each turn.
	 */
	public void initOnNewTurn() {
		robotsNearby = null;
		hostileNearby = null;
		enemyTeamNearby = null;
	}

	/**
	 * Calls RobotController to get all robots nearby.
	 */
	private void getRobotsNearby() {
		assert robotsNearby == null;

		RobotInfo[] robots = rc.senseNearbyRobots();
		robotsNearby = Arrays.asList(robots);
	}

	/**
	 * @return Hostile robots nearby. Caches results to avoid overhead.
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
		return hostileNearby;
	}

	/**
	 * @return Enemy team's robots nearby. Caches results to avoid overhead.
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
		return enemyTeamNearby;
	}
}
