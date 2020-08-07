using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Types
{
    public class Bean : Type
    {
        public static void SkipUnknownField(int tagid, Zeze.Serialize.ByteBuffer bb)
        {
            int tagType = tagid & Type.TAG_MASK;
            switch (tagType)
            {
                case Type.BOOL: bb.ReadBool(); break;
                case Type.BYTE: bb.ReadByte(); break;
                case Type.SHORT: bb.ReadShort(); break;
                case Type.INT: bb.ReadInt(); break;
                case Type.LONG: bb.ReadLong(); break;
                case Type.FLOAT: bb.ReadFloat(); break;
                case Type.DOUBLE: bb.ReadDouble(); break;

                case Type.STRING:
                case Type.BYTES:
                case Type.LIST:
                case Type.SET:
                case Type.MAP:
                case Type.BEAN:
                    bb.SkipBytes();
                    break;

                default:
                    throw new Exception("SkipUnknownField");
            }
        }

    }
}
