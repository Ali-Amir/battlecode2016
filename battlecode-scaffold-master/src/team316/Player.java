package team316;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public interface Player {
	void play(RobotController rc) throws GameActionException;
}
