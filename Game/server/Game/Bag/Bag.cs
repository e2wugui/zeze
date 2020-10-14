using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Text;
using Microsoft.VisualBasic.CompilerServices;

namespace Game.Bag
{
    public class Bag
    {
        public long RoleId { get; }
        private BBag bag;

        public Bag(long roleId, BBag bag)
        {
            this.RoleId = roleId;
            this.bag = bag;
        }

        public void SetCapacity(int capacity)
        {
            bag.Capacity = capacity;
        }

        public void SetMoney(long money)
        {
            bag.Money = money;
        }

        public long GetMoney()
        {
            return bag.Money;
        }

        public void AddOrDecMoney(long addOrDec)
        {
            bag.Money += addOrDec;
        }

        /// <summary>
        /// 删除number数量的指定id物品。
        /// warning: 如果返回false，表示物品不够。此时应该回滚事务，否则会部分删除。
        /// 由于逻辑调用删除物品都是为了使用，如果不够，使用失败，回滚事务是比较合理的。
        /// </summary>
        /// <param name="id"></param>
        /// <param name="number"></param>
        /// <returns></returns>
        public bool Remove(int id, int number)
        {
            if (number <= 0)
                throw new ArgumentException();

            foreach (var item in bag.Items)
            {
                if (item.Value.Id == id)
                {
                    if (item.Value.Number > number)
                    {
                        item.Value.Number -= number;
                        return true;
                    }
                    number -= item.Value.Number;
                    bag.Items.Remove(item.Key);
                    if (number <= 0)
                        return true;
                }
            }
            return false;
        }

        /// <summary>
        /// 优先删除 positionHint 指定的格子的物品。
        /// 游戏在某个格子上右键使用物品时，如果没有指定格子的信息，就会优先删除前面格子的物品，操作有一点点不大友好。
        /// </summary>
        /// <param name="positionHint"></param>
        /// <param name="id"></param>
        /// <param name="number"></param>
        /// <returns></returns>
        public bool Remove(int positionHint, int id, int number)
        {
            if (number <= 0)
                throw new ArgumentException();

            if (bag.Items.TryGetValue(positionHint, out var bItem))
            {
                if (id != bItem.Id)
                    return Remove(id, number);

                if (bItem.Number > number)
                {
                    bItem.Number -= number;
                    return true;
                }
                number -= bItem.Number;
                bag.Items.Remove(positionHint);
                if (number <= 0)
                    return true;
            }
            return Remove(id, number);
        }

        /// <summary>
        /// 加入简单物品，只有id和number
        /// </summary>
        /// <param name="id"></param>
        /// <param name="number"></param>
        public int Add(int id, int number)
        {
            return Add(-1, new BItem() { Id = id, Number = number });
        }

        /// <summary>
        /// 加入物品：优先堆叠到已有的格子里面；然后如果很多，自动拆分。
        /// 失败处理：如果外面调用者在失败时回滚事务，那么所有的添加都会被回滚。
        ///           如果没有回滚，那么就会完成部分添加。此时返回剩余number，逻辑可能需要把剩余数量的物品转到其他系统（比如邮件中）。
        ///           另外如果想回滚全部添加，但是又不回滚整个事务，应该使用嵌套事务。
        ///           在嵌套事务中尝试添加，失败的话回滚嵌套事务，然后继续把所有物品转到其他系统。
        /// </summary>
        /// <param name="item"></param>
        public int Add(int positionHint, BItem itemAdd)
        {
            if (itemAdd.Number <= 0)
                throw new ArgumentException();

            int pileMax = GetItemPileMax(itemAdd.Id);

            // 优先加到提示格子
            if (positionHint >= 0 && positionHint < bag.Capacity)
            {
                if (bag.Items.TryGetValue(positionHint, out var bItemHint))
                {
                    if (bItemHint.Id == itemAdd.Id)
                    {
                        int numberNew = bItemHint.Number + itemAdd.Number;
                        if (numberNew <= pileMax)
                        {
                            bItemHint.Number = numberNew;
                            return 0; // all pile done
                        }
                        bItemHint.Number = pileMax;
                        itemAdd.Number = numberNew - pileMax;
                        // continue to add
                    }
                    // continue to add
                }
                else
                {
                    bag.Items.Add(positionHint, itemAdd); // in managed
                    if (itemAdd.Number <= pileMax)
                    {
                        return 0;
                    }

                    int remain = itemAdd.Number - pileMax;
                    itemAdd.Number = pileMax;
                    itemAdd = itemAdd.Copy(); // current itemAdd has in mananged.
                    itemAdd.Number = remain;
                    // ready to continue add
                }
            }

            foreach (var item in bag.Items)
            {
                if (item.Value.Id == itemAdd.Id)
                {
                    int numberNew = item.Value.Number + itemAdd.Number;
                    if (numberNew > pileMax)
                    {
                        item.Value.Number = pileMax;
                        itemAdd.Number = numberNew - pileMax;
                        continue;
                    }
                    item.Value.Number = numberNew;
                    return 0; // all pile done
                }
            }
            while (itemAdd.Number > pileMax)
            {
                int pos = GetEmptyPosition();
                if (pos == -1)
                    return itemAdd.Number;

                BItem itemNew = itemAdd.Copy();
                itemNew.Number = pileMax;
                itemAdd.Number -= pileMax;
                bag.Items.Add(pos, itemNew);
            }
            if (itemAdd.Number > 0)
            {
                int pos = GetEmptyPosition();
                if (pos == -1)
                    return itemAdd.Number;
                bag.Items.Add(pos, itemAdd);
            }
            return 0;
        }

        private int GetEmptyPosition()
        {
            for (int pos = 0; pos < bag.Capacity; ++pos)
            {
                if (false == bag.Items.TryGetValue(pos, out var _))
                    return pos;
            }
            return -1;
        }

        private int GetItemPileMax(int itemId)
        {
            return 99; // TODO load from config
        }

        /// <summary>
        /// 移动物品，从一个格子移动到另一个格子。实现功能：移动，交换，叠加，拆分。
        /// </summary>
        /// <param name="from"></param>
        /// <param name="to"></param>
        /// <param name="number">-1 表示尽量移动所有的</param>
        public void Move(int from, int to, int number)
        {
            BItem itemFrom;
            if (false == bag.Items.TryGetValue(from, out itemFrom))
                return;

            // validate parameter
            if (from < 0 || from >= bag.Capacity)
                return;

            if (to < 0 || to >= bag.Capacity)
                return;

            if (number < 0 || number > itemFrom.Number)
                number = itemFrom.Number; // move all

            int pileMax = GetItemPileMax(itemFrom.Id);
            if (bag.Items.TryGetValue(to, out var itemTo))
            { 
                if (itemFrom.Id != itemTo.Id)
                {
                    if (number < itemFrom.Number)
                        return; // 试图拆分，但是目标已经存在不同物品
                    // 交换
                    BItem.Swap(itemFrom, itemTo);
                    return;
                }
                // 叠加（或拆分）
                int numberToWill = itemTo.Number + number;
                if (numberToWill > pileMax)
                {
                    itemTo.Number = pileMax;
                    itemFrom.Number = numberToWill - pileMax;
                }
                else
                {
                    itemTo.Number = numberToWill;
                    bag.Items.Remove(from);
                }
                return;
            }
            // 移动（或拆分）
            BItem itemNew = itemFrom.Copy(); // 先复制一份再设置成目标数量。
            itemNew.Number = number;
            if (itemFrom.Number == number)
            {
                // 移动
                bag.Items.Remove(from);
                bag.Items.Add(to, itemNew);
                return;
            }
            // 拆分
            itemFrom.Number -= number;
            bag.Items.Add(to, itemNew);
        }

        public void Destory(int from)
        {
            bag.Items.Remove(from);
        }

        public void Sort()
        {
            Sort((x, y) => x.Value.Id.CompareTo(y.Value.Id)); // sort by item.Id
        }

        public void Sort(Comparison<KeyValuePair<int, BItem>> comparison)
        {
            KeyValuePair<int, BItem> [] sort = bag.Items.ToArray();
            Array.Sort(sort, comparison);
            for (int i = 0; i < sort.Length; ++i)
            {
                BItem copy = sort[i].Value.Copy();
                sort[i] = KeyValuePair.Create(i, copy); // old item IsManaged. need Copy a new one.
            }
            bag.Items.Clear();
            bag.Items.AddRange(sort); // use AddRange for performence
        }

        // warning. 暴露了内部数据。可以用来实现一些不是通用的方法。
        public Zeze.Transaction.Collections.PMap2<int, Game.Bag.BItem> Items => bag.Items;

        public Game.Item.Item GetItem(int position)
        {
            if (bag.Items.TryGetValue(position, out var bItem))
            {
                Zeze.Transaction.Bean dynamicBean = bItem.Extra;
                switch (dynamicBean.TypeId)
                {
                    case Item.BFoodExtra.TYPEID: return new Item.Food(bItem, (Item.BFoodExtra)dynamicBean);
                    case Item.BHorseExtra.TYPEID: return new Item.Horse(bItem, (Item.BHorseExtra)dynamicBean);
                    case Equip.BEquipExtra.TYPEID: return new Equip.Equip(bItem, (Equip.BEquipExtra)dynamicBean);
                    default:
                        throw new System.Exception("unknown extra");
                }
            }
            return null;
        }
    }
}
