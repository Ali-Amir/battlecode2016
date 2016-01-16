package team316;

import java.util.Arrays;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;
import battlecode.common.Team;
import team316.navigation.EnemyLocationModel;
import team316.navigation.ParticleType;
import team316.navigation.PotentialField;
import team316.navigation.motion.MotionController;
import team316.utils.Battle;
import team316.utils.Turn;


public class Guard implements Player {

	private final PotentialField field;
	private final MotionController mc;
	private int lastBroadcastTurn = -100;
	private int lastReceived = -100;
	private static final int MESSAGE_DELAY_TURNS = 50;
	private static final int BROADCAST_RADIUSSQR = 200;
	private final MapLocation archonLoc;
	private final EnemyLocationModel elm;

	public Guard(MapLocation archonLoc, PotentialField field,
			MotionController mc) {
		this.archonLoc = archonLoc;
		this.field = field;
		this.mc = mc;
		this.elm = new EnemyLocationModel(archonLoc);
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		field.addParticle(elm.predictEnemyBase(rc));

		// Do message signaling stuff.
		System.out.println();
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
				if(signal.getMessage() == null){
					field.addParticle(ParticleType.FIGHTING_ALLY, signal.getLocation(), 10);
				}else{
					if(signal.getMessage()[0] == RobotPlayer.MESSAGE_HELP_ARCHON){
						field.addParticle(ParticleType.ARCHON_ATTACKED, signal.getLocation(), 10);
					}
				}

				// lastReceived = Turn.currentTurn();
			} else {
				field.addParticle(ParticleType.OPPOSITE_GUARD,
						signal.getLocation(), 10);
			}
		}

		// Do sensing.
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(),
				rc.getType().sensorRadiusSquared);
		Battle.addEnemyParticles(enemyArray, field, 5);

		// rc.setIndicatorString(2, "Enemies around: " + enemyArray.length + "
		// turn: " + Turn.currentTurn());
		if (enemyArray.length > 0) {
			lastReceived = Turn.currentTurn();
			if (rc.isWeaponReady()) {
				// look for adjacent enemies to attack
				Arrays.sort(enemyArray, (a, b) -> {
					double weaknessDiff = Battle.weakness(a)
							- Battle.weakness(b);
					return weaknessDiff < 0 ? 1 : weaknessDiff > 0 ? -1 : 0;
				});
				for (RobotInfo oneEnemy : enemyArray) {
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
			} else {
				return;
			}

			// could not find any enemies adjacent to attack
			// try to move toward them
			if (rc.isCoreReady()) {
				// MapLocation goal = enemyArray[0].location;
				// Direction toEnemy = rc.getLocation().directionTo(goal);
				// RobotPlayer.tryToMove(rc, toEnemy);
				mc.tryToMove(rc);
				return;
			}
		}
		// rc.setIndicatorString(1, "Got here " + Turn.currentTurn());
		// there are no enemies nearby
		// check to see if we are in the way of friends
		// we are obstructing them
		if (rc.isCoreReady()) {
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
			Battle.addAllyParticles(nearbyFriends, field, 2);
			if (field.particles().size() == 0 && nearbyFriends.length > 2) {
				mc.tryToMoveRandom(rc);
			} else {
				mc.tryToMove(rc);
			}

		}
	}

}
