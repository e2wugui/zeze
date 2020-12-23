using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Windows.Forms;
using System.Xml;

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
                if (!System.IO.Directory.Exists(home))
                    home = Environment.CurrentDirectory;

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
            // remove deleted directory.
            for (int i = Config.RecentHomes.Count - 1; i >= 0; --i)
            {
                string home = Config.RecentHomes[i];
                if (System.IO.Directory.Exists(home))
                {
                    continue;
                }
                Config.RecentHomes.RemoveAt(i);
            }

            FormSelectRecentHome select = new FormSelectRecentHome();
            select.StartPosition = FormStartPosition.CenterParent;
            select.Config = Config;
            if (DialogResult.OK != select.ShowDialog(this))
            {
                select.Dispose();
                Close();
                return;
            }
            Config.SetRecentHome(select.ComboBoxRecentHomes.Text);
            select.Dispose();
            SaveConfig();
            this.TopMost = true;
            this.BringToFront();
            this.TopMost = false;
        }

        private void LoadDocumentToView(DataGridView grid, Document doc)
        {
            doc.BeanDefine.BuildGridColumns(grid, 0, new ColumnTag(ColumnTag.ETag.Normal), -1, false);
            foreach (var bean in doc.Beans)
            {
                AddGridRow(grid);
                DataGridViewCellCollection cells = grid.Rows[grid.RowCount - 1].Cells;
                int colIndex = 0;
                if (bean.Update(grid, cells, ref colIndex, 0, Bean.EUpdate.Grid))
                    break;
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
            doc.IsChanged = true;
            if (e.RowIndex == grid.RowCount - 1) // is last row
            {
                doc.Beans.Add(new Bean(doc));
                AddGridRow(grid);
            }
            DataGridViewCellCollection cells = grid.Rows[e.RowIndex].Cells;
            int colIndex = e.ColumnIndex;
            doc.Beans[e.RowIndex].Update(grid, cells, ref colIndex, 0, Bean.EUpdate.Data);
        }

        public bool VerifyName(string name, bool showMsg = true)
        {
            if (name.Length == 0)
                throw new Exception("name cannot empty.");
            if (char.IsDigit(name[0]))
                throw new Exception("name cannot begin with number.");
            switch (name)
            {
                case "bean":
                case "list":
                case "BeanDefine":
                case "variable":
                    throw new Exception(name + " is reserved");
            }
            foreach (var c in name)
            {
                if (char.IsWhiteSpace(c) || c == '.')
                {
                    string err = "name cannot use WhiteSpace and '.'";
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
                    input.StartPosition = FormStartPosition.CenterParent;
                    while (true)
                    {
                        input.TextBoxVarName.Text = varName;
                        if (DialogResult.OK == input.ShowDialog(this))
                        {
                            varName = input.TextBoxVarName.Text;
                            if (false == VerifyName(varName))
                                continue;
                            if (null != tag.PathLast.Define.Parent.GetVariable(varName))
                            {
                                MessageBox.Show("新增变量(列)的名字已经存在或者为空。");
                                continue;
                            }

                            VarDefine varDefine = new VarDefine(tag.PathLast.Define.Parent)
                            {
                                Name = varName,
                                GridColumnValueWidth = 60,
                                Type = input.CheckBoxIsList.Checked ? "list" : "",
                                Value = input.TextBoxListRefBeanName.Text,
                            };
                            bool createRef = false;
                            if (input.CheckBoxIsList.Checked && varDefine.Value.Length == 0)
                            {
                                varDefine.Value = varDefine.FullName();
                                createRef = true;
                            }
                            tag.PathLast.Define.Parent.Variables.Add(varDefine);
                            tag.PathLast.Define.Parent.Document.IsChanged = true;

                            foreach (var tab in tabs.Controls)
                            {
                                DataGridView gridref = (DataGridView)((TabPage)tab).Controls[0];
                                gridref.SuspendLayout();
                                for (int c = 0; c < gridref.ColumnCount; ++c)
                                {
                                    ColumnTag tagref = (ColumnTag)gridref.Columns[c].Tag;
                                    if (tagref.Tag == ColumnTag.ETag.AddVariable
                                        && tagref.PathLast.Define.Parent == tag.PathLast.Define.Parent)
                                    {
                                        c += varDefine.BuildGridColumns(gridref, c,
                                            tagref.Parent(ColumnTag.ETag.Normal), -1, createRef);
                                        ((Document)gridref.Tag).IsChanged = true;
                                    }
                                }
                                gridref.ResumeLayout();
                            }
                        }
                        break;
                    }
                    input.Dispose();
                    break;

                case ColumnTag.ETag.ListEnd:
                    // add list item now
                    ColumnTag tagSeed = tag.Copy(ColumnTag.ETag.Normal);
                    tagSeed.PathLast.ListIndex = -tag.PathLast.ListIndex;
                    --tag.PathLast.ListIndex;
                    tag.PathLast.Define.Reference.BuildGridColumns(grid, columnIndex, tagSeed, -1, false);
                    //(grid.Tag as Document).IsChanged = true;
                    break;
            }
        }
        public void OnGridDoubleClick(object sender, DataGridViewCellMouseEventArgs e)
        {
            if (e.Button != MouseButtons.Left)
                return;
            if (e.ColumnIndex < 0)
                return;
            try
            {
                DataGridView grid = (DataGridView)sender;
                DoActionByColumnTag(grid, e.ColumnIndex, (ColumnTag)grid.Columns[e.ColumnIndex].Tag);
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        public void OnGridKeyDown(object sender, KeyEventArgs e)
        {
            DataGridView grid = (DataGridView)sender;
            if (grid.CurrentCell == null)
                return;

            try
            {
                switch (e.KeyCode)
                {
                    case Keys.Enter:
                        DoActionByColumnTag(grid, grid.CurrentCell.ColumnIndex,
                            (ColumnTag)grid.Columns[grid.CurrentCell.ColumnIndex].Tag);
                        break;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
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
            grid.CellMouseDown += OnCellMouseDown;

            TabPage tab = new TabPage();
            tab.Text = text;
            tab.Controls.Add(grid);
            return tab;
        }

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
            this.saveFileDialog1.InitialDirectory = Config.RecentHomes[0];
            this.saveFileDialog1.FileName = "";
            this.saveFileDialog1.Filter = "(*.xml)|*.xml";
            if (DialogResult.OK != this.saveFileDialog1.ShowDialog())
                return; // 取消保存，不关闭窗口
            string file = this.saveFileDialog1.FileName;
            if (!file.EndsWith(".xml"))
                file = file + ".xml";
            try
            {
                Document doc = new Document(this);
                doc.SetFileName(file);
                TabPage tab = NewTabPage(doc.RelateName);
                DataGridView grid = (DataGridView)tab.Controls[0];
                doc.Save();
                grid.Tag = doc;
                doc.Grid = grid;
                doc.BeanDefine.BuildGridColumns(grid, 0, new ColumnTag(ColumnTag.ETag.Normal), -1, false);
                AddGridRow(grid);

                Documents.Add(doc.RelateName, doc);
                tabs.Controls.Add(tab);
                tabs.SelectedTab = tab;
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        private Dictionary<string, Document> Documents = new Dictionary<string, Document>();

        private bool Save(Document doc)
        {
            try
            {
                if (doc.IsChanged)
                {
                    doc.Save();
                    doc.IsChanged = false;
                }
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
            if (null == tabs.SelectedTab)
                return;
            Save((Document)tabs.SelectedTab.Controls[0].Tag);
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
                        LoadDocumentToView(grid, doc);
                        tabs.Controls.Add(tab);
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
                    LoadDocumentToView(grid, doc);
                    tabs.Controls.Add(tab);
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
            foreach (var doc in Documents.Values)
            {
                if (false == Save(doc))
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

        public void OnCellMouseDown(object sender, DataGridViewCellMouseEventArgs e)
        {
            if (e.Button != MouseButtons.Right)
                return;

            int col = e.ColumnIndex >= 0 ? e.ColumnIndex : 0;
            int row = e.RowIndex >= 0 ? e.RowIndex : 0;
            DataGridView grid = sender as DataGridView;
            DataGridViewCell c = grid[col, row];
            if (!c.Selected)
            {
                c.DataGridView.CurrentCell = c;
            }
            contextMenuStrip1.Show(grid, grid.PointToClient(Cursor.Position));
        }
        private void deleteVariableColumnToolStripMenuItem_Click(object sender, EventArgs e)
        {
            if (tabs.SelectedTab == null)
                return;
            DataGridView grid = (DataGridView)tabs.SelectedTab.Controls[0];
            if (grid.CurrentCell == null)
                return;

            ColumnTag tag = (ColumnTag)grid.Columns[grid.CurrentCell.ColumnIndex].Tag;
            switch (tag.Tag)
            {
                case ColumnTag.ETag.AddVariable:
                    return;

                case ColumnTag.ETag.ListEnd:
                case ColumnTag.ETag.ListStart:
                case ColumnTag.ETag.Normal:
                    if (DialogResult.OK != MessageBox.Show("确定删除？所有引用该列的数据也会被删除。",
                        "确认", MessageBoxButtons.OKCancel))
                        return;
                    // delete data and column, all reference(opened grid).
                    foreach (var tab in tabs.Controls)
                    {
                        DataGridView gridref = (DataGridView)((TabPage)tab).Controls[0];
                        Document doc = (Document)gridref.Tag;
                        gridref.SuspendLayout();
                        for (int c = 0; c < gridref.ColumnCount; ++c)
                        {
                            ColumnTag tagref = (ColumnTag)gridref.Columns[c].Tag;
                            if (tagref.PathLast.Define == tag.PathLast.Define)
                            {
                                // delete data
                                for (int r = 0; r < gridref.RowCount - 1; ++r)
                                {
                                    DataGridViewCellCollection cells = gridref.Rows[r].Cells;
                                    int colref = c;
                                    doc.Beans[r].Update(gridref, cells, ref colref, 0, Bean.EUpdate.DeleteData);
                                }
                                // delete columns
                                switch (tagref.Tag)
                                {
                                    case ColumnTag.ETag.Normal:
                                        gridref.Columns.RemoveAt(c);
                                        --c;
                                        break;
                                    case ColumnTag.ETag.ListStart:
                                        int colListEnd = FindCloseListEnd(gridref, c);
                                        while (colListEnd >= c)
                                        {
                                            gridref.Columns.RemoveAt(colListEnd);
                                            --colListEnd;
                                        }
                                        --c;
                                        break;
                                    default:
                                        MessageBox.Show("ListEnd?");
                                        break;
                                }
                                doc.IsChanged = true;
                            }
                        }
                        gridref.ResumeLayout();
                    }
                    // delete define
                    tag.PathLast.Define.Delete();
                    break;
            }
        }

        private int FindCloseListEnd(DataGridView grid, int startColIndex)
        {
            int listStartCount = 1;
            for (int c = startColIndex + 1; c < grid.ColumnCount; ++c)
            {
                ColumnTag tag = (ColumnTag)grid.Columns[c].Tag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListEnd:
                        --listStartCount;
                        if (listStartCount == 0)
                            return c;
                        break;
                    case ColumnTag.ETag.ListStart:
                        ++listStartCount;
                        break;
                }
            }
            throw new Exception("List Not Closed.");
        }

        private int FindNextListEnd(DataGridView grid, int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex; c < grid.ColumnCount; ++c)
            {
                ColumnTag tag = (ColumnTag)grid.Columns[c].Tag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            --skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            ++skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListStart:
                        ++skipNestListCount;
                        break;
                    //case ColumnTag.ETag.AddVariable:
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        return c;
                }
            }
            return -1;
        }

        private int FindColumnBeanBegin(DataGridView grid, int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex - 1; c >= 0; --c)
            {
                ColumnTag tag = (ColumnTag)grid.Columns[c].Tag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            ++skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            --skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                        return c + 1;
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        ++skipNestListCount;
                        break;
                }
            }
            throw new Exception("FindColumnBeanBegin");
        }

        private int DoActionUntilBeanEnd(DataGridView grid, int colBeanBegin, int colListEnd, Action<int> action)
        {
            int skipNestListCount = 0;
            for (int c = colBeanBegin; c < colListEnd; ++c)
            {
                action(c);
                ColumnTag tag = (ColumnTag)grid.Columns[c].Tag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            --skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            ++skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListStart:
                        ++skipNestListCount;
                        break;
                    case ColumnTag.ETag.AddVariable:
                        return c + 1;
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        throw new Exception("DoActionUntilBeanEnd");
                }
            }
            return colListEnd;
        }

        private void deleteListItemToolStripMenuItem_Click(object sender, EventArgs e)
        {
            if (tabs.SelectedTab == null)
                return;

            DataGridView grid = (DataGridView)tabs.SelectedTab.Controls[0];
            if (grid.CurrentCell == null)
                return;

            ColumnTag tagSelected = (ColumnTag)grid.Columns[grid.CurrentCell.ColumnIndex].Tag;
            switch (tagSelected.Tag)
            {
                case ColumnTag.ETag.ListStart:
                case ColumnTag.ETag.ListEnd:
                    MessageBox.Show("请选择 List 中间的列。");
                    return;
                /*
                case ColumnTag.ETag.Normal:
                case ColumnTag.ETag.AddVariable:
                    break;
                */
            }

            int colListEnd = FindNextListEnd(grid, grid.CurrentCell.ColumnIndex);
            if (colListEnd < 0)
            {
                MessageBox.Show("请选择 List 中间的列。");
                return; // not in list
            }

            Document doc = grid.Tag as Document;
            ColumnTag tagListEnd = (ColumnTag)grid.Columns[colListEnd].Tag;
            int pathEndIndex = tagListEnd.Path.Count - 1;
            int colBeanBegin = FindColumnBeanBegin(grid, grid.CurrentCell.ColumnIndex);
            int listIndex = tagSelected.Path[pathEndIndex].ListIndex;

            // delete data(list item)
            for (int row = 0; row < grid.RowCount - 1; ++row)
            {
                doc.Beans[row].GetVarData(0, tagSelected, pathEndIndex)?.DeleteBeanAt(listIndex);
            }

            if (tagListEnd.PathLast.ListIndex == -1)
            {
                // 只有一个item，仅删除数据，不需要删除Column。需要更新grid。
                for (int row = 0; row < grid.RowCount - 1; ++row)
                {
                    DoActionUntilBeanEnd(grid, colBeanBegin, colListEnd,
                        (int col) =>
                        {
                            switch ((grid.Columns[col].Tag as ColumnTag).Tag)
                            {
                                case ColumnTag.ETag.Normal:
                                    grid[col, row].Value = null;
                                    break;
                            }
                        });
                }
                return;
            }

            grid.SuspendLayout();
            {
                // delete column
                List<int> colDelete = new List<int>();
                DoActionUntilBeanEnd(grid, colBeanBegin, colListEnd, (int col) => colDelete.Add(col));
                for (int i = colDelete.Count - 1; i >= 0; --i)
                    grid.Columns.RemoveAt(colDelete[i]);
                colListEnd -= colDelete.Count;
            }
            grid.ResumeLayout();

            // reduce ListIndex In Current List after deleted item.
            while (colBeanBegin < colListEnd)
            {
                colBeanBegin = DoActionUntilBeanEnd(grid, colBeanBegin, colListEnd,
                    (int col) =>
                    {
                        ColumnTag tagReduce = (ColumnTag)grid.Columns[col].Tag;
                        --tagReduce.Path[pathEndIndex].ListIndex;
                    });
            }
            ++tagListEnd.PathLast.ListIndex;
        }
    }
}
