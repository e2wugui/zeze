package Zeze.World;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.World.BObject;
import Zeze.Serialize.Vector3;

/**
 * 把二维空间划分成一个个相邻的Grid。
 * 地图中的玩家或者物品记录在所在的Grid中。
 * 用来快速找到某个坐标周围的实体。
 */
public class GridIndexMap {
	private final ConcurrentHashMap<GridIndex, Grid> grids = new ConcurrentHashMap<>();
	private final int gridX;
	private final int gridY;

	public final int getGridX() {
		return gridX;
	}

	public final int getGridY() {
		return gridY;
	}

	public final GridIndex toIndex(Vector3 vector3) {
		GridIndex tempVar = new GridIndex();
		tempVar.setX((long)(vector3.x / gridX));
		tempVar.setY((long)(vector3.y / gridY));
		return tempVar;
	}

	public final GridIndex toIndex(float x, float y, float z) {
		GridIndex tempVar = new GridIndex();
		tempVar.setX((long)(x / gridX));
		tempVar.setY((long)(y / gridY));
		return tempVar;
	}

	/**
	 * 构造地图实例，参数为切割长宽。
	 * @param gridX 切割长度
	 * @param gridY 切割宽度
	 */
	public GridIndexMap(int gridX, int gridY) {
		if (gridX <= 0)
			throw new IllegalArgumentException("cubeSizeX <= 0");
		if (gridY <= 0)
			throw new IllegalArgumentException("cubeSizeY <= 0");

		this.gridX = gridX;
		this.gridY = gridY;
	}

	/**
	 * 返回 center grid 为中心，+-rangeX, +- rangeY 范围的内的所有Grid.
	 * @param center 中心
	 * @param rangeX x轴左右
	 * @param rangeY y轴左右
	 * @return List Of Grid.
	 */
	public final ArrayList<Grid> gridsOf(GridIndex center, int rangeX, int rangeY) {
		var result = new ArrayList<Grid>();
		for (long i = center.getX() - rangeX; i <= center.getX() + rangeX; ++i) {
			for (long j = center.getY() - rangeY; j <= center.getY() + rangeY; ++j) {
				var index = new GridIndex();
				index.setX(i);
				index.setY(j);
				var cube = grids.computeIfAbsent(index, (key) -> new Grid());
				result.add(cube);
			}
		}
		return result;
	}

	// 九宫格
	public final ArrayList<Grid> gridsOf(GridIndex center) {
		return gridsOf(center, 1, 1);
	}

	public final ArrayList<Grid> gridsOf(Vector3 center) {
		return gridsOf(toIndex(center));
	}

	public final ArrayList<Grid> gridsOf(BObject center) {
		return gridsOf(toIndex(center.getPosition()));
	}
}
