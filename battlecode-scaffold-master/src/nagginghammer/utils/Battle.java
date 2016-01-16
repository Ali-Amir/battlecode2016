package nagginghammer.utils;

import nagginghammer.navigation.ParticleType;
import nagginghammer.navigation.PotentialField;
import battlecode.common.RobotInfo;

public class Battle {
	public static void addAllyParticles(RobotInfo[] allyArray, PotentialField field, int lifetime) {
		for (RobotInfo e : allyArray) {
			switch (e.type) {
			case ARCHON:
				field.addParticle(ParticleType.ALLY_ARCHON, e.location, lifetime);
				break;
			case TTM:
				field.addParticle(ParticleType.ALLY_TURRET, e.location, lifetime);
				break;
			case TURRET:
				field.addParticle(ParticleType.ALLY_TURRET, e.location, lifetime);
				break;
			case GUARD:
			case SCOUT:
			case SOLDIER:
			case VIPER:
				//field.addParticle(ParticleType.ALLY_DEFAULT, e.location, lifetime);
				break;
			default:
				throw new RuntimeException("Unknown type!");
			}
		}
	}
	
	public static void addEnemyParticles(RobotInfo[] enemyArray, PotentialField field, int lifetime) {
		for (RobotInfo e : enemyArray) {
			switch (e.type) {
			case ARCHON:
				field.addParticle(ParticleType.OPPOSITE_ARCHON, e.location, lifetime);
				break;
			case GUARD:
				field.addParticle(ParticleType.OPPOSITE_GUARD, e.location, lifetime);
				break;
			case SCOUT:
				field.addParticle(ParticleType.OPPOSITE_SCOUT, e.location, lifetime);
				break;
			case TTM:
				field.addParticle(ParticleType.OPPOSITE_TURRET, e.location, lifetime);
				break;
			case TURRET:
				field.addParticle(ParticleType.OPPOSITE_TURRET, e.location, lifetime);
				break;
			case SOLDIER:
				field.addParticle(ParticleType.OPPOSITE_SOLDIER, e.location, lifetime);
				break;
			case VIPER:
				field.addParticle(ParticleType.OPPOSITE_VIPER, e.location, lifetime);
				break;
			case BIGZOMBIE:
			case FASTZOMBIE:
			case RANGEDZOMBIE:
			case STANDARDZOMBIE:
				field.addParticle(ParticleType.ZOMBIE, e.location, lifetime);
				break;
			case ZOMBIEDEN:
				field.addParticle(ParticleType.DEN, e.location, lifetime);
				break;
			default:
				throw new RuntimeException("Unknown type!");
			}
		}
	}
	
	public static double weakness(RobotInfo r) {
		double weakness = r.maxHealth - r.health;
		return weakness;
	}
}
