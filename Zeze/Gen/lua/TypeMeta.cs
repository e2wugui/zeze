using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.lua
{
    public class TypeMeta : Types.Visitor
    {
        public int Type { get; private set; }
        public long TypeBeanTypeId { get; private set; }
        public int Key { get; private set; }
        public long KeyBeanTypeId { get; private set; }
        public int Value { get; private set; }
        public long ValueBeanTypeId { get; private set; }

        public override string ToString()
        {
            return $"{{ {Type},{TypeBeanTypeId},{Key},{KeyBeanTypeId},{Value},{ValueBeanTypeId} }}";
        }

        public static TypeMeta Get(Types.Type type)
        {
            TypeMeta v = new TypeMeta();
            type.Accept(v);
            return v;
        }

        void Visitor.Visit(Bean type)
        {
            Type = Zeze.Serialize.ByteBuffer.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        void Visitor.Visit(BeanKey type)
        {
            Type = Zeze.Serialize.ByteBuffer.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        void Visitor.Visit(TypeByte type)
        {
            Type = Zeze.Serialize.ByteBuffer.BYTE;
        }

        void Visitor.Visit(TypeShort type)
        {
            Type = Zeze.Serialize.ByteBuffer.SHORT;
        }

        void Visitor.Visit(TypeInt type)
        {
            Type = Zeze.Serialize.ByteBuffer.INT;
        }

        void Visitor.Visit(TypeLong type)
        {
            Type = Zeze.Serialize.ByteBuffer.LONG;
        }

        void Visitor.Visit(TypeBool type)
        {
            Type = Zeze.Serialize.ByteBuffer.BOOL;
        }

        void Visitor.Visit(TypeBinary type)
        {
            Type = Zeze.Serialize.ByteBuffer.BYTES;
        }

        void Visitor.Visit(TypeString type)
        {
            Type = Zeze.Serialize.ByteBuffer.STRING;
        }

        void Visitor.Visit(TypeFloat type)
        {
            Type = Zeze.Serialize.ByteBuffer.FLOAT;
        }

        void Visitor.Visit(TypeDouble type)
        {
            Type = Zeze.Serialize.ByteBuffer.DOUBLE;
        }

        void Visitor.Visit(TypeList type)
        {
            Type = Zeze.Serialize.ByteBuffer.LIST;
            TypeMeta vm = TypeMeta.Get(type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeSet type)
        {
            Type = Zeze.Serialize.ByteBuffer.SET;
            TypeMeta vm = TypeMeta.Get(type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeMap type)
        {
            Type = Zeze.Serialize.ByteBuffer.MAP;

            TypeMeta km = TypeMeta.Get(type.KeyType);
            Key = km.Type;
            KeyBeanTypeId = km.TypeBeanTypeId;

            TypeMeta vm = TypeMeta.Get(type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Type = Zeze.Serialize.ByteBuffer.DYNAMIC;
            // TypeBeanTypeId = 使用的时候指定。
        }
    }
}
