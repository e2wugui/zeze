package World;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.World.BObject;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Util.Benchmark;
import Zeze.Util.Task;
import Zeze.World.CubeIndexMap;
import Zeze.World.Graphics2D;
import org.junit.Assert;
import org.junit.Test;

public class TestAoi {
	@Test
	public void testBresenham2d() {
		System.out.println(Graphics2D.fastAbs(-1L));
		Graphics2D.bresenham2d(0, 0, 0, 0, (x, y) -> System.out.print("(" + x + ", " + y + ")"));
		System.out.println();
		Graphics2D.bresenham2d(0, 2, 0, 0, (x, y) -> System.out.print("(" + x + ", " + y + ")"));
		System.out.println();
		Graphics2D.bresenham2d(-2, 0, 1, 0, (x, y) -> System.out.print("(" + x + ", " + y + ")"));
	}

	@Test
	public void testMapPolygon() {
		var map = new CubeIndexMap(64, 64);
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

		var cubes = map.polygon2d(convex, true);
		System.out.println(cubes.keySet());
		System.out.println(cubes.size());
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

	@Test
	public void testMoveBench() throws Exception {
		Task.tryInitThreadPool(null, null, null);

		var map = new Zeze.World.CubeIndexMap(128, 128);
		var center = map.toIndex(0, 0, 0);
		var cubes2d = map.center2d(center, 1, 1);
		var instanceId = 0L;
		var objectCubeCount = 222; // 222 * 9 ~= 2000
		for (var cube : cubes2d.values()) {
			for (int i = 0; i < objectCubeCount; ++i) {
				var oid = new Zeze.Builtin.World.ObjectId(0, 0, instanceId++);
				cube.add(oid, new BObject());
			}
		}
		// total = objectCubeCount * 9;
		var server = new Service("testMoveBench.Server");
		server.AddFactoryHandle(new Zeze.Builtin.World.Move().getTypeId(), new Service.ProtocolFactoryHandle<>(
				Zeze.Builtin.World.Move::new, this::ProcessMove));
		server.newServerSocket("127.0.0.1", 9999, null);
		var client = new Client();
		var connection = client.newClientSocket("127.0.0.1", 9999, null, null);
		connected.get();

		var moveBenchCount = 10000;
		// buffer要足够大，要不然会溢出。
		connection.getService().getSocketOptions().setOutputBufferMaxSize((long)moveBenchCount * objectCubeCount * 9 * 200);

		// 目前这个测试还没有aoi运算内容，先搭出框架。
		var b = new Benchmark();
		for (int i = 0; i < moveBenchCount; ++i) {
			for (var cube : cubes2d.values())
				cube.lock();
			try {
				var move = new Zeze.Builtin.World.Move();
				var bb = ByteBuffer.Allocate();
				move.encodeWithHead(bb);
				for (var cube : cubes2d.values()) {
					for (var obj : cube.objects().values()) {
						Assert.assertTrue(connection.Send(bb.Bytes, bb.ReadIndex, bb.size()));
					}
				}
			} finally {
				for (var cube : cubes2d.values())
					cube.unlock();
			}
		}
		b.report("testAoiBench", moveBenchCount);
		//Thread.sleep(2000);
		System.out.println("moveCounter=" + moveCounter.get());
		client.stop();
		server.stop();
	}

	final Zeze.Util.TaskCompletionSource<AsyncSocket> connected = new Zeze.Util.TaskCompletionSource<>();

	public class Client extends Service {
		public Client() {
			super("testMoveBench.Client");
		}

		@Override
		public void OnSocketConnected(AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			System.out.println("connected");
			connected.setResult(so);
		}
	}

	private final AtomicLong moveCounter = new AtomicLong();

	private long ProcessMove(Zeze.Builtin.World.Move move) {
		moveCounter.incrementAndGet();
		return 0;
	}
}
