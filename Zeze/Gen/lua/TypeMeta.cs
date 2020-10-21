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
            Type = Zeze.Serialize.Helper.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        void Visitor.Visit(BeanKey type)
        {
            Type = Zeze.Serialize.Helper.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        void Visitor.Visit(TypeByte type)
        {
            Type = Zeze.Serialize.Helper.BYTE;
        }

        void Visitor.Visit(TypeShort type)
        {
            Type = Zeze.Serialize.Helper.SHORT;
        }

        void Visitor.Visit(TypeInt type)
        {
            Type = Zeze.Serialize.Helper.INT;
        }

        void Visitor.Visit(TypeLong type)
        {
            Type = Zeze.Serialize.Helper.LONG;
        }

        void Visitor.Visit(TypeBool type)
        {
            Type = Zeze.Serialize.Helper.BOOL;
        }

        void Visitor.Visit(TypeBinary type)
        {
            Type = Zeze.Serialize.Helper.BYTES;
        }

        void Visitor.Visit(TypeString type)
        {
            Type = Zeze.Serialize.Helper.STRING;
        }

        void Visitor.Visit(TypeFloat type)
        {
            Type = Zeze.Serialize.Helper.FLOAT;
        }

        void Visitor.Visit(TypeDouble type)
        {
            Type = Zeze.Serialize.Helper.DOUBLE;
        }

        void Visitor.Visit(TypeList type)
        {
            Type = Zeze.Serialize.Helper.LIST;
            TypeMeta vm = TypeMeta.Get(type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeSet type)
        {
            Type = Zeze.Serialize.Helper.SET;
            TypeMeta vm = TypeMeta.Get(type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeMap type)
        {
            Type = Zeze.Serialize.Helper.MAP;

            TypeMeta km = TypeMeta.Get(type.KeyType);
            Key = km.Type;
            KeyBeanTypeId = km.TypeBeanTypeId;

            TypeMeta vm = TypeMeta.Get(type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Type = Zeze.Serialize.Helper.DYNAMIC;
            // TypeBeanTypeId = 使用的时候指定。
        }
    }
}
