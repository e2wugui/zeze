package Zeze.World.Astar;

import java.io.IOException;
import java.nio.file.Path;

public class ResourceMap2D implements IResourceMap {
	@SuppressWarnings("MismatchedReadAndWriteOfArray")
	private final byte[] masks;
	private final int width;
	private final int height;

	public ResourceMap2D(Path path) throws IOException {
		width = (int)(1024 * 30 / getUnitWidth());
		height = (int)(768 * 30 / getUnitHeight());
		masks = new byte[width * height];
		// todo load from file
		// todo edit code
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public boolean walkable(int x, int z) {
		if (x < 0 || x >= getWidth())
			return false;
		if (z < 0 || z >= getHeight())
			return false;
		var mask = 0xff;
		return (masks[x + z * getWidth()] & mask) != 0;
	}

	@Override
	public void traverseNeighbors(IAstar astar, Node current, Node target) {
		astar.traverseCorner(this, current, target, -1, -1, 7, 0, -1, -1, 0);
		astar.traverseCross(this, current, target, toIndex(current.index.x, current.index.z - 1), 5);
		astar.traverseCorner(this, current, target, +1, -1, 7, 0, -1, +1, 0);
		astar.traverseCross(this, current, target, toIndex(current.index.x - 1, current.index.z), 5);
		astar.traverseCross(this, current, target, toIndex(current.index.x + 1, current.index.z), 5);
		astar.traverseCorner(this, current, target, -1, +1, 7, -1, 0, 0, +1);
		astar.traverseCross(this, current, target, toIndex(current.index.x - 1, current.index.z + 1), 5);
		astar.traverseCorner(this, current, target, +1, +1, 7, +1, 0, 0, +1);
	}
}
