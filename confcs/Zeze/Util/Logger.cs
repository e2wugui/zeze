using System;
using System.Threading;

namespace Zeze.Util
{
    public interface ILogger
    {
        void Trace(string fmt, params object[] args);
        void Debug(string fmt, params object[] args);
        void Info(string fmt, params object[] args);
        void Warn(string fmt, params object[] args);
        void Error(string fmt, params object[] args);
        void Fatal(string fmt, params object[] args);
        void Trace(Exception ex, string fmt, params object[] args);
        void Debug(Exception ex, string fmt, params object[] args);
        void Info(Exception ex, string fmt, params object[] args);
        void Warn(Exception ex, string fmt, params object[] args);
        void Error(Exception ex, string fmt, params object[] args);
        void Fatal(Exception ex, string fmt, params object[] args);
        void Trace(Exception ex);
        void Debug(Exception ex);
        void Info(Exception ex);
        void Warn(Exception ex);
        void Error(Exception ex);
        void Fatal(Exception ex);
        void Log(Config.LogLevel logLevel, string fmt, params object[] args);
        void Log(Config.LogLevel logLevel, Exception ex, string fmt, params object[] args);
    }

    public interface ILogFactory
    {
        ILogger GetLogger(Type type = null);
    }

    public static class LogManager
    {
        private static ILogFactory factory = new ConsoleLogFactory();

        public static ILogFactory Factory
        {
            get => factory;
            set => factory = value ?? throw new ArgumentNullException();
        }
    }

    public class ConsoleLogFactory : ILogFactory
    {
        public ILogger GetLogger(Type type = null)
        {
            return new ConsoleLogger(type);
        }
    }

    public class ConsoleLogger : ILogger
    {
        private readonly Type type;

        public ConsoleLogger(Type type = null)
        {
            this.type = type;
        }

        public void Trace(string fmt, params object[] args)
        {
            Log(Config.LogLevel.Trace, fmt, args);
        }

        public void Debug(string fmt, params object[] args)
        {
            Log(Config.LogLevel.Debug, fmt, args);
        }

        public void Info(string fmt, params object[] args)
        {
            Log(Config.LogLevel.Info, fmt, args);
        }

        public void Warn(string fmt, params object[] args)
        {
            Log(Config.LogLevel.Warn, fmt, args);
        }

        public void Error(string fmt, params object[] args)
        {
            Log(Config.LogLevel.Error, fmt, args);
        }

        public void Fatal(string fmt, params object[] args)
        {
            Log(Config.LogLevel.Fatal, fmt, args);
        }

        public void Trace(Exception ex, string fmt, params object[] args)
        {
            Log(Config.LogLevel.Trace, ex, fmt, args);
        }

        public void Debug(Exception ex, string fmt, params object[] args)
        {
            Log(Config.LogLevel.Debug, ex, fmt, args);
        }

        public void Info(Exception ex, string fmt, params object[] args)
        {
            Log(Config.LogLevel.Info, ex, fmt, args);
        }

        public void Warn(Exception ex, string fmt, params object[] args)
        {
            Log(Config.LogLevel.Warn, ex, fmt, args);
        }

        public void Error(Exception ex, string fmt, params object[] args)
        {
            Log(Config.LogLevel.Error, ex, fmt, args);
        }

        public void Fatal(Exception ex, string fmt, params object[] args)
        {
            Log(Config.LogLevel.Fatal, ex, fmt, args);
        }

        public void Trace(Exception ex)
        {
            Log(Config.LogLevel.Trace, ex, null, null);
        }

        public void Debug(Exception ex)
        {
            Log(Config.LogLevel.Debug, ex, null, null);
        }

        public void Info(Exception ex)
        {
            Log(Config.LogLevel.Info, ex, null, null);
        }

        public void Warn(Exception ex)
        {
            Log(Config.LogLevel.Warn, ex, null, null);
        }

        public void Error(Exception ex)
        {
            Log(Config.LogLevel.Error, ex, null, null);
        }

        public void Fatal(Exception ex)
        {
            Log(Config.LogLevel.Fatal, ex, null, null);
        }

        public void Log(Config.LogLevel logLevel, string fmt, params object[] args)
        {
            Log(logLevel, null, fmt, args);
        }

        public void Log(Config.LogLevel logLevel, Exception ex, string fmt, params object[] args)
        {
            var msg = args == null || args.Length == 0 ? fmt ?? "" : string.Format(fmt, args);
            Console.WriteLine(type != null
                ? $"{logLevel} [{Thread.CurrentThread.Name}] {type.Name}: {msg}"
                : $"{logLevel} [{Thread.CurrentThread.Name}] {msg}");
            if (ex != null)
                Console.WriteLine(ex);
        }
    }
}
