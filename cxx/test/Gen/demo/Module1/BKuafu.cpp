
#include "Gen/demo/Module1/BKuafu.hpp"

namespace demo {
namespace Module1 {

BKuafu::BKuafu()
{
    Account = 0;
    Money = 0;
}

BKuafu::BKuafu(int64_t Account_, int64_t Money_)
{
    Account = Account_;
    Money = Money_;
}

void BKuafu::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BKuafu&>(other));
}

void BKuafu::Assign(const BKuafu& other) {
    Account = other.Account;
    Money = other.Money;
}

BKuafu& BKuafu::operator=(const BKuafu& other) {
    Assign(other);
    return *this;
}

void BKuafu::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Account;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    {
        auto _x_ = Money;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BKuafu::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Account = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 2) {
        Money = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
