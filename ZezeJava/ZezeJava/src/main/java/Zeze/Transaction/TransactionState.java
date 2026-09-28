package Zeze.Transaction;

/** 事务生命周期状态：Running→（Abort / Redo / RedoAndReleaseLock）→Completed 终态。 */
public enum TransactionState {
    Running, // 正常运行状态。
    Abort, // 事务需要中止。
    Redo, // 事务需要重做。
    RedoAndReleaseLock, // 事务需要释放全部锁以后重做。
    Completed, // 事务已经完成，最后一个状态，不会再改变。
}
