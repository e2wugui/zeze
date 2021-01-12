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
    public partial class FormError : Form
    {
        public FormError()
        {
            InitializeComponent();
            // Double buffering can make DGV slow in remote desktop
            if (!System.Windows.Forms.SystemInformation.TerminalServerSession)
            {
                Type dgvType = grid.GetType();
                PropertyInfo pi = dgvType.GetProperty("DoubleBuffered",
                  BindingFlags.Instance | BindingFlags.NonPublic);
                pi.SetValue(grid, true, null);
            }
        }

        private void FormError_FormClosing(object sender, FormClosingEventArgs e)
        {
            e.Cancel = true;
            Hide();
        }

        private void FormError_Load(object sender, EventArgs e)
        {
            if (FormMain.Instance.ConfigEditor.FormErrorLocation != null)
                this.Location = FormMain.Instance.ConfigEditor.FormErrorLocation;
            if (FormMain.Instance.ConfigEditor.FormErrorSize != null)
                this.Size = FormMain.Instance.ConfigEditor.FormErrorSize;
            this.WindowState = FormMain.Instance.ConfigEditor.FormErrorState;

        }

        public class Error
        {
            public Property.ErrorLevel Level { get; set; }
            public string Description { get; set; }
            public DataGridViewRow Row { get; set; }
        }

        private Dictionary<GridData.Cell, SortedDictionary<string, Error>> Errors
            = new Dictionary<GridData.Cell, SortedDictionary<string, Error>>(new IdentityEqualityComparer());

        public void AddError(HashSet<GridData.Cell> cells, Property.IProperty p, Property.ErrorLevel level, string desc)
        {
            foreach (var cell in cells)
                AddError(cell, p, level, desc);
        }

        public delegate void AddErrorAction(GridData.Cell cell, Property.IProperty p, Property.ErrorLevel level, string desc);
        public AddErrorAction OnAddError { get; set; }

        private delegate void DelegateInvoke();

        public void AddError(GridData.Cell cell, Property.IProperty p, Property.ErrorLevel level, string desc)
        {
            // 在调用线程中回调。
            if (OnAddError != null)
                OnAddError(cell, p, level, desc);

            if (this.InvokeRequired)
            {
                DelegateInvoke d = delegate { _AddError(cell, p, level, desc); };
                this.BeginInvoke(d);
            }
            else
            {
                _AddError(cell, p, level, desc);
            }
        }

        private void _AddError(GridData.Cell cell, Property.IProperty p, Property.ErrorLevel level, string desc)
        {
            if (IsDisposed) // 某些 verify 是异步的，可能在窗口关闭后返回。
                return;

            if (false == Errors.TryGetValue(cell, out var errors))
                Errors.Add(cell, errors = new SortedDictionary<string, Error>());

            if (errors.ContainsKey(p.Name)) // 同一个cell相同的prop只报告一次。
                return;

            grid.Rows.Add();
            DataGridViewRow row = grid.Rows[grid.RowCount - 1];
            row.Cells["Level"].Value = System.Enum.GetName(typeof(Property.ErrorLevel), level);
            row.Cells["Level"].Tag = cell;
            row.Cells["Description"].Value = desc;
            row.Cells["File"].Value = cell.Row.GridData.Document.RelateName;

            errors.Add(p.Name, new Error() { Level = level, Description = desc, Row = row, });
            UpdateErrorCell(cell, errors);
        }

        public void RemoveError(GridData.Cell cell, Property.IProperty p)
        {
            if (this.InvokeRequired)
            {
                DelegateInvoke d = delegate { _RemoveError(cell, p); };
                this.BeginInvoke(d);
            }
            else
            {
                _RemoveError(cell, p);
            }
        }

        private void _RemoveError(GridData.Cell cell, Property.IProperty p)
        {
            if (false == Errors.TryGetValue(cell, out var errors))
                return;

            if (false == errors.TryGetValue(p.Name, out var error))
                return;

            errors.Remove(p.Name);
            grid.Rows.Remove(error.Row);

            if (errors.Count == 0)
            {
                Errors.Remove(cell);
                cell.BackColor = Color.White;
                cell.ToolTipText = null;
                cell.Invalidate();
            }
            else
            {
                UpdateErrorCell(cell, errors);
            }
        }

        private void UpdateErrorCell(GridData.Cell cell, SortedDictionary<string, Error> errors)
        {
            Property.ErrorLevel max = Property.ErrorLevel.Warn;
            StringBuilder sb = new StringBuilder();
            foreach (var e in errors)
            {
                sb.Append(e.Key).Append(": ").Append(e.Value.Description).Append(Environment.NewLine);
                if (e.Value.Level > max)
                    max = e.Value.Level;
            }
            cell.BackColor = max == Property.ErrorLevel.Error ? Color.Red : Color.Yellow;
            cell.ToolTipText = sb.ToString();
            cell.Invalidate();
        }

        private void grid_CellDoubleClick(object sender, DataGridViewCellEventArgs e)
        {
            if (e.RowIndex < 0)
                return;
            // TODO Buid过程可能在没有View的时候需要记录错误。处理双击定位错误时需要处理这种情况。
            /*
            var maincell = grid.Rows[e.RowIndex].Cells["Level"].Tag as GridData.Cell;
            DataGridView maingrid = maincell.Row.GridData.View;
            FormMain.Instance.Tabs.SelectedTab = maingrid.Parent as TabPage;
            maingrid.FirstDisplayedCell = maincell;
            maingrid.CurrentCell = maincell;
            */
        }

        public void RemoveErrorByGrid(GridData gridedit)
        {
            if (this.InvokeRequired)
            {
                DelegateInvoke d = delegate { _RemoveErrorByGrid(gridedit); };
                this.BeginInvoke(d);
            }
            else
            {
                _RemoveErrorByGrid(gridedit);
            }
        }

        private void _RemoveErrorByGrid(GridData gridedit)
        {
            if (null == gridedit)
                return;

            // 现在只显示打开grid的文件错误。如果要显示所有文件的。
            // 就不能记住Cell的引用，应该使用文件名+(ColIndex, RowIndex)。
            // 但是由于文件会变化，(ColIndex, RowIndex)可能不再准确（看看怎么处理这种情况）。

            grid.SuspendLayout();
            Dictionary<GridData.Cell, int> removed
                = new Dictionary<GridData.Cell, int>(new IdentityEqualityComparer());
            for (int i = grid.RowCount - 1; i >= 0; --i)
            {
                GridData.Cell c = grid.Rows[i].Cells["Level"].Tag as GridData.Cell;
                if (c.Row.GridData == gridedit)
                {
                    removed[c] = 1;
                    c.BackColor = Color.White;
                    c.ToolTipText = null;
                    c.Invalidate();
                    grid.Rows.RemoveAt(i);
                }
            }
            grid.ResumeLayout();

            foreach (var c in removed.Keys)
            {
                Errors.Remove(c);
            }
        }

        public void Clear()
        {
            Errors.Clear();
            grid.Rows.Clear();
        }

        public int GetErrorCount()
        {
            int error = 0;
            foreach (var errorsByCell in Errors.Values)
            {
                foreach (var errors in errorsByCell.Values)
                {
                    if (errors.Level == Property.ErrorLevel.Error)
                        ++error;
                }
            }
            return error;
        }
    }
}
