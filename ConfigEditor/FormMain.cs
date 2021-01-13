using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormMain : Form
    {
        public EditorConfig ConfigEditor { get; private set; }
        public ProjectConfig ConfigProject { get; private set; }

        public static FormMain Instance;

        public FormMain()
        {
            InitializeComponent();
            LoadConfigEditor();
            PropertyManager = new Property.Manager();
            FormError = new FormError();
            FormPopupListBox =  new FormPopupListBox();
            Instance = this;
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

        ///<summary>
        /// 该函数设置由不同线程产生的窗口的显示状态
        /// </summary>
        /// <param name="hWnd">窗口句柄</param>
        /// <param name="cmdShow">指定窗口如何显示。查看允许值列表，请查阅ShowWindow函数的说明部分</param>
        /// <returns>如果函数原来可见，返回值为非零；如果函数原来被隐藏，返回值为零</returns>
        [DllImport("User32.dll")]
        private static extern bool ShowWindowAsync(IntPtr hWnd, int cmdShow);

        /// <summary>
        ///  该函数将创建指定窗口的线程设置到前台，并且激活该窗口。键盘输入转向该窗口，并为用户改各种可视的记号。
        ///  系统给创建前台窗口的线程分配的权限稍高于其他线程。 
        /// </summary>
        /// <param name="hWnd">将被激活并被调入前台的窗口句柄</param>
        /// <returns>如果窗口设入了前台，返回值为非零；如果窗口未被设入前台，返回值为零</returns>
        [DllImport("User32.dll", EntryPoint = "SetForegroundWindow")]
        private static extern bool SetForegroundWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern void SwitchToThisWindow(IntPtr hWnd, bool fAltTab);

        private const int SW_SHOWNOMAL = 1;

        private bool LockHome()
        {
            var home = ConfigEditor.GetHome();
            try
            {
                var lockFile = System.IO.Path.Combine(home, "ConfigEditor.lock");
                System.IO.File.Create(lockFile, 1024, System.IO.FileOptions.DeleteOnClose);
                return true;
            }
            catch (Exception)
            {
                //MessageBox.Show($"Home 已经在编辑中。");
                var processes = Process.GetProcessesByName("ConfigEditor");
                foreach (var process in processes)
                {
                    if (process.MainWindowTitle.Equals(home))
                    {
                        ShowWindowAsync(process.MainWindowHandle, SW_SHOWNOMAL);
                        SetForegroundWindow(process.MainWindowHandle);
                        SwitchToThisWindow(process.MainWindowHandle, true);
                        break;
                    }
                }
                return false;
            }
        }
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
            if (false == LockHome())
            {
                LoadCancel = true;
                Close();
                return;
            }
            LoadConfigProject();

            Documents = new Documents();
            Documents.Build();

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

        public void OnGridCellValidating(object sender, DataGridViewCellValidatingEventArgs e)
        {
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
                if (null == enumDefine.GetValueDefine(newValue))
                {
                    switch (MessageBox.Show("输入的枚举名字不存在，是否添加进去？", "提示", MessageBoxButtons.YesNoCancel))
                    {
                        case DialogResult.Yes:
                            enumDefine.AddValue(new EnumDefine.ValueDefine(enumDefine, newValue, -1));
                            break;

                        case DialogResult.No:
                            break; // 继续，允许错误输入。

                        case DialogResult.Cancel:
                            return; // cancel
                    }
                }
                e.Cancel = false;
            }
        }

        public void OnGridCellEndEdit(object sender, DataGridViewCellEventArgs e)
        {
            HideFormHelp();
        }

        public void UpdateWhenAddVariable(VarDefine var)
        {
            // TODO 只更新 RefBy 的地方。
            Documents.ForEachFile((Documents.File file) =>
            { 
                file.Document?.GridData?.UpdateWhenAddVariable(var);
                return true;
            });
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
            Documents.ForEachFile((Documents.File file) =>
            {
                file.Document?.BeanDefine.CollectFullNameIncludeSubBeanDefine(beanDefineFullNames);
                return true;
            });
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
                    tag.PathLast.Define.Reference.BuildGridColumns((grid.Tag as Document).GridData, columnIndex, tagSeed, -1);
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

        public TabPage NewTabPage(string text)
        {
            TabPage tab = new TabPage();
            tab.Text = text;
            tab.Size = new Size(tabs.ClientSize.Width, tabs.ClientSize.Height);

            DataGridView grid = new DataGridViewKeyPreview() { FormMain = this };
            grid.VirtualMode = true;
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
            grid.EditingControlShowing += OnGridEditingControlShowing;

            // virtualmode
            grid.CellValueNeeded += OnCellValueNeeded;
            grid.CellValuePushed += OnCellValuePushed;

            tab.Controls.Add(grid);
            return tab;
        }

        private void OnCellValuePushed(object sender, DataGridViewCellValueEventArgs e)
        {
            var grid = sender as DataGridView;
            DataGridViewColumn col = grid.Columns[e.ColumnIndex];
            ColumnTag tag = (ColumnTag)col.Tag;
            if (ColumnTag.ETag.Normal != tag.Tag)
                return; // 不可能。特殊列都是不可编辑的。

            var doc = grid.Tag as Document;

            bool addRow = false;
            if (e.RowIndex == doc.GridData.RowCount)
            {
                doc.AddBean(new Bean(doc, ""));
                doc.GridData.InsertRow(e.RowIndex, false);
                grid.Rows.Add();
                addRow = true;
            }

            if (addRow)
            {
                doc.GridData.BuildUniqueIndexOnAddRow(e.RowIndex);
            }

            var cell = doc.GridData.GetCell(e.ColumnIndex, e.RowIndex);
            var oldValue = cell.Value;
            cell.Value = e.Value as string;
            if (cell.Value == null)
                cell.Value = "";
            var newValue = cell.Value;

            // save data
            int colIndex = e.ColumnIndex;
            var updateParam = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.Data };
            doc.Beans[e.RowIndex].Update(doc.GridData, doc.GridData.GetRow(e.RowIndex), ref colIndex, 0, updateParam);

            // verify
            tag.UpdateUniqueIndex(oldValue, newValue, cell);
            var param = new Property.VerifyParam()
            {
                FormMain = this,
                Grid = doc.GridData,
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

        private void OnCellValueNeeded(object sender, DataGridViewCellValueEventArgs e)
        {
            var grid = sender as DataGridView;
            var doc = grid.Tag as Document;
            if (e.RowIndex == doc.GridData.RowCount)
                return;

            var cell = doc.GridData.GetCell(e.ColumnIndex, e.RowIndex);
            e.Value = cell.Value;
            grid[e.ColumnIndex, e.RowIndex].Style.BackColor = cell.BackColor;
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

        /*
        private void SetSpecialColumnText(DataGridView grid, DataGridViewCellCollection cells)
        {
            for (int colIndex = 0; colIndex < grid.ColumnCount; ++colIndex)
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
        */

        private delegate void DelegateVoid();

        public void InvokeOpenGrid(Document doc, bool select = true)
        {
            DelegateVoid d = delegate { OpenGrid(doc, select); };
            this.BeginInvoke(d);
        }

        private void OpenGrid(Document doc, bool select = true)
        {
            if (null == doc)
                return;

            try
            {
                if (doc.GridData?.View == null)
                {
                    if (doc.GridData == null)
                    {
                        doc.BeanDefine.InitializeReference(); // XXX
                        doc.BuildGridData();
                        doc.GridData.VerifyAll(false);
                    }

                    TabPage tab = NewTabPage(doc.RelateName);
                    DataGridView grid = (DataGridView)tab.Controls[0];
                    grid.SuspendLayout();
                    doc.GridData.View = grid;
                    doc.GridData.SyncToView();
                    tabs.Controls.Add(tab);
                    if (select)
                        tabs.SelectedTab = tab;
                    grid.ResumeLayout();
                    grid.Tag = doc;
                }
                else
                {
                    // has opened
                    TabPage tab = (TabPage)doc.GridData.View.Parent;
                    tabs.SelectedTab = tab;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        private void newButton_Click(object sender, EventArgs e)
        {
            this.saveFileDialog1.InitialDirectory = ConfigEditor.RecentHomes[0];
            this.saveFileDialog1.FileName = "";
            this.saveFileDialog1.Filter = "(*.xml)|*.xml";
            if (DialogResult.OK != this.saveFileDialog1.ShowDialog())
                return; // 取消
            string file = this.saveFileDialog1.FileName;
            if (!file.EndsWith(".xml"))
                file = file + ".xml";
            OpenGrid(Documents.OpenFile(file, true)?.Open(true));
        }

        public Documents Documents { get; private set; }

        private void saveButton_Click(object sender, EventArgs e)
        {
            (tabs.SelectedTab?.Controls[0].Tag as Document).SaveIfChanged();
        }

        private void openButton_Click(object sender, EventArgs e)
        {
            this.openFileDialog1.InitialDirectory = ConfigEditor.RecentHomes[0];
            this.openFileDialog1.FileName = "";
            this.openFileDialog1.Filter = "(*.xml)|*.xml";
            if (DialogResult.OK != this.openFileDialog1.ShowDialog())
                return;
            
            OpenGrid(Documents.OpenFile(this.openFileDialog1.FileName, false)?.Open(true));
        }

        public bool SaveAll()
        {
            Documents?.ForEachFile((Documents.File file) =>
            {
                file.Document?.SaveIfChanged();
                return true;
            });
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
            FormMain.Instance = null;
        }

        private void buildButton_Click(object sender, EventArgs e)
        {
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
            FormBuildProgress progress = new FormBuildProgress();
            progress.ShowDialog();
            progress.StopAndWait();
            progress.Dispose();
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

        public void DeleteVariable(VarDefine var, bool confirm,
            HashSet<BeanDefine> deletedBeanDefines, HashSet<EnumDefine> deletedEnumDefines)
        {
            if (var.Parent.Locked)
            {
                MessageBox.Show("bean is Locked");
                return;
            }

            if (confirm)
            {
                if (DialogResult.OK != MessageBox.Show("确定删除？所有引用该列的数据也会被删除。", "确认", MessageBoxButtons.OKCancel))
                    return;
            }

            // TODO delete data and column, all reference.
            Documents.ForEachFile((Documents.File file) =>
            {
                file.Document?.GridData?.DeleteVariable(var);
                return true;
            });

            // delete define
            var.Delete(deletedBeanDefines, deletedEnumDefines);
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
                    DeleteVariable(tag.PathLast.Define, true, null, null);
                    break;
            }
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
            grid.SuspendLayout();
            (grid.Tag as Document).GridData.DeleteListItem(grid.CurrentCell.ColumnIndex);
            grid.ResumeLayout();
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

        private DataGridViewCell GetSafeCell(DataGridView grid, DataGridViewCell hint)
        {
            if (null == hint)
                return null;
            if (hint.ColumnIndex >= 0 && hint.ColumnIndex < grid.ColumnCount
                && hint.RowIndex >= 0 && hint.RowIndex < grid.RowCount)
                return grid[hint.ColumnIndex, hint.RowIndex];
            return null;
        }

        private void toolStripButtonDefine_Click(object sender, EventArgs e)
        {
            if (null == FormDefine)
            {
                FormDefine = new FormDefine();
                FormDefine.LoadDefine();

                // Dialog 模式不需要同步更新数据，简单点，先这个方案。
                FormDefine.ShowDialog(this);
                FormDefine.Dispose();
                FormDefine = null;

                foreach (var gridReload in ReloadGridsAfterFormDefineClosed)
                {
                    var firstDisplayCell = gridReload.FirstDisplayedCell;
                    var currentCell = gridReload.CurrentCell;

                    gridReload.SuspendLayout();

                    var doc = gridReload.Tag as Document;
                    doc.GridData.View = null;
                    doc.BuildGridData();
                    doc.GridData.VerifyAll(false);
                    doc.GridData.View = gridReload;
                    doc.GridData.SyncToView();

                    // resore view.
                    var firstDisplayCellNow = GetSafeCell(gridReload, firstDisplayCell);
                    if (null != firstDisplayCellNow)
                        gridReload.FirstDisplayedCell = firstDisplayCellNow;
                    var currentCellNow = GetSafeCell(gridReload, currentCell);
                    if (null != currentCellNow)
                        gridReload.CurrentCell = currentCellNow;

                    gridReload.ResumeLayout();
                }

                if (tabs.SelectedTab != null)
                {
                    DataGridView grid = tabs.SelectedTab.Controls[0] as DataGridView;
                    if (false == ReloadGridsAfterFormDefineClosed.Contains(grid)) // 如果已经Reload过，就不需要再次VerifyAll。
                    {
                        grid.SuspendLayout();
                        (grid.Tag as Document).GridData.VerifyAll(true);
                        grid.ResumeLayout();
                    }
                }
                ReloadGridsAfterFormDefineClosed.Clear();

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
            //char.IsSeparator
            MessageBox.Show($":{char.IsPunctuation(':')};{char.IsPunctuation(';')}.{char.IsPunctuation('.')}'{char.IsPunctuation('\'')}\"{char.IsPunctuation('\"')}");
        }

        public FormError FormError { get; }

        private void toolStripButtonError_Click(object sender, EventArgs e)
        {
            FormError.Show();
            FormError.BringToFront();
        }

        private void toolStripButtonClose_Click(object sender, EventArgs e)
        {
            var seltab = tabs.SelectedTab;
            if (seltab == null)
                return;

            DataGridView grid = seltab.Controls[0] as DataGridView;
            Document doc = grid.Tag as Document;
            doc.SaveIfChanged();
            HashSet<BeanDefine> deps = new HashSet<BeanDefine>();
            Documents.ForEachFile((Documents.File file) =>
            {
                if (doc == file.Document)
                    return true; // skip self
                file.Document?.BeanDefine.Depends(deps);
                return true;
            });

            if (doc.BeanDefine.InDepends(deps))
            {
                doc.GridData.View = null;
                //MessageBox.Show("提示：这个文件里面的Bean定义被其他文件依赖，所以仅仅关闭编辑界面。");
            }
            else
            {
                doc.Close();
            }
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

        private void OnGridEditingControlShowing(object sender, DataGridViewEditingControlShowingEventArgs e)
        {
            var grid = sender as DataGridView;
            e.Control.TextChanged += OnGridEditingControlTextChanged;
        }

        private void OnGridEditingControlTextChanged(object sender, EventArgs e)
        {
            if (FormPopupListBox.Visible)
            {
                var editingControl = sender as Control;
                FormPopupListBox.ListBox.SelectedIndex = FormPopupListBox.ListBox.Items.IndexOf(editingControl.Text);
            }
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
                    if (null != value)
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

        public void InvokeShowFormError()
        {
            DelegateVoid d = delegate { this.FormError.Show(); };
            this.BeginInvoke(d);
        }
    }
}
