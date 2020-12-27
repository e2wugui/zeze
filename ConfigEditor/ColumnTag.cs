using System;
using System.Collections.Generic;
using System.Drawing;
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
            public VarInfo Copy()
            {
                return new VarInfo() { Define = this.Define, ListIndex = this.ListIndex };
            }
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

        // 存储是否唯一
        public Dictionary<string, HashSet<DataGridViewCell>> UniqueIndex { get; }
            = new Dictionary<string, HashSet<DataGridViewCell>>();

        public void UpdateUniqueIndex(string oldValue, DataGridViewCell cell)
        {
            if (null == oldValue)
                oldValue = "";

            string newValue = cell.Value as string;
            if (null == newValue)
                newValue = "";

            if (oldValue.Equals(newValue))
                return;

            if (UniqueIndex.TryGetValue(oldValue, out var cells))
            {
                cells.Remove(cell);

                switch (cells.Count)
                {
                    case 1:
                        PathLast.Define.Parent.Document.Main.FormError.ReportVerifyResult(null, cells);
                        break;

                    case 0:
                        UniqueIndex.Remove(oldValue);
                        break;
                }
            }
            AddUniqueIndex(cell);
        }

        public void AddUniqueIndex(DataGridViewCell cell)
        {
            string value = cell.Value as string;
            if (null == value)
                value = "";
            if (false == UniqueIndex.TryGetValue(value, out var cells))
                UniqueIndex.Add(value, cells = new HashSet<DataGridViewCell>());
            cells.Add(cell);
        }

        public void BuildUniqueIndex(DataGridView grid, int columnIndex)
        {
            for (int i = 0; i < grid.RowCount; ++i)
            {
                DataGridViewCell cell = grid.Rows[i].Cells[columnIndex];
                AddUniqueIndex(cell);
            }
        }

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
            int copyCount = Path.Count;
            for (int i = 0; i < copyCount; ++i)
                copy.Path.Add(Path[i].Copy());
            return copy;
        }

        public ColumnTag Parent(ETag tag)
        {
            ColumnTag copy = new ColumnTag(tag);
            int copyCount = Path.Count - 1;
            for (int i = 0; i < copyCount; ++i)
                copy.Path.Add(Path[i].Copy());
            return copy;
        }
    }
}
