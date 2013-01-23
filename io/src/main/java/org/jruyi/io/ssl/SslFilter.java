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

import java.lang.reflect.Method;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.jruyi.common.StrUtil;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.ISslContextInfo;
import org.jruyi.io.ShortCodec;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SslFilter implements IFilter {

	private static final Logger c_logger = LoggerFactory
			.getLogger(SslFilter.class);
	private static final Object SSL_CODEC = new Object();
	private SSLContext m_sslContext;
	private Configuration m_conf;
	private ISslContextInfo m_sslci;

	static final class Configuration {

		private static final String[] M_PROPS = { "protocol", "provider" };
		private static final Method[] c_mProps;
		private String m_protocol;
		private String m_provider;
		private Boolean m_needClientAuth;
		private Boolean m_wantClientAuth;
		private String[] m_enabledProtocols;
		private String[] m_enabledCipherSuites;

		static {
			Class<Configuration> clazz = Configuration.class;
			c_mProps = new Method[M_PROPS.length];
			try {
				for (int i = 0; i < M_PROPS.length; ++i)
					c_mProps[i] = clazz.getMethod(M_PROPS[i]);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		public void initialize(Map<String, ?> properties) {
			protocol((String) properties.get("protocol"));
			provider((String) properties.get("provider"));
			needClientAuth((Boolean) properties.get("needClientAuth"));
			wantClientAuth((Boolean) properties.get("wantClientAuth"));
			enabledProtocols((String[]) properties.get("enabledProtocols"));
			enabledCipherSuites((String[]) properties
					.get("enabledCipherSuites"));
		}

		public void protocol(String protocol) {
			m_protocol = protocol;
		}

		public String protocol() {
			return m_protocol;
		}

		public void provider(String provider) {
			m_provider = provider;
		}

		public String provider() {
			return m_provider;
		}

		public void needClientAuth(Boolean needClientAuth) {
			m_needClientAuth = needClientAuth;
		}

		public Boolean needClientAuth() {
			return m_needClientAuth;
		}

		public void wantClientAuth(Boolean wantClientAuth) {
			m_wantClientAuth = wantClientAuth;
		}

		public Boolean wantClientAuth() {
			return m_wantClientAuth;
		}

		public void enabledProtocols(String[] enabledProtocols) {
			m_enabledProtocols = enabledProtocols;
		}

		public String[] enabledProtocols() {
			return m_enabledProtocols;
		}

		public void enabledCipherSuites(String[] enabledCipherSuites) {
			m_enabledCipherSuites = enabledCipherSuites;
		}

		public String[] enabledCipherSuites() {
			return m_enabledCipherSuites;
		}

		public boolean isMandatoryChanged(Configuration conf) throws Exception {
			for (Method m : c_mProps) {
				Object v1 = m.invoke(this);
				Object v2 = m.invoke(conf);
				if (v1 == v2)
					continue;

				if (!(v1 == null ? v2.equals(v1) : v1.equals(v2)))
					return true;
			}

			return false;
		}
	}

	// Byte 0 = SSL record type, if type >= 0x80, SSLv2
	// Bytes 1-2 = SSL version (major/minor)
	// Bytes 3-4 = Length of data in the record (excluding the header itself).
	// The maximum SSL supports is 16384 (16K).
	@Override
	public int tellBoundary(ISession session, IBuffer in) {
		int type = in.byteAt(0) & 0xFF;
		if (type > 0x17) { // SSLv2
			if (in.length() < 2)
				return E_UNDERFLOW;

			int mask = type < 0x80 ? 0x3F : 0x7F;
			return ((type & mask) << 8 | (in.byteAt(1) & 0xFF)) + 2;
		}

		if (in.length() < 5)
			return E_UNDERFLOW;

		return in.getUnsignedShort(3, ShortCodec.bigEndian()) + 5;
	}

	@Override
	public boolean onMsgArrive(ISession session, Object msg,
			IFilterOutput output) {
		IBuffer netData;
		try {
			netData = (IBuffer) msg;
		} catch (ClassCastException e) {
			throw new RuntimeException(
					"SSL filter only handles data of type IBuffer");
		}

		SslCodec sslCodec = (SslCodec) session.inquiry(SSL_CODEC);
		if (sslCodec == null) {
			// server mode
			sslCodec = new SslCodec(createEngine(false));
			session.deposit(SSL_CODEC, sslCodec);
		}

		IBuffer appBuf = netData.newBuffer();
		try {
			SSLEngine engine = sslCodec.engine();
			for (;;) {
				appBuf.write(netData, sslCodec);
				SSLEngineResult result = sslCodec.unwrapResult();
				Status status = result.getStatus();
				if (status == Status.OK) {
					HandshakeStatus hs = result.getHandshakeStatus();
					if (hs == HandshakeStatus.NEED_TASK) {
						runDelegatedTask(engine);
						continue;
					}

					if (hs == HandshakeStatus.NEED_UNWRAP) {
						appBuf.close();
						return true;
					} else if (hs == HandshakeStatus.FINISHED) {
						appBuf.close();
						IBuffer inception = sslCodec.inception();
						if (inception == null)
							return true;

						sslCodec.inception(null);

						appBuf = inception;
					}

					break;
				}

				appBuf.close();
				// BUFFER_UNDERFLOW, CLOSED
				return true;
			}

			output.add(appBuf);
			return !appBuf.isEmpty();
		} catch (Exception e) {
			c_logger.error(StrUtil.buildString(session, " failed to unwrap"), e);
			appBuf.close();
			return false;
		} finally {
			netData.close();
		}
	}

	@Override
	public boolean onMsgDepart(ISession session, Object msg,
			IFilterOutput output) {
		IBuffer appData;
		try {
			appData = (IBuffer) msg;
		} catch (ClassCastException e) {
			throw new RuntimeException(
					"SSL filter only handles data of type IBuffer");
		}

		SslCodec sslCodec = (SslCodec) session.inquiry(SSL_CODEC);
		if (sslCodec == null) {
			// client mode
			sslCodec = new SslCodec(createEngine(true));
			session.deposit(SSL_CODEC, sslCodec);

			if (!appData.isEmpty())
				sslCodec.inception(appData.split(appData.size()));
		}

		IBuffer netBuf = appData.newBuffer();
		try {
			SSLEngine engine = sslCodec.engine();
			for (;;) {
				appData.read(netBuf, sslCodec);
				SSLEngineResult result = sslCodec.wrapResult();
				Status status = result.getStatus();
				if (status == Status.OK) {
					HandshakeStatus hs = result.getHandshakeStatus();
					if (hs == HandshakeStatus.NEED_TASK) {
						runDelegatedTask(engine);
						continue;
					}

					if (hs == HandshakeStatus.NEED_WRAP) {
						appData.compact();
						continue;
					}

					break;
				}

				netBuf.close();
				return status == Status.BUFFER_UNDERFLOW;
			}

			output.add(netBuf);
			return true;
		} catch (Exception e) {
			c_logger.error(StrUtil.buildString(session, " failed to wrap"), e);
			netBuf.close();
			return false;
		} finally {
			appData.close();
		}
	}

	protected void setSslContextInfo(ISslContextInfo sslci) {
		m_sslci = sslci;
	}

	protected void unsetSslContextInfo(ISslContextInfo sslci) {
		if (m_sslci == sslci)
			m_sslci = null;
	}

	protected void updatedSslContextInfo(ISslContextInfo sslci)
			throws Exception {
		m_sslContext = createSslContext(m_conf);
	}

	protected void modified(Map<String, ?> properties) throws Exception {
		Configuration newConf = getConf(properties);
		if (m_conf.isMandatoryChanged(newConf))
			m_sslContext = createSslContext(newConf);

		m_conf = newConf;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		Configuration conf = getConf(properties);
		m_sslContext = createSslContext(conf);
		m_conf = conf;
	}

	protected void deactivate() {
		m_conf = null;
		m_sslContext = null;
	}

	private synchronized SSLContext createSslContext(Configuration conf)
			throws Exception {
		String provider = conf.provider();
		SSLContext sslContext = (provider == null || provider.length() < 1) ? SSLContext
				.getInstance(conf.protocol()) : SSLContext.getInstance(
				conf.protocol(), provider);

		ISslContextInfo sslci = m_sslci;
		sslContext.init(sslci.getKeyManagers(), sslci.getCertManagers(),
				sslci.getSecureRandom());
		return sslContext;
	}

	private Configuration getConf(Map<String, ?> properties) throws Exception {
		Configuration conf = new Configuration();
		conf.initialize(properties);
		return conf;
	}

	private SSLEngine createEngine(boolean clientMode) {
		SSLEngine engine = m_sslContext.createSSLEngine();
		Configuration conf = m_conf;
		if (conf.enabledProtocols() != null)
			engine.setEnabledProtocols(conf.enabledProtocols());

		if (conf.enabledCipherSuites() != null)
			engine.setEnabledCipherSuites(conf.enabledCipherSuites());

		engine.setNeedClientAuth(conf.needClientAuth());
		engine.setWantClientAuth(conf.wantClientAuth());
		engine.setUseClientMode(clientMode);

		return engine;
	}

	private void runDelegatedTask(SSLEngine engine) {
		Runnable task;
		while ((task = engine.getDelegatedTask()) != null)
			task.run();
	}
}
