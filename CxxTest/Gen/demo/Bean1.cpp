
#include "Gen/demo/Bean1.hpp"

namespace demo {

Bean1::Bean1()
{
    V1 = 1;
}

Bean1::Bean1(int V1_)
{
    V1 = V1_;
}

void Bean1::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const Bean1&>(other));
}

void Bean1::Assign(const Bean1& other) {
    V1 = other.V1;
    V2 = other.V2;
}

Bean1& Bean1::operator=(const Bean1& other) {
    Assign(other);
    return *this;
}

void Bean1::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = V1;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    {
        const auto& _x_ = V2;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt(it->first);
                _o_.WriteInt(it->second);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    _o_.WriteByte(0);
}

void Bean1::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        V1 = _o_.ReadInt(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    } else
        V1 = 0;
    if (_i_ == 2) {
        auto& _x_ = V2;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                int _k_;
                _k_ = _o_.ReadInt();
                int _v_;
                _v_ = _o_.ReadInt();
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
