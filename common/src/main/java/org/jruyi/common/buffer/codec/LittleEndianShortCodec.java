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

import org.jruyi.common.IBuffer2.IUnit;
import org.jruyi.common.IBuffer2.IUnitChain;
import org.jruyi.common.ShortCodec;

public final class LittleEndianShortCodec extends ShortCodec {

	@Override
	public short read(IUnitChain unitChain, int length) {
		int s = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int position = unit.position();
			int count = size - position;
			if (count >= length) {
				s = getShortL(unit, position, s, length);
				unit.position(position + length);
				break;
			}
			s = getShortL(unit, position, s, count);
			unit.position(size);

			length -= count;
			unit = unitChain.nextUnit();
		}
		return (short) s;
	}

	@Override
	public void write(short s, IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		byte[] array = unit.array();
		int size = unit.size();
		int capacity = unit.capacity();
		if (size >= capacity) {
			unit = unitChain.nextUnit();
			array = unit.array();
			size = unit.size();
			capacity = unit.capacity();
		}

		array[size] = (byte) s;
		if (++size >= capacity) {
			unit.size(size);
			unit = unitChain.nextUnit();
			array = unit.array();
			size = unit.size();
			capacity = unit.capacity();
		}

		array[size] = (byte) (s >> 8);
		unit.size(++size);
	}

	@Override
	public short get(IUnitChain unitChain, int index, int length) {
		int s = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int count = unit.size() - index;
			if (count >= length) {
				s = getShortL(unit, index, s, length);
				break;
			}
			s = getShortL(unit, index, s, count);
			length -= count;
			unit = unitChain.nextUnit();
			index = 0;
		}
		return (short) s;
	}

	@Override
	public void set(short s, IUnitChain unitChain, int index) {
		IUnit unit = unitChain.currentUnit();
		byte[] array = unit.array();
		array[index] = (byte) s;
		if (++index >= unit.size()) {
			unit = unitChain.nextUnit();
			array = unit.array();
			index = 0;
		}
		array[index] = (byte) (s >> 8);
	}

	private static int getShortL(IUnit unit, int position, int s, final int length) {
		position += unit.start();
		int end = position + length;
		byte[] array = unit.array();
		for (; position < end; ++position)
			s = (s >>> 8) | (array[position] << 8);

		return s;
	}
}
