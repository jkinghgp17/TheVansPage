package TheVansPage;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class Scout extends Robot {

	int currentArchonTarget = 0;

	ArrayList<MapLocation> badTargets;

	Scout(RobotController rc) {
		super(rc);
		badTargets = new ArrayList<MapLocation>();
	}

	@Override
	void onUpdate() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(13, Team.NEUTRAL);
		
		if (trees.length > 0) {
			boolean skip = false;
			for (TreeInfo t : trees) {
				if (t.location.equals(target)) {
					skip = true;
					break;
				}
			}
			if (!skip) {
				for (TreeInfo t : trees) {
					if (t.getContainedBullets() > 0) {
						target = t.getLocation();
						break;
					}
				}
			}
		}

		tryShakeTree();

		rc.broadcast(SCOUT_STATUS, rc.getRoundNum());
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		broadcastRobot(robots);
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
			float x = rc.readBroadcastFloat(ind);
			float y = rc.readBroadcastFloat(ind + 1);
			target = new MapLocation(x, y);
		}
		if (target != null) {
			tryMove(rc.getLocation(), target);
			rc.setIndicatorLine(rc.getLocation(), target, 200, 200, 200);
			if (rc.getLocation().distanceTo(target) < 1.50f) {
				badTargets.add(target);
				target = null;
			}
		} else {
			tryMove(randomDirection());
		}
		doCrapAndYield();
	}

	void tryShakeTree() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		for (TreeInfo t : trees) {
			if (t.containedBullets > 0 && rc.canShake(t.ID)) {
				rc.shake(t.ID);
				return;
			}
		}
	}
}
