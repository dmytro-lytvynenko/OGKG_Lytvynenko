package visibilityPolygon;

import visibilityPolygon.CommonUtils.Pair;
import visibilityPolygon.CommonUtils.Ray2D;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

public class VisibilityPolygon {

	public static CCWPolygon computeVisPol(CCWPolygon pol, Point2D z) {

		if (pol.getVertices().size() < 3)
			return null;

		return compute(pol, z);
	}
	

	public static List<CCWPolygon> computeVisPol(CCWPolygon inputPol, List<Point2D> viewPoints) {

		if (inputPol.getVertices().size() < 3)
			return null;

		
		List<CCWPolygon> visPolygons = new ArrayList<>();

		for (Point2D curr : viewPoints) {
			CCWPolygon currVP = compute(inputPol, curr);
			visPolygons.add(currVP);
		}

		assert(visPolygons.size() == viewPoints.size());
		
		return visPolygons;
	}

	private static CCWPolygon compute(CCWPolygon pol, Point2D z) {
		Pair<VsRep, Double> temp = preprocess(pol, z);

		VsRep vs = temp.first;
		double initAngle = temp.second;

		VertDispl v0 = vs.get(0);

		Deque<VertDispl> s0 = new LinkedList<>();
		s0.push(v0);

		assert (vs.n > 1);

		List<VertDispl> s;
		if (CommonUtils.epsGEQ(vs.get(1).getDisplacement(), v0.getDisplacement()))
			s = advance(vs, s0, 0);
		else
			s = scan(vs, s0, 0, null, Orientation.CLOCKWISE);

		assert (CommonUtils.epsEquals(s.get(s.size() - 1).getPoint().toCartesian(), v0.getPoint().toCartesian()));

		return postprocess(s, vs, z, initAngle);
	}

	private static List<VertDispl> advance(VsRep v, Deque<VertDispl> s, int iprev) {

		int n = v.n - 1;

		assert (iprev + 1 <= n);

		if (CommonUtils.epsLEQ(v.get(iprev + 1).getDisplacement(), CommonUtils.PI2)) {
			int i = iprev + 1;
			s.push(v.get(i));

			if (i == n)
				return new ArrayList<VertDispl>(s);

			if (CommonUtils.epsLess(v.get(i + 1).getDisplacement(), v.get(i).getDisplacement())
					&& CommonUtils.pointTurn(v.get(i - 1).getPoint().toCartesian(), v.get(i).getPoint().toCartesian(),
							v.get(i + 1).getPoint().toCartesian()) == Orientation.CLOCKWISE) {
				
				return scan(v, s, i, null, Orientation.CLOCKWISE);
			} else if (CommonUtils.epsLess(v.get(i + 1).getDisplacement(), v.get(i).getDisplacement())
					&& CommonUtils.pointTurn(v.get(i - 1).getPoint().toCartesian(), v.get(i).getPoint().toCartesian(),
							v.get(i + 1).getPoint().toCartesian()) == Orientation.COUNTERCLOCKWISE) {

				return retard(v, s, i);
			} else {
				return advance(v, s, i);
			}
		} else {
			VertDispl v0 = v.get(0);

			if (s.peekFirst().getDisplacement() < CommonUtils.PI2) {
				Ray2D ray = new Ray2D(CommonUtils.origin2D, v0.getPoint().theta);

				Point2D isect = LineSegment.intersectSegmentWithRay(
						new LineSegment(v.get(iprev).getPoint().toCartesian(),
								v.get(iprev + 1).getPoint().toCartesian()), ray);

				assert (isect != null);

				VertDispl st = displacementInBetween(new PolarPoint2D(isect), v.get(iprev), v.get(iprev + 1));
				s.push(st);
			}

			return scan(v, s, iprev, v0, Orientation.COUNTERCLOCKWISE); // 1 is Counterclockwise
		}

	}

	private static List<VertDispl> retard(VsRep v, Deque<VertDispl> sOld, int iprev) {
		VertDispl sj1 = sOld.peekFirst();
		List<VertDispl> ssTail = new ArrayList<>(sOld).subList(1, sOld.size());
		
		Pair<Deque<VertDispl>, VertDispl> temp = locateSj(v.get(iprev), v.get(iprev + 1), sj1, ssTail);
		Deque<VertDispl> s = temp.first;
		VertDispl sjNext = temp.second;
		
		VertDispl sj = s.peekFirst();
		
		if (sj.getDisplacement() < v.get(iprev + 1).getDisplacement()) {
			int i = iprev + 1;	
			
			VertDispl vi = v.get(i);
			Point2D p = LineSegment.intersectSegmentWithRay(new LineSegment(sj.getPoint().toCartesian(),
					sjNext.getPoint().toCartesian()), new Ray2D(CommonUtils.origin2D, vi.getPoint().theta));
			VertDispl st1 = displacementInBetween(new PolarPoint2D(p), sj, sjNext);
			
			if (st1 != null)
				s.push(st1);
			
			s.push(vi);

			if (i == v.n - 1) {
				return new ArrayList<VertDispl>(s);
			}
			else if (CommonUtils.epsGEQ(v.get(i+1).getDisplacement(),
					vi.getDisplacement()) && CommonUtils.pointTurn(v.get(i-1).getPoint().toCartesian(),
					vi.getPoint().toCartesian(), v.get(i+1).getPoint().toCartesian()) == Orientation.CLOCKWISE) {
				return advance(v, s, i);
			} else if (CommonUtils.epsGreater(v.get(i+1).getDisplacement(),
					vi.getDisplacement()) && CommonUtils.pointTurn(v.get(i-1).getPoint().toCartesian(),
					vi.getPoint().toCartesian(), v.get(i+1).getPoint().toCartesian()) == Orientation.COUNTERCLOCKWISE) {
				s.pop();
				return scan(v, s, i, vi, Orientation.COUNTERCLOCKWISE);
			} else {
				s.pop();
				return retard(v, s, i);
			}
		} else {
			if (CommonUtils.epsEquals(v.get(iprev + 1).getDisplacement(), sj.getDisplacement()) &&
				CommonUtils.epsGreater(v.get(iprev + 2).getDisplacement(), v.get(iprev + 1).getDisplacement()) &&
				CommonUtils.pointTurn(v.get(iprev).getPoint().toCartesian(), v.get(iprev + 1).getPoint().toCartesian(),
						v.get(iprev + 2).getPoint().toCartesian()) == Orientation.CLOCKWISE) {
				
				s.push(v.get(iprev + 1));
				return advance(v, s, iprev + 1);
				
			} else {
				VertDispl w = intersectWithWindow(v.get(iprev), v.get(iprev + 1), sj, sjNext);

				assert(w != null);
				return scan(v, s, iprev, w, Orientation.CLOCKWISE);
			}
		}
	}

	private static List<VertDispl> scan(VsRep v, Deque<VertDispl> s, int iprev, VertDispl windowEnd, Orientation ori) {
		int i = iprev + 1;
		
		if (i+1 == v.n) return new ArrayList<VertDispl>(s);
		
		if (ori == Orientation.CLOCKWISE &&
			CommonUtils.epsGreater(v.get(i+1).getDisplacement(), s.peekFirst().getDisplacement()) && 
			CommonUtils.epsGEQ(s.peekFirst().getDisplacement(), v.get(i).getDisplacement())) {
			
			VertDispl intersec = intersectWithWindow(v.get(i), v.get(i+1), s.peekFirst(), windowEnd);
			
			if (intersec != null && !(windowEnd != null && CommonUtils.epsEquals(intersec.getPoint().toCartesian(),
					windowEnd.getPoint().toCartesian()))) {
				s.push(intersec);
				return advance(v, s, i);
			} else {
				return scan(v, s, i, windowEnd, ori);
			}
			
			
		} else if (ori == Orientation.COUNTERCLOCKWISE &&
				   CommonUtils.epsLEQ(v.get(i+1).getDisplacement(), s.peekFirst().getDisplacement()) &&
				   s.peekFirst().getDisplacement() < v.get(i).getDisplacement()) {
			
			if (intersectWithWindow(v.get(i), v.get(i+1), s.peekFirst(), windowEnd) != null) {
				return retard(v, s, i);
			} else {
				return scan(v, s, i, windowEnd, ori);
			}
		} else {
			return scan(v, s, i, windowEnd, ori);
		}
		
	}

	private static Pair<VsRep, Double> preprocess(CCWPolygon pol, Point2D z) {

		pol = pol.shiftToOrigin(z);

		boolean zIsVertex = pol.getVertices().contains(CommonUtils.origin2D);

		PolarPoint2D v0 = getInitialVertex(pol, zIsVertex);

		assert (!v0.equals(z));

		List<PolarPoint2D> l = pol.getVertices().stream().map(x -> new PolarPoint2D(x)).collect(Collectors.toList());

		placeV0First(l, v0);

		assert (l.get(0).equals(v0));

		adjustPositionOfz(l, zIsVertex, z);

		for (PolarPoint2D curr : l) {
			if (!curr.isOrigin())
				curr.rotateClockWise(v0.theta);
		}

		assert (l.get(0).theta == 0);

		return new Pair(new VsRep(l, zIsVertex), v0.theta);
	}

	private static CCWPolygon postprocess(List<VertDispl> pre_s, VsRep vs, Point2D z, double initAngle) {
		if (vs.zIsVertex)
			pre_s.add(0, new VertDispl(new PolarPoint2D(CommonUtils.origin2D), 0));

		Collections.reverse(pre_s);

		List<PolarPoint2D> rotatedPol = pre_s.stream().map(v -> v.getPoint()).collect(Collectors.toList());

		for (PolarPoint2D curr : rotatedPol) {
			curr.rotateClockWise(-initAngle);
		}

		List<Point2D> shiftedPol = rotatedPol.stream().map(v -> v.toCartesian()).collect(Collectors.toList());

		for (Point2D curr : shiftedPol)
			curr.setLocation(curr.getX() + z.getX(), curr.getY() + z.getY());

		return new CCWPolygon(shiftedPol);
	}

	private static VertDispl intersectWithWindow(VertDispl a, VertDispl b, VertDispl orig, VertDispl endpoint) {
		LineSegment s1 = new LineSegment(a.getPoint(), b.getPoint());
		
		Point2D res;
		if (endpoint != null) {
			LineSegment s2 = new LineSegment(orig.getPoint(), endpoint.getPoint());
			res = LineSegment.intersectSegments(s1, s2);
		}
		else {
			Ray2D ray = new Ray2D(orig.getPoint().toCartesian(), orig.getDisplacement());
			res = LineSegment.intersectSegmentWithRay(s1, ray);
		}
		
		return res != null ? displacementInBetween(new PolarPoint2D(res), a, b) : null;
	}

	private static VertDispl displacementInBetween(PolarPoint2D s, VertDispl v1, VertDispl v2) {
		double bot = Math.min(v1.getDisplacement(), v2.getDisplacement());
		double top = Math.max(v1.getDisplacement(), v2.getDisplacement());
		
		if (CommonUtils.epsEquals(bot, top))
			return new VertDispl(s, bot);

		
		double temp = s.theta;
		while (CommonUtils.epsGreater(temp, top))
			temp -= CommonUtils.PI2;

		while (CommonUtils.epsLess(temp, bot))
			temp += CommonUtils.PI2;

		assert (CommonUtils.epsLEQ(bot, temp) && CommonUtils.epsLEQ(temp, top));
		return new VertDispl(s, temp);
	}

	private static Pair<Deque<VertDispl>, VertDispl> locateSj(VertDispl vi, VertDispl vi1, VertDispl sj1,
			List<VertDispl> ss) {
		
		VertDispl sj = ss.get(0);
		List<VertDispl> sTail = ss.subList(1, ss.size());
		
		if (CommonUtils.epsLess(sj.getDisplacement(),
				vi1.getDisplacement()) && CommonUtils.epsLEQ(vi1.getDisplacement(), sj1.getDisplacement())) {
			return new Pair<Deque<VertDispl>, VertDispl>(new ArrayDeque<>(ss), sj1);
		}
		
		Point2D y = LineSegment.intersectSegments(new LineSegment(vi.getPoint().toCartesian(),
				vi1.getPoint().toCartesian()), new LineSegment(sj.getPoint().toCartesian(),
				sj1.getPoint().toCartesian()));
		
		if (y != null && CommonUtils.epsLEQ(vi1.getDisplacement(), sj.getDisplacement()) &&
			CommonUtils.epsLEQ(sj.getDisplacement(), sj1.getDisplacement()) &&
			CommonUtils.epsNEquals(y, sj.getPoint().toCartesian()) && 
			CommonUtils.epsNEquals(y, sj1.getPoint().toCartesian())) {
			
			return new Pair<Deque<VertDispl>, VertDispl>(new ArrayDeque<>(ss), sj1);
		}

		return locateSj(vi, vi1, sj, sTail);
	}

	private static List<VertDispl> computeAngularDisplacements(List<PolarPoint2D> v) {

		List<VertDispl> ret = new ArrayList<>();

		for (int i = 0; i < v.size(); i++) {

			if (i == 0) {
				ret.add(new VertDispl(v.get(0), v.get(0).theta));
			} else {
				PolarPoint2D vi = v.get(i);
				PolarPoint2D viprev = v.get(i - 1);
				double phi = vi.theta;
				double rawAngle = Math.abs(phi - viprev.theta);

				assert (rawAngle < CommonUtils.PI2);

				double angle = Math.min(rawAngle, CommonUtils.PI2 - rawAngle);
				int sigma = CommonUtils.pointTurn(CommonUtils.origin2D, viprev.toCartesian(), vi.toCartesian()).toInt();
				double alpha_vi = ret.get(i - 1).getDisplacement() + sigma * angle;

				assert (Math.abs(alpha_vi - ret.get(i - 1).getDisplacement()) < CommonUtils.PI);

				ret.add(new VertDispl(vi, alpha_vi));
			}
		}

		return ret;
	}

	private static void adjustPositionOfz(List<PolarPoint2D> l, boolean zIsVertex, Point2D z) {
		if (zIsVertex) {
			boolean temp = l.remove(new PolarPoint2D(0, 0));
			assert (temp);

			l.add(0, new PolarPoint2D(0, 0));
		}
	}

	private static void placeV0First(List<PolarPoint2D> l, PolarPoint2D v0) {
		assert (l.contains(v0));
		assert (l.indexOf(v0) == l.lastIndexOf(v0));

		Collections.rotate(l, -l.indexOf(v0));

		assert (l.get(0).equals(v0));
	}

	private static PolarPoint2D getInitialVertex(CCWPolygon shiftedPol, boolean zIsVertex) {
		List<LineSegment> es = shiftedPol.getEdges();
		List<Point2D> vs = shiftedPol.getVertices();

		if (zIsVertex) {
			Point2D ret;

			for (LineSegment curr : es) {
				if (CommonUtils.epsEquals(curr.a, CommonUtils.origin2D, CommonUtils.Eps))
					return new PolarPoint2D(curr.b);
			}

		}

		for (LineSegment curr : es) {
			if (curr.pointOnSegment(CommonUtils.origin2D, curr))
				return new PolarPoint2D(curr.b);
		}

		List<Point2D> visible = new ArrayList<Point2D>();

		for (Point2D v : vs) {
			if (shiftedPol.visibleFromOrigin(v))
				visible.add(v);
		}

		assert (!visible.isEmpty());

		List<PolarPoint2D> visiblePolar = new ArrayList<>();
		for (Point2D curr : visible)
			visiblePolar.add(new PolarPoint2D(curr));

		PolarPoint2D closestVisibleVertex = visiblePolar.get(0);

		for (PolarPoint2D curr : visiblePolar) {
			if (curr.r < closestVisibleVertex.r && (curr.r > 0 || CommonUtils.epsEquals(curr.r, 0, CommonUtils.Eps)))
				closestVisibleVertex = curr;
		}

		return closestVisibleVertex;
	}

	public static class VsRep {
		public List<VertDispl> v;
		public boolean zIsVertex;
		int n;

		public VsRep(List<PolarPoint2D> vs, boolean zIsVertex) {
			this.zIsVertex = zIsVertex;
			n = (zIsVertex) ? vs.size() - 1 : vs.size();

			if (zIsVertex)
				v = computeAngularDisplacements(vs.subList(1, vs.size()));
			else
				v = computeAngularDisplacements(vs);
		}

		public VertDispl get(int i) {
			return v.get(i);
		}

		public int size() {
			return v.size();
		}
	}
}
