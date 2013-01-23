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

import org.jruyi.common.IByteSequence;
import org.jruyi.io.internal.CodecProvider;

public final class Codec<T> {

	private static final ICodecProvider c_provider = CodecProvider
			.getInstance().getCodecProvider();

	public interface ICodecProvider {

		public ICodec<byte[]> byteArray();

		public ICodec<IByteSequence> byteSequence();

		public ICodec<String> utf_8();

		public ICodec<String> utf_16();

		public ICodec<String> utf_16le();

		public ICodec<String> utf_16be();

		public ICodec<String> iso_8859_1();

		public ICodec<String> us_ascii();

		public ICodec<char[]> utf_8_array();

		public ICodec<char[]> utf_16_array();

		public ICodec<char[]> utf_16le_array();

		public ICodec<char[]> utf_16be_array();

		public ICodec<char[]> iso_8859_1_array();

		public ICodec<char[]> us_ascii_array();

		public ICodec<CharSequence> utf_8_sequence();

		public ICodec<CharSequence> utf_16_sequence();

		public ICodec<CharSequence> utf_16le_sequence();

		public ICodec<CharSequence> utf_16be_sequence();

		public ICodec<CharSequence> iso_8859_1_sequence();

		public ICodec<CharSequence> us_ascii_sequence();

		public ICodec<String> charset(String charsetName);

		public ICodec<char[]> charset_array(String charsetName);

		public ICodec<CharSequence> charset_sequence(String charsetName);
	}

	private Codec() {
	}

	public static ICodec<byte[]> byteArray() {
		return c_provider.byteArray();
	}

	public static ICodec<IByteSequence> byteSequence() {
		return c_provider.byteSequence();
	}

	public static ICodec<String> utf_8() {
		return c_provider.utf_8();
	}

	public static ICodec<String> utf_16() {
		return c_provider.utf_16();
	}

	public static ICodec<String> utf_16le() {
		return c_provider.utf_16le();
	}

	public static ICodec<String> utf_16be() {
		return c_provider.utf_16be();
	}

	public static ICodec<String> iso_8859_1() {
		return c_provider.iso_8859_1();
	}

	public static ICodec<String> us_ascii() {
		return c_provider.us_ascii();
	}

	public static ICodec<char[]> utf_8_array() {
		return c_provider.utf_8_array();
	}

	public static ICodec<char[]> utf_16_array() {
		return c_provider.utf_16_array();
	}

	public static ICodec<char[]> utf_16le_array() {
		return c_provider.utf_16le_array();
	}

	public static ICodec<char[]> utf_16be_array() {
		return c_provider.utf_16be_array();
	}

	public static ICodec<char[]> iso_8859_1_array() {
		return c_provider.iso_8859_1_array();
	}

	public static ICodec<char[]> us_ascii_array() {
		return c_provider.us_ascii_array();
	}

	public static ICodec<CharSequence> utf_8_sequence() {
		return c_provider.utf_8_sequence();
	}

	public static ICodec<CharSequence> utf_16_sequence() {
		return c_provider.utf_16_sequence();
	}

	public static ICodec<CharSequence> utf_16le_sequence() {
		return c_provider.utf_16le_sequence();
	}

	public static ICodec<CharSequence> utf_16be_sequence() {
		return c_provider.utf_16be_sequence();
	}

	public static ICodec<CharSequence> iso_8859_1_sequence() {
		return c_provider.iso_8859_1_sequence();
	}

	public static ICodec<CharSequence> us_ascii_sequence() {
		return c_provider.us_ascii_sequence();
	}

	public static ICodec<String> charset(String charsetName) {
		return c_provider.charset(charsetName);
	}

	public static ICodec<char[]> charset_array(String charsetName) {
		return c_provider.charset_array(charsetName);
	}

	public static ICodec<CharSequence> charset_sequence(String charsetName) {
		return c_provider.charset_sequence(charsetName);
	}
}
