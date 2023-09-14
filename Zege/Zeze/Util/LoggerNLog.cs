using System;
using NLog;

namespace Zeze.Util
{
    public class NLogLogFactory : ILogFactory
    {
        public ILogger GetLogger()
        {
            return new NLogLogger();
        }

        public ILogger GetLogger(Type type)
        {
            return new NLogLogger(type.Name);
        }

        public ILogger GetLogger(string type)
        {
            return new NLogLogger(type);
        }

        public void Shutdown()
        {
            NLog.LogManager.Shutdown();
        }
    }

    public class NLogLogger : ILogger
    {
        private readonly Logger logger;

        public NLogLogger(string type = null)
        {
            logger = type == null
                ? NLog.LogManager.GetCurrentClassLogger()
                : NLog.LogManager.GetLogger(type);
        }

        public void Trace(string fmt, params object[] args)
        {
            logger.Trace(fmt, args);
        }

        public void Debug(string fmt, params object[] args)
        {
            logger.Debug(fmt, args);
        }

        public void Info(string fmt, params object[] args)
        {
            logger.Info(fmt, args);
        }

        public void Warn(string fmt, params object[] args)
        {
            logger.Warn(fmt, args);
        }

        public void Error(string fmt, params object[] args)
        {
            logger.Error(fmt, args);
        }

        public void Fatal(string fmt, params object[] args)
        {
            logger.Fatal(fmt, args);
        }

        public void Trace(Exception ex, string fmt, params object[] args)
        {
            logger.Trace(ex, fmt, args);
        }

        public void Debug(Exception ex, string fmt, params object[] args)
        {
            logger.Debug(ex, fmt, args);
        }

        public void Info(Exception ex, string fmt, params object[] args)
        {
            logger.Info(ex, fmt, args);
        }

        public void Warn(Exception ex, string fmt, params object[] args)
        {
            logger.Warn(ex, fmt, args);
        }

        public void Error(Exception ex, string fmt, params object[] args)
        {
            logger.Error(ex, fmt, args);
        }

        public void Fatal(Exception ex, string fmt, params object[] args)
        {
            logger.Fatal(ex, fmt, args);
        }

        public void Trace(Exception ex)
        {
            logger.Trace(ex, "");
        }

        public void Debug(Exception ex)
        {
            logger.Debug(ex, "");
        }

        public void Info(Exception ex)
        {
            logger.Info(ex, "");
        }

        public void Warn(Exception ex)
        {
            logger.Warn(ex, "");
        }

        public void Error(Exception ex)
        {
            logger.Error(ex, "");
        }

        public void Fatal(Exception ex)
        {
            logger.Fatal(ex, "");
        }

        public void Log(LogLevel logLevel, string fmt, params object[] args)
        {
            logger.Log(toNLogLevel(logLevel), fmt, args);
        }

        public void Log(LogLevel logLevel, Exception ex, string fmt, params object[] args)
        {
            logger.Log(toNLogLevel(logLevel), ex, fmt, args);
        }

        private static NLog.LogLevel toNLogLevel(LogLevel logLevel)
        {
            switch (logLevel)
            {
                case LogLevel.Trace:
                    return NLog.LogLevel.Trace;
                case LogLevel.Debug:
                    return NLog.LogLevel.Debug;
                case LogLevel.Info:
                    return NLog.LogLevel.Info;
                case LogLevel.Warn:
                    return NLog.LogLevel.Warn;
                case LogLevel.Error:
                    return NLog.LogLevel.Error;
                case LogLevel.Fatal:
                    return NLog.LogLevel.Fatal;
                case LogLevel.Off:
                    return NLog.LogLevel.Off;
                default:
                    throw new ArgumentException($"unknown logLevel={logLevel}");
            }
        }
    }
}
