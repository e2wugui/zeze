package Zeze.World.Astar;

import java.util.List;
import Zeze.World.CubeIndex;

/**
 * 限制搜索范围.
 */
public class ResourceMapView implements IResourceMap {
	private final CubeIndex offset;
	private final IResourceMap map;
	private final int width;
	private final int height;

	public ResourceMapView(CubeIndex center, CubeIndex range, IResourceMap map) {
		this.offset = center.sub(range);
		this.map = map;
		this.width = range.x << 1 + 1;
		this.height = range.z << 1 + 1;
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
		if (x >= width || z >= height)
			return false;
		return map.walkable(x + offset.x, z + offset.z);
	}

	@Override
	public boolean walkable(int fromX, int fromZ, int toX, int toZ) {
		if (fromX >= width || fromZ >= height
				|| toX >= width || toZ >= height)
			return false;
		return map.walkable(fromX + offset.x, fromZ + offset.z, toX + offset.x, toZ + offset.z);
	}

	@Override
	public List<CubeIndex> ways(int x, int z) {
		return null;
	}
}
