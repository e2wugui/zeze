﻿using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class TypeName : Visitor
    {
        protected string name;
        internal string nameCollectionImplement; // 容器内部类型。其他情况下为 null。TODO 这个没有再使用了，确认以后，可以考虑删除掉。暂时先保留。
        internal string nameRaw; // 容器，其他为null。
        string nameOmitted;

        public static string GetName(Type type)
        {
            TypeName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public static string GetNameOmitted(Type type)
        {
            var visitor = new TypeName();
            type.Accept(visitor);
            return visitor.nameOmitted;
        }

        public virtual void Visit(TypeBool type)
        {
            name = "boolean";
        }

        public virtual void Visit(TypeByte type)
        {
            name = "byte";
        }

        public virtual void Visit(TypeShort type)
        {
            name = "short";
        }

        public virtual void Visit(TypeInt type)
        {
            name = "int";
        }

        public virtual void Visit(TypeLong type)
        {
            name = "long";
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
            name = "Zeze.Net.Binary";
        }

        public virtual void Visit(TypeString type)
        {
            name = "String";
        }

        public virtual void Visit(TypeList type)
        {
            string valueName = BoxingName.GetBoxingName(type.ValueType);
            nameRaw = "Zeze.Transaction.Collections.PList";
            nameOmitted = nameRaw + (type.ValueType.IsNormalBean ? '2' : '1');
            name = nameOmitted + '<' + valueName + '>';
            nameCollectionImplement = "org.pcollections.PVector<" + valueName + '>';
        }

        public virtual void Visit(TypeSet type)
        {
            string valueName = BoxingName.GetBoxingName(type.ValueType);
            nameRaw = "Zeze.Transaction.Collections.PSet";
            nameOmitted = nameRaw + '1';
            name = nameOmitted + '<' + valueName + '>';
            nameCollectionImplement = "org.pcollections.PSet<" + valueName + '>';
        }

        public virtual void Visit(TypeMap type)
        {
            string key = BoxingName.GetBoxingName(type.KeyType);
            string value = BoxingName.GetBoxingName(type.ValueType);
            nameRaw = "Zeze.Transaction.Collections.PMap";
            nameOmitted = nameRaw + (type.ValueType.IsNormalBean ? '2' : '1');
            name = nameOmitted + '<' + key + ", " + value + '>';
            nameCollectionImplement = "org.pcollections.PMap<" + key + ", " + value + '>';
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
            name = "Zeze.Transaction.DynamicBean";
        }

        public void Visit(TypeQuaternion type)
        {
            name = "Zeze.Serialize.Quaternion";
        }

        public void Visit(TypeVector2 type)
        {
            name = "Zeze.Serialize.Vector2";
        }

        public void Visit(TypeVector2Int type)
        {
            name = "Zeze.Serialize.Vector2Int";
        }

        public void Visit(TypeVector3 type)
        {
            name = "Zeze.Serialize.Vector3";
        }

        public void Visit(TypeVector3Int type)
        {
            name = "Zeze.Serialize.Vector3Int";
        }

        public void Visit(TypeVector4 type)
        {
            name = "Zeze.Serialize.Vector4";
        }

        public void Visit(TypeDecimal type)
        {
            name = "java.math.BigDecimal";
        }

        public void Visit(TypeGTable type)
        {
            string rowKey = BoxingName.GetBoxingName(type.RowKeyType);
            string colKey = BoxingName.GetBoxingName(type.ColKeyType);
            string value = BoxingName.GetBoxingName(type.ValueType);

            nameRaw = "Zeze.Transaction.GTable.GTable";
            var name12 = type.ValueType.IsNormalBean ? '2' : '1';
            var valueReadOnly = type.ValueType.IsNormalBean ? $", {value}ReadOnly" : "";
            nameOmitted = nameRaw + name12;
            name = nameOmitted + '<' + rowKey + ", " + colKey + ", " + value + valueReadOnly + ">";
        }
    }
}
