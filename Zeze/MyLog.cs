using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze
{
    public class MyLog
    {
        public static MyLog Instance = new MyLog();

        public static MyLog GetLogger(Type type)
        {
            return Instance;
        }

        public void Trace(params object[] msg)
        {

        }

        public void Debug(params object[] msg)
        {

        }

        public void Info(params object[] msg)
        {

        }

        public void Warn(params object[] msg)
        {

        }

        public void Error(params object[] msg)
        {

        }

        public void Fatal(params object[] msg)
        {

        }

        public void Trace(Exception ex, params object[] msg)
        {

        }

        public void Debug(Exception ex, params object[] msg)
        {

        }

        public void Info(Exception ex, params object[] msg)
        {

        }

        public void Warn(Exception ex, params object[] msg)
        {

        }

        public void Error(Exception ex, params object[] msg)
        {

        }

        public void Fatal(Exception ex, params object[] msg)
        {

        }

        public void Log(Config.LogLevel logLevel, params object[] msg)
        {

        }

        public void Log(Config.LogLevel logLevel, Exception ex, params object[] msg)
        {

        }
    }
}
