using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public class ColumnTag
    {
        public class VarInfo
        {
            public VarDefine Define { get; set; }
            public int Index { get; set; } = -1; // TODO 不同的值有不同的含义。

        }
        public BeanDefine BeanDefine { get; }
        public enum ETag
        {
            Normal = 0,
            AddVariable = 1,
            ListStart = 2,
            ListEnd = 3,
        }
        public ETag Tag { get; }

        public List<VarInfo> Path { get; } = new List<VarInfo>();

        public ColumnTag(BeanDefine define, ETag tag)
        {
            this.BeanDefine = define;
            this.Tag = tag;
        }

        public ColumnTag(BeanDefine beanDefine, ETag tag, VarDefine define, int index)
        {
            this.BeanDefine = beanDefine;
            this.Tag = tag;

            AddVar(define, index);
        }

        public ColumnTag AddVar(VarDefine define, int index)
        {
            Path.Add(new VarInfo() { Define = define, Index = index });
            return this;
        }

        public ColumnTag Copy(ETag tag)
        {
            ColumnTag copy = new ColumnTag(this.BeanDefine, tag);
            copy.Path.AddRange(Path);
            return copy;
        }
    }
}
