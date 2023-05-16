using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Enum
	{
		public string Name { get; private set; }
		public string NamePinyin => Program.ToPinyin(Name);

		public string Value { get; private set; }
		public string Comment { get; private set; } = "";
		public string Type { get; private set; }

		public Enum(XmlElement self)
		{
			Name = self.GetAttribute("name").Trim();
			Program.CheckReserveName(Name, null);
			Value = self.GetAttribute("value").Trim();
			Comment = self.GetAttribute("description").Trim();
			Type = self.GetAttribute("type").Trim();
			if (Type.Length == 0)
				Type = "int";

			if (Comment.Length == 0)
			{
				Comment = self.GetAttribute("comment").Trim();
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
			}
			if (Comment.Length > 0)
				Comment = " // " + Comment;
		}
	}
}
