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
    public partial class FormProperties : Form
    {
        public FormProperties()
        {
            InitializeComponent();
        }

        public FormDefine FormDefine { get; set; }
        public List<Property.IProperty> Properties { get; set; }

        private void FormProperties_Load(object sender, EventArgs e)
        {
            var group = FormMain.Instance.PropertyManager.SortedGroup();

            this.SuspendLayout();

            if (group.TryGetValue(Property.Group.Normal, out var normal))
                AddButtonTo(this.groupBoxNormal, normal);

            GroupBox prev = this.groupBoxNormal;
            foreach (var g in group)
            {
                if (g.Key == Property.Group.Normal)
                    continue; // normal 专门处理。

                GroupBox gb = new GroupBox();
                gb.Location = new Point(prev.Location.X, prev.Location.Y + prev.Size.Height + 5);
                gb.Name = System.Enum.GetName(typeof(Property.Group), g.Key);
                gb.Text = gb.Name;
                gb.TabStop = false;
                this.Controls.Add(gb);

                AddButtonTo(gb, g.Value);
                prev = gb;
            }

            foreach (var p in Properties)
            {
                p.ButtonChecked = true;
            }

            this.ResumeLayout();
        }

        private void AddButtonTo(GroupBox group, List<Property.IProperty> ps)
        {
            Point location = new Point(6, 20);
            int lines = 1;

            for (int i = 0; i < ps.Count; ++i)
            {
                Property.IProperty p = ps[i];

                ButtonBase check = null;
                if (p.GroupRadio)
                    check = new RadioButton();
                else
                    check = new CheckBox();

                check.Location = location;
                check.Name = "button" + p.Name;
                check.Size = new Size(90, 24);
                check.TabIndex = i;
                check.Text = p.Name;
                toolTip1.SetToolTip(check, p.Comment);
                check.UseVisualStyleBackColor = true;

                group.Controls.Add(check);
                p.Button = check;

                location.X += 94;
                if ((i + 1) % 5 == 0) // new line
                {
                    location.X = 6;
                    location.Y += 29;
                    if (i < ps.Count - 1) // 刚好满一行就不增加lines了。
                        ++lines;
                }
            }

            group.Size = new Size(534, 28 * lines + 20);
        }
    }
}
