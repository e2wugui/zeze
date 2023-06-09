package Zeze.Util;

public interface Lockey<Subclass> extends Comparable<Subclass>{
	Subclass alloc();
}
