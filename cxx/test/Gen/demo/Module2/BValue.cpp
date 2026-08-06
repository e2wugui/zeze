
#include "Gen/demo/Module2/BValue.hpp"

namespace demo {
namespace Module2 {

BValue::BValue()
{
    S = 1;
}

BValue::BValue(int S_)
{
    S = S_;
}

void BValue::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BValue&>(other));
}

void BValue::Assign(const BValue& other) {
    S = other.S;
    Plist = other.Plist;
    Psortedmap = other.Psortedmap;
}

BValue& BValue::operator=(const BValue& other) {
    Assign(other);
    return *this;
}

void BValue::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = S;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    {
        const auto& _x_ = Plist;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::runtime_error("concurrent modify.");
        }
    }
    {
        const auto& _x_ = Psortedmap;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 3, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt(it->first);
                _o_.WriteInt(it->second);
                _n_--;
            }
            if (_n_ != 0)
                throw std::runtime_error("concurrent modify.");
        }
    }
    _o_.WriteByte(0);
}

void BValue::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        S = _o_.ReadInt(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    } else
        S = 0;
    if (_i_ == 2) {
        auto& _x_ = Plist;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int _e_;
                _e_ = _o_.ReadInt();
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 3) {
        auto& _x_ = Psortedmap;
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
}
