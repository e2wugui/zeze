using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.Types
{
    public class TypeGTable : Type
    {
        public string RowKey { get; private set; }
        public string ColKey { get; private set; }
        public string Value { get; private set; }

        public Type RowKeyType { get; private set; }
        public Type ColKeyType { get; private set; }
        public Type ValueType { get; private set; }

        public override string Name => "gtable";
        public override bool IsImmutable => false;
        private bool? isNeedNegativeCheckCache = null;
        public override bool IsNeedNegativeCheck
        {
            get
            {
                if (isNeedNegativeCheckCache != null)
                    return isNeedNegativeCheckCache.Value;
                isNeedNegativeCheckCache = false;
                if (ValueType.IsNeedNegativeCheck)
                    return true;
                isNeedNegativeCheckCache = null;
                return false;
            }
        }
        public override bool IsJavaPrimitive => false;

        public override void Accept(Visitor visitor)
        {
            visitor.Visit(this);
        }

        public override Type Compile(ModuleSpace space, string key, string value, Variable var)
        {
            return new TypeGTable(space, key, value, var);
        }

        public override void Depends(HashSet<Type> includes, string parent)
        {
            if (includes.Add(this))
            {
                RowKeyType.Depends(includes, parent);
                ColKeyType.Depends(includes, parent);
                ValueType.Depends(includes, parent);
            }
        }

        public override void DependsIncludesNoRecursive(HashSet<Type> includes)
        {
            if (includes.Add(this))
            {
                RowKeyType.DependsIncludesNoRecursive(includes);
                ColKeyType.DependsIncludesNoRecursive(includes);
                ValueType.DependsIncludesNoRecursive(includes);
            }
        }

        private TypeGTable(ModuleSpace space, string key, string value, Variable var)
        {
            Variable = var;
            if (key.Length == 0)
                throw new Exception("gtable type need two keys");
            if (value.Length == 0)
                throw new Exception("gtable type need a value");

            var keys = key.Split(',');
            if (keys.Length != 2)
                throw new Exception("gtable error keys " + key);

            RowKey = keys[0];
            ColKey = keys[1];
            Value = value;

            RowKeyType = Type.Compile(space, RowKey, null, null, var);
            if (!RowKeyType.IsKeyable)
                throw new Exception("gtable rowkey need a keyable type");
            ColKeyType = Type.Compile(space, ColKey, null, null, var);
            if (!ColKeyType.IsKeyable)
                throw new Exception("gtable colkey need a keyable type");
            ValueType = Type.Compile(space, value, null, null, var);

            if (ValueType is Bean b)
                b.MapKeyTypes.Add(ColKeyType); // 这里加入的ColKey，value存储在ColKey的Map中。
            //else if (ValueType is TypeDynamic d)
            //	d.MapKeyTypes.Add(ColType);

            //if (ValueType is TypeBinary)
            //	throw new Exception(Name + " Error : value type is binary.");
        }

        internal TypeGTable(SortedDictionary<string, Type> types)
        {
            types.Add(Name, this);
        }
    }
}
