package Zeze.Util;

import Zeze.*;
import java.util.*;

public class GameCube extends Cube<GameObjectId> {
	private ImmutableHashSet<GameObjectId> ObjectIds = ImmutableHashSet<GameObjectId>.Empty;
	public final ImmutableHashSet<GameObjectId> getObjectIds() {
		return ObjectIds;
	}
	private void setObjectIds(ImmutableHashSet<GameObjectId> value) {
		ObjectIds = value;
	}

	@Override
	public void Add(CubeIndex index, GameObjectId obj) {
		// under lock(cube)
		setObjectIds(getObjectIds().Add(obj));
	}

	/** 
	 返回 True 表示 Cube 可以删除。这是为了回收内存。
	 
	 @param index
	 @param obj
	 @return 
	*/
	@Override
	public boolean Remove(CubeIndex index, GameObjectId obj) {
		// under lock(cube)
		setObjectIds(getObjectIds().Remove(obj));
		return getObjectIds().Count == 0;
	}
}