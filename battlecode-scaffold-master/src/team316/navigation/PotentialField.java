package team316.navigation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import team316.RobotPlayer;
import team316.navigation.configurations.ArchonConfigurator;
import team316.navigation.configurations.GuardConfigurator;
import team316.navigation.configurations.ScoutConfigurator;
import team316.navigation.configurations.SoldierConfigurator;
import team316.navigation.configurations.TurretConfigurator;
import team316.navigation.configurations.ViperConfigurator;
import team316.utils.Turn;
import team316.utils.Vector;

public class PotentialField {
	private final static int PARTICLE_LIMIT = 20;
	private final static int COMPRESSION_DISTANCE = 20;

	private final static double SQRT2 = Math.sqrt(2.0);
	// Configuration object that gives correct charged particles for each
	// observation.
	private final RobotPotentialConfigurator config;
	// List of observed particles in the field.
	private final ChargedParticle[] particles;
	public int numParticles;
	// List of IDs currently in particles.
	// private final Set<Integer> currentIDs = new HashSet<>();
	// List of IDs to be removed in the next time directionsByAttraction is
	// called.
	// private final Set<Integer> removeIDWaitlist = new HashSet<>();

	// private final Map<Integer, ChargedParticle> queuedParticles = new
	// HashMap<>();
	public PotentialField(RobotPotentialConfigurator config) {
		this.config = config;

		particles = new ChargedParticle[20 * PARTICLE_LIMIT];
		numParticles = 0;
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
		if (numParticles + 1 >= PARTICLE_LIMIT) {
			compressParticles();
		}

		particles[numParticles] = particle;
		++numParticles;
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
		if (numParticles + 1 >= PARTICLE_LIMIT) {
			compressParticles();
		}

		particles[numParticles] = config.particle(type, location, lifetime);
		++numParticles;
	}

	/**
	 * @return Directions with most attraction.
	 */
	public Direction strongetAttractionDirection(MapLocation to) {
		return Direction.values()[directionsByAttraction(to)[0]];
	}

	/**
	 * @return Directions sorted by attraction force. Strongest attraction
	 *         direction is first.
	 */
	public int[] directionsByAttraction(MapLocation to) {
		discardDeadParticles();

		Vector totalForce = new Vector(0, 0);
		for (int i = 0; i < numParticles; ++i) {
			ChargedParticle particle = particles[i];
			Vector newForce = particle.force(to);
			double randomXAdjustment = RobotPlayer.rnd.nextDouble() / 1000000.0;
			double randomYAdjustment = RobotPlayer.rnd.nextDouble() / 1000000.0;
			totalForce = new Vector(
					totalForce.x() + newForce.x() + randomXAdjustment,
					totalForce.y() + newForce.y() + randomYAdjustment);
		}

		final int[] directions = new int[]{Direction.NORTH.ordinal(),
				Direction.NORTH_EAST.ordinal(), Direction.EAST.ordinal(),
				Direction.SOUTH_EAST.ordinal(), Direction.SOUTH.ordinal(),
				Direction.SOUTH_WEST.ordinal(), Direction.WEST.ordinal(),
				Direction.NORTH_WEST.ordinal()};
		final int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
		final int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};

		double strongestAttraction = -(1e9);
		int strongestDir = -1;
		for (int i = 0; i < directions.length; ++i) {
			double currentAttraction = (dx[i] * totalForce.x()
					+ dy[i] * totalForce.y());
			if (i % 2 == 1) {
				currentAttraction /= SQRT2;
			}

			if (currentAttraction > strongestAttraction) {
				strongestAttraction = currentAttraction;
				strongestDir = i;
			}
		}

		int[] sortedDirections = new int[directions.length];
		sortedDirections[0] = directions[strongestDir];
		for (int dir = RobotPlayer.rnd.nextBoolean() == true
				? 1
				: -1, i = 1; i < 8; ++i, dir = -dir) {
			int curDir = (strongestDir + ((i + 1) / 2) * dir + 8) & 7;
			sortedDirections[i] = directions[curDir];
		}
		return sortedDirections;
	}

	/**
	 * Discards particles that are not alive.
	 */
	private void discardDeadParticles() {
		for (int i = 0; i < numParticles; ++i) {
			if (!particles[i].isAlive()) {
				particles[i] = particles[numParticles - 1];
				particles[numParticles - 1] = null;
				--numParticles;
				--i;
			}
		}
	}

	/**
	 * Compresses neighboring particles into one.
	 */
	private void compressParticles() {
		discardDeadParticles();

		final int curTurn = Turn.currentTurn();
		for (int i = 0; i < numParticles; ++i) {
			double x = particles[i].location.y, y = particles[i].location.y;
			int total = 1;
			double totalCharge = particles[i].charge;
			int maxEndTurn = particles[i].expiryTurn;
			for (int j = i + 1; j < numParticles; ++j) {
				if (particles[i].location.distanceSquaredTo(
						particles[j].location) < COMPRESSION_DISTANCE) {
					total += 1;
					totalCharge += (particles[j].expiryTurn - curTurn)
							* particles[j].charge;
					if (maxEndTurn < particles[j].expiryTurn) {
						maxEndTurn = particles[j].expiryTurn;
					}
					x += particles[i].location.x;
					y += particles[i].location.y;

					particles[j] = particles[--numParticles];
					--j;
				}
			}
			particles[i].location = new MapLocation((int) (x / total),
					(int) (y / total));
			// particles[i].charge = totalCharge / total / (maxEndTurn -
			// curTurn);
			particles[i].charge = totalCharge / (maxEndTurn - curTurn);
			particles[i].expiryTurn = maxEndTurn;
		}
	}

	public List<ChargedParticle> particles() {
		return Collections.unmodifiableList(
				Arrays.asList(particles).subList(0, numParticles));
	}

	@Override
	public String toString() {
		String res = "{";
		for (int i = 0; i < numParticles; ++i) {
			res += particles[i].toString() + ",";
		}
		return res + "}";
	}

}
