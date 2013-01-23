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

public final class LittleEndianCharCodec implements ICharCodec {

	public static final ICharCodec INST = new LittleEndianCharCodec();

	private LittleEndianCharCodec() {
	}

	public char read(IUnitChain unitChain) {
		return (char) LittleEndianShortCodec.INST.read(unitChain);
	}

	@Override
	public void write(char c, IUnitChain unitChain) {
		LittleEndianShortCodec.INST.write((short) c, unitChain);
	}

	@Override
	public char get(IUnitChain unitChain, int index) {
		return (char) LittleEndianShortCodec.INST.get(unitChain, index);
	}

	@Override
	public void set(char c, IUnitChain unitChain, int index) {
		LittleEndianShortCodec.INST.set((short) c, unitChain, index);
	}

	@Override
	public void prepend(char c, IUnitChain unitChain) {
		LittleEndianShortCodec.INST.prepend((short) c, unitChain);
	}
}
