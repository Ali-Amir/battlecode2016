package team316;

import java.util.ArrayList;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import team316.navigation.EnemyLocationModel;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.RCWrapper;
import team316.utils.Turn;

public class SoldierPF implements Player {

	private final PotentialField field;
	private final MotionController mc;
	private int lastBroadcastTurn = -100;
	private int lastReceived = -100;
	private static final int MESSAGE_DELAY_TURNS = 50;
	private static final int BROADCAST_RADIUSSQR = 200;
	private final MapLocation archonLoc;
	private final EnemyLocationModel elm;
	private final RCWrapper rcWrapper;

	public SoldierPF(MapLocation archonLoc, PotentialField field,
			MotionController mc, RobotController rc) {
		this.archonLoc = archonLoc;
		this.field = field;
		this.mc = mc;
		this.rcWrapper = new RCWrapper(rc);
		RobotPlayer.rcWrapper = rcWrapper;
		this.elm = new EnemyLocationModel(archonLoc);
		this.elm.enemyBaseAt(rc.getInitialArchonLocations(rcWrapper.enemyTeam));
	}

	/*
	 * Smallest number here must be non-negative.
	 */
	private int getAttakTypePriority(RobotType t) {
		switch (t) {
			case ARCHON :
				return 5;

			case GUARD :
				return 15;

			case SOLDIER :
				return 100;

			case SCOUT :
				return 1;

			case VIPER :
				return 10;

			case TTM :
				return 5;

			case TURRET :
				return 5;
			case BIGZOMBIE :
				return 10;
			case RANGEDZOMBIE :
				return 10;
			case STANDARDZOMBIE :
				return 5;
			case FASTZOMBIE :
				return 10;
			case ZOMBIEDEN :
				return 2;
			default :
				throw new RuntimeException("UNKNOWN ROBOT TYPE!");
		}
	}

	private RobotInfo chooseTarget(RobotController rc,
			ArrayList<RobotInfo> attackableEnemies) {
		RobotInfo bestTarget = null;
		int bestTypePriority = -1;
		double currentWeakness = -10000000;
		for (RobotInfo enemy : attackableEnemies) {
			if (getAttakTypePriority(enemy.type) == bestTypePriority
					&& Battle.weakness(enemy) > currentWeakness) {
				bestTarget = enemy;
				currentWeakness = Battle.weakness(enemy);
			}
			if (getAttakTypePriority(enemy.type) > bestTypePriority) {
				bestTarget = enemy;
				bestTypePriority = getAttakTypePriority(enemy.type);
				currentWeakness = Battle.weakness(enemy);;
			}
		}
		return bestTarget;
	}

	public void receiveIncomingSignals(RobotController rc)
			throws GameActionException {
		// Do message signaling stuff.
		rc.setIndicatorString(0, "Current enemy base prediction: "
				+ elm.predictEnemyBase(rc) + " turn: " + Turn.currentTurn());
		if (Turn.currentTurn() - lastBroadcastTurn > MESSAGE_DELAY_TURNS
				&& lastReceived > MESSAGE_DELAY_TURNS + lastBroadcastTurn) {
			rc.broadcastSignal(BROADCAST_RADIUSSQR);
			lastBroadcastTurn = Turn.currentTurn();
			rc.setIndicatorString(1,
					"Relay message at turn: " + Turn.currentTurn());
		}

		final Signal[] signals = rc.emptySignalQueue();
		for (Signal signal : signals) {
			// If ally. Then ally is reporting enemies.
			if (signal.getTeam().equals(rc.getTeam())) {
				field.addParticle(ParticleType.FIGHTING_ALLY,
						signal.getLocation(), 10);
				// elm.enemyAlertFromLocation(signal.getLocation(), rc);
				// lastReceived = Turn.currentTurn();
			} else {
				field.addParticle(ParticleType.OPPOSITE_GUARD,
						signal.getLocation(), 10);
			}
		}
	}

	public void initOnNewTurn(RobotController rc) throws GameActionException {
		// Attract towards closest enemy base location prediction.
		//field.addParticle(elm.predictEnemyBase(rc));

		rcWrapper.initOnNewTurn();
	}

	/**
	 * Implements logic for walking when there are no enemy robots around.
	 * 
	 * @param rc
	 * @throws GameActionException
	 */
	public void walkingModeCode(RobotController rc) throws GameActionException {
		// rc.setIndicatorString(1, "Got here " + Turn.currentTurn());
		// there are no enemies nearby
		// check to see if we are in the way of friends
		// we are obstructing them
		if (rc.isCoreReady()) {
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
			Battle.addAllyParticles(nearbyFriends, field, 2);
			if (field.particles().size() == 0 || nearbyFriends.length > 2) {
				mc.tryToMoveRandom(rc);
			} else {
				mc.tryToMove(rc);
			}
		}
	}

	/**
	 * Implements logic for fighting, that is when an enemy robot is visible.
	 * 
	 * @param rc
	 * @throws GameActionException
	 */
	public void fightingModeCode(RobotController rc)
			throws GameActionException {
		// Add two types of particles for each hostile:
		// 1. Repelling that lasts only for one turn (so that we walk away as we
		// shoot).
		// 2. And attracting that lasts for 5 turns (so that when the enemy out
		// of sight we try to go back).
		Battle.addScaryParticles(rcWrapper.hostileRobotsNearby(), field, 1);
		Battle.addEnemyParticles(rcWrapper.hostileRobotsNearby(), field, 5);

		lastReceived = Turn.currentTurn();
		if (rc.isWeaponReady()) {
			for (RobotInfo oneEnemy : rcWrapper.hostileRobotsNearby()) {
				if (rc.canAttackLocation(oneEnemy.location)) {
					rc.setIndicatorString(0,
							"trying to attack " + Turn.currentTurn());
					rc.attackLocation(oneEnemy.location);
					if (!oneEnemy.team.equals(rc.getTeam())
							&& !oneEnemy.team.equals(Team.NEUTRAL)
							&& !oneEnemy.team.equals(Team.ZOMBIE)) {
						elm.enemyAtLocation(oneEnemy.location, rc);
					}
					break;
				}
			}
		}

		// could not find any enemies adjacent to attack
		// try to move toward them
		if (rc.isCoreReady()) {
			mc.tryToMove(rc);
			return;
		}
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		// Initialize all we can.
		initOnNewTurn(rc);

		// Receive signals and update field based on the contents.
		receiveIncomingSignals(rc);

		// Decide on mode: Walking vs. Fighting.
		if (rcWrapper.hostileRobotsNearby().isEmpty()) { // Walking.
			walkingModeCode(rc);
		} else { // Fighting.
			fightingModeCode(rc);
		}
	}

}
