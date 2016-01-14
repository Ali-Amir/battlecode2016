package team316.navigation;

import battlecode.common.MapLocation;

/**
 * Contains methods to create charged particles based on type of particle in
 * interest.
 * 
 * @author aliamir
 */
public abstract class RobotPotentialConfigurator {
	/**
	 * Creates a new particle with given parameters.
	 * 
	 * @param type
	 *            Type of the particle. See ParticleType enum.
	 * @param location
	 *            Location of the particle on the map.
	 * @param lifetime
	 *            Lifetime of the particle.
	 * @return New particle.
	 */
	public ChargedParticle particle(ParticleType type, MapLocation location, int lifetime) {
		ChargedParticle result;
		switch (type) {
		case OPPOSITE_ARCHON:
			result = new ChargedParticle(oppositeArchonCharge(), location, lifetime);
			break;
		case OPPOSITE_GUARD:
			result = new ChargedParticle(oppositeGuardCharge(), location, lifetime);
			break;
		case OPPOSITE_SOLDIER:
			result = new ChargedParticle(oppositeSoldierCharge(), location, lifetime);
			break;
		case OPPOSITE_VIPER:
			result = new ChargedParticle(oppositeViperCharge(), location, lifetime);
			break;
		case OPPOSITE_SCOUT:
			result = new ChargedParticle(oppositeScoutCharge(), location, lifetime);
			break;
		case OPPOSITE_TURRET:
			result = new ChargedParticle(oppositeTurretCharge(), location, lifetime);
			break;
		case ALLY_ARCHON:
			result = new ChargedParticle(allyArchonCharge(), location, lifetime);
			break;
		case ALLY_TURRET:
			result = new ChargedParticle(allyTurretCharge(), location, lifetime);
			break;
		case FIGHTING_ALLY:
			result = new ChargedParticle(fightingAllyCharge(), location, lifetime);
			break;
		case ZOMBIE:
			result = new ChargedParticle(zombieCharge(), location, lifetime);
			break;
		case DEN:
			result = new ChargedParticle(denCharge(), location, lifetime);
			break;
		case ARCHON_ATTACKED:
			result = new ChargedParticle(6, location, lifetime);
			break;
		default:
			System.out.println("UNKNOWN TYPE: " + type + ". Using default configuration.");
			result = new ChargedParticle(defaultCharge(), location, lifetime);
			break;
		}
		return result;
	}
	
	protected abstract double oppositeArchonCharge();
	
	protected abstract double oppositeGuardCharge();
	
	protected abstract double oppositeSoldierCharge();
	
	protected abstract double oppositeViperCharge();
	
	protected abstract double oppositeScoutCharge();
	
	protected abstract double oppositeTurretCharge();
	
	protected abstract double allyArchonCharge();
	
	protected abstract double allyTurretCharge();
	
	protected abstract double fightingAllyCharge();
	
	protected abstract double zombieCharge();
	
	protected abstract double denCharge();
	
	protected abstract double defaultCharge();
	
	protected abstract double allyDefaultCharge();
}
