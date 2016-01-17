package team316.navigation;

import java.util.LinkedList;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import team316.RobotPlayer;

public class EnemyLocationModel {
	private final LinkedList<MapLocation> enemyArchon = new LinkedList<MapLocation>();
	private final MapLocation archonLoc;

	public EnemyLocationModel(MapLocation archonLoc) {
		this.archonLoc = archonLoc;
	}
	
	public void enemyBaseAt(MapLocation[] locs) {
		for (MapLocation loc : locs) {
			enemyArchon.add(loc);
		}
	}

	public ChargedParticle predictEnemyBase(RobotController rc) {
		int p = 0;
		for (; !enemyArchon.isEmpty();) {
			p = 0;
			for (int i = 0; i < enemyArchon.size(); ++i) {
				if (enemyArchon.get(i)
						.distanceSquaredTo(rc.getLocation()) < enemyArchon
								.get(p).distanceSquaredTo(rc.getLocation())) {
					p = i;
				}
			}
			if (enemyArchon.get(p).distanceSquaredTo(
					rc.getLocation()) > rc.getType().sensorRadiusSquared) {
				break;
			}
			enemyArchon.remove(p);
		}

		if (enemyArchon.isEmpty()) {
			return new ChargedParticle(0.0, rc.getLocation(), 0);
		}

		return new ChargedParticle(2.0, enemyArchon.get(p), 1);
		/*
		int p = 0;
		for (; !enemyArchon.isEmpty();) {
			p = 0;
			for (int i = 0; i < enemyArchon.size(); ++i) {
				if (enemyArchon.get(i)
						.distanceSquaredTo(rc.getLocation()) < enemyArchon
								.get(p).distanceSquaredTo(rc.getLocation())) {
					p = i;
				}
			}
			if (enemyArchon.get(p).distanceSquaredTo(
					rc.getLocation()) > rc.getType().sensorRadiusSquared) {
				break;
			}
			enemyArchon.remove(p);
		}

		if (enemyArchon.isEmpty()) {
			return new ChargedParticle(0.0, rc.getLocation(), 0);
		}

		return new ChargedParticle(2.0, enemyArchon.get(p), 1);
		*/
	}

	public void enemyAtLocation(MapLocation loc, RobotController rc) {
		return;
		/*
		MapLocation estimate = new MapLocation(
				loc.x + (loc.x - archonLoc.x) * 2,
				loc.y + (loc.y - archonLoc.y) * 2);
		for (MapLocation location : enemyArchon) {
			if (estimate.distanceSquaredTo(location) < 25) {
				return;
			}
		}
		enemyArchon.add(estimate);
		if (enemyArchon.size() > 10) {
			enemyArchon.remove(RobotPlayer.rnd.nextInt(11));
		}
		*/
	}

	public void enemyAlertFromLocation(MapLocation loc, RobotController rc) {
		return;
		/*
		MapLocation estimate = new MapLocation(
				rc.getLocation().x + (loc.x - archonLoc.x),
				rc.getLocation().y + (loc.y - archonLoc.y));
		for (MapLocation location : enemyArchon) {
			if (estimate.distanceSquaredTo(location) < 25) {
				return;
			}
		}
		enemyArchon.add(estimate);
		if (enemyArchon.size() > 10) {
			enemyArchon.remove(RobotPlayer.rnd.nextInt(11));
		}
		*/
	}
}
