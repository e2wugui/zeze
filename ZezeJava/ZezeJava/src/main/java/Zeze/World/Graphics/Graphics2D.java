package Zeze.World.Graphics;

import java.util.ArrayList;
import Zeze.Serialize.Vector3;
import Zeze.World.Action2dInt;
import Zeze.World.CubeIndex;

/**
 * 2d 图形算法包装。
 * 部分 2d 算法可以用把一个轴设成0来使用。
 */
public class Graphics2D {
	public static class BoxFloat {
		public float minX;
		public float maxX;
		public float minZ;
		public float maxZ;

		public BoxFloat() {

		}

		public BoxFloat(float minX, float maxX, float minZ, float maxZ) {
			this.minX = minX;
			this.maxX = maxX;
			this.minZ = minZ;
			this.maxZ = maxZ;
		}

		public BoxFloat(java.util.List<Vector3> polygon) {
			for (var p : polygon) {
				if (p.x < minX) minX = p.x;
				if (p.x > maxX) maxX = p.x;
				if (p.z < minZ) minZ = p.z;
				if (p.z > maxZ) maxZ = p.z;
			}
		}

		/**
		 * 是否在包围盒内，在边线也算在里面。
		 * @param position 位置
		 * @return true if inside.
		 */
		public boolean inside(Vector3 position) {
			return position.x >= minX && position.x <= maxX && position.z >= minZ && position.z <= maxZ;
		}

		public BoxFloat add(Vector3 diff) {
			return new BoxFloat(minX + diff.x, maxX + diff.x, minZ + diff.z, maxZ + diff.z);
		}
	}

	// 暂时还没发现需要。CubeMap.polygin2d那样写可以少new一个对象。
//	public static class BoxLong {
//		public long minX;
//		public long maxX;
//		public long minZ;
//		public long maxZ;
//
//		public BoxLong() {
//
//		}
//
//		public BoxLong(long minX, long maxX, long minZ, long maxZ) {
//			this.minX = minX;
//			this.maxX = maxX;
//			this.minZ = minZ;
//			this.maxZ = maxZ;
//		}
//
//		/**
//		 * 是否在包围盒内，在边线也算在里面。
//		 * @param position 位置
//		 * @return true if inside.
//		 */
//		public boolean inside(CubeIndex position) {
//			return position.x >= minX && position.x <= maxX && position.z >= minZ && position.z <= maxZ;
//		}
//	}

	// 判断点落在凸多边形内。
	public static boolean insideConvexPolygon(CubeIndex point, java.util.List<CubeIndex> convexPolygon) {
		return insideConvexPolygon(point.toVector3(), toVector3(convexPolygon));
	}

	public static boolean insideConvexPolygon(Vector3 point, java.util.List<Vector3> convexPolygon) {
		if (convexPolygon.size() < 3)
			return false;

		int j = convexPolygon.size() - 1;
		boolean oldNodes = false;
		for (int i = 0; i < convexPolygon.size(); ++i) {
			// y1 < y && y >= y2
			// y2 < y && y >= y1
			var z = point.z;
			var z1 = convexPolygon.get(i).z;
			var z2 = convexPolygon.get(j).z;
			var x = point.x;
			var x1 = convexPolygon.get(i).x;
			var x2 = convexPolygon.get(j).x;

			// 向量PC
			var difXPC = x2 - x;
			var difZPC = z2 - z;
			// 向量PA
			var difXPA = x1 - x;
			var difZPA = z1 - z;

			var crossResult = difXPC * difZPA - difZPC * difXPA;
			var cross = crossResult >= 0.0f;
			if (i == 0)
				oldNodes = cross;
			if (cross != oldNodes)
				return false;

			j = i;
		}

		return true;
	}

	public static java.util.List<Vector3> toVector3(java.util.List<CubeIndex> polygon) {
		var result = new ArrayList<Vector3>(polygon.size());
		for (var cube : polygon)
			result.add(cube.toVector3());
		return result;
	}

	// 判断点落在任意多边形内
	public static boolean insidePolygon(CubeIndex point, java.util.List<CubeIndex> polygon) {
		return insidePolygon(point.toVector3(), toVector3(polygon));
	}

	public static boolean insidePolygon(Vector3 point, java.util.List<Vector3> polygon) {
		if (polygon.size() < 3)
			return false;

		var raycastLen = 10000f;
		var comparePoint = polygon.get(0).add(polygon.get(1)).multiply(0.5f);
		// 此处一定要这样写表示射线，不然comparePoint在边上计算会有误差
		comparePoint = comparePoint.add(comparePoint.sub(point).normalized().multiply(raycastLen));

		int count = 0;
		for (int i = 0; i < polygon.size(); ++i) {
			var a = polygon.get(i);
			var b = polygon.get((i + 1) % polygon.size());
			// 循环判断每条边与testPoint射向comparePoint的射线是否有交点
			if (isIntersection(a, b, point, comparePoint))
				++count;
		}
		return count % 2 == 1;
	}

	public static final float epsilon = (float)1.4e-45; // c# Single.Epsilon

	// 【unity】Compares two floating point values if they are similar.
	public static boolean approximately(float a, float b) {
		// If a or b is zero, compare that the other is less or equal to epsilon.
		// If neither a or b are 0, then find an epsilon that is good for
		// comparing numbers at the maximum magnitude of a and b.
		// Floating points have about 7 significant digits, so
		// 1.000001f can be represented while 1.0000001f is rounded to zero,
		// thus we could use an epsilon of 0.000001f for comparing values close to 1.
		// We multiply this epsilon by the biggest magnitude of a and b.
		return Math.abs(b - a) < Math.max(0.000001f * Math.max(Math.abs(a), Math.abs(b)), epsilon * 8);
	}

	public static boolean isIntersection(Vector3 a, Vector3 b, Vector3 originPoint, Vector3 comparePoint) {
		// 判断是否同向
		var crossA = Math.signum(Vector3.cross(comparePoint.sub(originPoint), a.sub(originPoint)).y);
		var crossB = Math.signum(Vector3.cross(comparePoint.sub(originPoint), b.sub(originPoint)).y);
		if (approximately(crossA, crossB))
			return false;

		var crossC = Math.signum(Vector3.cross(b.sub(a), originPoint.sub(a)).y);
		var crossD = Math.signum(Vector3.cross(b.sub(a), comparePoint.sub(a)).y);
		return !approximately(crossC, crossD);
	}

	@SuppressWarnings("SuspiciousNameCombination")
	public static void bresenham2d(int x0, int y0, int x1, int y1, Action2dInt plot) {
		var swapXY = Math.abs( y1 - y0 ) > Math.abs( x1 - x0 );
		if ( swapXY ) {
			// 交换 x 和 y
			// 交换 x0 和 y0
			var tmp = x0;
			x0 = y0;
			y0 = tmp;
			// 交换 x1 和 y1
			tmp = x1;
			x1 = y1;
			y1 = tmp;
		}

		if ( x0 > x1 ) {
			// 确保 x0 < x1
			// 交换 x0 和 x1
			var tmp = x0;
			x0 = x1;
			x1 = tmp;
			// 交换 y0 和 y1
			tmp = y0;
			y0 = y1;
			y1 = tmp;
		}

		var deltaX = x1 - x0;
		var deltaY = Math.abs( y1 - y0 );
		var error = deltaX >> 1;
		var y = y0;
		var yStep = y0 < y1 ? 1 : -1;
		/*
		// 用下面这个替换下面两个分支循环，有性能损失吗？
		Action2dLong action = swapXY ? (_x, _y) -> plot.run(_y, _x) : plot;
		for (var x = x0; x <= x1; ++x){
			action.run(x, y);
			error -= deltaY;
			if (error < 0) {
				y += yStep;
				error += deltaX;
			}
		}
		*/
		if( swapXY ) {
			// Y / X
			for (var x = x0; x <= x1; ++x){
				plot.run(y, x);
				error -= deltaY;
				if (error < 0) {
					y += yStep;
					error += deltaX;
				}
			}
		} else {
			// X / Y
			for (var x = x0; x <= x1; ++x){
				plot.run(x, y);
				error -= deltaY;
				if (error < 0) {
					y += yStep;
					error += deltaX;
				}
			}
		}
	}
}
