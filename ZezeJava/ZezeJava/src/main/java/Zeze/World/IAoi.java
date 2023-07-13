package Zeze.World;

import java.io.IOException;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Transaction.Data;

/**
 * Aoi 接口。
 * 基本实现AoiSimple（简单的严格以自己为中心的周边格子组成的视野的同步）
 * 扩展实现AoiStable（处理玩家在格子分割线周边移动造成的不断Enter，Leave的问题）
 * 扩展实现AoiSubscribe?（有限的订阅列表）
 */
public interface IAoi {
	/**
	 * 进入aoi。
	 * @param oid oid
	 * @throws IOException
	 * @return error code, 0 success.
	 */
	long enter(long oid) throws IOException;

	/**
	 * 1. 移动到新的位置，会发起视野内的更新广播。
	 * 2. 当Cube发生了变更，还可能会发起enter，leave。
	 * 3. 当目标是玩家时，还会装载新加入的Cube的数据。
	 * 4. 等等。
	 */
	void moveTo(long oid, Vector3 position) throws Exception;

	/**
	 * 整个物体数据全量更新。
	 * 1. 玩家视野Cube新增时，装载新增Cube内物体的全部数据。
	 * 2. 物体视野Cube新增时，通知自己的全部数据给玩家。
 	 */
	int eOperateIdFull = 0;

	/**
	 * 物体发生了数据修改，使用这个方法发起变更广播。
	 * operateId > 0 是自定义编辑分类。
	 * 例子：
	 * 1. eOperateIdEquip = 1; 玩家装备发生了变化。
	 * 2. eOperateIdHorse = 2; 玩家坐骑发生了变化。
	 * 3. eOperateIdFight = 3; 战斗属性发生了变化。
	 * 4. ...
	 *
	 * 分类原则：
	 * 1. 优化网络传输。减少每次编辑需要传输的数据。
	 * 2. 优化客户端性能。编辑操作的数据传输到客户端以后，客户端需要的操作也能比较精确，不会过于浪费性能。
	 * 3. 分类适中。不要过细，也不要过粗。【废话】
	 *
	 * 问题：
	 * 1. 有了分类，每次编辑确定了OperateId以后，就不能随便修改编辑数据的范围。
	 *    如果需求发生了变化，建议新增OperateId。
	 *
	 * 自动增量更新探讨：
	 * 1. zeze实际已有完备的增量更新机制，能精确的把任意改动用最小的带宽把变更传输到客户端并更新它的数据。
	 * 2. 但是自动增量更新缺失了OperateId这样的分类。客户端是拿到了最新的数据，但无法区分到底什么修改了，无法优化操作。
	 * 3. 所以这个方案只适合用来做自动复制之类的功能。比如Raft之间的复制。
	 *
	 * @param self 编辑对象。
	 * @param operateId 操作Id。
	 * @param operate 编辑操作。仅客户端解释，需要能完成数据编辑，并且拥有完整的操作参数。
	 */
	void notify(Entity self, int operateId, Data operate) throws Exception;

	/**
	 * 默认实现是传输整个对象数据。
	 * 【实际上这个一般也需要定制】
	 * 【不能把服务器定义的数据全部都传给客户端】
 	 */
	default Binary encodeEnter(Entity e) {
		var bb = ByteBuffer.Allocate();
		e.getBean().encode(bb);
		return new Binary(bb);
	}

}
