package Zeze.Util;

@FunctionalInterface
public interface Factory<T> {
	T create();
}
