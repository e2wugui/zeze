using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormDefine : Form
    {
        public FormMain FormMain { get; set; }

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
                FormMain.FormDefine = null;
        }

        public void LoadDefine()
        {
            define.Rows.Clear();

            if (null == FormMain.Tabs.SelectedTab)
                return; // no file

            DataGridView grid = (DataGridView)FormMain.Tabs.SelectedTab.Controls[0];
            Document = (Document)grid.Tag;
            LoadDocument(Document);
        }

        Document Document { get; set; } // 现在还不需要记住这个，保留来以后同时装载多个Document的BeanDefine

        public bool IsLoadedDocument(Document doc)
        {
            return doc == Document;
        }

        private void LoadDocument(Document doc)
        {
            SortedDictionary<string, BeanDefine> BeanDefines = new SortedDictionary<string, BeanDefine>();
            doc.BeanDefine.ForEach((BeanDefine bd) => { BeanDefines.Add(bd.FullName(), bd); return true; });

            define.SuspendLayout();
            foreach (var e in BeanDefines)
            {
                InsertBeanDefine(define.RowCount, e.Key, e.Value);
            }
            define.ResumeLayout();
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
            cellBeanEnd.Tag = new VarDefine(bean);
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

        private void DeleteVariable(int rowIndex, VarDefine var, bool confirm)
        {
            var beanDeleted = FormMain.DeleteVariable(var, confirm);

            define.SuspendLayout();
            define.Rows.RemoveAt(rowIndex);
            if (null != beanDeleted && IsLoadedDocument(beanDeleted.Document))
            {
                // remove bean
                int i = 0;
                for (; i < define.RowCount; ++i)
                {
                    if (define.Rows[i].Cells["BeanLocked"].Tag == beanDeleted)
                        break;
                }
                while (i < define.RowCount)
                {
                    define.Rows.RemoveAt(i);
                    if (i >= define.RowCount)
                        break;
                    DataGridViewCell cell = define.Rows[i].Cells["BeanLocked"];
                    if (cell.Tag != null && cell.Tag != beanDeleted)
                        break;
                }
            }
            define.ResumeLayout();
        }

        private void deleteVariableColumnToolStripMenuItem_Click(object sender, EventArgs e)
        {
            if (define.CurrentCell == null)
                return;
            VarDefine var = define.Rows[define.CurrentCell.RowIndex].Cells["VarName"].Tag as VarDefine;
            if (null == var)
                return;
            DeleteVariable(define.CurrentCell.RowIndex, var, true);
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
                    if (cellVarName.Value as string == ",")
                    {
                        (VarDefine var, bool create) = FormMain.AddVariable(cellVarName.Tag as VarDefine);
                        UpdateWhenAddVariable(e.RowIndex, var, create);
                    }
                    break;

                case "BeanLocked":
                    DataGridViewCell cellBeanLocked = define[e.ColumnIndex, e.RowIndex];
                    var bean = cellBeanLocked.Tag as BeanDefine;
                    if (null != bean)
                    {
                        bean.Locked = !bean.Locked;
                        bean.Document.IsChanged = true;
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
                foreach (var p in FormMain.PropertyManager.Properties)
                {
                    if (p.Value.ButtonChecked)
                    {
                        current.Add(p.Value);
                    }
                    p.Value.Button = null;
                }
                var.Properties = FormMain.PropertyManager.BuildString(current);
                var.Parent.Document.IsChanged = true;
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
                    cell.ToolTipText = FormMain.PropertyManager.BuildToolTipText(cell.Value as string);
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
                e.Cancel = true;
                MessageBox.Show("TODO modify bean name");
                return;
            }

            // modify Var
            VarDefine var = cells["VarName"].Tag as VarDefine;
            if (null == var || null == var.Name) // var.Name is null when row is ',' for addvar
            {
                e.Cancel = true;
                MessageBox.Show("只有var的行才允许编辑，其他的应该都设置了 ReadOnly. 怎么到这里的？");
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
                        if (string.IsNullOrEmpty(newValue))
                        {
                            FormMain.OpenDocument(newValue, out var r);
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
                        if (false == VarDefine.CheckType(var.Type, e.FormattedValue as string))
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

        private void UpdateData(Document doc, VarDefine var, string newVarName)
        {
            HashSet<BeanDefine> deps = new HashSet<BeanDefine>();
            doc.BeanDefine.Depends(deps);
            if (deps.Contains(var.Parent))
            {
                DataGridView gridTmp = new DataGridView();
                gridTmp.Visible = false;
                gridTmp.SuspendLayout();
                doc.BeanDefine.BuildGridColumns(gridTmp, 0, new ColumnTag(ColumnTag.ETag.Normal), -1);
                HashSet<Bean.VarData> varDatas = new HashSet<Bean.VarData>();
                var param = new Bean.UpdateParam()
                {
                    UpdateType = Bean.EUpdate.CallAction,
                    UpdateAction = (DataGridView grid, int col, ColumnTag.VarInfo varInfo, Bean.VarData varData) =>
                    {
                        if (varInfo.Define == var)
                            varDatas.Add(varData);
                    },
                };
                foreach (var bean in doc.Beans)
                {
                    gridTmp.Rows.Add();
                    DataGridViewCellCollection cellsTmp = gridTmp.Rows[gridTmp.RowCount - 1].Cells;
                    int colIndex = 0;
                    if (bean.Update(gridTmp, cellsTmp, ref colIndex, 0, param))
                        break;
                }
                foreach (var varData in varDatas)
                {
                    varData.Parent.RenameVar(varData.Name, newVarName);
                }
                doc.IsChanged = varDatas.Count > 0;
                gridTmp.Dispose();
            }
        }

        private void define_CellEndEdit(object sender, DataGridViewCellEventArgs e)
        {
            if (e.ColumnIndex < 0)
                return;

            if (e.RowIndex < 0)
                return;

            DataGridViewCellCollection cells = define.Rows[e.RowIndex].Cells;
            VarDefine var = cells["VarName"].Tag as VarDefine;
            if (null == var)
                return;

            string colName = define.Columns[e.ColumnIndex].Name;
            switch (colName)
            {
                case "VarName":
                    string newVarName = cells[colName].Value as string;
                    if (false == var.Name.Equals(newVarName))
                    {
                        string oldVarName = var.Name;
                        string oldForeignName = var.Parent.FullName() + ":" + var.Name;
                        var.Name = newVarName;
                        string newForengnName = var.Parent.FullName() + ":" + var.Name;
                        var.Name = oldVarName; // 修改数据里面的名字需要用到旧名字。最后再来修改。

                        FormMain.LoadAllDocument();
                        foreach (var doc in FormMain.Documents.Values)
                        {
                            UpdateData(doc, var, newVarName);
                            doc.BeanDefine.UpdateForeign(oldForeignName, newForengnName);
                        }
                        var.Name = newVarName;
                        var.Parent.Document.IsChanged = true;
                        FormMain.ReloadAllGridIfContains(var);
                    }
                    break;

                case "VarType":
                    VarDefine.EType newType = (VarDefine.EType)cells[colName].Value;
                    if (var.Type != newType)
                    {
                        var.Type = newType;
                        var.Parent.Document.IsChanged = true;
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
                        FormMain.UpdateWhenAddVariable(varNew);
                    }
                    else
                    {
                        cells[colName].Value = var.Value; // restore
                        MessageBox.Show(err);
                    }
                    break;

                case "VarForeign":
                    var.Foreign = cells[colName].Value as string;
                    var.Parent.Document.IsChanged = true;
                    break;

                case "VarProperties":
                    var.Properties = cells[colName].Value as string;
                    var.Parent.Document.IsChanged = true;
                    break;

                case "VarDefault":
                    var.Default = cells[colName].Value as string;
                    var.Parent.Document.IsChanged = true;
                    break;

                case "VarComment":
                    var.Comment = cells[colName].Value as string;
                    var.Parent.Document.IsChanged = true;
                    break;
            }
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
                    FormMain.ReloadAllGridIfContains(dragVarDefine);
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
    }
}
