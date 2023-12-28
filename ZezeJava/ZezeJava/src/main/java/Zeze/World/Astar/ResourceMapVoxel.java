package Zeze.World.Astar;

import Zeze.Serialize.Vector3;
import java.util.HashMap;
import java.util.List;

public class ResourceMapVoxel implements IResourceMap {
	private long width;
	private long height;
	private int unitWidth;
	private int unitHeight;
	private HashMap<VoxelIndex, List<VoxelNode>> voxels;

	@Override
	public long getWidth() {
		return width;
	}

	@Override
	public long getHeight() {
		return height;
	}

	@Override
	public float getUnitWidth() {
		return unitWidth;
	}

	@Override
	public float getUnitHeight() {
		return unitHeight;
	}

	@Override
	public NodeIndex toIndex(Vector3 position) {
		return IResourceMap.super.toIndex(position);
	}

	@Override
	public NodeIndex toIndex(int x, int z) {
		return IResourceMap.super.toIndex(x, z);
	}

	@Override
	public boolean walkable(long x, long z) {
		return false;
	}

	@Override
	public boolean walkable(NodeIndex from, int toX, int toZ, int toYIndex) {
		return IResourceMap.super.walkable(from, toX, toZ, toYIndex);
	}

	@Override
	public void traverseNeighbors(IAstar astar, Node current, Node target) {
		astar.traverseCross(this, current, target, toIndex(current.index.x, current.index.z - 1), 5);
		astar.traverseCross(this, current, target, toIndex(current.index.x - 1, current.index.z), 5);
		astar.traverseCross(this, current, target, toIndex(current.index.x + 1, current.index.z), 5);
		astar.traverseCross(this, current, target, toIndex(current.index.x, current.index.z + 1), 5);
	}
}
