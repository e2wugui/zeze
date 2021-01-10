using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class Document
    {
        public Documents.File File { get; }
        public string FileName => File.AbsoluteName;
        public string RelateName { get; }
        public string Name => File.Name;
        public string NamePinyin => Tools.ToPinyin(Name);
        public BeanDefine BeanDefine { get; private set; } // bean in this file

        public List<Bean> Beans { get; } = new List<Bean>();
        public GridData GridData { get; set; }

        public bool IsChanged { get; set; } = false;

        public static string NamespacePrefix { get; set; } = "Config";

        // 不包含文档名，仅包含目录名。
        public string Namespace
        {
            get
            {
                string docpath = File.Parent.RelateName;
                if (string.IsNullOrEmpty(docpath))
                    return NamespacePrefix;
                return NamespacePrefix + "." + docpath;
            }
        }

        public StreamWriter OpenStreamWriter(string srcHome, string ext)
        {
            string dir = Path.Combine(srcHome, NamespacePrefix, File.Parent.RelateName);
            Directory.CreateDirectory(dir);
            string path = Path.Combine(dir, Name + ext);
            return new StreamWriter(path, false, Encoding.UTF8);
        }

        public Document(Documents.File file)
        {
            File = file;
            RelateName = file.RelateName.Replace(System.IO.Path.DirectorySeparatorChar, '.');
            BeanDefine = new BeanDefine(this);
            BeanDefine.Name = file.Name;
            GridData = new GridData(this);
        }

        public void BuildGridData()
        {
            GridData.Clear();

            BeanDefine.BuildGridColumns(GridData, 0, new ColumnTag(ColumnTag.ETag.Normal), -1);

            var param = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.Grid };
            foreach (var bean in Beans)
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

            GridData.VerifyAll();
        }

        public void Close()
        {
            File.Close(this);
            GridData.View = null;
            FormMain.Instance.FormError.RemoveErrorByGrid(GridData);
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

            for (int i = 0; i < Beans.Count; ++i)
            {
                Bean b = Beans[i];
                b.RowIndex = i;
                b.SaveAs(xml, xml.DocumentElement, create, flags);
            }

            using (TextWriter sw = new StreamWriter(fileName, false, Encoding.UTF8))
            {
                xml.Save(sw);
            }
        }

        public void Open()
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
                        Beans.Add(new Bean(this, e));
                        break;
                    default:
                        throw new Exception("Unknown Element Name " + e.Name);
                }
            }
        }
    }

}
