using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new FormMain());
        }

        public static Encoding EncodingUtf8NoBom = new UTF8Encoding(false);
        public static StreamWriter OpenStreamWriter(string file, bool append = false)
        {
            return new StreamWriter(file, append, EncodingUtf8NoBom) { NewLine = "\n" };
        }
    }
}
