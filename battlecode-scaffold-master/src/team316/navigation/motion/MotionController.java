package team316.navigation.motion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import team316.navigation.PotentialField;

public class MotionController {
	// Potential field. Could be modified from outside this class.
	private final PotentialField field;

	public MotionController(PotentialField field) {
		this.field = field;
	}

	public boolean tryToMoveRandom(RobotController rc)
			throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}

		List<Direction> directions = new ArrayList<>(
				Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH,
						Direction.WEST));
		Collections.shuffle(directions);
		for (int i = 0; i < directions.size(); ++i) {
			Direction maybeForward = directions.get(i);
			if (rc.canMove(maybeForward)
					&& rc.onTheMap(rc.getLocation().add(maybeForward))) {
				rc.move(maybeForward);
				return true;
			}
		}

		if (!rc.getType().canClearRubble()) {
			return false;
		}

		for (int i = 0; i < directions.size(); ++i) {
			Direction maybeForward = directions.get(i);
			MapLocation ahead = rc.getLocation().add(maybeForward);
			if (rc.senseRubble(
					ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				rc.clearRubble(maybeForward);
				return true;
			}
		}

		return false;
	}

	public boolean tryToMove(RobotController rc) throws GameActionException {
		return tryToMove(rc, rc.getLocation());
	}

	public boolean tryToMove(RobotController rc, MapLocation reference)
			throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}

		if (field.numParticles == 0) {
			int[] directions = field.directionsByAttraction(reference);
			for (int i = 0; i < directions.length; ++i) {
				Direction maybeForward = Direction.values()[directions[i]];
				MapLocation ahead = rc.getLocation().add(maybeForward);
				if (rc.getType().canClearRubble() && rc.senseRubble(
						ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(maybeForward);
					return true;
				}
			}
			return false;
		}

		int[] directions = field.directionsByAttraction(reference);
		for (int i = 0; i < directions.length; ++i) {
			Direction maybeForward = Direction.values()[directions[i]];
			if (!rc.getType().equals(RobotType.SCOUT)) {
				MapLocation ahead = rc.getLocation().add(maybeForward);
				if (rc.isCoreReady()
						&& rc.senseRubble(
								ahead) >= GameConstants.RUBBLE_SLOW_THRESH
						&& rc.senseRubble(ahead) <= 1000) {
					rc.clearRubble(maybeForward);
				}
			}
			if (rc.isCoreReady() && rc.canMove(maybeForward)
					&& rc.onTheMap(rc.getLocation().add(maybeForward))) {
				rc.move(maybeForward);
				return true;
			}
		}

		return false;
	}
	

	public boolean fallBack(RobotController rc)
			throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}
		
		MapLocation reference = rc.getLocation();

		if (field.numParticles == 0) {
			int[] directions = field.directionsByAttraction(reference);
			for (int i = 0; i < directions.length; ++i) {
				Direction maybeForward = Direction.values()[directions[i]];
				MapLocation ahead = rc.getLocation().add(maybeForward);
				if (rc.getType().canClearRubble() && rc.senseRubble(
						ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(maybeForward);
					return true;
				}
			}
			return false;
		}

		int[] directions = field.directionsByAttraction(reference);
		for (int i = 0; i < 4; ++i) {
			Direction maybeForward = Direction.values()[directions[i]];
			if (rc.isCoreReady() && rc.canMove(maybeForward)
					&& rc.onTheMap(rc.getLocation().add(maybeForward))) {
				rc.move(maybeForward);
				return true;
			}
		}
		
		for (int i = 0; i < directions.length; ++i) {
			Direction maybeForward = Direction.values()[directions[i]];
			if (!rc.getType().equals(RobotType.SCOUT)) {
				MapLocation ahead = rc.getLocation().add(maybeForward);
				if (rc.isCoreReady()
						&& rc.senseRubble(
								ahead) >= GameConstants.RUBBLE_SLOW_THRESH
						&& rc.senseRubble(ahead) <= 1000) {
					rc.clearRubble(maybeForward);
				}
			}
		}

		return false;
	}
}
