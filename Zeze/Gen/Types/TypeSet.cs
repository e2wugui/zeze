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

		public override Type Compile(ModuleSpace space, string key, string value, Variable var)
		{
			return new TypeSet(space, key, value, var);
		}

		private TypeSet(ModuleSpace space, string key, string value, Variable var)
		{
			Variable = var;
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);
			ValueType = Type.Compile(space, value, null, null, var);
			if (ValueType is TypeDynamic)
				throw new Exception(Name + " Error : value type is dynamic.");
			if (!ValueType.IsKeyable)
				throw new Exception("set value need a keyable type.");
		}

		internal TypeSet(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

		public override string Name => "set";
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


    }
}
