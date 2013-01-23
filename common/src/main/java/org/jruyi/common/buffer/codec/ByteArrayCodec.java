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

import java.nio.BufferUnderflowException;

import org.jruyi.common.Codec;
import org.jruyi.common.IBuffer2.IUnit;
import org.jruyi.common.IBuffer2.IUnitChain;

public final class RawBytesCodec extends Codec<byte[]> {

	public static final Codec<byte[]> INST = new RawBytesCodec();

	private RawBytesCodec() {
	}

	@Override
	public byte[] read(IUnitChain unitChain, int length) {
		if (length < 0)
			throw new IllegalArgumentException();

		byte[] dst = new byte[length];
		if (length > 0) {
			IUnit unit = unitChain.currentUnit();
			if (unit == null)
				throw new BufferUnderflowException();
			int offset = 0;
			int n = 0;
			while ((n = read(dst, offset, length, unit)) < length) {
				offset += n;
				length -= n;
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new BufferUnderflowException();
			}
		}
		return dst;
	}

	@Override
	public int read(byte[] dst, IUnitChain unitChain) {
		int offset = 0;
		int n = 0;
		int length = dst.length;
		IUnit unit = unitChain.currentUnit();
		while (unit != null && (n = read(dst, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
		}
		return dst.length - length;
	}

	@Override
	public int read(byte[] dst, int offset, int length, IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (dst.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();
		int n = 0;
		int count = length;
		IUnit unit = unitChain.currentUnit();
		while (unit != null && (n = read(dst, offset, count, unit)) < count) {
			count -= n;
			offset += n;
			unit = unitChain.nextUnit();
		}
		return length - count;
	}

	@Override
	public void write(byte[] src, IUnitChain unitChain) {
		int length = src.length;
		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n = 0;
		int offset = 0;
		while ((n = write(src, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public void write(byte[] src, int offset, int length, IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n = 0;
		while ((n = write(src, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public byte[] get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0)
			throw new IndexOutOfBoundsException();

		byte[] dst = new byte[length];
		if (length == 0)
			return dst;

		int offset = 0;
		IUnit unit = unitChain.currentUnit();
		if (unit == null)
			throw new IndexOutOfBoundsException();
		int n = get(dst, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, offset, length, unit, 0);
		}

		return dst;
	}

	@Override
	public void get(byte[] dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = dst.length;
		if (length == 0)
			return;

		int offset = 0;
		IUnit unit = unitChain.currentUnit();
		if (unit == null)
			throw new IndexOutOfBoundsException();
		int n = get(dst, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, offset, length, unit, 0);
		}
	}

	@Override
	public void get(byte[] dst, int offset, int length, IUnitChain unitChain,
			int index) {
		if ((offset | length | (offset + length) | (dst.length - (offset + length))) < 0
				|| index < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		if (unit == null)
			throw new IndexOutOfBoundsException();
		int n = get(dst, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, offset, length, unit, 0);
		}
	}

	@Override
	public void set(byte[] src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = src.length;
		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		if (unit == null)
			throw new IndexOutOfBoundsException();
		int offset = 0;
		int n = set(src, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = set(src, offset, length, unit, 0);
		}
	}

	@Override
	public void set(byte[] src, int offset, int length, IUnitChain unitChain,
			int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		if (unit == null)
			throw new IndexOutOfBoundsException();
		int n = set(src, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = set(src, offset, length, unit, 0);
		}
	}

	private static int get(byte[] dst, int offset, int length, IUnit unit,
			int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		System.arraycopy(unit.array(), unit.start() + position, dst, offset, n);
		return n;
	}

	private static int set(byte[] src, int offset, int length, IUnit unit,
			int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		System.arraycopy(src, offset, unit.array(), unit.start() + position, n);
		return n;
	}

	private static int read(byte[] dst, int offset, int length, IUnit unit) {
		int position = unit.position();
		int n = unit.size() - position;
		if (n > length)
			n = length;

		System.arraycopy(unit.array(), unit.start() + position, dst, offset, n);
		unit.position(position + n);
		return n;
	}

	private static int write(byte[] src, int offset, int length, IUnit unit) {
		int size = unit.size();
		int n = unit.capacity() - size;
		if (n > length)
			n = length;

		System.arraycopy(src, offset, unit.array(), unit.start() + size, n);
		unit.size(size + n);
		return n;
	}
}
