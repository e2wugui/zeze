using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Serialize
{
    public interface Serializable
    {
        public void Decode(ByteBuffer bb);
        public void Encode(ByteBuffer bb);
    }
}
