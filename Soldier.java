package KingBattleCodeV6;

import java.util.ArrayList;
import java.util.List;
import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.GameConstants;

public abstract class Soldier extends Robot {

	List<MapLocation> badTargets;

	boolean isDefender;

	Soldier(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
		target = initialArchonLocations[(int) (rnd * initialArchonLocations.length)];
		badTargets = new ArrayList<MapLocation>();
		try {
			int attackerCnt = rc.readBroadcast(ATTACKER_OFFSET);
			int defenderCnt = rc.readBroadcast(DEFENDER_OFFSET);
			if ((defenderCnt == 0) || (defenderCnt * 2 < attackerCnt)) {
				isDefender = true;
				rc.broadcast(DEFENDER_OFFSET, defenderCnt + 1);
			} else {
				isDefender = false;
				rc.broadcast(ATTACKER_OFFSET, attackerCnt + 1);
			}
		} catch(GameActionException e) {
			System.out.println("Soldier init error");
			e.printStackTrace();
		}
	}
	
	void shootTarget(RobotInfo robot) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(robot.getLocation());
		switch (robot.getType()) {
			case GARDENER:
				//shootGardener(dir);
				shootPentad(dir);
				break;
			case ARCHON:
				shootArchon(dir);
				break;
			default:
				shootPentad(dir);
				break;
		}
	}

	private void shootArchon(Direction dir) throws GameActionException {
		if (isTimeToAttackArchons()) {
			shootPentad(dir);
		}
	}

	private void shootGardener(Direction dir) throws GameActionException {
		int result = shootRay(dir, rc.getLocation());
		if (result != 1) {
			if (rc.canFireSingleShot()) {
				rc.fireSingleShot(dir);
			}
		}
	}
	
	private void shootPentad(Direction dir) throws GameActionException {
		if (isConeClear(dir, 15 * 5) && rc.canFirePentadShot()) {
			rc.firePentadShot(dir);
			rc.broadcast(LAST_BULLET_FIRED, rc.getRoundNum());
		} else if (isConeClear(dir, 3 * 20) && rc.canFireTriadShot()) {
			rc.fireTriadShot(dir);
			rc.broadcast(LAST_BULLET_FIRED, rc.getRoundNum());
		} else if (isConeClear(dir, 3) && rc.canFireSingleShot()) {
			rc.fireSingleShot(dir);
			rc.broadcast(LAST_BULLET_FIRED, rc.getRoundNum());
		}
	}

	/**
	 * Keeps proper distance from enemy robot
	 * 
	 * @param robot
	 */
	void chase(RobotInfo robot) {
		MapLocation loc = robot.getLocation();
		Direction dir = rc.getLocation().directionTo(loc);
		if (rc.getLocation().distanceTo(loc) < 5f) {
			target = rc.getLocation().add(dir.rotateLeftDegrees(180));
		} else {
			target = rc.getLocation().add(dir);
		}
	}
}
