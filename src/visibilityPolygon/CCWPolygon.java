package visibilityPolygon;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.*;

public class CCWPolygon {

	private static Point2D findCentroid(List<Point2D> points) {
		int x = 0;
		int y = 0;

		for (Point2D p : points) {
			x += p.getX();
			y += p.getY();
		}

		return new Point2D.Double((double) x / points.size(), (double) y / points.size());
	}

	private static List<Point2D> makeCCWOrder(List<Point2D> vertices) {
		Point2D center = findCentroid(vertices);
		Collections.sort(vertices, (a, b) -> {
			double a1 = (Math.toDegrees(Math.atan2(a.getY() - center.getY(), a.getX() - center.getX())) + 360) % 360;
			double a2 = (Math.toDegrees(Math.atan2(b.getY() - center.getY(), b.getX() - center.getX())) + 360) % 360;
			return (int) (a1 - a2);
		});
		return vertices;
	}

	private List<Point2D> vertices = new ArrayList<>();

	public CCWPolygon(List<Point2D> vertices){

		this.vertices = new ArrayList<>(vertices);
		//this.vertices = makeCCWOrder(this.vertices);
	}
	
	public List<Point2D> getVertices() {
		return vertices;
	}
	
	public void addVertex(Point2D newVertex) {
		vertices.add(newVertex);
	}

	public List<LineSegment> getEdges() {
		
		List<LineSegment> ret = new LinkedList<LineSegment>();
		
		int n = vertices.size();
		
		for (int i = 1; i < n; i++) {
			ret.add(new LineSegment(vertices.get(i-1), vertices.get(i)));
		}
		
		// closes the polygon
		ret.add(new LineSegment(vertices.get(n-1), vertices.get(0)));
		
		assert(ret.get(0).a.equals(ret.get(n-1).b));
		
		return ret;
	}

	public boolean visibleFromOrigin(Point2D v) {
		
		List<LineSegment> es = getEdges();
		LineSegment e = new LineSegment(CommonUtils.origin2D, v);
		
		for (LineSegment curr : es) {
			if (curr.intersectProper(e))
				return false;
		}

		return true;
	}

	public CCWPolygon shiftToOrigin(Point2D p) {
		List<Point2D> shiftedPol = new ArrayList<>();
		
		for (Point2D curr : vertices) {
			shiftedPol.add(new Point2D.Double(curr.getX() - p.getX(), curr.getY() - p.getY()));
		}
		
		return new CCWPolygon(shiftedPol);
	}

	public Path2D getPolygon() {
		Path2D pol = new Path2D.Double();
		
		pol.moveTo(vertices.get(0).getX(), vertices.get(0).getY());
		
		for (Point2D curr : vertices) {
			pol.lineTo(curr.getX(), curr.getY());
		}
		
		pol.closePath();
		
		return pol;
	}

	public CCWPolygon scale(double x) {
		List<Point2D> scaledPol = new ArrayList<>();
		
		for (Point2D curr : vertices) {
			scaledPol.add(new Point2D.Double(curr.getX()*x, curr.getY()*x));
		}
		
		return new CCWPolygon(scaledPol);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CCWPolygon that = (CCWPolygon) o;
		return Objects.equals(vertices, that.vertices);
	}

	@Override
	public int hashCode() {
		return Objects.hash(vertices);
	}
}

