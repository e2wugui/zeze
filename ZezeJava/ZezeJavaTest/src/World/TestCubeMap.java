package World;

import java.util.ArrayList;
import Zeze.Serialize.Vector3;
import Zeze.Util.Benchmark;
import Zeze.World.CubeMap;
import Zeze.World.Graphics.Graphics2D;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCubeMap {
	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testMapPolygon() throws Exception {
		var map = new CubeMap(App.Instance.world.getMapManager(), 0, 64, 64);
		{
			var convex = new ArrayList<Vector3>();
			// 一个正方形
			convex.add(new Vector3(-128, 0, -128));
			convex.add(new Vector3(128, 0, -128));
			convex.add(new Vector3(128, 0, 128));
			convex.add(new Vector3(-128, 0, 128));

			Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(64, 0, 64), convex));
			Assert.assertFalse(Graphics2D.insideConvexPolygon(new Vector3(129, 0, 129), convex));
			// 边缘
			Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(-128, 0, -128), convex));
			Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(128, 0, -128), convex));
			Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(128, 0, 128), convex));
			Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(-128, 0, 128), convex));
			Assert.assertTrue(Graphics2D.insideConvexPolygon(new Vector3(-128, 0, 0), convex));

			var b = new Benchmark();
			long sum = 0;
			var benchCount = 50_0000;
			for (var i = 0; i < benchCount; ++i) {
				var cubes = map.polygon2d(convex, true);
				sum += cubes.size();
				//System.out.println(cubes.keySet());
				//System.out.println(cubes.size());
			}
			b.report("polygon2d full", benchCount);
			System.out.println("sum=" + sum);
		}
		{
			var convex = new ArrayList<Vector3>();
			// 一个正方形
			convex.add(new Vector3(0, 0, 0));
			convex.add(new Vector3(64, 0, 0));
			convex.add(new Vector3(64, 0, 64));
			convex.add(new Vector3(0, 0, 64));
			var b = new Benchmark();
			long sum = 0;
			var benchCount = 50_0000;
			for (var i = 0; i < benchCount; ++i) {
				var cubes = map.polygon2d(convex, true);
				sum += cubes.size();
				//System.out.println(cubes.keySet());
				//System.out.println(cubes.size());
			}
			b.report("polygon2d 4-cubes(inside is empty)", benchCount);
			System.out.println("sum=" + sum);
		}
		{
			var convex = new ArrayList<Vector3>();
			// 一个正方形
			convex.add(new Vector3(0, 0, 0));
			convex.add(new Vector3(64, 0, 0));
			convex.add(new Vector3(63, 0, 63));
			convex.add(new Vector3(0, 0, 63));
			var b = new Benchmark();
			long sum = 0;
			var benchCount = 50_0000;
			for (var i = 0; i < benchCount; ++i) {
				var cubes = map.polygon2d(convex, true);
				sum += cubes.size();
				//System.out.println(cubes.keySet());
				//System.out.println(cubes.size());
			}
			b.report("polygon2d 2-cubes", benchCount);
			System.out.println("sum=" + sum);
		}
		{
			var convex = new ArrayList<Vector3>();
			// 一个正方形
			convex.add(new Vector3(0, 0, 0));
			convex.add(new Vector3(63, 0, 0));
			convex.add(new Vector3(63, 0, 63));
			convex.add(new Vector3(0, 0, 63));
			var b = new Benchmark();
			long sum = 0;
			var benchCount = 50_0000;
			for (var i = 0; i < benchCount; ++i) {
				var cubes = map.polygon2d(convex, true);
				sum += cubes.size();
				//System.out.println(cubes.keySet());
				//System.out.println(cubes.size());
			}
			b.report("polygon2d 1-cubes", benchCount);
			System.out.println("sum=" + sum);
		}
	}
}
