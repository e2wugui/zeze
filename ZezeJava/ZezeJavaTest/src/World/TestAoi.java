package World;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.World.BObject;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.Benchmark;
import org.junit.Assert;
import org.junit.Test;

public class TestAoi {
	@Test
	public void testMoveBench() throws Exception {
		var map = new Zeze.World.CubeIndexMap(128, 128);
		var center = map.toIndex(0, 0, 0);
		var cubes2d = map.cubes2D(center, 1, 1);
		var instanceId = 0L;
		var objectCubeCount = 250;
		for (var cube : cubes2d) {
			for (int i = 0; i < objectCubeCount; ++i) {
				var oid = new Zeze.Builtin.World.ObjectId(0, 0, instanceId++);
				cube.addObject(oid, new BObject());
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
		connection.getService().getSocketOptions().setOutputBufferMaxSize((long)moveBenchCount * objectCubeCount * 9 * 200);

		// 目前这个测试实际压力都在Socket上，先搭出框架。
		var b = new Benchmark();
		for (int i = 0; i < moveBenchCount; ++i) {
			for (var cube : cubes2d) {
				for (var obj : cube.getObjects().values()) {
					var move = new Zeze.Builtin.World.Move();
					var bb = move.encode();
					Assert.assertTrue(connection.Send(bb));
				}
			}
		}
		b.report("testAoiBench", moveBenchCount);
		System.out.println("moveCount = " + moveCounter.get());
		client.stop();
		server.stop();;
	}

	final Zeze.Util.TaskCompletionSource<AsyncSocket> connected = new Zeze.Util.TaskCompletionSource<>();

	public class Client extends Service {
		public Client() {
			super("testMoveBench.Client");
		}

		@Override
		public void OnSocketConnected(AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			connected.setResult(so);
		}
	}

	private final AtomicLong moveCounter = new AtomicLong();

	private long ProcessMove(Zeze.Builtin.World.Move move) {
		moveCounter.incrementAndGet();
		return 0;
	}
}
