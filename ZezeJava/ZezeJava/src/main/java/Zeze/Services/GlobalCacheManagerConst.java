package Zeze.Services;

import java.io.IOException;

public interface GlobalCacheManagerConst {
	IOException kickException = new IOException("GlobalCacheManager kick");

	int StateInvalid = 0;
	int StateShare = 1;
	int StateModify = 2;
	int StateRemoving = 3;

	int StateRemoved = 10; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
	int StateReduceRpcTimeout = 11; // 用来表示 reduce 超时失败。不是状态。
	int StateReduceException = 12; // 用来表示 reduce 异常失败。不是状态。
	int StateReduceNetError = 13; // 用来表示 reduce 网络失败。不是状态。
	int StateReduceDuplicate = 14; // 用来表示重复的 reduce。错误报告，不是状态。
	int StateReduceSessionNotFound = 15;
	int StateReduceErrorFreshAcquire = 16; // 错误码，too many try 处理机制

	int AcquireShareDeadLockFound = 21;
	int AcquireShareAlreadyIsModify = 22;
	int AcquireModifyDeadLockFound = 23;
	int AcquireErrorState = 24;
	int AcquireModifyAlreadyIsModify = 25;
	int AcquireShareFailed = 26;
	int AcquireModifyFailed = 27;
	int AcquireException = 28;
	int AcquireInvalidFailed = 29;
	int AcquireNotLogin = 30;
	int AcquireFreshSource = 31;

	int ReduceErrorState = 41;
	int ReduceShareAlreadyIsInvalid = 42;
	int ReduceShareAlreadyIsShare = 43;
	int ReduceInvalidAlreadyIsInvalid = 44;

	int CleanupErrorSecureKey = 60;
	int CleanupErrorGlobalCacheManagerHashIndex = 61;
	int CleanupErrorHasConnection = 62;

	int ReLoginBindSocketFail = 80;

	int NormalCloseUnbindFail = 100;

	int LoginBindSocketFail = 120;
}
