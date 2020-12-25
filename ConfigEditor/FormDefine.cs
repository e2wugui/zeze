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

            foreach (var e in sortedByFullName)
            {
                // row for bean start
                define.Rows.Add();
                DataGridViewCellCollection cellsBeanStart = define.Rows[define.RowCount - 1].Cells;
                for (int i = 0; i < cellsBeanStart.Count; ++i)
                    cellsBeanStart[i].ReadOnly = true;
                cellsBeanStart["BeanLocked"].Value = e.Value.IsLocked ? "Yes" : "No";
                cellsBeanStart["VarName"].Value = e.Key; // bean full name

                // row for vars
                foreach (var v in e.Value.Variables)
                {
                    define.Rows.Add();
                    DataGridViewCellCollection cellsVar = define.Rows[define.RowCount - 1].Cells;
                    cellsVar["BeanLocked"].ReadOnly = true;
                    cellsVar["VarName"].Value = v.Name;
                    cellsVar["VarType"].Value = v.Type;
                    cellsVar["VarValue"].Value = v.Value;
                    cellsVar["VarForeign"].Value = v.Foreign;
                    cellsVar["VarProperties"].Value = v.Properties;
                    cellsVar["VarComment"].Value = v.Comment;
                }

                // row for bean end
                define.Rows.Add();
                DataGridViewCellCollection cellsBeanEnd = define.Rows[define.RowCount - 1].Cells;
                for (int i = 0; i < cellsBeanEnd.Count; ++i)
                    cellsBeanEnd[i].ReadOnly = true;
                DataGridViewCell cellBeanEnd = cellsBeanEnd["VarName"];
                cellBeanEnd.Value = ",";
                cellBeanEnd.ToolTipText = "双击增加变量（数据列）";
            }
        }
    }
}
