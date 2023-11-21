package Zeze.Arch.Beans;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public final class BSendResult extends Zeze.Transaction.Bean {
	public static final long TYPEID = -7186434891670297524L;

	private final Zeze.Util.LongList _ErrorLinkSids = new Zeze.Util.LongList();

	public Zeze.Util.LongList getErrorLinkSids() {
		return _ErrorLinkSids;
	}

	public BSendResult() {
	}

	public void assign(BSendResult other) {
		_ErrorLinkSids.clear();
		_ErrorLinkSids.addAll(other._ErrorLinkSids);
	}

	@Override
	public BSendResult copy() {
		var copy = new BSendResult();
		copy.assign(this);
		return copy;
	}

	@Override
	public long typeId() {
		return TYPEID;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		buildString(sb, 0);
		return sb.append(System.lineSeparator()).toString();
	}

	@Override
	public void buildString(StringBuilder sb, int level) {
		sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSendResult: {").append(System.lineSeparator());
		level += 4;
		sb.append(Zeze.Util.Str.indent(level)).append("ErrorLinkSids=[");
		if (!_ErrorLinkSids.isEmpty()) {
			sb.append(System.lineSeparator());
			level += 4;
			for (int i = 0, n = _ErrorLinkSids.size(); i < n; i++) {
				sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_ErrorLinkSids.get(i)).append(',').append(System.lineSeparator());
			}
			level -= 4;
			sb.append(Zeze.Util.Str.indent(level));
		}
		sb.append(']').append(System.lineSeparator());
		level -= 4;
		sb.append(Zeze.Util.Str.indent(level)).append('}');
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void encode(ByteBuffer _o_) {
		int _i_ = 0;
		{
			var _x_ = _ErrorLinkSids;
			int _n_ = _x_.size();
			if (_n_ != 0) {
				_i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
				_o_.WriteListType(_n_, ByteBuffer.INTEGER);
				for (int _j_ = 0; _j_ < _n_; _j_++)
					_o_.WriteLong(_x_.get(_j_));
			}
		}
		_o_.WriteByte(0);
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void decode(IByteBuffer _o_) {
		int _t_ = _o_.ReadByte();
		int _i_ = _o_.ReadTagSize(_t_);
		if (_i_ == 1) {
			var _x_ = _ErrorLinkSids;
			_x_.clear();
			if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
				int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte());
				if (_x_.capacity() < _n_) {
					_x_.wraps(new long[Math.min(_n_, 0x10000)]);
					_x_.clear();
				}
				for (; _n_ > 0; _n_--)
					_x_.add(_o_.ReadLong(_t_));
			} else
				_o_.SkipUnknownFieldOrThrow(_t_, "Collection");
			_i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
		while (_t_ != 0) {
			_o_.SkipUnknownField(_t_);
			_o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
	}
}
