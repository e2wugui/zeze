package World;

import java.util.ArrayList;
import Zeze.Serialize.Vector3;
import Zeze.World.Graphics.Graphics2D;
import org.junit.Assert;
import org.junit.Test;

public class TestGraphics2D {
	@Test
	public void testBresenham2d() {
		System.out.println(Math.abs(-1L));
		Graphics2D.bresenham2d(0, 0, 0, 0, (x, y) -> System.out.print("(" + x + ", " + y + ")"));
		System.out.println();
		Graphics2D.bresenham2d(0, 2, 0, 0, (x, y) -> System.out.print("(" + x + ", " + y + ")"));
		System.out.println();
		Graphics2D.bresenham2d(-2, 0, 1, 0, (x, y) -> System.out.print("(" + x + ", " + y + ")"));
	}

	@Test
	public void testConvexPolygon() {
		var convex = new ArrayList<Vector3>();
		// 一个正方形
		convex.add(new Vector3(0, 0, 0));
		convex.add(new Vector3(0, 0, 128));
		convex.add(new Vector3(128, 0, 128));
		convex.add(new Vector3(128, 0, 0));

		Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(64, 0, 64), convex));
		Assert.assertFalse(Graphics2D.insideConvexPolygon(new Vector3(129, 0, 129), convex));
		// 边缘
		Assert.assertFalse(Graphics2D.insideConvexPolygon(new Vector3(0, 0, 0), convex));
	}

	@Test
	public void testPolygon() {
		var polygon = new ArrayList<Vector3>();
		// 一个正方形
		polygon.add(new Vector3(0, 0, 0));
		polygon.add(new Vector3(0, 0, 128));
		polygon.add(new Vector3(128, 0, 128));
		polygon.add(new Vector3(128, 0, 0));

		Assert.assertTrue(Graphics2D.insidePolygon(new Vector3(64, 0, 64), polygon));
		Assert.assertFalse(Graphics2D.insidePolygon(new Vector3(129, 0, 129), polygon));
		// 边缘
		Assert.assertFalse(Graphics2D.insidePolygon(new Vector3(0, 0, 0), polygon));
	}

}
