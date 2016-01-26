package team316;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.Encoding;
import team316.utils.Probability;
import team316.utils.Turn;

public class Turret implements Player {

	private final PotentialField field;
	private final MotionController mc;
	private final MapLocation archonLoc;
	private int lastBroadcastTurn = -100;
	private int BROADCAST_RADIUSSQR = 150;
	private int lastTimeEnemySeen = -100;
	private Signal[] incomingSignals;
	private static final int MESSAGE_DELAY_TURNS = 30;
	private static final int BORING_TURNS_LIMIT = 50;

	public Turret(MapLocation archonLoc, PotentialField field,
			MotionController mc) {
		this.archonLoc = archonLoc;
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		incomingSignals = rc.emptySignalQueue();
		if (rc.getType().equals(RobotType.TURRET)) {
			turretCode(rc);
		} else {
			ttmCode(rc);
		}
	}

	private void addParticles(RobotController rc) {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(),
				rc.getType().sensorRadiusSquared);
		RobotInfo[] allyArray = rc.senseNearbyRobots(
				rc.getType().sensorRadiusSquared, rc.getTeam());
		Battle.addEnemyParticles(enemyArray, field, 1);
		int lifetime = 1;
		for (RobotInfo e : allyArray) {
			switch (e.type) {
				case ARCHON :
					field.addParticle(ParticleType.ALLY_ARCHON, e.location,
							lifetime);
					break;
				case TTM :
					field.addParticle(ParticleType.ALLY_TURRET, e.location,
							lifetime);
					break;
				case TURRET :
					field.addParticle(ParticleType.ALLY_TURRET, e.location,
							lifetime);
					break;
				case GUARD :
					break;
				case SCOUT :
					break;
				case SOLDIER :
					field.addParticle(ParticleType.ALLY_DEFAULT, e.location,
							lifetime);
					break;
				case VIPER :
					// field.addParticle(ParticleType.ALLY_DEFAULT, e.location,
					// lifetime);
					break;
				default :
					throw new RuntimeException("Unknown type!");
			}
		}

	}
	public void ttmCode(RobotController rc) throws GameActionException {
		addParticles(rc);
		if(rc.senseHostileRobots(rc.getLocation(),
				1000000).length > 0){
			lastTimeEnemySeen = Turn.currentTurn();
			if(rc.isCoreReady()){
				rc.unpack();
			}else{
				return;
			}
		}
		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
		}
		if (rc.isCoreReady()) {
			rc.unpack();
		}
	}

	private void attackCode(RobotController rc) throws GameActionException {
		rc.setIndicatorString(2, "Searching for enemy");
		if (!rc.isWeaponReady()) {
			return;
		}
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(),
				rc.getType().sensorRadiusSquared);
		if (visibleEnemyArray.length > 0) {
			lastTimeEnemySeen = Turn.currentTurn();
		}

		double weakest = -100000;
		MapLocation weakestLocation = null;
		for (RobotInfo enemy : visibleEnemyArray) {
			int distanceToSignalingEnemy = rc.getLocation()
					.distanceSquaredTo(enemy.location);

			if (distanceToSignalingEnemy >= 6) {
				if (Battle.weakness(enemy) > weakest) {
					weakestLocation = enemy.location;
					weakest = Battle.weakness(enemy);
				}
			}
		}

		if (weakestLocation != null) {
			rc.attackLocation(weakestLocation);
			return;
		}
		rc.setIndicatorString(2, "Can't see any enemies!");
		for (Signal s : incomingSignals) {
			if (s.getTeam() == rc.getTeam().opponent()) {
				MapLocation enemySignalLocation = s.getLocation();
				int distanceToSignalingEnemy = rc.getLocation()
						.distanceSquaredTo(enemySignalLocation);
				if (distanceToSignalingEnemy <= rc
						.getType().attackRadiusSquared) {
					rc.attackLocation(enemySignalLocation);
					break;
				}
			}
		}

	}

	public void turretCode(RobotController rc) throws GameActionException {
		if (Turn.turnsSince(lastBroadcastTurn) > MESSAGE_DELAY_TURNS
				&& lastTimeEnemySeen + 1 == Turn.currentTurn()) {
			rc.broadcastSignal(BROADCAST_RADIUSSQR);
			lastBroadcastTurn = Turn.currentTurn();
		}

		attackCode(rc);

		if (rc.isCoreReady() && Turn.turnsSince(lastTimeEnemySeen) > BORING_TURNS_LIMIT) {
			rc.pack();
		}

	}



}
