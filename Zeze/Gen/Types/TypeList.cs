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

		private TypeList(global::Zeze.Gen.ModuleSpace space, String key, String value)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);

			ValueType = Type.Compile(space, value, null, null);
			//if (ValueType is TypeBinary)
			//	throw new Exception(Name + " Error : value type is binary.");
			if (ValueType is TypeDynamic)
				throw new Exception(Name + " Error : value type is dynamic.");
		}

		internal TypeList(SortedDictionary<String, Type> types)
		{
			types.Add(Name, this);
		}

		public override String Name => "list";
		public override bool IsNeedNegativeCheck => ValueType.IsNeedNegativeCheck;

	}
}
