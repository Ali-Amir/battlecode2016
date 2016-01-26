package team316.navigation.configurations;

import team316.navigation.RobotPotentialConfigurator;

public class ArchonConfigurator extends RobotPotentialConfigurator {

	@Override
	protected double oppositeArchonCharge() {
		return 0.0;
	}

	@Override
	protected double oppositeGuardCharge() {
		return -1e3;
	}

	@Override
	protected double oppositeSoldierCharge() {
		return -5e3;
	}

	@Override
	protected double oppositeViperCharge() {
		return -1e3;
	}

	@Override
	protected double oppositeScoutCharge() {
		return 0.0;
	}

	@Override
	protected double oppositeTurretCharge() {
		return -1e4;
	}

	@Override
	protected double allyArchonCharge() {
		return 0.0;
	}

	@Override
	protected double allyTurretCharge() {
		return 0.0;
	}

	@Override
	protected double fightingAllyCharge() {
		return -1.0;
	}
	
	@Override
	protected double allyDefaultCharge() {
		return 1e2;
	}

	@Override
	protected double zombieCharge() {
		return -1e4;
	}

	@Override
	protected double denCharge() {
		return -1e3;
	}

	@Override
	protected double defaultCharge() {
		return 1.0;
	}

	@Override
	protected double bigZombieCharge() {
		return -1e5;
	}

	@Override
	protected double fastZombieCharge() {
		return -4e4;
	}

	@Override
	protected double rangedZombieCharge() {
		return -2e4;
	}

}
