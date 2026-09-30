package Zeze.History;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Application;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.List2Meta;
import Zeze.Transaction.Collections.LogList1;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Collections.LogOneMeta;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.Set1Meta;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.Log;
import Zeze.Transaction.LogDynamic;
import Zeze.Transaction.Logs.LogBeanKey;
import Zeze.Transaction.Logs.LogBinary;
import Zeze.Transaction.Logs.LogBool;
import Zeze.Transaction.Logs.LogByte;
import Zeze.Transaction.Logs.LogDecimal;
import Zeze.Transaction.Logs.LogDouble;
import Zeze.Transaction.Logs.LogFloat;
import Zeze.Transaction.Logs.LogInt;
import Zeze.Transaction.Logs.LogLong;
import Zeze.Transaction.Logs.LogQuaternion;
import Zeze.Transaction.Logs.LogShort;
import Zeze.Transaction.Logs.LogString;
import Zeze.Transaction.Logs.LogVector2;
import Zeze.Transaction.Logs.LogVector2Int;
import Zeze.Transaction.Logs.LogVector3;
import Zeze.Transaction.Logs.LogVector3Int;
import Zeze.Transaction.Logs.LogVector4;
import Zeze.Transaction.Table;
import Zeze.Util.KV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

/**
 * 回放端日志注册助手：扫描表（启动期全部表，或后启开的单表）的键值依赖，
 * 注册各集合/变量类型 Log 的解码工厂。
 */
public class Helper {
	private static final Logger logger = LogManager.getLogger(Helper.class);

	/** dynamic家族：工厂对+来源（宿主bean类#变量名），供同typeId多家族留痕。 */
	public static final class DynamicFamily {
		public final KV<ToLongFunction<Bean>, LongFunction<Bean>> factories;
		public final String where;

		DynamicFamily(@NotNull KV<ToLongFunction<Bean>, LongFunction<Bean>> factories, @NotNull String where) {
			this.factories = factories;
			this.where = where;
		}
	}

	/** dynamic外层pmapMeta家族登记项：外层meta+来源（宿主bean类#变量名），供同typeId决胜。 */
	public static final class DynamicMetaFamily {
		public final Map2Meta<?, ? extends Bean> meta;
		public final String where;

		DynamicMetaFamily(@NotNull Map2Meta<?, ? extends Bean> meta, @NotNull String where) {
			this.meta = meta;
			this.where = where;
		}
	}

	public static class DependsResult {
		public final HashSet<Class<?>> allBeans = new HashSet<>();
		public final HashSet<Class<? extends Bean>> beans = new HashSet<>();
		public final HashSet<Class<? extends Serializable>> beanKeys = new HashSet<>();
		public final HashSet<Class<?>> list1 = new HashSet<>();
		public final HashSet<Class<? extends Bean>> list2 = new HashSet<>();
		// dynamic list 家族按固定哨兵键登记：List2Meta 的 dynamic 构造器
		// typeId 是全局固定单值（Meta1.dynamicBeanTypeId，连 keyClass 分桶都没有）——任意两个
		// dynamic list 变量必然同 typeId，全部家族同键（哨兵）恰好表达"任意两个都冲突"。
		public final HashMap<KV<Class<?>, Class<?>>, DynamicFamily> list2Dynamic = new HashMap<>();
		public final HashSet<KV<Class<?>, Class<?>>> map1 = new HashSet<>();
		public final HashSet<KV<Class<?>, Class<? extends Bean>>> map2 = new HashSet<>();
		public final HashMap<KV<Class<?>, Class<? extends Bean>>, DynamicFamily> map2Dynamic = new HashMap<>();
		public final HashSet<Map1Meta<?, ?>> map1Metas = new HashSet<>();
		public final HashSet<Map2Meta<?, ? extends Bean>> map2Metas = new HashSet<>();
		// dynamic GTable 外层 pmapMeta 按决胜键登记（hist-01 外层收口，FND26/FND27 两波独立发现）：
		// 外层 typeId 的值身份固定 DynamicBean（GTable2 动态工厂路径），同 (rowClass,colClass) 的
		// 多个 dynamic gtable 变量产生 typeId 相同、valueCtor（各自家族 create 闭包）不同的
		// pmapMeta——若走 map2Metas 的 HashSet 身份序注册，Log.register 的 putIfAbsent 先到先得
		// 使胜者随 JVM 运行变化（identityHashCode 桶序），回放解码工厂跨重启翻转。同型于
		// map2Dynamic 的 (keyClass,DynamicBean) 决胜，键补齐外层维度 (rowClass,colClass)。
		public final HashMap<KV<Class<?>, Class<?>>, DynamicMetaFamily> map2MetasDynamic = new HashMap<>();
		public final HashSet<Class<?>> set1 = new HashSet<>();
		public final HashSet<KV<Class<? extends Comparable<?>>, Class<?>>> sortedMap1 = new HashSet<>();
		public final HashSet<KV<Class<? extends Comparable<?>>, Class<? extends Bean>>> sortedMap2 = new HashSet<>();
		public final HashMap<KV<Class<? extends Comparable<?>>, Class<? extends Bean>>, DynamicFamily>
				sortedMap2Dynamic = new HashMap<>();
		public final HashSet<SortedMap1Meta<? extends Comparable<?>, ?>> sortedMap1Metas = new HashSet<>();
		public final HashSet<SortedMap2Meta<? extends Comparable<?>, ? extends Bean>> sortedMap2Metas = new HashSet<>();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void registerAllTableLogs(@NotNull Application zeze) throws Exception {
		var result = new DependsResult();
		for (var db : zeze.getDatabases().values()) {
			for (var table : db.getTables()) {
				dependsTable(table, result);
			}
		}
		applyRegistrations(result);
	}

	/**
	 * 单表形式的依赖注册：后启动态开表（Application.openDynamicTable）的增量入口，
	 * 与启动期全表扫描（registerAllTableLogs）同构——开表不注册则该表的日志 typeId
	 * 永不注册，回放端解码失败。幂等：dependsBean 按 Class 去重，dynamic 家族经
	 * 进程级累积状态跨批次按 where 字典序决胜（同家族重复登记无操作），普通注册
	 * Log.register 先到先得且同名重复注册无害；只读表/无集合字段表为空操作。
	 */
	public static void registerTableLogs(@NotNull Table table) throws Exception {
		var result = new DependsResult();
		dependsTable(table, result);
		applyRegistrations(result);
	}

	private static void dependsTable(@NotNull Table table, @NotNull DependsResult result) throws Exception {
		var keyClass = table.getKeyClass();
		if (Serializable.class.isAssignableFrom(keyClass)) {
			// must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = table.getValueClass();
		dependsBean(valueClass, result);
	}

	// 包内可见：测试直驱跨批次注册形态（增量开表批次 vs 启动期批次的冲突面）。
	static void applyRegistrations(@NotNull DependsResult result) throws Exception {
		for (var beanClass : result.beans)
			registerLogOne(beanClass); // 没做为其他Bean的变量时是不需要注册的。这里区分了。
		for (var beanKeyClass : result.beanKeys)
			registerLogBeanKey(beanKeyClass);
		for (var list1Class : result.list1)
			registerLogList1(list1Class);
		for (var list2Class : result.list2)
			registerLogList2(list2Class);
		for (var e : sortedDynamic(result.list2Dynamic, f -> f.where))
			registerLogList2Dynamic(e.getValue().factories.getKey(), e.getValue().factories.getValue(),
					e.getValue().where);
		for (var map1KV : result.map1)
			registerLogMap1(map1KV.getKey(), map1KV.getValue());
		for (var map2KV : result.map2)
			registerLogMap2(map2KV.getKey(), map2KV.getValue());
		for (var e : sortedDynamic(result.map2Dynamic, f -> f.where))
			registerLogMap2Dynamic(e.getKey().getKey(), e.getValue().factories.getKey(),
					e.getValue().factories.getValue(), e.getValue().where);
		for (var meta : result.map1Metas)
			registerLogMap1Meta(meta);
		for (var meta : result.map2Metas)
			registerLogMap2Meta(meta);
		// dynamic外层pmapMeta在普通map2Metas之后按确定序注册（hist-01外层收口）：位置固定+
		// where排序使注册终态与注册顺序都是schema的纯函数（跨key typeId哈希碰撞也由where序
		// 决出唯一胜者，与内层sortedDynamic同口径）。
		for (var e : sortedDynamic(result.map2MetasDynamic, f -> f.where))
			registerLogMap2MetaDynamic(e.getValue().meta, e.getValue().where);
		for (var set1Class : result.set1)
			registerLogSet1(set1Class);
		for (var kv : result.sortedMap1)
			registerLogSortedMap1((Class<? extends Comparable>)kv.getKey(), kv.getValue());
		for (var kv : result.sortedMap2)
			registerLogSortedMap2((Class<? extends Comparable>)kv.getKey(), kv.getValue());
		for (var e : sortedDynamic(result.sortedMap2Dynamic, f -> f.where)) {
			registerLogSortedMap2Dynamic((Class<? extends Comparable>)e.getKey().getKey(),
					e.getValue().factories.getKey(), e.getValue().factories.getValue(), e.getValue().where);
		}
		for (var meta : result.sortedMap1Metas)
			registerLogSortedMap1Meta((SortedMap1Meta<? extends Comparable, ?>)meta);
		for (var meta : result.sortedMap2Metas)
			registerLogSortedMap2Meta((SortedMap2Meta<? extends Comparable, ? extends Bean>)meta);
		registerLogs();
	}

	@SuppressWarnings("unchecked")
	public static void dependsBean(@NotNull Class<?> beanClass, @NotNull DependsResult result) throws Exception {
		if (result.allBeans.add(beanClass)) {
			var obj = beanClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
			if (obj instanceof BeanKey beanKey) {
				result.beanKeys.add((Class<? extends Serializable>)beanClass);
				for (var v : beanKey.variables()) {
					var type = v.getType();
					if (!isBuiltinType(type))
						dependsBean(Class.forName(type), result);
				}
			} else if (obj instanceof Bean bean) {
				result.beans.add((Class<? extends Bean>)beanClass);
				for (var v : bean.variables()) {
					var type = v.getType();
					switch (type) {
					case "list":
					case "array":
						dependsList(beanClass, v, v.getValue(), result);
						break;
					case "map":
						dependsMap(beanClass, v, v.getKey(), v.getValue(), result);
						break;
					case "sortedmap":
						dependsSortedMap(beanClass, v, v.getKey(), v.getValue(), result);
						break;
					case "set":
						dependsSet(v.getValue(), result);
						break;
					case "gtable":
						var keys = v.getKey().split(",");
						dependsGTable(beanClass, v, keys[0].trim(), keys[1].trim(), v.getValue(), result);
						break;
					case "dynamic":
						// dynamic 变量的 value 由生成器携带允许 bean 集全名（逗号分隔，见
						// Gen BeanFormatter）——仅经 dynamic 可达的 bean 的集合/BeanKey 日志
						// 工厂依赖它注册（isBuiltinType("dynamic") 为真，default 分支直接跳过
						// 且无其他数据源），缺失时该类日志 typeId 永不注册，回放端 Log.create
						// 抛 unknown log typeId 毒记录卡死游标。空集=无可达 bean，无注册义务。
						if (!v.getValue().isEmpty())
							for (var beanName : v.getValue().split(","))
								dependsBean(Class.forName(beanName.trim()), result);
						break;
					default:
						if (!isBuiltinType(type))
							dependsBean(Class.forName(type), result);
						break;
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsList(@NotNull Class<?> beanClass, @NotNull BVariable.Data v, @NotNull String valueType,
								   @NotNull DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass != null) {
			if (valueClass == DynamicBean.class) {
				// list dynamic 家族同走 putDynamicFamily：HashSet 去重对 lambda/method-ref 工厂
				// 不生效（KV 值equals但工厂按对象身份比较）且无告警；而 list 家族的冲突面比
				// map 更无条件（map 的 typeId 至少按 keyClass 分桶，list 是全局固定单值，见
				// DependsResult.list2Dynamic注释）。对齐 map2Dynamic 形态：同typeId冲突按
				// 家族来源名字典序稳定决胜，败者warn留痕（决胜收敛在putDynamicFamily一处，
				// 与depends扫描的到达顺序无关）。list 无 key 维度，
				// 键用固定哨兵（与 map2Dynamic 的 (keyClass,DynamicBean) 键同形态）。
				putDynamicFamily(result.list2Dynamic, KV.create(DynamicBean.class, DynamicBean.class), beanClass, v);
			} else
				result.list1.add(valueClass);
			return;
		}
		valueClass = Class.forName(valueType);
		dependsBean(valueClass, result);
		// 选族须与生成器一致（Gen/Gen/java/TypeName.cs：IsNormalBean ? '2' : '1'）：beankey
		// 不是 normal bean，值集合生成"1"族（PList1/LogList1）；按"2"族注册的 typeId 线上
		// 不出现，回放端解码抛 unknown log typeId。
		if (BeanKey.class.isAssignableFrom(valueClass))
			result.list1.add(valueClass);
		else
			result.list2.add((Class<? extends Bean>)valueClass);
	}

	// 反射调用宿主bean的newDynamicBean_<VarName>取该变量的dynamic工厂对。
	private static DynamicFamily newDynamicFamily(@NotNull Class<?> beanClass, @NotNull BVariable.Data v) {
		try {
			var db = (DynamicBean)beanClass.getMethod("newDynamicBean_"
					+ Character.toUpperCase(v.getName().charAt(0))
					+ v.getName().substring(1), (Class<?>[])null).invoke(null, (Object[])null);
			return new DynamicFamily(KV.create(db.getGetBean(), db.getCreateBean()),
					beanClass.getName() + '#' + v.getName());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// dynamic集合的logTypeId不含值工厂身份：同(keyClass,DynamicBean)的第二个
	// 家族与首个同typeId，Log.register按注册表先到先得保留先注册者，后注册家族的日志在
	// 回放端用别人的create工厂解码（显式Bean:id编号重叠时静默解出错误bean，默认编号抛
	// incompatible中断回放）。
	// 决胜（hist-01）：同typeId冲突按家族来源名（宿主bean全名#变量名）字典序稳定决胜，小者胜
	// ——替代putIfAbsent的先到先得（先到由depends扫描的到达顺序决定，DependsResult各容器的
	// Class对象identityHashCode桶序跨JVM/重启不稳定，胜者可翻转）。where是schema的纯函数：
	// 同schema下无论家族以何种顺序到达，终胜者恒为where字典序最小者，跨JVM重启恒定。
	// 败者warn留痕（含双方宿主bean类名与变量名）；语义冲突的启动error需要每变量的
	// specialTypeId→beanClass映射表（生成器侧暴露）。
	// list分支（dependsList）接入同型登记：List2Meta 的 dynamic typeId 是
	// 全局固定单值（无keyClass分桶），list 家族冲突比 map 更无条件——warn 是当前唯一的检测面。
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void putDynamicFamily(@NotNull HashMap families, @NotNull Object key,
										 @NotNull Class<?> beanClass, @NotNull BVariable.Data v) {
		var family = newDynamicFamily(beanClass, v);
		var exist = (DynamicFamily)families.putIfAbsent(key, family);
		if (exist == null || (exist.factories.getKey() == family.factories.getKey()
				&& exist.factories.getValue() == family.factories.getValue()))
			return; // 首个家族，或同工厂重复登记：无冲突
		var keep = exist.where.compareTo(family.where) <= 0 ? exist : family;
		var drop = keep == exist ? family : exist;
		if (keep != exist)
			families.put(key, keep);
		logger.warn("dynamic collection family dropped: same log typeId with different factories."
				+ " keep={} drop={} key=({},{})。胜者按家族来源名字典序稳定决胜（跨进程恒定，"
				+ "不由注册顺序决定）；回放端按胜者家族的工厂解码，显式Bean:id编号重叠时"
				+ "将静默解出错误类型的bean（FND8-30）",
				keep.where, drop.where, ((KV)key).getKey(), ((KV)key).getValue());
	}

	// 遍历侧确定序（hist-01）：动态家族的注册遍历按决胜键（家族来源名where）排序后进行，
	// HashMap的identityHashCode桶序不参与任何注册结果。决胜唯一发生在putDynamicFamily
	// （字典序小者胜），本遍历只是按确定顺序注册各key的终胜者：同key胜者唯一；跨key的typeId
	// 互异（map/sortedMap按keyClass分桶，list全局唯一哨兵键），即使typeId发生跨key哈希碰撞，
	// 胜者也由where序决定——注册表终态与注册顺序都是schema的纯函数，跨JVM/重启恒定可复现。
	// dynamic外层pmapMeta（map2MetasDynamic）同口径接入（hist-01外层收口）：外层typeId按
	// (rowClass,colClass)分桶、值身份固定DynamicBean，同桶多家族由where决胜，与内层三容器
	// 共享本遍历的确定序保证。
	private static <K, F> ArrayList<Map.Entry<K, F>> sortedDynamic(
			@NotNull HashMap<K, F> families, @NotNull java.util.function.Function<F, String> where) {
		var list = new ArrayList<Map.Entry<K, F>>(families.entrySet());
		list.sort(Comparator.comparing(e -> where.apply(e.getValue())));
		return list;
	}

	// dynamic外层pmapMeta决胜（hist-01外层收口，FND26 H/M-1 + FND27 H1）：与putDynamicFamily
	// 同型——同(rowClass,colClass)键的外层pmapMeta按家族来源名字典序稳定决胜，小者胜，败者warn
	// 留痕。此前外层走map2Metas的HashSet身份序+Log.register先到先得：胜者随JVM运行翻转，
	// 回放端解码外层LogMap2的行物化工厂跨重启非确定（Bean:id重叠静默解错bean、不重叠毒卡
	// 游标），且两meta同名连warn都没有。ROOT方案（外层typeId含家族身份，需Gen+全量重生成，
	// 会改logTypeId使存量tHistory不可解码）另立专项；本收口先消灭"胜者挑选的非确定性"与
	// 检测面缺失，使回放成为schema的纯函数。
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void putDynamicOuterMeta(@NotNull HashMap families, @NotNull Object key,
											@NotNull Map2Meta<?, ? extends Bean> meta, @NotNull String where) {
		var family = new DynamicMetaFamily(meta, where);
		var exist = (DynamicMetaFamily)families.putIfAbsent(key, family);
		if (exist == null || exist.meta == meta)
			return; // 首个家族，或同meta重复登记：无冲突
		var keep = exist.where.compareTo(where) <= 0 ? exist : family;
		var drop = keep == exist ? family : exist;
		if (keep != exist)
			families.put(key, keep);
		logger.warn("dynamic gtable outer meta dropped: same log typeId with different value factory."
				+ " keep={} drop={} key=({},{})。胜者按家族来源名字典序稳定决胜（跨进程恒定，"
				+ "不由注册顺序决定）；回放端按胜者家族的工厂物化外层行，显式Bean:id编号重叠时"
				+ "将静默解出错误类型的bean",
				keep.where, drop.where, ((KV)key).getKey(), ((KV)key).getValue());
	}

	/** 跨批次dynamic家族决胜的进程级登记项：胜者工厂按对象身份跟踪（工厂闭包无equals语义）。 */
	private static final class DynamicWinner {
		final @NotNull IntFunction<Log> factory;
		final @NotNull String where;

		DynamicWinner(@NotNull IntFunction<Log> factory, @NotNull String where) {
			this.factory = factory;
			this.where = where;
		}
	}

	// dynamic家族跨批次决胜的进程级累积状态：typeId→当前胜者，与启动期applyRegistrations
	// 批内决胜同构——按家族来源名字典序取argmin，注册终态=schema的纯函数，跨批次/JVM/重启
	// 恒定（不走Log.register的putIfAbsent先到先得，否则增量开表的新家族工厂被静默丢弃、
	// 终态由注册时机决定）。登记点（Application启动与openDynamicTable）各自持Application
	// 锁，多Application实例并发时"查胜者+条件替换"仍须原子，静态锁串行化（频度启动/开表级）。
	private static final Object dynamicRegisterLock = new Object();
	private static final ConcurrentHashMap<Integer, DynamicWinner> dynamicWinners = new ConcurrentHashMap<>();

	private static void registerDynamicWinner(@NotNull IntFunction<Log> factory, @NotNull String where) {
		var typeId = factory.apply(0).getTypeId();
		synchronized (dynamicRegisterLock) {
			var cur = dynamicWinners.get(typeId);
			if (cur == null) {
				// 进程首见该typeId：正常注册。槽位已被非dynamic注册占用（跨家族typeId
				// 哈希碰撞，如普通List2Meta/Map2Meta先到）时不接管——维持先到先得，warn留痕。
				if (Log.getRegistered(typeId) != null) {
					logger.warn("dynamic collection family skipped: log typeId({}) already occupied by"
							+ " non-dynamic registration, first registered wins. where={}", typeId, where);
					return;
				}
				Log.register(factory);
				dynamicWinners.put(typeId, new DynamicWinner(factory, where));
				return;
			}
			if (cur.where.equals(where))
				return; // 同家族重复登记（多Application实例重扫同schema）：幂等，槽位保持。
			if (cur.where.compareTo(where) < 0) {
				// 存量胜者字典序更小：新到家族落败，不进注册表，warn留痕。
				logger.warn("dynamic collection family dropped (cross-batch): same log typeId({}) with"
						+ " different factories. keep={} drop={}", typeId, cur.where, where);
				return;
			}
			// 新到家族字典序更小：跨批翻转接管——仅当槽位仍是本表先前登记的工厂（按对象
			// 身份条件替换）时生效；被外来注册占用则不接管（防御分支，理论上不可达）。
			if (Log.replaceRegistered(typeId, cur.factory, factory)) {
				dynamicWinners.put(typeId, new DynamicWinner(factory, where));
				logger.warn("dynamic collection family replaced (cross-batch): same log typeId({}) with"
						+ " different factories. keep={} drop={}", typeId, where, cur.where);
			} else {
				logger.warn("dynamic collection family replace failed (cross-batch): log typeId({}) slot"
						+ " not held by previous winner, keep={} drop={}", typeId, cur.where, where);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsSortedMap(@NotNull Class<?> beanClass, @NotNull BVariable.Data v, @NotNull String keyType,
										@NotNull String valueType, @NotNull DependsResult result) throws Exception {
		var keyClass = (Class<? extends Comparable<?>>)getBuiltinBoxingClass(keyType);
		if (keyClass == null) {
			keyClass = (Class<? extends Comparable<?>>)Class.forName(keyType); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType); // bean or beanKey
			dependsBean(valueClass, result);
			// 选族须与生成器一致（IsNormalBean ? '2' : '1'）：beankey 值生成"1"族（PSortedMap1/LogSortedMap1）。
			if (BeanKey.class.isAssignableFrom(valueClass))
				result.sortedMap1.add(KV.create(keyClass, valueClass));
			else
				result.sortedMap2.add(KV.create(keyClass, (Class<? extends Bean>)valueClass));
		} else if (valueClass == DynamicBean.class) {
			putDynamicFamily(result.sortedMap2Dynamic, KV.create(keyClass, (Class<? extends Bean>)valueClass), beanClass, v);
		} else {
			result.sortedMap1.add(KV.create(keyClass, valueClass));
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsMap(@NotNull Class<?> beanClass, @NotNull BVariable.Data v, @NotNull String keyType,
								  @NotNull String valueType, @NotNull DependsResult result) throws Exception {
		var keyClass = getBuiltinBoxingClass(keyType);
		if (keyClass == null) {
			keyClass = Class.forName(keyType); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType); // bean or beankey
			dependsBean(valueClass, result);
			// 选族须与生成器一致（IsNormalBean ? '2' : '1'）：beankey 值生成"1"族（PMap1/LogMap1）。
			if (BeanKey.class.isAssignableFrom(valueClass))
				result.map1.add(KV.create(keyClass, valueClass));
			else
				result.map2.add(KV.create(keyClass, (Class<? extends Bean>)valueClass));
		} else if (valueClass == DynamicBean.class) {
			putDynamicFamily(result.map2Dynamic, KV.create(keyClass, (Class<? extends Bean>)valueClass), beanClass, v);
		} else {
			result.map1.add(KV.create(keyClass, valueClass));
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsGTable(@NotNull Class<?> beanClass,
									 @NotNull BVariable.Data v,
									 @NotNull String key1Type,
									 @NotNull String key2Type,
									 @NotNull String valueType,
									 @NotNull DependsResult result) throws Exception {
		var key1Class = getBuiltinBoxingClass(key1Type);
		if (key1Class == null) {
			key1Class = Class.forName(key1Type); // must be BeanKey.
			dependsBean(key1Class, result);
		}
		var key2Class = getBuiltinBoxingClass(key2Type);
		if (key2Class == null) {
			key2Class = Class.forName(key2Type); // must be BeanKey.
			dependsBean(key2Class, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType); // bean or beanKey
			dependsBean(valueClass, result);
			// 选族须与生成器一致（IsNormalBean ? '2' : '1'）：beankey 值生成 GTable1（内层 BeanMap1）。
			if (BeanKey.class.isAssignableFrom(valueClass)) {
				var factory = GTable1.getFactory(key1Class, key2Class, valueClass);
				result.map2Metas.add(factory.getPmapMeta());
				result.map1Metas.add(factory.getBmapMeta());
			} else {
				var factory = GTable2.getFactory(key1Class, key2Class, (Class<? extends Bean>)valueClass);
				result.map2Metas.add(factory.getPmapMeta());
				result.map2Metas.add(factory.getBmapMeta());
			}
		} else if (valueClass == DynamicBean.class) {
			// 先取每变量的get/create工厂，再经dynamic重载构建（三参版对DynamicBean必抛：无无参构造器）。
			var family = newDynamicFamily(beanClass, v);
			var factory = GTable2.getFactory(key1Class, key2Class, family.factories.getKey(), family.factories.getValue());
			// 外层pmapMeta走决胜登记（hist-01外层收口）：不进map2Metas——其HashSet身份序使
			// 同(row,col)多dynamic变量的胜者跨JVM翻转（见DependsResult.map2MetasDynamic注释）。
			putDynamicOuterMeta(result.map2MetasDynamic, KV.create(key1Class, key2Class),
					factory.getPmapMeta(), family.where);
			putDynamicFamily(result.map2Dynamic, KV.create(key2Class, (Class<? extends Bean>)valueClass), beanClass, v);
		} else {
			var factory = GTable1.getFactory(key1Class, key2Class, valueClass);
			result.map2Metas.add(factory.getPmapMeta());
			result.map1Metas.add(factory.getBmapMeta());
		}
	}

	public static void dependsSet(@NotNull String valueType, @NotNull DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType); // must be beanKey
			dependsBean(valueClass, result);
		}
		result.set1.add(valueClass);
	}

	public static @Nullable Class<?> getBuiltinBoxingClass(@NotNull String type) {
		return switch (type) {
			//@formatter:off
		case "bool"->Boolean.class;
		case "byte"->Byte.class;
		case "short"->Short.class;
		case "int"->Integer.class;
		case "long"->Long.class;
		case "float"->Float.class;
		case "double"->Double.class;
		case "binary"->Zeze.Net.Binary.class;
		case "string"->String.class;
		case "decimal"->BigDecimal.class;
		case "vector2"->Zeze.Serialize.Vector2.class;
		case "vector2int"->Zeze.Serialize.Vector2Int.class;
		case "vector3"->Zeze.Serialize.Vector3.class;
		case "vector3int"->Zeze.Serialize.Vector3Int.class;
		case "vector4"->Zeze.Serialize.Vector4.class;
		case "quaternion"->Zeze.Serialize.Quaternion.class;
		case "dynamic"->DynamicBean.class;
		default -> null;
		};
	}

	public static boolean isBuiltinType(@NotNull String type) {
		return getBuiltinBoxingClass(type) != null;
	}

	// 工厂闭包直接捕获meta：解码是热路径（follower复制/history回放），勿在lambda内重查XxxMeta.get。
	public static <T extends Serializable> void registerLogBeanKey(@NotNull Class<T> beanClass) {
		var meta = BeanKeyMeta.get(beanClass);
		Log.register(varId -> new LogBeanKey<>(varId, meta));
	}

	public static <T> void registerLogList1(@NotNull Class<T> valueClass) {
		var meta = List1Meta.get(valueClass);
		Log.register(varId -> new LogList1<>(null, varId, null, Empty.vector(), meta));
	}

	public static <V extends Bean> void registerLogList2(@NotNull Class<V> valueClass) {
		var meta = List2Meta.get(valueClass);
		Log.register(varId -> new LogList2<>(null, varId, null, Empty.vector(), meta));
	}

	public static void registerLogList2Dynamic(@NotNull ToLongFunction<Bean> get,
											   @NotNull LongFunction<Bean> create,
											   @NotNull String where) {
		var meta = List2Meta.createDynamic(get, create);
		registerDynamicWinner(varId -> new LogList2<>(null, varId, null, Empty.vector(), meta), where);
	}

	public static <K, V> void registerLogMap1(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var meta = Map1Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogMap1<>(null, varId, null, Empty.map(), meta));
	}

	public static <K, V extends Bean> void registerLogMap2(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var meta = Map2Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta));
	}

	public static <K> void registerLogMap2Dynamic(@NotNull Class<K> keyClass,
												  @NotNull ToLongFunction<Bean> get,
												  @NotNull LongFunction<Bean> create,
												  @NotNull String where) {
		var meta = Map2Meta.createDynamic(keyClass, get, create);
		registerDynamicWinner(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta), where);
	}

	public static <K, V> void registerLogMap1Meta(@NotNull Map1Meta<K, V> meta) {
		Log.register(varId -> new LogMap1<>(null, varId, null, Empty.map(), meta));
	}

	public static <K, V extends Bean> void registerLogMap2Meta(@NotNull Map2Meta<K, V> meta) {
		Log.register(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta));
	}

	/** dynamic GTable 外层 pmapMeta 注册：经进程级跨批次决胜，与普通 map2Metas 的先到先得分流。 */
	public static <K, V extends Bean> void registerLogMap2MetaDynamic(@NotNull Map2Meta<K, V> meta,
																	  @NotNull String where) {
		registerDynamicWinner(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta), where);
	}

	public static <V> void registerLogSet1(@NotNull Class<V> valueClass) {
		var meta = Set1Meta.get(valueClass);
		Log.register(varId -> new LogSet1<>(null, varId, null, Empty.set(), meta));
	}

	public static <V extends Bean> void registerLogOne(@NotNull Class<V> beanClass) {
		var meta = LogOneMeta.get(beanClass);
		Log.register(varId -> new LogOne<>(varId, meta));
	}

	public static <K extends Comparable<K>, V> void registerLogSortedMap1(@NotNull Class<K> keyClass,
																		  @NotNull Class<V> valueClass) {
		var meta = SortedMap1Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogSortedMap1<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>, V extends Bean> void registerLogSortedMap2(@NotNull Class<K> keyClass,
																					   @NotNull Class<V> valueClass) {
		var meta = SortedMap2Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogSortedMap2<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>> void registerLogSortedMap2Dynamic(@NotNull Class<K> keyClass,
																			  @NotNull ToLongFunction<Bean> get,
																			  @NotNull LongFunction<Bean> create,
																			  @NotNull String where) {
		var meta = SortedMap2Meta.createDynamic(keyClass, get, create);
		registerDynamicWinner(varId -> new LogSortedMap2<>(null, varId, null, Empty.sortedMap(), meta), where);
	}

	public static <K extends Comparable<K>, V> void registerLogSortedMap1Meta(@NotNull SortedMap1Meta<K, V> meta) {
		Log.register(varId -> new LogSortedMap1<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>, V extends Bean> void registerLogSortedMap2Meta(@NotNull SortedMap2Meta<K, V> meta) {
		Log.register(varId -> new LogSortedMap2<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static void registerLogs() {
		Log.register(LogBool::new);
		Log.register(LogByte::new);
		Log.register(LogShort::new);
		Log.register(LogInt::new);
		Log.register(LogLong::new);
		Log.register(LogFloat::new);
		Log.register(LogDouble::new);
		Log.register(LogBinary::new);
		Log.register(LogString::new);
		Log.register(LogDecimal::new);
		Log.register(LogVector2::new);
		Log.register(LogVector2Int::new);
		Log.register(LogVector3::new);
		Log.register(LogVector3Int::new);
		Log.register(LogVector4::new);
		Log.register(LogQuaternion::new);
		Log.register(varId -> new LogBean(null, varId, null));
		Log.register(varId -> new LogDynamic(null, varId, null));
	}
}
