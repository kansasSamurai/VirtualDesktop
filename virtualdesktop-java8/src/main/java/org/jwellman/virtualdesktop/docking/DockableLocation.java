package org.jwellman.virtualdesktop.docking;

/**
 * Describes where a dockable should be positioned within a workspace.
 * This abstracts the underlying framework's location mechanism.
 *
 * <p>Use static factory methods to create locations:</p>
 * <pre>
 * DockableLocation.normalIn(workspace);              // center position
 * DockableLocation.minimalNorthIn(workspace, 0);     // top
 * DockableLocation.external(100, 100, 400, 300);     // floating window
 * </pre>
 *
 * @author Rick Wellman
 */
public abstract class DockableLocation {

    /**
     * Position in the center/normal area of the workspace.
     *
     * @param workspace The target workspace
     * @return A location in the center
     */
    public static DockableLocation normalIn(DockingWorkspace workspace) {
        return new NormalLocation(workspace);
    }

    /**
     * Position minimally at the north (top) of the workspace.
     *
     * @param workspace The target workspace
     * @param index Position index (0, 1, 2, ...) for stacking multiple dockables
     * @return A location at the top
     */
    public static DockableLocation minimalNorthIn(DockingWorkspace workspace, int index) {
        return new MinimalNorthLocation(workspace, index);
    }

    /**
     * Position minimally at the south (bottom) of the workspace.
     *
     * @param workspace The target workspace
     * @param index Position index for stacking
     * @return A location at the bottom
     */
    public static DockableLocation minimalSouthIn(DockingWorkspace workspace, int index) {
        return new MinimalSouthLocation(workspace, index);
    }

    /**
     * Position minimally at the west (left) of the workspace.
     *
     * @param workspace The target workspace
     * @param index Position index for stacking
     * @return A location at the left
     */
    public static DockableLocation minimalWestIn(DockingWorkspace workspace, int index) {
        return new MinimalWestLocation(workspace, index);
    }

    /**
     * Position minimally at the east (right) of the workspace.
     *
     * @param workspace The target workspace
     * @param index Position index for stacking
     * @return A location at the right
     */
    public static DockableLocation minimalEastIn(DockingWorkspace workspace, int index) {
        return new MinimalEastLocation(workspace, index);
    }

    /**
     * Position at the north (top) of the workspace, visible and taking a portion of the space.
     *
     * @param workspace The target workspace
     * @param size Size ratio (0.0 to 1.0) of the space to occupy
     * @return A location at the top
     */
    public static DockableLocation northIn(DockingWorkspace workspace, double size) {
        return new NorthLocation(workspace, size);
    }

    /**
     * Position at the south (bottom) of the workspace, visible and taking a portion of the space.
     *
     * @param workspace The target workspace
     * @param size Size ratio (0.0 to 1.0) of the space to occupy
     * @return A location at the bottom
     */
    public static DockableLocation southIn(DockingWorkspace workspace, double size) {
        return new SouthLocation(workspace, size);
    }

    /**
     * Position at the west (left) of the workspace, visible and taking a portion of the space.
     *
     * @param workspace The target workspace
     * @param size Size ratio (0.0 to 1.0) of the space to occupy
     * @return A location at the left
     */
    public static DockableLocation westIn(DockingWorkspace workspace, double size) {
        return new WestLocation(workspace, size);
    }

    /**
     * Position at the east (right) of the workspace, visible and taking a portion of the space.
     *
     * @param workspace The target workspace
     * @param size Size ratio (0.0 to 1.0) of the space to occupy
     * @return A location at the right
     */
    public static DockableLocation eastIn(DockingWorkspace workspace, double size) {
        return new EastLocation(workspace, size);
    }

    /**
     * Position in an external floating window.
     *
     * @param x X coordinate of the window
     * @param y Y coordinate of the window
     * @param width Width of the window
     * @param height Height of the window
     * @return An external location
     */
    public static DockableLocation external(int x, int y, int width, int height) {
        return new ExternalLocation(x, y, width, height);
    }

    /**
     * Convert this location to the native framework location object.
     * This is used internally by docking provider implementations.
     *
     * @return The native location object
     */
    public abstract Object toNativeLocation();

    // Inner classes for location types - these will be moved to impl.bibliothek package
    private static class NormalLocation extends DockableLocation {
        private final DockingWorkspace workspace;

        NormalLocation(DockingWorkspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "normal", workspace };
        }
    }

    private static class MinimalNorthLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final int index;

        MinimalNorthLocation(DockingWorkspace workspace, int index) {
            this.workspace = workspace;
            this.index = index;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "minimalNorth", workspace, index };
        }
    }

    private static class MinimalSouthLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final int index;

        MinimalSouthLocation(DockingWorkspace workspace, int index) {
            this.workspace = workspace;
            this.index = index;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "minimalSouth", workspace, index };
        }
    }

    private static class MinimalWestLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final int index;

        MinimalWestLocation(DockingWorkspace workspace, int index) {
            this.workspace = workspace;
            this.index = index;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "minimalWest", workspace, index };
        }
    }

    private static class MinimalEastLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final int index;

        MinimalEastLocation(DockingWorkspace workspace, int index) {
            this.workspace = workspace;
            this.index = index;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "minimalEast", workspace, index };
        }
    }

    private static class NorthLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final double size;

        NorthLocation(DockingWorkspace workspace, double size) {
            this.workspace = workspace;
            this.size = size;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "north", workspace, size };
        }
    }

    private static class SouthLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final double size;

        SouthLocation(DockingWorkspace workspace, double size) {
            this.workspace = workspace;
            this.size = size;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "south", workspace, size };
        }
    }

    private static class WestLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final double size;

        WestLocation(DockingWorkspace workspace, double size) {
            this.workspace = workspace;
            this.size = size;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "west", workspace, size };
        }
    }

    private static class EastLocation extends DockableLocation {
        private final DockingWorkspace workspace;
        private final double size;

        EastLocation(DockingWorkspace workspace, double size) {
            this.workspace = workspace;
            this.size = size;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "east", workspace, size };
        }
    }

    private static class ExternalLocation extends DockableLocation {
        private final int x, y, width, height;

        ExternalLocation(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public Object toNativeLocation() {
            return new Object[] { "external", x, y, width, height };
        }
    }
}
