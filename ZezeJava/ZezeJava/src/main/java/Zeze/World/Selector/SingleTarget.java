package Zeze.World.Selector;

import java.util.SortedMap;
import java.util.TreeMap;
import Zeze.World.Cube;
import Zeze.World.CubeIndex;
import Zeze.World.Entity;
import Zeze.World.ICompute;
import Zeze.World.ISelector;

/**
 * 以已经确定的单一目标实现ISelector。
 */
public class SingleTarget implements ISelector {
	private final Entity target;

	public SingleTarget(Entity target) {
		this.target = target;
	}

	public Entity getTarget() {
		return target;
	}

	@Override
	public SortedMap<CubeIndex, Cube> cubes(ICompute.Context context) {
		var origin = context.compute.caster();
		context.cubes = new TreeMap<>();
		context.cubes.put(origin.getCube().index, origin.getCube());
		if (origin != target)
			context.cubes.put(target.getCube().index, target.getCube());
		return context.cubes;
	}

	@Override
	public java.util.List<Entity> entities(ICompute.Context context) {
		return java.util.List.of(target);
	}
}
