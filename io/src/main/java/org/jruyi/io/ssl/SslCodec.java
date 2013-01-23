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
package org.jruyi.io.ssl;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLSession;

import org.jruyi.common.BytesBuilder;
import org.jruyi.io.AbstractCodec;
import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Buffer;
import org.jruyi.io.buffer.ByteBufferArray;
import org.jruyi.io.buffer.Util;

final class SslCodec extends AbstractCodec<IBuffer> {

	private final SSLEngine m_engine;
	private IBuffer m_inception;
	private SSLEngineResult m_unwrapResult;
	private SSLEngineResult m_wrapResult;
	private ArrayList<IUnit> m_units;

	SslCodec(SSLEngine engine) {
		m_engine = engine;
	}

	/**
	 * Decodes the SSL/TLS network data in the specified {@code src} into plain
	 * text application data.
	 * <p>
	 * netData =(unwrap)=> appData
	 * 
	 * <pre>
	 * appBuffer.write(netData)
	 * </pre>
	 * 
	 * @param src
	 *            netData
	 */
	@Override
	public void write(IBuffer src, IUnitChain appBuf) {
		ByteBuffer netData = null;
		if (src instanceof Buffer) {
			Buffer buffer = (Buffer) src;
			final IUnit lastUnit = buffer.lastUnit();
			IUnit unit = buffer.currentUnit();
			if (unit.isEmpty()) {
				do {
					unit = buffer.nextUnit();
					if (unit == null) {
						unit = lastUnit;
						break;
					}
				} while (unit.isEmpty());
			}
			if (unit == lastUnit)
				netData = unit.getByteBufferForRead();
		}

		BytesBuilder builder = null;
		if (netData == null) {
			int length = src.remaining();
			builder = BytesBuilder.get(length);
			builder.append(src, src.position(), length);
			netData = builder.getByteBuffer(0, length);
		}

		SSLEngineResult result = null;
		ByteBufferArray bba = ByteBufferArray.get();
		final ArrayList<IUnit> units = m_units;
		try {
			// int pos = netData.position();
			IUnit unit = Util.lastUnit(appBuf);
			units.add(unit);
			bba.add(unit.getByteBufferForWrite());
			final SSLEngine engine = m_engine;
			for (;;) {
				result = engine.unwrap(netData, bba.array(), 0, bba.size());
				Status status = result.getStatus();
				if (status != Status.BUFFER_OVERFLOW)
					break;

				unit = Util.appendNewUnit(appBuf);
				units.add(unit);
				bba.add(unit.getByteBufferForWrite());
			}

			// src.skip(netData.position() - pos);
			final ByteBuffer[] array = bba.array();
			int n = bba.size();
			for (int i = 0; i < n; ++i) {
				unit = units.get(i);
				unit.size(array[i].position() - unit.start());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (builder != null)
				builder.close();

			units.clear();
			bba.clear();
		}

		m_unwrapResult = result;
	}

	/**
	 * Encodes the plain text application data in the given {@code unitChain}
	 * into SSL/TLS network data and writes the resultant data into the given
	 * {@code dst}.
	 * <p>
	 * appData =(wrap)=> netData
	 * 
	 * <pre>
	 * appData.read(netBuffer)
	 * </pre>
	 * 
	 * @param dst
	 *            netBuffer
	 */
	@Override
	public int read(IBuffer dst, IUnitChain appBuf) {
		ByteBufferArray bba = ByteBufferArray.get();
		IUnit unit = appBuf.currentUnit();
		do {
			bba.add(unit.getByteBufferForRead());
			unit = appBuf.nextUnit();
		} while (unit != null);

		final SSLEngine engine = m_engine;
		BytesBuilder builder = null;
		ByteBuffer netBuf = null;
		Buffer dstBuf = null;
		if (dst instanceof Buffer) {
			dstBuf = (Buffer) dst;
			unit = Util.lastUnit(dstBuf);
			netBuf = unit.getByteBufferForWrite();
		} else {
			SSLSession session = engine.getSession();
			builder = BytesBuilder.get(session.getPacketBufferSize());
			netBuf = builder.getByteBuffer(0, builder.capacity());
		}

		SSLEngineResult result = null;
		try {
			ByteBuffer[] appData = bba.array();
			int size = bba.size();
			for (;;) {
				result = engine.wrap(appData, 0, size, netBuf);
				Status status = result.getStatus();
				if (status != Status.BUFFER_OVERFLOW)
					break;

				SSLSession session = engine.getSession();
				if (builder == null)
					builder = BytesBuilder.get(session.getPacketBufferSize());
				else
					builder.ensureCapacity(session.getPacketBufferSize());

				netBuf = builder.getByteBuffer(0, builder.capacity());
			}
			if (builder != null)
				dst.write(builder, 0, netBuf.position(), Codec.byteSequence());
			else
				unit.size(netBuf.position() - unit.start());
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (builder != null)
				builder.close();

			bba.clear();
		}

		m_wrapResult = result;
		return 0;
	}

	SSLEngineResult unwrapResult() {
		SSLEngineResult result = m_unwrapResult;
		m_unwrapResult = null;
		return result;
	}

	SSLEngineResult wrapResult() {
		SSLEngineResult result = m_wrapResult;
		m_wrapResult = null;
		return result;
	}

	SSLEngine engine() {
		return m_engine;
	}

	IBuffer inception() {
		return m_inception;
	}

	void inception(IBuffer inception) {
		m_inception = inception;
	}
}
