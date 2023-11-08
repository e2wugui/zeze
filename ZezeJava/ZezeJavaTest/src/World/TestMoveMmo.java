package World;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Benchmark;
import Zeze.Util.Task;
import Zeze.World.CubeMap;
import Zeze.World.Entity;
import Zeze.World.LockGuard;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMoveMmo {
	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testMoveBench() throws Exception {
		Task.tryInitThreadPool();

		var map = new CubeMap(App.Instance.world.getMapManager(), 0, 128, 128);
		var center = map.toIndex(0, 0, 0);
		var cubes2d = map.center(center, 1, 0, 1);
		var instanceId = 0L;
		var objectCubeCount = 222; // 222 * 9 ~= 2000
		for (var cube : cubes2d.values()) {
			for (int i = 0; i < objectCubeCount; ++i) {
				var oid = instanceId++;
				var entity = new Entity(oid);
				cube.objects.put(oid, entity);
				entity.internalSetCube(cube);
			}
		}
		// total = objectCubeCount * 9;
		var server = new Service("testMoveBench.Server");
		server.AddFactoryHandle(new Zeze.Builtin.World.Command().getTypeId(), new Service.ProtocolFactoryHandle<>(
				Zeze.Builtin.World.Command::new, this::ProcessMove));
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
			try (var ignoredGuard = new LockGuard(cubes2d)) {
				var move = new Zeze.Builtin.World.Command();
				var bb = ByteBuffer.Allocate();
				move.encodeWithHead(bb);
				for (var cube : cubes2d.values()) {
					for (var ignoredObj : cube.objects.values()) {
						Assert.assertTrue(connection.Send(bb.Bytes, bb.ReadIndex, bb.size()));
					}
				}
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

	private long ProcessMove(Zeze.Builtin.World.Command move) {
		moveCounter.incrementAndGet();
		return 0;
	}
}
