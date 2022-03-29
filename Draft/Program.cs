using System;
using System.Threading;

namespace Draft
{
    public class Program
    {
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
        public static void Main(string[] args)
        {
            var p = new Program();
            _ = Program.CallAsync(async () => { await Task.Delay(1); throw new Exception("xxx"); } );
            Thread.Sleep(1000);
            Console.WriteLine("Ok!");
        }
    }
}
