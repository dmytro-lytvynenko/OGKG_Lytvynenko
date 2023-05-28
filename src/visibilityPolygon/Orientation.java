package visibilityPolygon;

public enum Orientation {
	COUNTERCLOCKWISE, CLOCKWISE, COLLINEAR;

	public int toInt() {
		if (this == Orientation.COUNTERCLOCKWISE)
			return 1;
		else if (this == Orientation.CLOCKWISE)
			return -1;
		else
			return 0;
	}
}
