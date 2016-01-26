package team316.utils;

import team316.RobotPlayer;

public class Turn {
	public static int currentTurn() {
		return RobotPlayer.rc.getRoundNum();
	}

	public static int turnsSince(int turn) {
		return RobotPlayer.rc.getRoundNum() - turn;
	}
}
