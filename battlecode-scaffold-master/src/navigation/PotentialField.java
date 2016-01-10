package navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class PotentialField {
	// Configuration object that gives correct charged particles for each
	// observation.
	private final RobotPotentialConfigurator config;
	// List of observed particles in the field.
	private final List<ChargedParticle> particles;

	public PotentialField(RobotPotentialConfigurator config) {
		this.config = config;
		particles = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * @return Potential field for a soldier type robot.
	 */
	static PotentialField soldier() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * @return Potential field for a turret type robot.
	 */
	static PotentialField turret() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * @return Potential field for a viper type robot.
	 */
	static PotentialField viper() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * @return Potential field for a guard type robot.
	 */
	static PotentialField guard() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * @return Potential field for a scout type robot.
	 */
	static PotentialField scout() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * Adds a new particle into the field.
	 * 
	 * @param type
	 *            Type of the particle.
	 * @param location
	 *            Map location of the particle.
	 * @param lifetime
	 *            Life time of the particle in turns.
	 */
	public void addParticle(ParticleType type, MapLocation location, int lifetime) {
		particles.add(config.particle(type, location, lifetime));
	}

	/**
	 * @return Directions sorted by attraction force. Strongest attraction
	 *         direction is first.
	 */
	List<Direction> directionsByAttraction(MapLocation to) {
		Vector totalForce = new Vector(0, 0);
		for (ChargedParticle particle : particles) {
			Vector newForce = particle.force(to);
			totalForce = new Vector(totalForce.x() + newForce.x(), totalForce.y() + newForce.y());
		}
		throw new RuntimeException("unimplemented");
		// List<Direction> directions = Arrays.asList(a)
		// Direction.NORTH
		// return
	}

}
