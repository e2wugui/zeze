package Zeze.Game.Task;

public interface IBag {
	int count(int itemId);
	boolean remove(int itemId, int count);
}
