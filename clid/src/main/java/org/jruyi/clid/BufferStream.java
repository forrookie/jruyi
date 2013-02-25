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
package org.jruyi.clid;

import java.io.IOException;
import java.io.OutputStream;

import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProducer;

final class BufferStream extends OutputStream {

	private static final int FLUSH_THRESHOLD = 8088;
	private static final byte[] CR = { '\r' };
	private static final byte[] LF = { '\n' };
	private static final int HEAD_RESERVE_SIZE = 4;
	private final IProducer m_producer;
	private final LinkedQueue<IBuffer> m_queue;
	private IBuffer m_buffer;
	private IMessage m_message;

	public BufferStream(IProducer producer) {
		m_producer = producer;
		m_queue = new LinkedQueue<IBuffer>();
	}

	public void buffer(IBuffer buffer) {
		buffer.reserveHead(HEAD_RESERVE_SIZE);
		m_buffer = buffer;
	}

	public void message(IMessage message) {
		m_message = message;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void flush() throws IOException {
		IBuffer buffer = m_buffer;
		if (buffer.isEmpty())
			return;

		m_buffer = buffer.newBuffer();
		m_buffer.reserveHead(HEAD_RESERVE_SIZE);

		prependLength(buffer);
		m_queue.put(buffer);
		IMessage message = m_message.duplicate();
		message.attach(m_queue);
		m_producer.send(message);
	}

	public void write(String str) {
		m_buffer.write(str, Codec.utf_8());
	}

	public void write(CharSequence cs) {
		m_buffer.write(cs, Codec.utf_8_sequence());
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		m_buffer.write(b, off, len, Codec.byteArray());
		checkFlush();
	}

	@Override
	public void write(byte[] b) throws IOException {
		m_buffer.write(b, Codec.byteArray());
		checkFlush();
	}

	@Override
	public void write(int b) throws IOException {
		m_buffer.write((byte) b);
		checkFlush();
	}

	public void writeOut(String prompt) {
		IBuffer out = m_buffer;
		m_buffer = null;
		IMessage message = m_message;
		m_message = null;

		if (!out.isEmpty() && !out.endsWith(CR) && !out.endsWith(LF)) {
			out.write(CR[0]);
			out.write(LF[0]);
		}

		if (!prompt.isEmpty())
			out.write(prompt, Codec.utf_8());

		if (!out.isEmpty())
			prependLength(out);

		// EOF
		out.write((byte) 0);

		m_queue.put(out);
		message.attach(m_queue);
		m_producer.send(message);
	}

	private static void prependLength(IBuffer buffer) {
		int n = buffer.size();
		buffer.prepend((byte) (n & 0x7F));
		while ((n >>= 7) > 0)
			buffer.prepend((byte) ((n & 0x7F) | 0x80));
	}

	private void checkFlush() throws IOException {
		if (m_buffer.size() >= FLUSH_THRESHOLD)
			flush();
	}
}
