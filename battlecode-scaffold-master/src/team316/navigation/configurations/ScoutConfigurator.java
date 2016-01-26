package team316.navigation.configurations;

import team316.navigation.RobotPotentialConfigurator;

public class ScoutConfigurator extends RobotPotentialConfigurator {
	
	@Override
	protected double oppositeArchonCharge() {
		return 3.0;
	}

	@Override
	protected double oppositeGuardCharge() {
		return -1.0;
	}

	@Override
	protected double oppositeSoldierCharge() {
		return -1.5;
	}

	@Override
	protected double oppositeViperCharge() {
		return -1.0;
	}

	@Override
	protected double oppositeScoutCharge() {
		return 0.0;
	}

	@Override
	protected double oppositeTurretCharge() {
		return -5.0;
	}

	@Override
	protected double allyArchonCharge() {
		return 1.0;
	}

	@Override
	protected double allyTurretCharge() {
		return 1.0;
	}

	@Override
	protected double fightingAllyCharge() {
		return 2.0;
	}
	
	@Override
	protected double allyDefaultCharge() {
		return 0.5;
	}

	@Override
	protected double zombieCharge() {
		return -5.0;
	}

	@Override
	protected double denCharge() {
		return -0.5;
	}

	@Override
	protected double defaultCharge() {
		return 1.0;
	}

	@Override
	protected double bigZombieCharge() {
		return -10.0;
	}

	@Override
	protected double fastZombieCharge() {
		return -5.0;
	}

	@Override
	protected double rangedZombieCharge() {
		return -3.0;
	}

}
