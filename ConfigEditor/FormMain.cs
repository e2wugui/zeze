﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Text.Json;
using System.Xml;

namespace ConfigEditor
{
    public partial class FormMain : Form
    {
        public class EditorConfig
        {
            public IList<string> RecentHomes { get; set; }

            public string Home { get { return RecentHomes[0]; } }

            public void SetRecentHome(string home)
            {
                RecentHomes.Insert(0, home);
                IList<string> distinct = new List<string>();
                foreach (var r in RecentHomes.Distinct())
                    distinct.Add(r);
                RecentHomes = distinct;
                if (RecentHomes.Count > 10)
                    RecentHomes.RemoveAt(RecentHomes.Count - 1);
            }
        }

        public EditorConfig Config { get; private set; }
        public FormMain()
        {
            InitializeComponent();
            LoadConfig();
        }

        private string GetConfigFileFullName()
        {
            string localappdata = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string confighome = System.IO.Path.Combine(localappdata, "zeze");
            System.IO.Directory.CreateDirectory(confighome);
            return System.IO.Path.Combine(confighome, "ConfigEditor.json");
        }

        private void LoadConfig()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes(GetConfigFileFullName()));
                Config = JsonSerializer.Deserialize<EditorConfig>(json);
            }
            catch (Exception)
            {
                // skip error
            }
            if (null == Config)
                Config = new EditorConfig() { RecentHomes = new List<string>() };
        }

        private void SaveConfig()
        {
            var options = new JsonSerializerOptions { WriteIndented = true };
            System.IO.File.WriteAllBytes(GetConfigFileFullName(), JsonSerializer.SerializeToUtf8Bytes(Config, options));
        }

        private void FormMain_Load(object sender, EventArgs e)
        {
            while (Config.RecentHomes.Count > 0)
            {
                string first = Config.RecentHomes.First();
                if (System.IO.File.Exists(first))
                {
                    this.folderBrowserDialog.SelectedPath = first;
                    break;
                }
                Config.RecentHomes.RemoveAt(0);
            }
            this.folderBrowserDialog.ShowDialog();
            Config.SetRecentHome(this.folderBrowserDialog.SelectedPath);
            SaveConfig();
        }

        private DataGridView newGrid(string text)
        {
            DataGridView grid = new DataGridView();
            grid.AllowUserToAddRows = false;
            grid.AllowUserToDeleteRows = false;
            grid.Anchor = ((AnchorStyles)((((AnchorStyles.Top | AnchorStyles.Bottom) | AnchorStyles.Left) | AnchorStyles.Right)));
            grid.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            grid.Location = new Point(0, 0);
            grid.Margin = new Padding(2);
            grid.MultiSelect = false;
            grid.Name = "Grid";
            grid.RowHeadersWidth = 82;
            grid.RowTemplate.Height = 37;
            //gird.Size = new Size(848, 476);
            grid.TabIndex = 0;

            TabPage tab = new TabPage();
            tab.Text = text;
            tab.Controls.Add(grid);
            tabs.Controls.Add(tab);
            return grid;
        }

        private void newButton_Click(object sender, EventArgs e)
        {
            newGrid("NewFile");
        }

        enum Type
        {
            NotDefined = 0,
            Int = 1,
            Long = 2,
            String = 3,
            Bean = 4,
            List = 5,
            Map = 6,
        }

        class Variable
        {
            public string Name { get; set; }
            public Type Type { get; set; }
            public string Foreign { get; set; }
            public string Properties { get; set; } // unique;
        }

        class Bean
        {
            public string Name { get; set; }
            public List<Variable> Variables { get; } = new List<Variable>();
        }

        class Document
        {
            public string FileName { get; private set; }
            public string RelateName { get; private set; }

            private Document()
            { 
            }

            public static Document New(FormMain fm, string fileName)
            {
                Document doc = new Document();
                doc.FileName = System.IO.Path.GetFullPath(fileName);
                if (!doc.FileName.StartsWith(fm.Config.Home))
                {
                    MessageBox.Show("文件必须在配置Home目录下");
                    return null;
                }
                string relate = doc.FileName.Substring(fm.Config.Home.Length);
                string []relates = relate.Split(new char[] { '/', '\\' });
                doc.RelateName = relates[0];
                for (int i = 1; i < relates.Length; ++i)
                {
                    doc.RelateName = doc.RelateName + '/' + System.IO.Path.GetFileNameWithoutExtension(relates[i]);
                }
                return doc;
            }

            public XmlDocument Xml { get; } = new XmlDocument();

            public DataGridView Grid { get; private set; }
            public void BuildNew(DataGridView grid)
            {
                Grid = grid;
            }

            public void Open(DataGridView grid)
            {
                Xml.Load(FileName);
                Grid = grid;
            }
        }

        private Dictionary<string, Document> Documents = new Dictionary<string, Document>();

        private void save(TabPage tab)
        {
            if (tab == null)
                return;

            System.Collections.IEnumerator ie = tab.Controls.GetEnumerator();
            if (!ie.MoveNext())
                return;
            DataGridView grid = (DataGridView)tab.Controls.GetEnumerator().Current;

            Document doc = (Document)grid.Tag;
            if (null == doc)
            {
                this.openFileDialog1.InitialDirectory = Config.RecentHomes[0];
                this.openFileDialog1.FileName = "";
                this.openFileDialog1.Filter = "(*.xml)|*.xml";
                if (DialogResult.OK != this.openFileDialog1.ShowDialog())
                    return;
                string file = this.openFileDialog1.FileName;
                if (!file.EndsWith(".xml"))
                    file = file + ".xml";

                doc = Document.New(this, file);
                if (null != doc)
                {
                    doc.BuildNew(grid);
                    doc.Xml.Save(file);
                    grid.Tag = doc;
                    Documents.Add(doc.RelateName, doc);
                }
                return;
            }
            doc.Xml.Save(doc.FileName);
        }

        private void saveButton_Click(object sender, EventArgs e)
        {
            save(tabs.SelectedTab);
        }

        private void openButton_Click(object sender, EventArgs e)
        {
            this.openFileDialog1.InitialDirectory = Config.RecentHomes[0];
            this.openFileDialog1.FileName = "";
            this.openFileDialog1.Filter = "(*.xml)|*.xml";
            if (DialogResult.OK != this.openFileDialog1.ShowDialog())
                return;
            Document doc = Document.New(this, this.openFileDialog1.FileName);
            if (null == doc)
                return;

            if (Documents.TryGetValue(doc.RelateName, out var odoc))
            {
                if (odoc.Grid != null)
                {
                    // has opened
                    TabPage tab = (TabPage)odoc.Grid.Parent;
                    tab.Select();
                }
                else
                {
                    // used by foreign, not opened
                    DataGridView grid = newGrid(odoc.RelateName);
                    odoc.Open(grid);
                    grid.Tag = doc;
                }
            }
            else 
            {
                DataGridView grid = newGrid(doc.RelateName);
                doc.Open(grid);
                grid.Tag = doc;
                Documents.Add(doc.RelateName, doc);
            }
        }

        private void saveAll()
        {
            System.Collections.IEnumerator ie = tabs.Controls.GetEnumerator();
            while (ie.MoveNext())
                save((TabPage)ie.Current);
        }

        private void saveAllButton_Click(object sender, EventArgs e)
        {
            saveAll();
        }

        private void FormMain_FormClosing(object sender, FormClosingEventArgs e)
        {
            saveAll();
        }
    }
}
