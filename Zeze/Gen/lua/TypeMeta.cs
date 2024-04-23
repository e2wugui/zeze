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

        public void Visit(TypeBool type)
        {
            Type = ByteBuffer.LUA_BOOL;
        }

        public void Visit(TypeByte type)
        {
            Type = ByteBuffer.INTEGER;
        }

        public void Visit(TypeShort type)
        {
            Type = ByteBuffer.INTEGER;
        }

        public void Visit(TypeInt type)
        {
            Type = ByteBuffer.INTEGER;
        }

        public void Visit(TypeLong type)
        {
            Type = ByteBuffer.INTEGER;
        }

        public void Visit(TypeFloat type)
        {
            Type = ByteBuffer.FLOAT;
        }

        public void Visit(TypeDouble type)
        {
            Type = ByteBuffer.DOUBLE;
        }

        public void Visit(TypeBinary type)
        {
            Type = ByteBuffer.BYTES;
        }

        public void Visit(TypeString type)
        {
            Type = ByteBuffer.BYTES;
        }

        public void Visit(TypeList type)
        {
            Type = ByteBuffer.LIST;
            TypeMeta vm = Get(Var, type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        public void Visit(TypeSet type)
        {
            Type = ByteBuffer.LUA_SET;
            TypeMeta vm = Get(Var, type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        public void Visit(TypeMap type)
        {
            Type = ByteBuffer.MAP;

            TypeMeta km = Get(Var, type.KeyType);
            Key = km.Type;
            KeyBeanTypeId = km.TypeBeanTypeId;

            TypeMeta vm = Get(Var, type.ValueType);
            Value = vm.Type;
            ValueBeanTypeId = vm.TypeBeanTypeId;
        }

        public void Visit(Bean type)
        {
            Type = ByteBuffer.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        public void Visit(BeanKey type)
        {
            Type = ByteBuffer.BEAN;
            TypeBeanTypeId = type.TypeId;
        }

        public void Visit(TypeDynamic type)
        {
            Type = ByteBuffer.DYNAMIC;
            // TypeBeanTypeId = 使用的时候指定。
        }

        public void Visit(TypeVector2 type)
        {
            Type = ByteBuffer.VECTOR2;
        }

        public void Visit(TypeVector2Int type)
        {
            Type = ByteBuffer.VECTOR2;
        }

        public void Visit(TypeVector3 type)
        {
            Type = ByteBuffer.VECTOR3;
        }

        public void Visit(TypeVector3Int type)
        {
            Type = ByteBuffer.VECTOR3;
        }

        public void Visit(TypeVector4 type)
        {
            Type = ByteBuffer.VECTOR4;
        }

        public void Visit(TypeQuaternion type)
        {
            Type = ByteBuffer.VECTOR4;
        }

        public void Visit(TypeDecimal type)
        {
            Type = ByteBuffer.BYTES;
        }
    }
}
