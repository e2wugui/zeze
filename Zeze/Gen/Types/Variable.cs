using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Variable
	{
		public Type Bean{ get; private set; } // Bean or BeanKey
		public String Name { get; private set; }
		public String NamePrivate => "_" + Name;
		public String NameUpper1 => Name.Substring(0, 1).ToUpper() + Name.Substring(1);
		public int Id { get; private set; }
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
		/*
		public Variable(String name, String type, String key, String value)
		{
			this.Name = name;
			this.Type = type;
			this.Key = key;
			this.Value = value;
			this.Initial = "";
		}
		*/

		public string GetBeanFullName()
		{
			if (Bean is Bean)
				return ((Bean)Bean).FullName;

			if (Bean is BeanKey)
				return ((BeanKey)Bean).FullName;

			throw new Exception("Variable holder is not a bean");
		}

		public Variable(Type bean, XmlElement self)
		{
			Bean = bean;
			Name = self.GetAttribute("name").Trim();
			verfiyReserveVariableName(Name);
			Id = int.Parse(self.GetAttribute("id"));
			if (Id < 0 || Id > global::Zeze.Transaction.Bean.MaxVariableId)
				throw new Exception("variable id invalid. range [0, " + global::Zeze.Transaction.Bean.MaxVariableId + "] @" + GetBeanFullName());
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
					Comment = c.InnerText.Trim();
					Regex regex = new Regex("[\r\n]");
					Comment = regex.Replace(Comment, "");
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
