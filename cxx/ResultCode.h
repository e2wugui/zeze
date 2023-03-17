#pragma once

#include <cstdint>

namespace Zeze
{
	class ResultCode
	{
	public:
        static const int64_t Success = 0;
        static const int64_t Exception = -1;
        static const int64_t TooManyTry = -2;
        static const int64_t NotImplement = -3;
        static const int64_t Unknown = -4;
        static const int64_t ErrorSavepoint = -5;
        static const int64_t LogicError = -6;
        static const int64_t RedoAndRelease = -7;
        static const int64_t AbortException = -8;
        static const int64_t ProviderNotExist = -9;
        static const int64_t Timeout = -10;
        static const int64_t CancelException = -11;
        static const int64_t DuplicateRequest = -12;
        static const int64_t ErrorRequestId = -13;
        static const int64_t ErrorSendFail = -14;
        static const int64_t RaftRetry = -15;
        static const int64_t RaftApplied = -16;
        static const int64_t RaftExpired = -17;
        static const int64_t Closed = -18;
        static const int64_t Busy = -19;
        // >0 用户自定义。
    };
}
