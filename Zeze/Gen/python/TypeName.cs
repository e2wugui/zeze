using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class TypeName : Visitor
    {
        protected string name;

        public static string GetName(Type type)
        {
            TypeName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public virtual void Visit(TypeBool type)
        {
            name = "bool";
        }

        public virtual void Visit(TypeByte type)
        {
            name = "int";
        }

        public virtual void Visit(TypeShort type)
        {
            name = "int";
        }

        public virtual void Visit(TypeInt type)
        {
            name = "int";
        }

        public virtual void Visit(TypeLong type)
        {
            name = "int";
        }

        public virtual void Visit(TypeFloat type)
        {
            name = "float";
        }

        public virtual void Visit(TypeDouble type)
        {
            name = "double";
        }

        public virtual void Visit(TypeBinary type)
        {
            name = "bytes";
        }

        public virtual void Visit(TypeString type)
        {
            name = "str";
        }

        public virtual void Visit(TypeDecimal type)
        {
            name = "str";
        }

        public virtual void Visit(TypeList type)
        {
            name = "list";
        }

        public virtual void Visit(TypeSet type)
        {
            name = "set";
        }

        public virtual void Visit(TypeMap type)
        {
            name = "dict";
        }

        public virtual void Visit(Bean type)
        {
            name = type.FullName;
        }

        public virtual void Visit(BeanKey type)
        {
            name = type.FullName;
        }

        public virtual void Visit(TypeDynamic type)
        {
            name = "DynamicBean";
        }

        public void Visit(TypeVector2 type)
        {
            name = "Vector2";
        }

        public void Visit(TypeVector2Int type)
        {
            name = "Vector2Int";
        }

        public void Visit(TypeVector3 type)
        {
            name = "Vector3";
        }

        public void Visit(TypeVector3Int type)
        {
            name = "Vector3Int";
        }

        public void Visit(TypeVector4 type)
        {
            name = "Vector4";
        }

        public void Visit(TypeQuaternion type)
        {
            name = "Quaternion";
        }
    }
}
