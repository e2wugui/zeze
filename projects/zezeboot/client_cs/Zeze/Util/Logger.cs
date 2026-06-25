using System;
using System.Threading;

namespace Zeze.Util
{
    public enum LogLevel
    {
        Trace,
        Debug,
        Info,
        Warn,
        Error,
        Fatal,
        Off
    }

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
        void Log(LogLevel logLevel, string fmt, params object[] args);
        void Log(LogLevel logLevel, Exception ex, string fmt, params object[] args);
    }

    public interface ILogFactory
    {
        ILogger GetLogger();
        ILogger GetLogger(Type type);
        ILogger GetLogger(string type);
        void Shutdown();
    }

    public static class LogManager
    {
        private static ILogFactory factory;

        public static ILogFactory Factory
        {
            get => factory;
            set => factory = value ?? throw new ArgumentNullException();
        }

        static LogManager()
        {
#if HAS_NLOG
            factory = new NLogLogFactory();
#else
            factory = new ConsoleLogFactory();
#endif
        }

        public static ILogger GetLogger()
        {
            return factory.GetLogger();
        }

        public static ILogger GetLogger(Type type)
        {
            return factory.GetLogger(type);
        }

        public static ILogger GetLogger(string type)
        {
            return factory.GetLogger(type);
        }

        public static void Shutdown()
        {
            factory.Shutdown();
        }
    }

    public class ConsoleLogFactory : ILogFactory
    {
        public ILogger GetLogger()
        {
            return new ConsoleLogger();
        }

        public ILogger GetLogger(Type type)
        {
            return new ConsoleLogger(type?.Name);
        }

        public ILogger GetLogger(string type)
        {
            return new ConsoleLogger(type);
        }

        public void Shutdown()
        {
        }
    }

    public class ConsoleLogger : ILogger
    {
        private readonly string type;

        public ConsoleLogger(string type = null)
        {
            this.type = string.IsNullOrEmpty(type) ? null : type;
        }

        public void Trace(string fmt, params object[] args)
        {
            Log(LogLevel.Trace, fmt, args);
        }

        public void Debug(string fmt, params object[] args)
        {
            Log(LogLevel.Debug, fmt, args);
        }

        public void Info(string fmt, params object[] args)
        {
            Log(LogLevel.Info, fmt, args);
        }

        public void Warn(string fmt, params object[] args)
        {
            Log(LogLevel.Warn, fmt, args);
        }

        public void Error(string fmt, params object[] args)
        {
            Log(LogLevel.Error, fmt, args);
        }

        public void Fatal(string fmt, params object[] args)
        {
            Log(LogLevel.Fatal, fmt, args);
        }

        public void Trace(Exception ex, string fmt, params object[] args)
        {
            Log(LogLevel.Trace, ex, fmt, args);
        }

        public void Debug(Exception ex, string fmt, params object[] args)
        {
            Log(LogLevel.Debug, ex, fmt, args);
        }

        public void Info(Exception ex, string fmt, params object[] args)
        {
            Log(LogLevel.Info, ex, fmt, args);
        }

        public void Warn(Exception ex, string fmt, params object[] args)
        {
            Log(LogLevel.Warn, ex, fmt, args);
        }

        public void Error(Exception ex, string fmt, params object[] args)
        {
            Log(LogLevel.Error, ex, fmt, args);
        }

        public void Fatal(Exception ex, string fmt, params object[] args)
        {
            Log(LogLevel.Fatal, ex, fmt, args);
        }

        public void Trace(Exception ex)
        {
            Log(LogLevel.Trace, ex, null, null);
        }

        public void Debug(Exception ex)
        {
            Log(LogLevel.Debug, ex, null, null);
        }

        public void Info(Exception ex)
        {
            Log(LogLevel.Info, ex, null, null);
        }

        public void Warn(Exception ex)
        {
            Log(LogLevel.Warn, ex, null, null);
        }

        public void Error(Exception ex)
        {
            Log(LogLevel.Error, ex, null, null);
        }

        public void Fatal(Exception ex)
        {
            Log(LogLevel.Fatal, ex, null, null);
        }

        public void Log(LogLevel logLevel, string fmt, params object[] args)
        {
            Log(logLevel, null, fmt, args);
        }

        public void Log(LogLevel logLevel, Exception ex, string fmt, params object[] args)
        {
            var msg = args == null || args.Length == 0 ? fmt ?? "" : string.Format(fmt, args);
            Console.WriteLine(type != null
                ? $"{logLevel} [{Thread.CurrentThread.Name}] {type}: {msg}"
                : $"{logLevel} [{Thread.CurrentThread.Name}] {msg}");
            if (ex != null)
                Console.WriteLine(ex);
        }
    }
}
