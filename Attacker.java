package KingBattleCodeV6;

import battlecode.common.BulletInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Attacker extends Soldier {

	Attacker(RobotController rc) {
		super(rc);
	}

	int timeSpentMoving = 0;

	@Override
	void onUpdate() throws GameActionException {
		// See if there are any nearby enemy robots
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

		// See if there are any nearby bullets
		BulletInfo[] bullets = rc.senseNearbyBullets(-1);

		broadcastRobot(robots);

		timeSpentMoving++;

		if (robots.length > 0) {
			chase(robots[0]);
		} else {
			if (target == null) {
				float closestDis = 100000;
				int ind = 0;
				for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
					float x = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3);
					float y = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3 + 1);
					int ID = rc.readBroadcast(ARCHON_LOCATIONS + i * 3 + 2);
					MapLocation loc = new MapLocation(x, y);
					for (MapLocation mp : badTargets) {
						if (mp != null && loc != null) {
							if (mp.equals(loc)) {
								continue;
							}
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
							if (mp != null && loc != null) {
								if (mp.equals(loc)) {
									continue;
								}
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
				timeSpentMoving = 0;
			} else {
				if (rc.getLocation().distanceTo(target) < 3.50f) {
					badTargets.add(target);
					target = null;
				}
				if (timeSpentMoving > 100) {
					badTargets.add(target);
					target = null;
				}
			}
		}

		for (BulletInfo b : bullets) {
			if (willCollideWithMe(b) && !rc.hasMoved()) {
				tryMove(b.getDir().rotateLeftDegrees(90));
			}
		}
		if (!rc.hasMoved() && target != null) {
			bugPathToLoc(target);
		}
		// If there are some...
		if (robots.length > 0) {
			// And we have enough bullets, and haven't attacked yet this
			// turn...
			shootTarget(robots[0]);
		}
		if (debug) {
			if (target != null) {
				rc.setIndicatorLine(rc.getLocation(), target, 200, 200, 200);
			}
		}

		// Clock.yield() makes the robot wait until the next turn, then
		// it will perform this loop again
		doCrapAndYield();
	}
}
