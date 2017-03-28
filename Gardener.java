package TheVansPage;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Gardener extends Robot {

	TreeInfo[] myTrees = null;

	Direction[] treeDirs = { new Direction(0), new Direction((float) (Math.PI) / 3),
			new Direction((float) (Math.PI * 2) / 3), new Direction((float) Math.PI),
			new Direction((float) (Math.PI * 5) / 3), new Direction((float) (Math.PI * 4) / 3) };

	MapLocation[] myTreeLocs = null;

	int[] myTreeIds = null;

	boolean isTrappedByTrees = false;

	boolean hasHome = false;

	int timeLooking = 0;

	int totalTimeLooking = 0;
	
	boolean didILumber = false;

	Direction dirFromArchon;

	Gardener(RobotController rc) {
		super(rc);
		myTreeLocs = new MapLocation[treeDirs.length];
		RobotInfo[] robots = rc.senseNearbyRobots(-1, ally);
		for (RobotInfo r : robots) {
			if (r.type == RobotType.ARCHON) {
				dirFromArchon = r.getLocation().directionTo(rc.getLocation());
				break;
			}
		}
	}

	@Override
	void onUpdate() throws GameActionException {
		/*
		//Create 1 lumberjack.
		if (!didILumber) {
//<<<<<<< HEAD
            if (buildRobot(RobotType.LUMBERJACK)) {
                buildRobot(RobotType.LUMBERJACK);
//=======
			Direction dir = rc.getLocation().directionTo(initialArchonLocations[0]);
            if (rc.canBuildRobot(RobotType.LUMBERJACK,dir)) {
                rc.buildRobot(RobotType.LUMBERJACK,dir);
//>>>>>>> origin/master
                didILumber = true;
            }	
        }
		*/
		if (debug && hasHome) {
			for (int i = 0; i < myTreeLocs.length; i++) {
				if (rc.isLocationOccupiedByTree(myTreeLocs[i])) {
					rc.setIndicatorDot(myTreeLocs[i], 0, 200, 200);
				} else {
					rc.setIndicatorDot(myTreeLocs[i], 0, 200, 0);
				}
			}
		}

		if (!hasHome) {
			totalTimeLooking++;
			if (totalTimeLooking > (rc.readBroadcast(SETTLED_GARDENER) < 2 ? 100 : 250)) {
				if (rc.hasRobotBuildRequirements(RobotType.LUMBERJACK)
						&& rc.readBroadcast(RobotType.LUMBERJACK.ordinal()) < LUMBERJACK_MAX
						&& rc.senseNearbyTrees(-1, Team.NEUTRAL).length > 3) {
					Direction dir = randomDirection();
					if (rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
						rc.buildRobot(RobotType.LUMBERJACK, dir);
						rc.broadcast(RobotType.LUMBERJACK.ordinal(),
								rc.readBroadcast(RobotType.LUMBERJACK.ordinal()) + 1);
					}
				}
				hasHome = true;
				rc.broadcast(SETTLED_GARDENER, rc.readBroadcast(SETTLED_GARDENER) + 1);
				for (int i = 0; i < myTreeLocs.length; i++) {
					myTreeLocs[i] = rc.getLocation().add(treeDirs[i], 1);
				}
			}
			if (target == null) {
				//target = moveAwayFromArchon();
				if (target == null) {
					target = lookForHome();
					if (target == null) {
						target = sweepArea();
					}
				}
				timeLooking = 0;
			}
			
			
			if (target != null && !rc.hasMoved()) {
				if (debug) {
					rc.setIndicatorLine(rc.getLocation(), target, 200, 200, 200);
				}
				bugPathToLoc(target);
				timeLooking++;
				if (target.distanceTo(rc.getLocation()) < 0.25f) {
					if (likelyHome(rc.getLocation())) {
						hasHome = true;
						rc.broadcast(SETTLED_GARDENER, rc.readBroadcast(SETTLED_GARDENER) + 1);
						for (int i = 0; i < myTreeLocs.length; i++) {
							myTreeLocs[i] = rc.getLocation().add(treeDirs[i], 1);
						}
					} else {
						target = null;
					}
				}
				if (target != null) {
					if (rc.getLocation().distanceTo(target) > 7 || timeLooking > 15) {
						target = null;
					}
				}
			}
		} else {
			tryPlantTrees();
			tryWater();
		}

		tryShakeTrees();
		tryBuildRobots();

		doCrapAndYield();
	}
	

	private MapLocation moveAwayFromArchon() {
		if (rc.canMove(dirFromArchon)) {
			return rc.getLocation().add(dirFromArchon);
		}
		return null;
	}

	boolean likelyHome(MapLocation target) throws GameActionException {
		int problems = 0;
		for (int i = 0; i < treeDirs.length; i++) {
			MapLocation testLoc = rc.getLocation().add(treeDirs[i], 2);
			if (rc.isCircleOccupiedExceptByThisRobot(testLoc, 1)) {
				problems++;
			}
		}
		return (problems < 2) && spawnPoint.distanceTo(rc.getLocation()) > 8;
	}

	/**
	 * Builds a unit of secified type if possible
	 * 
	 * @throws GameActionException
	 */
	boolean buildRobot(RobotType type) throws GameActionException {
		if (rc.hasRobotBuildRequirements(type)) {
			for (int i = 0; i < 10; i++) {
				Direction dir = randomDirection();
				if (rc.canBuildRobot(type, dir)) {
					rc.buildRobot(type, dir);
					rc.broadcast(type.ordinal(), rc.readBroadcast(type.ordinal()) + 1);
					return true;
				}
			}
		}
		return false;
	}

	boolean needsBattleScouts() {
		return (rc.getOpponentVictoryPoints() > 250 && rc.getRoundNum() < 1000);
	}

	/**
	 * Checks if more soldiers are needed a bit rough
	 * 
	 * @throws GameActionException
	 */
	boolean needsMoreSoldiers() throws GameActionException {

		return (rc.readBroadcast(RobotType.SOLDIER.ordinal()) < 15) && (rc.getRoundNum() < 2500);
	}
	
	/**
	 * Checks if more tanks are needed
	 * 
	 * @return
	 * @throws GameActionException
	 */
	boolean needsMoreTanks() throws GameActionException {
		// Tanks are limited to 15 (same as soldiers)
		// Tanks are only built if we are in combat
		return (rc.readBroadcast(RobotType.TANK.ordinal()) < 15 && rc.readBroadcast(LAST_BULLET_FIRED) + 5 >= rc.getRoundNum());
	}

	boolean needsMoreLumberjacks() throws GameActionException {
		if (rc.readBroadcastBoolean(NEED_LUMBER)) {
			rc.broadcastBoolean(NEED_LUMBER, false);
			return true;
		}
		return ((rc.readBroadcast(RobotType.LUMBERJACK.ordinal()) < LUMBERJACK_MAX) && (rc.senseNearbyTrees(-1, Team.NEUTRAL).length > 3));	
	}
	private void tryBuildRobots() throws GameActionException {
		/*
		 * if (needsBattleScouts() &&
		 * rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
		 * buildRobot(RobotType.SCOUT); } else
		 */ if (rc.hasRobotBuildRequirements(RobotType.SCOUT) && rc.readBroadcast(SCOUT_STATUS) + 2 < rc.getRoundNum()
				&& rc.readBroadcast(RobotType.SCOUT.ordinal()) < ((rc.getRoundNum() < 1500) ? 5 : 10)) {
			buildRobot(RobotType.SCOUT);
		} else if ((rc.hasRobotBuildRequirements(RobotType.LUMBERJACK) && needsMoreLumberjacks())) {
			buildRobot(RobotType.LUMBERJACK);
		} else if (rc.hasRobotBuildRequirements(RobotType.SOLDIER) && needsMoreSoldiers()) {
			buildRobot(RobotType.SOLDIER);
		} else if (rc.hasRobotBuildRequirements(RobotType.TANK) && needsMoreTanks()) {
			buildRobot(RobotType.TANK);
		}
	}

	private void tryWater() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam());

		for (TreeInfo t : trees) {
			if (t.health < GameConstants.BULLET_TREE_MAX_HEALTH - 5) {
				if (rc.canWater(t.ID)) {
					rc.water(t.ID);
					// return;
				}
			}
		}
	}

	void tryShakeTree() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, ally);
		for (TreeInfo t : trees) {
			if (rc.canShake(t.ID)) {
				rc.shake(t.ID);
				// return;
			}
		}
	}

	private void checkIfTrapped() throws GameActionException {
		int num = 0;
		for (int i = 0; i < myTreeLocs.length; i++) {
			if (rc.isLocationOccupied(myTreeLocs[i])) {
				num++;
			}
		}
		if (num > 5) {
			isTrappedByTrees = true;
		}
	}

	private void tryPlantTrees() throws GameActionException {
		for (int i = 0; i < treeDirs.length; i++) {
			Direction dir = treeDirs[i];
			if (rc.canPlantTree(dir)) {
				rc.plantTree(dir);
				return;
			}
		}
	}

}
