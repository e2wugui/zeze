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

	// edit helper
	public void set(long x, long z, byte b) {
		if (x < 0 || x >= getWidth() || z < 0 || z >= getHeight())
			throw new RuntimeException("x or z out of range.");

		var index = x + z * getWidth();
		mappedFile.set(index, b);
	}

	public void copy(long tx, long tz, long sx, long sz, int width, int height) {
		if (tx < 0 || tx >= getWidth() || tz < 0 || tz >= getHeight())
			throw new RuntimeException("tx or tz out of range.");

		if (sx < 0 || sx >= getWidth() || sz < 0 || sz >= getHeight())
			throw new RuntimeException("sx or sz out of range.");

		if (width <= 0 || height <= 0)
			throw new RuntimeException("width or height is negative.");

		// 修正区域
		var sw = Math.min(width, (int)(getWidth() - sx));
		var sh = Math.min(height, (int)(getHeight() - sz));

		// 不需要工作内存的方案需要考虑拷贝区域和目标区域重叠问题。需要写出4个不同的实现。
		// 考虑到这个方法用于屏幕编辑，一般范围不大。这里采用复制到临时空间再写入的方案。
		var tmp = new byte[sw * sh];
		for (var row = 0; row < sh; ++row) {
			for (var col = 0 ; col < sw; ++col) {
				var tmpIndex = col + row * sw;
				var index = (row + sx) + (col + sz) * getWidth();
				tmp[tmpIndex] = mappedFile.get(index);
			}
		}
		var tw = Math.min(sw, (int)(getWidth() - tx));
		var th = Math.min(sh, (int)(getHeight() - tz));
		for (var row = 0; row < th; ++row) {
			for (var col = 0 ; col < tw; ++col) {
				var tmpIndex = col + row * sw; // 注意，这里需要用sw作为每一行的宽度。
				var index = (row + tx) + (col + tz) * getWidth();
				mappedFile.set(index, tmp[tmpIndex]);
			}
		}
	}
}
