package team316;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;

public class Viper extends Soldier {

	public Viper(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		super(archonLoc, field, mc, rc);
	}

}
