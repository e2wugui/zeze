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

public class Polygon implements ISelector {
	private final java.util.List<Vector3> polygon;
	private final boolean isConvex;

	// 优化，初始化的时候算出包围盒，以后用来更快速的判断。
	private float boxMinX = Float.MAX_VALUE;
	private float boxMinZ = Float.MAX_VALUE;

	private float boxMaxX = Float.MIN_VALUE;
	private float boxMaxZ = Float.MIN_VALUE;

	/**
	 * 构造。
	 * @param polygon 以(0, 0, 0)为原点。
	 * @param isConvex 是否凸多边形。
	 */
	public Polygon(java.util.List<Vector3> polygon, boolean isConvex) {
		this.polygon = polygon;
		this.isConvex = isConvex;

		for (var p : polygon) {
			if (p.x < boxMinX) boxMinX = p.x;
			if (p.x > boxMaxX) boxMaxX = p.x;
			if (p.z < boxMinZ) boxMinZ = p.z;
			if (p.z > boxMaxZ) boxMaxZ = p.z;
		}
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

	private boolean insideBox(Vector3 point) {
		return point.x >= boxMinX && point.x <= boxMaxX && point.z >= boxMinZ && point.z <= boxMaxZ;
	}

	@Override
	public List<Entity> entities(Entity origin) {
		// 转换成以origin为原定的坐标。
		var worldPolygon = new ArrayList<Vector3>(polygon.size());
		for (var p : polygon) {
			worldPolygon.add(p.add(origin.getBean().getMoving().getPosition()));
		}
		var entities = new ArrayList<Entity>();
		var cubes = CubeMap.polygon2d(origin.getCube().map, worldPolygon, isConvex);
		for (var cube : cubes.values()) {
			for (var entity : cube.objects.values()) {
				var position = entity.getBean().getMoving().getPosition();
				if (!insideBox(position)) // 快速判断，优化，如果不在包围盒内，肯定不在多边形内。
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
