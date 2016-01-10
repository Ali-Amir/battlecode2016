package navigation.motion;

import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import navigation.PotentialField;

public class MotionController {
	// Potential field. Could be modified from outside this class.
	private final PotentialField field;

	public MotionController(PotentialField field) {
		this.field = field;
	}

	public boolean tryToMove(RobotController rc) throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}

		List<Direction> directions = field.directionsByAttraction(rc.getLocation());
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

		// failed to move, look to clear rubble
		Direction forward = field.strongetAttractionDirection(rc.getLocation());
		MapLocation ahead = rc.getLocation().add(forward);
		if (rc.senseRubble(ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
			rc.clearRubble(forward);
		}
		return true;
	}
}
