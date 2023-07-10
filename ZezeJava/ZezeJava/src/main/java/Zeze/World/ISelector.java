package Zeze.World;

import java.util.SortedMap;

/**
 * 得到以center为相关点周边的Cubes。
 * 尽量提供常用选择实现。
 * 都不满足要求时，还可以自己实现。
 */
public interface ISelector {
	SortedMap<CubeIndex, Cube> select(Entity center);
}
