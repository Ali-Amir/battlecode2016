package armstrong;

import battlecode.common.RobotController;
import navigation.PotentialField;
import navigation.motion.MotionController;

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
