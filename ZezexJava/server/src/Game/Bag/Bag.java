package Game.Bag;

import Zeze.Transaction.*;
import Zeze.Transaction.Collections.*;
import Game.*;
import java.util.*;

public class Bag {
	private long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private BBag bag;

	public Bag(long roleId, BBag bag) {
		this.RoleId = roleId;
		this.bag = bag;
	}

	public final void SetCapacity(int capacity) {
		bag.Capacity = capacity;
	}

	public final void SetMoney(long money) {
		bag.Money = money;
	}

	public final long GetMoney() {
		return bag.getMoney();
	}

	public final void AddOrDecMoney(long addOrDec) {
		bag.Money += addOrDec;
	}

	/** 
	 删除number数量的指定id物品。
	 warning: 如果返回false，表示物品不够。此时应该回滚事务，否则会部分删除。
	 由于逻辑调用删除物品都是为了使用，如果不够，使用失败，回滚事务是比较合理的。
	 
	 @param id
	 @param number
	 @return 
	*/
	public final boolean Remove(int id, int number) {
		if (number <= 0) {
			throw new IllegalArgumentException();
		}

		for (var item : bag.getItems()) {
			if (item.Value.Id == id) {
				if (item.Value.Number > number) {
					item.Value.Number -= number;
					return true;
				}
				number -= item.Value.Number;
				bag.getItems().Remove(item.Key);
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
	 
	 @param positionHint
	 @param id
	 @param number
	 @return 
	*/
	public final boolean Remove(int positionHint, int id, int number) {
		if (number <= 0) {
			throw new IllegalArgumentException();
		}

		V bItem;
		tangible.OutObject<V> tempOut_bItem = new tangible.OutObject<V>();
		if (bag.getItems().TryGetValue(positionHint, tempOut_bItem)) {
		bItem = tempOut_bItem.outArgValue;
			if (id != bItem.Id) {
				return Remove(id, number);
			}

			if (bItem.Number > number) {
				bItem.Number -= number;
				return true;
			}
			number -= bItem.Number;
			bag.getItems().Remove(positionHint);
			if (number <= 0) {
				return true;
			}
		}
	else {
		bItem = tempOut_bItem.outArgValue;
	}
		return Remove(id, number);
	}

	/** 
	 加入简单物品，只有id和number
	 
	 @param id
	 @param number
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
	 
	 @param item
	*/
	public final int Add(int positionHint, BItem itemAdd) {
		if (itemAdd.getNumber() <= 0) {
			throw new IllegalArgumentException();
		}

		int pileMax = GetItemPileMax(itemAdd.getId());

		// 优先加到提示格子
		if (positionHint >= 0 && positionHint < bag.getCapacity()) {
			V bItemHint;
			tangible.OutObject<V> tempOut_bItemHint = new tangible.OutObject<V>();
			if (bag.getItems().TryGetValue(positionHint, tempOut_bItemHint)) {
			bItemHint = tempOut_bItemHint.outArgValue;
				if (bItemHint.Id == itemAdd.getId()) {
					int numberNew = bItemHint.Number + itemAdd.getNumber();
					if (numberNew <= pileMax) {
						bItemHint.Number = numberNew;
						return 0; // all pile done
					}
					bItemHint.Number = pileMax;
					itemAdd.Number = numberNew - pileMax;
					// continue to add
				}
				// continue to add
			}
			else {
			bItemHint = tempOut_bItemHint.outArgValue;
				bag.getItems().Add(positionHint, itemAdd); // in managed
				if (itemAdd.getNumber() <= pileMax) {
					return 0;
				}

				int remain = itemAdd.getNumber() - pileMax;
				itemAdd.Number = pileMax;
				itemAdd = itemAdd.Copy(); // current itemAdd has in mananged.
				itemAdd.Number = remain;
				// ready to continue add
			}
		}

		for (var item : bag.getItems()) {
			if (item.Value.Id == itemAdd.getId()) {
				int numberNew = item.Value.Number + itemAdd.getNumber();
				if (numberNew > pileMax) {
					item.Value.Number = pileMax;
					itemAdd.Number = numberNew - pileMax;
					continue;
				}
				item.Value.Number = numberNew;
				return 0; // all pile done
			}
		}
		while (itemAdd.getNumber() > pileMax) {
			int pos = GetEmptyPosition();
			if (pos == -1) {
				return itemAdd.getNumber();
			}

			BItem itemNew = itemAdd.Copy();
			itemNew.Number = pileMax;
			itemAdd.Number -= pileMax;
			bag.getItems().Add(pos, itemNew);
		}
		if (itemAdd.getNumber() > 0) {
			int pos = GetEmptyPosition();
			if (pos == -1) {
				return itemAdd.getNumber();
			}
			bag.getItems().Add(pos, itemAdd);
		}
		return 0;
	}

	private int GetEmptyPosition() {
		for (int pos = 0; pos < bag.getCapacity(); ++pos) {
			V _;
			tangible.OutObject<V> tempOut__ = new tangible.OutObject<V>();
			if (false == bag.getItems().TryGetValue(pos, tempOut__)) {
			_ = tempOut__.outArgValue;
				return pos;
			}
		else {
			_ = tempOut__.outArgValue;
		}
		}
		return -1;
	}

	private int GetItemPileMax(int itemId) {
		return 99; // TODO load from config
	}

	/** 
	 移动物品，从一个格子移动到另一个格子。实现功能：移动，交换，叠加，拆分。
	 
	 @param from
	 @param to
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

		BItem itemFrom;
		tangible.OutObject<Game.Bag.BItem> tempOut_itemFrom = new tangible.OutObject<Game.Bag.BItem>();
		if (false == bag.getItems().TryGetValue(from, tempOut_itemFrom)) {
		itemFrom = tempOut_itemFrom.outArgValue;
			return ModuleBag.ResultCodeFromNotExsit;
		}
	else {
		itemFrom = tempOut_itemFrom.outArgValue;
	}

		if (number < 0 || number > itemFrom.getNumber()) {
			number = itemFrom.getNumber(); // move all
		}

		int pileMax = GetItemPileMax(itemFrom.getId());
		V itemTo;
		tangible.OutObject<V> tempOut_itemTo = new tangible.OutObject<V>();
		if (bag.getItems().TryGetValue(to, tempOut_itemTo)) {
		itemTo = tempOut_itemTo.outArgValue;
			if (itemFrom.getId() != itemTo.Id) {
				if (number < itemFrom.getNumber()) {
					// 试图拆分，但是目标已经存在不同物品
					return ModuleBag.ResultCodeTrySplitButTargetExsitDifferenceItem;
				}

				// 交换
				BItem.Swap(itemFrom, itemTo);
				return 0;
			}
			// 叠加（或拆分）
			int numberToWill = itemTo.Number + number;
			if (numberToWill > pileMax) {
				itemTo.Number = pileMax;
				itemFrom.Number = numberToWill - pileMax;
			}
			else {
				itemTo.Number = numberToWill;
				bag.getItems().Remove(from);
			}
			return 0;
		}
	else {
		itemTo = tempOut_itemTo.outArgValue;
	}
		// 移动（或拆分）
		BItem itemNew = itemFrom.Copy(); // 先复制一份再设置成目标数量。
		itemNew.Number = number;
		if (itemFrom.getNumber() == number) {
			// 移动
			bag.getItems().Remove(from);
			bag.getItems().Add(to, itemNew);
			return 0;
		}
		// 拆分
		itemFrom.Number -= number;
		bag.getItems().Add(to, itemNew);
		return 0;
	}

	public final int Destory(int from) {
		bag.getItems().Remove(from);
		return 0;
	}

	public final void Sort() {
		Sort((x, y) -> x.Value.Id.CompareTo(y.Value.Id)); // sort by item.Id
	}

	public final void Sort(Comparison<Map.Entry<Integer, BItem>> comparison) {
		Map.Entry<Integer, BItem> [] sort = bag.getItems().ToArray();
		Arrays.sort(sort, comparison);
		for (int i = 0; i < sort.length; ++i) {
			BItem copy = sort[i].getValue().Copy();
			sort[i] = KeyValuePair.Create(i, copy); // old item IsManaged. need Copy a new one.
		}
		bag.getItems().Clear();
		bag.getItems().AddRange(sort); // use AddRange for performence
	}

	// warning. 暴露了内部数据。可以用来实现一些不是通用的方法。
	public final PMap2<Integer, Game.Bag.BItem> getItems() {
		return bag.getItems();
	}

	public final void ToProtocol(BBag b) {
		ToProtocol(bag, b);
	}

	public static void ToProtocol(BBag bag, BBag b) {
		b.Money = bag.getMoney();
		b.Capacity = bag.getCapacity();
		b.getItems().AddRange(bag.getItems()); // 只能临时引用一下。发完协议就丢弃，不能保存。
	}

	public final Game.Item.Item GetItem(int position) {
		V bItem;
		tangible.OutObject<V> tempOut_bItem = new tangible.OutObject<V>();
		if (bag.getItems().TryGetValue(position, tempOut_bItem)) {
		bItem = tempOut_bItem.outArgValue;
			switch (bItem.Extra.TypeId) {
				case Item.BFoodExtra.TYPEID:
					return new Item.Food(bItem, (Item.BFoodExtra)bItem.Extra.Bean);
				case Item.BHorseExtra.TYPEID:
					return new Item.Horse(bItem, (Item.BHorseExtra)bItem.Extra.Bean);
				case Equip.BEquipExtra.TYPEID:
					return new Equip.Equip(bItem, (Equip.BEquipExtra)bItem.Extra.Bean);
				default:
					throw new RuntimeException("unknown extra");
			}
		}
	else {
		bItem = tempOut_bItem.outArgValue;
	}
		return null;
	}
}