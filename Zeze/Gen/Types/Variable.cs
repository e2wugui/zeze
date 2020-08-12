using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Variable
	{
		public String Name { get; private set; }
		public String Type { get; private set; }
		public String Key { get; private set; }
		public String Value { get; private set; }
		public String Initial { get; private set; }
		public String Comment { get; private set; }
		public String Validator { get; private set; }

		public static void verfiyReserveVariableName(String name)
		{
			if (name.Equals("type"))
				throw new Exception("name of 'type' is reserved");
		}

		////////////////////////////////////
		// FOR dynamic create
		public Variable(String name, String type, String key, String value)
		{
			this.Name = name;
			this.Type = type;
			this.Key = key;
			this.Value = value;
			this.Initial = "";
		}

		public Variable(XmlElement self)
		{
			Name = self.GetAttribute("name").Trim();
			verfiyReserveVariableName(Name);
			Type = self.GetAttribute("type").Trim();
			Key = self.GetAttribute("key").Trim();
			Value = self.GetAttribute("value").Trim();
			Initial = self.GetAttribute("default").Trim();
			Validator = self.GetAttribute("validator").Trim();

			Comment = self.GetAttribute("comment");
			if (Comment.Length == 0)
			{
				XmlNode c = self.NextSibling;
				if (c != null && XmlNodeType.Text == c.NodeType)
				{
					Comment = c.InnerText.Trim().Replace("[\r\n]", "");
				}
			}
			if (Comment.Length > 0)
				Comment = " // " + Comment;
		}

		public Type VariableType { get; private set; }

		public void Compile(ModuleSpace space)
		{
            VariableType = Types.Type.Compile(space, Type, Key, Value);
		}
	}
}
