package Zeze.World.Selector;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import Zeze.Serialize.Vector3;
import Zeze.World.Cube;
import Zeze.World.CubeIndex;
import Zeze.World.CubeMap;
import Zeze.World.Entity;
import Zeze.World.Graphics2D;
import Zeze.World.ISelector;

/**
 * 选择多边形限定的范围的cubes和entities。
 */
public class Polygon implements ISelector {
	private final java.util.List<Vector3> polygon;
	private final boolean isConvex;

	// 优化，初始化的时候算出包围盒，以后用来更快速的判断。
	private final Graphics2D.BoxFloat box;

	/**
	 * 构造。
	 * @param polygon 以(0, 0, 0)为原点。
	 * @param isConvex 是否凸多边形。
	 */
	public Polygon(java.util.List<Vector3> polygon, boolean isConvex) {
		this.polygon = polygon;
		this.isConvex = isConvex;
		this.box = new Graphics2D.BoxFloat(polygon);
	}

	@Override
	public SortedMap<CubeIndex, Cube> cubes(Entity origin) {
		// 转换成以origin为原定的坐标。
		var worldPolygon = new ArrayList<Vector3>(polygon.size());
		for (var p : polygon) {
			worldPolygon.add(p.add(origin.getBean().getMoving().getPosition()));
		}
		return CubeMap.polygon2d(origin.getCube().map, worldPolygon, isConvex);
	}

	/**
	 * 选择多边形内的实体。
	 * 【对于mmo战斗逻辑来说，这些实体还需要进行敌我识别过滤。是否在这里抽象还需确认。】
	 *
	 * @param origin 发起者
	 * @return entities in polygon.
	 */
	@Override
	public List<Entity> entities(Entity origin) {
		// 转换成以origin为原定的坐标。
		var worldPolygon = new ArrayList<Vector3>(polygon.size());
		var originPosition = origin.getBean().getMoving().getPosition();
		for (var p : polygon) {
			worldPolygon.add(p.add(originPosition));
		}
		var entities = new ArrayList<Entity>();
		var cubes = CubeMap.polygon2d(origin.getCube().map, worldPolygon, isConvex);
		var worldBox = box.add(originPosition);
		for (var cube : cubes.values()) {
			for (var entity : cube.objects.values()) {
				var position = entity.getBean().getMoving().getPosition();
				if (!worldBox.inside(position)) // 快速判断，优化，如果不在包围盒内，肯定不在多边形内。
					continue;

				if (isConvex) {
					if (Graphics2D.insideConvexPolygon(position, worldPolygon))
						entities.add(entity);
				} else {
					if (Graphics2D.insidePolygon(position, worldPolygon))
						entities.add(entity);
				}
			}
		}
		return entities;
	}
}
