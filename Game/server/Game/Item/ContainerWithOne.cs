using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    /// <summary>
    /// 当存在多个不同类型的物品容器时，需要抽象。
    /// 这个接口用来封装一个物品和相关容器信息。
    /// 这个抽象要求：不管物品存储在哪里，都需要提供Id和Extra。
    /// </summary>
    public interface ContainerWithOne
    {
        public int ItemId { get; }

        public bool Remove(int number);

        /// <summary>
        /// 把存贮在容器中的当前物品映射到具体的物品类。
        /// 由于不同的容器支持的Extra不一样，所以这个方法由容器模块具体实现。
        /// </summary>
        /// <returns></returns>
        public Item ToItem();
    }
}
