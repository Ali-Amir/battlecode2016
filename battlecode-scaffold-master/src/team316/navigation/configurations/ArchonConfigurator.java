package team316.navigation.configurations;

import team316.navigation.RobotPotentialConfigurator;

public class ArchonConfigurator extends RobotPotentialConfigurator {

	@Override
	protected double oppositeArchonCharge() {
		return 0.0;
	}

	@Override
	protected double oppositeGuardCharge() {
		return -1.0;
	}

	@Override
	protected double oppositeSoldierCharge() {
		return -10;
	}

	@Override
	protected double oppositeViperCharge() {
		return -10;
	}

	@Override
	protected double oppositeScoutCharge() {
		return -0.5;
	}

	@Override
	protected double oppositeTurretCharge() {
		return -20.0;
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
		return 30.0;
	}

	@Override
	protected double zombieCharge() {
		return -90.0;
	}

	@Override
	protected double denCharge() {
		return -100.0;
	}

	@Override
	protected double defaultCharge() {
		return 1.0;
	}

}
