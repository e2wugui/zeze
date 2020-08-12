using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeSet : TypeCollection
	{
		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, String key, String value)
		{
			return new TypeSet(space, key, value);
		}

		private TypeSet(ModuleSpace space, String key, String value)
		{
			_compile(space, key, value);
		}

		internal TypeSet(SortedDictionary<String, Type> types)
		{
			types.Add(Name, this);
		}

		public override string Name => "set";
    }
}
