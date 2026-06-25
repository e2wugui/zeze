using System;
using System.Collections.Concurrent;
using System.Text;
using Zeze.Net;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    /// <summary>
    /// 操作日志。
    /// 主要用于 bean.variable 的修改。
    /// 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
    /// </summary>
    public abstract class Log : Serializable
    {
        // 会被系列化，实际上由LogBean管理。
        public abstract int TypeId { get; }
        public int VariableId;

        public abstract void Encode(ByteBuffer bb);
        public abstract void Decode(ByteBuffer bb);

        public static readonly ConcurrentDictionary<int, Func<Log>> Factories = new ConcurrentDictionary<int, Func<Log>>();

        public static Log Create(int typeId)
        {
            if (Factories.TryGetValue(typeId, out var factory))
                return factory();
            throw new Exception($"unknown log typeId={typeId}");
        }

        public static void Register<T>() where T : Log, new()
        {
            Factories.TryAdd(new T().TypeId, () => new T());
        }

        public bool BoolValue()
        {
            switch (this)
            {
                case Log<bool> _: return ((Log<bool>)this).Value;
                case Log<byte> _: return ((Log<byte>)this).Value != 0;
                case Log<short> _: return ((Log<short>)this).Value != 0;
                case Log<int> _: return ((Log<int>)this).Value != 0;
                case Log<long> _: return ((Log<long>)this).Value != 0;
                case Log<float> _: return ((Log<float>)this).Value != 0;
                case Log<double> _: return ((Log<double>)this).Value != 0;
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public byte ByteValue()
        {
            switch (this)
            {
                case Log<byte> _: return ((Log<byte>)this).Value;
                case Log<short> _: return (byte)((Log<short>)this).Value;
                case Log<int> _: return (byte)((Log<int>)this).Value;
                case Log<long> _: return (byte)((Log<long>)this).Value;
                case Log<float> _: return (byte)((Log<float>)this).Value;
                case Log<double> _: return (byte)((Log<double>)this).Value;
                case Log<bool> _: return (byte)(((Log<bool>)this).Value ? 1 : 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public short ShortValue()
        {
            switch (this)
            {
                case Log<short> _: return ((Log<short>)this).Value;
                case Log<byte> _: return ((Log<byte>)this).Value;
                case Log<int> _: return (short)((Log<int>)this).Value;
                case Log<long> _: return (short)((Log<long>)this).Value;
                case Log<float> _: return (short)((Log<float>)this).Value;
                case Log<double> _: return (short)((Log<double>)this).Value;
                case Log<bool> _: return (short)(((Log<bool>)this).Value ? 1 : 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public int IntValue()
        {
            switch (this)
            {
                case Log<int> _: return ((Log<int>)this).Value;
                case Log<byte> _: return ((Log<byte>)this).Value;
                case Log<short> _: return ((Log<short>)this).Value;
                case Log<long> _: return (int)((Log<long>)this).Value;
                case Log<float> _: return (int)((Log<float>)this).Value;
                case Log<double> _: return (int)((Log<double>)this).Value;
                case Log<bool> _: return ((Log<bool>)this).Value ? 1 : 0;
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public long LongValue()
        {
            switch (this)
            {
                case Log<long> _: return ((Log<long>)this).Value;
                case Log<byte> _: return ((Log<byte>)this).Value;
                case Log<short> _: return ((Log<short>)this).Value;
                case Log<int> _: return ((Log<int>)this).Value;
                case Log<float> _: return (long)((Log<float>)this).Value;
                case Log<double> _: return (long)((Log<double>)this).Value;
                case Log<bool> _: return ((Log<bool>)this).Value ? 1 : 0;
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public float FloatValue()
        {
            switch (this)
            {
                case Log<float> _: return ((Log<float>)this).Value;
                case Log<byte> _: return ((Log<byte>)this).Value;
                case Log<short> _: return ((Log<short>)this).Value;
                case Log<int> _: return ((Log<int>)this).Value;
                case Log<long> _: return ((Log<long>)this).Value;
                case Log<double> _: return (float)((Log<double>)this).Value;
                case Log<bool> _: return ((Log<bool>)this).Value ? 1 : 0;
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public double DoubleValue()
        {
            switch (this)
            {
                case Log<double> _: return ((Log<double>)this).Value;
                case Log<byte> _: return ((Log<byte>)this).Value;
                case Log<short> _: return ((Log<short>)this).Value;
                case Log<int> _: return ((Log<int>)this).Value;
                case Log<long> _: return ((Log<long>)this).Value;
                case Log<float> _: return ((Log<float>)this).Value;
                case Log<bool> _: return ((Log<bool>)this).Value ? 1 : 0;
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Binary BinaryValue()
        {
            switch (this)
            {
                case Log<Binary> _: return ((Log<Binary>)this).Value;
                case Log<string> _: return new Binary(Encoding.UTF8.GetBytes(((Log<string>)this).Value));
                case Log<decimal> _: return new Binary(Encoding.UTF8.GetBytes(((Log<decimal>)this).Value.ToString()));
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public string StringValue()
        {
            switch (this)
            {
                case Log<string> _: return ((Log<string>)this).Value;
                case Log<Binary> _:
                    var b = ((Log<Binary>)this).Value;
                    return Encoding.UTF8.GetString(b.Bytes, b.Offset, b.Count);
                case Log<decimal> _: return ((Log<decimal>)this).Value.ToString();
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public decimal DecimalValue()
        {
            switch (this)
            {
                case Log<decimal> _: return ((Log<decimal>)this).Value;
                case Log<Binary> _:
                    var b = ((Log<Binary>)this).Value;
                    return decimal.Parse(Encoding.UTF8.GetString(b.Bytes, b.Offset, b.Count));
                case Log<string> _: return decimal.Parse(((Log<string>)this).Value);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Vector2 Vector2Value()
        {
            switch (this)
            {
                case Log<Vector2> _: return ((Log<Vector2>)this).Value;
                case Log<Vector2Int> _: return new Vector2(((Log<Vector2Int>)this).Value);
                case Log<Vector3> _: return ((Log<Vector3>)this).Value;
                case Log<Vector3Int> _: return new Vector3(((Log<Vector3Int>)this).Value);
                case Log<Vector4> _: return ((Log<Vector4>)this).Value;
                case Log<Quaternion> _: return ((Log<Quaternion>)this).Value;
                case Log<byte> _:
                case Log<short> _:
                case Log<int> _:
                case Log<long> _:
                case Log<float> _:
                case Log<double> _:
                case Log<bool> _: return new Vector2(FloatValue(), 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Vector2Int Vector2IntValue()
        {
            switch (this)
            {
                case Log<Vector2Int> _: return ((Log<Vector2Int>)this).Value;
                case Log<Vector2> _: return new Vector2Int(((Log<Vector2>)this).Value);
                case Log<Vector3> _: return new Vector3Int(((Log<Vector3>)this).Value);
                case Log<Vector3Int> _: return ((Log<Vector3Int>)this).Value;
                case Log<Vector4> _: return new Vector3Int(((Log<Vector4>)this).Value);
                case Log<Quaternion> _: return new Vector3Int(((Log<Quaternion>)this).Value);
                case Log<byte> _:
                case Log<short> _:
                case Log<int> _:
                case Log<long> _:
                case Log<float> _:
                case Log<double> _:
                case Log<bool> _: return new Vector2Int(IntValue(), 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Vector3 Vector3Value()
        {
            switch (this)
            {
                case Log<Vector3> _: return ((Log<Vector3>)this).Value;
                case Log<Vector2> _: return new Vector3(((Log<Vector2>)this).Value);
                case Log<Vector2Int> _: return new Vector3(((Log<Vector2Int>)this).Value);
                case Log<Vector3Int> _: return new Vector3(((Log<Vector3Int>)this).Value);
                case Log<Vector4> _: return ((Log<Vector4>)this).Value;
                case Log<Quaternion> _: return ((Log<Quaternion>)this).Value;
                case Log<byte> _:
                case Log<short> _:
                case Log<int> _:
                case Log<long> _:
                case Log<float> _:
                case Log<double> _:
                case Log<bool> _: return new Vector3(FloatValue(), 0, 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Vector3Int Vector3IntValue()
        {
            switch (this)
            {
                case Log<Vector3Int> _: return ((Log<Vector3Int>)this).Value;
                case Log<Vector2> _: return new Vector3Int(((Log<Vector2>)this).Value);
                case Log<Vector2Int> _: return new Vector3Int(((Log<Vector2Int>)this).Value);
                case Log<Vector3> _: return new Vector3Int(((Log<Vector3>)this).Value);
                case Log<Vector4> _: return new Vector3Int(((Log<Vector4>)this).Value);
                case Log<Quaternion> _: return new Vector3Int(((Log<Quaternion>)this).Value);
                case Log<byte> _:
                case Log<short> _:
                case Log<int> _:
                case Log<long> _:
                case Log<float> _:
                case Log<double> _:
                case Log<bool> _: return new Vector3Int(IntValue(), 0, 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Vector4 Vector4Value()
        {
            switch (this)
            {
                case Log<Vector4> _: return ((Log<Vector4>)this).Value;
                case Log<Vector2> _: return new Vector4(((Log<Vector2>)this).Value);
                case Log<Vector2Int> _: return new Vector4(((Log<Vector2Int>)this).Value);
                case Log<Vector3> _: return new Vector4(((Log<Vector3>)this).Value);
                case Log<Vector3Int> _: return new Vector4(((Log<Vector3Int>)this).Value);
                case Log<Quaternion> _: return ((Log<Quaternion>)this).Value;
                case Log<byte> _:
                case Log<short> _:
                case Log<int> _:
                case Log<long> _:
                case Log<float> _:
                case Log<double> _:
                case Log<bool> _: return new Vector4(FloatValue(), 0, 0, 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }

        public Quaternion QuaternionValue()
        {
            switch (this)
            {
                case Log<Quaternion> _: return ((Log<Quaternion>)this).Value;
                case Log<Vector2> _: return new Quaternion(((Log<Vector2>)this).Value);
                case Log<Vector2Int> _: return new Quaternion(((Log<Vector2Int>)this).Value);
                case Log<Vector3> _: return new Quaternion(((Log<Vector3>)this).Value);
                case Log<Vector3Int> _: return new Quaternion(((Log<Vector3Int>)this).Value);
                case Log<Vector4> _: return new Quaternion(((Log<Vector4>)this).Value);
                case Log<byte> _:
                case Log<short> _:
                case Log<int> _:
                case Log<long> _:
                case Log<float> _:
                case Log<double> _:
                case Log<bool> _: return new Quaternion(FloatValue(), 0, 0, 0);
                default: throw new NotSupportedException(GetType().FullName);
            }
        }
    }

    public class Log<T> : Log
    {
        public static readonly string StableName = Util.Reflect.GetStableName(typeof(Log<T>));
        public static readonly int TypeId_ = Util.FixedHash.Hash32(StableName);

        public T Value;

        public override int TypeId => TypeId_;

        public override void Encode(ByteBuffer bb)
        {
            SerializeHelper<T>.Encode(bb, Value);
        }

        public override void Decode(ByteBuffer bb)
        {
            Value = SerializeHelper<T>.Decode(bb);
        }

        public override string ToString()
        {
            return $"{Value}";
        }
    }
}
