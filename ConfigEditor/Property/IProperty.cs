using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public enum ErrorLevel
    {
        Ok,
        Warn,
        Error,
    }

    // 属性分组。在界面中分组显示。
    public enum Group
    {
        Normal, // 默认分组。
        DataType, // 数据类型，这个类型里面的属性是单选的。
        GenTarget, // 标明生成输出。
        // more group
    }

    public abstract class IProperty
    {
        public abstract string Name { get; }
        public abstract string Comment { get; }
        public virtual Group Group => Group.Normal; 

        public ButtonBase Button { get; set; } // FormProperties使用，仅在编辑的时候才有效。

        public virtual bool BuildIn { get; } = false;

        public bool ButtonChecked
        {
            get
            {
                if (Button is CheckBox cb) return cb.Checked;
                if (Button is RadioButton rb) return rb.Checked;
                return false;
            }

            set
            {
                if (Button is CheckBox cb) cb.Checked = value;
                else if (Button is RadioButton rb) rb.Checked = value;
            }
        }

        public virtual bool GroupRadio
        {
            get
            {
                switch (Group)
                {
                    case Group.DataType:
                        return true;
                }
                return false;
            }
        }

        public abstract void VerifyCell(VerifyParam param);
    }
}
