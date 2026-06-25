using System.Threading;
using zezeboot;

internal class Program
{
    static void Main(string[] args)
    {
        LinkClient.Instance.Connect("127.0.0.1", 11000);
        Log.Info("LinkClient started");

        Thread.Sleep(999999999);
    }
}
