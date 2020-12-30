using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public class EditorConfig
    {
        public IList<string> RecentHomes { get; set; }

        public Size FormMainSize { get; set; } = new Size(828, 569);
        public Point FormMainLocation { get; set; } = new Point(150, 80);
        public FormWindowState FormMainState { get; set; } = FormWindowState.Maximized;

        public Size FormErrorSize { get; set; } = new Size(925, 209);
        public Point FormErrorLocation { get; set; } = new Point(100, 649);
        public FormWindowState FormErrorState { get; set; } = FormWindowState.Normal;

        public string GetHome()
        {
            return RecentHomes[0];
        }

        public void SetRecentHome(string home)
        {
            if (!System.IO.Directory.Exists(home))
                home = Environment.CurrentDirectory;

            RecentHomes.Insert(0, home);
            IList<string> distinct = new List<string>();
            foreach (var r in RecentHomes.Distinct())
                distinct.Add(r);
            RecentHomes = distinct;
            while (RecentHomes.Count > 10)
                RecentHomes.RemoveAt(RecentHomes.Count - 1);
        }
    }
}
