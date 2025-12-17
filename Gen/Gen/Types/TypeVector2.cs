using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public class TypeVector2 : Type
	{
		public override Type Compile(ModuleSpace space, string key, string value, Variable var)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);

			if (value != null && value.Length > 0)
				throw new Exception(Name + " type does not need a value. " + value);

			return this;
		}

		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override void Depends(HashSet<Type> includes, string parent)
		{
			includes.Add(this);
		}

        public override void DependsIncludesNoRecursive(HashSet<Type> includes)
        {
            includes.Add(this);
        }

        internal TypeVector2(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

		public override string Name => "vector2";
		public override bool IsNeedNegativeCheck => false;

		public override bool IsImmutable => true;
		public override bool IsJavaPrimitive => false;
	}
}
