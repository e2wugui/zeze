using System;
using System.Threading;

namespace Draft
{
    public class Program
    {
        AsyncLocal<int> AsyncLocal = new AsyncLocal<int>();

        public async Task<int> Start(int i)
        {
            Console.WriteLine("Start." + i + " " + AsyncLocal.Value);
            AsyncLocal.Value = 3 - i;
            await Nest(i, 0);
            await Task.Delay(1100 - i * 1000);
            await Nest(i, 1);
            return i;
        }

        public async Task Nest(int i, int j)
        {
            Console.WriteLine("Nest." + i + "." + j + " " + AsyncLocal.Value);
        }

        public static void Main(string[] args)
        {
            var p = new Program();
            var tasks = new Task[2];
            for (int i = 0; i < tasks.Length; i++)
            {
                tasks[i] = p.Start(i);
            }
            Task.WaitAll(tasks);
        }
    }
}
