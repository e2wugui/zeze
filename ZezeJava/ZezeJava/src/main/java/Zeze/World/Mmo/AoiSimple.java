package Zeze.World.Mmo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import Zeze.Builtin.World.BAoiLeaves;
import Zeze.Builtin.World.BAoiOperate;
import Zeze.Builtin.World.BAoiOperates;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BMove;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
import Zeze.World.CubeIndex;
import Zeze.World.Entity;
import Zeze.World.LockGuard;
import Zeze.World.World;
import Zeze.World.Cube;
import Zeze.World.CubeMap;
import Zeze.World.IAoi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AoiSimple implements IAoi {
	private static final Logger logger = LogManager.getLogger(AoiSimple.class);

	public final World world;
	private final CubeMap map;
	private final int rangeX;
	private final int rangeY;
	private final int rangeZ;

	public int getRangeX() {
		return rangeX;
	}

	public int getRangeY() {
		return rangeY;
	}

	public int getRangeZ() {
		return rangeZ;
	}

	public AoiSimple(World world, CubeMap map, int rangeX, int rangeZ) {
		this(world, map, rangeX, 0, rangeZ);
	}

	public AoiSimple(World world, CubeMap map, int rangeX, int rangeY, int rangeZ) {
		this.world = world;
		this.map = map;
		this.rangeX = rangeX;
		this.rangeY = rangeY;
		this.rangeZ = rangeZ;
	}

	public CubeMap getCubeMap() {
		return map;
	}

	public void fastCollectWithFixedX(SortedMap<CubeIndex, Cube> result, CubeIndex center, int dx) {
		if (dx != 0) {
			var i = center.x + dx * rangeX;
			for (var j = center.y - rangeY; j <= center.y + rangeY; ++j) {
				for (var k = center.z - rangeZ; k <= center.z + rangeZ; ++k) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	public void fastCollectWithFixedY(SortedMap<CubeIndex, Cube> result, CubeIndex center, int dy) {
		if (dy != 0) {
			// 2d never go here.
			var j = center.y + dy * rangeY;
			for (var i = center.x - rangeX; i <= center.x + rangeX; ++i) {
				for (var k = center.z - rangeZ; k <= center.z + rangeZ; ++k) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	public void fastCollectWithFixedZ(SortedMap<CubeIndex, Cube> result, CubeIndex center, int dz) {
		if (dz != 0) {
			var k = center.z + dz * rangeZ;
			for (var i = center.x - rangeX; i <= center.x + rangeX; ++i) {
				for (var j = center.y - rangeY; j <= center.y + rangeY; ++j) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	protected static void update(Entity self, BMove.Data move, Cube cube, Cube newCube) {
		self.getBean().setMoving(move);
		if (cube != newCube) {
			var entity = cube.objects.remove(self.getId());
			newCube.objects.put(self.getId(), entity);
			entity.internalSetCube(newCube);
		}
	}

	@Override
	public long enter(Entity entity) throws IOException {
		// 第一次访问aoi。
		var firstEnters = map.center(entity.getCube().index, rangeX, rangeY, rangeZ);
		try (var ignored = new LockGuard(firstEnters)) {
			var self = entity.getCube().pending.remove(entity.getId());
			entity.getCube().objects.put(entity.getId(), entity);
			processEnters(self, firstEnters);
		}
		return 0;
	}

	@Override
	public void moveTo(Entity entity, BMove.Data move) throws Exception {
		var cube = entity.getCube();
		// 计算新index，并根据进入新Cube的距离完成数据更新。
		var newIndex = map.toIndex(move.getPosition());
		var newCube = map.getOrAdd(newIndex);

		// 先最大化锁住所有cube，保证可用。
		//  一次锁住较少的cube的方案需要验证是否可行，备注-微信群里有点分析。
		var dp = cube.index.distancePerpendicular(newIndex);
		switch (dp) {
		case 0:
			var cubes = map.center(cube.index, rangeX, rangeY, rangeZ);
			try (var ignored = new LockGuard(cubes)) {
				update(entity, move, cube, newCube);
				processNotify(entity, cubes, eOperateIdMove, move);
			}
			break;

		case 1:
			// 【优化】进入到临近的cube，快速得到enter & leave。
			// 对于较少的视野，比如就通知9宫格，这个优化应该不明显。
			var dx = newIndex.x - cube.index.x;
			var dy = newIndex.y - cube.index.y;
			var dz = newIndex.z - cube.index.z;

			var fastEnters = new TreeMap<CubeIndex, Cube>();
			fastCollectWithFixedX(fastEnters, newIndex, dx);
			fastCollectWithFixedY(fastEnters, newIndex, dy);
			fastCollectWithFixedZ(fastEnters, newIndex, dz);

			var fastLeaves = new TreeMap<CubeIndex, Cube>();
			fastCollectWithFixedX(fastLeaves, cube.index, -dx);
			fastCollectWithFixedY(fastLeaves, cube.index, -dy);
			fastCollectWithFixedZ(fastLeaves, cube.index, -dz);

			var locks1 = map.center(newCube.index, rangeX, rangeY, rangeZ);
			// 计算不变的部分Cube。
			var notifies1 = new TreeMap<CubeIndex, Cube>();
			for (var e : locks1.entrySet()) {
				if (!fastEnters.containsKey(e.getKey()))
					notifies1.put(e.getKey(), e.getValue());
			}
			//locks1.putAll(fastEnters); // fastEnters 肯定在以newCube为中心的"9"宫格内。
			locks1.putAll(fastLeaves);
			try (var ignored = new LockGuard(locks1)) {
				update(entity, move, cube, newCube);
				processEnters(entity, fastEnters);
				processLeaves(entity, fastLeaves);
				processNotify(entity, notifies1, eOperateIdMove, move);
			}
			break;

		default:
			// 到达变化步长超过1的新的位置（这个实际上能处理任意步长）
			var locks2 = new TreeMap<CubeIndex, Cube>();
			var olds = map.center(cube.index, rangeX, rangeY, rangeZ);
			locks2.putAll(olds); // 必须先收集，后面的diff会修改olds。
			var news = map.center(newCube.index, rangeX, rangeY, rangeZ);
			locks2.putAll(news);

			var enters = new TreeMap<CubeIndex, Cube>();
			var notifies = new TreeMap<CubeIndex, Cube>();
			diff(olds, news, enters, notifies);

			try (var ignored = new LockGuard(locks2)) {
				update(entity, move, cube, newCube);
				processEnters(entity, enters);
				processLeaves(entity, olds);
				processNotify(entity, notifies, eOperateIdMove, move);
			}
			break;
		}
	}

	protected void processNotify(Entity self, SortedMap<CubeIndex, Cube> notifies, int operateId, Data operate) {
		var targets = new ArrayList<Entity>();
		var aoiOperates = new BAoiOperates.Data();

		// operate 总是修改一个实体，不需要处理tree问题。
		var aoiOperate = new BAoiOperate.Data();

		aoiOperate.setOperateId(operateId);
		var bb = ByteBuffer.Allocate();
		operate.encode(bb);
		aoiOperate.setParam(new Binary(bb));
		aoiOperates.getOperates().put(self.getId(), aoiOperate);

		for (var cube : notifies.values()) {
			for (var entity : cube.objects.values()) {
				if (entity.isPlayer())
					targets.add(entity);
			}
		}
		if (!targets.isEmpty())
			world.getLinkSender().sendCommand(targets, map.getInstanceId(), BCommand.eAoiOperate, aoiOperates);
	}

	protected void processEnters(Entity self,
								 SortedMap<CubeIndex, Cube> enters) throws IOException {
		var targets = new ArrayList<Entity>();
		// 收集玩家对象，用来发送自己进入的通知。
		var aoiEnters = new BAoiOperates.Data();
		for (var enter : enters.values()) {
			for (var e : enter.objects.entrySet()) {
				Entity.buildNonePlayerTree(aoiEnters.getOperates(), e.getValue().lastParent(), this::encodeEnter);
				Entity.buildPlayer(aoiEnters.getOperates(), e.getValue(), this::encodeEnter);

				if (e.getValue().isPlayer())
					targets.add(e.getValue());

				// 限制一次传输过多数据，达到数量，马上发送。
				if (aoiEnters.getOperates().size() > 200) {
					world.getLinkSender().sendCommand(
							self.getBean().getLinkName(), self.getBean().getLinkSid(),
							map.getInstanceId(), BCommand.eAoiEnter, aoiEnters);
					aoiEnters.getOperates().clear();
				}
			}
		}
		if (!aoiEnters.getOperates().isEmpty()) {
			world.getLinkSender().sendCommand(
					self.getBean().getLinkName(), self.getBean().getLinkSid(),
					map.getInstanceId(), BCommand.eAoiEnter, aoiEnters);
		}
		if (!targets.isEmpty())
		{
			// encode 自己的数据。
			var enterMe = new BAoiOperates.Data();
			Entity.buildPlayer(enterMe.getOperates(), self, this::encodeEnter);
			world.getLinkSender().sendCommand(targets, map.getInstanceId(), BCommand.eAoiEnter, enterMe);
		}
	}

	protected void processLeaves(Entity self,
								 SortedMap<CubeIndex, Cube> leaves) throws IOException {
		// 【优化】
		// 客户端也使用CubeMap结构组织数据，那么只需要打包发送所有leaves的CubeIndex.
		var aoiLeaves = new BAoiLeaves.Data();
		var targets = new ArrayList<Entity>();
		for (var leave : leaves.values()) {
			for (var entity : leave.objects.values()) {
				if (entity.isPlayer())
					targets.add(entity); // 收集通知自己离开的目标。

				aoiLeaves.getKeys().add(entity.getId());
				if (aoiLeaves.getKeys().size() > 200) {
					world.getLinkSender().sendCommand(
							self.getBean().getLinkName(), self.getBean().getLinkSid(),
							map.getInstanceId(), BCommand.eAoiLeave, aoiLeaves);
					aoiLeaves.getKeys().clear();
				}
			}
		}
		if (!aoiLeaves.getKeys().isEmpty()) {
			world.getLinkSender().sendCommand(
					self.getBean().getLinkName(), self.getBean().getLinkSid(),
					map.getInstanceId(), BCommand.eAoiLeave, aoiLeaves);
		}

		if (!targets.isEmpty()) {
			var removeMe = new BAoiLeaves.Data();
			removeMe.getKeys().add(self.getId());
			world.getLinkSender().sendCommand(targets, map.getInstanceId(), BCommand.eAoiLeave, removeMe);
		}
	}

	/**
	 * 计算差异。
	 * @param olds 旧的cubes，计算完成剩下的就是leave。
	 * @param news 新的cubes，可能和olds有重叠，计算后保持不变。
	 * @param enters 计算完成后，确实是新增的。
	 * @param notifies 重叠的。
	 */
	public static void diff(SortedMap<CubeIndex, Cube> olds, SortedMap<CubeIndex, Cube> news,
							SortedMap<CubeIndex, Cube> enters, SortedMap<CubeIndex, Cube> notifies) {
		for (var cube : news.values()) {
			var exist = olds.remove(cube.index);
			if (null == exist) {
				enters.put(cube.index, cube); // 新增的
			} else {
				notifies.put(cube.index, exist); // 重叠的
			}
		}
	}

	@Override
	public void notify(Entity self, int operateId, Data operate) throws IOException {
		var index = map.toIndex(self.getBean().getMoving().getPosition());
		var centers = map.center(index, rangeX, rangeY, rangeZ);

		// 由于notify的处理，客户端在没有enter时可以忽略。
		//  这里应该可以一个一个cube锁定。先最大化锁定！
		try (var ignored = new LockGuard(centers)) {
			processNotify(self, centers, operateId, operate);
		}
	}
}
