/**
 * Copyright 2012 JRuyi.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jruyi.common.buffer.codec;

import java.nio.CharBuffer;

import org.jruyi.common.BytesBuilder;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.Codec;
import org.jruyi.common.IBuffer.IUnit;
import org.jruyi.common.IBuffer.IUnitChain;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.buffer.ByteBufferArray;
import org.jruyi.common.buffer.Util;

public final class CharArrayCodec extends Codec<char[]> {

	private final String m_charsetName;

	public CharArrayCodec(String charsetName) {
		m_charsetName = charsetName;
	}

	@Override
	public char[] read(IUnitChain unitChain) {
		ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(unit.position(), unit.remaining()));
			unit.position(unit.size());
			while ((unit = unitChain.nextUnit()) != null) {
				bba.add(unit.getByteBufferForRead(unit.position(),
						unit.remaining()));
				unit.position(unit.size());
			}
			ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.decode(bba.array());
		} finally {
			bba.clear();
		}
	}

	@Override
	public char[] read(IUnitChain unitChain, int length) {
		if (length < 0)
			throw new IllegalArgumentException();
		if (length == 0)
			return Helper.EMPTY_CHARS;

		ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(unit.position(), length));
			while ((length -= unit.skip(length)) > 0) {
				unit = unitChain.nextUnit();
				bba.add(unit.getByteBufferForRead(unit.position(), length));
			}
			ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.decode(bba.array());
		} finally {
			bba.clear();
		}
	}

	@Override
	public char[] get(IUnitChain unitChain, int index) {
		ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(index, unit.size() - index));
			while ((unit = unitChain.nextUnit()) != null)
				bba.add(unit.getByteBufferForRead(0, unit.size()));
			ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.decode(bba.array());
		} finally {
			bba.clear();
		}
	}

	@Override
	public char[] get(IUnitChain unitChain, int index, int length) {
		if (length < 0)
			throw new IndexOutOfBoundsException();
		if (length == 0)
			return Helper.EMPTY_CHARS;

		ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(index, length));
			length -= (unit.size() - index);
			while (length > 0) {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				bba.add(unit.getByteBufferForRead(0, length));
				length -= unit.size();
			}
			ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.decode(bba.array());
		} finally {
			bba.clear();
		}
	}

	@Override
	public void write(char[] chars, IUnitChain unitChain) {
		ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.write(cc, CharBuffer.wrap(chars), unitChain);
	}

	@Override
	public void write(char[] chars, int offset, int length, IUnitChain unitChain) {
		ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.write(cc, CharBuffer.wrap(chars, offset, length), unitChain);
	}

	@Override
	public void prepend(char[] chars, IUnitChain unitChain) {
		ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		BytesBuilder bb = BytesBuilder.get();
		try {
			cc.encode(chars, bb);
			int length = bb.length();
			IUnit unit = Util.firstUnit(unitChain);
			while ((length -= Helper.prepend(bb, 0, length, unit)) > 0)
				unit = Util.prependNewUnit(unitChain);
		} finally {
			bb.close();
		}
	}

	@Override
	public void prepend(char[] chars, int offset, int length,
			IUnitChain unitChain) {
		ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		BytesBuilder bb = BytesBuilder.get();
		try {
			cc.encode(chars, offset, length, bb);
			length = bb.length();
			IUnit unit = Util.firstUnit(unitChain);
			while ((length -= Helper.prepend(bb, 0, length, unit)) > 0)
				unit = Util.prependNewUnit(unitChain);
		} finally {
			bb.close();
		}
	}
}