using System;
using System.Threading;

namespace Draft
{
    public class Program
    {
        Nito.AsyncEx.AsyncMonitor Monitor = new();

        public async Task Enter(int i)
        {
            using (await Monitor.EnterAsync())
            {
                Console.WriteLine($"Enter {i}");
                if (i == 0)
                {
                    ExecutionContext.SuppressFlow();
                    _ = Task.Run(async () =>
                    {
                        await Enter(i + 1);
                    });
                    ExecutionContext.RestoreFlow();
                }
                await Task.Delay(1000);
                Console.WriteLine($"Exit {i}");
            }
        }
        public static void Main(string[] args)
        {
            var p = new Program();
            p.Enter(0).Wait();
            Thread.Sleep(2000);
        }
    }
}
