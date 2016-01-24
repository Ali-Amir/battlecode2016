package team316.utils;

import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import team316.RobotPlayer;
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
					break;
				case SCOUT :
					break;
				case SOLDIER :
					break;
				case VIPER :
					// field.addParticle(ParticleType.ALLY_DEFAULT, e.location,
					// lifetime);
					break;
				default :
					throw new RuntimeException("Unknown type!");
			}
		}
	}

	public static boolean addScaryParticles(RobotInfo[] scaryArray,
			PotentialField field, int lifetime) {
		boolean added = false;
		for (RobotInfo s : scaryArray) {
			switch (s.type) {
				case SOLDIER :
					break;
				/*
				 * if (!RobotPlayer.rcWrapper.isUnderAttack()) { break; } added
				 * = true; field.addParticle( new ChargedParticle(-100.0,
				 * s.location, lifetime)); break;
				 */
				case RANGEDZOMBIE :
					/*
					 * if (!RobotPlayer.rcWrapper.isUnderAttack()) { break; }
					 * added = true; field.addParticle( new
					 * ChargedParticle(-100.0, s.location, lifetime));
					 */
					break;
				case ARCHON :
					break;
				case ZOMBIEDEN :
					break;
				case TURRET :
					break;
				case TTM :
					break;
				case GUARD :
					if (s.location.distanceSquaredTo(
							RobotPlayer.rc.getLocation()) >= 6) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(-100.0, s.location, lifetime));
					break;
				case BIGZOMBIE :
					if (s.location.distanceSquaredTo(
							RobotPlayer.rc.getLocation()) >= 6) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(-100.0, s.location, lifetime));
					break;
				case STANDARDZOMBIE :
					if (s.location.distanceSquaredTo(
							RobotPlayer.rc.getLocation()) >= 6) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(-100.0, s.location, lifetime));
					break;
				case SCOUT :
					break;
				default :
					added = true;
					field.addParticle(
							new ChargedParticle(-100.0, s.location, lifetime));
			}
		}
		return added;
	}

	public static void addEnemyParticles(List<RobotInfo> enemyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : enemyArray) {
			addEnemyParticle(e, field, lifetime);
		}
	}

	public static void addEnemyParticles(RobotInfo[] enemyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : enemyArray) {
			addEnemyParticle(e, field, lifetime);
		}
	}

	public static void addEnemyParticle(RobotInfo e, PotentialField field,
			int lifetime) {
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
				break;
			case FASTZOMBIE :
				break;
			case RANGEDZOMBIE :
				break;
			case STANDARDZOMBIE :
				field.addParticle(ParticleType.ZOMBIE, e.location, lifetime);
				break;
			case ZOMBIEDEN :
				field.addParticle(ParticleType.DEN, e.location, lifetime);
				break;
			default :
				throw new RuntimeException("Unknown type!");
		}
	}

	public static double weakness(RobotInfo r) {
		double weakness = r.attackPower / (r.health + 1.0) / r.maxHealth;
		return weakness;
	}

	public static void lookForNeutrals(RobotController rc, PotentialField field)
			throws GameActionException {
		return;
	}

	public static void addBorderParticles(RCWrapper rcWrapper,
			PotentialField field) throws GameActionException {
		Direction[] directions = {Direction.NORTH, Direction.SOUTH,
				Direction.EAST, Direction.WEST};
		final int fromDistance = 1;
		final int width = 1;
		int x = rcWrapper.getCurrentLocation().x;
		int y = rcWrapper.getCurrentLocation().y;
		for (Direction direction : directions) {
			Integer c = rcWrapper.getMaxCoordinate(direction);
			if (c == null || c == -1) {
				continue;
			}
			MapLocation location;
			int charge = -1000;
			// number of charges we add; must be odd.
			boolean bordersAreClose = false;
			boolean isVertical = (direction.equals(Direction.NORTH)
					|| direction.equals(Direction.SOUTH));
			if (isVertical) {
				bordersAreClose = (c - y <= fromDistance)
						&& (y - c <= fromDistance);
			} else {
				bordersAreClose = (c - x <= fromDistance)
						&& (x - c <= fromDistance);
			}
			if (!bordersAreClose) {
				return;
			}
			for (int i = -width / 2; i <= width / 2; i++) {
				if (isVertical) {
					location = new MapLocation(x + i, c);
				} else {
					location = new MapLocation(c, y + i);
				}
				// We put the charge one tile off the map.
				location = location.add(direction);
				field.addParticle(new ChargedParticle(charge, location, 1));
			}

		}
	}

	public static RobotInfo[] robotsWhoCanAttackLocation(MapLocation loc,
			RobotInfo[] robots) {
		int count = 0;
		for (RobotInfo r : robots) {
			if (r.location
					.distanceSquaredTo(loc) <= r.type.attackRadiusSquared) {
				++count;
			}
		}

		RobotInfo[] attackers = new RobotInfo[count];
		int index = 0;
		for (RobotInfo r : robots) {
			if (r.location
					.distanceSquaredTo(loc) <= r.type.attackRadiusSquared) {
				attackers[index++] = r;
			}
		}
		return attackers;
	}

	public static MapLocation centerOfMassPlusPoint(RobotInfo[] robots,
			MapLocation self) {
		double x = self.x, y = self.y;
		int sum = 1;
		for (RobotInfo r : robots) {
			x += r.location.x;
			y += r.location.y;
			++sum;
		}
		return new MapLocation((int) (x / sum), (int) (y / sum));
	}

}
