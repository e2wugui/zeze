using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public enum JsonType
	{
		None,
		JSON_OBJECT,
		JSON_ARRAY,
	}

	public class TypeString : Type
	{
		private JsonType type = JsonType.None;

		public TypeString(JsonType type)
        {
            this.type = type;
        }

		public JsonType getJsonType()
		{
			return type;
		}

		public string getJsonTypeClass()
		{
			switch (type)
			{
				case JsonType.JSON_OBJECT:
					return "com.alibaba.fastjson.JSONObject";
				case JsonType.JSON_ARRAY:
					return "com.alibaba.fastjson.JSONArray";
				default:
					throw new Exception("JsonType.None");
            }
		}

        public override Type Compile(ModuleSpace space, string key, string value, Variable var)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);

			if (string.IsNullOrEmpty(value))
				return this;

			JsonType type;
			switch (value)
			{
				case "JSON_OBJECT":
					type = JsonType.JSON_OBJECT;
					break;
				case "JSON_ARRAY":
					type = JsonType.JSON_ARRAY;
					break;
				default:
                    throw new Exception(Name + " invalid value. " + value);
            }
            return new TypeString(type);
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

        public override string Name => "string";
        public override bool IsImmutable => true; // xxx language depends
		public override bool IsNeedNegativeCheck => false;
		public override bool IsJavaPrimitive => false;

		internal TypeString(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}
	}
}
