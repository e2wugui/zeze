package Zeze.World.Astar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

	// edit helper
	public void set(long x, long z, byte b) {
		if (x < 0 || x >= getWidth() || z < 0 || z >= getHeight())
			throw new RuntimeException("x or z out of range.");

		var index = x + z * getWidth();
		mappedFile.set(index, b);
	}

	public void set(long x, long z, int width, int height, byte b) {
		if (x < 0 || x >= getWidth() || z < 0 || z >= getHeight())
			throw new RuntimeException("x or z out of range.");

		if (width <= 0 || height <= 0)
			return; // skip empty rect

		var xEnd = Math.min(x + width, getWidth());
		var zEnd = Math.min(z + height, getHeight());

		var rowBytes = new byte[(int)(xEnd - x)];
		if (b != 0)
			Arrays.fill(rowBytes, b);
		for (var row = z; row < zEnd; ++row) {
			var rowBegin = row * getWidth();
			mappedFile.set(rowBegin, rowBytes);
		}
	}

	// copy paste 在可达图编辑里面看起来没什么用，先不实现了。
}
