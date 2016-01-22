package team316.navigation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import team316.utils.EncodedMessage;
import team316.utils.RCWrapper;
import team316.utils.EncodedMessage.MessageType;
import team316.utils.Grid;

public class EnemyLocationModel {

	final Set<MapLocation> knownZombieDens;
	final Set<MapLocation> knownNeutrals;
	final Set<Direction> knownBorders;
	public Queue<Integer> notificationsPending;
	final Map<Direction, Integer> maxCoordinateSofar;
	private Map<Direction, Integer> maxSoFarCoordinate = new HashMap<>();
	public EnemyLocationModel() {
		knownZombieDens = new HashSet<>();
		knownNeutrals = new HashSet<>();
		notificationsPending = new LinkedList<>();
		knownBorders = new HashSet<>();
		maxCoordinateSofar = new HashMap<>();
	}

	public void addZombieDenLocation(RobotInfo r) {
		if (!knownZombieDens.contains(r.location)) {
			knownZombieDens.add(r.location);
			notificationsPending
					.add(EncodedMessage.zombieDenLocation(r.location));
		}
	}
	public void addNeutralArchon(MapLocation loc) {
		if (!knownNeutrals.contains(loc)) {
			knownNeutrals.add(loc);
			notificationsPending.add(EncodedMessage
					.makeMessage(MessageType.NEUTRAL_ARCHON_LOCATION, loc));
		}
	}
	public void addNeutralNonArchon(MapLocation loc) {
		if (!knownNeutrals.contains(loc)) {
			knownNeutrals.add(loc);
			notificationsPending.add(EncodedMessage
					.makeMessage(MessageType.NEUTRAL_NON_ARCHON_LOCATION, loc));
		}
	}
	
	public void addBorders(Direction direction, int value) throws GameActionException{
		if(!knownBorders.contains(direction)){
			knownBorders.add(direction);
			if(Grid.isVertical(direction)){
				notificationsPending.add(EncodedMessage
						.makeMessage(MessageType.Y_BORDER, new MapLocation(0, value) ));
			}else{
				notificationsPending.add(EncodedMessage
						.makeMessage(MessageType.X_BORDER, new MapLocation(value, 0) ));
			}
			
		}
	}
	
	public void onNewTurn(RCWrapper rcWrapper) throws GameActionException {
		for(int i = 0; i < 4; i++){
			Direction direction = Grid.mainDirections[i];
			maxSoFarCoordinate.put(direction, rcWrapper.getMaxSoFarCoordinate(direction));
		}
	}
}
