package team316.utils;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import team316.navigation.ChargedParticle;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;

public class Battle {
	public static void addAllyParticles(RobotInfo[] allyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : allyArray) {
			switch (e.type) {
				case ARCHON :
					field.addParticle(ParticleType.ALLY_ARCHON, e.location,
							lifetime);
					break;
				case TTM :
					field.addParticle(ParticleType.ALLY_TURRET, e.location,
							lifetime);
					break;
				case TURRET :
					field.addParticle(ParticleType.ALLY_TURRET, e.location,
							lifetime);
					break;
				case GUARD :
				case SCOUT :
				case SOLDIER :
				case VIPER :
					// field.addParticle(ParticleType.ALLY_DEFAULT, e.location,
					// lifetime);
					break;
				default :
					throw new RuntimeException("Unknown type!");
			}
		}
	}

	public static void addEnemyParticles(RobotInfo[] enemyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : enemyArray) {
			switch (e.type) {
				case ARCHON :
					field.addParticle(ParticleType.OPPOSITE_ARCHON, e.location,
							lifetime);
					break;
				case GUARD :
					field.addParticle(ParticleType.OPPOSITE_GUARD, e.location,
							lifetime);
					break;
				case SCOUT :
					field.addParticle(ParticleType.OPPOSITE_SCOUT, e.location,
							lifetime);
					break;
				case TTM :
					field.addParticle(ParticleType.OPPOSITE_TURRET, e.location,
							lifetime);
					break;
				case TURRET :
					field.addParticle(ParticleType.OPPOSITE_TURRET, e.location,
							lifetime);
					break;
				case SOLDIER :
					field.addParticle(ParticleType.OPPOSITE_SOLDIER, e.location,
							lifetime);
					break;
				case VIPER :
					field.addParticle(ParticleType.OPPOSITE_VIPER, e.location,
							lifetime);
					break;
				case BIGZOMBIE :
				case FASTZOMBIE :
				case RANGEDZOMBIE :
				case STANDARDZOMBIE :
					field.addParticle(ParticleType.ZOMBIE, e.location,
							lifetime);
					break;
				case ZOMBIEDEN :
					field.addParticle(ParticleType.DEN, e.location, lifetime);
					break;
				default :
					throw new RuntimeException("Unknown type!");
			}
		}
	}
	
	public static double weakness(RobotInfo r) {
		double weakness = r.attackPower * (r.maxHealth - r.health + 1.0);
		return weakness;
	}

	public static void lookForNeutrals(RobotController rc, PotentialField field)
			throws GameActionException {
		return;
//		// Do sensing.
//		RobotInfo[] neutralArray = rc.senseNearbyRobots(
//				rc.getType().attackRadiusSquared, Team.NEUTRAL);
//		for (RobotInfo oneNeutral : neutralArray) {
//			field.addParticle(new ChargedParticle(1.0, oneNeutral.location, 2));
//		}
//		for (RobotInfo oneNeutral : neutralArray) {
//			if (!rc.isCoreReady()) {
//				break;
//			}
//			if (rc.getLocation().distanceSquaredTo(oneNeutral.location) <= 2) {
//				rc.activate(oneNeutral.location);
//			}
//		}
	}
}
