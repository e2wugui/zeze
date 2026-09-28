package Zeze.Transaction;

/** 检查点落库并发模式：单/多线程，以及是否把多个脏集合并成 FlushSet 批量提交。 */
public enum CheckpointFlushMode {
	SingleThread,
	MultiThread,
	SingleThreadMerge,
	MultiThreadMerge
}
