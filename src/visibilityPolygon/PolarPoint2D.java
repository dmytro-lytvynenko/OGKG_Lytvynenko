package visibilityPolygon;

import java.awt.geom.Point2D;

public class PolarPoint2D {
	double r;
	double theta;

	public double getR() { return r; }
	public double getTheta() { return theta; }

	public PolarPoint2D(double r, double theta) {
		this.r = r;
		this.theta = theta;
	}

	public PolarPoint2D(Point2D p) {
		PolarPoint2D temp = cartesianToPolar(p);
		r = temp.r;
		theta = temp.theta;
	}

	public Point2D toCartesian() {
		return polarToCartesian(this);
	}

	public static PolarPoint2D cartesianToPolar(Point2D p) {
		double r = Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
		double theta = Math.atan2(p.getY(), p.getX());

		return new PolarPoint2D(r, theta);
	}

	public static Point2D.Double polarToCartesian(PolarPoint2D p) {
		double x = Math.cos(p.theta) * p.r;
		double y = Math.sin(p.theta) * p.r;

		return new Point2D.Double(x, y);
	}

	public boolean isOrigin() {
		return CommonUtils.epsEquals(r, 0);
	}

	public void rotateClockWise(double theta) {
		this.theta -= theta;
		normalize(2 * Math.PI);
	}

	public void normalize(double period) {
		while (theta <= 0.0)
			theta += period;

		while (theta >= period)
			theta -= period;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;

		if (o == null)
			return false;

		if (getClass() != o.getClass())
			return false;
		
		PolarPoint2D p = (PolarPoint2D) o;

		return CommonUtils.epsEquals(r, p.r, CommonUtils.Eps) && CommonUtils.epsEquals(theta, p.theta, CommonUtils.Eps);
	}
	
	@Override
	public int hashCode() {
	    return 53 * 3 + Double.hashCode(r) + Double.hashCode(theta);
	}

	@Override
	public String toString() {
		return "[" + r + ", " + theta + "]";
	}
}
