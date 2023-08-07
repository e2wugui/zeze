using System.Collections.Concurrent;
using System.Collections.Generic;
#if UNITY_2017_1_OR_NEWER
using Zeze.Serialize;
#endif

namespace Zeze.Util
{
    public static class ConcurrentDictionaryExtension
    {
        // .NET 4.x 不支持这个接口，先实现一个不太安全的
        public static bool TryRemove<TKey, TValue>(this ConcurrentDictionary<TKey, TValue> map,
            KeyValuePair<TKey, TValue> pair)
        {
            for (;;)
            {
                if (!map.TryGetValue(pair.Key, out var v1) || !v1.Equals(pair.Value))
                    return false;
                if (!map.TryRemove(pair.Key, out var v2))
                    return false;
                if (v2.Equals(pair.Value))
                    return true;
                if (map.TryAdd(pair.Key, v2))
                    return false;
            }
        }
    }

#if UNITY_2017_1_OR_NEWER
    public static class UnityVectorHelper
    {
        public static void Encode(UnityEngine.Vector2 v, ByteBuffer bb)
        {
            if (v != null)
            {
                bb.WriteFloat(v.x);
                bb.WriteFloat(v.y);
            }
            else
            {
                bb.WriteFloat(0);
                bb.WriteFloat(0);
            }
        }

        public static void Encode(UnityEngine.Vector2Int v, ByteBuffer bb)
        {
            if (v != null)
            {
                bb.WriteInt(v.x);
                bb.WriteInt(v.y);
            }
            else
            {
                bb.WriteByte(0);
                bb.WriteByte(0);
            }
        }

        public static void Encode(UnityEngine.Vector3 v, ByteBuffer bb)
        {
            if (v != null)
            {
                bb.WriteFloat(v.x);
                bb.WriteFloat(v.y);
                bb.WriteFloat(v.z);
            }
            else
            {
                bb.WriteFloat(0);
                bb.WriteFloat(0);
                bb.WriteFloat(0);
            }
        }

        public static void Encode(UnityEngine.Vector3Int v, ByteBuffer bb)
        {
            if (v != null)
            {
                bb.WriteInt(v.x);
                bb.WriteInt(v.y);
                bb.WriteInt(v.z);
            }
            else
            {
                bb.WriteByte(0);
                bb.WriteByte(0);
                bb.WriteByte(0);
            }
        }

        public static void Encode(UnityEngine.Vector4 v, ByteBuffer bb)
        {
            if (v != null)
            {
                bb.WriteFloat(v.x);
                bb.WriteFloat(v.y);
                bb.WriteFloat(v.z);
                bb.WriteFloat(v.w);
            }
            else
            {
                bb.WriteFloat(0);
                bb.WriteFloat(0);
                bb.WriteFloat(0);
                bb.WriteFloat(0);
            }
        }

        public static void Encode(UnityEngine.Quaternion v, ByteBuffer bb)
        {
            if (v != null)
            {
                bb.WriteFloat(v.x);
                bb.WriteFloat(v.y);
                bb.WriteFloat(v.z);
                bb.WriteFloat(v.w);
            }
            else
            {
                bb.WriteFloat(0);
                bb.WriteFloat(0);
                bb.WriteFloat(0);
                bb.WriteFloat(0);
            }
        }

        public static UnityEngine.Vector2 ReadVector2(ByteBuffer bb)
        {
            return new UnityEngine.Vector2
            {
                x = bb.ReadFloat(),
                y = bb.ReadFloat()
            };
        }

        public static UnityEngine.Vector2Int ReadVector2Int(ByteBuffer bb)
        {
            return new UnityEngine.Vector2Int
            {
                x = bb.ReadInt(),
                y = bb.ReadInt()
            };
        }

        public static UnityEngine.Vector3 ReadVector3(ByteBuffer bb)
        {
            return new UnityEngine.Vector3
            {
                x = bb.ReadFloat(),
                y = bb.ReadFloat(),
                z = bb.ReadFloat()
            };
        }

        public static UnityEngine.Vector3Int ReadVector3Int(ByteBuffer bb)
        {
            return new UnityEngine.Vector3Int
            {
                x = bb.ReadInt(),
                y = bb.ReadInt(),
                z = bb.ReadInt()
            };
        }

        public static UnityEngine.Vector4 ReadVector4(ByteBuffer bb)
        {
            return new UnityEngine.Vector4
            {
                x = bb.ReadFloat(),
                y = bb.ReadFloat(),
                z = bb.ReadFloat(),
                w = bb.ReadFloat()
            };
        }

        public static UnityEngine.Quaternion ReadQuaternion(ByteBuffer bb)
        {
            return new UnityEngine.Quaternion
            {
                x = bb.ReadFloat(),
                y = bb.ReadFloat(),
                z = bb.ReadFloat(),
                w = bb.ReadFloat()
            };
        }

#pragma warning disable CS0162 // Unreachable code detected
        public static UnityEngine.Vector2 ReadVector2(ByteBuffer bb, int tag)
        {
            int type = tag & ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.VECTOR2)
                return ReadVector2(bb);
            if (type == ByteBuffer.VECTOR3)
            {
                var v = ReadVector2(bb);
                bb.ReadFloat();
                return v;
            }
            if (type == ByteBuffer.VECTOR4)
            {
                var v = ReadVector2(bb);
                bb.ReadFloat();
                bb.ReadFloat();
                return v;
            }
            if (type == ByteBuffer.VECTOR2INT)
            {
                int x = bb.ReadInt();
                int y = bb.ReadInt();
                return new UnityEngine.Vector2(x, y);
            }
            if (type == ByteBuffer.VECTOR3INT)
            {
                int x = bb.ReadInt();
                int y = bb.ReadInt();
                bb.ReadInt();
                return new UnityEngine.Vector2(x, y);
            }
            if (type == ByteBuffer.FLOAT)
                return new UnityEngine.Vector2(bb.ReadFloat(), 0);
            if (type == ByteBuffer.DOUBLE)
                return new UnityEngine.Vector2((float)bb.ReadDouble(), 0);
            if (type == ByteBuffer.INTEGER)
                return new UnityEngine.Vector2(bb.ReadLong(), 0);
            if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
            {
                bb.SkipUnknownField(tag);
                return new UnityEngine.Vector2();
            }
            throw new Exception("can not ReadVector2 for type=" + type);
        }

        public static UnityEngine.Vector2Int ReadVector2Int(ByteBuffer bb, int tag)
        {
            int type = tag & ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.VECTOR2INT)
                return ReadVector2Int(bb);
            if (type == ByteBuffer.VECTOR3INT)
            {
                var v = ReadVector2Int(bb);
                bb.ReadInt();
                return v;
            }
            if (type == ByteBuffer.VECTOR2)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                return new UnityEngine.Vector2Int((int)x, (int)y);
            }
            if (type == ByteBuffer.VECTOR3)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                bb.ReadFloat();
                return new UnityEngine.Vector2Int((int)x, (int)y);
            }
            if (type == ByteBuffer.VECTOR4)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                bb.ReadFloat();
                bb.ReadFloat();
                return new UnityEngine.Vector2Int((int)x, (int)y);
            }
            if (type == ByteBuffer.INTEGER)
                return new UnityEngine.Vector2Int(bb.ReadInt(), 0);
            if (type == ByteBuffer.FLOAT)
                return new UnityEngine.Vector2Int((int)bb.ReadFloat(), 0);
            if (type == ByteBuffer.DOUBLE)
                return new UnityEngine.Vector2Int((int)bb.ReadDouble(), 0);
            if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
            {
                bb.SkipUnknownField(tag);
                return new UnityEngine.Vector2Int();
            }
            throw new Exception("can not ReadVector2Int for type=" + type);
        }

        public static UnityEngine.Vector3 ReadVector3(ByteBuffer bb, int tag)
        {
            int type = tag & ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.VECTOR3)
                return ReadVector3(bb);
            if (type == ByteBuffer.VECTOR2)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                return new UnityEngine.Vector3(x, y, 0);
            }
            if (type == ByteBuffer.VECTOR4)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                var z = bb.ReadFloat();
                bb.ReadFloat();
                return new UnityEngine.Vector3(x, y, z);
            }
            if (type == ByteBuffer.VECTOR3INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                var z = bb.ReadInt();
                return new UnityEngine.Vector3(x, y, z);
            }
            if (type == ByteBuffer.VECTOR2INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                return new UnityEngine.Vector3(x, y, 0);
            }
            if (type == ByteBuffer.FLOAT)
                return new UnityEngine.Vector3(bb.ReadFloat(), 0, 0);
            if (type == ByteBuffer.DOUBLE)
                return new UnityEngine.Vector3((float)bb.ReadDouble(), 0, 0);
            if (type == ByteBuffer.INTEGER)
                return new UnityEngine.Vector3(bb.ReadLong(), 0, 0);
            if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
            {
                bb.SkipUnknownField(tag);
                return new UnityEngine.Vector3();
            }
            throw new Exception("can not ReadVector3 for type=" + type);
        }

        public static UnityEngine.Vector3Int ReadVector3Int(ByteBuffer bb, int tag)
        {
            int type = tag & ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.VECTOR3INT)
                return ReadVector3Int(bb);
            if (type == ByteBuffer.VECTOR2INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                return new UnityEngine.Vector3Int(x, y, 0);
            }
            if (type == ByteBuffer.VECTOR3)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                var z = bb.ReadFloat();
                return new UnityEngine.Vector3Int((int)x, (int)y, (int)z);
            }
            if (type == ByteBuffer.VECTOR2)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                return new UnityEngine.Vector3Int((int)x, (int)y, 0);
            }
            if (type == ByteBuffer.VECTOR4)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                var z = bb.ReadFloat();
                bb.ReadFloat();
                return new UnityEngine.Vector3Int((int)x, (int)y, (int)z);
            }
            if (type == ByteBuffer.INTEGER)
                return new UnityEngine.Vector3Int(bb.ReadInt(), 0, 0);
            if (type == ByteBuffer.FLOAT)
                return new UnityEngine.Vector3Int((int)bb.ReadFloat(), 0, 0);
            if (type == ByteBuffer.DOUBLE)
                return new UnityEngine.Vector3Int((int)bb.ReadDouble(), 0, 0);
            if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
            {
                bb.SkipUnknownField(tag);
                return new UnityEngine.Vector3Int();
            }
            throw new Exception("can not ReadVector3Int for type=" + type);
        }

        public static UnityEngine.Vector4 ReadVector4(ByteBuffer bb, int tag)
        {
            int type = tag & ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.VECTOR4)
                return ReadVector4(bb);
            if (type == ByteBuffer.VECTOR3)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                var z = bb.ReadFloat();
                return new UnityEngine.Vector4(x, y, z, 0);
            }
            if (type == ByteBuffer.VECTOR2)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                return new UnityEngine.Vector4(x, y, 0, 0);
            }
            if (type == ByteBuffer.VECTOR3INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                var z = bb.ReadInt();
                return new UnityEngine.Vector4(x, y, z, 0);
            }
            if (type == ByteBuffer.VECTOR2INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                return new UnityEngine.Vector4(x, y, 0, 0);
            }
            if (type == ByteBuffer.FLOAT)
                return new UnityEngine.Vector4(bb.ReadFloat(), 0, 0, 0);
            if (type == ByteBuffer.DOUBLE)
                return new UnityEngine.Vector4((float)bb.ReadDouble(), 0, 0, 0);
            if (type == ByteBuffer.INTEGER)
                return new UnityEngine.Vector4(bb.ReadLong(), 0, 0, 0);
            if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
            {
                bb.SkipUnknownField(tag);
                return new UnityEngine.Vector4();
            }
            throw new Exception("can not ReadVector4 for type=" + type);
        }

        public static UnityEngine.Quaternion ReadQuaternion(ByteBuffer bb, int tag)
        {
            int type = tag & ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.VECTOR4)
                return ReadQuaternion(bb);
            if (type == ByteBuffer.VECTOR3)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                var z = bb.ReadFloat();
                return new UnityEngine.Quaternion(x, y, z, 0);
            }
            if (type == ByteBuffer.VECTOR2)
            {
                var x = bb.ReadFloat();
                var y = bb.ReadFloat();
                return new UnityEngine.Quaternion(x, y, 0, 0);
            }
            if (type == ByteBuffer.VECTOR3INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                var z = bb.ReadInt();
                return new UnityEngine.Quaternion(x, y, z, 0);
            }
            if (type == ByteBuffer.VECTOR2INT)
            {
                var x = bb.ReadInt();
                var y = bb.ReadInt();
                return new UnityEngine.Quaternion(x, y, 0, 0);
            }
            if (type == ByteBuffer.FLOAT)
                return new UnityEngine.Quaternion(bb.ReadFloat(), 0, 0, 0);
            if (type == ByteBuffer.DOUBLE)
                return new UnityEngine.Quaternion((float)bb.ReadDouble(), 0, 0, 0);
            if (type == ByteBuffer.INTEGER)
                return new UnityEngine.Quaternion(bb.ReadLong(), 0, 0, 0);
            if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
            {
                bb.SkipUnknownField(tag);
                return new UnityEngine.Quaternion();
            }
            throw new Exception("can not ReadQuaternion for type=" + type);
        }
#pragma warning restore CS0162 // Unreachable code detected
    }
#endif
}
