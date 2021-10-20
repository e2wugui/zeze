using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Serialize
{
    public class SerializeHelper
    {
        public static Func<ByteBuffer, T> CreateDecodeFunc<T>()
        {
            var type = typeof(T);

            if (type == typeof(byte))
            {
                return (Func<ByteBuffer, T>)(Delegate)(new Func<ByteBuffer, byte>(
                    (ByteBuffer bb) => bb.ReadByte()));
            }

            if (type == typeof(short))
            {
                return (Func<ByteBuffer, T>)(Delegate)(new Func<ByteBuffer, short>(
                    (ByteBuffer bb) => bb.ReadShort()));
            }

            if (type == typeof(int))
            {
                return (Func<ByteBuffer, T>)(Delegate)(new Func<ByteBuffer, int>(
                    (ByteBuffer bb) => bb.ReadInt()));
            }

            if (type == typeof(long))
            {
                return (Func<ByteBuffer, T>)(Delegate)(new Func<ByteBuffer, long>(
                    (ByteBuffer bb) => bb.ReadLong()));
            }

            if (type == typeof(string))
            {
                return (Func<ByteBuffer, T>)(Delegate)(new Func<ByteBuffer, string>(
                    (ByteBuffer bb) => bb.ReadString()));
            }

            if (type == typeof(Zeze.Net.Binary))
            {
                return (Func<ByteBuffer, T>)(Delegate)(new Func<ByteBuffer, Zeze.Net.Binary>(
                    (ByteBuffer bb) => bb.ReadBinary()));
            }

            if (type.IsAssignableTo(typeof(Serializable)))
            {
                return (Func<ByteBuffer, T>)Delegate.CreateDelegate(
                    typeof(Func<ByteBuffer, T>),
                    typeof(SerializeHelper)
                    .GetMethod(nameof(CreateSerialiableDecodeFunc))
                    .MakeGenericMethod(type));
            }

            return null;
        }

        public static T CreateSerialiableDecodeFunc<T>(ByteBuffer buf)
            where T : Serializable, new()
        {
            var value = new T();
            value.Decode(buf);
            return value;
        }

        public static Action<ByteBuffer, T> CreateEncodeFunc<T>()
        {
            var type = typeof(T);

            if (type == typeof(byte))
            {
                return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, byte>(
                        (ByteBuffer buf, byte x) => buf.WriteByte(x)));
            }

            if (type == typeof(short))
            {
                return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, short>(
                        (ByteBuffer buf, short x) => buf.WriteShort(x)));
            }

            if (type == typeof(int))
            {
                return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, int>(
                        (ByteBuffer buf, int x) => buf.WriteInt(x)));
            }

            if (type == typeof(long))
            {
                return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, long>(
                        (ByteBuffer buf, long x) => buf.WriteLong(x)));
            }

            if (type == typeof(string))
            {
                return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, string>(
                        (ByteBuffer buf, string x) => buf.WriteString(x)));
            }

            if (type == typeof(Zeze.Net.Binary))
            {
                return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, Zeze.Net.Binary>(
                        (ByteBuffer buf, Zeze.Net.Binary x) => buf.WriteBinary(x)));
            }

            if (typeof(Serializable).IsAssignableFrom(typeof(T)))
            {
                return (ByteBuffer buf, T x) => (x as Serializable).Encode(buf);
            }

            return null;
        }
    }

    public sealed class SerializeHelper<T> : SerializeHelper
    {
        public static Func<ByteBuffer, T> Decode { get; } = CreateDecodeFunc<T>();
        public static Action<ByteBuffer, T> Encode { get; } = CreateEncodeFunc<T>();
    }
}
