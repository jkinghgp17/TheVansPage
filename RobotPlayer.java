package KingBattleCodeV6;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	/**
	 * run() is the method that is called when a robot is instantiated in the
	 * Battlecode world. If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {

		// This is the RobotController object. You use it to perform actions
		// from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;
		Robot robot = null;

		// Here, we've separated the controls into a different method for each
		// RobotType.
		// You can add the missing ones or rewrite this into your own control
		// structure.
		switch (rc.getType()) {
		case ARCHON:
			robot = new Archon(rc);
			break;
		case GARDENER:
			robot = new Gardener(rc);
			break;
		case SOLDIER:
			robot = createSoldier(rc);
			break;
		case LUMBERJACK:
			robot = new Lumberjack(rc);
			break;
		case TANK:
			robot = new Tank(rc);
			break;
		case SCOUT:
			robot = /*createScout(rc);*/ new Scout(rc);
			break;
		}
		while (true) {
			try {
				// First initalize robot
				robot.init();
				while (true) {
					robot.onUpdate();
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	/*
	static boolean needsBattleScouts() {
		return (rc.getOpponentVictoryPoints() > 250 && rc.getRoundNum() < 1000);
	}

	static Robot createScout(RobotController rc) {
		try {
			if (needsBattleScouts()) {
				return new BattleScout(rc);
			} else {
				rc.broadcast(RobotType.SCOUT.ordinal(), rc.readBroadcast(RobotType.SCOUT.ordinal()) + 1);
				return new Scout(rc);
			}
		} catch (GameActionException e) {
			System.out.println("Error while creating scout");
			e.printStackTrace();
		}
		return null;
	}
	*/
	static Robot createSoldier(RobotController rc) {
		try {
			int aCnt = rc.readBroadcast(Robot.ATTACKER_OFFSET);
			int dCnt = rc.readBroadcast(Robot.DEFENDER_OFFSET);
			if ((dCnt == 0 || dCnt * 2 < aCnt) && false) {
				rc.broadcast(Robot.DEFENDER_OFFSET, dCnt + 1);
				return new Defender(rc);
			} else {
				rc.broadcast(Robot.ATTACKER_OFFSET, dCnt + 1);
				return new Attacker(rc);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return null;
	}
}
