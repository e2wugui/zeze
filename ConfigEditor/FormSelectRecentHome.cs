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
    public partial class FormSelectRecentHome : Form
    {
        public FormSelectRecentHome()
        {
            InitializeComponent();
        }

        public void InitComboRecentHomes(EditorConfig Config)
        {
            comboBoxRecentHomes.Items.Clear();
            foreach (var home in Config.RecentHomes)
            {
                comboBoxRecentHomes.Items.Add(home);
            }
            if (comboBoxRecentHomes.Items.Count > 0)
                comboBoxRecentHomes.SelectedIndex = 0;
        }
        public ComboBox ComboBoxRecentHomes { get { return comboBoxRecentHomes; } }

        private void buttonBrowse_Click(object sender, EventArgs e)
        {
            this.folderBrowserDialog.Description = "选择配置目录(Home)";
            this.folderBrowserDialog.SelectedPath = comboBoxRecentHomes.Text;
            if (DialogResult.OK == this.folderBrowserDialog.ShowDialog(this))
            {
                comboBoxRecentHomes.Text = this.folderBrowserDialog.SelectedPath;
            }
        }
    }
}
