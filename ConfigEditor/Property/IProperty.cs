using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public enum Result
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

        public virtual Group Group => Group.Normal; 

        public virtual bool IsGroupRadio
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

        public abstract Result VerifyCell(DataGridView grid, int columnIndex, int rowIndex);
    }
}
