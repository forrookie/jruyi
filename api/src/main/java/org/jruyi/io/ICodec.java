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

/**
 * A codec is used to encode/decode data to/from a buffer-unit chain.
 * 
 * @param <T>
 *            the type of the data object to be encode/decode
 */
public interface ICodec<T> {

	public T read(IUnitChain unitChain);

	public T read(IUnitChain unitChain, int length);

	public int read(T dst, IUnitChain unitChain);

	public int read(T dst, int offset, int length, IUnitChain unitChain);

	public void write(T src, IUnitChain unitChain);

	public void write(T src, int offset, int length, IUnitChain unitChain);

	public T get(IUnitChain unitChain, int index);

	public T get(IUnitChain unitChain, int index, int length);

	public void get(T dst, IUnitChain unitChain, int index);

	public void get(T dst, int offset, int length, IUnitChain unitChain,
			int index);

	public void set(T src, IUnitChain unitChain, int index);

	public void set(T src, int offset, int length, IUnitChain unitChain,
			int index);

	public void prepend(T src, IUnitChain unitChain);

	public void prepend(T src, int offset, int length, IUnitChain unitChain);
}
