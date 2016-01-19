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

	public static void addUniqueAllyParticles(RobotInfo[] allyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : allyArray) {
			switch (e.type) {
				case ARCHON :
					field.addParticle(e.ID, ParticleType.ALLY_ARCHON,
							e.location, lifetime);
					break;
				case TTM :
					field.addParticle(e.ID, ParticleType.ALLY_TURRET,
							e.location, lifetime);
					break;
				case TURRET :
					field.addParticle(e.ID, ParticleType.ALLY_TURRET,
							e.location, lifetime);
					break;
				case GUARD :
					field.addParticle(e.ID, ParticleType.ALLY_DEFAULT,
							e.location, lifetime);
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
					if (!RobotPlayer.rcWrapper.isUnderAttack()) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(-100.0, s.location, lifetime));
					break;
				case RANGEDZOMBIE :
					if (!RobotPlayer.rcWrapper.isUnderAttack()) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(-100.0, s.location, lifetime));
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

	public static boolean addUniqueScaryParticles(List<RobotInfo> scaryArray,
			PotentialField field, int lifetime) {
		boolean added = false;
		for (RobotInfo s : scaryArray) {
			switch (s.type) {
				case SOLDIER :
					if (!RobotPlayer.rcWrapper.isUnderAttack()) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(s.ID, -100.0, s.location, lifetime));
					break;
				case RANGEDZOMBIE :
					if (!RobotPlayer.rcWrapper.isUnderAttack()) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(s.ID, -100.0, s.location, lifetime));
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
							new ChargedParticle(s.ID, -100.0, s.location, lifetime));
					break;
				case BIGZOMBIE :
					if (s.location.distanceSquaredTo(
							RobotPlayer.rc.getLocation()) >= 6) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(s.ID, -100.0, s.location, lifetime));
					break;
				case STANDARDZOMBIE :
					if (s.location.distanceSquaredTo(
							RobotPlayer.rc.getLocation()) >= 6) {
						break;
					}
					added = true;
					field.addParticle(
							new ChargedParticle(s.ID, -100.0, s.location, lifetime));
					break;
				default :
					added = true;
					field.addParticle(
							new ChargedParticle(s.ID, -100.0, s.location, lifetime));
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

	public static void addUniqueEnemyParticles(List<RobotInfo> enemyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : enemyArray) {
			addUniqueEnemyParticle(e, field, lifetime);
		}
	}

	public static void addUniqueEnemyParticles(RobotInfo[] enemyArray,
			PotentialField field, int lifetime) {
		for (RobotInfo e : enemyArray) {
			addUniqueEnemyParticle(e, field, lifetime);
		}
	}
	public static void addUniqueEnemyParticle(RobotInfo e, PotentialField field,
			int lifetime) {
		switch (e.type) {
			case ARCHON :
				field.addParticle(e.ID, ParticleType.OPPOSITE_ARCHON,
						e.location, lifetime);
				break;
			case GUARD :
				field.addParticle(e.ID, ParticleType.OPPOSITE_GUARD, e.location,
						lifetime);
				break;
			case SCOUT :
				field.addParticle(e.ID, ParticleType.OPPOSITE_SCOUT, e.location,
						lifetime);
				break;
			case TTM :
				field.addParticle(e.ID, ParticleType.OPPOSITE_TURRET,
						e.location, lifetime);
				break;
			case TURRET :
				field.addParticle(e.ID, ParticleType.OPPOSITE_TURRET,
						e.location, lifetime);
				break;
			case SOLDIER :
				field.addParticle(e.ID, ParticleType.OPPOSITE_SOLDIER,
						e.location, lifetime);
				break;
			case VIPER :
				field.addParticle(e.ID, ParticleType.OPPOSITE_VIPER, e.location,
						lifetime);
				break;
			case BIGZOMBIE :
				field.addParticle(e.ID, ParticleType.ZOMBIE, e.location,
						lifetime);
				break;
			case FASTZOMBIE :
				field.addParticle(e.ID, ParticleType.ZOMBIE, e.location,
						lifetime);
				break;
			case RANGEDZOMBIE :
				field.addParticle(e.ID, ParticleType.ZOMBIE, e.location,
						lifetime);
				break;
			case STANDARDZOMBIE :
				field.addParticle(e.ID, ParticleType.ZOMBIE, e.location,
						lifetime);
				break;
			case ZOMBIEDEN :
				field.addParticle(e.ID, ParticleType.DEN, e.location, lifetime);
				break;
			default :
				throw new RuntimeException("Unknown type!");
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
		// // Do sensing.
		// RobotInfo[] neutralArray = rc.senseNearbyRobots(
		// rc.getType().attackRadiusSquared, Team.NEUTRAL);
		// for (RobotInfo oneNeutral : neutralArray) {
		// field.addParticle(new ChargedParticle(1.0, oneNeutral.location, 2));
		// }
		// for (RobotInfo oneNeutral : neutralArray) {
		// if (!rc.isCoreReady()) {
		// break;
		// }
		// if (rc.getLocation().distanceSquaredTo(oneNeutral.location) <= 2) {
		// rc.activate(oneNeutral.location);
		// }
		// }
	}
	public static void addUniqueBorderParticles(RCWrapper rcWrapper, PotentialField field) throws GameActionException{
		Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		final int fromDistance = 2;
		for(Direction direction: directions){
			Integer c = rcWrapper.getMaxCoordinate(direction);
			if(c == null){
				continue;
			}
			//System.out.println("Direction" + direction + ", c = " + c);
			int x = rcWrapper.getCurrentLocation().x;			
			int y = rcWrapper.getCurrentLocation().y;
			MapLocation location;
			int charge = -1000;
			if(direction.equals(Direction.NORTH) || direction.equals(Direction.SOUTH)){
				if(c - y <= fromDistance && y - c <= fromDistance){
					location = new MapLocation(x, c);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					/*
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.EAST, 1);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.EAST, 2);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.WEST, 1);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.WEST, 2);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));					
					*/
				}
			}else{
				if(c - x <= fromDistance && x -c <= fromDistance){
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					/*
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.NORTH, 1);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.NORTH, 2);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.SOUTH, 1);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					location = new MapLocation(c, rcWrapper.getCurrentLocation().y).add(Direction.SOUTH, 2);
					field.addParticle(new ChargedParticle(Encoding.encodeBorderID(location), charge, location, 1));
					*/
				}
			}
			
		}
		
	}

}
