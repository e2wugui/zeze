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
    public partial class FormError : Form
    {
        public FormMain FormMain { get; set; }

        public FormError()
        {
            InitializeComponent();
        }

        private void FormError_FormClosing(object sender, FormClosingEventArgs e)
        {
            e.Cancel = true;
            Hide();
        }

        private void FormError_Load(object sender, EventArgs e)
        {
            if (FormMain.ConfigEditor.FormErrorLocation != null)
                this.Location = FormMain.ConfigEditor.FormErrorLocation;
            if (FormMain.ConfigEditor.FormErrorSize != null)
                this.Size = FormMain.ConfigEditor.FormErrorSize;
            this.WindowState = FormMain.ConfigEditor.FormErrorState;

        }

        public void ReportVerifyResult(Property.VerifyParam param, HashSet<DataGridViewCell> cells = null,
            Property.Result result = Property.Result.Ok, string tip = null)
        {
            Color back = Color.White;
            switch (result)
            {
                case Property.Result.Ok:
                    back = Color.White;
                    break;

                case Property.Result.Warn:
                    back = Color.Yellow;
                    break;

                case Property.Result.Error:
                    back = Color.Red;
                    break;
            }

            if (null == cells)
            {
                // update current cell
                DataGridViewCell cell = param.Grid[param.ColumnIndex, param.RowIndex];
                cell.ToolTipText = tip; // last tip
                if (cell.Style.BackColor != back)
                {
                    cell.Style.BackColor = back;
                }
            }
            else
            {
                foreach (var cell in cells)
                {
                    cell.ToolTipText = tip; // last tip
                    if (cell.Style.BackColor != back)
                        cell.Style.BackColor = back;
                }
            }
        }
    }
}
