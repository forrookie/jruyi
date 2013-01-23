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

import java.nio.ByteBuffer;

import org.jruyi.common.IByteSequence;

public interface IUnit extends IByteSequence {

	public void set(int index, byte b);

	public void set(int index, IByteSequence src, int srcBegin, int srcEnd);

	public void set(int index, byte[] src, int offset, int length);

	public void setFill(int index, byte b, int count);

	public int start();

	public void start(int start);

	public int position();

	public void position(int position);

	public int size();

	public void size(int size);

	public int remaining();

	public int availabe();

	public int capacity();

	public int mark();

	public void mark(int mark);

	public boolean appendable();

	public boolean prependable();

	public boolean isEmpty();

	public void reset();

	public void rewind();

	public void clear();

	public void compact();

	public int skip(int n);

	public ByteBuffer getByteBufferForRead();

	public ByteBuffer getByteBufferForRead(int offset, int length);

	public ByteBuffer getByteBufferForWrite();
}