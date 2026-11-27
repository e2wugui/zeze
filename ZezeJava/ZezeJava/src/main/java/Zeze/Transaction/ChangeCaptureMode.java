package Zeze.Transaction;

/** 集合事务的变更采集模式：NONE 不保留增量笔记；DELTA 保留增量供监听器或 History 消费。 */
public enum ChangeCaptureMode {NONE, DELTA}
