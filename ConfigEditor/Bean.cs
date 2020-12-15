using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class Bean
    {
        public string Name { get; set; }
        public List<Enum> Enums { get; } = new List<Enum>();
        public List<Variable> Variables { get; } = new List<Variable>();
        public XmlElement Self { get; }
        public Document Document { get; }
        public Bean(Document doc, XmlElement self)
        {
            this.Document = doc;
            Name = self.GetAttribute("name");
            if (Name.Length == 0)
                Name = doc.Name;

            doc.Beans.Add(Name, this);
            this.Self = self;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                String nodename = e.Name;
                switch (e.Name)
                {
                    case "variable":
                        Variables.Add(new Variable(this, e));
                        break;
                    case "enum":
                        Enums.Add(new Enum(this, e));
                        break;
                    default:
                        throw new Exception("node=" + nodename);
                }
            }
        }
    }
}
