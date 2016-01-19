package team316.utils;

import battlecode.common.MapLocation;

public class EncodedMessage {

	// Please, append new types to the end.
	public enum MessageType {
		EMPTY_MESSAGE, ZOMBIE_DEN_LOCATION
	}

	/**
	 * @param loc
	 *            Location of the zombie den.
	 * @return Message encoding zombie den location.
	 */
	public static int zombieDenLocation(MapLocation loc) {
		return MessageType.ZOMBIE_DEN_LOCATION.ordinal()
				+ (encodeLocation20bits(loc) << 4);
	}

	public static int encodeLocation20bits(MapLocation loc) {
		// To get x back do: (encoding >> 10).
		// To get y back do: (encoding & 1023).
		int encoding = (loc.x << 10) + loc.y;
		return encoding;
	}
}
