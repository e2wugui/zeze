using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Enum
	{
		public String Name { get; private set; }
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
						Comment = c.InnerText.Trim().Replace("[\r\n]", ""); // c# string 内部有正则表达式吗
					}
				}
			}
			if (Comment.Length > 0)
				Comment = " // " + Comment;
		}
	}
}
