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
import org.jruyi.common.IntCodec;

public final class BigEndianIntCodec extends IntCodec {

	public static final IntCodec INST = new BigEndianIntCodec();

	private BigEndianIntCodec() {
	}

	@Override
	public int read(IUnitChain unitChain, int length) {
		int i = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int position = unit.position();
			int count = size - position;
			if (count >= length) {
				i = getIntB(unit, position, i, length);
				unit.position(position + length);
				break;
			}
			i = getIntB(unit, position, i, count);
			unit.position(size);

			length -= count;
			unit = unitChain.nextUnit();
		}
		return i;
	}

	@Override
	public void write(int i, IUnitChain unitChain) {
		// TODO Auto-generated method stub

	}

	@Override
	public int get(IUnitChain unitChain, int index, int length) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void set(int i, IUnitChain unitChain, int index) {
		// TODO Auto-generated method stub

	}

	/**
	 * Return an int value by left shifting the next {@code length} bytes
	 * starting at {@code position} into the given {@code i} sequentially. The
	 * {@code length} passed in must be not greater than {@code size()
	 * - position}.
	 * 
	 * @param unit
	 * @param position
	 *            the offset of the first byte to be left shifted
	 * @param i
	 *            the base int value to be left shifted into
	 * @param length
	 *            number of bytes to be left shifted into {@code i}
	 * @return the resultant int value
	 */
	private static int getIntB(IUnit unit, int position, int i, final int length) {
		position += unit.start();
		int end = position + length;
		byte[] array = unit.array();
		for (; position < end; ++position)
			i = (i << 8) | (array[position] & 0xFF);

		return i;
	}

	private static int setIntB(IUnit unit, int position, int i, final int length) {
		position += unit.start();
		int end = position + length;
		byte[] array = unit.array();
		for (; position < end; ++position) {
			array[position] = (byte) (i >>> 24);
			i <<= 8;
		}

		return i;
	}
}
