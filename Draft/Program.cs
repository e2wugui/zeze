using System;
using System.Threading;

namespace Draft
{
    public class Program
    {
        static AsyncLocal<string> alocal = new();

        public static async Task<long> CallAsync(Func<Task<long>> func)
        {
            try
            {
                return await func();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.ToString());
                return -1;
            }
        }

        public async Task Test()
        {
            await CallAsync(async () =>
            {
                Console.WriteLine(DateTime.Now + " 1111 value=" + alocal.Value);
                alocal.Value = "1111";
                await Task.Delay(1);
                Console.WriteLine(DateTime.Now + " 1111 value=" + alocal.Value);
                ExecutionContext.SuppressFlow();
                _ = CallAsync(async () =>
                {
                    Console.WriteLine(DateTime.Now + " 1111_______ value=" + alocal.Value);
                    await Task.Delay(0);
                    return 0;
                });
                ExecutionContext.RestoreFlow();
                _ = Task.Run(async () => { Console.WriteLine("Task.Run async " + alocal.Value); await Task.Delay(0); });
                _ = Task.Run(() => { Console.WriteLine("Task.Run " + alocal.Value); });
                new Task(() => { Console.WriteLine("new Task.Run " + alocal.Value); }, TaskCreationOptions.None).Start();
                new Task(async () => { Console.WriteLine("new Task.Run async " + alocal.Value); await Task.Delay(0); }).Start();
                return 0;
            });
            await CallAsync(async () =>
            {
                await Task.Delay(5);
                Console.WriteLine(DateTime.Now + " 2222 value=" + alocal.Value);
                alocal.Value = "2222";
                await Task.Delay(10);
                Console.WriteLine(DateTime.Now + " 2222 value=" + alocal.Value);
                return 0;
            });
            await CallAsync(async () =>
            {
                Console.WriteLine(DateTime.Now + " 3333 value=" + alocal.Value);
                alocal.Value = "3333";
                Console.WriteLine(DateTime.Now + " 3333 value=" + alocal.Value);
                await Task.Delay(0);
                return 0;
            });
        }

        public static void Main(string[] args)
        {
            new Program().Test().Wait();
            Thread.Sleep(5000);
            Console.WriteLine("Ok!");
        }
    }
}
