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
package org.jruyi.io;

import org.jruyi.io.internal.CodecProvider;

public final class LongCodec {

	private static final ILongCodecProvider c_provider = CodecProvider
			.getInstance().getLongCodecProvider();

	public interface ILongCodecProvider {

		public ILongCodec bigEndianLongCodec();

		public ILongCodec littleEndianLongCodec();
	}

	public static ILongCodec bigEndian() {
		return c_provider.bigEndianLongCodec();
	}

	public static ILongCodec littleEndian() {
		return c_provider.littleEndianLongCodec();
	}

	private LongCodec() {
	}
}
