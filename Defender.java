package KingBattleCodeV6;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Defender extends Soldier {

	Defender(RobotController rc) {
		super(rc);
	}

	@Override
	void onUpdate() throws GameActionException {
		// See if there are any nearby enemy robots
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

		// See if there are any nearby bullets
		BulletInfo[] bullets = rc.senseNearbyBullets(-1);
		
		broadcastRobot(robots);
		
		if (robots.length > 0) {
			chase(robots[0]);
		} else {
			if (target == null) {
				float closestDis = 100000;
				int ind = 0;
				for (int i = 0; i < NUMBER_OF_TARGETS; i++) {
					int index = HIGH_PRIORITY_TARGET_OFFSET + i * 4;
					if (rc.readBroadcast(index + 3) != 0) {
						float x = rc.readBroadcastFloat(index);
						float y = rc.readBroadcastFloat(index + 1);
						MapLocation loc = new MapLocation(x, y);
						if (rc.getLocation().distanceTo(loc) < closestDis) {
							closestDis = rc.getLocation().distanceTo(loc);
							ind = index;
						}
					}
				}
				if (ind != 0) {
					closestDis = -1;
				}
				for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
					float x = rc.readBroadcastFloat(ALLIED_ARCHON_LOCATIONS + i * 3);
					float y = rc.readBroadcastFloat(ALLIED_ARCHON_LOCATIONS + i * 3 + 1);
					int ID = rc.readBroadcast(ALLIED_ARCHON_LOCATIONS + i * 3 + 2);
					MapLocation loc = new MapLocation(x, y);
					/*for (MapLocation mp : badTargets) {
						if (mp.equals(loc)) {
							continue;
						}
					}*/
					if (rc.getLocation().distanceTo(loc) < closestDis) {
						closestDis = rc.getLocation().distanceTo(loc);
						ind = ALLIED_ARCHON_LOCATIONS + i * 3;
					}
				}
				float x = rc.readBroadcastFloat(ind);
				float y = rc.readBroadcastFloat(ind + 1);
				target = new MapLocation(x, y);
			} else {
			if (rc.getLocation().distanceTo(target) < 0.50f) {
				badTargets.add(target);
				target = null;
			}
		}
	}

	for(BulletInfo b:bullets) {
		if (willCollideWithMe(b) && !rc.hasMoved()) {
			tryMove(b.getDir().rotateLeftDegrees(90));
		}
	} 
	if(!rc.hasMoved()&&target!=null) {
		bugPathToLoc(target);
	}
	// If there are some...
	if(robots.length>0) {
		// And we have enough bullets, and haven't attacked yet this
		// turn...
		shootTarget(robots[0]);
	}if(debug) {
		if (target != null) {
			rc.setIndicatorLine(rc.getLocation(), target, 200, 200, 200);
		}
	}

	// Clock.yield() makes the robot wait until the next turn, then
	// it will perform this loop again
	doCrapAndYield();
}}
