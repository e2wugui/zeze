using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;
using System.Text.RegularExpressions;


namespace ConfigEditor
{
    public class Variable
    {
        public string Name { get; set; }
        public string Type { get; set; }
        public string Key { get; set; }
        public string Value { get; set; }
        public string Foreign { get; set; }
        public string Properties { get; set; } // unique;
        public string Comment { get; set; }

        public XmlElement Self { get; }
        public BeanDefine Bean { get; }
        public Variable(BeanDefine bean, XmlElement self)
        {
            this.Self = self;
            this.Bean = bean;

            Name = self.GetAttribute("name");
            Type = self.GetAttribute("type");
            Key = self.GetAttribute("key");
            Value = self.GetAttribute("value");
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
        }
    }
}
