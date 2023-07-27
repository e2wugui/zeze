package Zeze.World;

import java.util.SortedMap;

/**
 * 得到以center为相关点周边的Cubes。
 * 尽量提供常用选择实现。
 * 都不满足要求时，还可以自己实现。
 */
public interface ISelector {

	default ICompute.Context beginSelect() {
		return new ICompute.Context();
	}

	/**
	 * 以origin.position为原点，按一定规则选中附近cube。
	 * 【多线程使用这个结果加锁。】
	 *
	 * @param context 计算上下文
	 * @return cubes
	 */
	SortedMap<CubeIndex, Cube> cubes(ICompute.Context context);

	/**
	 * 以origin.position为原点，按一定规则选中附近的entity。
	 * 【使用List描述结果，因为结果可能需要排序】
	 *
	 * @param context 计算上下文
	 * @return entities
	 */
	java.util.List<Entity> entities(ICompute.Context context);
}
