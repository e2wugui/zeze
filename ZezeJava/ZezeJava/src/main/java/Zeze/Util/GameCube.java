package Zeze.Util;

import org.pcollections.Empty;
import org.pcollections.PSet;

public class GameCube extends Cube<GameObjectId> {
	private PSet<GameObjectId> ObjectIds = Empty.set();

	public final PSet<GameObjectId> getObjectIds() {
		return ObjectIds;
	}

	@Override
	public void Add(CubeIndex index, GameObjectId obj) {
		// under lock(cube)
		ObjectIds = ObjectIds.plus(obj);
	}

	/** 
	 返回 True 表示 Cube 可以删除。这是为了回收内存。
	 
	 @param index
	 cube index
	 @param obj
	 game object id
	 @return
	 true|false
	*/
	@Override
	public boolean Remove(CubeIndex index, GameObjectId obj) {
		// under lock(cube)
		ObjectIds = ObjectIds.minus(obj);
		return ObjectIds.isEmpty();
	}
}