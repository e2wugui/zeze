
#include "Gen/demo/Module1/BOnlyData.hpp"

namespace demo {
namespace Module1 {

BOnlyData::BOnlyData()
{
    S = 1;
}

BOnlyData::BOnlyData(int S_)
{
    S = S_;
}

void BOnlyData::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BOnlyData&>(other));
}

void BOnlyData::Assign(const BOnlyData& other) {
    S = other.S;
}

BOnlyData& BOnlyData::operator=(const BOnlyData& other) {
    Assign(other);
    return *this;
}

void BOnlyData::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = S;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BOnlyData::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        S = _o_.ReadInt(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    } else
        S = 0;
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
