package Zeze.Game;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Bag.BBag;
import Zeze.Builtin.Game.Bag.BBagKey;
import Zeze.Builtin.Game.Bag.BItem;
import Zeze.Builtin.Game.Bag.tbag;
import Zeze.Collections.BeanFactory;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Util.Action1;
import Zeze.Util.ConcurrentHashSet;
import org.jspecify.annotations.Nullable;

/**
 * 游戏背包：按格子管理物品的堆叠、拆分、移动与排序，配套内建 Move/Destroy 协议模块。
 */
public class Bag {
	// 物品加入包裹时，自动注册；
	// 注册的Bean.ClassName会被持久化保存下来。
	// Module.Start的时候自动装载注册的ClassName。
	private static final BeanFactory beanFactory = new BeanFactory();
	// 追踪当前已向 HotManager 注册的活跃 Bag.Module，供静态 Module.register(Bean) 转发热更模块登记。
	private static final ConcurrentHashSet<Module> hotRegisteredModules = new ConcurrentHashSet<>();

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	private final Module module;

	Bag(Module module, BBag bean, String bagName) {
		this.module = module;
		this.name = bagName;
		this.bean = bean;
	}

	private final String name;
	private final BBag bean;

	public String getName() {
		return name;
	}

	public BBag getBean() {
		return bean;
	}

	public int getCapacity() {
		return bean.getCapacity();
	}

	public void setCapacity(int capacity) {
		bean.setCapacity(capacity);
	}

	/**
	 * 删除number数量的指定id物品。
	 * warning: 如果返回false，表示物品不够。此时应该回滚事务，否则会部分删除。
	 * 由于逻辑调用删除物品都是为了使用，如果不够，使用失败，回滚事务是比较合理的。
	 */
	public final boolean remove(int id, int number) {
		if (number <= 0) {
			throw new IllegalArgumentException();
		}

		for (var item : bean.getItems().entrySet()) {
			if (item.getValue().getId() == id) {
				if (item.getValue().getNumber() > number) {
					item.getValue().setNumber(item.getValue().getNumber() - number);
					return true;
				}
				number -= item.getValue().getNumber();
				bean.getItems().remove(item.getKey());
				if (number <= 0) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 优先删除 positionHint 指定的格子的物品。
	 * 游戏在某个格子上右键使用物品时，如果没有指定格子的信息，就会优先删除前面格子的物品，操作有一点点不大友好。
	 */
	public final boolean remove(int positionHint, int id, int number) {
		if (number <= 0) {
			throw new IllegalArgumentException();
		}

		var bItem = bean.getItems().get(positionHint);
		if (null != bItem) {
			if (id != bItem.getId()) {
				return remove(id, number);
			}

			if (bItem.getNumber() > number) {
				bItem.setNumber(bItem.getNumber() - number);
				return true;
			}
			number -= bItem.getNumber();
			bean.getItems().remove(positionHint);
			if (number <= 0) {
				return true;
			}
		}
		return remove(id, number);
	}

	/**
	 * 加入简单物品，只有id和number
	 */
	public final int add(int id, int number) {
		BItem tempVar = new BItem();
		tempVar.setId(id);
		tempVar.setNumber(number);
		return add(-1, tempVar);
	}

	/**
	 * 加入物品：优先堆叠到已有的格子里面；然后如果很多，自动拆分。
	 * 失败处理：如果外面调用者在失败时回滚事务，那么所有的添加都会被回滚。
	 * 如果没有回滚，那么就会完成部分添加。此时返回剩余number，逻辑可能需要把剩余数量的物品转到其他系统（比如邮件中）。
	 * 另外如果想回滚全部添加，但是又不回滚整个事务，应该使用嵌套事务。
	 * 在嵌套事务中尝试添加，失败的话回滚嵌套事务，然后继续把所有物品转到其他系统。
	 */
	public final int add(int positionHint, BItem itemAdd) {
		if (itemAdd.getNumber() <= 0) {
			throw new IllegalArgumentException();
		}
		// 自动注册加入的BItem.Item的class。
		Module.register(itemAdd.getItem().getBean());

		int pileMax = module.getItemPileMax(itemAdd.getId());

		// 优先加到提示格子
		if (positionHint >= 0 && positionHint < bean.getCapacity()) {
			var bItemHint = bean.getItems().get(positionHint);
			if (null != bItemHint) {
				if (bItemHint.getId() == itemAdd.getId()) {
					int remaining = stackInto(bItemHint, itemAdd.getNumber(), pileMax);
					if (remaining == 0) {
						return 0; // all pile done
					}
					itemAdd.setNumber(remaining);
					// continue to add
				}
				// continue to add
			} else {
				bean.getItems().put(positionHint, itemAdd); // in managed
				if (itemAdd.getNumber() <= pileMax) {
					return 0;
				}

				int remain = itemAdd.getNumber() - pileMax;
				itemAdd.setNumber(pileMax);
				itemAdd = itemAdd.copy(); // current itemAdd has in managed.
				itemAdd.setNumber(remain);
				// ready to continue add
			}
		}

		for (var item : bean.getItems().values()) {
			if (item.getId() == itemAdd.getId()) {
				int remaining = stackInto(item, itemAdd.getNumber(), pileMax);
				if (remaining > 0) {
					itemAdd.setNumber(remaining);
					continue;
				}
				return 0; // all pile done
			}
		}
		while (itemAdd.getNumber() > pileMax) {
			int pos = getEmptyPosition();
			if (pos == -1) {
				return itemAdd.getNumber();
			}

			BItem itemNew = itemAdd.copy();
			itemNew.setNumber(pileMax);
			itemAdd.setNumber(itemAdd.getNumber() - pileMax);
			bean.getItems().put(pos, itemNew);
		}
		if (itemAdd.getNumber() > 0) {
			int pos = getEmptyPosition();
			if (pos == -1) {
				return itemAdd.getNumber();
			}
			bean.getItems().put(pos, itemAdd);
		}
		return 0;
	}

	// 旧堆可能超过当前上限（例如堆叠配置下调），保留旧堆并让其停止接收新物品。
	private static int stackInto(BItem target, int number, int pileMax) {
		int current = target.getNumber();
		if (current >= pileMax)
			return number;
		int added = (int)Math.min(number, (long)pileMax - current);
		target.setNumber(current + added);
		return number - added;
	}

	private int getEmptyPosition() {
		for (int pos = 0; pos < bean.getCapacity(); ++pos) {
			var exist = bean.getItems().get(pos);
			if (null == exist)
				return pos;
		}
		return -1;
	}

	/**
	 * 移动物品，从一个格子移动到另一个格子。实现功能：移动，交换，叠加，拆分。
	 *
	 * @param number -1 表示尽量移动所有的；0 拒绝（ResultCodeNumberInvalid）
	 */
	public final int move(int from, int to, int number) {
		// validate parameter
		if (from < 0 || from >= bean.getCapacity()) {
			return Module.ResultCodeFromInvalid;
		}

		if (to < 0 || to >= bean.getCapacity()) {
			return Module.ResultCodeToInvalid;
		}

		var itemFrom = bean.getItems().get(from);
		if (null == itemFrom)
			return Module.ResultCodeFromNotExist;

		// number==0：钳制只修正<0与>from，0原样穿透到空格拆分路径会写入数量0的物品
		// 占格并污染持久化（对齐getItemPileMax对number下界的防护判例）。-1=移动全部
		// 是协议契约（solution.zeze.xml BMove），不在拒绝范围。
		if (number == 0)
			return Module.ResultCodeNumberInvalid;

		if (number < 0 || number > itemFrom.getNumber()) {
			number = itemFrom.getNumber(); // move all
		}

		int pileMax = module.getItemPileMax(itemFrom.getId());
		if (from == to)
			return 0;
		var itemTo = bean.getItems().get(to);
		if (null != itemTo) {
			if (itemFrom.getId() != itemTo.getId()) {
				if (number < itemFrom.getNumber()) {
					// 试图拆分，但是目标已经存在不同物品
					return Module.ResultCodeTrySplitButTargetExistDifferenceItem;
				}

				// 交换
				BItem.swap(itemFrom, itemTo);
				return 0;
			}
			// 叠加（或拆分）
			int remaining = stackInto(itemTo, number, pileMax);
			int remainNum = itemFrom.getNumber() - number + remaining;
			if (remainNum > 0)
				itemFrom.setNumber(remainNum);
			else
				bean.getItems().remove(from);
			return 0;
		}
		// 移动（或拆分）
		BItem itemNew = itemFrom.copy(); // 先复制一份再设置成目标数量。
		itemNew.setNumber(number);
		if (itemFrom.getNumber() == number) {
			// 移动
			bean.getItems().remove(from);
			bean.getItems().put(to, itemNew);
			return 0;
		}
		// 拆分
		itemFrom.setNumber(itemFrom.getNumber() - number);
		bean.getItems().put(to, itemNew);
		return 0;
	}

	public final int destroy(int from) {
		bean.getItems().remove(from);
		return 0;
	}

	public final void sort(@Nullable Comparator<Map.Entry<Integer, BItem>> comparison) {
		if (null == comparison)
			comparison = Comparator.comparingLong(x -> x.getValue().getId());

		@SuppressWarnings("unchecked")
		var sort = (Map.Entry<Integer, BItem>[])bean.getItems().entrySet().toArray(new Map.Entry[0]);
		Arrays.sort(sort, comparison);
		for (int i = 0; i < sort.length; ++i) {
			BItem copy = sort[i].getValue().copy();
			sort[i] = Map.entry(i, copy); // old item IsManaged. need Copy a new one.
		}
		bean.getItems().clear();
		var sortMap = new HashMap<Integer, BItem>();
		for (var s : sort)
			sortMap.put(s.getKey(), s.getValue());
		bean.getItems().putAll(sortMap); // use AddRange for performance
	}

	public static class Module extends AbstractBag implements HotBeanFactory {
		public @Nullable ProviderApp providerApp;
		public final Application zeze;
		public volatile @Nullable IntUnaryOperator funcItemPileMax;

		private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
		private final AtomicBoolean freshStopModuleDynamic = new AtomicBoolean();
		// this::方法引用每次求值都产生新实例，而 ConcurrentHashSet 按实例判等：
		// 固定为实例字段，保证 stopEvents 与 beanFactory watch 的登记幂等、注销真正生效。
		private final Action1<HotModule> onHotModuleStopRef = this::onHotModuleStop;
		private final Consumer<Class<?>> tryRecordHotModuleRef = this::tryRecordHotModule;
		private volatile @Nullable HotManager registeredHotManager;

		// 根据物品Id查询物品堆叠数量。不设置这个方法，全部max==1，都不能堆叠。

		// 用于UserApp服务，可以处理客户端发送的协议。
		public Module(ProviderApp pa, @Nullable IntUnaryOperator funcItemPileMax) {
			providerApp = pa;
			zeze = providerApp.zeze;
			RegisterProtocols(providerApp.providerService);
			RegisterZezeTables(zeze);
			providerApp.builtinModules.put(this.getFullName(), this);
			this.funcItemPileMax = funcItemPileMax;
			registerHot();
		}

		public int getItemPileMax(int itemId) {
			var tmp = funcItemPileMax;
			if (null == tmp)
				return 1;
			// 下界1：自定义函数返回0/负数（配置缺项映射成0是常见写法）时，
			// add的堆叠循环条件number>pileMax恒真且每次扣减0，会把背包全部空格
			// 填成number=0的物品堆并污染持久化数据。
			return Math.max(1, tmp.applyAsInt(itemId));
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public void UnRegister() {
			unregisterHot();
			if (null != providerApp) {
				UnRegisterProtocols(providerApp.providerService);
			}
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		@Override
		public void UnRegisterZezeTables(Application zeze) {
			unregisterHot();
			super.UnRegisterZezeTables(zeze);
		}

		// 用于数据测试，测试不支持协议。
		public Module(Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
			registerHot();
		}

		private void registerHot() {
			lock();
			try {
				var hotManager = zeze.getHotManager();
				if (hotManager == null || registeredHotManager == hotManager)
					return;
				if (registeredHotManager != null)
					unregisterHotLocked();
				// 先发布 registeredHotManager 再开放 watch / hotRegisteredModules 入口，
				// 避免并发观察者提前回调时因读到 null 而漏记 HotModule。
				registeredHotManager = hotManager;
				hotManager.addHotBeanFactory(this);
				beanFactory.registerWatch(tryRecordHotModuleRef);
				hotRegisteredModules.add(this);
			} finally {
				unlock();
			}
		}

		private void unregisterHot() {
			lock();
			try {
				unregisterHotLocked();
			} finally {
				unlock();
			}
		}

		private void unregisterHotLocked() {
			var hotManager = registeredHotManager;
			registeredHotManager = null;
			hotRegisteredModules.remove(this);
			beanFactory.unregisterWatch(tryRecordHotModuleRef);
			if (hotManager != null)
				hotManager.removeHotBeanFactory(this);
			for (var hotModule : hotModulesHaveDynamic) {
				if (hotModulesHaveDynamic.remove(hotModule) != null)
					hotModule.stopEvents.remove(onHotModuleStopRef);
			}
			freshStopModuleDynamic.set(false);
		}

		private static boolean belongsToHotManager(HotModule hotModule, @Nullable HotManager hotManager) {
			if (hotManager == null)
				return false;
			var parent = hotModule.getParent();
			return !(parent instanceof HotManager owner) || owner == hotManager;
		}

		private void onHotModuleStop(HotModule hot) {
			if (hotModulesHaveDynamic.remove(hot) != null)
				freshStopModuleDynamic.set(true);
		}

		private void tryRecordHotModule(Class<?> customClass) {
			var cl = customClass.getClassLoader();
			if (!HotManager.isHotModule(cl))
				return;
			var hotModule = (HotModule)cl;
			if (!belongsToHotManager(hotModule, registeredHotManager) || hotModulesHaveDynamic.contains(hotModule))
				return;
			lock();
			try {
				if (!belongsToHotManager(hotModule, registeredHotManager) || hotModulesHaveDynamic.contains(hotModule))
					return;
				// 先入 hotModulesHaveDynamic 再挂 stopEvents，保证 stopInternal 回调到达时必能命中集合并置位。
				hotModulesHaveDynamic.add(hotModule);
				hotModule.stopEvents.add(onHotModuleStopRef);
			} finally {
				unlock();
			}
		}

		@Override
		public void clearTableCache() {
			_tbag.__ClearTableCacheUnsafe__();
		}

		@Override
		public BeanFactory beanFactory() {
			return beanFactory;
		}

		@Override
		public boolean hasFreshStopModuleDynamicOnce() {
			return freshStopModuleDynamic.getAndSet(false);
		}

		@Override
		public void processWithNewClasses(List<Class<?>> newClasses) {
			for (var cls : newClasses)
				tryRecordHotModule(cls);
		}

		public tbag getTable() {
			return _tbag;
		}

		// 需要在事务内使用。
		// 使用完不要保存。
		public Bag open(long roleId, String bagName) {
			var b = _tbag.getOrAdd(new BBagKey(roleId, bagName));
			return new Bag(this, b, bagName);
		}

		// 需要在事务内使用。非建行：行不存在返回null。
		// 供内建协议(Move/Destroy)使用——bagName虽来自客户端载荷，但表键roleId取自会话，
		// 寻址被限制在本角色分区内（跨角色越权随BBagKey改造在键结构上关闭）；
		// 不建行是因为destroy对空行恒成功，getOrAdd会让任意bagName的空行随事务提交落盘。
		public @Nullable Bag openOrNull(long roleId, String bagName) {
			var b = _tbag.get(new BBagKey(roleId, bagName));
			return null != b ? new Bag(this, b, bagName) : null;
		}

		public static void register(Bean bean) {
			beanFactory.register(bean);
			var cls = bean.getClass();
			if (HotManager.isHotModule(cls.getClassLoader())) {
				for (var module : hotRegisteredModules)
					module.tryRecordHotModule(cls);
			}
		}

		/**
		 * 启动背包模块：若已装配 {@link HotManager}，则向其注册 {@link HotBeanFactory} 并监听动态道具类加载。
		 * 幂等，支持 {@link #stop()} 后重新启动；未启用 {@link HotManager} 时零额外开销。
		 */
		public void start() {
			if (null != providerApp)
				providerApp.builtinModules.put(this.getFullName(), this);
			registerHot();
		}

		public void start(Application zeze) {
			start();
		}

		/**
		 * 停止背包模块：对称注销 {@link HotManager}、{@link BeanFactory} 监听及已订阅的 {@link HotModule#stopEvents}。
		 */
		public void stop() {
			unregisterHot();
		}

		@Override
		protected long ProcessDestroyRequest(Zeze.Builtin.Game.Bag.Destroy r) {
			var session = ProviderUserSession.get(r);
			var roleId = session.getRoleId();
			if (null == roleId)
				return errorCode(ResultCodeNotLogin);
			var bag = openOrNull(roleId, r.Argument.getBagName());
			if (null == bag)
				return errorCode(ResultCodeBagNotExist);
			var moduleCode = bag.destroy(r.Argument.getPosition());
			if (0 != moduleCode) {
				return errorCode(moduleCode);
			}
			session.respond(r);
			return 0;
		}

		@Override
		protected long ProcessMoveRequest(Zeze.Builtin.Game.Bag.Move r) {
			var session = ProviderUserSession.get(r);
			var roleId = session.getRoleId();
			if (null == roleId)
				return errorCode(ResultCodeNotLogin);
			var bag = openOrNull(roleId, r.Argument.getBagName());
			if (null == bag)
				return errorCode(ResultCodeBagNotExist);
			var moduleCode = bag.move(
					r.Argument.getPositionFrom(), r.Argument.getPositionTo(), r.Argument.getNumber());
			if (moduleCode != 0) {
				return errorCode(moduleCode);
			}
			session.respond(r);
			return 0;
		}
	}
}
