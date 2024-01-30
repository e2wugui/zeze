package World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.World.BAoiLeaves;
import Zeze.Builtin.World.BAoiOperates;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BMove;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Transaction.Data;
import Zeze.Util.Benchmark;
import Zeze.Util.Task;
import Zeze.World.Mmo.AoiSimple;
import Zeze.World.Cube;
import Zeze.World.CubeIndex;
import Zeze.World.CubeMap;
import Zeze.World.Entity;
import Zeze.World.LockGuard;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAoi {

	// 模拟客户端，用来接收enter,operate,leave。
	// 多线程创建随机穿越Cube的aoi请求，
	// 可以全局暂停多线程移动，然后验证这个”客户端“的数据是总是两两互相可见。
	static class TestLinkSender implements Zeze.World.ILinkSender {

		// 下面的调用在cubes锁内。同一个玩家（用不同LinkSid区分），不会发生并发。
		private final ConcurrentHashMap<Long, HashSet<Long>> players = new ConcurrentHashMap<>();

		@Override
		public boolean sendLink(String linkName, ByteBuffer fullEncodedProtocol) {
			throw new RuntimeException("impossible!");
		}

		@Override
		public boolean sendLink(String linkName, Send send) {
			throw new RuntimeException("impossible!");
		}

		@Override
		public boolean sendCommand(String linkName, long linkSid, long mapInstanceId, int commandId, Data data) {
			var views = players.computeIfAbsent(linkSid, (key) -> new HashSet<>());
			switch (commandId) {
			case BCommand.eAoiEnter:
				var aoiEnters = (BAoiOperates.Data)data;
				views.addAll(aoiEnters.getOperates().keySet());
				break;

			case BCommand.eAoiLeave:
				var aoiLeaves = (BAoiLeaves.Data)data;
				aoiLeaves.getKeys().forEach(views::remove);
				break;

			case BCommand.eAoiOperate:
				// skip now;
				break;
			}
			return true;
		}

		@Override
		public boolean sendCommand(Collection<Entity> targets, long mapInstanceId, int commandId, Data data) {
			for (var player : targets) {
				sendCommand("not used", player.getBean().getLinkSid(), mapInstanceId, commandId, data);
			}
			return false;
		}

		public void verify() {
			for (var player : players.entrySet()) {
				for (var see : player.getValue()) {
					var other = players.get(see);
					Assert.assertTrue(other.contains(player.getKey()));
				}
			}
		}
	}

	volatile boolean testRunning = true;

	final AtomicLong moveCount = new AtomicLong();

	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testAoiFull() throws Exception {
		var world = App.Instance.world;
		var client = new TestLinkSender();
		world.setLinkSender(client);

		var map = world.getMapManager().createMap();
		var aoi = map.getAoi();

		var cubeNumber = 10;
		var cubeObjectNumber = 10;
		var xBase = 32;
		var yBase = 0;
		var zBase = 0;

		var globalRWLock = new ReentrantReadWriteLock();
		var players = new ArrayList<Future<?>>();
		for (var i = 0; i < cubeNumber; ++i) {
			for (var j = 0; j < cubeObjectNumber; ++j) {
				var position = new Vector3(xBase + 64 * i, yBase, zBase);
				var entityId = (long)(i * cubeObjectNumber + j + 1);
				var cube = map.getOrAdd(map.toIndex(position));
				Entity entity;
				try (var ignored = new LockGuard(cube)) {
					entity = cube.pending.computeIfAbsent(entityId, Entity::new);
					entity.getBean().getMoving().setPosition(position);
					// 创建假的Link信息。
					entity.getBean().setLinkName("1");
					entity.getBean().setLinkSid(entityId);
					entity.internalSetCube(cube);
					map.entities.put(entityId, entity); // 实体索引。
				}

				aoi.enter(entity);

				players.add(Task.runUnsafe(() -> {
					while (testRunning) {
						var rLock = globalRWLock.readLock();
						try {
							rLock.lock();
							var cubeX = Zeze.Util.Random.getInstance().nextInt(cubeNumber);
							var p = new Vector3(xBase + 64 * cubeX, yBase, zBase);
							var move = new BMove.Data();
							move.setPosition(p);
							aoi.moveTo(entity, move);
							moveCount.incrementAndGet();
						} finally {
							rLock.unlock();
						}
					}
				}, ""));
			}
		}

		for (int i = 0; i < 1000; ++i) {
			var wLock = globalRWLock.writeLock();
			try {
				wLock.lock();
				// stop aoi.moveTo & verify
				client.verify();
			} finally {
				wLock.unlock();
			}
		}

		var b = new Benchmark();
		moveCount.set(0);
		Thread.sleep(3000);
		testRunning = false;
		for (var player : players)
			player.get();

		b.report("Aoi.moveTo", moveCount.get());
	}

	@Test
	public void testDiff() throws Exception {
		var map = new CubeMap(App.Instance.world.getMapManager(), 0, 64, 64);
		{
			var olds = map.center(new CubeIndex(0, 0, 0), 1, 0, 1);
			var news = map.center(new CubeIndex(2, 0, 0), 1, 0, 1);
			System.out.println("olds=" + olds.keySet());
			System.out.println("news=" + news.keySet());

			var enters = new TreeMap<CubeIndex, Cube>();
			var notifies = new TreeMap<CubeIndex, Cube>();
			AoiSimple.diff(olds, news, enters, notifies);

			System.out.println("enters=" + enters.keySet());
			System.out.println("leaves=" + olds.keySet());
			System.out.println("notifies=" + notifies.keySet());

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
	public void testAioSimple() throws Exception {
		var map = new CubeMap(App.Instance.world.getMapManager(), 0, 64, 64);
		var aoi = new AoiSimple(App.Instance.world, map, 1, 1);
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
			aoi.fastCollectWithFixedX(tmp, toIndex, dx);
			System.out.println("enterX=" + tmp.keySet());
			var result = Set.of(new CubeIndex(2, 0, 0), new CubeIndex(2, 0, 1), new CubeIndex(2, 0, 2));
			Assert.assertEquals(result, tmp.keySet());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectWithFixedY(tmp, toIndex, dy);
			System.out.println("enterY=" + tmp.keySet());
			Assert.assertTrue(tmp.isEmpty());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectWithFixedZ(tmp, toIndex, dz);
			System.out.println("enterZ=" + tmp.keySet());
			var result = Set.of(new CubeIndex(0, 0, 2), new CubeIndex(1, 0, 2), new CubeIndex(2, 0, 2));
			Assert.assertEquals(result, tmp.keySet());
		}

		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectWithFixedX(tmp, from.index, -dx);
			System.out.println("leaveX=" + tmp.keySet());
			var result = Set.of(new CubeIndex(-1, 0, -1), new CubeIndex(-1, 0, 0), new CubeIndex(-1, 0, 1));
			Assert.assertEquals(result, tmp.keySet());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectWithFixedY(tmp, from.index, -dy);
			System.out.println("leaveY=" + tmp.keySet());
			Assert.assertTrue(tmp.isEmpty());
		}
		{
			var tmp = new TreeMap<CubeIndex, Cube>();
			aoi.fastCollectWithFixedZ(tmp, from.index, -dz);
			System.out.println("leaveZ=" + tmp.keySet());
			var result = Set.of(new CubeIndex(-1, 0, -1), new CubeIndex(0, 0, -1), new CubeIndex(1, 0, -1));
			Assert.assertEquals(result, tmp.keySet());
		}

		aoi.fastCollectWithFixedX(enters, toIndex, dx);
		aoi.fastCollectWithFixedY(enters, toIndex, dy);
		aoi.fastCollectWithFixedZ(enters, toIndex, dz);

		aoi.fastCollectWithFixedX(leaves, from.index, -dx);
		aoi.fastCollectWithFixedY(leaves, from.index, -dy);
		aoi.fastCollectWithFixedZ(leaves, from.index, -dz);

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
