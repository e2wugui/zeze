
#include "Gen/demo/Module1/BRemoved2.hpp"

namespace demo {
namespace Module1 {

BRemoved2::BRemoved2()
{
    Int_1 = 0;
}

BRemoved2::BRemoved2(int Int_1_)
{
    Int_1 = Int_1_;
}

void BRemoved2::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BRemoved2&>(other));
}

void BRemoved2::Assign(const BRemoved2& other) {
    Int_1 = other.Int_1;
}

BRemoved2& BRemoved2::operator=(const BRemoved2& other) {
    Assign(other);
    return *this;
}

void BRemoved2::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Int_1;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BRemoved2::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Int_1 = _o_.ReadInt(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
