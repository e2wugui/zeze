using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class TypeTagName : Visitor
    {
        public string Name { get; private set; }

        public static string GetName(Type type)
        {
            TypeTagName v = new TypeTagName();
            type.Accept(v);
            return v.Name;
        }

        public void Visit(TypeBool type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        public void Visit(TypeByte type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        public void Visit(TypeShort type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        public void Visit(TypeInt type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        public void Visit(TypeLong type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        public void Visit(TypeFloat type)
        {
            Name = "ByteBuffer.FLOAT";
        }

        public void Visit(TypeDouble type)
        {
            Name = "ByteBuffer.DOUBLE";
        }

        public void Visit(TypeBinary type)
        {
            Name = "ByteBuffer.BYTES";
        }

        public void Visit(TypeString type)
        {
            Name = "ByteBuffer.BYTES";
        }

        public void Visit(TypeList type)
        {
            Name = "ByteBuffer.LIST";
        }

        public void Visit(TypeSet type)
        {
            Name = "ByteBuffer.LIST";
        }

        public void Visit(TypeMap type)
        {
            Name = "ByteBuffer.MAP";
        }

        public void Visit(Bean type)
        {
            Name = "ByteBuffer.BEAN";
        }

        public void Visit(BeanKey type)
        {
            Name = "ByteBuffer.BEAN";
        }

        public void Visit(TypeDynamic type)
        {
            Name = "ByteBuffer.DYNAMIC";
        }

        public void Visit(TypeQuaternion type)
        {
            Name = "ByteBuffer.VECTOR4";
        }

        public void Visit(TypeVector2 type)
        {
            Name = "ByteBuffer.VECTOR2";
        }

        public void Visit(TypeVector2Int type)
        {
            Name = "ByteBuffer.VECTOR2INT";
        }

        public void Visit(TypeVector3 type)
        {
            Name = "ByteBuffer.VECTOR3";
        }

        public void Visit(TypeVector3Int type)
        {
            Name = "ByteBuffer.VECTOR3INT";
        }

        public void Visit(TypeVector4 type)
        {
            Name = "ByteBuffer.VECTOR4";
        }

        public void Visit(TypeDecimal type)
        {
            Name = "ByteBuffer.BYTES";
        }
    }
}
