using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public abstract class Item
    {
        protected Game.Bag.BItem bItem; // bItem.Position: 根据所在容器不同，含义可能不一样：比如在包裹中是格子号，在装备中就是装备位置。

        public Item(Game.Bag.BItem bItem)
        {
            this.bItem = bItem;
        }

        public int Id => bItem.Id;

        public abstract bool Use();

        // 物品提示信息格式化。TODO 需要定义通用的 Tip 结构
        public virtual string FormatTip()
        {
            return "";
        }

        public virtual void CalculateFighter(Game.Fight.Fighter fighter)
        { 
        }

        // 其他更多操作？
    }
}
