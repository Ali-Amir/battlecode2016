package team316;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;

public class Viper implements Player {

	private final PotentialField field;
	private final MotionController mc;
	private final MapLocation archonLoc;
	
	public Viper(MapLocation archonLoc, PotentialField field, MotionController mc) {
		this.archonLoc = archonLoc;
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) {
		// TODO Auto-generated method stub
		
	}

}
