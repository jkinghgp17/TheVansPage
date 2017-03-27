package TheVansPage;

import battlecode.common.*;

public class Archon extends Robot {

	Archon(RobotController rc) {
		super(rc);
		// Broadcast ID
		try {
			float closestDis = 100000;
			int index = 0;
			for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); ++i) {
				int ind = ALLIED_ARCHON_LOCATIONS + i * 3;
				float x = rc.readBroadcastFloat(ind);
				float y = rc.readBroadcastFloat(ind + 1);
				int id = rc.readBroadcast(ind + 2);
				if (id != -1) {
					continue;
				}
				MapLocation loc = new MapLocation(x, y);
				if (rc.getLocation().distanceTo(loc) <= closestDis) {
					closestDis = rc.getLocation().distanceTo(loc);
					index = ind;
				}
			}
			rc.broadcast(index + 2, rc.getID());

			// Stupid Lumberjack
			rc.broadcast(RobotType.LUMBERJACK.ordinal(), 0);
		} catch (GameActionException e) {
			System.out.println("Archon init error");
			e.printStackTrace();
		}
	}

	int lastGardener = 0;

	@Override
	void onUpdate() throws GameActionException {
		for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
			int ind = ALLIED_ARCHON_LOCATIONS + i * 3;
			int id = rc.readBroadcast(ind + 2);
			if (id == rc.getID() || id == 0) {
				rc.broadcastFloat(ind, rc.getLocation().x);
				rc.broadcastFloat(ind + 1, rc.getLocation().y);
				rc.broadcast(ind + 2, rc.getID());
				break;
			}
		}
		if (rc.hasRobotBuildRequirements(RobotType.GARDENER) && needsMoreGardeners()) {
			int tries = 0;
			int maxTries = rc.readBroadcast(RobotType.GARDENER.ordinal()) < 2 ? 100 : 20;
			while (tries < maxTries) {
				Direction dir = randomDirection();
				MapLocation testLocation = rc.getLocation().add(dir,
						type.bodyRadius + RobotType.GARDENER.bodyRadius + GameConstants.GENERAL_SPAWN_OFFSET);
				if (debug) {
					rc.setIndicatorDot(testLocation, 200, 0, 0);
				}
				if (rc.onTheMap(testLocation) && isLikelyGoodLocation(testLocation)) {
					if (rc.canBuildRobot(RobotType.GARDENER, dir)) {
						rc.buildRobot(RobotType.GARDENER, dir);
						rc.broadcast(RobotType.GARDENER.ordinal(), rc.readBroadcast(RobotType.GARDENER.ordinal()) + 1);
						lastGardener = rc.getRoundNum();
						break;
					}
				}
			}
		}
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		for (RobotInfo r : robots) {
			switch (rc.getType()) {
			case SOLDIER:
				highTarget(r);
				runAway(r);
				break;
			case TANK:
				highTarget(r);
				runAway(r);
				break;
			case SCOUT:
				highTarget(r);
				break;
			default:
				break;
			}
		}
		// Finding targets and moving
		if (target == null) {
			target = targetNearestArchon();
			if (target == null) {
				target = lookForHome();
				if (target == null) {
					target = sweepArea();
				}
			}
		}
		if (target != null && !rc.hasMoved()) {
			bugPathToLoc(target);
		}
		if (rc.getLocation().distanceTo(target) < 1.5f) {
			target = null;
		}

		if (rc.getRoundNum() % 200 == 0) {
			System.out.println(rc.readBroadcast(RobotType.SOLDIER.ordinal()));
			System.out.println(rc.readBroadcast(RobotType.LUMBERJACK.ordinal()));
			System.out.println(rc.readBroadcast(RobotType.GARDENER.ordinal()));
			System.out.println(RobotType.LUMBERJACK.ordinal());
		}

		doCrapAndYield();
	}

	private void runAway(RobotInfo r) {
		MapLocation enemyLoc = r.getLocation();
		Direction dir = enemyLoc.directionTo(rc.getLocation());
		target = rc.getLocation().add(dir);
	}

	MapLocation targetNearestArchon() throws GameActionException {
		float closestDis = 100000;
		int ind = 0;
		for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
			float x = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3);
			float y = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3 + 1);
			int ID = rc.readBroadcast(ARCHON_LOCATIONS + i * 3 + 2);
			MapLocation loc = new MapLocation(x, y);
			if (rc.getLocation().distanceTo(loc) < closestDis) {
				closestDis = rc.getLocation().distanceTo(loc);
				ind = ARCHON_LOCATIONS + i * 3;
			}
		}
		float x = rc.readBroadcastFloat(ind);
		float y = rc.readBroadcastFloat(ind + 1);
		return new MapLocation(x, y);
	}

	private void highTarget(RobotInfo r) throws GameActionException {
		// See if TARGET is already reported
		for (int i = 0; i < NUMBER_OF_TARGETS; i++) {
			int ind = HIGH_PRIORITY_TARGET_OFFSET + i * 4;
			if (rc.readBroadcast(ind + 2) == r.ID) {
				rc.broadcastFloat(ind, r.location.x);
				rc.broadcastFloat(ind + 1, r.location.y);
				rc.broadcast(ind + 3, rc.getRoundNum());
				return;
			}
		}
		// See if there is empty slot for TARGET
		for (int i = 0; i < NUMBER_OF_TARGETS; i++) {
			int ind = HIGH_PRIORITY_TARGET_OFFSET + i * 4;
			if (rc.readBroadcast(ind + 2) == 0) {
				rc.broadcastFloat(ind, r.location.x);
				rc.broadcastFloat(ind + 1, r.location.y);
				rc.broadcast(ind + 2, r.ID);
				rc.broadcast(ind + 3, rc.getRoundNum());
				return;
			}
		}
		if (debug) {
			rc.setIndicatorLine(rc.getLocation(), r.getLocation(), 200, 0, 200);
		}
	}

	boolean needsMoreGardeners() throws GameActionException {
		int gardenerCnt = rc.readBroadcast(RobotType.GARDENER.ordinal());
		int settledGardener = rc.readBroadcast(SETTLED_GARDENER);

		return (gardenerCnt < rc.getInitialArchonLocations(ally).length) || (gardenerCnt <= settledGardener + 2)
				|| (rc.getTreeCount() < 5) || lastGardener + 400 < rc.getRoundNum();
	}

	boolean isLikelyGoodLocation(MapLocation testLocation) throws GameActionException {
		// Check if the Circle is Clear if so return true
		boolean isCircleClear = rc.isCircleOccupiedExceptByThisRobot(testLocation, RobotType.GARDENER.bodyRadius
				+ GameConstants.BULLET_TREE_RADIUS + GameConstants.GENERAL_SPAWN_OFFSET + 2);
		if (!isCircleClear) {
			return true;
		}
		// Look at each Location in 3 by Three Box with testLocation as its
		// center
		int numOfObjects = 0;
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				MapLocation testBox = testLocation.translate(i, j);
				if (rc.isLocationOccupied(testBox)) {
					numOfObjects++;
				}
			}
		}
		if (numOfObjects < 2) {
			return true;
		}
		return false;
	}

}
