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

		public override Type Compile(ModuleSpace space, string key, string value, object param)
		{
			return new TypeSet(space, key, value, param);
		}

		private TypeSet(ModuleSpace space, string key, string value, object param)
		{
			_compile(space, key, value, param);
			if (!ValueType.IsKeyable)
				throw new Exception("set value need a keyable type.");
		}

		internal TypeSet(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

		public override string Name => "set";
		public override bool IsNeedNegativeCheck => ValueType.IsNeedNegativeCheck;

	}
}
