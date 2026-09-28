package Zeze.Transaction;

/** 检查点模式：Immediately 提交即同步落库；Table 挂脏集（RelativeRecordSet）由检查点线程批量落库。 */
public enum CheckpointMode {
	Immediately,
	Table
}