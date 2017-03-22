package KingBattleCodeV6;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.GameConstants;


public class Tank extends Robot  {

	Tank(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	void onUpdate() throws GameActionException {
		// See if there are any nearby enemy robots
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

		// See if there are any nearby bullets
		BulletInfo[] bullets = rc.senseNearbyBullets(-1);

		if (robots.length > 0) {
			target = robots[0].getLocation();
		} else {
			target = initialArchonLocations[(int) (rnd * initialArchonLocations.length)];
		}

		for (BulletInfo b : bullets) {
			if (willCollideWithMe(b) && !rc.hasMoved()) {
				tryMove(b.getDir().rotateLeftDegrees(90));
			}
		}
		if (!rc.hasMoved()) {
			tryMove(rc.getLocation(), target);
		}
		// If there are some...
		if (robots.length > 0) {
			// And we have enough bullets, and haven't attacked yet this
			// turn...
			if (rc.canFirePentadShot()) {
				// ...Then fire a bullet in the direction of the enemy.
				rc.firePentadShot(rc.getLocation().directionTo(robots[0].location));
			}
		}
		//if (debug) {
		//	if (target != null) {
		//		rc.setIndicatorLine(rc.getLocation(), target, 200, 200, 200);
		//	}
		//}

		// Clock.yield() makes the robot wait until the next turn, then
		// it will perform this loop again
		Clock.yield();
	}
}
