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
    public partial class FormPopupListBox : Form
    {
        public FormPopupListBox()
        {
            InitializeComponent();
        }

        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams ret = base.CreateParams;
                ret.Style = (int)WindowStyles.WS_THICKFRAME | (int)WindowStyles.WS_CHILD;
                ret.ExStyle |= (int)WindowStyles.WS_EX_NOACTIVATE | (int)WindowStyles.WS_EX_TOOLWINDOW;
                ret.X = this.Location.X;
                ret.Y = this.Location.Y;
                return ret;
            }
        }

        public ListBox ListBox => listBox1;

        private void listBox1_MouseDown(object sender, MouseEventArgs e)
        {
            int indexMouseDown = listBox1.IndexFromPoint(e.X, e.Y);
            if (indexMouseDown >= 0)
                listBox1.SelectedIndex = indexMouseDown;
        }

        const int WM_KEYDOWN = 0x100;
        const int VK_UP = 0x26;
        const int VK_DOWN = 0x28;

        public bool ProcessGridKeyPreview(ref Message m)
        {
            if (m.Msg != WM_KEYDOWN)
                return false;

            int indexSel = listBox1.SelectedIndex;
            switch (m.WParam.ToInt32())
            {
                case VK_UP:
                    if (indexSel < 0)
                    {
                        if (listBox1.Items.Count > 0)
                            listBox1.SelectedIndex = listBox1.Items.Count - 1; // last;
                    }
                    else if (indexSel == 0)
                    {
                        // first. no change.
                    }
                    else
                    {
                        --indexSel;
                        listBox1.SelectedIndex = indexSel;
                    }
                    return true;

                case VK_DOWN:
                    if (indexSel < 0)
                    {
                        if (listBox1.Items.Count > 0)
                            listBox1.SelectedIndex = 0; // first;
                    }
                    else if (indexSel >= listBox1.Items.Count  - 1)
                    {
                        // last. no change.
                    }
                    else
                    {
                        ++indexSel;
                        listBox1.SelectedIndex = indexSel;
                    }
                    return true;
            }
            return false;
        }

        private void listBox1_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (null == FormMain.Instance.Tabs.SelectedTab)
                return;
            var grid = FormMain.Instance.Tabs.SelectedTab.Controls[0] as DataGridView;
            var seltext = ListBox.SelectedItem as string;
            if (null != seltext && null != grid.EditingControl)
            {
                grid.EditingControl.Text = seltext;
            }

        }
    }
}
