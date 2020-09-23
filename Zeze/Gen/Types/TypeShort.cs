using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeShort : Type
	{

		public override Type Compile(ModuleSpace space, String key, String value)
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

		public override String Name => "short";
        public override bool IsImmutable => true;
		public override bool IsNeedNegativeCheck => true;

		internal TypeShort(SortedDictionary<String, Type> types)
		{
			types.Add(Name, this);
		}
	}
}
