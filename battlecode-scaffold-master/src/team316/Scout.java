package team316;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Encoding;

public class Scout implements Player {

	private final PotentialField field;
	private final MotionController mc;
	private final MapLocation archonLoc;

	public Scout(MapLocation archonLoc, PotentialField field, MotionController mc) {
		this.archonLoc = archonLoc;
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
	}

}
