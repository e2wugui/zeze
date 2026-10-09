package harness;

import java.util.ArrayList;
import java.util.List;

/**
 * @Fast 并行车道（同 JVM 类级并发）有库 App 的 serverId 统一分配处。
 * 每个 serverId 号段 [base, base+growth) 属单一持有类；Application.start 对
 * zeze_cache_&lt;serverId&gt; 先锁后删再开，同段并发即 FileMutex fail-fast。
 * 新增有库 serverId 只准：本文件加一行 seg(...)、TakeoverTestEnv.newConf 动态
 * 发号（100-199 已预留）、或 setNoDatabase(true)。号段两两不重叠由
 * TestFastAdmissionGuard 机械校验。净层（无 Application：Agent/OnzServer/Daemon/
 * GCM 等）与仅会话身份的 serverId 不占缓存目录，不入本表。
 */
public final class FastServerIds {

	/** 号段条目：[base, base+growth) 属 owner 独占；growth=该类一次全量运行的最大发号数。 */
	public record Segment(String owner, int base, int growth) {
	}

	private static final List<Segment> SEGMENTS = new ArrayList<>();

	private static int seg(String owner, int base, int growth) {
		SEGMENTS.add(new Segment(owner, base, growth));
		return base;
	}

	/** TestFastAdmissionGuard 校验用全量快照。 */
	public static List<Segment> segments() {
		return List.copyOf(SEGMENTS);
	}

	// DatabaseMemory 同 JVM 按 url 静态分桶：同名 url 的库实例直接共享存储（静默串
	// 数据，无任何报警）。固定字面量 url 必须登记为下方常量；"前缀_"+serverId 派生
	// 式随号段唯一，免登记。dbhome 等非内存库 url 不在此维度。

	private static final List<String> MEMORY_URLS = new ArrayList<>();

	private static String memUrl(String url) {
		MEMORY_URLS.add(url);
		return url;
	}

	/** TestFastAdmissionGuard 查重用全量快照。 */
	public static List<String> memoryUrls() {
		return List.copyOf(MEMORY_URLS);
	}

	public static final String URL_TEST_VERSION_BUCKET = memUrl("version_bucket_memory");
	public static final String URL_TEST_SUPERSEDE_CLOSES = memUrl("supersede_closes_memory");
	public static final String URL_TEST_REMOVE_SERVER = memUrl("remove_server_memory");
	public static final String URL_TEST_CACHE_DIR_LOCK = memUrl("cache_dir_lock_memory");
	public static final String URL_TEST_CREATE_DATABASE = memUrl("create_database_memory");
	public static final String URL_TEST_RENAME_TABLE = memUrl("rename_table_memory");
	public static final String URL_TEST_CLEAR_IN_USE = memUrl("clear_in_use_memory");
	public static final String URL_TEST_REDO_QUEUE_SERVER_DISPATCH = memUrl("redo_queue_server_dispatch_test");
	public static final String URL_TEST_MEMORY_EMPTY_VALUE_REPLACE = memUrl("empty_value_replace");
	public static final String URL_TEST_WALK_INTERRUPT_COUNT = memUrl("walk_interrupt_count");
	public static final String URL_TEST_HOT_INSTALL_RESIDUE = memUrl("hot_install_residue_memory");
	public static final String URL_TEST_HOT_TRY_DISTRIBUTE = memUrl("hot_try_distribute_memory");
	public static final String URL_TEST_ADD_TABLE = memUrl("add_table_memory");
	public static final String URL_TEST_HISTORY_FLUSH_COMMIT_BINDING = memUrl("history_commit_binding_unit");
	public static final String URL_TEST_HOT_TRY_DISTRIBUTE_GUARD = memUrl("hot_trydistribute_test");
	public static final String URL_TEST_KV_KEY_LENGTH_PAGED_WALK = memUrl("kv_keylen_pagedwalk");
	public static final String URL_TEST_DYNAMIC_BEAN_ELEMENT_LOG_KEY = memUrl("test_dynamic_elk_memory");


	// 按基点升序；growth 取该类 @Test 数×发号调用点（计数器）或 1（固定号）。

	public static final int TAKEOVER_POOL = seg("TakeoverTestEnv.newConf动态池", 100, 100);

	// 动态池共享发号：所有从100-199段取号的测试必须经同一计数器——各自从基点起号
	// 会在并行@Fast下互撞（FileMutex fail-fast即红）。TakeoverTestEnv与各直接取号测试共用。
	private static final java.util.concurrent.atomic.AtomicInteger TAKEOVER_POOL_NEXT =
			new java.util.concurrent.atomic.AtomicInteger(TAKEOVER_POOL);

	public static int takeoverPoolNext() {
		return TAKEOVER_POOL_NEXT.getAndIncrement();
	}

	public static final int TEST_AUTO_KEY_INVALIDATE_RANGE = seg("TestAutoKeyInvalidateRange", 200, 7);

	public static final int TEST_DEPARTMENT_TREE_MANAGER_GUARDS = seg("TestDepartmentTreeManagerGuards", 730, 1);
	public static final int TEST_DEPARTMENT_TREE_ROOT_GUARDS = seg("TestDepartmentTreeRootGuards", 733, 1);
	public static final int TEST_DEPARTMENT_TREE_DESTROY = seg("TestDepartmentTreeDestroy", 738, 2);

	public static final int TEST_ALWAYS_RELEASE_LOCK_WHEN_REDO = seg("TestAlwaysReleaseLockWhenRedo", 750, 2);
	public static final int TEST_BAG_PARTITION = seg("TestBagPartition", 760, 1);
	public static final int TEST_REDO_QUEUE_SERVER_DISPATCH = seg("TestRedoQueueServerDispatch", 761, 1);
	public static final int TEST_CLEAR_FAIL_FAST = seg("TestClearFailFast", 762, 1);
	public static final int TEST_BAG_MOVE_ZERO = seg("TestBagMoveZero", 770, 1);

	public static final int TEST_HOT_ROLLBACK_MEMORY_TABLE = seg("TestHotRollbackMemoryTable", 800, 1);
	public static final int TEST_HOT_UPGRADE_INCOMPATIBLE_FAILFAST = seg("TestUpgradeIncompatibleFailFast", 810, 1);
	public static final int TEST_GET_OR_ADD_IS_ADD_CONTRACT = seg("TestGetOrAddIsAddContract", 820, 1);

	public static final int TEST_ONZ_REDO_ROTATION_CURSOR = seg("TestOnzRedoRotationCursor", 857, 2);

	public static final int TEST_HTTP_SESSION_FIXATION = seg("TestHttpSessionFixation", 1410, 3);

	public static final int TEST_LIST_ITERATOR_FAILFAST = seg("TestListIteratorFailFast", 7070, 1);
	public static final int TEST_SET_BULK_CHANGE_RETURN = seg("TestSetBulkChangeReturn", 7080, 1);
	public static final int TEST_CHECKPOINT_IMMEDIATELY = seg("TestCheckpointImmediately", 7123, 1);
	public static final int TEST_HISTORY_GID_FAIL_CLEAN = seg("TestHistoryGidFailClean", 7125, 1);
	public static final int TEST_ONLINE_HOT_STOP_EVENT_REF = seg("TestOnlineHotStopEventRef", 7150, 3);
	public static final int TEST_TIMER_STOP_START_RESTART = seg("TestTimerStopStartRestart", 7160, 2);
	public static final int TEST_PROCESS_LINK_BROKEN_NON_ROLE_CONTEXT = seg("TestProcessLinkBrokenNonRoleContext", 7250, 3);
	public static final int TEST_TRANSACTION_DECODE_FAIL_CLOSES = seg("TestTransactionDecodeFailCloses", 7300, 2);
	public static final int TEST_MID_FLUSH_HALT_WINDOW = seg("TestMidFlushHaltWindow", 7310, 1);
	public static final int TEST_FLUSH_UNIT_ISOLATION = seg("TestFlushUnitIsolation", 7312, 1);
	public static final int TEST_TABLE_X_MIRROR_MISS_FALLBACK = seg("TestTableXMirrorMissFallback", 7313, 1);
	public static final int TEST_VERIFY_BATCH_RC_LOGGED = seg("TestVerifyBatchRcLogged", 7320, 2);
	public static final int TEST_REDIRECT_REMOVE_LOCAL_RC_LOGGED = seg("TestRedirectRemoveLocalRcLogged", 7340, 2);
	public static final int TEST_RANK_CACHE_EVICT = seg("TestRankCacheEvict", 7350, 4);
	public static final int TEST_RANK_COUNT_NEED_KEY = seg("TestRankCountNeedKey", 7360, 1);
	public static final int TEST_RANK_SINGLE_SEGMENT_MERGE = seg("TestRankSingleSegmentMerge", 7370, 1);
	public static final int TEST_HISTORY_FLUSH_COMMIT_BINDING = seg("TestHistoryFlushCommitBinding", 7371, 1);
	public static final int TEST_RANK_CACHE_ROLLBACK_POLLUTION = seg("TestRankCacheRollbackPollution", 7390, 1);
	public static final int TEST_PROVIDER_DIRECT_ALL_REDO_LEAK = seg("TestProviderDirectAllRedoLeak", 7410, 1);
	public static final int TEST_DYNAMIC_BEAN_COLLECT = seg("TestDynamicBeanCollect", 7420, 2);
	public static final int TEST_SAGA_BUSINESS_LOCK_SERIALIZATION = seg("TestSagaBusinessLockSerialization", 7430, 2);
	public static final int TEST_LOGIN_TIMES_REDO_ONCE = seg("TestLoginTimesRedoOnce", 7440, 2);
	public static final int TEST_SAGA_END_BEFORE_SAGA_REGISTRATION = seg("TestSagaEndBeforeSagaRegistration", 7450, 3);
	public static final int TEST_SAGA_CANCEL_DECODE_KEEPS_CONTEXT = seg("TestSagaCancelDecodeKeepsContext", 7460, 2);
	public static final int TEST_APPLICATION_RESTART_CONTRACT = seg("TestApplicationRestartContract", 7470, 1);
	public static final int TEST_GAME_LINK_BROKEN_TRIGGER_RC = seg("TestGameLinkBrokenTriggerRc", 7480, 4);
	public static final int TEST_CLEAR_TABLE_CACHE_TIMERS = seg("TestClearTableCacheTimers", 7490, 2);
	public static final int TEST_HOT_UPGRADE_MEMORY_TABLE_DATA = seg("TestHotUpgradeMemoryTableData", 7500, 1);
	public static final int TEST_LINKED_MAP_BROKEN_DATA = seg("TestLinkedMapBrokenData", 7510, 4);
	public static final int TEST_HOT_REPLACE_TABLE_CACHE_CLOSE = seg("TestHotReplaceTableCacheClose", 7520, 2);
	public static final int TEST_DELAY_REMOVE_ON_TIMER = seg("TestDelayRemoveOnTimer", 7530, 3);
	public static final int TEST_REDUCE_INVALID_ALL_FLUSH = seg("TestReduceInvalidAllFlush", 7540, 1);
	public static final int TEST_QUEUE_COMPATIBLE = seg("TestQueueCompatible", 7550, 3);
	public static final int TEST_INVALID_DIRTY_LOAD = seg("TestInvalidDirtyLoad", 7560, 2);
	public static final int TEST_APPLY_HELPER_CURSOR_HOLE = seg("TestApplyHelperCursorHole", 7570, 4);
	public static final int TEST_TABLE_CACHE_LRU = seg("TestTableCacheLru", 7580, 7);
	public static final int TEST_CROSS_FAMILY_CANCEL = seg("TestCrossFamilyCancel", 7590, 3);
	public static final int TEST_MEMORY_TABLE_SIZE = seg("TestMemoryTableSize", 7600, 1);
	public static final int TEST_CHECKPOINT_RUN_THREAD_SENTINEL = seg("TestCheckpointRunThreadSentinel", 7630, 1);
	public static final int TEST_TIMER_LOAD_MISSFIRE_ASYNC = seg("TestTimerLoadMissfireAsync", 7640, 1);
	public static final int TEST_TIMER_LOAD_EXHAUSTED_CRON = seg("TestTimerLoadExhaustedCron", 7650, 1);
	public static final int TEST_STOP_COMMIT_GATE = seg("TestStopCommitGate", 7660, 1);
	public static final int TEST_FLUSH_WHEN_REDUCE_NULL_CHECKPOINT = seg("TestFlushWhenReduceNullCheckpoint", 7670, 1);
	public static final int TEST_STOP_STEP_ISOLATION = seg("TestStopStepIsolation", 7680, 1);
	public static final int TEST_DB_WEB_TOKEN = seg("TestDbWebToken", 7690, 1);
	public static final int TEST_FINAL_ACTIONS_ISOLATION = seg("TestFinalActionsIsolation", 7700, 2);
	public static final int TEST_PLIST2_ATTACH_ORDER = seg("TestPList2AttachOrder", 7710, 1);
	public static final int TEST_REDIRECT_FUTURE_COMMITTED_RESULT = seg("TestRedirectFutureCommittedResult", 7720, 2);
	public static final int TEST_LINKED_MAP_BROKEN_CHAIN_LOGGED = seg("TestLinkedMapBrokenChainLogged", 8790, 4);
	public static final int TEST_ONLINE_RELIABLE_LOGOUT = seg("TestOnlineReliableLogout", 8810, 2);
	public static final int TEST_ONLINE_LOGIN_RETRY_LIMIT = seg("TestOnlineLoginRetryLimit", 8820, 2);
	public static final int TEST_ONLINE_LOGIN_VALIDATION = seg("TestOnlineLoginValidation", 8830, 2);

	public static final int TEST_HOT_INSTALL_START_LAST_FILTER = seg("TestInstallStartLastFilter", 12810, 1);
	public static final int TEST_HOT_TRY_DISTRIBUTE_SUCCESS_DELETE_FAIL = seg("TestTryDistributeSuccessDeleteFail", 12811, 1);
	public static final int TEST_COMMIT_PATH_SALVAGE = seg("TestCommitPathSalvage", 12818, 1);
	public static final int TEST_CHECKPOINT_RUN_RACE = seg("TestCheckpointRunRace", 12821, 1);
	public static final int TEST_RENAME_TABLE_AFTER_COMPAT_CHECK = seg("TestRenameTableAfterCompatCheck", 12822, 1);
	public static final int TEST_ADD_TABLE_ATOMICITY = seg("TestAddTableAtomicity", 12823, 1);
	public static final int TEST_CLEAR_IN_USE_CLOSES_CREATED_DATABASES = seg("TestClearInUseClosesCreatedDatabases", 12824, 1);
	public static final int TEST_CACHE_DIR_LOCK = seg("TestCacheDirLock", 12826, 1);
	public static final int TEST_APPLY_CURSOR_PERSISTENCE = seg("TestApplyCursorPersistence", 12828, 1);
	public static final int TEST_APPLY_CURSOR_PERSISTENCE_MEMORY = seg("TestApplyCursorPersistence#memory", 12829, 1);
	public static final int TEST_CREATE_DATABASE_CLOSES_ON_FAILURE = seg("TestCreateDatabaseClosesOnFailure", 12850, 1);
	public static final int TEST_ONZ_ROLLBACK_AFTER_READY = seg("TestOnzRollbackAfterReady", 12876, 1);
	public static final int TEST_ONZ_READY_WAIT_INTERRUPTED = seg("TestOnzReadyWaitInterrupted", 12880, 1);
	public static final int TEST_ONZ_COMMIT_DIVERGENCE_SIGNAL = seg("TestOnzCommitDivergenceSignal", 12881, 1);

	public static final int TEST_OFFLINE_TIMER_BOOKKEEPING = seg("TestOfflineTimerBookkeeping", 16181, 5);
	public static final int TEST_RC_PASSTHROUGH = seg("TestRcPassthrough", 16191, 6);
	public static final int TEST_TRANSMIT_UNKNOWN_ACTION = seg("TestTransmitUnknownAction", 16201, 3);
	public static final int TEST_LOAD_BASE_CHAIN_RESILIENCE = seg("TestLoadBaseChainResilience", 16211, 10);
	public static final int TEST_QUEUE_BROKEN_CHAIN_DIAGNOSIS = seg("TestQueueBrokenChainDiagnosis", 16221, 3);
	public static final int TEST_PRODUCER_TXN_SEND_REJECTS_ENV_TRANSACTION = seg("TestProducerTxnSendRejectsEnvTransaction", 16231, 1);
	public static final int TEST_PRODUCER_STOP_IDEMPOTENCY = seg("TestProducerStopIdempotency", 16233, 2);
	public static final int TEST_CLIENT_CONFIG_PASS_THROUGH = seg("TestClientConfigPassThrough", 16235, 1);
	public static final int TEST_PRODUCER_MULTIPLE_INSTANCES_SHARE_PROCESS = seg("TestProducerMultipleInstancesShareProcess", 16241, 2);
	public static final int TEST_PRODUCER_STOP_REBUILD_SAME_APPLICATION = seg("TestProducerStopRebuildSameApplication", 16243, 1);
	public static final int TEST_DYNAMIC_BEAN_ELEMENT_LOG_KEY = seg("TestDynamicBeanElementLogKey", 16251, 4);
	public static final int TEST_TIMER_HOT_WATCH_REF = seg("TestTimerHotWatchRef", 16261, 4);
	public static final int TEST_SET_MAP_PARTIAL_CHANGE_PHANTOM_DELTA = seg("TestSetMapPartialChangePhantomDelta", 16271, 2);
	public static final int TEST_GTABLE_STALE_PHANTOM_ROW = seg("TestGTableStaleRowAndPhantomRow", 16281, 2);
	public static final int TEST_PMAP2_COPY_DEEP = seg("TestPMap2CopyDeep", 16291, 1);
	public static final int TEST_DYNAMIC_TABLE_LOG_REGISTRY = seg("TestDynamicTableLogRegistry", 16301, 1);
	public static final int TEST_PRODUCER_CTOR_FAIL_REBUILDS = seg("TestProducerCtorFailRebuildsSameApplication", 16311, 1);
	public static final int TEST_PRODUCER_STOP_DRAINS_CHECK_BEFORE_SHUTDOWN = seg("TestProducerStopDrainsCheckBeforeShutdown", 16321, 1);
	public static final int TEST_HISTORY_SHARED_TABLE_OWNER_NAME_MISMATCH = seg("TestHistorySharedTableOwnerNameMismatch", 16331, 6);
	public static final int TEST_PRODUCER_START_REJECTS_MEMORY_TSENT = seg("TestProducerStartRejectsMemoryTSent", 16341, 1);
	public static final int TEST_HISTORY_SEGMENT_EXHAUST_CLEAN_FAIL = seg("TestHistorySegmentExhaustCleanFail", 16351, 1);
	public static final int TEST_PENDING_GID_LEDGER_SWEEP_STOPS_WITH_APP = seg("TestPendingGidLedgerSweepStopsWithApplication", 16361, 2);
	public static final int TEST_HISTORY_HTTP_SERVER_BIND_FAILURE = seg("TestHistoryHttpServerBindFailureFailsFast", 16371, 1);
	public static final int TEST_PRODUCER_PLAIN_SEND_REJECTS_ENV_TRANSACTION = seg("TestProducerPlainSendRejectsEnvTransaction", 16381, 1);
	public static final int TEST_RANK_CACHE_CROSS_TRANSACTION = seg("TestRankCacheCrossTransactionPollution", 16382, 1);
	public static final int TEST_ONLINE_LATE_LINK_BROKEN_GHOST = seg("TestOnlineLateLinkBrokenGhost", 16383, 1);
	public static final int TEST_TOO_MANY_TRY_LAST_EXCEPTION = seg("TestTooManyTryKeepsLastException", 16375, 1);
	public static final int TEST_FINAL_CALLBACK_ASSERTION = seg("TestFinalCallbackAssertionError", 16374, 1);

	private FastServerIds() {
	}
}
