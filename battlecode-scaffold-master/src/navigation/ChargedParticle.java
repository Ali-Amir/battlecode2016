package navigation;

import battlecode.common.MapLocation;
import utils.Turn;

/**
 * Represents a charged particle in a potential field. Charge represents the
 * strength with which a particle attracts/repulses other particles. Not that
 * charge refers to an imaginary charge (not a real physical property).
 * 
 * @author aliamir
 */
public class ChargedParticle {
	// Charge value.
	private final double charge;
	// Charge location.
	private final MapLocation location;
	// Turn at which the particle expires.
	private final int expiryTurn;

	/**
	 * Creates a new charge.
	 * 
	 * @param charge
	 *            Charge value.
	 * @param location
	 *            Charge location.
	 * @param lifetime
	 *            Number of turns that the particle is valid for. 1 means it is
	 *            valid only for the current turn.
	 */
	ChargedParticle(double charge, MapLocation location, int lifetime) {
		this.charge = charge;
		this.location = location;
		this.expiryTurn = lifetime + Turn.currentTurn();
	}

	/**
	 * Determines force exerted by this on particle at location to.
	 * 
	 * @param to
	 *            Target location.
	 * @return Force exerted by this.
	 */
	Vector force(MapLocation to) {
		if (Turn.currentTurn() >= expiryTurn) {
			return new Vector(0.0, 0.0);
		}
		return new Vector((location.x - to.x) * charge, (location.y - to.y) * charge);
	}
	
	public boolean isAlive() {
		return Turn.currentTurn() < expiryTurn;
	}
}

/**
 * Represents a 2D vector.
 * 
 * @author aliamir
 */
class Vector {
	// x - coordinate of the vector.
	private final double x;
	// y - coordinate of the vector.
	private final double y;

	/**
	 * Creates a new instance of Vector with given coordinates.
	 * 
	 * @param x_
	 *            x coordinate.
	 * @param y_
	 *            y coordinate.
	 */
	Vector(double x_, double y_) {
		this.x = x_;
		this.y = y_;
	}

	/**
	 * @return x coordinate of this.
	 */
	public double x() {
		return x;
	}

	/**
	 * @return y coordinate of this.
	 */
	public double y() {
		return y;
	}
}
