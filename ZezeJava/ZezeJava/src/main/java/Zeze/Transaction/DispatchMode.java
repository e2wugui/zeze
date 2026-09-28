package Zeze.Transaction;

/** 过程派发方式：选择过程在哪个线程池（或调用者线程）执行。 */
public enum DispatchMode {
	Normal, // 在普通线程池中执行。
	Critical, // 在重要线程池中执行。
	Direct, // 在调用者线程执行。
}
