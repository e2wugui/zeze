package Zeze.World.Aoi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import Zeze.Builtin.World.BAoiLeave;
import Zeze.Builtin.World.BAoiLeaves;
import Zeze.Builtin.World.BAoiOperates;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BCubeIndex;
import Zeze.Builtin.World.BObjectId;
import Zeze.Transaction.Data;
import Zeze.World.CubeIndex;
import Zeze.World.Entity;
import Zeze.World.LockGuard;
import Zeze.World.World;
import Zeze.Serialize.Vector3;
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

	private final HashMap<BObjectId, Cube> objectToCube = new HashMap<>();

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

	public void fastCollectWithFixedX(SortedMap<CubeIndex, Cube> result, CubeIndex center, long dx) {
		if (dx != 0) {
			var i = center.x + dx * rangeX;
			for (var j = center.y - rangeY; j <= center.y + rangeY; ++j) {
				for (var k = center.z - rangeZ; k <= center.z + rangeZ; ++k) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	public void fastCollectWithFixedY(SortedMap<CubeIndex, Cube> result, CubeIndex center, long dy) {
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

	public void fastCollectWithFixedZ(SortedMap<CubeIndex, Cube> result, CubeIndex center, long dz) {
		if (dz != 0) {
			var k = center.z + dz * rangeZ;
			for (var i = center.x - rangeX; i <= center.x + rangeX; ++i) {
				for (var j = center.y - rangeY; j <= center.y + rangeY; ++j) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	private void updateSelf(Entity self, Vector3 position, Cube cube, Cube newCube) {
		self.getBean().setPosition(position);
		if (cube != newCube) {
			var entity = cube.objects.remove(self.getId());
			newCube.objects.put(self.getId(), entity);
			objectToCube.put(self.getId(), newCube); // update to new cube
		}
	}

	@Override
	public void moveTo(BObjectId oid, Vector3 position) throws Exception {
		var cube = objectToCube.get(oid);
		if (null == cube) {
			// 第一次访问aoi，创建对象。
			var index = map.toIndex(position);
			cube = map.getOrAdd(index);
		}

		var entity = cube.objects.get(oid);
		if (null == entity) {
			// 第一次进入，enter 视野。
			// todo 新建对象初始化框架。
			cube.objects.put(oid, entity = new Entity(oid));
			objectToCube.put(oid, cube);
			entity.getBean().setPosition(position);
			var newIndex = map.toIndex(position);
			var enters = map.center(newIndex, rangeX, rangeY, rangeZ);
			processEnters(cube, entity, enters);
			return;
		}


		// 计算新index，并根据进入新Cube的距离完成数据更新。

		var newIndex = map.toIndex(position);
		var newCube = map.getOrAdd(newIndex);

		// todo 先最大化锁住所有cube，保证可用。
		//  一次锁住较少的cube的方案需要验证是否可行，备注-微信群里有点分析。
		var dp = (int)cube.index.distancePerpendicular(newIndex);
		switch (dp) {
		case 0:
			try (var ignored = new LockGuard(cube, newCube)) {
				updateSelf(entity, position, cube, newCube);
			}
			// same cube
			// 根据位置同步协议，可能需要向第三方广播一下。
			// 如果位置同步协议总是即时的依赖发起方的命令。
			// 那除非算法要求，同一个cube是不需要广播的。
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

			var locks1 = map.center(cube.index, rangeX, rangeY, rangeZ);
			locks1.putAll(fastEnters);
			locks1.putAll(fastLeaves);
			try (var ignored = new LockGuard(locks1)) {
				updateSelf(entity, position, cube, newCube);
				processEnters(cube, entity, fastEnters);
				processLeaves(cube, entity, fastLeaves);
			}
			break;

		default:
			// 跳过cube到达全新的位置
			var locks2 = new TreeMap<CubeIndex, Cube>();
			var olds = map.center(cube.index, rangeX, rangeY, rangeZ);
			locks2.putAll(olds); // 必须先收集，后面的diff会修改olds。
			var news = map.center(newIndex, rangeX, rangeY, rangeZ);
			locks2.putAll(news);

			var enters = new TreeMap<CubeIndex, Cube>();
			diff(olds, news, enters);

			try (var ignored = new LockGuard(locks2)) {
				updateSelf(entity, position, cube, newCube);
				processEnters(cube, entity, enters);
				processLeaves(cube, entity, olds);
			}
			break;
		}
	}

	protected void processEnters(Cube my, Entity self,
								 SortedMap<CubeIndex, Cube> enters) throws IOException {
		for (var enter : enters.values()) {
			// 收集玩家对象，用来发送自己进入的通知。
			var targets = new ArrayList<Entity>();
			var putData = new BAoiOperates.Data();

			// 少new一个对象
			putData.getCubeIndex().setX(enter.index.x);
			putData.getCubeIndex().setY(enter.index.y);
			putData.getCubeIndex().setZ(enter.index.z);

			for (var e : enter.objects.entrySet()) {
				Entity.buildTree(putData.getOperates(), e.getValue().root(), this::encodeEnter);

				if (e.getValue().isPlayer())
					targets.add(e.getValue());

				// 限制一次传输过多数据，达到数量，马上发送。
				if (putData.getOperates().size() > 200) {
					world.sendCommand(self.getBean().getLinkName(), self.getBean().getLinkSid(), BCommand.eAoiEnter, putData);
					putData.getOperates().clear();
				}

				if (!putData.getOperates().isEmpty()) {
					world.sendCommand(self.getBean().getLinkName(), self.getBean().getLinkSid(), BCommand.eAoiEnter, putData);
				}
			}
			// encode 自己的数据。
			{
				var enterMe = new BAoiOperates.Data();
				enterMe.getCubeIndex().setX(my.index.x);
				enterMe.getCubeIndex().setY(my.index.y);
				enterMe.getCubeIndex().setZ(my.index.z);
				Entity.buildTree(putData.getOperates(), self.root(), this::encodeEnter);
				world.sendCommand(targets, BCommand.eAoiEnter, enterMe);
			}
		}
	}

	protected void processLeaves(Cube my, Entity self,
								 SortedMap<CubeIndex, Cube> leaves) throws IOException {
		// 【优化】
		// 客户端也使用CubeMap结构组织数据，那么只需要打包发送所有leaves的CubeIndex.
		var indexes = new BAoiLeaves.Data();
		for (var leave : leaves.values()) {
			indexes.getCubeIndexs().add(new BCubeIndex.Data(leave.index.x, leave.index.y, leave.index.z));
		}
		world.sendCommand(self.getBean().getLinkName(), self.getBean().getLinkSid(), BCommand.eAoiLeaves, indexes);

		var targets = new ArrayList<Entity>();
		for (var cube : leaves.values()) {
			for (var obj : cube.objects.values()) {
				if (obj.isPlayer())
					targets.add(obj);
			}
		}

		var remove = new BAoiLeave.Data();
		remove.getCubeIndex().setX(my.index.x);
		remove.getCubeIndex().setY(my.index.y);
		remove.getCubeIndex().setZ(my.index.z);
		remove.getKeys().add(self.getId());
		world.sendCommand(targets, BCommand.eAoiLeave, remove);
	}

	/**
	 * 计算差异。
	 * @param olds 旧的cubes，计算完成剩下的就是leave。
	 * @param news 新的cubes，可能和olds有重叠，计算后保持不变。
	 * @param enters 计算完成后，确实是新增的。
	 */
	public static void diff(SortedMap<CubeIndex, Cube> olds, SortedMap<CubeIndex, Cube> news,
							SortedMap<CubeIndex, Cube> enters) {
		for (var cube : news.values()) {
			if (null == olds.remove(cube.index)) {
				enters.put(cube.index, cube);
			}
		}
	}

	@Override
	public void notify(BObjectId oid, int operateId, Data operate) {

	}
}
