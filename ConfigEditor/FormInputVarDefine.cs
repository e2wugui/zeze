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
        }

        private void FormInputVarDefine_Load(object sender, EventArgs e)
        {

        }

        public TextBox TextBoxVarName { get { return textBoxVarName; } }
        public CheckBox CheckBoxIsList { get { return checkBoxIsList;  } }
        public TextBox TextBoxListRefBeanName {  get { return textBoxListRefBeanName; } }
    }
}
