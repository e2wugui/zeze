﻿using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class BeanDefine
    {
        public string Name { get; set; }
        public List<Enum> Enums { get; } = new List<Enum>();
        public List<Variable> Variables { get; } = new List<Variable>();
        public List<BeanDefine> BeanDefines { get; } = new List<BeanDefine>();

        public XmlElement Self { get; private set; }
        public Document Document { get; }
        public BeanDefine Parent { get; }
        public bool IsLocked { get; set; } = false;

        public Variable GetVariable(string name)
        {
            foreach (var v in Variables)
            {
                if (v.Name == name)
                    return v;
            }
            return null;
        }

        public BeanDefine GetSubBeanDefine(string name)
        {
            foreach (var v in BeanDefines)
            {
                if (v.Name == name)
                    return v;
            }
            return null;
        }

        public BeanDefine Search(string [] path, int offset)
        {
            BeanDefine r = this;
            for (int i = offset; i < path.Length && null != r; ++i)
            {
                r = r.GetSubBeanDefine(path[i]);
            }
            return r;
        }

        public void BuildGridColumns(DataGridView grid, ColumnTag tag)
        {
            foreach (var v in Variables)
            {
                v.BuildGridColumns(grid, tag);
            }
            // 这里创建的列用来新增。
            grid.Columns.Add(new DataGridViewColumn(new DataGridViewTextBoxCell())
            {
                HeaderText = ",",
                Width = 60,
                Tag = tag.Copy(),
            });
        }

        public BeanDefine CreateSubBeanDefine(string name)
        {
            return new BeanDefine(Document, name, this);
        }

        public BeanDefine(Document doc, string name = null, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            this.Name = null != name ? name : doc.Name;
        }

        public void Save()
        {
            if (null == Self)
            {
                Self = Document.Xml.CreateElement("BeanDefine");
                if (Parent == null)
                    Document.Xml.DocumentElement.AppendChild(Self);
                else
                    Parent.Self.AppendChild(Self);
            }
            foreach (var e in Enums)
            {
                e.Save(Self);
            }
            foreach (var b in BeanDefines)
            {
                b.Save();
            }
            foreach (var v in Variables)
            {
                v.Save(Self);
            }
        }

        public BeanDefine(Document doc, XmlElement self, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            Name = self.GetAttribute("name");
            if (Name.Length == 0)
                Name = doc.Name;
            this.Self = self;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                switch (e.Name)
                {
                    case "BeanDefine":
                        BeanDefines.Add(new BeanDefine(Document, e, this));
                        break;
                    case "variable":
                        Variables.Add(new Variable(this, e));
                        break;
                    case "enum":
                        Enums.Add(new Enum(this, e));
                        break;
                    default:
                        throw new Exception("node=" + e.Name);
                }
            }
        }
    }
}
