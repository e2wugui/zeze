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
        private BBag bag;

        public Bag(long roleid, tbag table)
        {
            bag = table.GetOrAdd(roleid);
        }

        public class Change
        { 

        }

        /// <summary>
        /// 加入简单物品，只有id和number
        /// </summary>
        /// <param name="id"></param>
        /// <param name="number"></param>
        public void Add(int id, int number)
        {
            Add(id, number, -1, 0);
        }

        /// <summary>
        /// 加入物品
        /// </summary>
        /// <param name="id"></param>
        /// <param name="number"></param>
        /// <param name="type"></param>
        /// <param name="extrakey"></param>
        public void Add(int id, int number, int type, long extrakey)
        {
            Add(new BItem() { Id = id, Number = number, Type = type, Extrakey = extrakey });
        }

        /// <summary>
        /// 加入物品
        /// </summary>
        /// <param name="item"></param>
        public void Add(BItem item)
        {
            
        }

        /// <summary>
        /// 移动物品，从一个格子移动到另一个格子。实现功能：移动，交换，叠加，拆分。
        /// </summary>
        /// <param name="from"></param>
        /// <param name="to"></param>
        /// <param name="number">-1 表示尽量移动所有的</param>
        public void Move(int from, int to, int number, BChangedResult changed)
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

            int pileMax = 99; // TODO GetItemPileMax(itemFrom.Id)
            if (bag.Items.TryGetValue(to, out var itemTo))
            { 
                if (itemFrom.Id != itemTo.Id)
                {
                    if (number < itemFrom.Number)
                        return; // 试图拆分，但是目标已经存在不同物品
                    // 交换
                    BItem.Swap(itemFrom, itemTo);
                    changed.ItemsReplace.Add(from, itemFrom);
                    changed.ItemsReplace.Add(to, itemTo);
                    return;
                }
                // 叠加（或拆分）
                int numberToWill = itemTo.Number + number;
                if (numberToWill > pileMax)
                {
                    itemTo.Number = pileMax;
                    itemFrom.Number = numberToWill - pileMax;
                    changed.ItemsReplace.Add(from, itemFrom);
                    changed.ItemsReplace.Add(to, itemTo);
                }
                else
                {
                    itemTo.Number = numberToWill;
                    bag.Items.Remove(from);
                    changed.ItemsRemove.Add(from);
                    changed.ItemsReplace.Add(to, itemTo);
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
                changed.ItemsRemove.Add(from);
                changed.ItemsReplace.Add(to, itemNew);
                return;
            }
            // 拆分
            itemFrom.Number -= number;
            bag.Items.Add(to, itemNew);
            changed.ItemsReplace.Add(from, itemFrom);
            changed.ItemsReplace.Add(to, itemNew);
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
                sort[i] = KeyValuePair.Create(i, sort[i].Value.Copy()); // old item IsManaged. need Copy a new one.
            bag.Items.Clear();
            bag.Items.AddRange(sort); // use AddRange for performence
        }

        // warning. 暴露了内部数据。
        public Zeze.Transaction.Collections.PMap2<int, Game.Bag.BItem> Items => bag.Items;
    }
}
