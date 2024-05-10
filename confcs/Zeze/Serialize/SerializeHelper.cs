using System;
using Zeze.Net;

namespace Zeze.Serialize
{
    public abstract class SerializeHelper
    {
        public static Func<ByteBuffer, T> CreateDecodeFunc<T>()
        {
            var type = typeof(T);

            if (type == typeof(bool))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, bool>(bb => bb.ReadBool());

            if (type == typeof(byte))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, byte>(bb => (byte)bb.ReadLong());

            if (type == typeof(short))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, short>(bb => (short)bb.ReadLong());

            if (type == typeof(int))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, int>(bb => bb.ReadInt());

            if (type == typeof(long))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, long>(bb => bb.ReadLong());

            if (type == typeof(string))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, string>(bb => bb.ReadString());

            if (type == typeof(double))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, double>(bb => bb.ReadDouble());

            if (type == typeof(float))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, float>(bb => bb.ReadFloat());

            if (type == typeof(decimal))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, decimal>(bb => decimal.Parse(bb.ReadString()));

            if (type == typeof(Binary))
                return (Func<ByteBuffer, T>)(Delegate)new Func<ByteBuffer, Binary>(bb => bb.ReadBinary());

            if (typeof(Serializable).IsAssignableFrom(type))
            {
                return (Func<ByteBuffer, T>)Delegate.CreateDelegate(
                    typeof(Func<ByteBuffer, T>),
                    // ReSharper disable once PossibleNullReferenceException
                    typeof(SerializeHelper).GetMethod(nameof(CreateSerializableDecodeFunc)).MakeGenericMethod(type));
            }

            return null;
        }

        public static T CreateSerializableDecodeFunc<T>(ByteBuffer buf)
            where T : Serializable, new()
        {
            var value = new T();
            value.Decode(buf);
            return value;
        }

        public static Action<ByteBuffer, T> CreateEncodeFunc<T>()
        {
            var type = typeof(T);

            if (type == typeof(bool))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, bool>((buf, x) => buf.WriteBool(x));

            if (type == typeof(byte))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, byte>((buf, x) => buf.WriteLong((sbyte)x));

            if (type == typeof(short))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, short>((buf, x) => buf.WriteLong(x));

            if (type == typeof(int))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, int>((buf, x) => buf.WriteInt(x));

            if (type == typeof(long))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, long>((buf, x) => buf.WriteLong(x));

            if (type == typeof(string))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, string>((buf, x) => buf.WriteString(x));

            if (type == typeof(double))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, double>((buf, x) => buf.WriteDouble(x));

            if (type == typeof(float))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, float>((buf, x) => buf.WriteFloat(x));

            if (type == typeof(decimal))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, decimal>((buf, x) => buf.WriteString(x.ToString()));

            if (type == typeof(Binary))
                return (Action<ByteBuffer, T>)(Delegate)new Action<ByteBuffer, Binary>((buf, x) => buf.WriteBinary(x));

            if (typeof(Serializable).IsAssignableFrom(typeof(T)))
                return (buf, x) => ((Serializable)x).Encode(buf);

            return null;
        }
    }

    public abstract class SerializeHelper<T> : SerializeHelper
    {
        public static Func<ByteBuffer, T> Decode { get; } = CreateDecodeFunc<T>();
        public static Action<ByteBuffer, T> Encode { get; } = CreateEncodeFunc<T>();
    }
}
