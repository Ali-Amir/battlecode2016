package team316;

import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import javafx.scene.shape.ArcType;
import team316.navigation.ChargedParticle;
import team316.navigation.EnemyLocationModel;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.EncodedMessage;
import team316.utils.EncodedMessage.MessageType;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class Scout implements Player {

	private static final int BROADCAST_RADIUS = 80 * 80 * 2;

	private final PotentialField field;
	private final MotionController mc;
	private final EnemyLocationModel elm;
	private final RCWrapper rcWrapper;

	private int curDirection = 0;
	private Direction[] bordersYetToDiscover = {Direction.NORTH,
			Direction.SOUTH, Direction.EAST, Direction.WEST};
	public enum ScoutState {
		RUNAWAY, ROAM_AROUND, NEED_TO_BROADCAST
	}

	public Scout(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		this.field = field;
		this.mc = mc;
		this.rcWrapper = new RCWrapper(rc);
		RobotPlayer.rcWrapper = rcWrapper;
		this.elm = new EnemyLocationModel();
	}

	public void initOnNewTurn(RobotController rc) throws GameActionException {
		elm.onNewTurn();
		rcWrapper.initOnNewTurn();
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		initOnNewTurn(rc);

		ScoutState state = assessSituation(rc);

		rc.setIndicatorString(0, "Currently in mode: " + state);

		switch (state) {
			case RUNAWAY :
				runaway(rc);
				break;
			case ROAM_AROUND :
				roamAround(rc);
				break;
			case NEED_TO_BROADCAST :
				publishData(rc);
				roamAround(rc);
				break;

			default :
				assert(0 == 1) : "One state case was missed!";
		}
	}

	public void publishData(RobotController rc) throws GameActionException {
		int messageA = elm.notificationsPending.poll();
		int messageB = elm.notificationsPending.isEmpty()
				? 0
				: elm.notificationsPending.poll();
		System.out.println("messageA: " + messageA + ", messageB:" + messageB);
		rc.broadcastMessageSignal(messageA, messageB, BROADCAST_RADIUS);
	}

	public void roamAround(RobotController rc) throws GameActionException {
		if (rc.isCoreReady()) {
			for (int i = 0; i < 8; ++i, curDirection = (curDirection + 1) & 7) {
				Direction maybeForward = Direction.values()[curDirection];
				if (rc.canMove(maybeForward)) {
					rc.move(maybeForward);
					return;
				}
			}

			for (int i = 0; i < 8; ++i, curDirection = (curDirection + 1) & 7) {
				Direction maybeForward = Direction.values()[curDirection];
				MapLocation ahead = rc.getLocation().add(maybeForward);
				if (rc.senseRubble(
						ahead) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(maybeForward);
					return;
				}
			}

		}
	}

	public void runaway(RobotController rc) throws GameActionException {
		RobotInfo[] robotsWhoCanAttackMe = Battle.robotsWhoCanAttackLocation(
				rc.getLocation(), rcWrapper.enemyTeamRobotsNearby());
		for (RobotInfo r : robotsWhoCanAttackMe) {
			field.addParticle(new ChargedParticle(-1.0, r.location, 1));
		}

		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
		}
	}

	public ScoutState assessSituation(RobotController rc) throws GameActionException {
		inspectEnemiesWithinSightRange();
		inspectNeutralRobotswithinSightRange(rc);
		inspectBorders(rcWrapper);
		RobotInfo[] robotsWhoCanAttackMe = Battle.robotsWhoCanAttackLocation(
				rc.getLocation(), rcWrapper.enemyTeamRobotsNearby());
		if (robotsWhoCanAttackMe.length > 0) {
			return ScoutState.RUNAWAY;
		} else {
			if (elm.notificationsPending.size() > 0) {
				return ScoutState.NEED_TO_BROADCAST;
			} else {
				return ScoutState.ROAM_AROUND;
			}
		}
	}

	private void inspectEnemiesWithinSightRange() {
		RobotInfo[] robotsISee = rcWrapper.hostileRobotsNearby();
		for (RobotInfo r : robotsISee) {
			if (r.type.equals(RobotType.ZOMBIEDEN)) {
				elm.addZombieDenLocation(r);
			}
		}
	}
	
	private void inspectNeutralRobotswithinSightRange(RobotController rc) {
		RobotInfo[] neutralIsee = rc.senseNearbyRobots(
				RobotType.SCOUT.sensorRadiusSquared, Team.NEUTRAL);
		for (RobotInfo r : neutralIsee) {
			if (r.type.equals(RobotType.ARCHON)) {
				elm.addNeutralArchon(r.location);
			} else {
				elm.addNeutralNonArchon(r.location);
			}
		}
	}
	
	private void inspectBorders(RCWrapper rcWrapper) throws GameActionException{
		for(int i = 0; i < 4; i ++){
			Direction direction = bordersYetToDiscover[i]; 
			if(direction == Direction.NONE || rcWrapper.getMaxCoordinate(direction) != null){
				elm.addBorders(direction, rcWrapper);
				bordersYetToDiscover[i] = Direction.NONE;
			}
		}
	}
	
}
