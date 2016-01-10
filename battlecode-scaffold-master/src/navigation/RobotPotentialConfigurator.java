package navigation;

import battlecode.common.MapLocation;

/**
 * Contains methods to create charged particles based on type of particle in
 * interest.
 * 
 * @author aliamir
 */
public interface RobotPotentialConfigurator {
	ChargedParticle particle(ParticleType type, MapLocation location, int lifetime);
}
