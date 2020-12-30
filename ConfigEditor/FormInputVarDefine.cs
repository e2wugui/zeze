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
    public partial class FormInputVarDefine : Form
    {
        public FormInputVarDefine()
        {
            InitializeComponent();
            comboBoxTypes.DataSource = System.Enum.GetValues(typeof(VarDefine.EType));
            comboBoxTypes.SelectedIndex = 0;
        }

        public TextBox TextBoxVarName { get { return textBoxVarName; } }
        public ComboBox ComboBoxVarType { get { return comboBoxTypes;  } }
        public TextBox TextBoxListRefBeanName {  get { return textBoxListRefBeanName; } }
    }
}
