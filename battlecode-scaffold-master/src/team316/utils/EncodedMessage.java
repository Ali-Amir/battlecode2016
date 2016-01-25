package team316.utils;

import battlecode.common.MapLocation;

public class EncodedMessage {

	// Please, append new types to the end.
	public enum MessageType {
		EMPTY_MESSAGE, ZOMBIE_DEN_LOCATION, ENEMY_ARCHON_LOCATION, NEUTRAL_ARCHON_LOCATION, NEUTRAL_NON_ARCHON_LOCATION , MESSAGE_HELLO_ARCHON, MESSAGE_WELCOME_ACTIVATED_ARCHON,
		MESSAGE_HELP_ARCHON, Y_BORDER, X_BORDER, DEFENSE_MODE_ON, GATHER, ATTACK, ACTIVATE
	}

	/**
	 * Gets message type.
	 * 
	 * @param message
	 * @return
	 */
	public static MessageType getMessageType(int message) {
		return MessageType.values()[(message & 15)];
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	public static MapLocation getMessageLocation(int message) {
		final int twentyones = (1 << 20) - 1;
		final int location20bits = ((message >> 4) & twentyones);
		return decodeLocation20bits(location20bits);
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

	public static int makeMessage(MessageType messageType, MapLocation loc) {
		return messageType.ordinal() + (encodeLocation20bits(loc) << 4);
	}

	public static int encodeLocation20bits(MapLocation loc) {
		int encoding = (loc.x << 10) + loc.y;
		return encoding;
	}

	public static MapLocation decodeLocation20bits(int encoding) {
		return new MapLocation(encoding >> 10, encoding & 1023);
	}

}
