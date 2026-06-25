namespace zezeboot
{
    public static class Log
    {
        public static void Info(string msg)
        {
            System.Console.WriteLine($"INFO : {msg}");
        }

        public static void Error(string msg)
        {
            System.Console.WriteLine($"ERROR: {msg}");
        }
    }
}
