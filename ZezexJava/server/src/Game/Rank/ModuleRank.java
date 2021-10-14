package Game.Rank;

import Zeze.Transaction.*;
import static Zezex.Provider.ModuleProvider.*;
import Game.*;
import java.util.*;
import java.time.*;

// auto-generated


/** 
 基本排行榜，实现了按long value从大到小进榜。
 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
*/
public class ModuleRank extends AbstractModule {

	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public final void Start(App app) {
	}

	public final void Stop(App app) {
	}

	/** 
	 根据 value 设置到排行榜中
	 
	 @param hash
	 @param rankType
	 @param roleId
	 @param value
	 @return Procudure.Success...
	*/
	protected final int UpdateRank(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx) {
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		int maxCount = GetRankComputeCount(keyHint.getRankType());

		var concurrentKey = new BConcurrentKey(keyHint.getRankType(), hash % concurrentLevel, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());

		var rank = _trank.GetOrAdd(concurrentKey);
		// remove if role exist. 看看有没有更快的算法。
		BRankValue exist = null;
		for (int i = 0; i < rank.getRankList().Count; ++i) {
			var rankValue = rank.getRankList().get(i);
			if (rankValue.getRoleId() == roleId) {
				exist = rankValue;
				rank.getRankList().RemoveAt(i);
				break;
			}
		}
		// insert if in rank. 这里可以用 BinarySearch。
		for (int i = 0; i < rank.getRankList().Count; ++i) {
			if (rank.getRankList().get(i).Value < value) {
				BRankValue tempVar = new BRankValue();
				tempVar.setRoleId(roleId);
				tempVar.setValue(value);
				tempVar.setValueEx(valueEx);
				tempVar.setAwardTaken(null == exist ? false : exist.getAwardTaken());
				rank.getRankList().Insert(i, tempVar);
				if (rank.getRankList().Count > maxCount) {
					rank.getRankList().RemoveAt(rank.getRankList().Count - 1);
				}
				return Procedure.Success;
			}
		}
		// A: 如果排行的Value可能减少，那么当它原来存在，但现在处于队尾时，不要再进榜。
		// 因为此时可能存在未进榜但比它大的Value。
		// B: 但是在进榜玩家比榜单数量少的时候，如果不进榜，队尾的玩家更新还在队尾就会消失。
		if (rank.getRankList().Count < GetRankCount(keyHint.getRankType()) || (rank.getRankList().Count < maxCount && null == exist)) {
			BRankValue tempVar2 = new BRankValue();
			tempVar2.setRoleId(roleId);
			tempVar2.setValue(value);
			tempVar2.setValueEx(valueEx);
			rank.getRankList().Add(tempVar2);
		}
		return Procedure.Success;
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

	private java.util.concurrent.ConcurrentHashMap<BConcurrentKey, Rank> Ranks = new java.util.concurrent.ConcurrentHashMap<BConcurrentKey, Rank>();
	public static final long RebuildTime = 5 * 60 * 1000; // 5 min

	private BRankList Merge(BRankList left, BRankList right) {
		BRankList result = new BRankList();
		int indexLeft = 0;
		int indexRight = 0;
		while (indexLeft < left.getRankList().Count && indexRight < right.getRankList().Count) {
			if (left.getRankList().get(indexLeft).Value >= right.getRankList().get(indexRight).Value) {
				result.getRankList().Add(left.getRankList().get(indexLeft));
				++indexLeft;
			}
			else {
				result.getRankList().Add(right.getRankList().get(indexRight));
				++indexRight;
			}
		}
		// 下面两种情况不会同时存在，同时存在"在上面"处理。
		if (indexLeft < left.getRankList().Count) {
			while (indexLeft < left.getRankList().Count) {
				result.getRankList().Add(left.getRankList().get(indexLeft));
				++indexLeft;
			}
		}
		else if (indexRight < right.getRankList().Count) {
			while (indexRight < right.getRankList().Count) {
				result.getRankList().Add(right.getRankList().get(indexRight));
				++indexRight;
			}
		}
		return result;
	}

	private Rank GetRank(BConcurrentKey keyHint) {
		var Rank = Ranks.putIfAbsent(keyHint, (key) -> new Rank());
		synchronized (Rank) {
			long now = Zeze.Util.Time.NowUnixMillis;
			if (now - Rank.BuildTime < RebuildTime) {
				return Rank;
			}
			// rebuild
			ArrayList<BRankList> datas = new ArrayList<BRankList>();
			int cocurrentLevel = GetConcurrentLevel(keyHint.getRankType());
			for (int i = 0; i < cocurrentLevel; ++i) {
				var concurrentKey = new BConcurrentKey(keyHint.getRankType(), i, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
				var rank = _trank.GetOrAdd(concurrentKey);
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
						if (current.getRankList().Count > countNeed) {
							// 合并中间结果超过需要的数量可以先删除。
							// 第一个current直接引用table.data，不能删除。
							current.getRankList().RemoveRange(countNeed, current.getRankList().Count - countNeed);
						}
					}
					Rank.TableValue = current.Copy(); // current 可能还直接引用第一个，虽然逻辑上不大可能。先Copy。
					break;
			}
			Rank.BuildTime = now;
			if (Rank.TableValue.RankList.Count > countNeed) { // 再次删除多余的结果。
				Rank.TableValue.RankList.RemoveRange(countNeed, Rank.TableValue.RankList.Count - countNeed);
			}
		}
		return Rank;
	}

	/** 
	 相关数据变化时，更新排行榜。
	 最好在事务成功结束或者处理快完的时候或者ChangeListener中调用这个方法更新排行榜。
	 比如使用 Transaction.Current.RunWhileCommit(() => RunUpdateRank(...));
	 
	 @param rankType
	 @param roleId
	 @param value
	 @param valueEx 只保存，不参与比较。如果需要参与比较，需要另行实现自己的Update和Get。
	*/
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [ModuleRedirect()] public virtual void RunUpdateRank(BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
	public void RunUpdateRank(BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx) {
		int hash = ModuleRedirect.GetChoiceHashCode();
		getApp().getZeze().Run(() -> UpdateRank(hash, keyHint, roleId, value, valueEx), "RunUpdateRank", Zeze.TransactionModes.ExecuteInAnotherThread, hash);
	}

	// 名字必须和RunUpdateRankWithHash匹配，内部使用一样的实现。
	protected final int UpdateRankWithHash(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx) {
		return UpdateRank(hash, keyHint, roleId, value, valueEx);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [ModuleRedirectWithHash()] public virtual void RunUpdateRankWithHash(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
	public void RunUpdateRankWithHash(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx) {
		getApp().getZeze().Run(() -> UpdateRankWithHash(hash, keyHint, roleId, value, valueEx), "RunUpdateRankWithHash", Zeze.TransactionModes.ExecuteInAnotherThread, hash);
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
	protected final int GetRank(long sessionId, int hash, BConcurrentKey keyHint, tangible.Action4Param<Long, Integer, Integer, BRankList> onHashResult) {
		// 根据hash获取分组rank。
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		var concurrentKey = new BConcurrentKey(keyHint.getRankType(), hash % concurrentLevel, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
		onHashResult.invoke(sessionId, hash, Procedure.Success, _trank.GetOrAdd(concurrentKey));
		return Procedure.Success;
	}

	// 属性参数是获取总的并发分组数量的代码，直接复制到生成代码中。
	// 需要注意在子类上下文中可以编译通过。可以是常量。
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [ModuleRedirectAll("GetConcurrentLevel(keyHint.RankType)")] public virtual void RunGetRank(BConcurrentKey keyHint, System.Action<long, int, int, BRankList> onHashResult, Action<ModuleRedirectAllContext> onHashEnd)
	public void RunGetRank(BConcurrentKey keyHint, tangible.Action4Param<Long, Integer, Integer, BRankList> onHashResult, tangible.Action1Param<ModuleProvider.ModuleRedirectAllContext> onHashEnd) {
		// 默认实现是本地遍历调用，这里不使用App.Zeze.Run启动任务（这样无法等待），直接调用实现。
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		var ctx = new ModuleProvider.ModuleRedirectAllContext(concurrentLevel, String.format("%1$s:%2$s", getFullName(), "RunGetRank"));
		ctx.setOnHashEnd(onHashEnd);
		long sessionId = getApp().getServer().AddManualContextWithTimeout(ctx, 10000); // 处理hash分组结果需要一个上下文保存收集的结果。
		for (int i = 0; i < concurrentLevel; ++i) {
			GetRank(sessionId, i, keyHint, onHashResult);
		}
	}

	// 使用异步方案构建rank。
	private void GetRankAsync(BConcurrentKey keyHint, tangible.Action1Param<Rank> callback) {
		TValue rank;
		tangible.OutObject<TValue> tempOut_rank = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (Ranks.TryGetValue(keyHint, tempOut_rank)) {
		rank = tempOut_rank.outArgValue;
			long now = Zeze.Util.Time.NowUnixMillis;
			if (now - rank.BuildTime < RebuildTime) {
				callback.invoke(rank);
				return;
			}
		}
	else {
		rank = tempOut_rank.outArgValue;
	}
		// 异步方式没法锁住Rank，所以并发的情况下，可能多次去获取数据，多次构建，多次覆盖Ranks的cache。
		int countNeed = GetRankCount(keyHint.getRankType());
		int concurrentLevel = GetConcurrentLevel(keyHint.getRankType());
		RunGetRank(keyHint, (sessionId, hash, returnCode, BRankList) -> {
					if (getApp().getServer().<ModuleProvider.ModuleRedirectAllContext>TryGetManualContext(sessionId) != null) {
						getApp().getServer().<ModuleProvider.ModuleRedirectAllContext>TryGetManualContext(sessionId).ProcessHash;
					} {
						if (returnCode != Procedure.Success) {
							return returnCode;
						}
						if (rank.TableValue == null) {
							rank.TableValue = BRankList.CopyIfManaged();
						}
						else {
							rank.TableValue = Merge(rank.TableValue, BRankList);
						}
						if (rank.TableValue.RankList.Count > countNeed) {
							rank.TableValue.RankList.RemoveRange(countNeed, rank.TableValue.RankList.Count - countNeed);
						}
						return Procedure.Success;
					}
					);
		}, (context) -> {
					if (context.HashCodes.Count > 0) {
						// 一般是超时发生时还有未返回结果的hash分组。
						logger.Warn(String.format("OnHashEnd: timeout with hashs: %1$s", context.HashCodes));
					}

					var rank = context.UserState instanceof Rank ? (Rank)context.UserState : null;
					rank.setBuildTime(Zeze.Util.Time.NowUnixMillis);
					Ranks.put(keyHint, rank); // 覆盖最新的数据到缓存里面。
					callback.invoke(rank);
				});
	}

	/** 
	 为排行榜设置最大并发级别。【有默认值】
	 【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】
	 决定了最大的并发度，改变的时候，旧数据全部失效，需要清除，重建。
	 一般选择一个足够大，但是又不能太大的数据。
	*/
	public final int GetConcurrentLevel(int rankType) {
		return switch (rankType) {
			case BConcurrentKey.RankTypeGold -> 128;
			default -> 128; // default
		};
	}

	// 为排行榜设置需要的数量。【有默认值】
	public final int GetRankCount(int rankType) {
		return switch (rankType) {
			case BConcurrentKey.RankTypeGold -> 100;
			default -> 100;
		};
	}

	// 排行榜中间数据的数量。【有默认值】
	public final int GetRankComputeCount(int rankType) {
		return switch (rankType) {
			case BConcurrentKey.RankTypeGold -> 500;
			default -> GetRankCount(rankType) * 5;
		};
	}

	public final long GetCounter(long roleId, BConcurrentKey keyHint) {
		var counters = _trankcounters.GetOrAdd(roleId);
		V counter;
		tangible.OutObject<V> tempOut_counter = new tangible.OutObject<V>();
		if (false == counters.getCounters().TryGetValue(keyHint, tempOut_counter)) {
		counter = tempOut_counter.outArgValue;
			return 0;
		}
	else {
		counter = tempOut_counter.outArgValue;
	}

		return counter.Value;
	}


	public final void AddCounterAndUpdateRank(long roleId, int delta, BConcurrentKey keyHint) {
		AddCounterAndUpdateRank(roleId, delta, keyHint, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void AddCounterAndUpdateRank(long roleId, int delta, BConcurrentKey keyHint, Zeze.Net.Binary valueEx = null)
	public final void AddCounterAndUpdateRank(long roleId, int delta, BConcurrentKey keyHint, Zeze.Net.Binary valueEx) {
		var counters = _trankcounters.GetOrAdd(roleId);
		V counter;
		tangible.OutObject<V> tempOut_counter = new tangible.OutObject<V>();
		if (false == counters.getCounters().TryGetValue(keyHint, tempOut_counter)) {
		counter = tempOut_counter.outArgValue;
			counter = new BRankCounter();
			counters.getCounters().Add(keyHint, counter);
		}
	else {
		counter = tempOut_counter.outArgValue;
	}
		counter.Value += delta;

		if (null == valueEx) {
			valueEx = Zeze.Net.Binary.Empty;
		}

		RunUpdateRank(keyHint, roleId, counter.Value, valueEx);
	}

	@Override
	public int ProcessCGetRankList(CGetRankList protocol) {
		Login.Session session = Login.Session.Get(protocol);

		var result = new SGetRankList();
		if (session.getRoleId().equals(null)) {
			result.ResultCode = -1;
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
		result.getArgument().getRankList().AddRange(GetRank(NewRankKey(protocol.getArgument().getRankType(), protocol.getArgument().getTimeType())).getTableValue().getRankList());
		session.SendResponse(result);
		// */
		return Procedure.Success;
	}


	public final BConcurrentKey NewRankKey(int rankType, int timeType) {
		return NewRankKey(rankType, timeType, 0);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public BConcurrentKey NewRankKey(int rankType, int timeType, long customizeId = 0)
	public final BConcurrentKey NewRankKey(int rankType, int timeType, long customizeId) {
		return NewRankKey(LocalDateTime.now(), rankType, timeType, customizeId);
	}


	public final BConcurrentKey NewRankKey(java.time.LocalDateTime time, int rankType, int timeType) {
		return NewRankKey(time, rankType, timeType, 0);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public BConcurrentKey NewRankKey(DateTime time, int rankType, int timeType, long customizeId = 0)
	public final BConcurrentKey NewRankKey(LocalDateTime time, int rankType, int timeType, long customizeId) {
		var year = time.getYear(); // 后面根据TimeType可能覆盖这个值。
		long offset;

		switch (timeType) {
			case BConcurrentKey.TimeTypeTotal:
				year = 0;
				offset = 0;
				break;

			case BConcurrentKey.TimeTypeDay:
				offset = time.getDayOfYear();
				break;

			case BConcurrentKey.TimeTypeWeek:
				offset = Zeze.Util.Time.GetWeekOfYear(time);
				break;

			case BConcurrentKey.TimeTypeSeason:
				offset = Zeze.Util.Time.GetSimpleChineseSeason(time);
				break;

			case BConcurrentKey.TimeTypeYear:
				offset = 0;
				break;

			case BConcurrentKey.TimeTypeCustomize:
				year = 0;
				offset = customizeId;
				break;

			default:
				throw new RuntimeException(String.format("Unsupport TimeType=%1$s", timeType));
		}
		return new BConcurrentKey(rankType, 0, timeType, year, offset);
	}

	/******************************** ModuleRedirect 测试 *****************************************/
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [ModuleRedirect()] public virtual TaskCompletionSource<int> RunTest1(Zeze.TransactionModes mode)
	public TaskCompletionSource<Integer> RunTest1(Zeze.TransactionModes mode) {
		int hash = ModuleRedirect.GetChoiceHashCode();
		return getApp().getZeze().Run(() -> Test1(hash), "Test1", mode, hash);
	}

	protected final int Test1(int hash) {
		return Procedure.Success;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [ModuleRedirect()] public virtual void RunTest2(int inData, ref int refData, out int outData)
	public void RunTest2(int inData, tangible.RefObject<Integer> refData, tangible.OutObject<Integer> outData) {
		int hash = ModuleRedirect.GetChoiceHashCode();
		int outDataTmp = 0;
		int refDataTmp = refData.refArgValue;
		tangible.RefObject<Integer> tempRef_refDataTmp = new tangible.RefObject<Integer>(refDataTmp);
		tangible.OutObject<Integer> tempOut_outDataTmp = new tangible.OutObject<Integer>();
		var future = getApp().getZeze().Run(() -> Test2(hash, inData, tempRef_refDataTmp, tempOut_outDataTmp), "Test2", Zeze.TransactionModes.ExecuteInAnotherThread, hash);
	outDataTmp = tempOut_outDataTmp.outArgValue;
	refDataTmp = tempRef_refDataTmp.refArgValue;
		future.Task.Wait();
		refData.refArgValue = refDataTmp;
		outData.outArgValue = outDataTmp;
	}

	protected final int Test2(int hash, int inData, tangible.RefObject<Integer> refData, tangible.OutObject<Integer> outData) {
		outData.outArgValue = 1;
		++refData.refArgValue;
		return Procedure.Success;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [ModuleRedirect()] public virtual void RunTest3(int inData, ref int refData, out int outData, System.Action<int> resultCallback)
	public void RunTest3(int inData, tangible.RefObject<Integer> refData, tangible.OutObject<Integer> outData, tangible.Action1Param<Integer> resultCallback) {
		int hash = ModuleRedirect.GetChoiceHashCode();
		int outDataTmp = 0;
		int refDataTmp = refData.refArgValue;
		tangible.RefObject<Integer> tempRef_refDataTmp = new tangible.RefObject<Integer>(refDataTmp);
		tangible.OutObject<Integer> tempOut_outDataTmp = new tangible.OutObject<Integer>();
		var future = getApp().getZeze().Run(() -> Test3(hash, inData, tempRef_refDataTmp, tempOut_outDataTmp, resultCallback), "Test3", Zeze.TransactionModes.ExecuteInAnotherThread, hash);
	outDataTmp = tempOut_outDataTmp.outArgValue;
	refDataTmp = tempRef_refDataTmp.refArgValue;
		future.Task.Wait();
		refData.refArgValue = refDataTmp;
		outData.outArgValue = outDataTmp;
	}

	/*
	 * 一般来说 ref|out 和 Action 回调方式不该一起用。存粹为了测试。
	 * 如果混用，首先 Action 回调先发生，然后 ref|out 的变量才会被赋值。这个当然吧。
	 * 
	 * 可以包含多个 Action。纯粹为了...可以用 Empty 表示 null，嗯，总算找到理由了。
	 * 如果包含多个，按照真正的实现的回调顺序回调，不是定义顺序。这个也当然吧。
	 * 
	 * ref|out 方式需要同步等待，【不建议使用这种方式】【不建议使用这种方式】【不建议使用这种方式】
	 */
	protected final int Test3(int hash, int inData, tangible.RefObject<Integer> refData, tangible.OutObject<Integer> outData, tangible.Action1Param<Integer> resultCallback) {
		outData.outArgValue = 1;
		++refData.refArgValue;
		resultCallback.invoke(1);
		return Procedure.Success;
	}


	public static final int ModuleId = 9;

	private trank _trank = new trank();
	private trankcounters _trankcounters = new trankcounters();

	private App App;
	public final App getApp() {
		return App;
	}

	public ModuleRank(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getServer().AddFactoryHandle(612619, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Rank.CGetRankList(), Handle = Zeze.Net.Service.<CGetRankList>MakeHandle(this, this.getClass().getMethod("ProcessCGetRankList"))});
		// register table
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_trank.Name).DatabaseName, _trank);
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_trankcounters.Name).DatabaseName, _trankcounters);
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(612619, tempOut__);
	_ = tempOut__.outArgValue;
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_trank.Name).DatabaseName, _trank);
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_trankcounters.Name).DatabaseName, _trankcounters);
	}

}
