namespace Daemon;

public static class Program
{
    public static void Main(string[] args)
    {
        var port = Environment.GetEnvironmentVariable(Zeze.Services.Daemon.PropertyNamePort);
        if (port == null)
            Zeze.Services.Daemon.Main(args);
        else // only for test
        {
            Console.WriteLine($"envs[{Zeze.Services.Daemon.PropertyNamePort}] = {port}");
            for (var i = 0; i < args.Length; i++)
                Console.WriteLine($"args[{i}] = {args[i]}");
        }
    }
}
