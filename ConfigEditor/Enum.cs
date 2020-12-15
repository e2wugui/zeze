using System;
using System.Windows.Forms;
using System.Xml;
using System.Text.RegularExpressions;

namespace ConfigEditor
{
    public class Enum
    {
        public string Name { get; set; }
        public string Value { get; set; }
        public string Comment { get; set; }
        public XmlElement Self { get; }
        public Bean Bean { get; }
        public Enum(Bean bean, XmlElement self)
        {
            this.Self = self;
            this.Bean = bean;
            Name = self.GetAttribute("name");
            Value = self.GetAttribute("value");
            Comment = self.GetAttribute("description");
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
        }
    }

}
