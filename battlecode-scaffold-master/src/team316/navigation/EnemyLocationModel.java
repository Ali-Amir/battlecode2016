package team316.navigation;

import java.util.HashSet;
import java.util.LinkedList;
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

public class EnemyLocationModel {

	final Set<MapLocation> knownZombieDens;
	final Set<MapLocation> knownNeutrals;
	final Set<Direction> knownBorders;
	public Queue<Integer> notificationsPending;

	public EnemyLocationModel() {
		knownZombieDens = new HashSet<>();
		knownNeutrals = new HashSet<>();
		notificationsPending = new LinkedList<>();
		knownBorders = new HashSet<>();
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
	
	public void addBorders(Direction direction, RCWrapper rcWrapper) throws GameActionException{
		int coordinateMin;
		int coordinateMax;
		boolean isYBorder = (direction.equals(Direction.NORTH) || direction.equals(Direction.SOUTH));
		if(!knownBorders.contains(direction)){
			knownBorders.add(direction);
			if(isYBorder){
				coordinateMin = rcWrapper.getMaxCoordinate(Direction.NORTH);
				coordinateMax = rcWrapper.getMaxCoordinate(Direction.SOUTH);
				notificationsPending.add(EncodedMessage
						.makeMessage(MessageType.Y_BORDER, new MapLocation(coordinateMin, coordinateMax) ));
			}else{
				coordinateMin = rcWrapper.getMaxCoordinate(Direction.WEST);
				coordinateMax = rcWrapper.getMaxCoordinate(Direction.EAST);
				notificationsPending.add(EncodedMessage
						.makeMessage(MessageType.X_BORDER, new MapLocation(coordinateMin, coordinateMax) ));
			}
			
		}
	}
	
	public void onNewTurn() {
	}
}
