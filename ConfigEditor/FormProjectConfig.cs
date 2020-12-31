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
    public partial class FormProjectConfig : Form
    {
        public FormProjectConfig()
        {
            InitializeComponent();
        }

        private FormMain _FormMain;

        public FormMain FormMain
        {
            get
            {
                return _FormMain;
            }
            set
            {
                _FormMain = value;
                LoadProjectConfigToGrid();
            }
        }

        private void LoadProjectConfigToGrid()
        {
            ProjectConfig config = _FormMain.ConfigProject;
            var properties = config.GetType().GetProperties();

            foreach (var property in properties)
            {
                var attrs = property.GetCustomAttributes(typeof(ShowNameAttribute), false);
                if (attrs.Length == 0)
                    continue;
                var showName = attrs[0] as ShowNameAttribute;

                gridConfig.Rows.Add();
                DataGridViewCellCollection cells = gridConfig.Rows[gridConfig.RowCount - 1].Cells;
                DataGridViewCell cellConfigName = cells["ConfigName"];
                cellConfigName.Value = showName.Name;
                cellConfigName.Tag = property;
                cellConfigName.ToolTipText = showName.Tips;
                cells["ConfigValue"].Value = property.GetValue(config);
            }
        }

        private void gridConfig_CellValidating(object sender, DataGridViewCellValidatingEventArgs e)
        {
            // add Validating here
        }

        private void gridConfig_CellEndEdit(object sender, DataGridViewCellEventArgs e)
        {
            if (e.RowIndex < 0 || e.ColumnIndex < 0)
                return;

            DataGridViewCellCollection cells = gridConfig.Rows[e.RowIndex].Cells;
            var property = cells["ConfigName"].Tag as PropertyInfo;
            property.SetValue(_FormMain.ConfigProject, cells["ConfigValue"].Value);
        }
    }
}
