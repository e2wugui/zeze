package Zeze.Util;

import org.pcollections.Empty;
import org.pcollections.PSet;

public class GameCube extends Cube<GameObjectId> {
	private PSet<GameObjectId> objectIds = Empty.set();

	public final PSet<GameObjectId> getObjectIds() {
		return objectIds;
	}

	@Override
	public void add(CubeIndex index, GameObjectId obj) {
		// under lock(cube)
		objectIds = objectIds.plus(obj);
	}

	/**
	 * 返回 True 表示 Cube 可以删除。这是为了回收内存。
	 *
	 * @param index cube index
	 * @param obj   game object id
	 */
	@Override
	public boolean remove(CubeIndex index, GameObjectId obj) {
		// under lock(cube)
		objectIds = objectIds.minus(obj);
		return objectIds.isEmpty();
	}
}
