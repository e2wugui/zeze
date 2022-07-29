package Zeze.Transaction;

public enum DispatchMode {
	Normal, // 在普通线程池中执行。
	Critical, // 在重要线程池中执行。
	Direct, // 在调用者线程执行。
}
