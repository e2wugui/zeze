namespace Zeze.Util
{
    public class ResultCode
    {
        // >0 用户自定义。
        public const long Success = 0;
        public const long Exception = -1;
        public const long TooManyTry = -2;
        public const long NotImplement = -3;
        public const long Unknown = -4;
        public const long ErrorSavepoint = -5;
        public const long LogicError = -6;
        public const long RedoAndRelease = -7;
        public const long AbortException = -8;
        public const long ProviderNotExist = -9;
        public const long Timeout = -10;
        public const long CancelException = -11;
        public const long DuplicateRequest = -12;
        public const long ErrorRequestId = -13;
        public const long ErrorSendFail = -14;
        public const long RaftRetry = -15;
        public const long RaftApplied = -16;
        public const long RaftExpired = -17;
        public const long Closed = -18;
        public const long Busy = -19;
    }
}
