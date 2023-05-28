package Utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PolygonGenerator {
    public static List<Point2D> generatePolygon(Point2D center, double avgRadius, double minRadius,
                                                double irregularity, double spikiness,
                                                int numVertices) {

        if (irregularity < 0 || irregularity > 1) {
            throw new IllegalArgumentException("Irregularity must be between 0 and 1.");
        }
        if (spikiness < 0 || spikiness > 1) {
            throw new IllegalArgumentException("Spikiness must be between 0 and 1.");
        }
        if(avgRadius <= 0) {
            throw new IllegalArgumentException("AvgRadius must be positive.");
        }
        if(minRadius > avgRadius || minRadius < 0) {
            throw new IllegalArgumentException("MinRadius must be from 0 to avgRadius.");
        }
        if(numVertices < 0) {
            throw new IllegalArgumentException("Num of vertices must be positive.");
        }

        irregularity *= 2 * Math.PI / numVertices;
        spikiness *= avgRadius;
        double[] angleSteps = randomAngleSteps(numVertices, irregularity);

        // now generate the points
        List<Point2D> points = new ArrayList<>();
        double angle = (double) (Math.random() * 2 * Math.PI);
        for (int i = 0; i < numVertices; i++) {
            double radius = clip((double) (Math.random() * avgRadius + avgRadius - spikiness), minRadius, 2 * avgRadius);
            double x = (double) (center.getX() + radius * Math.cos(angle));
            double y = (double) (center.getY() + radius * Math.sin(angle));
            points.add(new Point2D.Double(x, y));
            angle += angleSteps[i];
        }

        return points;
    }

    private static double[] randomAngleSteps(int steps, double irregularity) {
        double[] angles = new double[steps];
        double lower = (2 * (double) Math.PI / steps) - irregularity;
        double upper = (2 * (double) Math.PI / steps) + irregularity;
        double cumsum = 0;
        Random random = new Random();
        for (int i = 0; i < steps; i++) {
            double angle = lower + random.nextDouble() * (upper - lower);
            angles[i] = angle;
            cumsum += angle;
        }

        cumsum /= (2 * (double) Math.PI);
        for (int i = 0; i < steps; i++) {
            angles[i] /= cumsum;
        }
        return angles;
    }

    private static double clip(double value, double lower, double upper) {
        return Math.min(upper, Math.max(value, lower));
    }

}
