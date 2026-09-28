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

	// 按基点升序；growth 取该类 @Test 数×发号调用点（计数器）或 1（固定号）。

	public static final int TAKEOVER_POOL = seg("TakeoverTestEnv.newConf动态池", 100, 100);

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
	public static final int TEST_FND14_HOT02_UPGRADE_INCOMPATIBLE_FAILFAST = seg("TestFnd14Hot02UpgradeIncompatibleFailFast", 810, 1);
	public static final int TEST_FND16_TXN02_GET_OR_ADD_IS_ADD_CONTRACT = seg("TestFnd16Txn02GetOrAddIsAddContract", 820, 1);

	public static final int TEST_FND857_HTTP_SESSION_FIXATION = seg("TestFnd857HttpSessionFixation", 1410, 3);

	public static final int TEST_FND707_LIST_ITERATOR_FAILFAST = seg("TestFnd707ListIteratorFailFast", 7070, 1);
	public static final int TEST_FND708_SET_BULK_CHANGE_RETURN = seg("TestFnd708SetBulkChangeReturn", 7080, 1);
	public static final int TEST_CHECKPOINT_IMMEDIATELY = seg("TestCheckpointImmediately", 7123, 1);
	public static final int TEST_ONLINE_HOT_STOP_EVENT_REF = seg("TestOnlineHotStopEventRef", 7150, 3);
	public static final int TEST_TIMER_STOP_START_RESTART = seg("TestTimerStopStartRestart", 7160, 2);
	public static final int TEST_PROCESS_LINK_BROKEN_NON_ROLE_CONTEXT = seg("TestProcessLinkBrokenNonRoleContext", 7250, 3);
	public static final int TEST_TRANSACTION_DECODE_FAIL_CLOSES = seg("TestTransactionDecodeFailCloses", 7300, 2);
	public static final int TEST_R3X_MID_FLUSH_HALT_WINDOW = seg("TestR3XMidFlushHaltWindow", 7310, 1);
	public static final int TEST_FLUSH_UNIT_ISOLATION = seg("TestFlushUnitIsolation", 7312, 1);
	public static final int TEST_TABLE_X_MIRROR_MISS_FALLBACK = seg("TestTableXMirrorMissFallback", 7313, 1);
	public static final int TEST_FND732_VERIFY_BATCH_RC_LOGGED = seg("TestFnd732VerifyBatchRcLogged", 7320, 2);
	public static final int TEST_FND732B_REDIRECT_REMOVE_LOCAL_RC_LOGGED = seg("TestFnd732bRedirectRemoveLocalRcLogged", 7340, 2);
	public static final int TEST_RANK_CACHE_EVICT = seg("TestRankCacheEvict", 7350, 4);
	public static final int TEST_RANK_COUNT_NEED_KEY = seg("TestRankCountNeedKey", 7360, 1);
	public static final int TEST_FND735_RANK_SINGLE_SEGMENT_MERGE = seg("TestFnd735RankSingleSegmentMerge", 7370, 1);
	public static final int TEST_HISTORY_FLUSH_COMMIT_BINDING = seg("TestHistoryFlushCommitBinding", 7371, 1);
	public static final int TEST_FND15_GAME01_RANK_CACHE_ROLLBACK_POLLUTION = seg("TestFnd15Game01RankCacheRollbackPollution", 7390, 1);
	public static final int TEST_PROVIDER_DIRECT_ALL_REDO_LEAK = seg("TestProviderDirectAllRedoLeak", 7410, 1);
	public static final int TEST_DYNAMIC_BEAN_COLLECT = seg("TestDynamicBeanCollect", 7420, 2);
	public static final int TEST_R3C_SAGA_BUSINESS_LOCK_SERIALIZATION = seg("TestR3cSagaBusinessLockSerialization", 7430, 2);
	public static final int TEST_FND731_LOGIN_TIMES_REDO_ONCE = seg("TestFnd731LoginTimesRedoOnce", 7440, 2);
	public static final int TEST_R3C_SAGA_END_BEFORE_SAGA_REGISTRATION = seg("TestR3cSagaEndBeforeSagaRegistration", 7450, 3);
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
	public static final int TEST_FND729_CROSS_FAMILY_CANCEL = seg("TestFnd729CrossFamilyCancel", 7590, 3);
	public static final int TEST_FND701_MEMORY_TABLE_SIZE = seg("TestFnd701MemoryTableSize", 7600, 1);
	public static final int TEST_CHECKPOINT_RUN_THREAD_SENTINEL = seg("TestCheckpointRunThreadSentinel", 7630, 1);
	public static final int TEST_TIMER_LOAD_MISSFIRE_ASYNC = seg("TestTimerLoadMissfireAsync", 7640, 1);
	public static final int TEST_FND14_COMP01_TIMER_LOAD_EXHAUSTED_CRON = seg("TestFnd14Comp01TimerLoadExhaustedCron", 7650, 1);
	public static final int TEST_FND754_STOP_COMMIT_GATE = seg("TestFnd754StopCommitGate", 7660, 1);
	public static final int TEST_FND755_FLUSH_WHEN_REDUCE_NULL_CHECKPOINT = seg("TestFnd755FlushWhenReduceNullCheckpoint", 7670, 1);
	public static final int TEST_FND756_STOP_STEP_ISOLATION = seg("TestFnd756StopStepIsolation", 7680, 1);
	public static final int TEST_FND14_COMP02_DB_WEB_TOKEN = seg("TestFnd14Comp02DbWebToken", 7690, 1);
	public static final int TEST_FND702_FINAL_ACTIONS_ISOLATION = seg("TestFnd702FinalActionsIsolation", 7700, 2);
	public static final int TEST_FND706_PLIST2_ATTACH_ORDER = seg("TestFnd706PList2AttachOrder", 7710, 1);
	public static final int TEST_FND879_LINKED_MAP_BROKEN_CHAIN_LOGGED = seg("TestFnd879LinkedMapBrokenChainLogged", 8790, 4);

	public static final int TEST_FND18_HOT01_INSTALL_START_LAST_FILTER = seg("TestFnd18Hot01InstallStartLastFilter", 12810, 1);
	public static final int TEST_FND18_HOT03_TRY_DISTRIBUTE_SUCCESS_DELETE_FAIL = seg("TestFnd18Hot03TryDistributeSuccessDeleteFail", 12811, 1);
	public static final int TEST_FND818_COMMIT_PATH_SALVAGE = seg("TestFnd818CommitPathSalvage", 12818, 1);
	public static final int TEST_FND821_CHECKPOINT_RUN_RACE = seg("TestFnd821CheckpointRunRace", 12821, 1);
	public static final int TEST_Z2F1_RENAME_TABLE_AFTER_COMPAT_CHECK = seg("TestZ2F1RenameTableAfterCompatCheck", 12822, 1);
	public static final int TEST_Z1F3_ADD_TABLE_ATOMICITY = seg("TestZ1F3AddTableAtomicity", 12823, 1);
	public static final int TEST_FND824_CLEAR_IN_USE_CLOSES_CREATED_DATABASES = seg("TestFnd824ClearInUseClosesCreatedDatabases", 12824, 1);
	public static final int TEST_FND826_CACHE_DIR_LOCK = seg("TestFnd826CacheDirLock", 12826, 1);
	public static final int TEST_FND828_APPLY_CURSOR_PERSISTENCE = seg("TestFnd828ApplyCursorPersistence", 12828, 1);
	public static final int TEST_FND828_APPLY_CURSOR_PERSISTENCE_MEMORY = seg("TestFnd828ApplyCursorPersistence#memory", 12829, 1);
	public static final int TEST_Z1F2_CREATE_DATABASE_CLOSES_ON_FAILURE = seg("TestZ1F2CreateDatabaseClosesOnFailure", 12850, 1);
	public static final int TEST_FND876_ONZ_ROLLBACK_AFTER_READY = seg("TestFnd876OnzRollbackAfterReady", 12876, 1);

	public static final int TEST_FND872_OFFLINE_TIMER_BOOKKEEPING = seg("TestFnd872OfflineTimerBookkeeping", 16181, 5);
	public static final int TEST_FND875_RC_PASSTHROUGH = seg("TestFnd875RcPassthrough", 16191, 6);
	public static final int TEST_FND877_TRANSMIT_UNKNOWN_ACTION = seg("TestFnd877TransmitUnknownAction", 16201, 3);
	public static final int TEST_FND890_LOAD_BASE_CHAIN_RESILIENCE = seg("TestFnd890LoadBaseChainResilience", 16211, 10);
	public static final int TEST_FND879_QUEUE_BROKEN_CHAIN_DIAGNOSIS = seg("TestFnd879QueueBrokenChainDiagnosis", 16221, 3);

	private FastServerIds() {
	}
}
