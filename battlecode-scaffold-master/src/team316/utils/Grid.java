package team316.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Grid {
	
	public static final Direction[] mainDirections = {Direction.NORTH,
			Direction.SOUTH, Direction.WEST, Direction.EAST};
	
	public static boolean isMainDirection(Direction direction) {
		return (isVertical(direction) || isHorizontal(direction));
	}
	public static boolean isVertical(Direction direction) {
		return (direction.equals(Direction.NORTH)
				|| direction.equals(Direction.SOUTH));
	}

	public static boolean isHorizontal(Direction direction) {
		return (direction.equals(Direction.WEST)
				|| direction.equals(Direction.EAST));
	}

	public static Integer getRelevantCoordinate(Direction direction,
			MapLocation location) {

		if (isVertical(direction)) {
			return location.y;
		}

		if (isHorizontal(direction)) {
			return location.x;
		}

		return null;
	}
}
