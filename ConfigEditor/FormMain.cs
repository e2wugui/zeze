using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormMain : Form
    {
        public class EditorConfig
        {
            public IList<string> RecentHomes { get; set; }

            public string GetHome()
            {
                return RecentHomes[0];
            }

            public void SetRecentHome(string home)
            {
                RecentHomes.Insert(0, home);
                IList<string> distinct = new List<string>();
                foreach (var r in RecentHomes.Distinct())
                    distinct.Add(r);
                RecentHomes = distinct;
                while (RecentHomes.Count > 10)
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
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
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
            this.folderBrowserDialog.Description = "选择配置所在的目录(Home)";
            while (Config.RecentHomes.Count > 0)
            {
                string first = Config.RecentHomes.First();
                if (System.IO.Directory.Exists(first))
                {
                    this.folderBrowserDialog.SelectedPath = first;
                    break;
                }
                Config.RecentHomes.RemoveAt(0);
            }
            this.folderBrowserDialog.ShowDialog();
            Config.SetRecentHome(this.folderBrowserDialog.SelectedPath.Length > 0
                ? this.folderBrowserDialog.SelectedPath : Environment.CurrentDirectory);
            SaveConfig();
            this.TopMost = true;
            this.BringToFront();
            this.TopMost = false;
        }

        private TabPage NewTabPage(string text)
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
            return tab;
        }

        private int NewFileSeed = 0;

        private void newButton_Click(object sender, EventArgs e)
        {
            ++NewFileSeed;
            TabPage tab = NewTabPage("NewFile_" + NewFileSeed);
            tabs.Controls.Add(tab);
            tab.Select();
        }

        private Dictionary<string, Document> Documents = new Dictionary<string, Document>();

        private bool Save(TabPage tab)
        {
            try
            {
                if (tab == null)
                    return true;

                System.Collections.IEnumerator ie = tab.Controls.GetEnumerator();
                if (!ie.MoveNext())
                    return true;
                DataGridView grid = (DataGridView)ie.Current;
                Document doc = (Document)grid.Tag;
                if (null == doc)
                {
                    switch (MessageBox.Show("是否保存新建文件？ " + tab.Text, "提示", MessageBoxButtons.YesNoCancel))
                    {
                        case DialogResult.Yes:
                            break;
                        case DialogResult.Cancel:
                            return false;
                        case DialogResult.No:
                            return true;
                    }
                    this.saveFileDialog1.InitialDirectory = Config.RecentHomes[0];
                    this.saveFileDialog1.FileName = tab.Text;
                    this.saveFileDialog1.Filter = "(*.xml)|*.xml";
                    if (DialogResult.OK != this.saveFileDialog1.ShowDialog())
                        return false; // 取消保存，不关闭窗口

                    string file = this.saveFileDialog1.FileName;
                    if (!file.EndsWith(".xml"))
                        file = file + ".xml";

                    doc = new Document(this, file);
                    Documents.Add(doc.RelateName, doc);
                    doc.Save();
                    grid.Tag = doc;
                    doc.Grid = grid;
                    return true;
                }
                doc.Save();
                return true;
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
            return false;
        }

        private void saveButton_Click(object sender, EventArgs e)
        {
            Save(tabs.SelectedTab);
        }

        private Document OpenDocument(string path, string[]refbeans, int offset)
        {
            Document doc = new Document(this, path);
            doc.Open();
            Documents.Add(doc.RelateName, doc);
            return doc;
        }

        public Document OpenDocument(string relatePath)
        {
            string[] relates = relatePath.Split('.');
            string path = Config.GetHome();

            for (int i = 0; i < relates.Length; ++i)
            {
                path = System.IO.Path.Combine(path, relates[i]);
                if (System.IO.File.Exists(path)) // 目录的话，这个判断会失败
                {
                    return OpenDocument(path, relates, i + 1);
                }
            }
            throw new Exception("Open Document Error: " + relatePath);
        }

        private void openButton_Click(object sender, EventArgs e)
        {
            try
            {
                this.openFileDialog1.InitialDirectory = Config.RecentHomes[0];
                this.openFileDialog1.FileName = "";
                this.openFileDialog1.Filter = "(*.xml)|*.xml";
                if (DialogResult.OK != this.openFileDialog1.ShowDialog())
                    return;
                Document doc = new Document(this, this.openFileDialog1.FileName);
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
                        // used by foreign, no view
                        TabPage tab = NewTabPage(odoc.RelateName);
                        DataGridView grid = (DataGridView)tab.Controls[0];
                        grid.SuspendLayout();
                        tabs.Controls.Add(tab);
                        tab.Select();
                        grid.ResumeLayout();
                        odoc.Grid = grid;
                        grid.Tag = doc;
                    }
                }
                else
                {
                    TabPage tab = NewTabPage(doc.RelateName);
                    DataGridView grid = (DataGridView)tab.Controls[0];
                    doc.Open();
                    Documents.Add(doc.RelateName, doc);
                    grid.SuspendLayout();
                    tabs.Controls.Add(tab);
                    tab.Select();
                    grid.ResumeLayout();
                    grid.Tag = doc;
                    doc.Grid = grid;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        private bool SaveAll()
        {
            System.Collections.IEnumerator ie = tabs.Controls.GetEnumerator();
            while (ie.MoveNext())
            {
                if (false == Save((TabPage)ie.Current))
                    return false;
            }
            return true;
        }

        private void saveAllButton_Click(object sender, EventArgs e)
        {
            SaveAll();
        }

        private void FormMain_FormClosing(object sender, FormClosingEventArgs e)
        {
            e.Cancel = false == SaveAll();
        }

        private void buildButton_Click(object sender, EventArgs e)
        {
            SaveAll();
            // TODO 遍历Home所有配置文件，并且生成代码等。
        }
    }
}
