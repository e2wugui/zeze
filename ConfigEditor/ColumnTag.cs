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
            public int ListIndex { get; set; } = -1; // 负数在ListEnd的时候表示(-List.Count)。

        }

        public enum ETag
        {
            Normal = 0,
            AddVariable = 1,
            ListStart = 2,
            ListEnd = 3,
        }
        public ETag Tag { get; }

        public List<VarInfo> Path { get; } = new List<VarInfo>();

        public ColumnTag(ETag tag)
        {
            this.Tag = tag;
        }

        public ColumnTag(ETag tag, VarDefine define, int index)
        {
            this.Tag = tag;

            AddVar(define, index);
        }

        public VarInfo PathLast { get { return Path[Path.Count - 1]; } }

        public ColumnTag AddVar(VarDefine define, int index)
        {
            Path.Add(new VarInfo() { Define = define, ListIndex = index });
            return this;
        }

        public ColumnTag Copy(ETag tag)
        {
            ColumnTag copy = new ColumnTag(tag);
            copy.Path.AddRange(Path);
            return copy;
        }

        public ColumnTag Parent(ETag tag)
        {
            ColumnTag copy = new ColumnTag(tag);
            int copyCount = Path.Count - 1;
            for (int i = 0; i < copyCount; ++i)
                copy.Path.Add(Path[i]);
            return copy;
        }
    }
}
