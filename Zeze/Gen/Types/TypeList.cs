using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class TypeList : TypeCollection
	{
		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, String key, String value)
		{
			return new TypeList(space, key, value);
		}

		private TypeList(Zeze.Gen.ModuleSpace space, String key, String value)
		{
			_compile(space, key, value);
		}

		internal TypeList(SortedDictionary<String, Type> types)
		{
			types.Add(Name, this);
		}

		public override String Name => "list";
	}
}
