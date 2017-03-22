package KingBattleCodeV6;

import battlecode.common.BulletInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class BattleScout extends Soldier {

	BattleScout(RobotController rc) {
		super(rc);
	}

	/**
	 *
	 * @see KingBattleCodeV6.Robot#onUpdate()
	 */
	void onUpdate() throws GameActionException {
		// See if there are any nearby enemy robots
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

		// See if there are any nearby bullets
		BulletInfo[] bullets = rc.senseNearbyBullets(-1);

		if (target != null) {
			if (rc.getLocation().distanceTo(target) < 0.05f) {
				badTargets.add(target);
				target = null;
			}
		}

		pickTarget(robots);

		if (rc.canSenseLocation(target) && target != null) {
			if (rc.isLocationOccupiedByRobot(target)) {
				if (rc.canFireSingleShot()) {
					rc.fireSingleShot(rc.getLocation().directionTo(target));
				}
			}
		}

		if (rc.canMove(target)) {
			rc.move(target);
		} else {
			bugPathToLoc(target);
		}

		doCrapAndYield();
	}

	void pickTarget(RobotInfo[] robots) throws GameActionException {
		for (RobotInfo r : robots) {
			switch (r.getType()) {
			case GARDENER:
				target = r.getLocation();
				break;
			case ARCHON:
				if (isTimeToAttackArchons()) {
					target = r.getLocation();
					break;
				}
			default:
				break;
			}
		}
		if (target == null) {
			float closestDis = 100000;
			int ind = 0;
			for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
				float x = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3);
				float y = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3 + 1);
				int ID = rc.readBroadcast(ARCHON_LOCATIONS + i * 3 + 2);
				MapLocation loc = new MapLocation(x, y);
				for (MapLocation mp : badTargets) {
					if (mp.equals(loc)) {
						continue;
					}
				}
				if (rc.getLocation().distanceTo(loc) < closestDis) {
					closestDis = rc.getLocation().distanceTo(loc);
					ind = ARCHON_LOCATIONS + i * 3;
				}
			}
			for (int i = 0; i < ENEMY_GARDENER_COUNT; i++) {
				if (rc.readBroadcast(ENEMY_GARDENER_OFFSET + i * 4 + 3) != 0) {
					float x = rc.readBroadcastFloat(ENEMY_GARDENER_OFFSET + i * 4);
					float y = rc.readBroadcastFloat(ENEMY_GARDENER_OFFSET + i * 4 + 1);
					MapLocation loc = new MapLocation(x, y);
					for (MapLocation mp : badTargets) {
						if (mp.equals(loc)) {
							continue;
						}
					}
					if (rc.getLocation().distanceTo(loc) < closestDis) {
						closestDis = rc.getLocation().distanceTo(loc);
						ind = ENEMY_GARDENER_OFFSET + i * 4;
					}
				}
			}
			float x = rc.readBroadcastFloat(ind);
			float y = rc.readBroadcastFloat(ind + 1);
			target = new MapLocation(x, y);
		}
	}
}
