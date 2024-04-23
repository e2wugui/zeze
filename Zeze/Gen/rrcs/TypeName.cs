using Zeze.Gen.Types;

namespace Zeze.Gen.rrcs
{
    public class TypeName : Visitor
    {
        internal string name;
        internal string nameCollectionImplement; // 容器内部类型。其他情况下为 null。

        public static string GetName(Type type)
        {
            TypeName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public void Visit(TypeBool type)
        {
            name = "bool";
        }

        public void Visit(TypeByte type)
        {
            name = "byte";
        }

        public void Visit(TypeShort type)
        {
            name = "short";
        }

        public void Visit(TypeInt type)
        {
            name = "int";
        }

        public void Visit(TypeLong type)
        {
            name = "long";
        }

        public void Visit(TypeFloat type)
        {
            name = "float";
        }

        public void Visit(TypeDouble type)
        {
            name = "double";
        }

        public void Visit(TypeBinary type)
        {
            name = "Zeze.Net.Binary";
        }

        public void Visit(TypeString type)
        {
            name = "string";
        }

        public void Visit(TypeList type)
        {
            string valueName = GetName(type.ValueType);
            name = "Zeze.Raft.RocksRaft.CollList" + (type.ValueType.IsNormalBeanOrRocks ? "2<" : "1<")  + valueName + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableList<" + valueName + ">";
        }

        public void Visit(TypeSet type)
        {
            string valueName = GetName(type.ValueType);
            name = "Zeze.Raft.RocksRaft.CollSet1<" + valueName + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableHashSet<" + valueName + ">";
        }

        public void Visit(TypeMap type)
        {
            string key = GetName(type.KeyType);
            string value = GetName(type.ValueType);
            name = "Zeze.Raft.RocksRaft.CollMap" + (type.ValueType.IsNormalBeanOrRocks ? "2<" : "1<") + key + ", " + value + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableDictionary<" + key + ", " + value + ">";
        }

        public void Visit(Bean type)
        {
            name = type.Space.Path(".", type.Name);
        }

        public void Visit(BeanKey type)
        {
            name = type.Space.Path(".", type.Name);
        }

        public void Visit(TypeDynamic type)
        {
            name = "Zeze.Raft.RocksRaft.DynamicBean";
        }

        public void Visit(TypeQuaternion type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeDecimal type)
        {
            name = "decimal";
        }
    }
}
