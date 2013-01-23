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

public final class ShortCodec {

	private static final IShortCodecProvider c_provider = CodecProvider
			.getInstance().getShortCodecProvider();

	public interface IShortCodecProvider {

		public IShortCodec bigEndianShortCodec();

		public IShortCodec littleEndianShortCodec();
	}

	public static IShortCodec bigEndian() {
		return c_provider.bigEndianShortCodec();
	}

	public static IShortCodec littleEndian() {
		return c_provider.littleEndianShortCodec();
	}

	private ShortCodec() {
	}
}
