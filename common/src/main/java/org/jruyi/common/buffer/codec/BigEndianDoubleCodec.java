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

import org.jruyi.common.DoubleCodec;
import org.jruyi.common.IBuffer2.IUnitChain;

public final class BigEndianDoubleCodec extends DoubleCodec {

	public static final DoubleCodec INST = new BigEndianDoubleCodec();

	@Override
	public double read(IUnitChain unitChain, int length) {
		return 0;
	}

	@Override
	public void write(double d, IUnitChain unitChain) {
	}

	@Override
	public double get(IUnitChain unitChain, int index, int length) {
		return 0;
	}

	@Override
	public void set(double d, IUnitChain unitChain, int index) {
	}

}
