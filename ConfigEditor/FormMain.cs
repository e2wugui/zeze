using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Text.Json;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormMain : Form
    {
        public EditorConfig ConfigEditor { get; private set; }
        public ProjectConfig ConfigProject { get; private set; }

        public FormMain()
        {
            InitializeComponent();
            LoadConfigEditor();
            PropertyManager = new Property.Manager();
            FormError = new FormError() { FormMain = this };
            FormPopupListBox =  new FormPopupListBox() { FormMain = this };
        }

        private string GetConfigFileFullName()
        {
            string localappdata = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string confighome = System.IO.Path.Combine(localappdata, "zeze");
            System.IO.Directory.CreateDirectory(confighome);
            return System.IO.Path.Combine(confighome, "ConfigEditor.json");
        }

        private void LoadConfigEditor()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes(GetConfigFileFullName()));
                ConfigEditor = JsonSerializer.Deserialize<EditorConfig>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == ConfigEditor)
                ConfigEditor = new EditorConfig() { RecentHomes = new List<string>() };
        }

        private void LoadConfigProject()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes(
                    System.IO.Path.Combine(ConfigEditor.GetHome(), "ProjectConfig.json")));
                ConfigProject = JsonSerializer.Deserialize<ProjectConfig>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == ConfigProject)
                ConfigProject = new ProjectConfig();
        }

        private void SaveConfig()
        {
            var options = new JsonSerializerOptions { WriteIndented = true };
            System.IO.File.WriteAllBytes(GetConfigFileFullName(), JsonSerializer.SerializeToUtf8Bytes(ConfigEditor, options));
            System.IO.File.WriteAllBytes(System.IO.Path.Combine(ConfigEditor.GetHome(), "ProjectConfig.json"),
                JsonSerializer.SerializeToUtf8Bytes(ConfigProject, options));
        }

        private bool LoadCancel = false;

        private void FormMain_Load(object sender, EventArgs e)
        {
            // remove deleted directory.
            for (int i = ConfigEditor.RecentHomes.Count - 1; i >= 0; --i)
            {
                string home = ConfigEditor.RecentHomes[i];
                if (System.IO.Directory.Exists(home))
                {
                    continue;
                }
                ConfigEditor.RecentHomes.RemoveAt(i);
            }

            FormSelectRecentHome select = new FormSelectRecentHome();
            select.InitComboRecentHomes(ConfigEditor);
            if (DialogResult.OK != select.ShowDialog(this))
            {
                select.Dispose();
                LoadCancel = true;
                Close();
                return;
            }
            ConfigEditor.SetRecentHome(select.ComboBoxRecentHomes.Text);
            this.Text = select.ComboBoxRecentHomes.Text;
            select.Dispose();
            Environment.CurrentDirectory = ConfigEditor.GetHome();
            LoadConfigProject();

            if (ConfigEditor.FormMainLocation != null)
                this.Location = ConfigEditor.FormMainLocation;
            if (ConfigEditor.FormMainSize != null)
                this.Size = ConfigEditor.FormMainSize;
            this.WindowState = ConfigEditor.FormMainState;

            this.TopMost = true;
            this.BringToFront();
            this.TopMost = false;
        }

        public HashSet<DataGridView> ReloadGridsAfterFormDefineClosed { get; } = new HashSet<DataGridView>();

        public void ReloadAllGridIfContains(VarDefine var)
        {
            foreach (var tab in tabs.Controls)
            {
                DataGridView gridref = (DataGridView)((TabPage)tab).Controls[0];
                for (int i = 0; i < gridref.ColumnCount; ++i)
                {
                    ColumnTag tagref = gridref.Columns[i].Tag as ColumnTag;
                    if (tagref.PathLast.Define == var)
                    {
                        ReloadGridsAfterFormDefineClosed.Add(gridref);
                        break;
                    }
                }
            }
        }

        private void LoadDocumentToView(DataGridView grid, Document doc)
        {
            grid.SuspendLayout();

            grid.Columns.Clear();
            grid.Rows.Clear();

            doc.BeanDefine.BuildGridColumns(grid, 0, new ColumnTag(ColumnTag.ETag.Normal), -1);

            var param = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.Grid };
            foreach (var bean in doc.Beans)
            {
                AddGridRow(grid);
                DataGridViewCellCollection cells = grid.Rows[grid.RowCount - 1].Cells;
                int colIndex = 0;
                if (bean.Update(grid, cells, ref colIndex, 0, param))
                    break;
            }

            AddGridRow(grid);

            for (int i = 0; i < grid.ColumnCount; ++i)
            {
                ColumnTag tag = grid.Columns[i].Tag as ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        continue;
                }
                tag.BuildUniqueIndex(grid, i);
            }
            VerifyAll(grid);
            grid.ResumeLayout();
        }

        public void VerifyAll(DataGridView grid)
        {
            try
            {
                FormError.RemoveErrorByGrid(grid);

                int skipLastRow = grid.RowCount - 1;
                for (int rowIndex = 0; rowIndex < skipLastRow; ++rowIndex)
                {
                    for (int colIndex = 0; colIndex < grid.ColumnCount; ++colIndex)
                    {
                        ColumnTag tag = grid.Columns[colIndex].Tag as ColumnTag;

                        if (tag.Tag != ColumnTag.ETag.Normal)
                            continue;

                        DataGridViewCell cell = grid[colIndex, rowIndex];
                        string newValue = cell.Value as string;
                        if (newValue == null)
                            newValue = "";
                        var param = new Property.VerifyParam()
                        {
                            FormMain = this,
                            Grid = grid,
                            ColumnIndex = colIndex,
                            RowIndex = rowIndex,
                            ColumnTag = tag,
                            NewValue = newValue,
                        };

                        foreach (var p in tag.PathLast.Define.PropertiesList)
                        {
                            p.VerifyCell(param);
                        }
                        tag.PathLast.Define.Verify(param);
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        public void OnGridCellValidating(object sender, DataGridViewCellValidatingEventArgs e)
        {
            // 编辑的时候仅使用文本，允许输入任何数据。所以验证肯定通过。
            // 使用这个事件是为了得到 oldValue 做一些处理。
            // 这里以后需要真正的校验并且cancel的话，需要注意不要影响下面的代码。
            // 问题：CurrentCell 改变的时候，即时没有在编辑模式，原来的Cell也会触发这个事件。
            DataGridView grid = (DataGridView)sender;
            if (false == grid.IsCurrentCellInEditMode)
                return;

            DataGridViewColumn col = grid.Columns[e.ColumnIndex];
            ColumnTag tag = (ColumnTag)col.Tag;
            if (ColumnTag.ETag.Normal != tag.Tag)
                return;

            string newValue = e.FormattedValue as string;

            if (tag.PathLast.Define.Type == VarDefine.EType.Enum)
            {
                e.Cancel = true;
                if (null != Tools.VerifyName(newValue, CheckNameType.ShowMsg))
                    return;
                if (false == tag.PathLast.Define.Parent.EnumDefines.TryGetValue(tag.PathLast.Define.Name, out var enumDefine))
                {
                    MessageBox.Show("Error EnumDefine Not Found!!!");
                    return;
                }
                if (false == enumDefine.ValueMap.TryGetValue(newValue, out var _))
                {
                    switch (MessageBox.Show("输入的枚举名字不存在，是否添加进去？", "提示", MessageBoxButtons.YesNoCancel))
                    {
                        case DialogResult.Yes:
                            enumDefine.AddValue(new EnumDefine.ValueDefine(enumDefine, newValue, -1));
                            enumDefine.Parent.Parent.Document.IsChanged = true;
                            break;

                        case DialogResult.No:
                            break; // 继续，允许错误输入。

                        case DialogResult.Cancel:
                            return; // cancel
                    }
                }
                e.Cancel = false;
            }

            DataGridViewCell cell = grid[e.ColumnIndex, e.RowIndex];
            string oldValue = cell.Value as string; // maybe null
            if (newValue == null)
                newValue = "";

            tag.UpdateUniqueIndex(oldValue, newValue, cell);

            var param = new Property.VerifyParam()
            {
                FormMain = this,
                Grid = grid,
                ColumnIndex = e.ColumnIndex,
                RowIndex = e.RowIndex,
                ColumnTag = tag,
                OldValue = oldValue,
                NewValue = newValue,
            };
            foreach (var p in tag.PathLast.Define.PropertiesList)
            {
                p.VerifyCell(param);
            }
            tag.PathLast.Define.Verify(param);
        }

        public void OnGridCellEndEdit(object sender, DataGridViewCellEventArgs e)
        {
            HideFormHelp();

            DataGridView grid = (DataGridView)sender;
            DataGridViewColumn col = grid.Columns[e.ColumnIndex];
            ColumnTag tag = (ColumnTag)col.Tag;
            if (ColumnTag.ETag.Normal != tag.Tag)
                return; // 不可能。特殊列都是不可编辑的。

            Document doc = (Document)grid.Tag;
            bool added = false;
            DataGridViewCellCollection cells = grid.Rows[e.RowIndex].Cells;
            if (e.RowIndex == grid.RowCount - 1) // is last row
            {
                // 最后一行输入但是取消不添加行。
                if (string.IsNullOrEmpty(cells[e.ColumnIndex].Value as string))
                    return;

                doc.Beans.Add(new Bean(doc, "")); // root bean allow empty.
                AddGridRow(grid);
                added = true;
            }
            int colIndex = e.ColumnIndex;
            var param = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.Data };
            doc.Beans[e.RowIndex].Update(grid, cells, ref colIndex, 0, param);
            doc.IsChanged = true;

            if (added)
            {
                BuildUniqueIndexOnAddRow(grid, e.RowIndex);
            }
        }

        private void BuildUniqueIndexOnAddRow(DataGridView grid, int rowIndex)
        {
            for (int i = 0; i < grid.ColumnCount; ++i)
            {
                ColumnTag tag = grid.Columns[i].Tag as ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        continue;
                }
                DataGridViewCell cell = grid.Rows[rowIndex].Cells[i];
                tag.AddUniqueIndex(cell.Value as string, cell);
            }
        }

        public void UpdateWhenAddVariable(VarDefine var)
        {
            foreach (var tab in tabs.Controls)
            {
                DataGridView gridref = (DataGridView)((TabPage)tab).Controls[0];
                gridref.SuspendLayout();
                for (int c = 0; c < gridref.ColumnCount; ++c)
                {
                    ColumnTag tagref = (ColumnTag)gridref.Columns[c].Tag;
                    if (tagref.Tag == ColumnTag.ETag.AddVariable && tagref.PathLast.Define.Parent == var.Parent)
                    {
                        c += var.BuildGridColumns(gridref, c, tagref.Parent(ColumnTag.ETag.Normal), -1);

                        // 如果是List，第一次加入的时候，默认创建一个Item列。
                        // 但是仍然有问题：如果这个Item没有输入数据，下一次打开时，不会默认创建。需要手动增加Item。
                        if (var.Type == VarDefine.EType.List)
                        {
                            ColumnTag tagListEnd = gridref.Columns[c - 1].Tag as ColumnTag;
                            ColumnTag tagListEndCopy = tagListEnd.Copy(ColumnTag.ETag.Normal);
                            tagListEndCopy.PathLast.ListIndex = -tagListEnd.PathLast.ListIndex; // 肯定是0，保险写法。
                            --tagListEnd.PathLast.ListIndex;
                            c += var.Reference.BuildGridColumns(gridref, c - 1, tagListEndCopy, -1);
                        }
                        ((Document)gridref.Tag).IsChanged = true;
                    }
                }
                gridref.ResumeLayout();
            }
        }

        public (VarDefine, bool) AddVariable(VarDefine hint)
        {
            if (hint.Parent.Locked)
            {
                MessageBox.Show("bean is Locked");
                return (null, false);
            }

            FormInputVarDefine input = new FormInputVarDefine();
            input.StartPosition = FormStartPosition.CenterParent;

            // 初始化 input.ComboBoxBeanDefines。
            // 如果要全部定义，调用 LoadAllDocument.
            List<string> beanDefineFullNames = new List<string>();
            foreach (var doc in Documents.Values)
            {
                doc.BeanDefine.CollectFullNameIncludeSubBeanDefine(beanDefineFullNames);
            }
            beanDefineFullNames.Sort();
            input.ComboBoxBeanDefines.Items.AddRange(beanDefineFullNames.ToArray());

            string varName = "";
            VarDefine result = null;
            bool createResult = false;
            while (true)
            {
                input.TextBoxVarName.Text = varName;
                if (DialogResult.OK != input.ShowDialog(this))
                    break;

                try
                {
                    varName = input.TextBoxVarName.Text;
                    if (null != Tools.VerifyName(varName, CheckNameType.ShowMsg))
                        continue;
                    VarDefine.EType varType = VarDefine.ToEType(input.ComboBoxVarType.Text);
                    (VarDefine var, bool create, string err) =
                        hint.Parent.AddVariable(varName, varType, input.ComboBoxBeanDefines.Text);

                    if (null == var)
                    {
                        MessageBox.Show(err);
                        continue;
                    }
                    result = var;
                    createResult = create;
                    this.UpdateWhenAddVariable(var);
                    break;
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.ToString());
                }
            }
            input.Dispose();
            return (result, createResult);
        }

        private void DoActionByColumnTag(DataGridView grid, int columnIndex, ColumnTag tag)
        {
            switch (tag.Tag)
            {
                case ColumnTag.ETag.AddVariable:
                    AddVariable(tag.PathLast.Define);
                    break;

                case ColumnTag.ETag.ListEnd:
                    // add list item now
                    ColumnTag tagSeed = tag.Copy(ColumnTag.ETag.Normal);
                    tagSeed.PathLast.ListIndex = -tag.PathLast.ListIndex;
                    --tag.PathLast.ListIndex;
                    tag.PathLast.Define.Reference.BuildGridColumns(grid, columnIndex, tagSeed, -1);
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

        public class DataGridViewKeyPreview : DataGridView
        {
            public FormMain FormMain { get; set; }

            protected override bool ProcessKeyPreview(ref Message m)
            {
                if (IsCurrentCellInEditMode && null != FormMain.FormPopup)
                {
                    if (FormMain.FormPopup.ProcessGridKeyPreview(ref m))
                    {
                        return true;
                    }
                    // 想像程序提示一样，回车确认选择。但是下面的代码不工作。
                    /*
                    const int VK_ENTER = 0x0d;
                    const int WM_KEYDOWN = 0x100;
                    if (m.Msg == WM_KEYDOWN && m.WParam.ToInt32() == VK_ENTER)
                    {
                        var seltext = FormMain.FormPopup.ListBox.SelectedItem as string;
                        if (null != seltext)
                        {
                            EditingControl.Text = seltext;
                        }
                    }
                    */
                }
                return base.ProcessKeyPreview(ref m);
            }
        }

        private TabPage NewTabPage(string text)
        {
            TabPage tab = new TabPage();
            tab.Text = text;
            tab.Size = new Size(tabs.ClientSize.Width, tabs.ClientSize.Height);

            DataGridView grid = new DataGridViewKeyPreview() { FormMain = this };
            grid.AllowUserToAddRows = false;
            grid.AllowUserToDeleteRows = false;
            grid.AllowUserToResizeRows = false;
            grid.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.None;
            grid.AutoSizeRowsMode = DataGridViewAutoSizeRowsMode.None;
            grid.Anchor = ((AnchorStyles)((((AnchorStyles.Top | AnchorStyles.Bottom) | AnchorStyles.Left) | AnchorStyles.Right)));
            grid.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.EnableResizing;
            grid.ColumnHeadersHeight = 20;
            grid.Location = new Point(0, 0);
            grid.Margin = new Padding(2);
            grid.MultiSelect = false;
            grid.Name = "Grid";
            grid.RowHeadersWidth = 25;
            grid.RowTemplate.Height = 20;
            grid.ScrollBars = ScrollBars.Both;
            grid.Size = new Size(tab.ClientSize.Width, tab.ClientSize.Height);
            grid.TabIndex = 0;

            // performance
            //grid.RowHeadersVisible = false;
            //grid.RowHeadersWidthSizeMode = DataGridViewRowHeadersWidthSizeMode.EnableResizing;
            grid.AutoSizeRowsMode = DataGridViewAutoSizeRowsMode.None;
            // Double buffering can make DGV slow in remote desktop
            if (!System.Windows.Forms.SystemInformation.TerminalServerSession)
            {
                Type dgvType = grid.GetType();
                PropertyInfo pi = dgvType.GetProperty("DoubleBuffered",
                  BindingFlags.Instance | BindingFlags.NonPublic);
                pi.SetValue(grid, true, null);
            }

            // event handle
            grid.CellValidating += OnGridCellValidating;
            grid.CellEndEdit += OnGridCellEndEdit;
            grid.CellBeginEdit += OnGridCellBeginEdit;
            grid.CellMouseDoubleClick += OnGridDoubleClick;
            grid.KeyDown += OnGridKeyDown;
            grid.CellMouseDown += OnCellMouseDown;
            grid.ColumnWidthChanged += OnGridColumnWidthChanged;

            tab.Controls.Add(grid);
            return tab;
        }

        public void OnGridColumnWidthChanged(object sender, DataGridViewColumnEventArgs e)
        {
            if (e.Column == null)
                return;

            ColumnTag tag = e.Column.Tag as ColumnTag;
            switch (tag.Tag)
            {
                case ColumnTag.ETag.Normal:
                    tag.PathLast.Define.GridColumnValueWidth = e.Column.Width;
                    tag.PathLast.Define.Parent.Document.IsChanged = true;
                    break;
            }
        }

        private void SetSpecialColumnText(DataGridView grid, DataGridViewCellCollection cells)
        {
            for (int colIndex = 0; colIndex < grid.ColumnCount; ++colIndex) // ColumnCount maybe change in loop
            {
                DataGridViewColumn col = grid.Columns[colIndex];
                switch (((ColumnTag)(col.Tag)).Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                        cells[colIndex].Value = ",";
                        break;
                    case ColumnTag.ETag.ListStart:
                        cells[colIndex].Value = "[";
                        break;
                    case ColumnTag.ETag.ListEnd:
                        cells[colIndex].Value = "]";
                        break;
                }
            }
        }

        private void AddGridRow(DataGridView grid)
        {
            grid.Rows.Add(); // prepare row to add data
            DataGridViewCellCollection cells = grid.Rows[grid.RowCount - 1].Cells;
            SetSpecialColumnText(grid, cells);
        }

        private void newButton_Click(object sender, EventArgs e)
        {
            this.saveFileDialog1.InitialDirectory = ConfigEditor.RecentHomes[0];
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
                doc.BeanDefine.BuildGridColumns(grid, 0, new ColumnTag(ColumnTag.ETag.Normal), -1);
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

        public Dictionary<string, Document> Documents { get; } = new Dictionary<string, Document>();

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

        private Document OpenDocument(string path, string[]refbeans, int offset, out BeanDefine define)
        {
            Document doc = new Document(this);
            doc.SetFileName(path);
            if (Documents.TryGetValue(doc.RelateName, out var exist))
            {
                define = exist.BeanDefine.Search(refbeans, offset);
                return exist;
            }

            doc.Open();
            Documents.Add(doc.RelateName, doc);
            define = doc.BeanDefine.Search(refbeans, offset);
            // 必须在 Documents.Add 之后初始化。否则里面查找就可能找不到。
            doc.BeanDefine.InitializeListReference();
            return doc;
        }

        public Document OpenDocument(string relatePath, out BeanDefine define)
        {
            string[] relates = relatePath.Split('.');
            string path = ConfigEditor.GetHome();

            for (int i = 0; i < relates.Length; ++i)
            {
                path = System.IO.Path.Combine(path, relates[i]);
                if (System.IO.Directory.Exists(path)) // is directory
                    continue;
                return OpenDocument(path + ".xml", relates, i + 1, out define);
            }
            throw new Exception("Open Document Error With '" + relatePath + "'");
        }

        private Document OpenDocumentWithFilePath(string fileName, out BeanDefine define)
        {
            Document doc = new Document(this);
            doc.SetFileName(fileName);
            return OpenDocument(doc.RelateName, out define);
        }

        public void LoadAllDocument()
        {
            foreach (var fileName in System.IO.Directory.EnumerateFiles(
                ConfigEditor.GetHome(), "*.xml", System.IO.SearchOption.AllDirectories))
            {
                OpenDocumentWithFilePath(fileName, out var _);
            }
        }

        private void openButton_Click(object sender, EventArgs e)
        {
            try
            {
                this.openFileDialog1.InitialDirectory = ConfigEditor.RecentHomes[0];
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
                        // no grid
                        TabPage tab = NewTabPage(odoc.RelateName);
                        DataGridView grid = (DataGridView)tab.Controls[0];
                        LoadDocumentToView(grid, odoc);
                        tabs.Controls.Add(tab);
                        tabs.SelectedTab = tab;
                        odoc.Grid = grid;
                        grid.Tag = odoc;
                    }
                }
                else
                {
                    TabPage tab = NewTabPage(doc.RelateName);
                    DataGridView grid = (DataGridView)tab.Controls[0];
                    doc.Open();
                    Documents.Add(doc.RelateName, doc);
                    // 必须在 Documents.Add 之后初始化。否则里面查找就可能找不到。
                    doc.BeanDefine.InitializeListReference();
                    LoadDocumentToView(grid, doc);
                    tabs.Controls.Add(tab);
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
            if (e.Cancel)
                return;

            if (false == LoadCancel)
            {
                ConfigEditor.FormMainLocation = this.Location;
                ConfigEditor.FormMainSize = this.Size;
                ConfigEditor.FormMainState = this.WindowState;

                ConfigEditor.FormErrorLocation = FormError.Location;
                ConfigEditor.FormErrorSize = FormError.Size;
                ConfigEditor.FormErrorState = FormError.WindowState;

                SaveConfig();
            }

            FormDefine?.Dispose();
            FormDefine = null;
            FormError.Dispose();
        }

        private void buildButton_Click(object sender, EventArgs e)
        {
            SaveAll();
            LoadAllDocument();

            while (true)
            {
                if (string.IsNullOrEmpty(ConfigProject.ServerSrcDirectory)
                    || string.IsNullOrEmpty(ConfigProject.ClientSrcDirectory)
                    || string.IsNullOrEmpty(ConfigProject.DataOutputDirectory)
                    )
                {
                    if (DialogResult.Cancel == MessageBox.Show("Build输出目录没有配置。请先设置。",
                        "配置错误", MessageBoxButtons.OKCancel))
                        return;
                    OpenFormProjectConfig();
                    continue; // check again.
                }
                if (System.IO.Path.GetFullPath(ConfigProject.DataOutputDirectory).StartsWith(ConfigEditor.GetHome()))
                {
                    MessageBox.Show("数据输出目录不能是配置Home的子目录。");
                    OpenFormProjectConfig();
                    continue;
                }
                break;
            }

            try
            {
                // verify
                foreach (var doc in Documents.Values)
                {
                    if (doc.Grid != null)
                        continue; // 已经打开的文档，已经有即时验证了。

                    int ErrorCount = 0;

                    // 创建一个临时的 Grid 用来Verify。
                    TabPage tab = NewTabPage(doc.RelateName);
                    DataGridView grid = (DataGridView)tab.Controls[0];
                    grid.SuspendLayout();
                    FormError.OnAddError = (DataGridViewCell cell, Property.IProperty p, Property.ErrorLevel level, string desc) =>
                    {
                        if (cell.DataGridView == grid)
                            ++ErrorCount;
                    };
                    LoadDocumentToView(grid, doc);
                    if (ErrorCount > 0)
                    {
                        // 如果有错误，也显示出来。
                        tabs.Controls.Add(tab);
                        //tabs.SelectedTab = tab;
                        doc.Grid = grid;
                        grid.Tag = doc;
                        grid.ResumeLayout(); // 仅在需要显示时才执行。
                    }
                    else
                    {
                        FormError.RemoveErrorByGrid(grid);
                        tab.Dispose();
                    }
                    FormError.OnAddError = null;
                }

                if (FormError.GetErrorCount() > 0)
                {
                    FormError.Show();
                    MessageBox.Show("存在一些验证错误。停止Build。");
                    return;
                }

                // 输出服务器使用的配置数据。现在是xml格式。
                string serverDir = System.IO.Path.Combine(ConfigProject.DataOutputDirectory, "Server");
                foreach (var doc in Documents.Values)
                {
                    string docpath = doc.RelatePath.Replace('.', System.IO.Path.DirectorySeparatorChar);

                    string serverDocDir = System.IO.Path.Combine(serverDir, docpath);
                    System.IO.Directory.CreateDirectory(serverDocDir);
                    string serverFileName = System.IO.Path.Combine(serverDocDir, doc.Name + ".xml");
                    doc.SaveAs(serverFileName, true, Property.DataOutputFlags.Server);
                }

                // check VarDefne.Default
                VarDefine hasDefaultError = null;
                foreach (var doc in Documents.Values)
                {
                    doc.BeanDefine.ForEach((BeanDefine beanDefine) =>
                    {
                        foreach (var varDefine in beanDefine.Variables)
                        {
                            if (false == varDefine.CheckType(varDefine.TypeNow, varDefine.Default))
                            {
                                hasDefaultError = varDefine;
                                return false;
                            }
                        }
                        return true;
                    }
                    );
                }
                if (hasDefaultError != null)
                {
                    MessageBox.Show(hasDefaultError.FullName() + " 默认值和类型不匹配。");
                    return;
                }

                Gen.cs.Main.Gen(this, Property.DataOutputFlags.Server);

                switch (string.IsNullOrEmpty(ConfigProject.ClientLanguage) ? "cs" : ConfigProject.ClientLanguage)
                {
                    case "cs":
                        Gen.cs.Main.Gen(this, Property.DataOutputFlags.Client);
                        // 输出客户端使用的配置数据。xml格式。
                        string clientDir = System.IO.Path.Combine(ConfigProject.DataOutputDirectory, "Client");
                        foreach (var doc in Documents.Values)
                        {
                            string docpath = doc.RelatePath.Replace('.', System.IO.Path.DirectorySeparatorChar);
                            string clientDocDir = System.IO.Path.Combine(clientDir, docpath);
                            System.IO.Directory.CreateDirectory(clientDocDir);
                            string clientFileName = System.IO.Path.Combine(clientDocDir, doc.Name + ".xml");
                            doc.SaveAs(clientFileName, true, Property.DataOutputFlags.Client);
                        }

                        break;

                    case "ts":
                        // 生成代码，数据也嵌入在代码中。
                        Gen.ts.Main.Gen(this, Property.DataOutputFlags.Client);
                        break;

                    case "lua":
                        // 生成代码，数据也嵌入在代码中。
                        Gen.lua.Main.Gen(this, Property.DataOutputFlags.Client);
                        break;

                    default:
                        MessageBox.Show("unkown client language: " + ConfigProject.ClientLanguage);
                        break;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
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

        public (BeanDefine, EnumDefine) DeleteVariable(VarDefine var, bool confirm)
        {
            if (var.Parent.Locked)
            {
                MessageBox.Show("bean is Locked");
                return (null, null);
            }

            if (confirm)
            {
                if (DialogResult.OK != MessageBox.Show("确定删除？所有引用该列的数据也会被删除。", "确认", MessageBoxButtons.OKCancel))
                    return (null, null);
            }

            var updateParam = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.DeleteData }; // never change
            // delete data and column, all reference(opened grid).
            foreach (var tab in tabs.Controls)
            {
                DataGridView gridref = (DataGridView)((TabPage)tab).Controls[0];
                Document doc = (Document)gridref.Tag;
                gridref.SuspendLayout();
                for (int c = 0; c < gridref.ColumnCount; ++c)
                {
                    ColumnTag tagref = (ColumnTag)gridref.Columns[c].Tag;
                    if (tagref.PathLast.Define == var)
                    {
                        // delete data
                        for (int r = 0; r < gridref.RowCount - 1; ++r)
                        {
                            DataGridViewCellCollection cells = gridref.Rows[r].Cells;
                            int colref = c;
                            doc.Beans[r].Update(gridref, cells, ref colref, 0, updateParam);
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
            return var.Delete();
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
                    DeleteVariable(tag.PathLast.Define, true);
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

        public int FindColumnListStart(DataGridView grid, int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex; c >= 0; --c)
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
                    case ColumnTag.ETag.ListStart:
                        return c;
 
                    case ColumnTag.ETag.ListEnd:
                        ++skipNestListCount;
                        break;
                }
            }
            return -1;
        }

        public int FindColumnListEnd(DataGridView grid, int startColIndex)
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

        public int FindColumnBeanBegin(DataGridView grid, int startColIndex)
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

        public int DoActionUntilBeanEnd(DataGridView grid, int colBeanBegin, int colListEnd, Action<int> action)
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

            int colListEnd = FindColumnListEnd(grid, grid.CurrentCell.ColumnIndex);
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

        private void FormMain_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Control)
            {
                switch (e.KeyCode)
                {
                    case Keys.A: saveAllButton.PerformClick(); break;
                    case Keys.B: buildButton.PerformClick(); break;
                    case Keys.D: toolStripButtonDefine.PerformClick(); break;
                    case Keys.E: toolStripButtonError.PerformClick(); break;
                    case Keys.N: newButton.PerformClick(); break;
                    case Keys.O: openButton.PerformClick(); break;
                    case Keys.S: saveButton.PerformClick(); break;
                }
            }
        }

        public FormDefine FormDefine { get; set; }
        public TabControl Tabs => tabs;
        public Property.Manager PropertyManager { get; }

        private void toolStripButtonDefine_Click(object sender, EventArgs e)
        {
            if (null == FormDefine)
            {
                FormDefine = new FormDefine();
                FormDefine.FormMain = this;
                FormDefine.LoadDefine();

                // Dialog 模式不需要同步更新数据，简单点，先这个方案。
                FormDefine.ShowDialog(this);
                FormDefine.Dispose();
                FormDefine = null;

                foreach (var gridReload in ReloadGridsAfterFormDefineClosed)
                {
                    LoadDocumentToView(gridReload, gridReload.Tag as Document);
                }
                ReloadGridsAfterFormDefineClosed.Clear();

                if (tabs.SelectedTab != null)
                {
                    DataGridView grid = tabs.SelectedTab.Controls[0] as DataGridView;
                    grid.SuspendLayout();
                    VerifyAll(grid);
                    grid.ResumeLayout();
                }

                // 同时显示两个窗口，需要同步数据。不是先这种方案了。
                // FormDefine.Show();
            }
            else
            {
                FormDefine.BringToFront();
            }
        }

        private void tabs_SelectedIndexChanged(object sender, EventArgs e)
        {
            FormDefine?.LoadDefine();
            
        }

        private void buttonSaveAs_Click(object sender, EventArgs e)
        {
            MessageBox.Show(ConfigProject.ResourceDirectory);
            // TODO
        }

        public FormError FormError { get; }

        private void toolStripButtonError_Click(object sender, EventArgs e)
        {
            FormError.Show();
            FormError.BringToFront();
        }

        private void toolStripButtonClose_Click(object sender, EventArgs e)
        {
            if (tabs.SelectedTab == null)
                return;

            DataGridView grid = tabs.SelectedTab.Controls[0] as DataGridView;
            Document doc = grid.Tag as Document;
            Save(doc);
            HashSet<BeanDefine> deps = new HashSet<BeanDefine>();
            foreach (var d in Documents.Values)
            {
                if (d == doc)
                    continue;
                d.BeanDefine.Depends(deps);
            }

            if (doc.BeanDefine.InDepends(deps))
            {
                doc.Grid = null;
                MessageBox.Show("提示：这个文件里面的Bean定义被其他文件依赖，所以仅仅关闭编辑界面。");
            }
            else
            {
                Documents.Remove(doc.RelateName);
            }
            FormError.RemoveErrorByGrid(grid);
            var seltab = tabs.SelectedTab;
            tabs.Controls.Remove(seltab);
            seltab.Dispose();
        }

        public void OpenFormProjectConfig()
        {
            FormProjectConfig form = new FormProjectConfig();
            form.FormMain = this;
            form.ShowDialog();
            form.Dispose();
        }

        private void toolStripButtonConfig_Click(object sender, EventArgs e)
        {
            OpenFormProjectConfig();
        }

        public FormPopupListBox FormPopupListBox;
        private FormPopupListBox FormPopup = null; // 当有其他Popup实现时，使用基类。要有个公共基类处理事件。

        public void HideFormHelp()
        {
            FormPopup?.Hide();
            FormPopup = null;
        }

        public void ShowFormHelp(DataGridView grid, int col, int row)
        {
            if (null == FormPopup)
                return;

            var rect = grid.GetCellDisplayRectangle(col, row, true);
            FormPopup.Location = grid.PointToScreen(new Point(rect.X, rect.Y + rect.Height));
            FormPopup.Show();
            //form.BringToFront();
        }

        private void OnGridCellBeginEdit(object sender, DataGridViewCellCancelEventArgs e)
        {
            var grid = sender as DataGridView;
            var tag = grid.Columns[e.ColumnIndex].Tag as ColumnTag;

            switch (tag.PathLast.Define.Type)
            {
                case VarDefine.EType.Undecided:
                case VarDefine.EType.String:
                case VarDefine.EType.Int:
                case VarDefine.EType.Long:
                case VarDefine.EType.Double:
                case VarDefine.EType.Float:
                case VarDefine.EType.Date:
                case VarDefine.EType.List:
                    return; // 先不支持。

                case VarDefine.EType.Enum:
                    if (!tag.PathLast.Define.Parent.EnumDefines.TryGetValue(tag.PathLast.Define.Name, out var enumDefine))
                        return;
                    FormPopupListBox.ListBox.Items.Clear();
                    foreach (var v in enumDefine.ValueMap.Values)
                    {
                        FormPopupListBox.ListBox.Items.Add(v.Name);
                    }
                    var value = grid[e.ColumnIndex, e.RowIndex].Value;
                    if (null == value)
                        value = "";
                    FormPopupListBox.ListBox.SelectedIndex = FormPopupListBox.ListBox.Items.IndexOf(value);
                    FormPopup = FormPopupListBox;
                    break;
            }

            ShowFormHelp(grid, e.ColumnIndex, e.RowIndex);
        }

        private void FormMain_Activated(object sender, EventArgs e)
        {
            if (null == tabs.SelectedTab)
                return;

            var grid = tabs.SelectedTab.Controls[0] as DataGridView;
            if (grid.IsCurrentCellInEditMode)
            {
                ShowFormHelp(grid, grid.CurrentCell.ColumnIndex, grid.CurrentCell.RowIndex);
            }
        }

        private void FormMain_Deactivate(object sender, EventArgs e)
        {
            FormPopup?.Hide();
        }
    }
}
