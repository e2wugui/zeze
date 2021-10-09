package Zeze.Util;

import Zeze.*;
import java.util.*;

public abstract class Cube<TObject> {
	public static final int StateNormal = 0;
	public static final int StateRemoved = -1;

	/** 
	 子类实现可以利用这个状态，自定义状态必须大于等于0，负数保留给内部使用。
	*/
	private int State;
	public final int getState() {
		return State;
	}
	public final void setState(int value) {
		State = value;
	}

	// under lock(cube)
	public abstract void Add(CubeIndex index, TObject obj);

	/** 
	 返回 True 表示 Cube 可以删除。这是为了回收内存，如果不需要回收，永远返回false即可。
	 under lock(cube)
	 
	 @param index
	 @param obj
	 @return 
	*/
	public abstract boolean Remove(CubeIndex index, TObject obj);
}