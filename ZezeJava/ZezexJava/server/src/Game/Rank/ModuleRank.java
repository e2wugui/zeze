package Game.Rank;

import Zeze.Transaction.*;
import Game.*;
import Zeze.Util.Action1;
import Zeze.Util.Str;
import Zeze.Arch.*;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

/**
 基本排行榜，实现了按long value从大到小进榜。
 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
*/
public class ModuleRank extends AbstractModule {

	private static final Logger logger = LogManager.getLogger(ModuleRank.class);

	@Override
	public void Initialize(Zeze.AppBase app) {
		super.putClassForMethod("RunGetRank", BRankList.class);
	}

	public final void Start(App app) {
	}

	public final void Stop(App app) {
	}

	/**
	 根据 value 设置到排行榜中

	 @param hash
	 @param roleId
	 @param value
	*/
	@RedirectHash
	protected void UpdateRank(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx) {
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
				tempVar.setAwardTaken(null == exist ? false : exist.isAwardTaken());
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
		return;
	}

	public static class Rank {
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

	private ConcurrentHashMap<BConcurrentKey, Rank> Ranks = new ConcurrentHashMap<BConcurrentKey, Rank>();
	public static final long RebuildTime = 5 * 60 * 1000; // 5 min

	private BRankList Merge(BRankList left, BRankList right) {
		BRankList result = new BRankList();
		int indexLeft = 0;
		int indexRight = 0;
		while (indexLeft < left.getRankList().size() && indexRight < right.getRankList().size()) {
			if (left.getRankList().get(indexLeft).getValue() >= right.getRankList().get(indexRight).getValue()) {
				result.getRankList().add(left.getRankList().get(indexLeft));
				++indexLeft;
			}
			else {
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
		}
		else if (indexRight < right.getRankList().size()) {
			while (indexRight < right.getRankList().size()) {
				result.getRankList().add(right.getRankList().get(indexRight));
				++indexRight;
			}
		}
		return result;
	}

	private Rank GetRank(BConcurrentKey keyHint) {
		var Rank = Ranks.computeIfAbsent(keyHint, (key) -> new Rank());
		synchronized (Rank) {
			long now = System.currentTimeMillis();
			if (now - Rank.BuildTime < RebuildTime) {
				return Rank;
			}
			// rebuild
			ArrayList<BRankList> datas = new ArrayList<BRankList>();
			int cocurrentLevel = GetConcurrentLevel(keyHint.getRankType());
			for (int i = 0; i < cocurrentLevel; ++i) {
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
					Rank.TableValue = datas.get(0).Copy();
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
							for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
								current.getRankList().remove(ir);
							//current.getRankList().RemoveRange(countNeed, current.getRankList().Count - countNeed);
						}
					}
					Rank.TableValue = current.Copy(); // current 可能还直接引用第一个，虽然逻辑上不大可能。先Copy。
					break;
			}
			Rank.BuildTime = now;
			if (Rank.TableValue.getRankList().size() > countNeed) { // 再次删除多余的结果。
				for (int ir = Rank.TableValue.getRankList().size() - 1; ir >= countNeed; --ir)
					Rank.TableValue.getRankList().remove(ir);
				//Rank.TableValue.getRankList().RemoveRange(countNeed, Rank.TableValue.RankList.Count - countNeed);
			}
		}
		return Rank;
	}

	/**
	 ModuleRedirectAll 实现要求：
	 1）第一个参数是调用会话id；
	 2）第二个参数是hash-index；
	 3）然后是实现自定义输入参数；
	 4）最后是结果回调,
		a) 第一参数是会话id，
		b) 第二参数hash-index，
		c) 第三个参数是returnCode，
		d) 剩下的是自定义参数。
	*/
	protected final long GetRank(long sessionId, int hash, BConcurrentKey keyHint,
								Zeze.Util.Action3<Long, Integer, BRankList> onHashResult) {
		// 根据hash获取分组rank。
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		var concurrentKey = new BConcurrentKey(keyHint.getRankType(), hash % concurrentLevel, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
		try {
			onHashResult.run(sessionId, hash, _trank.getOrAdd(concurrentKey));
			return Procedure.Success;
		} catch (Throwable e) {
			logger.error("", e);
			return Procedure.Exception;
		}
	}

	// 属性参数是获取总的并发分组数量的代码，直接复制到生成代码中。
	// 需要注意在子类上下文中可以编译通过。可以是常量。
	@RedirectAll(GetConcurrentLevelSource="GetConcurrentLevel(arg0.getRankType())")
	public void GetRank(BConcurrentKey keyHint, Zeze.Util.Action3<Long, Integer, BRankList> onHashResult, RedirectAllDoneHandle onHashEnd) {
		// 默认实现是本地遍历调用，这里不使用App.Zeze.Run启动任务（这样无法等待），直接调用实现。
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		var ctx = new ModuleRedirectAllContext(concurrentLevel,
				Str.format("{}:{}", getFullName(), "RunGetRank"));
		ctx.setOnHashEnd(onHashEnd);
		long sessionId = App.Server.AddManualContextWithTimeout(ctx, 10000); // 处理hash分组结果需要一个上下文保存收集的结果。
		for (int i = 0; i < concurrentLevel; ++i) {
			GetRank(sessionId, i, keyHint, onHashResult);
		}
	}

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
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		GetRank(keyHint,
				// Action OnHashResult
				(sessionId, hash, _result) -> {
					var ctx = App.Server.<ModuleRedirectAllContext>TryGetManualContext(sessionId);
					if (ctx == null)
						return;
					ctx.ProcessHash(hash, () -> new Rank(), (rank2)-> {
						var result = (BRankList)_result;
						if (rank2.TableValue == null) {
							rank2.TableValue = result.CopyIfManaged();
						}
						else {
							rank2.TableValue = Merge(rank2.TableValue, result);
						}
						if (rank2.TableValue.getRankList().size() > countNeed) {
							for (int ir = rank2.TableValue.getRankList().size() - 1; ir >= countNeed; --ir)
								rank2.TableValue.getRankList().remove(ir);
							//rank.TableValue.RankList.RemoveRange(countNeed, rank.TableValue.RankList.Count - countNeed);
						}
						return (long)Procedure.Success;
					});
				},
				// Action OnHashEnd
				(context) -> {
					if (context.getHashCodes().size() > 0) {
						// 一般是超时发生时还有未返回结果的hash分组。
						logger.warn("OnHashEnd: timeout with hashs: {}", context.getHashCodes());
					}

					var rank2 = (Rank)context.getUserState();
					rank.setBuildTime(System.currentTimeMillis());
					Ranks.put(keyHint, rank2); // 覆盖最新的数据到缓存里面。
					callback.run(rank2);
				});
	}

	/**
	 为排行榜设置最大并发级别。【有默认值】
	 【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】
	 决定了最大的并发度，改变的时候，旧数据全部失效，需要清除，重建。
	 一般选择一个足够大，但是又不能太大的数据。
	*/
	public final int GetConcurrentLevel(int rankType) {
		switch (rankType) {
			case BConcurrentKey.RankTypeGold: return 128;
			default: return 128; // default
		}
	}

	// 为排行榜设置需要的数量。【有默认值】
	public final int GetRankCount(int rankType) {
		switch (rankType) {
			case BConcurrentKey.RankTypeGold: return 100;
			default: return 100;
		}
	}

	// 排行榜中间数据的数量。【有默认值】
	public final int GetRankComputeCount(int rankType) {
		switch (rankType) {
			case BConcurrentKey.RankTypeGold: return 500;
			default: return GetRankCount(rankType) * 5;
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

	public final void AddCounterAndUpdateRank(long roleId, int delta, BConcurrentKey keyHint, Zeze.Net.Binary valueEx) {
		var counters = _trankcounters.getOrAdd(roleId);
		var counter = counters.getCounters().get(keyHint);
		if (null == counter) {
			counter = new BRankCounter();
			counters.getCounters().put(keyHint, counter);
		}
		counter.setValue(counter.getValue() + delta);

		if (null == valueEx) {
			valueEx = Zeze.Net.Binary.Empty;
		}

		UpdateRank(App.Zeze.Redirect.GetChoiceHashCode(), keyHint, roleId, counter.getValue(), valueEx);
	}

	@Override
	protected long ProcessCGetRankList(CGetRankList protocol) throws Throwable {
		var session = ProviderUserSession.Get(protocol);

		var result = new SGetRankList();
		if (session.getRoleId() == null) {
			result.setResultCode(-1);
			session.SendResponse(result);
			return Procedure.LogicError;
		}
		/*
		//异步方式获取rank
		GetRankAsync(protocol.Argument.RankType, (rank) =>
		{
		    result.Argument.RankList.AddRange(rank.TableValue.RankList);
		    session.SendResponse(result);
		});
		/*/
		// 同步方式获取rank
		var rankKey = NewRankKey(protocol.Argument.getRankType(), protocol.Argument.getTimeType());
		result.Argument.getRankList().addAll(GetRank(rankKey).getTableValue().getRankList());
		session.SendResponse(result);
		// */
		return Procedure.Success;
	}

	public final BConcurrentKey NewRankKey(int rankType, int timeType) {
		return NewRankKey(System.currentTimeMillis(), rankType, timeType, 0);
	}

	public final BConcurrentKey NewRankKey(int rankType, int timeType, long customizeId) {
		return NewRankKey(System.currentTimeMillis(), rankType, timeType, customizeId);
	}

	public final BConcurrentKey NewRankKey(long time, int rankType, int timeType) {
		return NewRankKey(time, rankType, timeType, 0);
	}

	public final BConcurrentKey NewRankKey(long time, int rankType, int timeType, long customizeId) {
		var c = java.util.Calendar.getInstance();
		c.setTimeInMillis(time);
		var year = c.get(java.util.Calendar.YEAR); // 后面根据TimeType可能覆盖这个值。
		long offset;

		switch (timeType) {
			case BConcurrentKey.TimeTypeTotal:
				year = 0;
				offset = 0;
				break;

			case BConcurrentKey.TimeTypeDay:
				offset = c.get(java.util.Calendar.DAY_OF_YEAR);
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
				throw new RuntimeException("Unsupport TimeType=" + timeType);
		}
		return new BConcurrentKey(rankType, 0, timeType, year, offset);
	}

	public int GetSimpleChineseSeason(java.util.Calendar c)
	{
		var month = c.get(Calendar.MONTH);
		if (month < 3) return 4; // 12,1,2
		if (month < 6) return 1; // 3,4,5
		if (month < 9) return 2; // 6,7,8
		if (month < 12) return 3; // 9,10,11
		return 4; // 12,1,2
	}

	/******************************** ModuleRedirect 测试 *****************************************/
	@RedirectToServer()
	protected TaskCompletionSource<Long> TestToServer(int serverId, int in, Zeze.Util.Action1<Integer> result) throws Throwable {
		result.run(in);
		return null;
	}

	@RedirectHash()
	public Zeze.Util.TaskCompletionSource<Long> Test1(int hash) {
		return null;
	}

	@RedirectHash()
	public void Test2(int hash, int inData) {
	}

	@RedirectHash()
	public void Test3(int hash, int inData, Zeze.Util.Action2<Integer, Zeze.Transaction.EmptyBean> result) throws Throwable {
		result.run(inData, new EmptyBean());
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleRank(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
