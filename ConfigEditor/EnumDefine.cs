using System;
using System.Windows.Forms;
using System.Xml;
using System.Text.RegularExpressions;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace ConfigEditor
{
    public class EnumDefine
    {
        private string _Name;
        private SortedDictionary<string, ValueDefine> _ValueMap = new SortedDictionary<string, ValueDefine>();

        public string Name
        {
            get
            {
                return _Name;
            }
            set
            {
                _Name = value;
                Parent.Document.IsChanged = true;
            }
        }
        public string NamePinyin => Tools.ToPinyin(Name);
        public ReadOnlyDictionary<string, ValueDefine> ValueMap { get;}

        public XmlElement Self { get; private set; }
        public BeanDefine Parent { get; }

        public ValueDefine GetValueDefine(string name)
        {
            if (_ValueMap.TryGetValue(name, out var v))
                return v;
            return null;
        }

        public void Delete()
        {
            if (null != Self)
            {
                Self.ParentNode.RemoveChild(Self);
            }
            Parent.RemoveEnumDefines(Name);
        }

        public string FullNamePinyin()
        {
            return Parent.FullNamePinyin() + "." + NamePinyin;
        }

        public string FullName()
        {
            return Parent.FullName() + "." + Name;
        }

        public EnumDefine(BeanDefine bean, string name)
        {
            this.Parent = bean;
            this._Name = name;
            ValueMap = new ReadOnlyDictionary<string, ValueDefine>(_ValueMap);
        }

        public EnumDefine(BeanDefine bean, XmlElement self)
        {
            this.Parent = bean;
            this.Self = self;
            this._Name = self.GetAttribute("name");
            ValueMap = new ReadOnlyDictionary<string, ValueDefine>(_ValueMap);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;
                XmlElement e = (XmlElement)node;

                switch (e.Name)
                {
                    case "value":
                        AddValue(new ValueDefine(this, e));
                        break;
                    default:
                        throw new Exception("node=" + e.Name);
                }
            }
        }

        private int MaxValue = -1;

        public void AddValue(ValueDefine valueDefine)
        {
            _ValueMap.Add(valueDefine.Name, valueDefine);
            if (valueDefine.Value < 0)
                valueDefine.Value = ++MaxValue;
            else if (valueDefine.Value > MaxValue)
                MaxValue = valueDefine.Value;
            Parent.Document.IsChanged = true;
        }

        public void ChangeValueName(ValueDefine valueDefine, string newName)
        {
            _ValueMap.Remove(valueDefine.Name);
            valueDefine.Name = newName;
            AddValue(valueDefine);
            Parent.Document.IsChanged = true;
        }

        public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
        {
            XmlElement self = create ? null : Self;

            if (null == self)
            {
                self = xml.CreateElement("enum");
                parent.AppendChild(self);
                if (false == create)
                    Self = self;
            }
            self.SetAttribute("name", Name);

            foreach (var v in _ValueMap.Values)
            {
                v.SaveAs(xml, self, create);
            }
        }

        public class ValueDefine
        {
            private string _Name = "";
            private int _Value = -1;
            private string _Comment;

            public string Name
            {
                get
                {
                    return _Name;
                }
                set
                {
                    _Name = value;
                    Parent.Parent.Document.IsChanged = true;
                }
            }

            public string NamePinyin => Tools.ToPinyin(Name);
            public int Value
            {
                get
                {
                    return _Value;
                }
                set
                {
                    _Value = value;
                    Parent.Parent.Document.IsChanged = true;
                }
            }

            public string Comment
            {
                get
                {
                    return _Comment;
                }
                set
                {
                    _Comment = value;
                    Parent.Parent.Document.IsChanged = true;
                }
            }
            public XmlElement Self { get; private set; }
            public EnumDefine Parent { get; }

            public string FullName()
            {
                return Parent.FullName() + "." + Name;
            }

            public void Delete()
            {
                Parent._ValueMap.Remove(Name);
                if (null != Self)
                {
                    Self.ParentNode.RemoveChild(Self);
                }
                Parent.Parent.Document.IsChanged = true;
            }

            public ValueDefine(EnumDefine e, string name, int val)
            {
                this.Parent = e;
                this._Name = name;
                this._Value = val;
            }

            public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
            {
                XmlElement self = create ? null : Self;

                if (null == self)
                {
                    self = xml.CreateElement("value");
                    parent.AppendChild(self);
                    if (false == create)
                        Self = self;
                }
                self.SetAttribute("name", Name);
                self.SetAttribute("value", Value.ToString());
            }

            public ValueDefine(EnumDefine e, XmlElement self)
            {
                this.Self = self;
                this.Parent = e;
                _Name = self.GetAttribute("name");
                var tmp = self.GetAttribute("value");
                _Value = tmp.Length > 0 ? int.Parse(tmp) : 0;
                _Comment = self.GetAttribute("comment").Trim();
                if (_Comment.Length == 0)
                {
                    XmlNode c = self.NextSibling;
                    if (c != null && XmlNodeType.Text == c.NodeType)
                    {
                        _Comment = c.InnerText.Trim();
                        Regex regex = new Regex("[\r\n]");
                        _Comment = regex.Replace(Comment, "");
                    }
                }
            }

        }
    }

}
