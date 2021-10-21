package Zeze.Services;

public class GlobalCacheManagerServer {
    public static final int StateInvalid = 0;
    public static final int StateShare = 1;
    public static final int StateModify = 2;
    public static final int StateRemoved = -1; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
    public static final int StateReduceRpcTimeout = -2; // 用来表示 reduce 超时失败。不是状态。
    public static final int StateReduceException = -3; // 用来表示 reduce 异常失败。不是状态。
    public static final int StateReduceNetError = -4;  // 用来表示 reduce 网络失败。不是状态。

    public static final int AcquireShareDeadLockFound = 1;
    public static final int AcquireShareAlreadyIsModify = 2;
    public static final int AcquireModifyDeadLockFound = 3;
    public static final int AcquireErrorState = 4;
    public static final int AcquireModifyAlreadyIsModify = 5;
    public static final int AcquireShareFaild = 6;
    public static final int AcquireModifyFaild = 7;

    public static final int ReduceErrorState = 11;
    public static final int ReduceShareAlreadyIsInvalid = 12;
    public static final int ReduceShareAlreadyIsShare = 13;
    public static final int ReduceInvalidAlreadyIsInvalid = 14;

    public static final int AcquireNotLogin = 20;

    public static final int CleanupErrorSecureKey = 30;
    public static final int CleanupErrorGlobalCacheManagerHashIndex = 31;
    public static final int CleanupErrorHasConnection = 32;

    public static final int ReLoginBindSocketFail = 40;

    public static final int NormalCloseUnbindFail = 50;

    public static final int LoginBindSocketFail = 60;
}
