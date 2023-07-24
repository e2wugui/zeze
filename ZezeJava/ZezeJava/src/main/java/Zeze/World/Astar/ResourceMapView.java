package Zeze.World.Astar;

import java.util.List;
import Zeze.World.CubeIndex;

/**
 * 限制搜索范围.
 */
public class ResourceMapView implements IResourceMap {
	private final Index offset;
	private final IResourceMap map;
	private final int width;
	private final int height;

	public ResourceMapView(Index center, Index range, IResourceMap map) {
		if (range.x <= 0 || range.z <= 0)
			throw new RuntimeException("invalid range. " + range);

		var x = center.x - range.x;
		var z = center.z - range.z;
		this.offset = map.toIndex(x, z);
		this.map = map;
		this.width = (range.x << 1) + 1;
		this.height= (range.z << 1) + 1;
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
	public float getUnitWidth() {
		return map.getUnitWidth();
	}

	@Override
	public float getUnitHeight() {
		return map.getUnitHeight();
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

		return map.walkable(
				fromX + offset.x, fromZ + offset.z,
				toX + offset.x, toZ + offset.z);
	}

	@Override
	public List<CubeIndex> ways(int x, int z) {
		return null;
	}
}
