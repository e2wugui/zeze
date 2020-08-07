using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Serialize
{
    public interface Serializable
    {
        public void Decode(ByteBuffer bb);
        public void Encode(ByteBuffer bb);
 
        public ByteBuffer Encode()
        {
            ByteBuffer bb = new ByteBuffer();
            Encode(bb);
            return bb;
        }
     }
}
