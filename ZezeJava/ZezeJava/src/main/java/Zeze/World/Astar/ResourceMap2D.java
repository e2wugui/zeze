package Zeze.World.Astar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import Zeze.World.CubeIndex;

public class ResourceMap2D implements IResourceMap {
	private final byte [] masks;
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
	public List<CubeIndex> ways(int x, int z) {
		return null;
	}
}
