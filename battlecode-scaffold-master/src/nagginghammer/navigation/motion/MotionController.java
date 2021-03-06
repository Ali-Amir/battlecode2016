package nagginghammer.navigation.motion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nagginghammer.navigation.ChargedParticle;
import nagginghammer.navigation.PotentialField;
import nagginghammer.utils.Turn;
import nagginghammer.utils.Vector;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MotionController {
	// Potential field. Could be modified from outside this class.
	private final PotentialField field;

	public MotionController(PotentialField field) {
		this.field = field;
	}

	public boolean tryToMoveRandom(RobotController rc) throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}

		List<Direction> directions = new ArrayList<>(
				Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
		Collections.shuffle(directions);
		for (int i = 0; i < directions.size(); ++i) {
			Direction maybeForward = directions.get(i);
			if (rc.canMove(maybeForward)) {
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
			if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				rc.clearRubble(maybeForward);
				return true;
			}
		}

		return false;
	}

	public boolean tryToMove(RobotController rc) throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}

		if (field.particles().size() == 0) {
			List<Direction> directions = field.directionsByAttraction(rc.getLocation());
			for (int i = 0; i < directions.size(); ++i) {
				Direction maybeForward = directions.get(i);
				MapLocation ahead = rc.getLocation().add(maybeForward);
				if (rc.getType().canClearRubble() && rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(maybeForward);
					return true;
				}
			}
			return false;
		}

		List<Direction> directions = field.directionsByAttraction(rc.getLocation());

		{ // TODO
			Vector totalForce = new Vector(0, 0);
			for (ChargedParticle particle : field.particles()) {
				Vector newForce = particle.force(rc.getLocation());
				totalForce = new Vector(totalForce.x() + newForce.x(), totalForce.y() + newForce.y());
			}
			rc.setIndicatorString(1, "Turn: " + Turn.currentTurn() + " pos: " + rc.getLocation().x + " vector: ("
					+ totalForce.x() + "," + totalForce.y() + ")" + " decided dir: " + directions.get(0));
		}

		Collections.shuffle(directions.subList(0, 2));
		Collections.shuffle(directions.subList(2, 4));
		Collections.shuffle(directions.subList(4, directions.size()));
		for (int i = 0; i < 3; ++i) {
			Direction maybeForward = directions.get(i);
			if (rc.canMove(maybeForward)) {
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
			if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				rc.clearRubble(maybeForward);
				return true;
			}
		}

		return false;
	}
}
