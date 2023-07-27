package Zeze.World;

import java.util.List;
import java.util.SortedMap;

/**
 * 移动同步之外其他逻辑计算接口。
 */
public interface ICompute {
	class Context {
		public ICompute compute;
		public SortedMap<CubeIndex, Cube> cubes;
	}

	ISelector selector();
	Entity caster();
	void compute(List<Entity> targets);
}
