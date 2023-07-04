package World;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.World.BObject;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Benchmark;
import org.junit.Assert;
import org.junit.Test;

public class TestAoi {
	@Test
	public void testMoveBench() throws Exception {
		var map = new Zeze.World.CubeIndexMap(128, 128);
		var center = map.toIndex(0, 0, 0);
		var cubes2d = map.cubes2d(center, 1, 1);
		var instanceId = 0L;
		var objectCubeCount = 222; // 222 * 9 ~= 2000
		for (var cube : cubes2d.values()) {
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
					for (var obj : cube.getObjects().values()) {
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
