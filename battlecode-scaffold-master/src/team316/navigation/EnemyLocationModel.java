package team316.navigation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import team316.utils.EncodedMessage;
import team316.utils.EncodedMessage.MessageType;

public class EnemyLocationModel {

	final Set<MapLocation> knownZombieDens;
	final Set<MapLocation> knownNeutralArchons;
	public Queue<Integer> notificationsPending;

	public EnemyLocationModel() {
		knownZombieDens = new HashSet<>();
		knownNeutralArchons = new HashSet<>();
		notificationsPending = new LinkedList<>();// new
													// LinkedBlockingQueue<Integer>();
	}

	public void addZombieDenLocation(RobotInfo r) {
		if (!knownZombieDens.contains(r.location)) {
			knownZombieDens.add(r.location);
			notificationsPending
					.add(EncodedMessage.zombieDenLocation(r.location));
		}
	}
	public void addNeutralArchon(MapLocation loc) {
		if (!knownNeutralArchons.contains(loc)) {
			knownNeutralArchons.add(loc);
			notificationsPending.add(EncodedMessage
					.makeMessage(MessageType.NEUTRAL_ARCHON_LOCATION, loc));
		}
	}

	public void onNewTurn() {
	}
}
