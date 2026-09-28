package Zeze.Transaction;

/** 事务隔离级别：None 事务外；Serializable 全记录冲突检测；AllowDirtyWhenAllRead 纯读免锁。 */
public enum TransactionLevel {
    None,                  // 事务外
    Serializable,          // 所有访问的记录都没有发生修改，事务才成功。【Default】
    AllowDirtyWhenAllRead, // 当没有修改操作时，不整体判断所读的记录是否发生变化。此时不对记录进行加锁。
}
