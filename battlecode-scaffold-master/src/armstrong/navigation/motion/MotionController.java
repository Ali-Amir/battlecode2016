package armstrong.navigation.motion;

import java.util.List;

import armstrong.navigation.PotentialField;
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

	public boolean tryToMove(RobotController rc) throws GameActionException {
		if (!rc.isCoreReady()) {
			return false;
		}

		List<Direction> directions = field.directionsByAttraction(rc.getLocation());
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
