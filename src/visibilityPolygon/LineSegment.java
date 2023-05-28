package visibilityPolygon;

import visibilityPolygon.CommonUtils.Direction;
import visibilityPolygon.CommonUtils.Ray2D;

import java.awt.geom.Point2D;
import java.util.Objects;

public class LineSegment {

	Point2D a;
	Point2D b;
	
	public LineSegment(Point2D a, Point2D b) {
		this.a = a;
		this.b = b;		
	}
	
	public LineSegment(PolarPoint2D a, PolarPoint2D b) {
		this.a = a.toCartesian();
		this.b = b.toCartesian();		
	}

	public static boolean pointOnSegment(Point2D p, LineSegment s) {

		if (s.a.equals(p) || s.b.equals(p)) return true;
		
		if (Math.abs(CommonUtils.crossProduct(s.a, s.b, p)) > CommonUtils.Eps) return false;
		
		double dotProduct = CommonUtils.dotProduct(s.a, s.b, p);
		if (dotProduct < 0) return false;
		
		boolean ret = dotProduct < CommonUtils.squaredLengthBA(s.a, s.b) ||
						dotProduct == CommonUtils.squaredLengthBA(s.a, s.b);
		
		return ret;
	}

	public boolean intersectProper(LineSegment s2) {

		Point2D c = s2.a;
		Point2D d = s2.b;
		
		boolean ret = CommonUtils.crossProduct(a, b, c) * CommonUtils.crossProduct(a, b, d) < 0 &&
				CommonUtils.crossProduct(c, d, a) * CommonUtils.crossProduct(c, d, b) < 0;
				
		return ret;
	}

	public static Point2D intersectSegments(LineSegment seg1, LineSegment seg2) {

		if (CommonUtils.epsEquals(seg1.a, seg1.b) && pointOnSegment(seg1.a, seg2)) {
			return seg1.a;
		}

		if (CommonUtils.epsEquals(seg2.a, seg2.b) && pointOnSegment(seg2.a, seg1)) {
			return seg2.a;
		}
		
		if (CommonUtils.epsEquals(seg1.a, seg2.a) || CommonUtils.epsEquals(seg1.a, seg2.b))
			return seg1.a;
		
		if (CommonUtils.epsEquals(seg1.b, seg2.a) || CommonUtils.epsEquals(seg1.b, seg2.b))
			return seg1.b;
		
		Point2D p = seg1.a;
		Direction r = new Direction(seg1.b.getX() - seg1.a.getX(), seg1.b.getY() - seg1.a.getY());
		Point2D q = seg2.a;
		Direction s = new Direction(seg2.b.getX() - seg2.a.getX(), seg2.b.getY() - seg2.a.getY());

		Direction qMinusp = new Direction(q.getX() - p.getX(), q.getY() - p.getY());

		if (CommonUtils.epsEquals(r.crossProd(s), 0.0) &&
				CommonUtils.epsEquals(qMinusp.crossProd(r), 0.0)) {
			
			double t0 = (qMinusp.scalarProd(r) / r.scalarProd(r));
			double t1 = t0 + s.scalarProd(r) / r.scalarProd(r);
		
			Double intersec = lineIntervalIntersection(t0, t1, 0, 1);
			if (intersec == null)
				return null;
			else {
				r.scaleBy(Math.max(intersec, 0));
				Point2D res = new Point2D.Double(p.getX() + r.dx, p.getY() + r.dy);
				return res;
			}
			
		}

		if (CommonUtils.epsEquals(r.crossProd(s), 0.0) && CommonUtils.epsNEquals(new Direction(q.getX(), q.getY()).crossProd(r), 0.0))
			return null;

		double u = qMinusp.crossProd(r) / r.crossProd(s);
		double t = qMinusp.crossProd(s) / r.crossProd(s);
		
		if (CommonUtils.epsNEquals(r.crossProd(s), 0.0) &&
			CommonUtils.epsLEQ(0.0, u) &&
			CommonUtils.epsLEQ(u, 1.0) &&
			CommonUtils.epsGEQ(t, 0.0) &&
			CommonUtils.epsLEQ(t, 1.0))
		{
				s.scaleBy(Math.max(u, 0));
				Point2D ret = new Point2D.Double(q.getX() + s.dx, q.getY() + s.dy);
				
				return ret;
		}

		return null;
	}
	
	public static Point2D intersectSegmentWithRay(LineSegment seg, Ray2D ray) {
		double phi = ray.phi;

		if (CommonUtils.epsEquals(seg.a, seg.b)) {
			double sphi = new PolarPoint2D(seg.a).theta;
			
			if (CommonUtils.epsEquals(Math.cos(sphi), Math.cos(phi)) &&
					CommonUtils.epsEquals(Math.sin(sphi), Math.sin(phi))) {
				return seg.a;
			} else
				return null;
		}
		
		Point2D p = ray.origin;
		Direction r = new Direction(Math.cos(phi), Math.sin(phi));
		Point2D q = seg.a;
		Direction s = new Direction(seg.b.getX() - seg.a.getX(), seg.b.getY() - seg.a.getY());

		Direction qMinusp = new Direction(q.getX() - p.getX(), q.getY() - p.getY());

		if (CommonUtils.epsEquals(r.crossProd(s), 0.0) &&
				CommonUtils.epsEquals(qMinusp.crossProd(r), 0.0)) {
			
			double t0 = qMinusp.scalarProd(r) / r.scalarProd(r);
			double t1 = t0 + s.scalarProd(r) / r.scalarProd(r);
			
			double tt = Math.max(t0, t1);
			
			if (CommonUtils.epsGEQ(tt, 0.0)) {
				r.scaleBy(Math.max(tt, 0));
				Point2D ret = new Point2D.Double(p.getX() + r.dx, p.getY() + r.dy);
				
				assert(pointOnSegment(ret, seg));
				return ret;
			} else
				return null;
		}

		if (CommonUtils.epsEquals(r.crossProd(s), 0.0) && CommonUtils.epsNEquals(new Direction(q.getX(), q.getY()).crossProd(r), 0.0))
			return null;

		double u = qMinusp.crossProd(r) / r.crossProd(s);
		double t = qMinusp.crossProd(s) / r.crossProd(s);
		
		if (CommonUtils.epsNEquals(r.crossProd(s), 0.0) &&
			CommonUtils.epsLEQ(0.0, u) &&
			CommonUtils.epsLEQ(u, 1.0) &&
			CommonUtils.epsGEQ(t, 0.0)) 
		{
			s.scaleBy(Math.max(u, 0));
			Point2D ret = new Point2D.Double(q.getX() + s.dx, q.getY() + s.dy);
			
			assert(pointOnSegment(ret, seg));
			return ret;
		}

		return null;
	}
	
	private static Double lineIntervalIntersection(double a1, double b1, double c1, double d1) {
		double a, b, c, d;
		
		if (CommonUtils.epsLEQ(a1, b1)) {
			a = a1;
			b = b1;
		} else {
			a = b1;
			b = a1;
		}
		
		if (CommonUtils.epsLEQ(c1, d1)) {
			c = c1;
			d = d1;
		} else {
			c = d1;
			d = c1;
		}
		
		assert(CommonUtils.epsLEQ(a, b));
		assert(CommonUtils.epsLEQ(c, d));

		if (CommonUtils.epsLEQ(a, c) && CommonUtils.epsLEQ(d, b)) return c;
		if (CommonUtils.epsLEQ(c, a) && CommonUtils.epsLEQ(b, d)) return a;

		if (CommonUtils.epsLEQ(a, c) && CommonUtils.epsLEQ(c, b)) return c;
		if (CommonUtils.epsLEQ(c, a) && CommonUtils.epsLEQ(a, d)) return a;

		if (CommonUtils.epsLEQ(a, d) && CommonUtils.epsLEQ(d, b)) return d;
		if (CommonUtils.epsLEQ(c, b) && CommonUtils.epsLEQ(b, d)) return b;
		
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LineSegment that = (LineSegment) o;
		return a.equals(that.a) && b.equals(that.b);
	}

	@Override
	public int hashCode() {
		return Objects.hash(a, b);
	}
}
