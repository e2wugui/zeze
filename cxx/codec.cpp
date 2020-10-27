#include "common.h"

namespace limax {

	Codec::~Codec() {

	}

	class Null : public Codec {
	public:
		virtual void update(int8_t c) override {
		}
		virtual void update(int8_t data[], int32_t off, int32_t len) override {
		}
		virtual void flush() override {
		}
	};

	static std::shared_ptr<Null> _null(new Null());
	std::shared_ptr<Codec> Codec::Null() {
		return _null;
	}

	BufferedSink::BufferedSink(std::shared_ptr<Codec> _sink) :
		sink(_sink), pos(0) {
	}

	void BufferedSink::flushInternal() {
		if (pos > 0) {
			sink->update(buffer, 0, pos);
			pos = 0;
		}
	}

	void BufferedSink::update(int8_t c) {
		if (capacity == pos) {
			flushInternal();
		}
		buffer[pos++] = c;
	}

	void BufferedSink::update(int8_t data[], int32_t off, int32_t len) {
		if (len >= capacity) {
			flushInternal();
			sink->update(data, off, len);
			return;
		}
		if (len > capacity - pos) {
			flushInternal();
		}
		memmove(buffer + pos, data + off, len);
		pos += len;
	}

	void BufferedSink::flush() {
		flushInternal();
		sink->flush();
	}

	SinkStream::SinkStream(std::shared_ptr<std::ostream> _os) :
		os(_os) {
	}

	void SinkStream::update(int8_t c) {
		os->put(c);
	}

	void SinkStream::update(int8_t data[], int32_t off, int32_t len) {
		os->write(reinterpret_cast<const char*>(data + off), len);
	}

	void SinkStream::flush() {
		os->flush();
	}

	SinkOctets::SinkOctets(Octets& _o) : o(_o) { }

	void SinkOctets::update(int8_t data[], int32_t off, int32_t len)
	{
		o.insert(o.end(), data + off, len);
	}

	void SinkOctets::update(int8_t c)
	{
		update(&c, 0, 1);
	}

	void SinkOctets::flush()
	{
	}

	int8_t* Base64Encode::ENCODE = (int8_t *)"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	Base64Encode::Base64Encode(std::shared_ptr<Codec> _sink)
		: sink(_sink), b0(0), b1(0), b2(0), n(0)
	{}

	void Base64Encode::update0(int8_t r[], int32_t j, int8_t data[], int32_t off, int32_t len)
	{
		for (n = len; n > 2; n -= 3) {
			b0 = data[off++];
			b1 = data[off++];
			b2 = data[off++];
			int32_t c = ((b0 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
			r[j++] = ENCODE[c >> 18];
			r[j++] = ENCODE[(c >> 12) & 0x3f];
			r[j++] = ENCODE[(c >> 6) & 0x3f];
			r[j++] = ENCODE[c & 0x3f];
		}
		if (n == 1) {
			b0 = data[off];
		}
		else if (n == 2) {
			b0 = data[off];
			b1 = data[off + 1];
		}
	}

	void Base64Encode::update1(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		switch (len) {
		case 0:
			return;
		case 1:
			b1 = data[off];
			n = 2;
			return;
		}
		b1 = data[off];
		b2 = data[off + 1];
		int32_t c = ((b0 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
		r[0] = ENCODE[c >> 18];
		r[1] = ENCODE[(c >> 12) & 0x3f];
		r[2] = ENCODE[(c >> 6) & 0x3f];
		r[3] = ENCODE[c & 0x3f];
		update0(r, 4, data, off + 2, len - 2);
	}

	void Base64Encode::update2(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		if (len == 0)
			return;
		b2 = data[off];
		int32_t c = ((b0 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b2 & 0xff);
		r[0] = ENCODE[c >> 18];
		r[1] = ENCODE[(c >> 12) & 0x3f];
		r[2] = ENCODE[(c >> 6) & 0x3f];
		r[3] = ENCODE[c & 0x3f];
		update0(r, 4, data, off + 1, len - 1);
	}

	void Base64Encode::update(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		switch (n) {
		case 0:
			update0(r, 0, data, off, len);
			return;
		case 1:
			update1(r, data, off, len);
			return;
		}
		update2(r, data, off, len);
	}

	void Base64Encode::update(int8_t data[], int32_t off, int32_t len)
	{
		int32_t size = (len + n) / 3 * 4;
		int8_t* r = (int8_t *)alloca(size);
		update(r, data, off, len);
		sink->update(r, 0, size);
	}

	void Base64Encode::update(int8_t c)
	{
		update(&c, 0, 1);
	}

	void Base64Encode::flush()
	{
		int32_t c;
		int8_t r[4];
		switch (n)
		{
		case 1:
			c = b0 & 0xff;
			r[0] = ENCODE[c >> 2];
			r[1] = ENCODE[(c << 4) & 0x3f];
			r[2] = B64PAD;
			r[3] = B64PAD;
			sink->update(r, 0, sizeof(r));
			break;
		case 2:
			c = ((b0 & 0xff) << 8) | (b1 & 0xff);
			r[0] = ENCODE[c >> 10];
			r[1] = ENCODE[(c >> 4) & 0x3f];
			r[2] = ENCODE[(c << 2) & 0x3f];
			r[3] = B64PAD;
			sink->update(r, 0, sizeof(r));
		}
		sink->flush();
		n = 0;
	}

	Octets Base64Encode::transform(Octets data)
	{
		Octets r;
		Base64Encode codec(std::make_shared<SinkOctets>(r));
		codec.update((int8_t *)data.begin(), 0, (int32_t)data.size());
		codec.flush();
		return r;
	}

	int8_t Base64Decode::DECODE[] = { (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0x3e, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0x3f, (int8_t)0x34, (int8_t)0x35, (int8_t)0x36, (int8_t)0x37, (int8_t)0x38, (int8_t)0x39, (int8_t)0x3a, (int8_t)0x3b, (int8_t)0x3c, (int8_t)0x3d, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0x7f, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0x00, (int8_t)0x01, (int8_t)0x02, (int8_t)0x03, (int8_t)0x04, (int8_t)0x05, (int8_t)0x06, (int8_t)0x07, (int8_t)0x08, (int8_t)0x09, (int8_t)0x0a, (int8_t)0x0b, (int8_t)0x0c, (int8_t)0x0d, (int8_t)0x0e, (int8_t)0x0f, (int8_t)0x10, (int8_t)0x11, (int8_t)0x12, (int8_t)0x13, (int8_t)0x14, (int8_t)0x15, (int8_t)0x16, (int8_t)0x17, (int8_t)0x18, (int8_t)0x19, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0x1a, (int8_t)0x1b, (int8_t)0x1c, (int8_t)0x1d, (int8_t)0x1e, (int8_t)0x1f, (int8_t)0x20, (int8_t)0x21, (int8_t)0x22, (int8_t)0x23, (int8_t)0x24, (int8_t)0x25, (int8_t)0x26, (int8_t)0x27, (int8_t)0x28, (int8_t)0x29, (int8_t)0x2a, (int8_t)0x2b, (int8_t)0x2c, (int8_t)0x2d, (int8_t)0x2e, (int8_t)0x2f, (int8_t)0x30, (int8_t)0x31, (int8_t)0x32, (int8_t)0x33, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0xff, (int8_t)0x00 };

	Base64Decode::Base64Decode(std::shared_ptr<Codec> _sink)
		: sink(_sink), b0(0), b1(0), b2(0), b3(0), n(0)
	{}

	int32_t Base64Decode::update0(int8_t r[], int32_t j, int8_t data[], int32_t off, int32_t len)
	{
		for (n = len; n > 7; n -= 4) {
			b0 = DECODE[data[off++] & 0xff];
			b1 = DECODE[data[off++] & 0xff];
			b2 = DECODE[data[off++] & 0xff];
			b3 = DECODE[data[off++] & 0xff];
			if (b0 == (int8_t)0xff || b1 == (int8_t)0xff || b2 == (int8_t)0xff || b3 == (int8_t)0xff)
				throw CodecException();
			r[j++] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			r[j++] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
			r[j++] = (int8_t)(((b2 << 6) & 0xc0) | (b3 & 0x3f));
		}
		if (n > 3) {
			n -= 4;
			b0 = DECODE[data[off++] & 0xff];
			b1 = DECODE[data[off++] & 0xff];
			b2 = DECODE[data[off++] & 0xff];
			b3 = DECODE[data[off++] & 0xff];
			if (b0 == (int8_t)0xff || b1 == (int8_t)0xff || b2 == (int8_t)0xff || b3 == (int8_t)0xff)
				throw CodecException();
			if (b2 == 0x7f) {
				r[j++] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
				return j;
			}
			else if (b3 == 0x7f) {
				r[j++] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
				r[j++] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
				return j;
			}
			r[j++] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			r[j++] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
			r[j++] = (int8_t)(((b2 << 6) & 0xc0) | (b3 & 0x3f));
		}
		if (n == 1) {
			b0 = DECODE[data[off] & 0xff];
		}
		else if (n == 2) {
			b0 = DECODE[data[off] & 0xff];
			b1 = DECODE[data[off + 1] & 0xff];
		}
		else if (n == 3) {
			b0 = DECODE[data[off] & 0xff];
			b1 = DECODE[data[off + 1] & 0xff];
			b2 = DECODE[data[off + 2] & 0xff];
		}
		return j;
	}

	int32_t Base64Decode::update1(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		switch (len) {
		case 0:
			return 0;
		case 1:
			b1 = DECODE[data[off] & 0xff];
			n = 2;
			return 0;
		case 2:
			b1 = DECODE[data[off] & 0xff];
			b2 = DECODE[data[off + 1] & 0xff];
			n = 3;
			return 0;
		}
		b1 = DECODE[data[off] & 0xff];
		b2 = DECODE[data[off + 1] & 0xff];
		b3 = DECODE[data[off + 2] & 0xff];
		if (b0 == (int8_t)0xff || b1 == (int8_t)0xff || b2 == (int8_t)0xff || b3 == (int8_t)0xff)
			throw CodecException();
		if (b2 == 0x7f) {
			r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			return 1;
		}
		else if (b3 == 0x7f) {
			r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			r[1] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
			return 2;
		}
		r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
		r[1] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
		r[2] = (int8_t)(((b2 << 6) & 0xc0) | (b3 & 0x3f));
		return update0(r, 3, data, off + 3, len - 3);
	}

	int32_t Base64Decode::update2(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		switch (len) {
		case 0:
			return 0;
		case 1:
			b2 = DECODE[data[off] & 0xff];
			n = 3;
			return 0;
		}
		b2 = DECODE[data[off] & 0xff];
		b3 = DECODE[data[off + 1] & 0xff];
		if (b0 == (int8_t)0xff || b1 == (int8_t)0xff || b2 == (int8_t)0xff || b3 == (int8_t)0xff)
			throw new CodecException();
		if (b2 == 0x7f) {
			r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			return 1;
		}
		else if (b3 == 0x7f) {
			r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			r[1] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
			return 2;
		}
		r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
		r[1] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
		r[2] = (int8_t)(((b2 << 6) & 0xc0) | (b3 & 0x3f));
		return update0(r, 3, data, off + 2, len - 2);
	}

	int32_t Base64Decode::update3(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		if (len == 0)
			return 0;
		b3 = DECODE[data[off] & 0xff];
		if (b0 == (int8_t)0xff || b1 == (int8_t)0xff || b2 == (int8_t)0xff || b3 == (int8_t)0xff)
			throw new CodecException();
		if (b2 == 0x7f) {
			r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			return 1;
		}
		else if (b3 == 0x7f) {
			r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
			r[1] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
			return 2;
		}
		r[0] = (int8_t)(((b0 << 2) & 0xfc) | ((b1 >> 4) & 0x3));
		r[1] = (int8_t)(((b1 << 4) & 0xf0) | ((b2 >> 2) & 0xf));
		r[2] = (int8_t)(((b2 << 6) & 0xc0) | (b3 & 0x3f));
		return update0(r, 3, data, off + 1, len - 1);
	}

	int32_t Base64Decode::update(int8_t r[], int8_t data[], int32_t off, int32_t len)
	{
		switch (n) {
		case 0:
			return update0(r, 0, data, off, len);
		case 1:
			return update1(r, data, off, len);
		case 2:
			return update2(r, data, off, len);
		}
		return update3(r, data, off, len);
	}

	void Base64Decode::update(int8_t data[], int32_t off, int32_t len)
	{
		int32_t length = (n + len) / 4 * 3;
		int8_t* r = (int8_t*)alloca(length);
		int32_t rlen = update(r, data, off, len);
		sink->update(r, 0, rlen < length ? rlen : length);
	}

	void Base64Decode::update(int8_t c)
	{
		update(&c, 0, 1);
	}

	void Base64Decode::flush()
	{
		sink->flush();
		n = 0;
	}

	Octets Base64Decode::transform(Octets data)
	{
		Octets r;
		Base64Decode codec(std::make_shared<SinkOctets>(r));
		try
		{
			codec.update((int8_t*)data.begin(), 0, (int32_t)data.size());
			codec.flush();
		}
		catch (CodecException)
		{
		}
		return r;
	}

} // namespace limax {

namespace limax {

	LIMAX_DLL_EXPORT_API Octets base64Encode(const void* input, size_t size)
	{
		return Base64Encode::transform(Octets(input, size));
	}

	LIMAX_DLL_EXPORT_API Octets base64Decode(const void* input, size_t size)
	{
		return Base64Decode::transform(Octets(input, size));
	}

} // namespace limax {

