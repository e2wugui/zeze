using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
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
            Document doc = (Document)grid.Tag;

            LoadDocument(doc);
        }

        private void LoadDocument(Document doc)
        {
            SortedDictionary<string, BeanDefine> sortedByFullName = new SortedDictionary<string, BeanDefine>();
            doc.BeanDefine.ForEach((BeanDefine bd) => sortedByFullName.Add(bd.FullName(), bd));

            define.SuspendLayout();
            foreach (var e in sortedByFullName)
            {
                // row for bean start
                define.Rows.Add();
                DataGridViewCellCollection cellsBeanStart = define.Rows[define.RowCount - 1].Cells;
                for (int i = 0; i < cellsBeanStart.Count; ++i)
                    cellsBeanStart[i].ReadOnly = true;
                DataGridViewCell cellLocked = cellsBeanStart["BeanLocked"];
                cellLocked.Value = e.Value.IsLocked ? "Yes" : "No";
                cellLocked.Tag = e.Value; // BeanDefine
                cellsBeanStart["VarName"].Value = e.Key; // bean full name

                // row for vars
                foreach (var v in e.Value.Variables)
                {
                    InsertVariable(define.RowCount, v);
                }

                // row for bean end
                define.Rows.Add();
                DataGridViewCellCollection cellsBeanEnd = define.Rows[define.RowCount - 1].Cells;
                for (int i = 0; i < cellsBeanEnd.Count; ++i)
                    cellsBeanEnd[i].ReadOnly = true;
                DataGridViewCell cellBeanEnd = cellsBeanEnd["VarName"];
                cellBeanEnd.Value = ",";
                cellBeanEnd.Tag = new VarDefine(e.Value);
                cellBeanEnd.ToolTipText = "双击增加变量（数据列）";
            }
            define.ResumeLayout();
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
            cellsVar["VarComment"].Value = var.Comment;
        }

        private void define_CellMouseDown(object sender, DataGridViewCellMouseEventArgs e)
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
            if (define.CurrentCell == null)
                return;
            VarDefine var = define.Rows[define.CurrentCell.RowIndex].Cells["VarName"].Tag as VarDefine;
            if (null == var)
                return;

            var beanDeleted = FormMain.DeleteVariable(var);

            define.SuspendLayout();
            define.Rows.RemoveAt(define.CurrentCell.RowIndex);
            if (null != beanDeleted && beanDeleted.Document == var.Parent.Document)
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
                        VarDefine var = FormMain.AddVariable(cellVarName.Tag as VarDefine);
                        if (null != var)
                            InsertVariable(e.RowIndex, var);
                    }
                    break;

                case "BeanLocked":
                    DataGridViewCell cellBeanLocked = define[e.ColumnIndex, e.RowIndex];
                    var bean = cellBeanLocked.Tag as BeanDefine;
                    bean.IsLocked = !bean.IsLocked;
                    bean.Document.IsChanged = true;
                    cellBeanLocked.Value = bean.IsLocked ? "Yes" : "No";
                    break;
            }
        }
    }
}
