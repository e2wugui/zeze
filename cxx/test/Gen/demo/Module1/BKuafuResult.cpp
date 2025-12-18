
#include "Gen/demo/Module1/BKuafuResult.hpp"

namespace demo {
namespace Module1 {

BKuafuResult::BKuafuResult()
{
    Money = 0;
}

BKuafuResult::BKuafuResult(int64_t Money_)
{
    Money = Money_;
}

void BKuafuResult::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BKuafuResult&>(other));
}

void BKuafuResult::Assign(const BKuafuResult& other) {
    Money = other.Money;
}

BKuafuResult& BKuafuResult::operator=(const BKuafuResult& other) {
    Assign(other);
    return *this;
}

void BKuafuResult::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Money;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BKuafuResult::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
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
