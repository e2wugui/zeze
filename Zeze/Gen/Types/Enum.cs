using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Enum
	{
		public String Name { get; private set; }
		public string NamePinyin => Program.ToPinyin(Name);

		public String Value { get; private set; }
		public String Comment { get; private set; } = "";

		public Enum(XmlElement self)
		{
			Name = self.GetAttribute("name").Trim();
			Value = self.GetAttribute("value").Trim();
			Comment = self.GetAttribute("description").Trim();
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
