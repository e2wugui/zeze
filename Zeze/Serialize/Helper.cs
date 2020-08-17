﻿using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Serialize
{
    public static class Helper
    {
        // 只能增加新的类型定义，增加时记得同步 SkipUnknownField
        public const int
        INT = 0,
        LONG = 1,
        STRING = 2,
        BOOL = 3,
        BYTE = 4,
        SHORT = 5,
        FLOAT = 6,
        DOUBLE = 7,
        BYTES = 8,
        LIST = 9,
        SET = 10,
        MAP = 11,
        BEAN = 12,
        TAG_MAX = 31;

        public const int TAG_SHIFT = 5;
        public const int TAG_MASK = (1 << TAG_SHIFT) - 1;
        /*
        public const int ID_MASK = (1 << (31 - TAG_SHIFT)) - 1;

        // 在生成代码的时候使用这个方法检查。生成后的代码不使用这个方法。
        // 可以定义的最大 Variable.Id 为 Zeze.Transaction.Bean.MaxVariableId
        public static int MakeTagId(int tag, int id)
        {
            if (tag < 0 || tag > TAG_MAX)
                throw new OverflowException("tag < 0 || tag > TAG_MAX");
            if (id < 0 || id > ID_MASK)
                throw new OverflowException("id < 0 || id > ID_MASK");

            return (id << TAG_SHIFT) | tag;
        }

        public static int GetTag(int tagid)
        {
            return tagid & TAG_MASK;
        }

        public static int GetId(int tagid)
        {
            return (tagid >> TAG_SHIFT) & ID_MASK;
        }
        */

        public static void VerifyArrayIndex(byte[] bytes, int offset, int length)
        {
            if (offset < 0 || offset > bytes.Length)
                throw new Exception();
            int endindex = offset + length;
            if (endindex < 0 || endindex > bytes.Length)
                throw new Exception();
            if (offset > endindex)
                throw new Exception();
        }

        public static ByteBuffer Encode(Serializable sa)
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            sa.Encode(bb);
            return bb;
        }

        public static void SkipUnknownField(int tagid, ByteBuffer bb)
        {
            int tagType = tagid & TAG_MASK;
            switch (tagType)
            {
                case BOOL: bb.ReadBool(); break;
                case BYTE: bb.ReadByte(); break;
                case SHORT: bb.ReadShort(); break;
                case INT: bb.ReadInt(); break;
                case LONG: bb.ReadLong(); break;
                case FLOAT: bb.ReadFloat(); break;
                case DOUBLE: bb.ReadDouble(); break;
                case STRING: case BYTES: case LIST: case SET: case MAP: case BEAN: bb.SkipBytes(); break;
                default:
                    throw new Exception("SkipUnknownField");
            }
        }

        public static byte[] Bytes4 = new byte[4]; // 不用每次都new

        public static void BuildString<T>(StringBuilder sb, IEnumerable<T> c)
        {
            sb.Append("[");
            foreach (var e in c)
            {
                sb.Append(e);
            }
            sb.Append("]");
        }


        public static void BuildString<TK, TV>(StringBuilder sb, IDictionary<TK, TV> dic)
        {
            sb.Append("{");
            foreach (var e in dic)
            {
                sb.Append(e.Key).Append(':');
                sb.Append(e.Value).Append(',');
            }
            sb.Append('}');
        }
    }
}
