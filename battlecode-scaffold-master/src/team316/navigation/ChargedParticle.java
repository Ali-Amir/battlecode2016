package team316.navigation;

import battlecode.common.MapLocation;
import team316.utils.Turn;
import team316.utils.Vector;

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
	
	private final int id;

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
	public ChargedParticle(double charge, MapLocation location, int lifetime) {
		this.id = -1;
		this.charge = charge;
		this.location = location;
		this.expiryTurn = lifetime + Turn.currentTurn();
	}
	/**
	 * Creates a new charge.
	 * 
	 * @param id
	 *            id of the given charge.
	 * 
	 * @param charge
	 *            Charge value.
	 * @param location
	 *            Charge location.
	 * @param lifetime
	 *            Number of turns that the particle is valid for. 1 means it is
	 *            valid only for the current turn.
	 */
	public ChargedParticle(int id, double charge, MapLocation location, int lifetime) {
		this.id  = id;
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
	public Vector force(MapLocation to) {
		if (Turn.currentTurn() >= expiryTurn) {
			return new Vector(0.0, 0.0);
		}
		return new Vector((location.x - to.x) * charge, (location.y - to.y) * charge);
	}
	
	public boolean isAlive() {
		return Turn.currentTurn() < expiryTurn;
	}
	public int getID(){
		return this.id;
	}
	@Override
	public String toString() {
		return this.charge + "(" + location.x + "," + location.y + ")";
	}
}
