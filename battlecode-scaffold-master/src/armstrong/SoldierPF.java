package armstrong;

import armstrong.navigation.PotentialField;
import armstrong.navigation.motion.MotionController;
import armstrong.utils.Turn;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class SoldierPF implements Player {

	private final PotentialField field;
	private final MotionController mc;
	private int lastBroadcastTurn = -100;
	private final static int MESSAGE_DELAY_TURNS = 5;

	public SoldierPF(PotentialField field, MotionController mc) {
		this.field = field;
		this.mc = mc;
	}

	@Override
	public void play(RobotController rc) throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		boolean isWeaponReady = rc.isWeaponReady();
		boolean enemyInAttackRange = false;
		if (enemyArray.length > 0) {
			if (Turn.currentTurn() - lastBroadcastTurn > MESSAGE_DELAY_TURNS) {
				rc.broadcastSignal(200);
				lastBroadcastTurn = Turn.currentTurn();
			}

			// look for adjacent enemies to attack
			for (RobotInfo oneEnemy : enemyArray) {
				if (rc.canAttackLocation(oneEnemy.location)) {
					enemyInAttackRange = true;
					if (isWeaponReady) {
						rc.attackLocation(oneEnemy.location);
					}
					break;
				}
			}
			if (rc.isCoreReady() && !enemyInAttackRange) {
				MapLocation goal = enemyArray[0].location;
				Direction toEnemy = rc.getLocation().directionTo(goal);
				RobotPlayer.tryToMove(rc, toEnemy);
			}
		} else {// there are no enemies nearby
				// check to see if we are in the way of friends
				// we are obstructing them
			if (rc.isCoreReady()) {
				if (Turn.currentTurn() - lastBroadcastTurn > MESSAGE_DELAY_TURNS) {
					// Do message signaling stuff.
					final Signal[] signals = rc.emptySignalQueue();
					for (Signal signal : signals) {
						// If ally. Then ally is reporting enemies.
						if (signal.getTeam().equals(rc.getTeam())) {
							// field.addParticle(ParticleType.FIGHTING_ALLY,
							// signal.getLocation(), 5);
							if (Turn.currentTurn() - lastBroadcastTurn > MESSAGE_DELAY_TURNS) {
								rc.broadcastSignal(200);
								lastBroadcastTurn = Turn.currentTurn();
							}

							Direction towardFightingAlly = rc.getLocation().directionTo(signal.getLocation());
							RobotPlayer.tryToMove(rc, towardFightingAlly);
							return;
						} else {
							// field.addParticle(ParticleType.OPPOSITE_GUARD,
							// signal.getLocation(), 10);
							rc.broadcastSignal(200);
							lastBroadcastTurn = Turn.currentTurn();

							Direction towardEnemy = rc.getLocation().directionTo(signal.getLocation());
							RobotPlayer.tryToMove(rc, towardEnemy);
							return;
						}
					}
				}

				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if (nearbyFriends.length > 3) {
					Direction away = RobotPlayer.randomDirection();
					RobotPlayer.tryToMove(rc, away);
				} else {// maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000, rc.getTeam());
					MapLocation weakestOne = RobotPlayer.findWeakest(alliesToHelp);
					if (weakestOne != null) {// found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						RobotPlayer.tryToMove(rc, towardFriend);
					}
				}
			}
		}
	}

}
