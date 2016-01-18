package team316.utils;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class Encoding {

	public static int encodeLocation(MapLocation lc) {
		final int maxOffset = 16000;
		final int range = 2 * maxOffset;
		int x = lc.x;
		int y = lc.y;
		x += maxOffset;
		y += maxOffset;
		return (range + 1) * x + y;
	}

	public static MapLocation decodeLocation(int code) {
		final int maxOffset = 16000;
		final int range = 2 * maxOffset;
		int x = code / (range + 1);
		int y = code % (range + 1);
		return new MapLocation(x - maxOffset, y - maxOffset);
	}
	
	/**
	 * Gives locations IDs such that no robot and location have the same ID.
	 */
	public static int encodeLocationID(MapLocation lc) {
		final int maxRobotID = 	32000;
		return maxRobotID + 1 + encodeLocation(lc);
	}
	
	/**
	 * Decode a certain code for a given location ID encoded according to encodeLocationID.
	 */
	public static MapLocation decodeLocationID(int code) {
		final int maxRobotID = 	32000;
		return decodeLocation(code - (maxRobotID + 1));
	}

}
