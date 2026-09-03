package Zeze.Game;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderDistribute;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BRankList;
import Zeze.Builtin.Game.Rank.BRankListReadOnly;
import Zeze.Builtin.Game.Rank.BRankValue;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Collections.BeanFactory;
import Zeze.Serialize.Serializable;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Bean;
import Zeze.Util.OutObject;
import org.jetbrains.annotations.NotNull;

public class Rank extends AbstractRank {
	private final AppBase app;
	protected static final BeanFactory beanFactory = new BeanFactory();

	private volatile IntUnaryOperator funcRankSize;
	private volatile IntUnaryOperator funcConcurrentLevel;
	private volatile LongUnaryOperator funcRankCacheTimeout;
	private volatile IntUnaryOperator funcRankCacheCapacity;

	@SuppressWarnings("CanBeFinal")
	private volatile float computeFactor = 2.5f;
	private volatile Comparator<Bean> compactor = new LongOnlyCompactor();

	public static class LongOnlyCompactor implements Comparator<Bean> {
		@Override
		public int compare(Bean o1, Bean o2) {
			if (o1.typeId() == BValueLong.TYPEID)
				return Long.compare(((BValueLong)o1).getValue(), ((BValueLong)o2).getValue());
			throw new RuntimeException("unknown compactor bean");
		}
	}

	public void setFuncConcurrentLevel(IntUnaryOperator funcConcurrentLevel) {
		this.funcConcurrentLevel = funcConcurrentLevel;
	}

	public IntUnaryOperator getFuncConcurrentLevel() {
		return funcConcurrentLevel;
	}

	public void setFuncRankCacheTimeout(LongUnaryOperator funcRankCacheTimeout) {
		this.funcRankCacheTimeout = funcRankCacheTimeout;
	}

	public LongUnaryOperator getFuncRankCacheTimeout() {
		return funcRankCacheTimeout;
	}

	public void setFuncRankCacheCapacity(IntUnaryOperator funcRankCacheCapacity) {
		this.funcRankCacheCapacity = funcRankCacheCapacity;
	}

	public IntUnaryOperator getFuncRankCacheCapacity() {
		return funcRankCacheCapacity;
	}

	public void setFuncRankSize(IntUnaryOperator funcRankSize) {
		this.funcRankSize = funcRankSize;
	}

	public IntUnaryOperator getFuncRankSize() {
		return funcRankSize;
	}

	public void setComputeFactor(float computeFactor) {
		this.computeFactor = computeFactor;
	}

	public float getComputeFactor() {
		return computeFactor;
	}

	public void setCompactor(Comparator<Bean> compactor) {
		this.compactor = compactor;
	}

	public Comparator<Bean> getCompactor() {
		return compactor;
	}

	public static Rank create(AppBase app) {
		return GenModule.createRedirectModule(Rank.class, app);
	}

	protected Rank(AppBase app) {
		if (app == null)
			throw new NullPointerException();
		this.app = app;
	}

	@Override
	public void Initialize(@NotNull AppBase app) {
		if (app != this.app)
			throw new IllegalArgumentException();
		RegisterZezeTables(app.getZeze());
		RegisterProtocols(app.getZeze().redirect.providerApp.providerService);
	}

	public static void register(@NotNull Class<? extends Serializable> cls) {
		beanFactory.register(cls);
	}

	public static long getSpecialTypeIdFromBean(@NotNull Serializable bean) {
		return bean.typeId();
	}

	public static @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(app.getZeze().redirect.providerApp.providerService);
		UnRegisterZezeTables(app.getZeze());
	}

	public void Start(String serviceNamePrefix, String providerDirectIp, int providerDirectPort) {
		var name = ProviderDistribute.makeServiceName(serviceNamePrefix, getId());
		var identity = String.valueOf(app.getZeze().getConfig().getServerId());
		//noinspection DataFlowIssue
		app.getZeze().getServiceManager().registerService(new BServiceInfo(
				name, identity, app.getZeze().getConfig().getAppMainVersion(), providerDirectIp, providerDirectPort));
	}

	public static BConcurrentKey newRankKey(int rankType, int timeType) {
		return newRankKey(System.currentTimeMillis(), rankType, timeType, 0);
	}

	public static BConcurrentKey newRankKey(long time, int rankType, int timeType) {
		return newRankKey(time, rankType, timeType, 0);
	}

	public static BConcurrentKey newRankKey(int rankType, long customizeId) {
		return newRankKey(System.currentTimeMillis(), rankType, BConcurrentKey.TimeTypeCustomize, customizeId);
	}

	public static BConcurrentKey newRankKey(long time, int rankType, int timeType, long customizeId) {
		var c = Calendar.getInstance();
		c.setTimeInMillis(time);
		var year = c.get(Calendar.YEAR); // 后面根据TimeType可能覆盖这个值。
		long offset = switch (timeType) {
			case BConcurrentKey.TimeTypeTotal -> {
				year = 0;
				yield 0;
			}
			case BConcurrentKey.TimeTypeDay -> c.get(Calendar.DAY_OF_YEAR);
			case BConcurrentKey.TimeTypeWeek -> c.get(Calendar.WEEK_OF_YEAR);
			case BConcurrentKey.TimeTypeSeason -> getSimpleChineseSeason(c);
			case BConcurrentKey.TimeTypeYear -> 0;
			case BConcurrentKey.TimeTypeCustomize -> {
				year = 0;
				yield customizeId;
			}
			default -> throw new UnsupportedOperationException("Unsupported TimeType=" + timeType);
		};

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
	 * rankCached 缓存的容量上限。【有默认值】
	 * FND-G1-7：周期榜（Day/Week/Season/Year）每个新周期 key、自定义榜（TimeTypeCustomize）
	 * 每个 customizeId 首次访问都会新建 RankTotal 并持有全量合并快照，旧 key 不再被查询。
	 * 超过上限时先淘汰已过期的条目，仍超限则按 BuildTime 淘汰最旧的（近似 LRU）。
	 * 返回 &lt;=0 表示不淘汰（不设上限）。
	 */
	public final int getRankCacheCapacity(int rankType) {
		var volatileTmp = funcRankCacheCapacity;
		if (null != volatileTmp)
			return volatileTmp.applyAsInt(rankType);
		// default：默认值与 rankSize(100)/concurrentLevel(128) 同量级。
		// 每条目一份全量快照（默认 <=100 条，约 1-3KB），256 条常驻上限 <1MB；
		// 足够容纳总榜+当期周期榜+典型规模的活跃自定义榜（公会/活动），
		// 避免活跃榜被挤出后每次访问都触发 getRankDirect 全量合并（默认 128 段）的性能反噬。
		return 256;
	}

	/**
	 * 排行榜中间数据的数量。【有默认值】
	 */
	public final int getComputeCount(int rankType) {
		var factor = computeFactor;
		if (factor < 2)
			factor = 2;
		return (int)(getRankSize(rankType) * factor);
	}

	/**
	 * 删除rank
	 */
	@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
	public RedirectFuture<Long> removeRank(int hash, BConcurrentKey keyHint, long roleId) {
		_removeRank(hash, keyHint, roleId, null);
		return RedirectFuture.finish(0L);
	}

	private BRankList _removeRank(int hash, BConcurrentKey keyHint, long roleId, OutObject<BRankValue> outExist) {
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());

		var concurrentKey = new BConcurrentKey(keyHint.getRankType(),
				Integer.remainderUnsigned(hash, concurrentLevel),
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
	public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, Bean value) {
		beanFactory.register(value);
		var outExist = new OutObject<BRankValue>();
		var rank = _removeRank(hash, keyHint, roleId, outExist);
		int maxCount = getComputeCount(keyHint.getRankType());
		// insert if in rank. 使用binarySearch会造成相同分数不稳定。
		for (int i = 0; i < rank.getRankList().size(); ++i) {
			var c = compactor.compare(rank.getRankList().get(i).getDynamic().getBean(), value);
			if (c < 0) {
				BRankValue tempVar = new BRankValue();
				tempVar.setRoleId(roleId);
				tempVar.getDynamic().setBean(value);
				rank.getRankList().add(i, tempVar);
				if (rank.getRankList().size() > maxCount) {
					rank.getRankList().removeLast();
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
			tempVar2.getDynamic().setBean(value);
			rank.getRankList().add(tempVar2);
		}
		return RedirectFuture.finish(0L);
	}

	private BRankList merge(BRankList left, BRankList right) {
		BRankList result = new BRankList();
		int indexLeft = 0;
		int indexRight = 0;
		while (indexLeft < left.getRankList().size() && indexRight < right.getRankList().size()) {
			var c = compactor.compare(left.getRankList().get(indexLeft).getDynamic().getBean(),
					right.getRankList().get(indexRight).getDynamic().getBean());
			if (c >= 0) {
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
		// volatile：TableValue在getRankPosition等处锁外读。写序必须先TableValue后BuildTime，
		// volatile写读序保证读者看到新BuildTime必见新TableValue（"旧time+新value"只会被判过期重建，无害）。
		private volatile long BuildTime;
		private volatile BRankListReadOnly TableValue;
		private final BConcurrentKey keyHint;

		public RankTotal(BConcurrentKey keyHint) {
			this.keyHint = keyHint;
		}

		public BConcurrentKey getKeyHint() {
			return keyHint;
		}

		public final long getBuildTime() {
			return BuildTime;
		}

		public final void setBuildTime(long value) {
			BuildTime = value;
		}

		public final BRankListReadOnly getTableValue() {
			return TableValue;
		}

		public final void setTableValue(BRankList value) {
			TableValue = value;
		}
	}

	private final ConcurrentHashMap<BConcurrentKey, RankTotal> rankCached = new ConcurrentHashMap<>();

	/**
	 * FND-G1-7：rankCached 超过容量上限时的淘汰。
	 * 先淘汰已过期的条目（下次访问自动重建，无功能影响），仍超限则按 BuildTime 淘汰最旧的
	 * （近似 LRU）；当前正在服务的 keep（刚 computeIfAbsent 得到/重建的）受保护，
	 * 避免新条目因 BuildTime 尚未设置被立即淘汰。
	 * 不同 rankType 共用本 map，统一使用当前 rankType 的配置，只影响淘汰早晚，不影响正确性。
	 * 淘汰仅移除 map 条目：已拿到旧 RankTotal 引用的读者不受影响（快照仍在对象内），
	 * 并发竞争（其他线程先移除/替换同 key）时本轮放弃，等待下次访问再触发。
	 */
	private void evictRankCacheIfOver(int rankType, RankTotal keep) {
		int capacity = getRankCacheCapacity(rankType);
		if (capacity <= 0 || rankCached.size() <= capacity)
			return;
		long now = System.currentTimeMillis();
		long timeout = getRankCacheTimeout(rankType);
		for (var it = rankCached.entrySet().iterator(); it.hasNext(); ) {
			if (rankCached.size() <= capacity)
				return;
			var total = it.next().getValue();
			if (total != keep && now - total.getBuildTime() >= timeout)
				it.remove();
		}
		while (rankCached.size() > capacity) {
			BConcurrentKey oldestKey = null;
			RankTotal oldestTotal = null;
			for (var entry : rankCached.entrySet()) {
				var total = entry.getValue();
				if (total == keep)
					continue;
				if (oldestTotal == null || total.getBuildTime() < oldestTotal.getBuildTime()) {
					oldestTotal = total;
					oldestKey = entry.getKey();
				}
			}
			if (oldestKey == null || !rankCached.remove(oldestKey, oldestTotal))
				return; // 只剩 keep 或并发竞争：交给下次访问
		}
	}

	public RankTotal getRankTotal(BConcurrentKey keyHint) {
		return getRankTotal(keyHint, getRankSize(keyHint.getRankType()));
	}

	public RankTotal getRankTotal(BConcurrentKey keyHint, int countNeed) {
		var rank = rankCached.computeIfAbsent(keyHint, __ -> new RankTotal(keyHint));
		evictRankCacheIfOver(keyHint.getRankType(), rank);
		var now = System.currentTimeMillis();
		rank.lock();
		try {
			// 锁的职责：freshness双检查 + 重建single-flight（并发miss不重复执行getRankDirect的跨段查询归并）。
			if (now - rank.getBuildTime() < getRankCacheTimeout(keyHint.getRankType()))
				return rank;
			rank.setTableValue(getRankDirect(keyHint, countNeed));
			rank.setBuildTime(now);
		} finally {
			rank.unlock();
		}
		return rank;
	}

	public long getRankPosition(BConcurrentKey keyHint, long roleId) {
		return getRankPosition(keyHint, roleId, null);
	}

	public long getRankPosition(BConcurrentKey keyHint, long roleId, OutObject<RankTotal> out) {
		var total = getRankTotal(keyHint);
		if (null != out)
			out.value = total;

		// 判断是否在版内，并且得到排名位置。
		var position = 0;
		for (var r : total.getTableValue().getRankListReadOnly()) {
			position++;
			if (r.getRoleId() == roleId) {
				return position;
			}
		}

		return -1;
	}

	public long getRankPositionWithGuess(BConcurrentKey keyHint, long roleId, long score, long totalUser) {
		var total = new OutObject<RankTotal>();
		var pos = getRankPosition(keyHint, roleId, total);
		if (pos > 0)
			return pos;

		var list = total.value.getTableValue().getRankListReadOnly();
		if (list.isEmpty())
			return totalUser; // 空榜无法估计，按排在所有人之后处理（原代码此时取list.get(-1)会越界崩溃）。
		var bean = list.get(list.size() - 1).getDynamicReadOnly().getBean();
		if (bean.typeId() != BValueLong.TYPEID)
			throw new RuntimeException("only value long has guess.");
		var lastRankScore = ((BValueLong)bean).getValue();
		var lastRankPosition = list.size();
		if (lastRankScore == 0)
			return totalUser; // 防除零：score/0 得Infinity，结果为负数。

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
	 * 直接合并hash分组，不适用缓存。
	 *
	 * @param keyHintFrom from
	 * @param keyHintTo   to
	 */
	public void mergeRank(BConcurrentKey keyHintFrom, BConcurrentKey keyHintTo) {
		if (keyHintFrom.getRankType() != keyHintTo.getRankType()
				|| keyHintFrom.getTimeType() != keyHintTo.getTimeType()
				|| keyHintFrom.getYear() != keyHintTo.getYear())
			throw new RuntimeException("rank type mismatch.");

		if (keyHintFrom.getOffset() == keyHintTo.getOffset())// same hint
			return;

		int concurrentLevel = getConcurrentLevel(keyHintFrom.getRankType());
		int countNeed = getComputeCount(keyHintFrom.getRankType());
		for (int i = 0; i < concurrentLevel; ++i) {
			var concurrentKeyFrom = new BConcurrentKey(keyHintFrom.getRankType(), i, keyHintFrom.getTimeType(),
					keyHintFrom.getYear(), keyHintFrom.getOffset());
			var concurrentKeyTo = new BConcurrentKey(keyHintTo.getRankType(), i, keyHintTo.getTimeType(),
					keyHintTo.getYear(), keyHintTo.getOffset());
			var from = _trank.getOrAdd(concurrentKeyFrom);
			// 把from里面的所有玩家从目标列表中清除。
			for (var j = 0; j < from.getRankList().size(); ++j)
				_removeRank(i, keyHintTo, from.getRankList().get(j).getRoleId(), null);
			// 合并到目标列表中。
			var merged = merge(_trank.getOrAdd(concurrentKeyTo), from);
			// 删除多余的数量。
			if (merged.getRankList().size() > countNeed) { // 再次删除多余的结果。
				//noinspection ListRemoveInLoop
				for (int ir = merged.getRankList().size() - 1; ir >= countNeed; --ir)
					merged.getRankList().remove(ir);
			}
			// 保存列表。
			_trank.put(concurrentKeyTo, merged);
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

	private BRankList merge(Collection<BRankList> datas, int countNeed) {
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
