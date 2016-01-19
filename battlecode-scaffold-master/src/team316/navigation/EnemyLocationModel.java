package team316.navigation;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import team316.utils.EncodedMessage;

public class EnemyLocationModel {

	final Set<MapLocation> knownZombieDens;
	final Queue<Integer> notificationsPending;

	public EnemyLocationModel() {
		knownZombieDens = new HashSet<>();
		notificationsPending = new LinkedBlockingQueue<Integer>();
	}

	public void addZombieDenLocation(RobotInfo r) {
		if (!knownZombieDens.contains(r.location)) {
			knownZombieDens.add(r.location);
			notificationsPending
					.add(EncodedMessage.zombieDenLocation(r.location));
		}
	}

	public void onNewTurn() {
	}
}
