package World;

import java.util.Set;
import java.util.TreeMap;
import Zeze.World.Aoi.AoiSimple;
import Zeze.World.Cube;
import Zeze.World.CubeIndex;
import Zeze.World.CubeMap;
import org.junit.Assert;
import org.junit.Test;

public class TestAoi {

	@Test
	public void testDiff() {
		var map = new CubeMap(64, 64);
		{
			var olds = map.center(new CubeIndex(0, 0, 0), 1, 0, 1);
			var news = map.center(new CubeIndex(2, 0, 0), 1, 0, 1);
			System.out.println("olds=" + olds.keySet());
			System.out.println("news=" + news.keySet());

			var enters = new TreeMap<CubeIndex, Cube>();
			AoiSimple.diff(olds, news, enters);

			System.out.println("enters=" + enters.keySet());
			System.out.println("leaves=" + olds.keySet());

			Assert.assertEquals(Set.of(
					new CubeIndex(2,0,-1), new CubeIndex(2,0,0),
					new CubeIndex(2,0,1), new CubeIndex(3,0,-1),
					new CubeIndex(3,0,0), new CubeIndex(3,0,1)
			), enters.keySet());

			Assert.assertEquals(Set.of(
					new CubeIndex(-1,0,-1), new CubeIndex(-1,0,0),
					new CubeIndex(-1,0,1), new CubeIndex(0,0,-1),
					new CubeIndex(0,0,0), new CubeIndex(0,0,1)
			), olds.keySet());
		}
	}

	@Test
	public void testAioSimple() {
		var map = new CubeMap(64, 64);
		var aoi = new AoiSimple(map, 1, 1);
		map.setAoi(aoi); // not used in this test

		var from = map.getOrAdd(new CubeIndex(0, 0, 0));
		var toIndex = new CubeIndex(1, 0, 1);
		var enters = new TreeMap<CubeIndex, Cube>();
		var leaves = new TreeMap<CubeIndex, Cube>();

		var dx = toIndex.x - from.index.x;
		var dy = toIndex.y - from.index.y;
		var dz = toIndex.z - from.index.z;

		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectX(tmp, toIndex, dx);
			System.out.println("enterX=" + tmp.keySet());
			var result = Set.of(new CubeIndex(2, 0, 0), new CubeIndex(2, 0, 1), new CubeIndex(2, 0, 2));
			Assert.assertEquals(result, tmp.keySet());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectY(tmp, toIndex, dy);
			System.out.println("enterY=" + tmp.keySet());
			Assert.assertTrue(tmp.isEmpty());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectZ(tmp, toIndex, dz);
			System.out.println("enterZ=" + tmp.keySet());
			var result = Set.of(new CubeIndex(0, 0, 2), new CubeIndex(1, 0, 2), new CubeIndex(2, 0, 2));
			Assert.assertEquals(result, tmp.keySet());
		}

		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectX(tmp, from.index, -dx);
			System.out.println("leaveX=" + tmp.keySet());
			var result = Set.of(new CubeIndex(-1, 0, -1), new CubeIndex(-1, 0, 0), new CubeIndex(-1, 0, 1));
			Assert.assertEquals(result, tmp.keySet());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectY(tmp, from.index, -dy);
			System.out.println("leaveY=" + tmp.keySet());
			Assert.assertTrue(tmp.isEmpty());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectZ(tmp, from.index, -dz);
			System.out.println("leaveZ=" + tmp.keySet());
			var result = Set.of(new CubeIndex(-1, 0, -1), new CubeIndex(0, 0, -1), new CubeIndex(1, 0, -1));
			Assert.assertEquals(result, tmp.keySet());
		}

		aoi.fastCollectX(enters, toIndex, dx);
		aoi.fastCollectY(enters, toIndex, dy);
		aoi.fastCollectZ(enters, toIndex, dz);

		aoi.fastCollectX(leaves, from.index, -dx);
		aoi.fastCollectY(leaves, from.index, -dy);
		aoi.fastCollectZ(leaves, from.index, -dz);

		System.out.println("enters=" + enters.keySet());
		System.out.println("leaves=" + leaves.keySet());
		var r = Set.of(new CubeIndex(2, 0, 0), new CubeIndex(2, 0, 1), new CubeIndex(2, 0, 2),
				new CubeIndex(0, 0, 2), new CubeIndex(1, 0, 2));
		Assert.assertEquals(r, enters.keySet());
		var l = Set.of(new CubeIndex(-1, 0, -1), new CubeIndex(0, 0, -1), new CubeIndex(1, 0, -1),
				new CubeIndex(-1, 0, 0), new CubeIndex(-1, 0, 1));
		Assert.assertEquals(l, leaves.keySet());
	}
}
