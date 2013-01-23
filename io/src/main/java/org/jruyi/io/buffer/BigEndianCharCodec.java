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
package org.jruyi.io.buffer;

import org.jruyi.io.ICharCodec;
import org.jruyi.io.IUnitChain;

public final class BigEndianCharCodec implements ICharCodec {

	public static final ICharCodec INST = new BigEndianCharCodec();

	private BigEndianCharCodec() {
	}

	@Override
	public char read(IUnitChain unitChain) {
		return (char) BigEndianShortCodec.INST.read(unitChain);
	}

	@Override
	public void write(char c, IUnitChain unitChain) {
		BigEndianShortCodec.INST.write((short) c, unitChain);
	}

	@Override
	public char get(IUnitChain unitChain, int index) {
		return (char) BigEndianShortCodec.INST.get(unitChain, index);
	}

	@Override
	public void set(char c, IUnitChain unitChain, int index) {
		BigEndianShortCodec.INST.set((short) c, unitChain, index);
	}

	@Override
	public void prepend(char c, IUnitChain unitChain) {
		BigEndianShortCodec.INST.prepend((short) c, unitChain);
	}
}
