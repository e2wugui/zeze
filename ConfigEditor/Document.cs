using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class Document
    {
        public Documents.File File { get; internal set; }
        public string FileName => File.AbsoluteName;
        public string RelateName { get; private set; }
        public string Name => File.Name;

        public string NamePinyin => Tools.ToPinyin(Name);
        public BeanDefine BeanDefine { get; private set; } // bean in this file

        private List<Bean> _Beans = new List<Bean>();
        public ReadOnlyCollection<Bean> Beans { get; }
        public GridData GridData { get; private set; }

        public bool IsChanged { get; set; } = false;

        // 目前只有c#使用了。所有的生成代码放在这个空间下。
        // 这样输入 Config. 就可以有配置提示。
        public static string NamespacePrefix { get; set; } = "Config";

        // 不包含文档名，仅包含目录名。
        public string Namespace
        {
            get
            {
                string docpath = File.Parent.RelateName;
                if (string.IsNullOrEmpty(docpath))
                    return NamespacePrefix;
                return NamespacePrefix + "." + docpath.Replace(Path.DirectorySeparatorChar, '.');
            }
        }

        public StreamWriter OpenStreamWriter(string srcHome, string ext)
        {
            string dir = Path.Combine(srcHome, NamespacePrefix, File.Parent.RelateName);
            Directory.CreateDirectory(dir);
            string path = Path.Combine(dir, Name + ext);
            return new StreamWriter(path, false, Encoding.UTF8);
        }

        public void AddBean(Bean bean)
        {
            _Beans.Add(bean);
            IsChanged = true;
        }

        public void UpdateRelateName()
        {
            var tmp = File.RelateName;
            if (tmp.EndsWith(".xml"))
                tmp = tmp.Substring(0, tmp.Length - 4);
            RelateName = tmp.Replace(System.IO.Path.DirectorySeparatorChar, '.');
            TabPage tab = GridData?.View?.Parent as TabPage;
            if (null != tab)
                tab.Text = RelateName;
        }

        public Document(Documents.File file)
        {
            File = file;
            UpdateRelateName();
            BeanDefine = new BeanDefine(this, file.Name);
            Beans = new ReadOnlyCollection<Bean>(_Beans);
        }

        public void BuildGridData()
        {
            FormMain.Instance.FormError.RemoveErrorByGrid(GridData);
            GridData = new GridData(this);

            BeanDefine.BuildGridColumns(GridData, 0, new ColumnTag(ColumnTag.ETag.Normal), -1);

            var param = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.Grid };
            foreach (var bean in _Beans)
            {
                int insertIndex = GridData.RowCount;
                GridData.InsertRow(insertIndex);

                int colIndex = 0;
                if (bean.Update(GridData, GridData.GetRow(insertIndex), ref colIndex, 0, param))
                    break;
            }

            for (int i = 0; i < GridData.ColumnCount; ++i)
            {
                ColumnTag tag = GridData.GetColumn(i).ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        continue;
                }
                tag.BuildUniqueIndex(GridData, i);
            }
        }

        public void Close()
        {
            File.Close(this);
            GridData.View = null;
            FormMain.Instance.FormError.RemoveErrorByGrid(GridData);
            GridData = null;
        }

        public XmlDocument Xml { get; private set; }

        public void SaveIfChanged()
        {
            if (IsChanged)
                Save();
            IsChanged = false;
        }

        public void Save()
        {
            SaveAs(FileName, false, Property.DataOutputFlags.All);
        }

        public void SaveAs(string fileName, bool create, Property.DataOutputFlags flags)
        {
            XmlDocument xml = create ? null : Xml;

            if (null == xml)
            {
                xml = new XmlDocument();
                xml.AppendChild(xml.CreateElement("ZezeConfig"));
                if (false == create)
                    Xml = xml;
            }

            if (flags == Property.DataOutputFlags.All)
                BeanDefine.SaveAs(xml, xml.DocumentElement, create);

            for (int i = 0; i < _Beans.Count; ++i)
            {
                Bean b = _Beans[i];
                b.RowIndex = i;
                b.SaveAs(xml, xml.DocumentElement, create, flags);
            }

            using (TextWriter sw = new StreamWriter(fileName, false, Encoding.UTF8))
            {
                xml.Save(sw);
            }
        }

        public void LoadXmlFile()
        {
            if (null != Xml)
                throw new Exception("Duplicate Open Document for " + FileName);
            Xml = new XmlDocument();
            Xml.Load(FileName);
            XmlElement self = Xml.DocumentElement;
            if (false == self.Name.Equals("ZezeConfig"))
                throw new Exception("node name is not ZezeConfig");
            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "BeanDefine":
                        this.BeanDefine = new BeanDefine(this, e);
                        break;
                    case "bean":
                        _Beans.Add(new Bean(this, e));
                        break;
                    default:
                        throw new Exception("Unknown Element Name " + e.Name);
                }
            }
        }
    }

}
