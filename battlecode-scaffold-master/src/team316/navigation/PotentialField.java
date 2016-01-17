package team316.navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import team316.utils.Turn;
import team316.RobotPlayer;
import team316.navigation.configurations.ArchonConfigurator;
import team316.navigation.configurations.GuardConfigurator;
import team316.navigation.configurations.ScoutConfigurator;
import team316.navigation.configurations.SoldierConfigurator;
import team316.navigation.configurations.TurretConfigurator;
import team316.navigation.configurations.ViperConfigurator;
import team316.utils.Vector;

public class PotentialField {
	// Configuration object that gives correct charged particles for each
	// observation.
	private final RobotPotentialConfigurator config;
	// List of observed particles in the field.
	private final List<ChargedParticle> particles;
	// List of IDs currently in particles.
	private final Set<Integer> currentIDs = new HashSet<>();
	// List of IDs to be removed in the next time directionsByAttraction is called.
	private final Set<Integer> removeIDWaitlist = new HashSet<>();
	
	private final Map<Integer, ChargedParticle> queuedParticles = new HashMap<>();
	public PotentialField(RobotPotentialConfigurator config) {
		this.config = config;
		particles = new LinkedList<>();
	}

	/**
	 * @return Potential field for an archon type robot.
	 */
	public static PotentialField archon() {
		return new PotentialField(new ArchonConfigurator());
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
	 * @param particle
	 *            New particle.
	 */
	public void addParticle(ChargedParticle particle) {
		particles.add(particle);
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
	public void addParticle(ParticleType type, MapLocation location,
			int lifetime) {
		particles.add(config.particle(type, location, lifetime));
	}

	/**
	 * Adds a new particle into the field.
	 * 
	 * @param id
	 *            ID of the particle.
	 * 
	 * @param type
	 *            Type of the particle.
	 * @param location
	 *            Map location of the particle.
	 * @param lifetime
	 *            Life time of the particle in turns.
	 */
	public void addParticle(int id, ParticleType type, MapLocation location,
			int lifetime) {
		if(!currentIDs.contains(id)){
			particles.add(config.particle(id, type, location, lifetime));
		}else{
			queuedParticles.put(id, config.particle(id, type, location, lifetime));
		}
	}

	public void removeParticleByID(int id) {
		if(currentIDs.contains(id)){
			removeIDWaitlist.add(id);
			queuedParticles.remove(id);			
		}
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
			double randomXAdjustment = RobotPlayer.rnd.nextDouble() / 100.0;
			double randomYAdjustment = RobotPlayer.rnd.nextDouble() / 100.0;
			totalForce = new Vector(
					totalForce.x() + newForce.x() + randomXAdjustment,
					totalForce.y() + newForce.y() + randomYAdjustment);
		}

		List<Direction> directions = new ArrayList<>(Arrays.asList(
				Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
				Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
				Direction.WEST, Direction.NORTH_WEST));
		final int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
		final int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
		List<Integer> p = new ArrayList<>(
				Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
		Collections.shuffle(p, RobotPlayer.rnd);
		final Vector finalForce = totalForce;
		Collections.sort(p, (a, b) -> {
			double dValue = (dx[b] * finalForce.x() + dy[b] * finalForce.y())
					/ Math.sqrt(dx[b] * dx[b] + dy[b] * dy[b])
					- (dx[a] * finalForce.x() + dy[a] * finalForce.y())
							/ Math.sqrt(dx[a] * dx[a] + dy[a] * dy[a]);
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
			int id = particles.get(i).getID();
			if (!particles.get(i).isAlive() || removeIDWaitlist.contains(id)) {
				currentIDs.remove(id);
				particles.remove(i);
				if(queuedParticles.containsKey(id)){
					particles.add(queuedParticles.get(id));
				}

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
