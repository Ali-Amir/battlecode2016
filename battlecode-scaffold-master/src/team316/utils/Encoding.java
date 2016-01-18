package team316.utils;


import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Direction;

public class Encoding {

	public static int encodeLocation(MapLocation lc) {
		final int range = 580;
		int x = lc.x;
		int y = lc.y;
		return (range + 1) * x + y;
	}

	public static MapLocation decodeLocation(int code) {
		final int range = 580;
		int x = code / (range + 1);
		int y = code % (range + 1);
		return new MapLocation(x, y);
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
		final int offset = maxRobotID + 1;
		return decodeLocation(code - offset);
	}
	
	/**
	 * Encodes a border ID for a given map location on a border.
	 * 
	 * @param lc
	 * @return
	 */
	public static int encodeBorderID(MapLocation lc){
		final int offset = encodeLocationID(new MapLocation(580, 580)) + 1;
		return offset + encodeLocationID(lc);
	}
	
	public static MapLocation decodeBorderID(int code){
		final int offset = encodeLocationID(new MapLocation(580, 580)) + 1;
		return decodeLocationID(code - offset);
	}
}
