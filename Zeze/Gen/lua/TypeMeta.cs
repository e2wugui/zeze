using Zeze.Gen.Types;
using Zeze.Serialize;

namespace Zeze.Gen.lua
{
    public class TypeMeta : Visitor
    {
        public Variable Var { get; private set; }
        public int Type { get; private set; }
        public long TypeBeanTypeId { get; private set; }
        public int Key { get; private set; }
        public long KeyBeanTypeId { get; private set; }
        public int Value { get; private set; }
        public long ValueBeanTypeId { get; private set; }

        public override string ToString()
        {
            return $"{{ {Type},{TypeBeanTypeId},{Key},{KeyBeanTypeId},{Value},{ValueBeanTypeId},\"{Var.NamePinyin}\" }}";
        }

        public TypeMeta(Variable var)
        {
            Var = var;
        }

        public static TypeMeta Get(Variable var, Type type)
        {
            TypeMeta v = new(var);
            type.Accept(v);
            return v;
        }

        void Visitor.Visit(TypeBool type)
        {
            Type = ByteBuffer.LUA_BOOL;
        }

        void Visitor.Visit(TypeByte type)
        {
            Type = ByteBuffer.INTEGER;
        }

        void Visitor.Visit(TypeShort type)
        {
            Type = ByteBuffer.INTEGER;
        }

        void Visitor.Visit(TypeInt type)
        {
            Type = ByteBuffer.INTEGER;
        }

        void Visitor.Visit(TypeLong type)
        {
            Type = ByteBuffer.INTEGER;
        }

        void Visitor.Visit(TypeFloat type)
        {
            Type = ByteBuffer.FLOAT;
        }

        void Visitor.Visit(TypeDouble type)
        {
            Type = ByteBuffer.DOUBLE;
        }

        void Visitor.Visit(TypeBinary type)
        {
            Type = ByteBuffer.BYTES;
        }

        void Visitor.Visit(TypeString type)
        {
            Type = ByteBuffer.BYTES;
        }

        void Visitor.Visit(TypeList type)
        {
            Type = ByteBuffer.LIST;
            TypeMeta vm = Get(Var, type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeSet type)
        {
            Type = ByteBuffer.LUA_SET;
            TypeMeta vm = Get(Var, type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(TypeMap type)
        {
            Type = ByteBuffer.MAP;

            TypeMeta km = Get(Var, type.KeyType);
            Key = km.Type;
            KeyBeanTypeId = km.TypeBeanTypeId;

            TypeMeta vm = Get(Var, type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        void Visitor.Visit(Bean type)
        {
            Type = ByteBuffer.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        void Visitor.Visit(BeanKey type)
        {
            Type = ByteBuffer.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Type = ByteBuffer.DYNAMIC;
            // TypeBeanTypeId = 使用的时候指定。
        }
    }
}
