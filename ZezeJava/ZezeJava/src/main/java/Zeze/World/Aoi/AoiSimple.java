package Zeze.World.Aoi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BCubeIndex;
import Zeze.Builtin.World.BCubeIndexs;
import Zeze.Builtin.World.BCubePutData;
import Zeze.Builtin.World.BCubeRemoveData;
import Zeze.Builtin.World.BEditData;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.BObjectId;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.World.CubeIndex;
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

	private final HashMap<BObjectId, Cube> objects = new HashMap<>();

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

	public void fastCollectX(SortedMap<CubeIndex, Cube> result, CubeIndex center, long dx) {
		if (dx != 0) {
			var i = center.x + dx * rangeX;
			for (var j = center.y - rangeY; j <= center.y + rangeY; ++j) {
				for (var k = center.z - rangeZ; k <= center.z + rangeZ; ++k) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	public void fastCollectY(SortedMap<CubeIndex, Cube> result, CubeIndex center, long dy) {
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

	public void fastCollectZ(SortedMap<CubeIndex, Cube> result, CubeIndex center, long dz) {
		if (dz != 0) {
			var k = center.z + dz * rangeZ;
			for (var i = center.x - rangeX; i <= center.x + rangeX; ++i) {
				for (var j = center.y - rangeY; j <= center.y + rangeY; ++j) {
					map.collect(result, new CubeIndex(i, j, k));
				}
			}
		}
	}

	@Override
	public void moveTo(BObjectId oid, Vector3 position) throws Exception {
		var cube = objects.get(oid);
		if (null == cube) {
			logger.error("object cube not found. oid={}", World.format(oid));
			return;
		}

		var object = cube.objects.get(oid);
		if (null == object) {
			logger.error("object not found. oid={}, cube={}", World.format(oid), cube.index);
			return;
		}

		// 更新到新的坐标
		object.setPosition(position);

		// 计算新index，并根据进入新Cube的距离完成数据更新。

		var newIndex = map.toIndex(position);
		var dp = (int)cube.index.distancePerpendicular(newIndex);
		switch (dp) {
		case 0:
			// same cube
			// 根据位置同步协议，可能需要向第三方广播一下。
			// 如果位置同步协议总是即时的依赖发起方的命令。
			// 那除非算法要求，同一个cube是不需要广播的。
			break;

		case 1:
			// 【优化】进入到临近的cube，快速得到enter & leave。
			var dx = newIndex.x - cube.index.x;
			var dy = newIndex.y - cube.index.y;
			var dz = newIndex.z - cube.index.z;

			var fastEnters = new TreeMap<CubeIndex, Cube>();
			fastCollectX(fastEnters, newIndex, dx);
			fastCollectY(fastEnters, newIndex, dy);
			fastCollectZ(fastEnters, newIndex, dz);

			var fastLeaves = new TreeMap<CubeIndex, Cube>();
			fastCollectX(fastLeaves, cube.index, -dx);
			fastCollectY(fastLeaves, cube.index, -dy);
			fastCollectZ(fastLeaves, cube.index, -dz);

			processEnters(cube, oid, object, fastEnters);
			processLeaves(cube, oid, object, fastLeaves);
			break;

		default:
			// 跳过cube到达全新的位置
			var olds = map.center(cube.index, rangeX, rangeY, rangeZ);
			var news = map.center(newIndex, rangeX, rangeY, rangeZ);
			var enters = new TreeMap<CubeIndex, Cube>();
			diff(olds, news, enters);
			processEnters(cube, oid, object, enters);
			processLeaves(cube, oid, object, olds);
			break;
		}
	}

	protected void processEnters(Cube my, BObjectId oid, BObject self,
								 SortedMap<CubeIndex, Cube> enters) throws IOException {
		for (var enter : enters.values()) {
			// 收集玩家对象，用来发送自己进入的通知。
			var linkSids = new ArrayList<Long>();
			try (var ignored = new LockGuard(enter)) {
				var putData = new BCubePutData.Data();

				// 少new一个对象
				putData.getCubeIndex().setX(enter.index.x);
				putData.getCubeIndex().setY(enter.index.y);
				putData.getCubeIndex().setZ(enter.index.z);

				for (var e : enter.objects.entrySet()) {
					var editData = new BEditData.Data();
					editData.setObjectId(e.getKey());
					editData.setEditId(IAoi.eEditIdFull);
					encodeEdit(IAoi.eEditIdFull, e.getKey(), e.getValue(), editData);
					putData.getDatas().add(editData);

					linkSids.add(e.getValue().getLinksid());
					// 限制一次传输过多数据，达到数量，马上发送。
					if (putData.getDatas().size() > 200) {
						world.sendCommand(self.getLinksid(), BCommand.eCubePutData, putData);
						putData.getDatas().clear();
					}
				}

				if (!putData.getDatas().isEmpty()) {
					world.sendCommand(self.getLinksid(), BCommand.eCubePutData, putData);
				}
			}
			// encode 自己的数据。
			{
				var myData = new BCubePutData.Data();
				myData.getCubeIndex().setX(my.index.x);
				myData.getCubeIndex().setY(my.index.y);
				myData.getCubeIndex().setZ(my.index.z);
				var editData = new BEditData.Data();
				editData.setObjectId(oid);
				editData.setEditId(IAoi.eEditIdFull);
				encodeEdit(IAoi.eEditIdFull, oid, self, editData);
				myData.getDatas().add(editData);
				world.sendCommand(linkSids, BCommand.eCubePutData, myData);
			}
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public void encodeEdit(int editId, BObjectId oid, BObject data, BEditData.Data edit) {
		if (editId != IAoi.eEditIdFull)
			throw new RuntimeException("special editId found, but encodeEdit not override.");

		// eEditIdFull 默认实现是传输整个对象数据。
		// 实际上这个一般也需要定制。

		var bb = ByteBuffer.Allocate();
		data.encode(bb);
		edit.setData(new Binary(bb));
	}

	protected void processLeaves(Cube my, BObjectId oid, BObject self,
								 SortedMap<CubeIndex, Cube> leaves) throws IOException {
		// 【优化】
		// 客户端也使用CubeMap结构组织数据，那么只需要打包发送所有leaves的CubeIndex.
		var indexes = new BCubeIndexs.Data();
		for (var leave : leaves.values()) {
			indexes.getCubeIndexs().add(new BCubeIndex.Data(leave.index.x, leave.index.y, leave.index.z));
		}
		world.sendCommand(self.getLinksid(), BCommand.eCubeLeaves, indexes);

		var linkSids = new ArrayList<Long>();
		try (var ignored = new LockGuard(leaves)) {
			for (var cube : leaves.values()) {
				for (var obj : cube.objects.values())
					linkSids.add(obj.getLinksid());
			}
		}

		var remove = new BCubeRemoveData.Data();
		remove.getCubeIndex().setX(my.index.x);
		remove.getCubeIndex().setY(my.index.y);
		remove.getCubeIndex().setZ(my.index.z);
		remove.getKeys().add(oid);
		world.sendCommand(linkSids, BCommand.eCubeRemoveData, remove);
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
	public void commitEdit(BObjectId oid, int editId) {

	}
}
