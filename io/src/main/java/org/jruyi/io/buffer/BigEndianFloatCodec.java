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

import org.jruyi.io.IFloatCodec;
import org.jruyi.io.IUnitChain;

public final class BigEndianFloatCodec implements IFloatCodec {

	public static final IFloatCodec INST = new BigEndianFloatCodec();

	private BigEndianFloatCodec() {
	}

	@Override
	public float read(IUnitChain unitChain) {
		return Float.intBitsToFloat(BigEndianIntCodec.INST.read(unitChain));
	}

	@Override
	public void write(float f, IUnitChain unitChain) {
		BigEndianIntCodec.INST.write(Float.floatToRawIntBits(f), unitChain);
	}

	@Override
	public float get(IUnitChain unitChain, int index) {
		return Float.intBitsToFloat(BigEndianIntCodec.INST
				.get(unitChain, index));
	}

	@Override
	public void set(float f, IUnitChain unitChain, int index) {
		BigEndianIntCodec.INST
				.set(Float.floatToRawIntBits(f), unitChain, index);
	}

	@Override
	public void prepend(float f, IUnitChain unitChain) {
		BigEndianIntCodec.INST.prepend(Float.floatToRawIntBits(f), unitChain);
	}
}
