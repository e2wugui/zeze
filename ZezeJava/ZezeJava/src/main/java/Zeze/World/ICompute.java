package Zeze.World;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

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
