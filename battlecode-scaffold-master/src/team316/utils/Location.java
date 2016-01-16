package team316.utils;

import battlecode.common.MapLocation;

public class Location {

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


}
