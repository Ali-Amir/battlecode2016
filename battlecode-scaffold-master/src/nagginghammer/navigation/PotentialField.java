package nagginghammer.navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nagginghammer.RobotPlayer;
import nagginghammer.navigation.configurations.GuardConfigurator;
import nagginghammer.navigation.configurations.ScoutConfigurator;
import nagginghammer.navigation.configurations.SoldierConfigurator;
import nagginghammer.navigation.configurations.TurretConfigurator;
import nagginghammer.navigation.configurations.ViperConfigurator;
import nagginghammer.utils.Vector;
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
	public static PotentialField soldier() {
		return new PotentialField(new SoldierConfigurator());
	}

	/**
	 * @return Potential field for a turret type robot.
	 */
	public static PotentialField turret() {
		return new PotentialField(new TurretConfigurator());
	}

	/**
	 * @return Potential field for a viper type robot.
	 */
	public static PotentialField viper() {
		return new PotentialField(new ViperConfigurator());
	}

	/**
	 * @return Potential field for a guard type robot.
	 */
	public static PotentialField guard() {
		return new PotentialField(new GuardConfigurator());
	}

	/**
	 * @return Potential field for a scout type robot.
	 */
	public static PotentialField scout() {
		return new PotentialField(new ScoutConfigurator());
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
	 * @return Directions with most attraction.
	 */
	public Direction strongetAttractionDirection(MapLocation to) {
		return directionsByAttraction(to).get(0);
	}

	/**
	 * @return Directions sorted by attraction force. Strongest attraction
	 *         direction is first.
	 */
	public List<Direction> directionsByAttraction(MapLocation to) {
		discardDeadParticles();

		Vector totalForce = new Vector(0, 0);
		for (ChargedParticle particle : particles) {
			Vector newForce = particle.force(to);
			totalForce = new Vector(totalForce.x() + newForce.x(), totalForce.y() + newForce.y());
		}

		List<Direction> directions = new ArrayList<>(
				Arrays.asList(Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
						Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST));
		final int[] dx = { 0, 1, 1, 1, 0, -1, -1, -1 };
		final int[] dy = { -1, -1, 0, 1, 1, 1, 0, -1 };
		List<Integer> p = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
		Collections.shuffle(p, RobotPlayer.rnd);
		final Vector finalForce = totalForce;
		Collections.sort(p, (a, b) -> {
			double dValue = (dx[b] * finalForce.x() + dy[b] * finalForce.y()) / Math.sqrt(dx[b] * dx[b] + dy[b] * dy[b])
					- (dx[a] * finalForce.x() + dy[a] * finalForce.y()) / Math.sqrt(dx[a] * dx[a] + dy[a] * dy[a]);
			return dValue < 0 ? -1 : dValue > 0 ? 1 : 0;
		});

		List<Direction> sortedDirections = new ArrayList<>();
		for (int i = 0; i < p.size(); ++i) {
			sortedDirections.add(directions.get(p.get(i)));
		}
		return sortedDirections;
	}

	/**
	 * Discards particles that are not alive.
	 */
	private void discardDeadParticles() {
		for (int i = 0; i < particles.size(); ++i) {
			if (!particles.get(i).isAlive()) {
				particles.remove(i);
				--i;
			}
		}
	}

	public List<ChargedParticle> particles() {
		return Collections.unmodifiableList(particles);
	}

	@Override
	public String toString() {
		String res = "{";
		for (ChargedParticle q : particles) {
			res += q.toString() + ",";
		}
		return res + "}";
	}

}
