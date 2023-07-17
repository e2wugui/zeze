using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.World;
using Zeze.Serialize;

namespace Zeze.World
{
    public class Entity
    {
        public long EntityId { get; }

        public Entity(long entityId)
        {
            EntityId = entityId;
        }

        internal void ProcessOperete(BAoiOperate operate)
        {
            // 先直接使用服务器数据结构的定义。
            var full = new BObject();
            full.Decode(ByteBuffer.Wrap(operate.Param));
        }
    }
}
