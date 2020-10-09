using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public abstract class Item
    {
        protected int position; // 根据所在容器不同，含义可能不一样：比如在包裹中是格子号，在装备中就是装备位置。
        protected Game.Bag.BItem bItem;

        public Item(int position, Game.Bag.BItem bItem)
        {
            this.position = position;
            this.bItem = bItem;
        }

        public int Id => bItem.Id;

        public abstract bool Use();

        // 物品提示信息格式化。TODO 需要定义通用的 Tip 结构
        public virtual string FormatTip()
        {
            return "";
        }

        // 其他更多操作？
    }
}
