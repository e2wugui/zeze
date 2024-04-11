package Game.Rank;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Game.App;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectKey;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Hot.HotService;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 基本排行榜，实现了按long value从大到小进榜。
 * 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
 * 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
 */
@SuppressWarnings({"MethodMayBeStatic", "RedundantSuppression"})
public class ModuleRank extends AbstractModule implements IModuleRank {
	private static final Logger logger = LogManager.getLogger(ModuleRank.class);
	public static final long RebuildTime = 5 * 60 * 1000; // 5 min

	public final void Start(App app) {
	}

	public final void Stop(App app) {
	}

	/**
	 * 根据 value 设置到排行榜中
	 */
	@RedirectHash(ConcurrentLevelSource = "GetConcurrentLevel(keyHint.getRankType())")
	protected void UpdateRank(int hash, BConcurrentKey keyHint, long roleId, long value, Binary valueEx) {
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		int maxCount = GetRankComputeCount(keyHint.getRankType());

		var concurrentKey = new BConcurrentKey(keyHint.getRankType(),
				hash % concurrentLevel,
				keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());

		var rank = _trank.getOrAdd(concurrentKey);
		// remove if role exist. 看看有没有更快的算法。
		BRankValue exist = null;
		for (int i = 0; i < rank.getRankList().size(); ++i) {
			var rankValue = rank.getRankList().get(i);
			if (rankValue.getRoleId() == roleId) {
				exist = rankValue;
				rank.getRankList().remove(i);
				break;
			}
		}
		// insert if in rank. 这里可以用 BinarySearch。
		for (int i = 0; i < rank.getRankList().size(); ++i) {
			if (rank.getRankList().get(i).getValue() < value) {
				BRankValue tempVar = new BRankValue();
				tempVar.setRoleId(roleId);
				tempVar.setValue(value);
				tempVar.setValueEx(valueEx);
				tempVar.setAwardTaken(exist != null && exist.isAwardTaken());
				rank.getRankList().add(i, tempVar);
				if (rank.getRankList().size() > maxCount) {
					rank.getRankList().remove(rank.getRankList().size() - 1);
				}
				return;
			}
		}
		// A: 如果排行的Value可能减少，那么当它原来存在，但现在处于队尾时，不要再进榜。
		// 因为此时可能存在未进榜但比它大的Value。
		// B: 但是在进榜玩家比榜单数量少的时候，如果不进榜，队尾的玩家更新还在队尾就会消失。
		if (rank.getRankList().size() < GetRankCount(keyHint.getRankType())
				|| (rank.getRankList().size() < maxCount && null == exist)) {
			BRankValue tempVar2 = new BRankValue();
			tempVar2.setRoleId(roleId);
			tempVar2.setValue(value);
			tempVar2.setValueEx(valueEx);
			rank.getRankList().add(tempVar2);
		}
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	public static class Rank extends ReentrantLock {
		private long BuildTime;

		public final long getBuildTime() {
			return BuildTime;
		}

		public final void setBuildTime(long value) {
			BuildTime = value;
		}

		private BRankList TableValue;

		public final BRankList getTableValue() {
			return TableValue;
		}

		public final void setTableValue(BRankList value) {
			TableValue = value;
		}
	}

	private final ConcurrentHashMap<BConcurrentKey, Rank> Ranks = new ConcurrentHashMap<>();

	private static BRankList Merge(BRankList left, BRankList right) {
		BRankList result = new BRankList();
		int indexLeft = 0;
		int indexRight = 0;
		while (indexLeft < left.getRankList().size() && indexRight < right.getRankList().size()) {
			if (left.getRankList().get(indexLeft).getValue() >= right.getRankList().get(indexRight).getValue()) {
				result.getRankList().add(left.getRankList().get(indexLeft));
				++indexLeft;
			} else {
				result.getRankList().add(right.getRankList().get(indexRight));
				++indexRight;
			}
		}
		// 下面两种情况不会同时存在，同时存在"在上面"处理。
		if (indexLeft < left.getRankList().size()) {
			while (indexLeft < left.getRankList().size()) {
				result.getRankList().add(left.getRankList().get(indexLeft));
				++indexLeft;
			}
		} else if (indexRight < right.getRankList().size()) {
			while (indexRight < right.getRankList().size()) {
				result.getRankList().add(right.getRankList().get(indexRight));
				++indexRight;
			}
		}
		return result;
	}

	private Rank GetRankSync(BConcurrentKey keyHint) {
		var Rank = Ranks.computeIfAbsent(keyHint, __ -> new Rank());
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		Rank.lock();
		try {
			long now = System.currentTimeMillis();
			if (now - Rank.BuildTime < RebuildTime) {
				return Rank;
			}
			// rebuild
			ArrayList<BRankList> datas = new ArrayList<>();
			int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
			for (int i = 0; i < concurrentLevel; ++i) {
				var concurrentKey = new BConcurrentKey(keyHint.getRankType(), i, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
				var rank = _trank.getOrAdd(concurrentKey);
				datas.add(rank);
			}
			int countNeed = GetRankCount(keyHint.getRankType());
			switch (datas.size()) {
			case 0:
				Rank.TableValue = new BRankList();
				break;

			case 1:
				Rank.TableValue = datas.get(0).copy();
				break;

			default:
				// 合并过程中，结果是新的 BRankList，List中的 BRankValue 引用到表中。
				// 最后 Copy 一次。
				BRankList current = datas.get(0);
				for (int i = 1; i < datas.size(); ++i) {
					current = Merge(current, datas.get(i));
					if (current.getRankList().size() > countNeed) {
						// 合并中间结果超过需要的数量可以先删除。
						// 第一个current直接引用table.data，不能删除。
						//noinspection ListRemoveInLoop
						for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
							current.getRankList().remove(ir);
						//current.getRankList().RemoveRange(countNeed, current.getRankList().Count - countNeed);
					}
				}
				Rank.TableValue = current.copy(); // current 可能还直接引用第一个，虽然逻辑上不大可能。先Copy。
				break;
			}
			Rank.BuildTime = now;
			if (Rank.TableValue.getRankList().size() > countNeed) { // 再次删除多余的结果。
				//noinspection ListRemoveInLoop
				for (int ir = Rank.TableValue.getRankList().size() - 1; ir >= countNeed; --ir)
					Rank.TableValue.getRankList().remove(ir);
				//Rank.TableValue.getRankList().RemoveRange(countNeed, Rank.TableValue.RankList.Count - countNeed);
			}
		} finally {
			Rank.unlock();
		}
		return Rank;
	}

	public RedirectAllFuture<RRankList> GetRank(BConcurrentKey keyHint) {
		return GetRank(GetConcurrentLevel(keyHint.getRankType()), keyHint);
	}

	// 属性参数是获取总的并发分组数量的代码，直接复制到生成代码中。
	// 最好改成protected并新增一个隐藏hash参数的public方法调用这里
	@RedirectAll
	protected RedirectAllFuture<RRankList> GetRank(int hash, BConcurrentKey keyHint) {
		// 根据hash获取分组rank。
		var result = new RRankList();
		try {
			int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
			var concurrentKey = new BConcurrentKey(keyHint.getRankType(), hash % concurrentLevel,
					keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
			result.rankList = _trank.getOrAdd(concurrentKey);
		} catch (Throwable e) {
			// rpc response.
			logger.error("", e);
		}
		return RedirectAllFuture.result(result);
/*
		// 默认实现是本地遍历调用，这里不使用App.Zeze.Run启动任务（这样无法等待），直接调用实现。
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		var ctx = new RedirectAllContext(concurrentLevel,
				Str.format("{}:{}", getFullName(), "RunGetRank"));
		ctx.setOnHashEnd(onHashEnd);
		long sessionId = App.Server.addManualContextWithTimeout(ctx, 10000); // 处理hash分组结果需要一个上下文保存收集的结果。
		for (int i = 0; i < concurrentLevel; ++i) {
			GetRank(sessionId, i, keyHint, onHashResult);
		}
*/
	}

/*
	// 使用异步方案构建rank。
	public void GetRankAsync(BConcurrentKey keyHint,
							 Action1<Rank> callback) {
		var rank = Ranks.get(keyHint);
		if (null != rank) {
			long now = System.currentTimeMillis();
			if (now - rank.BuildTime < RebuildTime) {
				try {
					callback.run(rank);
				} catch (Throwable e) {
					logger.error("", e);
				}
				return;
			}
		}

		// 异步方式没法锁住Rank，所以并发的情况下，可能多次去获取数据，多次构建，多次覆盖Ranks的cache。
		int countNeed = GetRankCount(keyHint.getRankType());
		// int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		GetRank(keyHint,
				// Action OnHashResult
				(sessionId, hash, result) -> {
					var ctx = App.Server.<RedirectAllContext>TryGetManualContext(sessionId);
					if (ctx == null)
						return;
					ctx.ProcessHash(hash, Rank::new, (rank2) -> {
						if (rank2.TableValue == null) {
							rank2.TableValue = result.CopyIfManaged();
						} else {
							rank2.TableValue = Merge(rank2.TableValue, result);
						}
						if (rank2.TableValue.getRankList().size() > countNeed) {
							//noinspection ListRemoveInLoop
							for (int ir = rank2.TableValue.getRankList().size() - 1; ir >= countNeed; --ir)
								rank2.TableValue.getRankList().remove(ir);
							//rank.TableValue.RankList.RemoveRange(countNeed, rank.TableValue.RankList.Count - countNeed);
						}
						return Procedure.Success;
					});
				},
				// Action OnHashEnd
				(context) -> {
					if (!context.getHashResults().isEmpty()) {
						// 一般是超时发生时还有未返回结果的hash分组。
						logger.warn("OnHashEnd: timeout with hashes: {}", context.getHashResults());
					}

					var rank2 = (Rank)context.getUserState();
					assert rank != null;
					rank.setBuildTime(System.currentTimeMillis());
					Ranks.put(keyHint, rank2); // 覆盖最新的数据到缓存里面。
					callback.run(rank2);
				});
	}
*/

	/**
	 * 为排行榜设置最大并发级别。【有默认值】
	 * 【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】
	 * 决定了最大的并发度，改变的时候，旧数据全部失效，需要清除，重建。
	 * 一般选择一个足够大，但是又不能太大的数据。
	 */
	public static int GetConcurrentLevel(int rankType) {
		//noinspection SwitchStatementWithTooFewBranches
		switch (rankType) {
		case BConcurrentKey.RankTypeGold:
		default:
			return 128; // default
		}
	}

	// 为排行榜设置需要的数量。【有默认值】
	public static int GetRankCount(int rankType) {
		//noinspection SwitchStatementWithTooFewBranches
		switch (rankType) {
		case BConcurrentKey.RankTypeGold:
		default:
			return 100;
		}
	}

	// 排行榜中间数据的数量。【有默认值】
	public static int GetRankComputeCount(int rankType) {
		//noinspection SwitchStatementWithTooFewBranches
		switch (rankType) {
		case BConcurrentKey.RankTypeGold:
			return 500;
		default:
			return GetRankCount(rankType) * 5;
		}
	}

	public final long GetCounter(long roleId, BConcurrentKey keyHint) {
		var counters = _trankcounters.getOrAdd(roleId);
		var counter = counters.getCounters().get(keyHint);
		if (null == counter)
			return 0;
		return counter.getValue();
	}

	public final void AddCounterAndUpdateRank(long roleId, int delta, BConcurrentKey keyHint) {
		AddCounterAndUpdateRank(roleId, delta, keyHint, null);
	}

	public final void AddCounterAndUpdateRank(long roleId, int delta, BConcurrentKey keyHint, Binary valueEx) {
		var counters = _trankcounters.getOrAdd(roleId);
		var counter = counters.getCounters().get(keyHint);
		if (null == counter) {
			counter = new BRankCounter();
			counters.getCounters().put(keyHint, counter);
		}
		counter.setValue(counter.getValue() + delta);

		if (null == valueEx) {
			valueEx = Binary.Empty;
		}

		UpdateRank(GetChoiceHashCode(), keyHint, roleId, counter.getValue(), valueEx);
	}

	public static int GetChoiceHashCode() {
		//noinspection ConstantConditions
		String account = ((ProviderUserSession)Transaction.userState()).getAccount();
		return Zeze.Serialize.ByteBuffer.calc_hashnr(account);
	}

	@Override
	protected long ProcessCGetRankList(CGetRankList protocol) {
		var session = ProviderUserSession.get(protocol);

		var result = new SGetRankList();
		if (session.getRoleId() == null) {
			result.setResultCode(-1);
			session.sendResponseWhileCommit(result);
			return Procedure.LogicError;
		}
		/*
		//异步方式获取rank
		GetRankAsync(protocol.Argument.RankType, (rank) =>
		{
		    result.Argument.RankList.AddRange(rank.TableValue.RankList);
		    session.sendResponseWhileCommit(result);
		});
		/*/
		// 同步方式获取rank
		var rankKey = NewRankKey(protocol.Argument.getRankType(), protocol.Argument.getTimeType());
		result.Argument.getRankList().addAll(GetRankSync(rankKey).getTableValue().getRankList());
		session.sendResponseWhileCommit(result);
		// */
		return Procedure.Success;
	}

	public static BConcurrentKey NewRankKey(int rankType, int timeType) {
		return NewRankKey(System.currentTimeMillis(), rankType, timeType, 0);
	}

	public static BConcurrentKey NewRankKey(int rankType, int timeType, long customizeId) {
		return NewRankKey(System.currentTimeMillis(), rankType, timeType, customizeId);
	}

	public static BConcurrentKey NewRankKey(long time, int rankType, int timeType) {
		return NewRankKey(time, rankType, timeType, 0);
	}

	public static BConcurrentKey NewRankKey(long time, int rankType, int timeType, long customizeId) {
		var c = Calendar.getInstance();
		c.setTimeInMillis(time);
		var year = c.get(Calendar.YEAR); // 后面根据TimeType可能覆盖这个值。
		long offset;

		switch (timeType) {
		case BConcurrentKey.TimeTypeTotal:
			year = 0;
			offset = 0;
			break;

		case BConcurrentKey.TimeTypeDay:
			offset = c.get(Calendar.DAY_OF_YEAR);
			break;

		case BConcurrentKey.TimeTypeWeek:
			offset = c.get(Calendar.WEEK_OF_YEAR);
			break;

		case BConcurrentKey.TimeTypeSeason:
			offset = GetSimpleChineseSeason(c);
			break;

		case BConcurrentKey.TimeTypeYear:
			offset = 0;
			break;

		case BConcurrentKey.TimeTypeCustomize:
			year = 0;
			offset = customizeId;
			break;

		default:
			throw new RuntimeException("Unsupported TimeType=" + timeType);
		}
		return new BConcurrentKey(rankType, 0, timeType, year, offset);
	}

	public static int GetSimpleChineseSeason(Calendar c) {
		//@formatter:off
		var month = c.get(Calendar.MONTH);
		if (month < 3) return 4; // 12,1,2
		if (month < 6) return 1; // 3,4,5
		if (month < 9) return 2; // 6,7,8
		if (month < 12) return 3; // 9,10,11
			return 4; // 12,1,2
		//@formatter:on
	}

	/******************************** ModuleRedirect 测试 *****************************************/
	// 单发给某个serverId执行(找不到或没连接会抛异常),可以是本服. 返回类型可以是void或RedirectFuture<自定义结果类型或Long(resultCode)>
	@Override
	@RedirectToServer(version = 1)
	public RedirectFuture<TestToServerResult> TestToServer(int serverId, int in) { // 首个参数serverId是固定必要的特殊参数,后面是自定义输入参数
		var result = new TestToServerResult();
		result.setOut(in);
		result.setServerId(App.Zeze.getConfig().getServerId());
		return RedirectFuture.finish(result); // 同步完成则先finish再返回,异步则可在返回后在其它位置调用finish完成
	}

	// 第一个参数hash是固定的特殊参数
	@Override
	@RedirectHash(version = 2) // 单发给某个hash值指定的server执行,可能是本服,找不到hash节点也会在本服执行. 返回类型同ToServer
	public RedirectFuture<TestHashResult> TestHash(int hash, int in) { // 首个参数hash是固定必要的特殊参数,后面是自定义输入参数
		var f = new RedirectFuture<TestHashResult>();
		Task.run(App.Zeze.newProcedure(() -> {
			TestHashResult result = new TestHashResult();
			result.setHash(hash);
			result.setOut(in);
			result.setServerId(App.Zeze.getConfig().getServerId());
			f.setResult(result); // 异步完成
			return Procedure.Success;
		}, "TestHashAsync"), null, null, DispatchMode.Normal);
		return f;
	}

	@Override
	@RedirectAll(version = 3) // 广播请求并获取所有回复结果
	public RedirectAllFuture<TestToAllResult> TestToAll(int hash, int in) throws Exception { // 首个参数hash在发起方是hash总数,处理方是当前hash,后面是自定义参数列表
		System.out.println("TestToAll hash=" + hash + ", in=" + in);
		switch (hash) {
		case 0: // local sync
		case 1: // remote sync
			var result = new TestToAllResult();
			result.out = in;
			return RedirectAllFuture.result(result);
		case 2: // local exception
		case 3: // remote exception
			throw new Exception("not bug, only for test");
		case 4: // local async
		case 5: // remote async
			var future = RedirectAllFuture.<TestToAllResult>async(); // 启用异步方式,之后在future.asyncResult()时回复结果
			Task.run(App.Zeze.newProcedure(() -> {
				var result1 = new TestToAllResult();
				result1.out = in;
				future.asyncResult(result1);
				return Procedure.Success;
			}, "TestToAllAsync"), null, null, DispatchMode.Normal);
			return future;
		}
		throw new UnsupportedOperationException();
	}

	@RedirectToServer // 返回结果可以是Long类型,表示只有resultCode值
	public RedirectFuture<Long> TestToServerLongResult(int serverId) { // 可以没有自定义输入参数,但必须至少有serverId参数
		return RedirectFuture.finish(Procedure.Success);
	}

	@RedirectHash
	public RedirectFuture<Long> TestHashLongResult(int hash) { // 可以没有自定义输入参数,但必须至少有hash参数
		return RedirectFuture.finish(Procedure.Success);
	}

	@RedirectToServer
	public RedirectFuture<String> TestToServerStringResult(int serverId) {
		return RedirectFuture.finish("ok");
	}

	@RedirectToServer
	public RedirectFuture<Binary> TestToServerBinaryResult(int serverId) {
		return RedirectFuture.finish(new Binary("ok"));
	}

	@RedirectToServer(timeout = 1000) // 返回结果可以是Bean类型,其中如果有setResultCode(long)方法则会自动设置成resultCode
	public RedirectFuture<BRankList> TestToServerBeanResult(int serverId) { // 可以没有自定义输入参数,但必须至少有serverId参数
		return RedirectFuture.finish(new BRankList());
	}

	public static class GenericResult<T extends Serializable> {
		public long resultCode;
		public T ser;
		public java.io.Serializable obj;
	}

	@RedirectHash(timeout = 2000)
	public RedirectFuture<GenericResult<BRankList>> TestHashGenericResult(int serverId, @RedirectKey Long arg) {
		return RedirectFuture.finish(new GenericResult<>());
	}

	@RedirectToServer
	public void TestToServerNoResult(int serverId, @RedirectKey List<Long> longList) {
	}

	@RedirectHash
	public void TestHashNoResult(@RedirectKey int hash, BRankList rankList) {
	}

	@RedirectAll(timeout = 3000)
	public void TestAllNoResult(int hash, java.io.Serializable obj) {
	}

	@RedirectAll
	public RedirectAllFuture<RedirectResult> TestSimpleResult(int hash) {
		return RedirectAllFuture.result(new RedirectResult());
	}

	public static final class InnerBean implements Serializable {
		public int a;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(a);
		}

		@Override
		public void decode(IByteBuffer bb) {
			a = bb.ReadInt();
		}
	}

	@RedirectToServer
	public void TestInnerClass(int serverId, InnerBean inner, @RedirectKey long key) {
	}

	@RedirectToServer(oneByOne = false)
	public void TestBeanList(int serverId, List<InnerBean> inner, List<Long> longList, Map<Long, InnerBean> map) {
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleRank(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
