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

        private void LoadDocumentToView(DataGridView grid, Document doc)
        {
            doc.BeanDefine.BuildGridColumns(grid, 0, new ColumnTag(doc.BeanDefine, 0), false);
            foreach (var bean in doc.Beans)
            {
                AddGridRow(grid);
                DataGridViewCellCollection cells = grid.Rows[grid.RowCount - 1].Cells;
                for (int colIndex = 0; colIndex < grid.ColumnCount; ++colIndex) // ColumnCount maybe change in loop
                {
                    ColumnTag tag = (ColumnTag)grid.Columns[colIndex].Tag;
                    Bean.VarData data = bean.SetVarDataToGrid(tag, 0);
                    if (null != data)
                        cells[colIndex].Value = data.Value;
                }
            }
            AddGridRow(grid);
        }

        public void OnGridCellEndEdit(object sender, DataGridViewCellEventArgs e)
        {
            DataGridView grid = (DataGridView)sender;
            DataGridViewColumn col = grid.Columns[e.ColumnIndex];
            ColumnTag tag = (ColumnTag)col.Tag;
            if (ColumnTag.ETag.Normal != tag.Tag)
                return; // 不可能。特殊列都是不可编辑的。

            Document doc = (Document)grid.Tag;
            ((TabPage)grid.Parent).Tag = 1; // setup changed
            if (e.RowIndex == grid.RowCount - 1) // is last row
            {
                doc.Beans.Add(new Bean(doc));
                AddGridRow(grid);
            }
            string value = (string)grid.Rows[e.RowIndex].Cells[e.ColumnIndex].Value;
            var varData = doc.Beans[e.RowIndex].SetVarDataToGrid(tag, 0, true);
            varData.Value = value;
        }

        public bool VerifyName(string name, bool showMsg = true)
        {
            foreach (var c in name)
            {
                if (char.IsWhiteSpace(c) || c == '.')
                {
                    string err = "Config FileName and path cannot use WhiteSpace and '.'";
                    if (showMsg)
                        MessageBox.Show(err);
                    else
                        throw new Exception(err);
                    return false;
                }
            }
            return true;
        }

        private void DoActionByColumnTag(DataGridView grid, int columnIndex, ColumnTag tag)
        {
            switch (tag.Tag)
            {
                case ColumnTag.ETag.AddVariable:
                    string varName = "";
                    FormInputVarDefine input = new FormInputVarDefine();
                    while (true)
                    {
                        input.TextBoxVarName.Text = varName;
                        if (DialogResult.OK == input.ShowDialog(this))
                        {
                            varName = input.TextBoxVarName.Text;
                            if (null != tag.BeanDefine.GetVariable(varName))
                            {
                                MessageBox.Show("新增变量(列)的名字已经存在。");
                                continue;
                            }
                            if (false == VerifyName(varName))
                                continue;

                            VarDefine varDefine = new VarDefine(tag.BeanDefine) { Name = varName, GridColumnValueWidth = 60 };
                            varDefine.Type = input.CheckBoxIsList.Checked ? "list" : "";
                            varDefine.Value = input.TextBoxListRefBeanName.Text;
                            if (varDefine.Value.Length == 0) // TODO，需要检测引用名字是否存在。
                                varDefine.Value = varDefine.FullName();
                            tag.BeanDefine.Variables.Add(varDefine);
                            // TODO 遍历所有打开的grid，查找所有对当前BeanDefine的引用，全部更新列。
                            varDefine.BuildGridColumns(grid, columnIndex, tag.Copy(ColumnTag.ETag.Normal), true);
                            ((TabPage)grid.Parent).Tag = 1; // setup changed
                        }
                        break;
                    }
                    input.Dispose();
                    break;

                case ColumnTag.ETag.ListEnd:
                    // TODO add list item now
                    break;
            }
        }
        public void OnGridDoubleClick(object sender, DataGridViewCellMouseEventArgs e)
        {
            if (e.Button != MouseButtons.Left)
                return;

            DataGridView grid = (DataGridView)sender;
            DoActionByColumnTag(grid, e.ColumnIndex, (ColumnTag)grid.Columns[e.ColumnIndex].Tag);
        }

        public void OnGridKeyDown(object sender, KeyEventArgs e)
        {
            DataGridView grid = (DataGridView)sender;
            if (grid.CurrentCell == null)
                return;

            switch (e.KeyCode)
            {
                case Keys.Enter:
                    DoActionByColumnTag(grid, grid.CurrentCell.ColumnIndex,
                        (ColumnTag)grid.Columns[grid.CurrentCell.ColumnIndex].Tag);
                    break;
            }
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
            grid.RowHeadersWidth = 40;
            grid.RowTemplate.Height = 18;
            //gird.Size = new Size(848, 476);
            grid.TabIndex = 0;

            grid.CellEndEdit += OnGridCellEndEdit;
            grid.CellMouseDoubleClick += OnGridDoubleClick;
            grid.KeyDown += OnGridKeyDown;

            TabPage tab = new TabPage();
            tab.Text = text;
            tab.Controls.Add(grid);
            return tab;
        }

        private int NewFileSeed = 0;

        private void AddGridRow(DataGridView grid)
        {
            grid.Rows.Add(); // prepare row to add data
            DataGridViewCellCollection cells = grid.Rows[grid.RowCount - 1].Cells;
            for (int colIndex = 0; colIndex < grid.ColumnCount; ++colIndex) // ColumnCount maybe change in loop
            {
                DataGridViewColumn col = (DataGridViewColumn)grid.Columns[colIndex];
                switch (((ColumnTag)(col.Tag)).Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        cells[colIndex].Value = col.HeaderText;
                        break;
                }
            }
        }

        private void newButton_Click(object sender, EventArgs e)
        {
            ++NewFileSeed;
            TabPage tab = NewTabPage("NewFile_" + NewFileSeed);
            tabs.Controls.Add(tab);
            Document doc = new Document(this);
            DataGridView grid = (DataGridView)tab.Controls[0];
            grid.Tag = doc;
            doc.Grid = grid;
            doc.BeanDefine.BuildGridColumns(grid, 0, new ColumnTag(doc.BeanDefine, 0), false);
            AddGridRow(grid);
            tabs.SelectedTab = tab;
        }

        private Dictionary<string, Document> Documents = new Dictionary<string, Document>();

        private bool Save(TabPage tab)
        {
            try
            {
                if (tab == null || tab.Tag == null) // Tag changed
                    return true;

                DataGridView grid = (DataGridView)tab.Controls[0];
                Document doc = (Document)grid.Tag;
                if (null == doc.FileName)
                {
                    switch (MessageBox.Show("是否保存文件？ " + tab.Text, "提示", MessageBoxButtons.YesNoCancel))
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

                    doc.SetFileName(file);
                    Documents.Add(doc.RelateName, doc);
                    doc.Save();
                    grid.Tag = doc;
                    doc.Grid = grid;
                    tab.Tag = null;
                    return true;
                }
                doc.Save();
                tab.Tag = null;
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

        private Document OpenDocument(string path, string[]refbeans, int offset, out BeanDefine define, bool createRefBeanIfNotExist)
        {
            Document doc = new Document(this);
            doc.SetFileName(path);
            if (Documents.TryGetValue(doc.RelateName, out var exist))
            {
                define = exist.BeanDefine.Search(refbeans, offset, createRefBeanIfNotExist);
                return exist;
            }

            doc.Open();
            Documents.Add(doc.RelateName, doc);
            define = doc.BeanDefine.Search(refbeans, offset, createRefBeanIfNotExist);
            return doc;
        }

        public Document OpenDocument(string relatePath, out BeanDefine define, bool createRefBeanIfNotExist)
        {
            string[] relates = relatePath.Split('.');
            string path = Config.GetHome();

            for (int i = 0; i < relates.Length; ++i)
            {
                path = System.IO.Path.Combine(path, relates[i]);
                if (System.IO.Directory.Exists(path)) // is directory
                    continue;
                return OpenDocument(path + ".xml", relates, i + 1, out define, createRefBeanIfNotExist);
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
                Document doc = new Document(this);
                doc.SetFileName(this.openFileDialog1.FileName);
                if (Documents.TryGetValue(doc.RelateName, out var odoc))
                {
                    if (odoc.Grid != null)
                    {
                        // has opened
                        TabPage tab = (TabPage)odoc.Grid.Parent;
                        tabs.SelectedTab = tab;
                    }
                    else
                    {
                        // used by foreign, no view
                        TabPage tab = NewTabPage(odoc.RelateName);
                        DataGridView grid = (DataGridView)tab.Controls[0];
                        grid.SuspendLayout();
                        tabs.Controls.Add(tab);
                        LoadDocumentToView(grid, doc);
                        grid.ResumeLayout();
                        tabs.SelectedTab = tab;
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
                    LoadDocumentToView(grid, doc);
                    grid.ResumeLayout();
                    tabs.SelectedTab = tab;
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
