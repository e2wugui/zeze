using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.Types
{
    public class TypeDecimal : Type
    {
        public override string Name => "decimal";

        public override bool IsImmutable => true;

        public override bool IsNeedNegativeCheck => true;


        internal TypeDecimal(SortedDictionary<string, Type> types)
        {
            types.Add(Name, this);
        }

        public override void Accept(Visitor visitor)
        {
            visitor.Visit(this);
        }

        public override Type Compile(ModuleSpace space, string key, string value, Variable var)
        {
            if (key != null && key.Length > 0)
                throw new Exception(Name + " type does not need a key. " + key);

            if (value != null && value.Length > 0)
                throw new Exception(Name + " type does not need a value. " + value);

            return this;
        }

        public override void Depends(HashSet<Type> includes, string parent)
        {
            includes.Add(this);
        }

        public override void DependsIncludesNoRecursive(HashSet<Type> includes)
        {
            includes.Add(this);
        }
    }
}
