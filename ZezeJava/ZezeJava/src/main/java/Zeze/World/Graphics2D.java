package Zeze.World;

import java.util.ArrayList;
import Zeze.Serialize.Vector3;

/**
 * 2d 图形算法包装。
 * 部分 2d 算法可以用把一个轴设成0来使用。
 */
public class Graphics2D {
	// 判断点落在凸多边形内。
	public static boolean insideConvexPolygon(CubeIndex point, java.util.List<CubeIndex> convexPolygon) {
		if (convexPolygon.size() < 3)
			return false;

		int j = convexPolygon.size() - 1;
		boolean oddNodes = false;
		for (int i = 0; i < convexPolygon.size(); ++i)
		{
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
			boolean cross = crossResult >= 0;
			if (i == 0)
				oddNodes = cross;
			if (cross != oddNodes)
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

	// 优化的Bresenham算法,不包括源端点,只支持 7fffffff 以内的坐标
	// 这个算法从以前 share/astar(a*) 扒出来的，原来的算法是int，现扩展到long，移位操作16改成了32，常量加了两个字节。
	public static void bresenham2d(long srcx, long srcy, long dstx, long dsty, Action2dLong plot) {
		if (srcx == dstx && srcy == dsty)
			return;

		boolean ylonger = false;
		var lenmin = dsty - srcy;
		var lenmax = dstx - srcx;
		if(Math.abs(lenmin) > Math.abs(lenmax))
		{
			// swap(lenmin, lenmax)
			var tmp = lenmin;
			lenmin = lenmax;
			lenmax = tmp;
			ylonger = true;
		}
		var delta = (lenmax == 0 ? 0 : (lenmin << 32) / lenmax);
		delta += delta < 0 ? 1 : 0;  // 更接近原Bresenham算法的修正

		if(ylonger)
		{
			if(lenmax > 0)
			{
				lenmax += srcy;
				for(var i = 0x7fffffff + (srcx << 32) + delta; ++srcy <= lenmax; i += delta)
					plot.run(i >> 32, srcy);

				return; // done
			}
			lenmax += srcy;
			for(var i = 0x80000000 + (srcx << 32) - delta; --srcy >= lenmax; i -= delta)
				plot.run(i >> 32, srcy);

			return; // done
		}

		if(lenmax > 0)
		{
			lenmax += srcx;
			for(var j = 0x7fffffff + (srcy << 32) + delta; ++srcx <= lenmax; j += delta)
				plot.run(srcx, j >> 32);

			return; // done
		}
		lenmax += srcx;
		for(var j = 0x80000000 + (srcy << 32) - delta; --srcx >= lenmax; j -= delta)
			plot.run(srcx, j >> 32);
		// return; // done
	}
}
