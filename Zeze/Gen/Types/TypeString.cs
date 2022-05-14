using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeString : Type
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

		public override void Depends(HashSet<Type> includes)
		{
			includes.Add(this);
		}

		public override string Name => "string";
        public override bool IsImmutable => true; // xxx language depends
		public override bool IsNeedNegativeCheck => false;

		internal TypeString(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}
	}
}
