using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Serialize
{
    public static class Helper
    {
        public static ByteBuffer Encode(Serializable sa)
        {
            ByteBuffer bb = new ByteBuffer();
            sa.Encode(bb);
            return bb;
        }
    }
}
