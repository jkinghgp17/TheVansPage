package TheVansPage;

import java.util.ArrayList;
import java.util.List;
import battlecode.common.BulletInfo;
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

public class Lumberjack extends Robot {

	List<MapLocation> badTargets;

	Lumberjack(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
		target = initialArchonLocations[(int) (rnd * initialArchonLocations.length)];
		badTargets = new ArrayList<MapLocation>();
	}

	@Override
	void onUpdate() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(2, Team.NEUTRAL);
		if (trees.length > 0) {
			if (rc.canChop(trees[0].ID)) {
				rc.chop(trees[0].ID);
			}
		}

		// See if there are any enemy robots within striking range
		// (distance 1 from lumberjack's radius)
		/*
		RobotInfo[] robotsStrike = rc.senseNearbyRobots(
				RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

		if (robotsStrike.length > 0 && !rc.hasAttacked()) {
			// Use strike() to hit all nearby robots!
			rc.strike();
		}*/

		/*
		// See if there are any nearby enemy robots
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

		// See if there are any nearby bullets
		BulletInfo[] bullets = rc.senseNearbyBullets(-1);

		if (trees.length > 0) {
			target = trees[0].getLocation();
		} else if (robots.length > 0) {
			target = robots[0].getLocation();
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
						if (mp.equals(loc)) {
							continue;
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
			} else {
				if (rc.getLocation().distanceTo(target) < 0.50f) {
					badTargets.add(target);
					target = null;
				}
			}
		}*/

		/*
		for (BulletInfo b : bullets) {
			if (willCollideWithMe(b)) {
				if (!rc.hasMoved())
					tryMove(b.getDir().rotateLeftDegrees(90));
			}
		}*/
		
		//Deforestation
    	///
    	
<<<<<<< HEAD
         TreeInfo[] treesTwo = rc.senseNearbyTrees();
=======
         trees = rc.senseNearbyTrees();
>>>>>>> origin/master

         MapLocation minMove = null;
         for (TreeInfo t : treesTwo) {
        	 if (minMove == null || (rc.getLocation().distanceTo(t.location) < rc.getLocation().distanceTo(minMove))) {
        		 if (t.team==Team.NEUTRAL) {
        			 minMove = t.location;
        			 rc.setIndicatorDot(minMove, 0, 255, 255);
        		 }
        	 }
         }
         
         for (TreeInfo t : trees) {
        	 
        	 if (rc.canShake(t.getLocation()) && t.containedBullets > 0 && t.team==Team.NEUTRAL) {
                 rc.shake(t.getLocation());  
             }
        	 
             if (rc.canChop(t.getLocation()) && t.team==Team.NEUTRAL) {
                 rc.chop(t.getLocation());
             }
         }
		
		if (!rc.hasMoved() && target != null) {
			tryMove(rc.getLocation(), target);
			/*if (!rc.hasMoved()) {
				bugPathToLoc(target);
			}*/
		}

		//tryShakeTrees();

		// Clock.yield() makes the robot wait until the next turn, then
		// it will perform this loop again
		Clock.yield();
	}

}
