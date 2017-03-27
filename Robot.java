package KingBattleCodeV6;

import java.util.ArrayList;

import battlecode.common.*;

public abstract class Robot {

	static final boolean debug = true;

	MapLocation target = null;
	MapLocation spawnPoint = null;

	RobotController rc = null;

	Team ally = null;
	Team enemy = null;

	RobotType type = null;

	int roundAtSpawn = 0;

	MapLocation[] initialArchonLocations;
	MapLocation[] ourInitialArchonLocations;
	MapLocation[] opponentArchonLocations;
	int[] opponentArchonIds;

	float rnd = (float) Math.random();

	boolean enemyHaveBeenSpoted = false;

	boolean mapEdgesDetermined = false;

	float mapEdgeRight, mapEdgeTop, mapEdgeLeft, mapEdgeBottom;

	static final int LAST_BULLET_FIRED = 6700;
	static final int SCOUT_STATUS = 8000;
	static final int SETTLED_GARDENER = 500;
	static final int HIGH_PRIORITY_TARGET_OFFSET = 8240;
	static final int GARDENER_OFFSET = 25;
	static final int GARDENER_CAN_PROBABLY_BUILD = 40;
	static final int PRIMARY_UNIT = 50;
	static final int TARGET_OFFSET = 60;
	static final int NUMBER_OF_TARGETS = 4;
	static final int ARCHON_COUNT = 2900;
	static final int ARCHON_LOCATIONS = 2901;
	static final int ALLIED_ARCHON_LOCATIONS = 2950;
	static final int HIGH_PRIORITY = 6000;
	static final int ARCHON_BUILD_SCORE = 7001;
	static final int ENEMIES_SPOTTED = 7100;
	static final int LUMBERJACK_MAX = 15;
	static final int ENEMY_GARDENER_OFFSET = 100;
	static final int ENEMY_GARDENER_COUNT = 5;
	static final int DEFENDER_OFFSET = 1100;
	static final int ATTACKER_OFFSET = 1101;

	Robot(RobotController rc) {
		this.rc = rc;
		this.enemy = rc.getTeam().opponent();
		this.ally = rc.getTeam();
		this.spawnPoint = rc.getLocation();
		this.roundAtSpawn = rc.getRoundNum();
		this.initialArchonLocations = rc.getInitialArchonLocations(enemy);
		this.ourInitialArchonLocations = rc.getInitialArchonLocations(ally);
		this.opponentArchonIds = new int[initialArchonLocations.length];
		this.type = rc.getType();
	}

	/**
	 * Runs every round robot is alive
	 */
	abstract void onUpdate() throws GameActionException;

	/**
	 * Runs when unit is spawned
	 * 
	 * @throws GameActionException
	 */
	void init() throws GameActionException {
		if (rc.readBroadcast(PRIMARY_UNIT) != rc.getRoundNum()) {
			rc.broadcast(PRIMARY_UNIT, rc.getRoundNum());

			if (rc.getRoundNum() == 1) {
				onGameStart();
			}
		}
	}

	/**
	 * Runs one time at the very start of the game
	 * 
	 * @throws GameActionException
	 */
	void onGameStart() throws GameActionException {

		rc.broadcast(RobotType.ARCHON.ordinal(), rc.getInitialArchonLocations(ally).length);

		rc.broadcast(ARCHON_COUNT, initialArchonLocations.length);
		for (int i = 0; i < initialArchonLocations.length; i++) {
			// Broadcast enemy archons
			int ind = ARCHON_LOCATIONS + i * 3;
			rc.broadcastFloat(ind + 0, initialArchonLocations[i].x);
			rc.broadcastFloat(ind + 1, initialArchonLocations[i].y);
			rc.broadcast(ind + 2, -1);
			// Broadcast friendly archons
			ind = ALLIED_ARCHON_LOCATIONS + i * 3;
			rc.broadcastFloat(ind + 0, ourInitialArchonLocations[i].x);
			rc.broadcastFloat(ind + 1, ourInitialArchonLocations[i].y);
			rc.broadcast(ind + 2, -1);
		}
	}

	/**
	 * Returns a random Direction
	 * 
	 * @return a random Direction
	 */
	Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	/**
	 * 
	 */
	void chooseTarget() throws GameActionException {
		for (int i = 0; i < ARCHON_COUNT; i++) {

		}
	}

	/**
	 * 
	 */
	void doCrapAndYield() throws GameActionException {
		cleanUpBroadcasts();
		considerBuyingVP();
		Clock.yield();
	}

	/**
	 * @return 0 if nothing is hit, 1 if tree is hit, 2 if robot is hit
	 */
	int shootRay(Direction dir, MapLocation loc) throws GameActionException {
		for (float walk = 0; walk < type.sensorRadius; walk += 0.25f) {
			MapLocation testLoc = loc.add(dir, walk);
			if (debug) {
				rc.setIndicatorLine(rc.getLocation(), testLoc, 200, 200, 200);
			}
			if (rc.isLocationOccupiedByTree(testLoc)) {
				return 1;
			}
			if (rc.isLocationOccupiedByRobot(testLoc)) {
				return 2;
			}
		}
		return 0;
	}

	/**
	 * 
	 */
	boolean isTimeToAttackArchons() {
		return (rc.getRoundNum() > 1500 || rc.getTeamBullets() > 250);
	}

	/**
	 * Attempts to move to a given MapLocation, while avoiding small obstacles
	 * directly in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	boolean tryMove(MapLocation current, MapLocation goal) throws GameActionException {
		return tryMove(new Direction(current, goal));
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * directly in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	boolean tryMove(Direction dir) throws GameActionException {
		return tryMove(dir, 20, 3);
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * direction in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @param degreeOffset
	 *            Spacing between checked directions (degrees)
	 * @param checksPerSide
	 *            Number of extra directions checked on each side, if intended
	 *            direction was unavailable
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

		// First, try intended direction
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
		boolean moved = false;
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}

	/**
	 * 
	 */
	void cleanUpBroadcasts() throws GameActionException {
		for (int i = 0; i < ENEMY_GARDENER_COUNT; i++) {
			int ind = ENEMY_GARDENER_OFFSET + i * 4;
			if (rc.readBroadcast(ind + 3) > 100 + rc.getRoundNum()) {
				rc.broadcast(ind, 0);
				rc.broadcastFloat(ind + 1, 0);
				rc.broadcastFloat(ind + 2, 0);
				rc.broadcast(ind + 3, 0);
			}
		}
		for (int i = 0; i < NUMBER_OF_TARGETS; i++) {
			int ind = HIGH_PRIORITY_TARGET_OFFSET + i * 4;
			if (rc.readBroadcast(ind + 3) > 100 + rc.getRoundNum()) {
				rc.broadcast(ind, 0);
				rc.broadcastFloat(ind + 1, 0);
				rc.broadcastFloat(ind + 2, 0);
				rc.broadcast(ind + 3, 0);
			}
		}
	}

	MapLocation lookForHome() throws GameActionException {
		int tests = 0;
		int maxTests = 50;
		while (tests < maxTests) {
			Direction dir = randomDirection();
			for (int i = 0; i < 4; i++) {
				MapLocation testLocation = rc.getLocation().add(dir, (float) i);
				if (!rc.isCircleOccupiedExceptByThisRobot(testLocation,
						type.bodyRadius + GameConstants.BULLET_TREE_RADIUS + GameConstants.GENERAL_SPAWN_OFFSET)
						&& testLocation.distanceTo(spawnPoint) > rc.getLocation().distanceTo(spawnPoint)) {
					return testLocation;
				}
			}
			tests++;
		}
		return null;
	}

	void broadcastRobot(RobotInfo[] robots) throws GameActionException {
		for (RobotInfo r : robots) {
			switch (r.type) {
			case ARCHON:
				broadcastArchon(r);
				break;
			case GARDENER:
				broadcastGardener(r);
				break;
			case SOLDIER:
				broadcastSoldier(r);
				break;
			}
		}
	}

	private void broadcastSoldier(RobotInfo robot) {

	}

	private void broadcastArchon(RobotInfo robot) throws GameActionException {
		// First see if this Archon has already been spotted
		for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
			if (rc.readBroadcast(ARCHON_LOCATIONS + i * 3 + 2) == robot.ID) {
				rc.broadcastFloat(ARCHON_LOCATIONS + i * 3, robot.location.x);
				rc.broadcastFloat(ARCHON_LOCATIONS + i * 3 + 1, robot.location.y);
				return;
			}
		}
		// if find which Archons have not been found
		float[] xPos = new float[rc.readBroadcast(ARCHON_COUNT)];
		float[] yPos = new float[rc.readBroadcast(ARCHON_COUNT)];
		for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
			if (rc.readBroadcast(ARCHON_LOCATIONS + i * 3 + 2) == -1) {
				xPos[i] = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3);
				yPos[i] = rc.readBroadcastFloat(ARCHON_LOCATIONS + i * 3 + 1);
			} else {
				xPos[i] = -1;
				yPos[i] = -1;
			}
		}
		// Then find which of the avaliable archons is closest to the robot
		// position
		float bestDis = 10000;
		int ind = 0;
		for (int i = 0; i < rc.readBroadcast(ARCHON_COUNT); i++) {
			if (xPos[i] != -1) {
				MapLocation testLoc = new MapLocation(xPos[i], yPos[i]);
				if (robot.location.distanceTo(testLoc) < bestDis) {
					bestDis = robot.location.distanceTo(testLoc);
					ind = i;
				}
			}
		}
		rc.broadcastFloat(ARCHON_LOCATIONS + ind * 3, robot.location.x);
		rc.broadcastFloat(ARCHON_LOCATIONS + ind * 3 + 1, robot.location.y);
		rc.broadcast(ARCHON_LOCATIONS + ind * 3 + 2, robot.ID);
	}

	private void broadcastGardener(RobotInfo robot) throws GameActionException {
		// See if Gardener is already reported
		for (int i = 0; i < ENEMY_GARDENER_COUNT; i++) {
			int ind = ENEMY_GARDENER_OFFSET + i * 4;
			if (rc.readBroadcast(ind + 2) == robot.ID) {
				rc.broadcastFloat(ind, robot.location.x);
				rc.broadcastFloat(ind + 1, robot.location.y);
				rc.broadcast(ind + 3, rc.getRoundNum());
				return;
			}
		}
		// See if there is empty slot for Gardener
		for (int i = 0; i < ENEMY_GARDENER_COUNT; i++) {
			int ind = ENEMY_GARDENER_OFFSET + i * 4;
			if (rc.readBroadcast(ind + 2) == 0) {
				rc.broadcastFloat(ind, robot.location.x);
				rc.broadcastFloat(ind + 1, robot.location.y);
				rc.broadcast(ind + 2, robot.ID);
				rc.broadcast(ind + 3, rc.getRoundNum());
				return;
			}
		}
	}

	/**
	 * Sees if there are any allied robots in given direction
	 * 
	 * @param dir-
	 *            Direction to shoot
	 * @param num-
	 *            number of places to check around the dir
	 */
	boolean isConeClear(Direction dir, int off) throws GameActionException {
		// Bug
		return true;
		// Bug
		/*RobotInfo[] robots = rc.senseNearbyRobots(-1, ally);

		for (RobotInfo r : robots) {
			Direction testDir = rc.getLocation().directionTo(r.getLocation());
			float testDis = rc.getLocation().distanceTo(r.getLocation());
			if (testDir.equals(dir, ((float)Math.PI * off) / 180.0f)) {
				return false;
			}
		}
		return true;*/
	}

	/**
	 * @throws GameActionException
	 *             Looks at area around Unit mapping out good locations
	 */
	MapLocation sweepArea() throws GameActionException {
		// Center of the circle
		MapLocation center = rc.getLocation();
		// This will store what the best location is in the end
		MapLocation bestLoc = null;
		int bestScore = -1;

		for (int x = (int) (-1 * type.sensorRadius); x < type.sensorRadius; x += 2) {
			for (int y = (int) (-1 * type.sensorRadius); y < type.sensorRadius; y += 2) {
				MapLocation loc = center.translate(x, y);
				if (rc.canSenseLocation(loc)) {
					// Only checks if the loc is on the map
					if (rc.onTheMap(loc)) {
						int score = 0;
						if (rc.isLocationOccupied(loc)) {
							// If the Location is not occupied we cannot go
							// there so go next
							rc.setIndicatorDot(loc, 0, 0, 200);
							score = -1000;
						} else {
							// Look around the point
							int hits = 0;
							for (int subX = -1; subX < 2; subX++) {
								for (int subY = -1; subY < 2; subY++) {
									MapLocation subLoc = loc.translate(subX, subY);
									if (rc.canSenseLocation(subLoc)) {
										if (rc.isLocationOccupied(subLoc)) {
											hits++;
										}
										if (rc.senseRobotAtLocation(subLoc) != null) {
											score = -10000;
										}
									}
								}
							}
							score += 100 - hits * 5;
							if (score > bestScore) {
								bestScore = score;
								bestLoc = loc;
							}
							if (debug) {
								if (score >= 90) {
									rc.setIndicatorDot(loc, 200, 0, 0);
								} else if (score >= 50) {
									rc.setIndicatorDot(loc, 0, 200, 0);
								} else {
									rc.setIndicatorDot(loc, 0, 0, 200);
								}
							}
						}
					}
				}
			}
		}
		return bestLoc;
	}

	void tryShakeTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);

		for (TreeInfo t : trees) {
			if (rc.canShake(t.ID)) {
				rc.shake(t.ID);
				return;
			}
		}
	}

	void initBugPath(MapLocation target) {
		bugPathClosestPoint = rc.getLocation();
		bugPathTarget = target;
		bugPathPreviousDir = rc.getLocation().directionTo(target);
	}

	// Bug Path variables
	Direction bugPathPreviousDir;
	MapLocation bugPathClosestPoint;
	MapLocation bugPathTarget;
	// 0 if this is closest it has ever been 1 if blocked
	int bugPathStatus = 0;
	int bugPathPatience = 100;

	/**
	 * 
	 */
	void bugPathToLoc(MapLocation target) throws GameActionException {
		if (bugPathTarget == null || bugPathPreviousDir == null || bugPathClosestPoint == null) {
			initBugPath(target);
		}
		if (!target.equals(bugPathTarget)) {
			bugPathTarget = target;
			bugPathClosestPoint = rc.getLocation();
			bugPathStatus = 0;
		}
		if (rc.getRoundNum() % 100 == 0) {
			bugPathClosestPoint = rc.getLocation();
			bugPathStatus = 0;
		}
		Direction previousDir = bugPathPreviousDir;
		MapLocation closestPoint = bugPathClosestPoint;
		MapLocation previousLoc = rc.getLocation();
		if (target.equals(rc.getLocation())) {

		} else {
			if (previousLoc.distanceTo(target) <= closestPoint.distanceTo(target)) {
				bugPathClosestPoint = previousLoc;
				if (rc.canMove(target)) {
					bugPathStatus = 0;
				} else {
					bugPathStatus = 1;
					for (int i = 0; i < 24; i++) {
						Direction testDir = previousDir.rotateLeftDegrees(360.0f * i / 24f);
						if (rc.canMove(testDir) && !rc.hasMoved()) {
							rc.move(testDir);
							bugPathPreviousDir = rc.getLocation().directionTo(target);
						}
					}
				}
			} else {
				bugPathStatus = 1;
			}

			if (bugPathStatus == 0) {
				if (!rc.hasMoved()) {
					rc.move(target);
					bugPathPreviousDir = previousLoc.directionTo(rc.getLocation());
				}
			} else {

				boolean obstacle = false;
				Direction testDir = null;
				int temp = 0;
				for (int i = 0; i < 24; i++) {
					temp = i;
					testDir = previousDir.rotateRightDegrees(180.0f - 360f * i / 24.0f);
					if (!rc.canMove(testDir)) {
						obstacle = true;
						break;
					}
				}
				if (!obstacle && !rc.hasMoved() && rc.canMove(target)) {
					rc.move(target);
					bugPathPreviousDir = previousLoc.directionTo(rc.getLocation());
				}

				boolean progress = false;
				for (int i = temp; i < 24; i++) {
					testDir = previousDir.rotateRightDegrees(180.0f - 360.0f * i / 24.0f);
					if (rc.canMove(testDir)) {
						progress = true;
						break;
					}
				}
				if (progress && !rc.hasMoved()) {
					rc.move(testDir);
					bugPathPreviousDir = previousLoc.directionTo(rc.getLocation());
				}
				if (rc.getLocation().distanceTo(target) < closestPoint.distanceTo(target)) {
					bugPathClosestPoint = rc.getLocation();
				}
			}
		}
	}

	/**
	 * 
	 */
	void considerBuyingVP() throws GameActionException {
		if (rc.getTeamBullets() > 500) {
			rc.donate(rc.getTeamBullets() - 500);
		}
	}

	/**
	 * A slightly more complicated example function, this returns true if the
	 * given bullet is on a collision course with the current robot. Doesn't
	 * take into account objects between the bullet and this robot.
	 *
	 * @param bullet
	 *            The bullet in question
	 * @return True if the line of the bullet's path intersects with this
	 *         robot's current position.
	 */
	boolean willCollideWithMe(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();

		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and
		// we can break early
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to
		// know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and
		// intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our
		// location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh
																					// cah
																					// toa
																					// :)

		return (perpendicularDist <= rc.getType().bodyRadius);
	}
}
