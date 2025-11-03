package entities;

/**
 * Small interface for renderables that can provide a height query (terrain)
 */
public interface HeightProvider {
    double getHeightAt(double worldX, double worldZ);
}
