package team316.utils;

import battlecode.common.MapLocation;

public class EncodedMessage {

	public enum MessageType {
		EMPTY_MESSAGE, ZOMBIE_DEN_LOCATION, ENEMY_ARCHON_LOCATION, NEUTRAL_ARCHON_LOCATION,
		NEUTRAL_NON_ARCHON_LOCATION, MESSAGE_HELLO_ARCHON, MESSAGE_WELCOME_ACTIVATED_ARCHON, MESSAGE_HELP_ARCHON,
		Y_BORDER, X_BORDER, GATHER, ENEMY_BASE_LOCATION,
		BLITZKRIEG, ACTIVATE, ATTACK
	}
	final static int COMMAND_BITS = 4;
	
	/**
	 * Gets message type.
	 * 
	 * @param message
	 * @return
	 */
	public static MessageType getMessageType(int message) {
		int ones = (1 << COMMAND_BITS) - 1;
		return MessageType.values()[(message & ones)];
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	public static MapLocation getMessageLocation(int message) {
		final int twentyones = (1 << 20) - 1;
		final int location20bits = ((message >> COMMAND_BITS) & twentyones);
		return decodeLocation20bits(location20bits);
	}

	/**
	 * @param loc
	 *            Location of the zombie den.
	 * @return Message encoding zombie den location.
	 */
	public static int zombieDenLocation(MapLocation loc) {
		return MessageType.ZOMBIE_DEN_LOCATION.ordinal()
				+ (encodeLocation20bits(loc) << COMMAND_BITS);
	}

	public static int makeMessage(MessageType messageType, MapLocation loc) {
		return messageType.ordinal() + (encodeLocation20bits(loc) << COMMAND_BITS);
	}

	public static int encodeLocation20bits(MapLocation loc) {
		int encoding = (loc.x << 10) + loc.y;
		return encoding;
	}

	public static MapLocation decodeLocation20bits(int encoding) {
		return new MapLocation(encoding >> 10, encoding & 1023);
	}

	public static int makeEmptyMessage() {
		return EncodedMessage.makeMessage(MessageType.ZOMBIE_DEN_LOCATION,
				new MapLocation(1000, 1000));
	}

	public static boolean isEmptyMessage(int message) {
		return message == makeEmptyMessage();
	}
	/*
	public int makeModeMessage(GameMode mode, MapLocation location) {
		return makeMessage(MessageType.GAME_MODE,location) + (mode.ordinal() << (20 + COMMAND_BITS) ) ;
	}
	
	public GameMode getMode(int message){
		int modeOrdinal = (message >> (20 + COMMAND_BITS));
		return GameMode.values()[modeOrdinal];
	}
	*/
}
