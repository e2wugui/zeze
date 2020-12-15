using System;
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
            if (Config.RecentHomes.Count > 0)
                this.folderBrowserDialog.SelectedPath = Config.RecentHomes.First();
            this.folderBrowserDialog.ShowDialog();
            Config.SetRecentHome(this.folderBrowserDialog.SelectedPath);
            SaveConfig();
        }

        private DataGridView newGrid()
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
            tab.Text = "NewFile";
            tab.Controls.Add(grid);
            tabs.Controls.Add(tab);
            return grid;
        }

        private void newButton_Click(object sender, EventArgs e)
        {
            newGrid();
        }

        class Document
        {
            public string FileName { get; set; }
            public XmlDocument Xml { get; set; }

            public void BuildNew(DataGridView grid)
            {
                Xml = new XmlDocument();

            }

            public void Open(DataGridView grid)
            {
                Xml = new XmlDocument();
                Xml.Load(FileName);

            }
        }

        private void saveButton_Click(object sender, EventArgs e)
        {
            if (tabs.SelectedTab == null)
                return;

            if (!tabs.SelectedTab.Controls.GetEnumerator().MoveNext())
                return;

            DataGridView grid = (DataGridView)tabs.SelectedTab.Controls.GetEnumerator().Current;
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

                doc = new Document() { FileName = file };
                doc.BuildNew(grid);
                doc.Xml.Save(file);
                grid.Tag = doc;
                return;
            }
            doc.Xml.Save(doc.FileName);
        }

        private void openButton_Click(object sender, EventArgs e)
        {
            this.openFileDialog1.InitialDirectory = Config.RecentHomes[0];
            this.openFileDialog1.FileName = "";
            this.openFileDialog1.Filter = "(*.xml)|*.xml";
            if (DialogResult.OK != this.openFileDialog1.ShowDialog())
                return;
            Document doc = new Document() { FileName = this.openFileDialog1.FileName };
            DataGridView grid = newGrid();
            doc.Open(grid);
            grid.Tag = doc;
        }
    }
}
