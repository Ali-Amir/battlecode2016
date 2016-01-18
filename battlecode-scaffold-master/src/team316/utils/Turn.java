package team316.utils;

import team316.RobotPlayer;

public class Turn {
	public static int currentTurn() {
		return RobotPlayer.rc.getRoundNum();
	}
}
