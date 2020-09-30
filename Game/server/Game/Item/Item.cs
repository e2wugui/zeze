using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public abstract class Item
    {
        protected ContainerWithOne container;

        public Item(ContainerWithOne container)
        {
            this.container = container;
        }

        public int Id => container.ItemId;

        public abstract void Use();

        // 物品提示信息格式化。TODO 需要定义通用的 Tip 结构
        public virtual string FormatTip()
        {
            return "";
        }

        // 其他更多操作？
    }
}
