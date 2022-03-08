using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Variable
	{
		public Type Bean { get; private set; } // Bean or BeanKey
		public string Name { get; private set; }
		public string NamePinyin => Program.ToPinyin(Name);
		public string NamePrivate => "_" + Name;
		public string NameUpper1 => Name.Substring(0, 1).ToUpper() + Name.Substring(1);
		public string NameGetter => Type.Equals("bool") ? $"is{NameUpper1}" : $"get{NameUpper1}";
		public string NameSetter => $"set{NameUpper1}";
		public string Getter => $"{NameGetter}()";
		public string Setter(string value) { return $"{NameSetter}({value})"; }
		public int Id { get; private set; }
		public string Type { get; private set; }
		public string Key { get; private set; }
		public string Value { get; private set; }
		public string Initial { get; private set; }
		public string Comment { get; private set; }
		public string Validator { get; private set; }
		public bool AllowNegative { get; private set; } = false;
		public bool Transient { get; private set; } = false;

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
			Program.CheckReserveName(Name);
			Id = int.Parse(self.GetAttribute("id"));
			if (Id <= 0 || Id > global::Zeze.Transaction.Bean.MaxVariableId)
				throw new Exception("variable id invalid. range [1, " + global::Zeze.Transaction.Bean.MaxVariableId + "] @" + GetBeanFullName());
			Type = self.GetAttribute("type").Trim();
			if (string.IsNullOrEmpty(Type))
				throw new Exception("Variable Type Can Not Be Empty.");
			Key = self.GetAttribute("key").Trim();
			Value = self.GetAttribute("value").Trim();
			Initial = self.GetAttribute("default").Trim();
			Validator = self.GetAttribute("validator").Trim();
			string attr = self.GetAttribute("AllowNegative");
			if (attr.Length > 0)
				AllowNegative = bool.Parse(attr);
			Transient = self.GetAttribute("transient").Equals("true");

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

			HashSet<string> dynamicValue = new HashSet<string>();
			XmlNodeList childNodes = self.ChildNodes;
			foreach (XmlNode node in childNodes)
			{
				if (XmlNodeType.Element != node.NodeType)
					continue;

				XmlElement e = (XmlElement)node;

                string nodename = e.Name;
				switch (e.Name)
				{
					case "value":
						dynamicValue.Add(e.GetAttribute("bean"));
						break;
					default:
						throw new Exception("node=" + nodename);
				}
			}
			foreach (string b in Value.Split(','))
				dynamicValue.Add(b.Trim());
			StringBuilder valueBuilder = new StringBuilder();
			bool first = true;
			foreach (string b in dynamicValue)
            {
				if (b.Length == 0)
					continue;
				if (first)
					first = false;
				else
					valueBuilder.Append(',');
				valueBuilder.Append(b);
			}
			Value = valueBuilder.ToString();
		}

		public Type VariableType { get; private set; }

		public void Compile(ModuleSpace space)
		{
            VariableType = Types.Type.Compile(space, Type, Key, Value);
		}
	}
}
