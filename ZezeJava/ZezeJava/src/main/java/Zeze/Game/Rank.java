package Zeze.Game;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderDistribute;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectResult;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BRankList;
import Zeze.Builtin.Game.Rank.BRankValue;
import Zeze.Net.Binary;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.OutObject;

public class Rank extends AbstractRank {
	private final AppBase app;

	public volatile IntUnaryOperator funcRankSize;
	public volatile IntUnaryOperator funcConcurrentLevel;
	public volatile LongUnaryOperator funcRankCacheTimeout;

	@SuppressWarnings("CanBeFinal")
	public volatile float computeFactor = 2.5f;

	public static Rank create(AppBase app) {
		return GenModule.createRedirectModule(Rank.class, app);
	}

	protected Rank(AppBase app) {
		if (app == null)
			throw new NullPointerException();
		this.app = app;
	}

	@Override
	public void Initialize(AppBase app) {
		if (app != this.app)
			throw new IllegalArgumentException();
		RegisterZezeTables(app.getZeze());
		RegisterProtocols(app.getZeze().redirect.providerApp.providerService);
	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(app.getZeze().redirect.providerApp.providerService);
		UnRegisterZezeTables(app.getZeze());
	}

	public void Start(String serviceNamePrefix, String providerDirectIp, int providerDirectPort) {
		var name = ProviderDistribute.makeServiceName(serviceNamePrefix, getId());
		var identity = String.valueOf(app.getZeze().getConfig().getServerId());
		app.getZeze().getServiceManager().registerService(new BServiceInfo(
				name, identity, app.getZeze().getConfig().getAppVersion(), providerDirectIp, providerDirectPort));
	}

	public static BConcurrentKey newRankKey(int rankType, int timeType) {
		return newRankKey(System.currentTimeMillis(), rankType, timeType, 0);
	}

	public static BConcurrentKey newRankKey(int rankType, int timeType, long customizeId) {
		return newRankKey(System.currentTimeMillis(), rankType, timeType, customizeId);
	}

	public static BConcurrentKey newRankKey(long time, int rankType, int timeType) {
		return newRankKey(time, rankType, timeType, 0);
	}

	public static BConcurrentKey newRankKey(long time, int rankType, int timeType, long customizeId) {
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
			offset = getSimpleChineseSeason(c);
			break;

		case BConcurrentKey.TimeTypeYear:
			offset = 0;
			break;

		case BConcurrentKey.TimeTypeCustomize:
			year = 0;
			offset = customizeId;
			break;

		default:
			throw new UnsupportedOperationException("Unsupported TimeType=" + timeType);
		}
		return new BConcurrentKey(rankType, 0, timeType, year, offset);
	}

	public static int getSimpleChineseSeason(Calendar c) {
		//@formatter:off
		var month = c.get(Calendar.MONTH);
		if (month < 3) return 4; // 12,1,2
		if (month < 6) return 1; // 3,4,5
		if (month < 9) return 2; // 6,7,8
		if (month < 12) return 3; // 9,10,11
		return 4; // 12,1,2
		//@formatter:on
	}

	/**
	 * 为排行榜设置需要的数量。【有默认值】
	 */
	public final int getRankSize(int rankType) {
		var volatileTmp = funcRankSize;
		if (null != volatileTmp)
			return volatileTmp.applyAsInt(rankType);
		return 100;
	}

	/**
	 * 为排行榜设置最大并发级别。【有默认值】
	 * 【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】
	 * 决定了最大的并发度，改变的时候，旧数据全部失效，需要清除，重建。
	 * 一般选择一个足够大，但是又不能太大的数据。
	 */
	public final int getConcurrentLevel(int rankType) {
		var volatileTmp = funcConcurrentLevel;
		if (null != volatileTmp)
			return volatileTmp.applyAsInt(rankType);
		return 128; // default
	}

	public final long getRankCacheTimeout(int rankType) {
		var volatileTmp = funcRankCacheTimeout;
		if (null != volatileTmp)
			return volatileTmp.applyAsLong(rankType);
		return 5 * 60 * 1000; // default
	}

	/**
	 * 排行榜中间数据的数量。【有默认值】
	 */
	public final int getComputeCount(int rankType) {
		var factor = computeFactor;
		if (factor < 2)
			factor = 2;
		return (int)(getConcurrentLevel(rankType) * factor);
	}

	/**
	 * 根据 value 设置到排行榜中
	 */
	@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
	public RedirectFuture<Long> removeRank(int hash, BConcurrentKey keyHint, long roleId) {
		_removeRank(hash, keyHint, roleId, null);
		return RedirectFuture.finish(0L);
	}

	private BRankList _removeRank(int hash, BConcurrentKey keyHint, long roleId, OutObject<BRankValue> outExist) {
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());

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
		if (null != outExist)
			outExist.value = exist;
		return rank;
	}

	/**
	 * 根据 value 设置到排行榜中
	 */
	@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
	public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, long value, Binary valueEx) {
		var outExist = new OutObject<BRankValue>();
		var rank = _removeRank(hash, keyHint, roleId, outExist);
		int maxCount = getComputeCount(keyHint.getRankType());
		// insert if in rank. 使用binarySearch会造成相同分数不稳定。
		for (int i = 0; i < rank.getRankList().size(); ++i) {
			if (rank.getRankList().get(i).getValue() < value) {
				BRankValue tempVar = new BRankValue();
				tempVar.setRoleId(roleId);
				tempVar.setValue(value);
				tempVar.setValueEx(valueEx);
				rank.getRankList().add(i, tempVar);
				if (rank.getRankList().size() > maxCount) {
					rank.getRankList().remove(rank.getRankList().size() - 1);
				}
				return RedirectFuture.finish(0L);
			}
		}
		// A: 如果排行的Value可能减少，那么当它原来存在，但现在处于队尾时，不要再进榜。
		// 因为此时可能存在未进榜但比它大的Value。
		// B: 但是在进榜玩家比榜单数量少的时候，如果不进榜，队尾的玩家更新还在队尾就会消失。
		if (rank.getRankList().size() < getRankSize(keyHint.getRankType())
				|| (rank.getRankList().size() < maxCount && null == outExist.value)) {
			BRankValue tempVar2 = new BRankValue();
			tempVar2.setRoleId(roleId);
			tempVar2.setValue(value);
			tempVar2.setValueEx(valueEx);
			rank.getRankList().add(tempVar2);
		}
		return RedirectFuture.finish(0L);
	}

	private static BRankList merge(BRankList left, BRankList right) {
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

	public static class RankTotal extends ReentrantLock {
		private long BuildTime;
		private BRankList TableValue;

		public final long getBuildTime() {
			return BuildTime;
		}

		public final void setBuildTime(long value) {
			BuildTime = value;
		}

		public final BRankList getTableValue() {
			return TableValue;
		}

		public final void setTableValue(BRankList value) {
			TableValue = value;
		}
	}

	private final ConcurrentHashMap<BConcurrentKey, RankTotal> rankCached = new ConcurrentHashMap<>();

	public RankTotal getRankTotal(BConcurrentKey keyHint) throws Exception {
		return getRankTotal(keyHint, getRankSize(keyHint.getRankType()));
	}

	public RankTotal getRankTotal(BConcurrentKey keyHint, int countNeed) throws Exception {
		var rank = rankCached.computeIfAbsent(keyHint, __ -> new RankTotal());
		rank.lock();
		try {
			long now = System.currentTimeMillis();
			if (now - rank.getBuildTime() < getRankCacheTimeout(keyHint.getRankType())) {
				return rank;
			}
			getRankAll(keyHint).onAllDone(ctx -> {
				var it = ctx.getAllResults().iterator();
				switch (ctx.getAllResults().size()) {
				case 0:
					rank.setTableValue(new BRankList());
					break;

				case 1:
					it.moveToNext();
					rank.setTableValue(it.value().rankList.copy());
					break;

				default:
					// 合并过程中，结果是新的 BRankList，List中的 BRankValue 引用到表中。
					// 最后 Copy 一次。
					it.moveToNext();
					BRankList current = it.value().rankList;
					while (it.moveToNext()) {
						current = merge(current, it.value().rankList);
						if (current.getRankList().size() > countNeed) {
							// 合并中间结果超过需要的数量可以先删除。
							// 第一个current直接引用table.data，不能删除。
							//noinspection ListRemoveInLoop
							for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
								current.getRankList().remove(ir);
							//current.getRankList().RemoveRange(countNeed, current.getRankList().Count - countNeed);
						}
					}
					rank.setTableValue(current.copy()); // current 可能还直接引用第一个，虽然逻辑上不大可能。先Copy。
					break;
				}
				rank.setBuildTime(System.currentTimeMillis());
				if (rank.getTableValue().getRankList().size() > countNeed) { // 再次删除多余的结果。
					//noinspection ListRemoveInLoop
					for (int ir = rank.getTableValue().getRankList().size() - 1; ir >= countNeed; --ir)
						rank.getTableValue().getRankList().remove(ir);
					//Rank.TableValue.getRankList().RemoveRange(countNeed, Rank.TableValue.RankList.Count - countNeed);
				}
			}).await();
			return rank;
		} finally {
			rank.unlock();
		}
	}

	public static class RRankList extends RedirectResult {
		public BRankList rankList = new BRankList(); // 目前要求输出结构的所有字段都不能为null,需要构造时创建
	}

	/**
	 * 分别去hash分组所在的服务器上查询，并得到所有的hash分组。
	 */
	public RedirectAllFuture<RRankList> getRankAll(BConcurrentKey keyHint) {
		return getRankAll(getConcurrentLevel(keyHint.getRankType()), keyHint);
	}

	// 属性参数是获取总的并发分组数量的代码，直接复制到生成代码中。
	// 最好改成protected并新增一个隐藏hash参数的public方法调用这里
	@RedirectAll
	protected RedirectAllFuture<RRankList> getRankAll(int hash, BConcurrentKey keyHint) {
		// 根据hash获取分组rank。
		var result = new RRankList();
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());
		var concurrentKey = new BConcurrentKey(keyHint.getRankType(), hash % concurrentLevel,
				keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
		result.rankList = _trank.getOrAdd(concurrentKey);
		return RedirectAllFuture.result(result);
	}

	public long getRankPosition(BConcurrentKey keyHint, long roleId) throws Exception {
		return getRankPosition(keyHint, roleId, null);
	}

	public long getRankPosition(BConcurrentKey keyHint, long roleId, OutObject<RankTotal> out) throws Exception {
		var total = getRankTotal(keyHint);
		if (null != out)
			out.value = total;

		// 判断是否在版内，并且得到排名位置。
		var position = 0;
		for (var r : total.getTableValue().getRankList()) {
			position++;
			if (r.getRoleId() == roleId) {
				return position;
			}
		}

		return -1;
	}

	public long getRankPositionWithGuess(BConcurrentKey keyHint, long roleId, long score, long totalUser) throws Exception {
		var total = new OutObject<RankTotal>();
		var pos = getRankPosition(keyHint, roleId, total);
		if (pos > 0)
			return pos;

		var list = total.value.getTableValue().getRankList();
		var lastRankScore = list.isEmpty() ? 0 : list.get(list.size() - 1).getValue();
		var lastRankPosition = list.size();

		return totalUser - (long)((double)score / lastRankScore * (totalUser - lastRankPosition));
	}

	public void deleteRank(BConcurrentKey keyHint) {
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());
		for (int i = 0; i < concurrentLevel; ++i) {
			var concurrentKey = new BConcurrentKey(
					keyHint.getRankType(),
					i,
					keyHint.getTimeType(),
					keyHint.getYear(),
					keyHint.getOffset());
			_trank.remove(concurrentKey);
		}
	}

	/**
	 * 直接查询数据库并合并分组数据。直接查询没有使用缓存。
	 */
	public BRankList getRankDirect(BConcurrentKey keyHint) {
		return getRankDirect(keyHint, getRankSize(keyHint.getRankType()));
	}

	private BRankList getRankDirect(BConcurrentKey keyHint, int countNeed) {
		// rebuild
		ArrayList<BRankList> datas = new ArrayList<>();
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());
		for (int i = 0; i < concurrentLevel; ++i) {
			var concurrentKey = new BConcurrentKey(keyHint.getRankType(), i, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
			datas.add(_trank.getOrAdd(concurrentKey));
		}
		return merge(datas, countNeed);
	}

	private static BRankList merge(Collection<BRankList> datas, int countNeed) {
		var size = datas.size();
		if (0 == size)
			return new BRankList();
		if (1 == size)
			return datas.iterator().next().copy(); // only one item

		// 合并过程中，结果是新的 BRankList，List中的 BRankValue 引用到表中。
		// 最后 Copy 一次。
		var it = datas.iterator();
		BRankList current = it.next();
		while (it.hasNext()) {
			current = merge(current, it.next());
			if (current.getRankList().size() > countNeed) {
				// 合并中间结果超过需要的数量可以先删除。
				// 第一个current直接引用table.data，不能删除。
				//noinspection ListRemoveInLoop
				for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
					current.getRankList().remove(ir);
			}
		}
		// current = current.copy(); // current 可能还直接引用第一个，虽然逻辑上不大可能。先copy。
		if (current.getRankList().size() > countNeed) { // 再次删除多余的结果。
			//noinspection ListRemoveInLoop
			for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
				current.getRankList().remove(ir);
		}
		return current;
	}
}
