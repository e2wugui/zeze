using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public static class DynamicField
    {
        internal static readonly int LogTypeId = FixedHash.Hash32("Zeze.Transaction.DynamicFieldLog");

        private static readonly ConcurrentDictionary<(long, int), Func<long, ConfBean>> ValueFactories
            = new ConcurrentDictionary<(long, int), Func<long, ConfBean>>();
        private static readonly ConcurrentDictionary<(long, int, int), Func<Log>> LogFactories
            = new ConcurrentDictionary<(long, int, int), Func<Log>>();

        public static void Register(long ownerSchemaId, int fieldId, Func<long, ConfBean> factory)
        {
            ValueFactories.TryAdd((ownerSchemaId, fieldId), factory);
        }

        public static void RegisterLog(long ownerSchemaId, int fieldId, int typeId, Func<Log> factory)
        {
            LogFactories.TryAdd((ownerSchemaId, fieldId, typeId), factory);
        }

        internal static Log CreateLog(ByteBuffer bb)
        {
            var owner = bb.ReadLong();
            var field = bb.ReadUInt();
            var type = bb.ReadInt4();
            if (LogFactories.TryGetValue((owner, field, type), out var factory))
                return factory();
            throw new InvalidOperationException($"unknown dynamic log ({owner}, {field}), typeId={type}");
        }

        internal static ConfBean ReadReplacement(ByteBuffer bb, out long specialTypeId)
        {
            var owner = bb.ReadLong();
            var field = bb.ReadUInt();
            specialTypeId = bb.ReadLong();
            if (!ValueFactories.TryGetValue((owner, field), out var factory))
                throw new InvalidOperationException($"unknown dynamic field ({owner}, {field})");
            return ReadValue(bb, specialTypeId, factory);
        }

        public static V ReadValue<V>(ByteBuffer bb, Func<long, V> factory) where V : ConfBean
        {
            return ReadValue(bb, bb.ReadLong(), factory);
        }

        private static V ReadValue<V>(ByteBuffer bb, long specialTypeId, Func<long, V> factory) where V : ConfBean
        {
            var value = factory(specialTypeId);
            if (value != null)
                value.Decode(bb);
            else if (specialTypeId == ConfEmptyBean.TYPEID)
                ConfEmptyBean.Instance.Decode(bb);
            else
                throw new InvalidOperationException($"unknown dynamic value {specialTypeId}");
            return value;
        }
    }

    public class LogBean : Log
    {
        public static readonly string StableName = Reflect.GetStableName(typeof(LogBean));
        public static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public Dictionary<int, Log> Variables { get; } = new Dictionary<int, Log>();

        private const int NestedBean = 0;
        private const int NestedDynamic = 1;

        protected static void EncodeLogBean(ByteBuffer bb, LogBean logBean)
        {
            bb.WriteByte(logBean is LogConfDynamic ? NestedDynamic : NestedBean);
            logBean.Encode(bb);
        }

        public static LogBean DecodeLogBean(ByteBuffer bb)
        {
            var type = bb.ReadByte();
            LogBean logBean;
            switch (type)
            {
                case NestedBean:
                    logBean = new LogBean();
                    break;
                case NestedDynamic:
                    logBean = new LogConfDynamic();
                    break;
                default:
                    throw new System.Exception("unknown logBean subclass type=" + type);
            }
            logBean.Decode(bb);
            return logBean;
        }

        public override void Decode(ByteBuffer bb)
        {
            for (int i = bb.ReadUInt(); i > 0; --i)
            {
                var typeId = bb.ReadInt4();
                var varId = bb.ReadUInt();
                var log = typeId == DynamicField.LogTypeId ? DynamicField.CreateLog(bb) : Create(typeId);
                log.VariableId = varId;
                log.Decode(bb);

                Variables.Add(varId, log);
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteUInt(Variables.Count);
            foreach (var log in Variables.Values)
            {
                bb.WriteInt4(log.TypeId);
                bb.WriteUInt(log.VariableId);
                log.Encode(bb);
            }
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            ByteBuffer.BuildString(sb, Variables, ComparerInt.Instance);
            return sb.ToString();
        }
    }
}
