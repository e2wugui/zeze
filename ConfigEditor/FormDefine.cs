using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormDefine : Form
    {
        public FormDefine()
        {
            InitializeComponent();

            DataGridViewComboBoxColumn col = (DataGridViewComboBoxColumn)define.Columns["VarType"];
            col.ValueType = typeof(VarDefine.EType);
            col.DataSource = System.Enum.GetValues(typeof(VarDefine.EType));

            // Double buffering can make DGV slow in remote desktop
            if (!System.Windows.Forms.SystemInformation.TerminalServerSession)
            {
                Type dgvType = define.GetType();
                PropertyInfo pi = dgvType.GetProperty("DoubleBuffered",
                  BindingFlags.Instance | BindingFlags.NonPublic);
                pi.SetValue(define, true, null);
            }
        }

        private void FormDefine_FormClosing(object sender, FormClosingEventArgs e)
        {
            if (false == this.Modal)
                FormMain.Instance.FormDefine = null;
        }

        private DataGridViewCell GetSafeCell(DataGridView grid, int col, int row)
        {
            if (col >= 0 && col < grid.ColumnCount && row >= 0 && row < grid.RowCount)
                return grid[col, row];
            return null;
        }

        public void LoadDefine()
        {
            var firstDisplayCellCol = -1;
            var firstDisplayCellRow = -1;
            if (null != define.FirstDisplayedCell)
            {
                firstDisplayCellCol = define.FirstDisplayedCell.ColumnIndex;
                firstDisplayCellRow = define.FirstDisplayedCell.RowIndex;
            }

            var currentCellCol = -1;
            var currentCellRow = -1;
            if (null != define.CurrentCell)
            {
                currentCellCol = define.CurrentCell.ColumnIndex;
                currentCellRow = define.CurrentCell.RowIndex;
            }

            define.Rows.Clear();
            if (null == FormMain.Instance.Tabs.SelectedTab)
                return; // no file

            DataGridView grid = (DataGridView)FormMain.Instance.Tabs.SelectedTab.Controls[0];
            Document = (Document)grid.Tag;
            LoadDocument(Document);

            var firstDisplayCellNow = GetSafeCell(define, firstDisplayCellCol, firstDisplayCellRow);
            if (null != firstDisplayCellNow)
                define.FirstDisplayedCell = firstDisplayCellNow;
            var currentCellNow = GetSafeCell(define, currentCellCol, currentCellRow);
            if (null != currentCellNow)
                define.CurrentCell = currentCellNow;
        }

        Document Document { get; set; } // 现在还不需要记住这个，保留来以后同时装载多个Document的BeanDefine

        public bool IsLoadedDocument(Document doc)
        {
            return doc == Document;
        }

        private void LoadDocument(Document doc)
        {
            SortedDictionary<string, BeanDefine> BeanDefines = new SortedDictionary<string, BeanDefine>();
            doc.BeanDefine.ForEach((BeanDefine bd) =>
            {
                if (bd.NamespaceOnly)
                    return true; // skip
                BeanDefines.Add(bd.FullName(), bd);
                return true;
            });

            SortedDictionary<string, EnumDefine> EnumDefines = new SortedDictionary<string, EnumDefine>();
            define.SuspendLayout();
            foreach (var e in BeanDefines)
            {
                InsertBeanDefine(define.RowCount, e.Key, e.Value);
                foreach (var ed in e.Value.EnumDefines.Values)
                    EnumDefines.Add(ed.FullName(), ed);
            }
            foreach (var e in EnumDefines)
            {
                InsertEnumDefine(define.RowCount, e.Key, e.Value);
            }
            define.ResumeLayout();
        }

        private void InsertEnumDefine(int insertIndex, string fullName, EnumDefine enumDefine)
        {
            // row for enum start
            define.Rows.Insert(insertIndex, 1);
            DataGridViewCellCollection cellsBeanStart = define.Rows[insertIndex].Cells;
            for (int i = 0; i < cellsBeanStart.Count; ++i)
                cellsBeanStart[i].ReadOnly = true;
            DataGridViewCell cellLocked = cellsBeanStart["BeanLocked"];
            cellLocked.Tag = enumDefine;
            cellsBeanStart["VarName"].Value = fullName;
            cellsBeanStart["VarName"].Style.ForeColor = Color.Blue;

            // row for value
            foreach (var v in enumDefine.ValueMap.Values)
            {
                ++insertIndex;
                InsertValueDefine(insertIndex, v);
            }

            // row for enum end
            ++insertIndex;
            define.Rows.Insert(insertIndex, 1);
            DataGridViewCellCollection cellsBeanEnd = define.Rows[insertIndex].Cells;
            for (int i = 0; i < cellsBeanEnd.Count; ++i)
                cellsBeanEnd[i].ReadOnly = true;
            DataGridViewCell cellBeanEnd = cellsBeanEnd["VarName"];
            cellBeanEnd.Value = ",";
            cellBeanEnd.Tag = new EnumDefine.ValueDefine(enumDefine, "", -1);
            cellBeanEnd.ToolTipText = "双击增加枚举";
        }

        private void InsertValueDefine(int rowIndex, EnumDefine.ValueDefine value)
        {
            define.Rows.Insert(rowIndex, 1);
            DataGridViewCellCollection cellsValue = define.Rows[rowIndex].Cells;
            for (int i = 0; i < cellsValue.Count; ++i)
                cellsValue[i].ReadOnly = true;

            cellsValue["VarName"].Value = value.Name;
            cellsValue["VarName"].Tag = value;
            cellsValue["VarName"].ReadOnly = false;
            cellsValue["VarName"].Style.ForeColor = Color.Blue;
            cellsValue["VarValue"].Value = value.Value;
            cellsValue["VarValue"].ReadOnly = false;
            cellsValue["VarComment"].Value = value.Comment;
            cellsValue["VarComment"].ReadOnly = false;
        }

        private void InsertBeanDefine(int insertIndex, string fullName, BeanDefine bean)
        {
            // row for bean start
            define.Rows.Insert(insertIndex, 1);
            DataGridViewCellCollection cellsBeanStart = define.Rows[insertIndex].Cells;
            for (int i = 0; i < cellsBeanStart.Count; ++i)
                cellsBeanStart[i].ReadOnly = true;
            DataGridViewCell cellLocked = cellsBeanStart["BeanLocked"];
            cellLocked.Value = bean.Locked ? "Yes" : "No";
            cellLocked.Tag = bean; // BeanDefine
            cellsBeanStart["VarName"].Value = fullName;
            cellsBeanStart["VarName"].ReadOnly = false;

            // row for vars
            foreach (var v in bean.Variables)
            {
                ++insertIndex;
                InsertVariable(insertIndex, v);
            }

            // row for bean end
            ++insertIndex;
            define.Rows.Insert(insertIndex, 1);
            DataGridViewCellCollection cellsBeanEnd = define.Rows[insertIndex].Cells;
            for (int i = 0; i < cellsBeanEnd.Count; ++i)
                cellsBeanEnd[i].ReadOnly = true;
            DataGridViewCell cellBeanEnd = cellsBeanEnd["VarName"];
            cellBeanEnd.Value = ",";
            cellBeanEnd.Tag = new VarDefine(bean, "");
            cellBeanEnd.ToolTipText = "双击增加变量（数据列）";
        }

        private void InsertVariable(int rowIndex, VarDefine var)
        {
            define.Rows.Insert(rowIndex, 1);
            DataGridViewCellCollection cellsVar = define.Rows[rowIndex].Cells;
            cellsVar["BeanLocked"].ReadOnly = true;
            DataGridViewCell cellVar = cellsVar["VarName"];
            cellVar.Value = var.Name;
            cellVar.Tag = var;
            cellsVar["VarType"].Value = var.Type;
            cellsVar["VarValue"].Value = var.Value;
            cellsVar["VarForeign"].Value = var.Foreign;
            cellsVar["VarProperties"].Value = var.Properties;
            cellsVar["VarDefault"].Value = var.Default;
            cellsVar["VarComment"].Value = var.Comment;
        }

        private void define_CellMouseDown(object sender, DataGridViewCellMouseEventArgs e)
        {
            switch (e.Button)
            {
                case MouseButtons.Right:
                    {
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
                    break;

                case MouseButtons.Left:
                    {
                        // DragDrop prepare. 这里记住鼠标位置，然后处理MouseMove，移动一定距离开始。
                        if (e.RowIndex >= 0 && e.ColumnIndex == -1) // row header
                        {
                            Size dragSize = SystemInformation.DragSize;
                            DragBoxFromMouseDown = new Rectangle(new Point(e.X - (dragSize.Width / 2), e.Y - (dragSize.Height / 2)), dragSize);
                        }
                        else
                        {
                            DragBoxFromMouseDown = Rectangle.Empty;
                        }
                    }
                    if (e.ColumnIndex >= 0 && e.RowIndex >= 0)
                    {
                        if (define.CurrentCell == define[e.ColumnIndex, e.RowIndex])
                        {
                            // 在选中的当前cell中再次按下鼠标，打开编辑窗口。
                            EditProperties(define.CurrentCell.RowIndex, define.CurrentCell.ColumnIndex);
                        }
                    }
                    break;
            }
        }

        private void DeleteRowsByBeanLockedTag(object tag)
        {
            int i = 0;
            for (; i < define.RowCount; ++i)
            {
                if (object.ReferenceEquals(define.Rows[i].Cells["BeanLocked"].Tag, tag))
                    break;
            }
            while (i < define.RowCount)
            {
                define.Rows.RemoveAt(i);
                if (i >= define.RowCount)
                    break;
                DataGridViewCell cell = define.Rows[i].Cells["BeanLocked"];
                if (cell.Tag != null && !object.ReferenceEquals(cell.Tag, tag))
                    break;
            }
        }

        private void DeleteVariable(int rowIndex, VarDefine var, bool confirm)
        {
            var deletedBeanDefines = new HashSet<BeanDefine>();
            var deletedEnumDefines = new HashSet<EnumDefine>();
            FormMain.Instance.DeleteVariable(var, confirm, deletedBeanDefines, deletedEnumDefines);

            define.SuspendLayout();
            define.Rows.RemoveAt(rowIndex);
            foreach (var beanDefine in deletedBeanDefines)
            {
                if (!IsLoadedDocument(beanDefine.Document))
                    continue;

                // remove bean
                DeleteRowsByBeanLockedTag(beanDefine);
                /*
                beanDefine.ForEach((BeanDefine bd) =>
                {
                    foreach (var enumdef in bd.EnumDefines.Values)
                    {
                        // remove enum
                        DeleteRowsByBeanLockedTag(enumdef);
                    }
                    return true;
                });
                */
            }
            foreach (var enumDefine in deletedEnumDefines)
            {
                if (!IsLoadedDocument(enumDefine.Parent.Document))
                    continue;
                DeleteRowsByBeanLockedTag(enumDefine);
            }
            define.ResumeLayout();
        }

        private void deleteVariableColumnToolStripMenuItem_Click(object sender, EventArgs e)
        {
            if (define.CurrentCell == null)
                return;
            object tag = define.Rows[define.CurrentCell.RowIndex].Cells["VarName"].Tag;
            if (null == tag)
                return;

            if (tag is EnumDefine.ValueDefine valueDefine)
            {
                valueDefine.Delete();
                define.Rows.RemoveAt(define.CurrentCell.RowIndex);
                return;
            }
            DeleteVariable(define.CurrentCell.RowIndex, tag as VarDefine, true);
        }

        private void define_CellMouseDoubleClick(object sender, DataGridViewCellMouseEventArgs e)
        {
            if (e.Button != MouseButtons.Left)
                return;

            if (e.RowIndex < 0)
                return;
            if (e.ColumnIndex < 0)
                return;

            switch (define.Columns[e.ColumnIndex].Name)
            {
                case "VarName":
                    DataGridViewCell cellVarName = define[e.ColumnIndex, e.RowIndex];
                    if (null != cellVarName.Tag && (cellVarName.Value as string == ","))
                    {
                        if (cellVarName.Tag is VarDefine varDefineHint)
                        {
                            (VarDefine var, bool create) = FormMain.Instance.AddVariable(varDefineHint);
                            UpdateWhenAddVariable(e.RowIndex, var, create);
                            if (null != var && var.Parent.EnumDefines.TryGetValue(var.Name, out var enumDefine))
                                InsertEnumDefine(define.RowCount, enumDefine.FullName(), enumDefine);
                        }
                        if (cellVarName.Tag is EnumDefine.ValueDefine valueDefine)
                        {
                            InsertValueDefine(e.RowIndex, new EnumDefine.ValueDefine(valueDefine.Parent, "", -1));
                            define.CurrentCell = define[e.ColumnIndex, e.RowIndex];
                            define.BeginEdit(true);
                            // 当编辑取消时，删掉新增的行。see define_CellEndEdit
                        }
                    }
                    break;

                case "BeanLocked":
                    DataGridViewCell cellBeanLocked = define[e.ColumnIndex, e.RowIndex];
                    var bean = cellBeanLocked.Tag as BeanDefine;
                    if (null != bean)
                    {
                        bean.Locked = !bean.Locked;
                        cellBeanLocked.Value = bean.Locked ? "Yes" : "No";
                    }
                    break;

                case "VarProperties":
                    EditProperties(e.RowIndex, e.ColumnIndex);
                    break;
            }
        }

        private void EditProperties(int rowIndex, int colIndex)
        {
            if (rowIndex < 0)
                return;

            DataGridViewCellCollection cells = define.Rows[rowIndex].Cells;
            VarDefine var = cells["VarName"].Tag as VarDefine;
            if (null == var || null == var.Name)
                return;

            if (!define.Columns[colIndex].Name.Equals("VarProperties"))
                return;

            DataGridViewCell cell = cells["VarProperties"];
            FormProperties fp = new FormProperties()
            {
                Properties = var.PropertiesList,
                FormDefine = this,
            };

            if (DialogResult.OK == fp.ShowDialog(this))
            {
                List<Property.IProperty> current = new List<Property.IProperty>();
                foreach (var p in FormMain.Instance.PropertyManager.Properties)
                {
                    if (p.Value.ButtonChecked)
                    {
                        current.Add(p.Value);
                    }
                    p.Value.Button = null;
                }
                var.Properties = FormMain.Instance.PropertyManager.BuildString(current);
                cell.Value = var.Properties;
            }
            fp.Dispose();
        }

        private void UpdateWhenAddVariable(int rowIndex, VarDefine var, bool create)
        {
            if (null != var)
            {
                InsertVariable(rowIndex, var);
                if (create && IsLoadedDocument(var.Reference.Document))
                {
                    string fullName = var.Reference.FullName();
                    int insertIndex = 0;
                    for (; insertIndex < define.RowCount; ++insertIndex)
                    {
                        DataGridViewCellCollection cells = define.Rows[insertIndex].Cells;
                        if (cells["BeanLocked"].Tag == null)
                            continue;
                        if (fullName.CompareTo(cells["VarName"].Value as string) < 0)
                            break;
                    }
                    InsertBeanDefine(insertIndex, fullName, var.Reference);
                }
            }
        }

        private void define_CellValueChanged(object sender, DataGridViewCellEventArgs e)
        {
            if (e.ColumnIndex < 0)
                return;

            if (e.RowIndex < 0)
                return;

            switch (define.Columns[e.ColumnIndex].Name)
            {
                case "VarProperties":
                    DataGridViewCell cell = define.Rows[e.RowIndex].Cells["VarProperties"];
                    cell.ToolTipText = FormMain.Instance.PropertyManager.BuildToolTipText(cell.Value as string);
                    break;
            }
        }

        private void define_CellValidating(object sender, DataGridViewCellValidatingEventArgs e)
        {
            if (e.ColumnIndex < 0)
                return;

            if (e.RowIndex < 0)
                return;

            if (false == define.IsCurrentCellInEditMode)
                return;

            DataGridViewCellCollection cells = define.Rows[e.RowIndex].Cells;

            if (cells["BeanLocked"].Tag != null)
            {
                // assert(cells["VarName"] == cells[e.ColumnIndex]);
                var newValue = e.FormattedValue as string;
                if (null != Tools.VerifyName(newValue, CheckNameType.ShowMsg))
                {
                    e.Cancel = true;
                    return; // VerifyName 里面已经显示消息了。
                }
                return;
            }

            DataGridViewCell cellVarName = cells["VarName"];
            if (null == cellVarName.Tag)
            {
                e.Cancel = true;
                MessageBox.Show("只有VarName.Tag设置的行才允许编辑，其他的应该都设置了 ReadOnly. 怎么到这里的？");
                return;
            }

            if (cellVarName.Tag is EnumDefine.ValueDefine valueDefine)
            {
                string colName = define.Columns[e.ColumnIndex].Name;
                switch (colName)
                {
                    case "VarName":
                        var newValue = e.FormattedValue as string;
                        if (null != Tools.VerifyName(newValue, CheckNameType.ShowMsg))
                        {
                            e.Cancel = true;
                            return;
                        }
                        if (false == valueDefine.Name.Equals(newValue)
                            && null != valueDefine.Parent.GetValueDefine(newValue))
                        {
                            e.Cancel = true;
                            MessageBox.Show("enum.value 重名了。");
                            return;
                        }
                        break;

                    case "VarValue":
                        if (false == int.TryParse(e.FormattedValue as string, out var _))
                        {
                            e.Cancel = true;
                            MessageBox.Show("enum.value只能是整数。");
                            return;
                        }
                        break;
                }
                return;
            }

            // modify Var
            VarDefine var = cellVarName.Tag as VarDefine;
            if (null == var.Name) // var.Name is null when row is ',' for addvar
            {
                e.Cancel = true; // 肯定时readonly吧。怎么到这里的。
                return;
            }

            try
            {
                string colName = define.Columns[e.ColumnIndex].Name;
                switch (colName)
                {
                    case "VarName":
                        string newVarName = e.FormattedValue as string;
                        if (var.Name.Equals(newVarName))
                            return;
                        
                        if (null != Tools.VerifyName(newVarName, CheckNameType.ShowMsg))
                        {
                            e.Cancel = true;
                            return; // VerifyName 里面已经显示消息了。
                        }
                        if (var.Parent.GetVariable(newVarName) != null)
                        {
                            e.Cancel = true;
                            MessageBox.Show("变量名字重复了。");
                            return;
                        }
                        break;

                    case "VarValue":
                        string newValue = e.FormattedValue as string;
                        if (false == string.IsNullOrEmpty(newValue))
                        {
                            var r = FormMain.Instance.Documents.SearchReference(newValue);
                            e.Cancel = r == null;
                            if (e.Cancel)
                                MessageBox.Show("引用的Bean名字没有找到。输入空将创建一个。");
                        }
                        break;

                    case "VarForeign":
                        string errForeign = var.OpenForeign(e.FormattedValue as string, out var _);
                        if (null != errForeign)
                        {
                            e.Cancel = true;
                            MessageBox.Show(errForeign);
                        }
                        break;

                    case "VarType":
                        VarDefine.EType newType = (VarDefine.EType)System.Enum.Parse(
                            typeof(VarDefine.EType), e.FormattedValue as string);
                        string errType = var.CanChangeTo(newType);
                        if (null != errType)
                        {
                            e.Cancel = true;
                            MessageBox.Show(errType);
                        }
                        break;

                    case "VarDefault":
                        if (false == var.CheckType(var.Type, e.FormattedValue as string))
                        {
                            e.Cancel = true;
                            MessageBox.Show("这个默认值和当前类型不匹配。");
                        }
                        break;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
                e.Cancel = true;
            }
        }

        /*
        private void UpdateData(Document doc, VarDefine var, string newVarName)
        {
            HashSet<BeanDefine> deps = new HashSet<BeanDefine>();
            doc.BeanDefine.Depends(deps);
            if (deps.Contains(var.Parent))
            {
                GridData gridDataTmp = new GridData(doc);
                doc.BeanDefine.BuildGridColumns(gridDataTmp, 0, new ColumnTag(ColumnTag.ETag.Normal), -1);
                HashSet<Bean.VarData> varDatas = new HashSet<Bean.VarData>();
                var param = new Bean.UpdateParam()
                {
                    UpdateType = Bean.EUpdate.CallAction,
                    UpdateAction = (GridData grid, int col, ColumnTag.VarInfo varInfo, Bean.VarData varData) =>
                    {
                        if (varInfo.Define == var)
                            varDatas.Add(varData);
                    },
                };
                foreach (var bean in doc.Beans)
                {
                    int insertIndex = gridDataTmp.RowCount;
                    gridDataTmp.InsertRow(insertIndex);
                    int colIndex = 0;
                    if (bean.Update(gridDataTmp, gridDataTmp.GetRow(insertIndex), ref colIndex, 0, param))
                        break;
                }
                foreach (var varData in varDatas)
                {
                    varData.Parent.RenameVar(varData.Name, newVarName);
                }
            }
        }
        */

        private void UpdateEnumDefine(EnumDefine enumDefine)
        {
            if (null == enumDefine)
                return;

            for (int i = 0; i < define.RowCount; ++i)
            {
                DataGridViewCellCollection cells = define.Rows[i].Cells;
                if (cells["BeanLocked"].Tag == enumDefine)
                {
                    cells["VarName"].Value = enumDefine.FullName();
                    break;
                }
            }
        }

        /// <summary>
        /// 在当前搜索使用参数变量的的所有列的值，并且构建EnumDefine。
        /// 不合法的Enum名字忽略。
        /// 仅在 FromDefine 里面调用。
        /// </summary>
        /// <param name="var"></param>
        public void BuildEnumFor(VarDefine var)
        {
            if (FormMain.Instance.Tabs.SelectedTab == null)
                return;

            var enumDefine = new EnumDefine(var.Parent, var.Name);
            DataGridView grid = (DataGridView)FormMain.Instance.Tabs.SelectedTab.Controls[0];
            for (int i = 0; i < grid.ColumnCount; ++i)
            {
                if ((grid.Columns[i].Tag as ColumnTag).PathLast.Define == var)
                {
                    for (int j = 0; j < grid.RowCount - 1; ++j)
                    {
                        var valueName = grid[i, j].Value as string;
                        if (null == Tools.VerifyName(valueName, CheckNameType.CheckOnly))
                        {
                            // 增加enum定义。
                            enumDefine.ChangeValueName(new EnumDefine.ValueDefine(enumDefine, "", -1), valueName);
                        }
                    }
                }
            }
            var.Parent.AddEnumDefine(enumDefine);
            InsertEnumDefine(define.RowCount, enumDefine.FullName(), enumDefine);
        }

        private void define_DragEnter(object sender, DragEventArgs e)
        {
            if (GetDragRow(e, out var rowIndex, out var row))
            {
                e.Effect = DragDropEffects.Move;
            }
            else
            {
                e.Effect = DragDropEffects.None;
            }
        }

        private void define_DragOver(object sender, DragEventArgs e)
        {
            if (GetDragRow(e, out var rowIndex, out var row))
            {
                e.Effect = DragDropEffects.Move;
            }
            else
            {
                e.Effect = DragDropEffects.None;
            }
        }

        private bool GetDragRow(DragEventArgs e, out int dropIndex, out DataGridViewRow dragRow)
        {
            dropIndex = -1;
            dragRow = null;

            //将屏幕坐标转换为控件坐标之后获取该坐标下DataGridView的行和列
            Point client = define.PointToClient(new Point(e.X, e.Y));
            DataGridView.HitTestInfo ht = define.HitTest(client.X, client.Y);

            if (ht.RowIndex < 0)
                return false;

            if (e.Data.GetDataPresent(typeof(DataGridViewRow)))
            {
                DataGridViewRow dragRowTmp = (DataGridViewRow)e.Data.GetData(typeof(DataGridViewRow));
                if (dragRowTmp.DataGridView != define)
                    return false;
                DataGridViewRow dropRow = define.Rows[ht.RowIndex];
                VarDefine dragVarDefine = dragRowTmp.Cells["VarName"].Tag as VarDefine;
                VarDefine dropVarDefine = dropRow.Cells["VarName"].Tag as VarDefine;
                if (null == dropVarDefine)
                    return false;
                if (dragVarDefine.Parent != dropVarDefine.Parent) // check same Bean
                    return false;
                if (dropVarDefine.Name == null) // check is special row "AddVariable"
                    return false;

                dropIndex = ht.RowIndex;
                dragRow = dragRowTmp;
                return true;
            }
            return false;
        }

        private void define_DragDrop(object sender, DragEventArgs e)
        {
            if (GetDragRow(e, out var dropIndex, out var dragRow))
            {
                DataGridViewRow dropRow = define.Rows[dropIndex];
                if (dropRow != dragRow)
                {
                    VarDefine dragVarDefine = dragRow.Cells["VarName"].Tag as VarDefine;
                    VarDefine dropVarDefine = dropRow.Cells["VarName"].Tag as VarDefine;
                    dragVarDefine.Parent.Move(dragVarDefine, dropVarDefine);
                    FormMain.Instance.ReloadAllGridIfContains(dragVarDefine);
                    define.Rows.Remove(dragRow);
                    define.Rows.Insert(dropIndex, dragRow);
                }
            }
        }

        private bool TryStartDragDrop(DataGridViewCellMouseEventArgs e)
        {
            if (e.ColumnIndex != -1)
                return false;

            if (e.RowIndex < 0)
                return false;

            DataGridViewRow row = define.Rows[e.RowIndex];
            VarDefine varDefine = row.Cells["VarName"].Tag as VarDefine;
            if (null == varDefine || null == varDefine.Name)
                return false;

            define.DoDragDrop(row, DragDropEffects.Move);
            return true;
        }

        private Rectangle DragBoxFromMouseDown;

        private void define_CellMouseMove(object sender, DataGridViewCellMouseEventArgs e)
        {
            if ((e.Button & MouseButtons.Left) == MouseButtons.Left)
            {
                // If the mouse moves outside the rectangle, start the drag.
                if (DragBoxFromMouseDown != Rectangle.Empty && !DragBoxFromMouseDown.Contains(e.X, e.Y))
                {
                    if (TryStartDragDrop(e))
                        DragBoxFromMouseDown = Rectangle.Empty;
                }
            }
        }

        private void define_CellBeginEdit(object sender, DataGridViewCellCancelEventArgs e)
        {
            if (e.ColumnIndex < 0)
                return;

            if (e.RowIndex < 0)
                return;

            DataGridViewCellCollection cells = define.Rows[e.RowIndex].Cells;

            if (cells["BeanLocked"].Tag != null)
            {
                var beanDefine = cells["BeanLocked"].Tag as BeanDefine;
                cells["VarName"].Value = beanDefine.Name; // 编辑的时候去掉path，只编辑名字。
                return;
            }
        }

        [DllImport("User32.dll", EntryPoint = "PostMessage")]
        public static extern int PostMessage(
            IntPtr hWnd,        // 信息发往的窗口的句柄
            int Msg,            // 消息ID
            int wParam,         // 参数1
            int lParam            // 参数2
        );

        const int WM_APP = 0x8000;
        const int WM_RELOAD_DEFINE = WM_APP + 1;

        private void PostReleadDefine()
        {
            PostMessage(this.Handle, WM_RELOAD_DEFINE, 0, 0);
        }

        protected override void WndProc(ref Message m)
        {
            switch (m.Msg)
            {
                case WM_RELOAD_DEFINE:
                    LoadDefine();
                    return;
            }
            base.WndProc(ref m);
        }

        private bool CellEndEditUpdateBeanDefineRow(DataGridViewCellCollection cells, DataGridViewCellEventArgs e)
        {
            var cellBeanLocked = cells["BeanLocked"];
            if (cellBeanLocked.Tag != null)
            {
                string col = define.Columns[e.ColumnIndex].Name;
                switch (col)
                {
                    case "VarName":
                        (cellBeanLocked.Tag as BeanDefine).Name = cells[e.ColumnIndex].Value as string;
                        PostReleadDefine();
                        break;
                }
                return true;
            }
            return false;
        }

        private bool CellEndEditUpdateEnumValueDefineRow(DataGridViewCellCollection cells,
            DataGridViewCell cellVarName, DataGridViewCellEventArgs e)
        {
            if (cellVarName.Tag is EnumDefine.ValueDefine valueDefine)
            {
                string col = define.Columns[e.ColumnIndex].Name;
                switch (col)
                {
                    case "VarName":
                        {
                            string newStrValue = cellVarName.Value as string;
                            if (string.IsNullOrEmpty(newStrValue))
                            {
                                // 新增ValueDefine，如果没有输入就看做取消。删除掉。
                                // 此时ValueDefine并没有被加入EnumDefine.
                                define.Rows.RemoveAt(e.RowIndex);
                            }
                            else if (false == valueDefine.Name.Equals(newStrValue))
                            {
                                valueDefine.Parent.ChangeValueName(valueDefine, newStrValue);
                                cells["VarValue"].Value = valueDefine.Value.ToString();
                            }
                        }
                        break;

                    case "VarValue":
                        {
                            string newStrValue = cells[col].Value as string;
                            int newValue = string.IsNullOrEmpty(newStrValue) ? -1 : int.Parse(newStrValue);
                            if (valueDefine.Value != newValue)
                            {
                                valueDefine.Value = newValue;
                            }
                        }
                        break;

                    case "VarComment":
                        valueDefine.Comment = cells[col].Value as string;
                        break;
                }
                return true;
            }
            return false;
        }

        private bool CellEndEditUpdateVarValueDefineRow(DataGridViewCellCollection cells,
            DataGridViewCell cellVarName, DataGridViewCellEventArgs e)
        {
            if (!(cellVarName.Tag is VarDefine))
                return false;

            VarDefine var = cellVarName.Tag as VarDefine;
            string colName = define.Columns[e.ColumnIndex].Name;
            switch (colName)
            {
                case "VarName":
                    //string oldVarName = var.Name;
                    var.Name = cells[colName].Value as string;
                    PostReleadDefine();
                    // 仅在Type==Enum时才有效。其他时候什么都不做。
                    //UpdateEnumDefine(var.Parent.ChangeEnumName(oldVarName, var.Name));
                    /*
                    string newVarName = cells[colName].Value as string;
                    if (false == var.Name.Equals(newVarName))
                    {
                        string oldVarName = var.Name;
                        string oldForeignName = var.Parent.FullName() + ":" + var.Name;
                        var.Name = newVarName;
                        string newForengnName = var.Parent.FullName() + ":" + var.Name;
                        var.Name = oldVarName; // 修改数据里面的名字需要用到旧名字。最后再来修改。

                        FormMain.Instance.Documents.LoadAllDocument();
                        FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                        {
                            var doc = file.Document;
                            if (null == doc)
                                return true;
                            UpdateData(doc, var, newVarName);
                            doc.BeanDefine.UpdateForeign(oldForeignName, newForengnName);
                            return true;
                        });
                        UpdateEnumDefine(var.Parent.ChangeEnumName(var.Name, newVarName));
                        var.Name = newVarName;
                        // TODO 还需要更新 var 所在 BeanDefine 的名字，以及相关引用。好像就实现 Bean 改名了。 
                        FormMain.Instance.ReloadAllGridIfContains(var);
                    }
                    */
                    break;

                case "VarType":
                    VarDefine.EType newType = (VarDefine.EType)cells[colName].Value;
                    if (var.Type != newType)
                    {
                        var.Type = newType;
                        if (newType == VarDefine.EType.Enum)
                        {
                            BuildEnumFor(var);
                        }
                    }
                    break;

                case "VarValue":
                    string newValue = cells[colName].Value as string;
                    if (newValue == null || newValue.CompareTo(var.Value) == 0)
                        break;

                    if (DialogResult.OK != MessageBox.Show("修改List引用的Bean类型导致旧数据全部清除。",
                        "确认", MessageBoxButtons.OKCancel))
                        break;

                    // 修改list引用的bean：删除旧变量，使用相同的名字增加一个变量。
                    DeleteVariable(e.RowIndex, var, false);
                    (VarDefine varNew, bool create, string err) = var.Parent.AddVariable(var.Name, VarDefine.EType.List, newValue);
                    if (null != varNew)
                    {
                        UpdateWhenAddVariable(e.RowIndex, varNew, create);
                        FormMain.Instance.UpdateWhenAddVariable(varNew);
                    }
                    else
                    {
                        cells[colName].Value = var.Value; // restore
                        MessageBox.Show(err);
                    }
                    break;

                case "VarForeign":
                    var.Foreign = cells[colName].Value as string;
                    break;

                case "VarProperties":
                    var.Properties = cells[colName].Value as string;
                    break;

                case "VarDefault":
                    var.Default = cells[colName].Value as string;
                    break;

                case "VarComment":
                    var.Comment = cells[colName].Value as string;
                    break;
            }
            return true;
        }

        private void define_CellEndEdit(object sender, DataGridViewCellEventArgs e)
        {
            DataGridViewCellCollection cells = define.Rows[e.RowIndex].Cells;
            if (CellEndEditUpdateBeanDefineRow(cells, e))
                return;

            DataGridViewCell cellVarName = cells["VarName"];
            if (null == cellVarName.Tag)
                return;

            if (CellEndEditUpdateEnumValueDefineRow(cells, cellVarName, e))
                return;

            if (CellEndEditUpdateVarValueDefineRow(cells, cellVarName, e))
                return;

        }
    }
}
