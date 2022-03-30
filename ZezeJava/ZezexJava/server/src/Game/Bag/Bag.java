package Game.Bag;

import Zeze.Transaction.Collections.*;
import java.util.*;

public class Bag {
	private final long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private final BBag bag;

	public Bag(long roleId, BBag bag) {
		this.RoleId = roleId;
		this.bag = bag;
	}

	public final void SetCapacity(int capacity) {
		bag.setCapacity(capacity);
	}

	public final void SetMoney(long money) {
		bag.setMoney(money);
	}

	public final long GetMoney() {
		return bag.getMoney();
	}

	public final void AddOrDecMoney(long addOrDec) {
		bag.setMoney(bag.getMoney() + addOrDec);
	}

	/**
	 删除number数量的指定id物品。
	 warning: 如果返回false，表示物品不够。此时应该回滚事务，否则会部分删除。
	 由于逻辑调用删除物品都是为了使用，如果不够，使用失败，回滚事务是比较合理的。
	*/
	public final boolean Remove(int id, int number) {
		if (number <= 0) {
			throw new IllegalArgumentException();
		}

		for (var item : bag.getItems().entrySet()) {
			if (item.getValue().getId() == id) {
				if (item.getValue().getNumber() > number) {
					item.getValue().setNumber(item.getValue().getNumber() - number);
					return true;
				}
				number -= item.getValue().getNumber();
				bag.getItems().remove(item.getKey());
				if (number <= 0) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 优先删除 positionHint 指定的格子的物品。
	 游戏在某个格子上右键使用物品时，如果没有指定格子的信息，就会优先删除前面格子的物品，操作有一点点不大友好。
	*/
	public final boolean Remove(int positionHint, int id, int number) {
		if (number <= 0) {
			throw new IllegalArgumentException();
		}

		var bItem = bag.getItems().get(positionHint);
		if (null != bItem) {
			if (id != bItem.getId()) {
				return Remove(id, number);
			}

			if (bItem.getNumber() > number) {
				bItem.setNumber(bItem.getNumber() - number);
				return true;
			}
			number -= bItem.getNumber();
			bag.getItems().remove(positionHint);
			if (number <= 0) {
				return true;
			}
		}
		return Remove(id, number);
	}

	/**
	 加入简单物品，只有id和number
	*/
	public final int Add(int id, int number) {
		BItem tempVar = new BItem();
		tempVar.setId(id);
		tempVar.setNumber(number);
		return Add(-1, tempVar);
	}

	/**
	 加入物品：优先堆叠到已有的格子里面；然后如果很多，自动拆分。
	 失败处理：如果外面调用者在失败时回滚事务，那么所有的添加都会被回滚。
			   如果没有回滚，那么就会完成部分添加。此时返回剩余number，逻辑可能需要把剩余数量的物品转到其他系统（比如邮件中）。
			   另外如果想回滚全部添加，但是又不回滚整个事务，应该使用嵌套事务。
			   在嵌套事务中尝试添加，失败的话回滚嵌套事务，然后继续把所有物品转到其他系统。
	 */
	public final int Add(int positionHint, BItem itemAdd) {
		if (itemAdd.getNumber() <= 0) {
			throw new IllegalArgumentException();
		}

		int pileMax = GetItemPileMax(itemAdd.getId());

		// 优先加到提示格子
		if (positionHint >= 0 && positionHint < bag.getCapacity()) {
			var bItemHint = bag.getItems().get(positionHint);
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
			}
			else {
				bag.getItems().put(positionHint, itemAdd); // in managed
				if (itemAdd.getNumber() <= pileMax) {
					return 0;
				}

				int remain = itemAdd.getNumber() - pileMax;
				itemAdd.setNumber(pileMax);
				itemAdd = itemAdd.Copy(); // current itemAdd has in mananged.
				itemAdd.setNumber(remain);
				// ready to continue add
			}
		}

		for (var item : bag.getItems().entrySet()) {
			if (item.getValue().getId() == itemAdd.getId()) {
				int numberNew = item.getValue().getNumber() + itemAdd.getNumber();
				if (numberNew > pileMax) {
					item.getValue().setNumber(pileMax);
					itemAdd.setNumber(numberNew - pileMax);
					continue;
				}
				item.getValue().setNumber(numberNew);
				return 0; // all pile done
			}
		}
		while (itemAdd.getNumber() > pileMax) {
			int pos = GetEmptyPosition();
			if (pos == -1) {
				return itemAdd.getNumber();
			}

			BItem itemNew = itemAdd.Copy();
			itemNew.setNumber(pileMax);
			itemAdd.setNumber(itemAdd.getNumber() - pileMax);
			bag.getItems().put(pos, itemNew);
		}
		if (itemAdd.getNumber() > 0) {
			int pos = GetEmptyPosition();
			if (pos == -1) {
				return itemAdd.getNumber();
			}
			bag.getItems().put(pos, itemAdd);
		}
		return 0;
	}

	private int GetEmptyPosition() {
		for (int pos = 0; pos < bag.getCapacity(); ++pos) {
			var exist = bag.getItems().get(pos);
			if (null == exist)
				return pos;
		}
		return -1;
	}

	private int GetItemPileMax(int itemId) {
		return 99; // TODO load from config
	}

	/**
	 移动物品，从一个格子移动到另一个格子。实现功能：移动，交换，叠加，拆分。

	 @param number -1 表示尽量移动所有的
	*/
	public final int Move(int from, int to, int number) {
		// validate parameter
		if (from < 0 || from >= bag.getCapacity()) {
			return ModuleBag.ResultCodeFromInvalid;
		}

		if (to < 0 || to >= bag.getCapacity()) {
			return ModuleBag.ResultCodeToInvalid;
		}

		var itemFrom = bag.getItems().get(from);
		if (null == itemFrom)
			return ModuleBag.ResultCodeFromNotExist;

		if (number < 0 || number > itemFrom.getNumber()) {
			number = itemFrom.getNumber(); // move all
		}

		int pileMax = GetItemPileMax(itemFrom.getId());
		var itemTo = bag.getItems().get(to);
		if (null != itemTo) {
			if (itemFrom.getId() != itemTo.getId()) {
				if (number < itemFrom.getNumber()) {
					// 试图拆分，但是目标已经存在不同物品
					return ModuleBag.ResultCodeTrySplitButTargetExistDifferenceItem;
				}

				// 交换
				BItem.Swap(itemFrom, itemTo);
				return 0;
			}
			// 叠加（或拆分）
			int numberToWill = itemTo.getNumber() + number;
			if (numberToWill > pileMax) {
				itemTo.setNumber(pileMax);
				itemFrom.setNumber(numberToWill - pileMax);
			}
			else {
				itemTo.setNumber(numberToWill);
				bag.getItems().remove(from);
			}
			return 0;
		}
		// 移动（或拆分）
		BItem itemNew = itemFrom.Copy(); // 先复制一份再设置成目标数量。
		itemNew.setNumber(number);
		if (itemFrom.getNumber() == number) {
			// 移动
			bag.getItems().remove(from);
			bag.getItems().put(to, itemNew);
			return 0;
		}
		// 拆分
		itemFrom.setNumber(itemFrom.getNumber() - number);
		bag.getItems().put(to, itemNew);
		return 0;
	}

	public final int Destory(int from) {
		bag.getItems().remove(from);
		return 0;
	}

	public final void Sort(Comparator<Map.Entry<Integer, BItem>> comparison) {
		if (null == comparison)
			comparison = Comparator.comparingLong(x -> x.getValue().getId());

		@SuppressWarnings("unchecked")
		var sort = (Map.Entry<Integer, BItem>[])bag.getItems().entrySet().toArray(new Map.Entry[bag.getItems().size()]);
		Arrays.sort(sort, comparison);
		for (int i = 0; i < sort.length; ++i) {
			BItem copy = sort[i].getValue().Copy();
			sort[i] = Map.entry(i, copy); // old item IsManaged. need Copy a new one.
		}
		bag.getItems().clear();
		var sortMap = new HashMap<Integer, BItem>();
		for (var s : sort)
			sortMap.put(s.getKey(), s.getValue());
		bag.getItems().putAll(sortMap); // use AddRange for performance
	}

	// warning. 暴露了内部数据。可以用来实现一些不是通用的方法。
	public final PMap2<Integer, Game.Bag.BItem> getItems() {
		return bag.getItems();
	}

	public final void ToProtocol(BBag b) {
		ToProtocol(bag, b);
	}

	public static void ToProtocol(BBag bag, BBag b) {
		b.setMoney(bag.getMoney());
		b.setCapacity(bag.getCapacity());
		b.getItems().putAll(bag.getItems()); // 只能临时引用一下。发完协议就丢弃，不能保存。
	}

	public final Game.Item.Item GetItem(int position) {
		var bItem = bag.getItems().get(position);
		if (null != bItem) {
			final var extryTypeId = bItem.getExtra().getTypeId();
			if (extryTypeId == Game.Item.BFoodExtra.TYPEID)
				return new Game.Item.Food(bItem, (Game.Item.BFoodExtra)bItem.getExtra().getBean());
			if (extryTypeId == Game.Item.BHorseExtra.TYPEID)
				return new Game.Item.Horse(bItem, (Game.Item.BHorseExtra)bItem.getExtra().getBean());
			if (extryTypeId == Game.Equip.BEquipExtra.TYPEID)
				return new Game.Equip.Equip(bItem, (Game.Equip.BEquipExtra)bItem.getExtra().getBean());
			throw new RuntimeException("unknown extra");
		}
		return null;
	}
}
