package Zeze.Game;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

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
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;

/**
 * 分布式排行榜：按并发分段存储、RedirectHash 路由更新，合并分段生成全量快照并缓存。
 */
public class Rank extends AbstractRank {
	private final AppBase app;
	protected static final BeanFactory beanFactory = new BeanFactory();

	private volatile @Nullable IntUnaryOperator funcRankSize;
	private volatile @Nullable IntUnaryOperator funcConcurrentLevel;
	private volatile @Nullable LongUnaryOperator funcRankCacheTimeout;
	private volatile @Nullable IntUnaryOperator funcRankCacheCapacity;

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

	public void setFuncConcurrentLevel(@Nullable IntUnaryOperator funcConcurrentLevel) {
		this.funcConcurrentLevel = funcConcurrentLevel;
	}

	public @Nullable IntUnaryOperator getFuncConcurrentLevel() {
		return funcConcurrentLevel;
	}

	public void setFuncRankCacheTimeout(@Nullable LongUnaryOperator funcRankCacheTimeout) {
		this.funcRankCacheTimeout = funcRankCacheTimeout;
	}

	public @Nullable LongUnaryOperator getFuncRankCacheTimeout() {
		return funcRankCacheTimeout;
	}

	public void setFuncRankCacheCapacity(@Nullable IntUnaryOperator funcRankCacheCapacity) {
		this.funcRankCacheCapacity = funcRankCacheCapacity;
	}

	public @Nullable IntUnaryOperator getFuncRankCacheCapacity() {
		return funcRankCacheCapacity;
	}

	public void setFuncRankSize(@Nullable IntUnaryOperator funcRankSize) {
		this.funcRankSize = funcRankSize;
	}

	public @Nullable IntUnaryOperator getFuncRankSize() {
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

	protected Rank(@Nullable AppBase app) {
		if (app == null)
			throw new NullPointerException();
		this.app = app;
	}

	@Override
	public void Initialize(AppBase app) {
		if (app != this.app)
			throw new IllegalArgumentException();
		RegisterZezeTables(Objects.requireNonNull(app.getZeze()));
		//noinspection DataFlowIssue
		RegisterProtocols(app.getZeze().redirect.providerApp.providerService);
	}

	public static void register(Class<? extends Serializable> cls) {
		beanFactory.register(cls);
	}

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return bean.typeId();
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	@Override
	public void UnRegister() {
		//noinspection DataFlowIssue
		UnRegisterProtocols(app.getZeze().redirect.providerApp.providerService);
		UnRegisterZezeTables(Objects.requireNonNull(app.getZeze()));
	}

	public void Start(String serviceNamePrefix, String providerDirectIp, int providerDirectPort) {
		var name = ProviderDistribute.makeServiceName(serviceNamePrefix, getId());
		//noinspection DataFlowIssue
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
		// 固定周定义（周一为一周之始、最少 1 天），消除多节点 JVM 默认 locale 差异。
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setMinimalDaysInFirstWeek(1);
		c.setTimeInMillis(time);
		var year = c.get(Calendar.YEAR); // 后面根据TimeType可能覆盖这个值。
		long offset = switch (timeType) {
			case BConcurrentKey.TimeTypeTotal -> {
				year = 0;
				yield 0;
			}
			case BConcurrentKey.TimeTypeDay -> c.get(Calendar.DAY_OF_YEAR);
			case BConcurrentKey.TimeTypeWeek -> {
				// 周榜使用基于周的年份（getWeekYear），避免年末跨周时与格里年错配。
				year = c.getWeekYear();
				yield c.get(Calendar.WEEK_OF_YEAR);
			}
			case BConcurrentKey.TimeTypeSeason -> {
				// 冬季（12/1/2 月）统一归入次年，避免跨年拆分同一冬季。
				if (c.get(Calendar.MONTH) == Calendar.DECEMBER)
					year = year + 1;
				yield getSimpleChineseSeason(c);
			}
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
		// Calendar.MONTH 为 0 基（0=1月..11=12月）。
		var month = c.get(Calendar.MONTH);
		if (month < 2) return 4; // 1,2月（冬）
		if (month < 5) return 1; // 3,4,5月（春）
		if (month < 8) return 2; // 6,7,8月（夏）
		if (month < 11) return 3; // 9,10,11月（秋）
		return 4; // 12月（冬）
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
		if (null != volatileTmp) {
			// 下界为 1，避免自定义函数返回 <=0 导致取余除零。
			return Math.max(1, volatileTmp.applyAsInt(rankType));
		}
		return 128; // default
	}

	public final long getRankCacheTimeout(int rankType) {
		var volatileTmp = funcRankCacheTimeout;
		if (null != volatileTmp)
			return volatileTmp.applyAsLong(rankType);
		return 5 * 60 * 1000; // default
	}

	/**
	 * rankCached 缓存的容量上限（默认 256，&lt;=0 表示不设上限）；超限时先淘汰过期条目，再按 BuildTime 淘汰最旧条目。
	 */
	public final int getRankCacheCapacity(int rankType) {
		var volatileTmp = funcRankCacheCapacity;
		if (null != volatileTmp)
			return volatileTmp.applyAsInt(rankType);
		return 256; // default
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

	private BRankList _removeRank(int hash, BConcurrentKey keyHint, long roleId, @Nullable OutObject<BRankValue> outExist) {
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());

		var concurrentKey = new BConcurrentKey(keyHint.getRankType(),
				Integer.remainderUnsigned(hash, concurrentLevel),
				keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());

		var rank = _trank.getOrAdd(concurrentKey);
		// remove if role exist.
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
		// 段尾分数下降时，未保留的候选人可能排在该角色前面。分数持平或上升时，
		// 已保留的角色仍应留在候选列表，供跨段合并和更大 countNeed 的查询使用。
		if (rank.getRankList().size() < getRankSize(keyHint.getRankType())
				|| (rank.getRankList().size() < maxCount && (outExist.value == null
				|| compactor.compare(outExist.value.getDynamic().getBean(), value) <= 0))) {
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
		// 锁外读 TableValue；发布时保持先写 TableValue 后写 BuildTime 的 volatile 写序。
		private volatile long BuildTime;
		private volatile @Nullable BRankListReadOnly TableValue;
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

		public final @Nullable BRankListReadOnly getTableValue() {
			return TableValue;
		}

		public final void setTableValue(BRankListReadOnly value) {
			TableValue = value;
		}
	}

	// countNeed 计入缓存键，避免不同容量查询复用被截断的快照。
	record RankCacheKey(BConcurrentKey keyHint, int countNeed) {
	}

	private final ConcurrentHashMap<RankCacheKey, RankTotal> rankCached = new ConcurrentHashMap<>();

	/**
	 * rankCached 超限时淘汰条目：先清已过期条目，再按 BuildTime 淘汰最旧条目，保护当前正在服务的 keep。
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
			RankCacheKey oldestKey = null;
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
		var rank = rankCached.computeIfAbsent(new RankCacheKey(keyHint, countNeed), __ -> new RankTotal(keyHint));
		evictRankCacheIfOver(keyHint.getRankType(), rank);
		var now = System.currentTimeMillis();
		rank.lock();
		try {
			// 锁内双检新鲜度并单飞重建，避免并发 miss 重复归并。
			var cached = rank.getTableValue();
			// 命中返回共享缓存快照，调用方应视为只读。
			if (cached != null && now - rank.getBuildTime() < getRankCacheTimeout(keyHint.getRankType()))
				return rank;
			var tableValue = getRankDirect(keyHint, countNeed);
			// 事务内返回局部快照，缓存写入延迟到提交后按先 value 后 time 发布，并预先深拷贝与调用方隔离。
			var cachedValue = tableValue.copy();
			Transaction.whileCommit(() -> {
				rank.setTableValue(cachedValue);
				rank.setBuildTime(System.currentTimeMillis());
			});
			var local = new RankTotal(keyHint);
			local.setTableValue(tableValue);
			local.setBuildTime(now);
			return local;
		} finally {
			rank.unlock();
		}
	}

	public long getRankPosition(BConcurrentKey keyHint, long roleId) {
		return getRankPosition(keyHint, roleId, null);
	}

	public long getRankPosition(BConcurrentKey keyHint, long roleId, @Nullable OutObject<RankTotal> out) {
		var total = getRankTotal(keyHint);
		if (null != out)
			out.value = total;

		// 判断是否在版内，并且得到排名位置。
		var position = 0;
		//noinspection DataFlowIssue
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

		//noinspection DataFlowIssue
		var list = total.value.getTableValue().getRankListReadOnly();
		if (list.isEmpty())
			return totalUser; // 空榜按排在所有人之后处理
		var bean = list.get(list.size() - 1).getDynamicReadOnly().getBean();
		if (bean.typeId() != BValueLong.TYPEID)
			throw new RuntimeException("only value long has guess.");
		var lastRankScore = ((BValueLong)bean).getValue();
		var lastRankPosition = list.size();
		if (lastRankScore == 0)
			return totalUser; // 防除零

		// 比值上限钳制为 1.0，避免未上榜高分者线性外推得负名次。
		var scoreRatio = Math.min(1.0, (double)score / lastRankScore);
		return totalUser - (long)(scoreRatio * (totalUser - lastRankPosition));
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
		invalidateRankCacheWhileCommit(keyHint.getRankType());
	}

	// 提交后按 rankType 整类失效缓存，避免回滚浪费重建。
	private void invalidateRankCacheWhileCommit(int rankType) {
		Transaction.whileCommit(() -> rankCached.keySet().removeIf(key -> key.keyHint().getRankType() == rankType));
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
			// merged 中的 BRankValue 借自受管表行，落表前必须深拷贝。
			_trank.put(concurrentKeyTo, merged.copy());
		}
		invalidateRankCacheWhileCommit(keyHintFrom.getRankType());
	}

	/**
	 * 直接查询数据库并合并分组数据。直接查询没有使用缓存。
	 */
	public BRankList getRankDirect(BConcurrentKey keyHint) {
		return getRankDirect(keyHint, getRankSize(keyHint.getRankType()));
	}

	private BRankList getRankDirect(BConcurrentKey keyHint, int countNeed) {
		// 走事务 get 而非 selectDirty，确保 isMemory 表已提交未 checkpoint 的分片也能通过 storage 装载且不建空行。
		ArrayList<BRankList> datas = new ArrayList<>();
		int concurrentLevel = getConcurrentLevel(keyHint.getRankType());
		for (int i = 0; i < concurrentLevel; ++i) {
			var concurrentKey = new BConcurrentKey(keyHint.getRankType(), i, keyHint.getTimeType(), keyHint.getYear(), keyHint.getOffset());
			var segment = _trank.get(concurrentKey);
			if (segment != null)
				datas.add(segment);
		}
		if (concurrentLevel > 1 && datas.size() == 1) {
			var segment = datas.get(0);
			var entries = segment.getRankList();
			// 稀疏榜单只复制要返回的条目，含未知字段的段沿用原有复制路径。
			if (segment.unknown() == null && countNeed >= 0 && entries.size() > countNeed) {
				var result = new BRankList();
				for (int i = 0; i < countNeed; ++i)
					result.getRankList().add(entries.get(i).copy());
				return result;
			}
		}
		return merge(datas, countNeed);
	}

	private BRankList merge(Collection<BRankList> datas, int countNeed) {
		var size = datas.size();
		if (0 == size)
			return new BRankList();
		if (1 == size) { // only one item
			var result = datas.iterator().next().copy();
			// 单段同样按 countNeed 截断（段内最多保留 computeCount 条中间数据），先 copy 再删避免改表。
			//noinspection ListRemoveInLoop
			for (int ir = result.getRankList().size() - 1; ir >= countNeed; --ir)
				result.getRankList().remove(ir);
			return result;
		}

		// 合并中间结果借用表内 BRankValue，仅在最终出口深拷贝一次以与表及缓存隔离。
		var it = datas.iterator();
		BRankList current = it.next();
		while (it.hasNext()) {
			current = merge(current, it.next());
			if (current.getRankList().size() > countNeed) {
				// 合并中间结果超过需要的数量可以先删除（current已是merge产物的新列表）。
				//noinspection ListRemoveInLoop
				for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
					current.getRankList().remove(ir);
			}
		}
		if (current.getRankList().size() > countNeed) { // 再次删除多余的结果。
			//noinspection ListRemoveInLoop
			for (int ir = current.getRankList().size() - 1; ir >= countNeed; --ir)
				current.getRankList().remove(ir);
		}
		return current.copy(); // 唯一出口深拷贝：结果与表彻底别名隔离
	}
}
