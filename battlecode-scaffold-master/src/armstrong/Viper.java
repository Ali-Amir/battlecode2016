package armstrong;

import armstrong.navigation.PotentialField;
import armstrong.navigation.motion.MotionController;
import battlecode.common.RobotController;

public class Viper implements Player {

	private final PotentialField field;
	private final MotionController mc;
	
	public Viper(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) {
		// TODO Auto-generated method stub
		
	}

}
