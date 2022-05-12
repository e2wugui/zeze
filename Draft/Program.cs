using System;
using System.Threading;

namespace Draft
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var timer = new Timer(TimerCallback, null, 1000, 1000);
            Thread.Sleep(5000);
            timer.Dispose();
        }

        static int i;

        public static async void TimerCallback(object? state)
        {
            var ii = i++;
            Console.WriteLine(DateTime.Now + " before deay ii=" + ii);
            await Task.Delay(1000);
            Console.WriteLine(DateTime.Now + $" after delay ii={ii} i={i}");
        }
    }
}
