package team316.utils;


/**
 * Represents a 2D vector.
 * 
 * @author aliamir
 */
public class Vector {
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
	public Vector(double x_, double y_) {
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