package Zeze.World.Astar;

import java.io.File;
import java.io.IOException;

public class ResourceMap2D implements IResourceMap {
	private final HugeMappedFile mappedFile;
	private final long width;
	private final long height;

	public ResourceMap2D(File file) throws Exception {
		var info = file.getName().split("_");
		this.width = Long.parseLong(info[1]);
		this.height = Long.parseLong(info[2]);
		this.mappedFile = new HugeMappedFile(file);
	}

	@Override
	public long getWidth() {
		return width;
	}

	@Override
	public long getHeight() {
		return height;
	}

	@Override
	public boolean walkable(long x, long z) {
		if (x < 0 || x >= getWidth())
			return false;
		if (z < 0 || z >= getHeight())
			return false;
		var mask = 0xff;
		var offset = x + z * getWidth();
		return (mappedFile.get(offset) & mask) != 0;
	}

	@Override
	public void traverseNeighbors(IAstar astar, Node current, Node target) {
		astar.traverseCorner(this, current, target, -1, -1, 7, 0, -1, -1, 0);
		astar.traverseCross(this, current, target, toIndex(current.index.x, current.index.z - 1), 5);
		astar.traverseCorner(this, current, target, +1, -1, 7, 0, -1, +1, 0);
		astar.traverseCross(this, current, target, toIndex(current.index.x - 1, current.index.z), 5);
		astar.traverseCross(this, current, target, toIndex(current.index.x + 1, current.index.z), 5);
		astar.traverseCorner(this, current, target, -1, +1, 7, -1, 0, 0, +1);
		astar.traverseCross(this, current, target, toIndex(current.index.x, current.index.z + 1), 5);
		astar.traverseCorner(this, current, target, +1, +1, 7, +1, 0, 0, +1);
	}

	@Override
	public void close() throws IOException {
		mappedFile.close();
	}
}
