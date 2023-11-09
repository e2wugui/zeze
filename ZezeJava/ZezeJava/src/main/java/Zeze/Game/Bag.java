package Zeze.Game;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Bag.BBag;
import Zeze.Builtin.Game.Bag.BItem;
import Zeze.Builtin.Game.Bag.tbag;
import Zeze.Collections.BeanFactory;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

public class Bag {
	// 物品加入包裹时，自动注册；
	// 注册的Bean.ClassName会被持久化保存下来。
	// Module.Start的时候自动装载注册的ClassName。
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	private final Module module;

	Bag(Module module, String bagName) {
		this.module = module;
		this.name = bagName;
		this.bean = module._tbag.getOrAdd(bagName);
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
					int numberNew = bItemHint.getNumber() + itemAdd.getNumber();
					if (numberNew <= pileMax) {
						bItemHint.setNumber(numberNew);
						return 0; // all pile done
					}
					bItemHint.setNumber(pileMax);
					itemAdd.setNumber(numberNew - pileMax);
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
				int numberNew = item.getNumber() + itemAdd.getNumber();
				if (numberNew > pileMax) {
					item.setNumber(pileMax);
					itemAdd.setNumber(numberNew - pileMax);
					continue;
				}
				item.setNumber(numberNew);
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
	 * @param number -1 表示尽量移动所有的
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

		if (number < 0 || number > itemFrom.getNumber()) {
			number = itemFrom.getNumber(); // move all
		}

		int pileMax = module.getItemPileMax(itemFrom.getId());
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
			int numberToWill = itemTo.getNumber() + number;
			if (numberToWill > pileMax) {
				itemTo.setNumber(pileMax);
				itemFrom.setNumber(itemFrom.getNumber() - number + (numberToWill - pileMax));
			} else {
				itemTo.setNumber(numberToWill);
				var remainNum = itemFrom.getNumber() - number;
				if (remainNum > 0)
					itemFrom.setNumber(remainNum);
				else
					bean.getItems().remove(from);
			}
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

	public final void sort(Comparator<Map.Entry<Integer, BItem>> comparison) {
		if (null == comparison)
			comparison = Comparator.comparingLong(x -> x.getValue().getId());

		@SuppressWarnings("unchecked")
		var sort = (Map.Entry<Integer, BItem>[])bean.getItems().entrySet().toArray(new Map.Entry[bean.getItems().size()]);
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

	public static class Module extends AbstractBag {
		private final ConcurrentHashMap<String, Bag> bags = new ConcurrentHashMap<>();
		public ProviderApp providerApp;
		public final Application zeze;
		public volatile IntUnaryOperator funcItemPileMax;

		// 根据物品Id查询物品堆叠数量。不设置这个方法，全部max==1，都不能堆叠。

		// 用于UserApp服务，可以处理客户端发送的协议。
		public Module(ProviderApp pa, IntUnaryOperator funcItemPileMax) {
			providerApp = pa;
			zeze = providerApp.zeze;
			RegisterProtocols(providerApp.providerService);
			RegisterZezeTables(zeze);
			providerApp.builtinModules.put(this.getFullName(), this);
			this.funcItemPileMax = funcItemPileMax;
		}

		public int getItemPileMax(int itemId) {
			var tmp = funcItemPileMax;
			if (null == tmp)
				return 1;
			return tmp.applyAsInt(itemId);
		}

		@Override
		public void UnRegister() {
			if (null != providerApp) {
				UnRegisterProtocols(providerApp.providerService);
			}
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		// 用于数据测试，测试不支持协议。
		public Module(Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		public tbag getTable() {
			return _tbag;
		}

		// 需要在事务内使用。
		// 使用完不要保存。
		public Bag open(String bagName) {
			return bags.computeIfAbsent(bagName, key -> new Bag(this, bagName));
		}

		public static void register(Bean bean) {
			beanFactory.register(bean);
		}

		public void start(Application zeze) {
			providerApp.builtinModules.put(this.getFullName(), this);
		}

		public void stop() {
		}

		@Override
		protected long ProcessDestroyRequest(Zeze.Builtin.Game.Bag.Destroy r) {
			var session = ProviderUserSession.get(r);
			var moduleCode = open(r.Argument.getBagName()).destroy(r.Argument.getPosition());
			if (0 != moduleCode) {
				return errorCode(moduleCode);
			}
			session.sendResponseWhileCommit(r);
			return 0;
		}

		@Override
		protected long ProcessMoveRequest(Zeze.Builtin.Game.Bag.Move r) {
			var session = ProviderUserSession.get(r);
			// throw exception if not login
			var moduleCode = open(r.Argument.getBagName()).move(
					r.Argument.getPositionFrom(), r.Argument.getPositionTo(), r.Argument.getNumber());
			if (moduleCode != 0) {
				return errorCode(moduleCode);
			}
			session.sendResponseWhileCommit(r);
			return 0;
		}
	}
}
