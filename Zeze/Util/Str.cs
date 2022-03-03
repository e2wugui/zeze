
namespace Zeze.Util
{
    public static class Str
    {
        public const int INDENT_MAX = 128;
        static readonly string[] INDENTS = new string[INDENT_MAX];

        static Str()
        {
            for (int i = 0; i < INDENT_MAX; i++)
                INDENTS[i] = new string(' ', i);
        }

        public static string Indent(int n)
        {
            if (n <= 0)
                return "";
            if (n >= INDENT_MAX)
                n = INDENT_MAX - 1;
            return INDENTS[n];
        }
    }
}
