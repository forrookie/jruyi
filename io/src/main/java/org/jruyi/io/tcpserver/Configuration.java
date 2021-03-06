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
package org.jruyi.io.tcpserver;

import java.lang.reflect.Method;
import java.util.Map;

import org.jruyi.io.tcp.TcpChannelConf;

public final class Configuration extends TcpChannelConf {

	private static final String[] M_PROPS = { "bindAddr", "port", "backlog",
			"reuseAddr", "recvBufSize", "performancePreferences" };
	private static final Method[] c_mProps;
	private Integer m_backlog;
	private Integer m_sessionIdleTimeout;
	private Integer m_initCapacityOfChannelMap;

	static {
		c_mProps = new Method[M_PROPS.length];
		Class<Configuration> clazz = Configuration.class;
		try {
			for (int i = 0; i < M_PROPS.length; ++i)
				c_mProps[i] = clazz.getMethod(M_PROPS[i]);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static Method[] getMandatoryPropsAccessors() {
		return c_mProps;
	}

	@Override
	public void initialize(Map<String, ?> properties) {
		super.initialize(properties);

		bindAddr((String) properties.get("bindAddr"));
		backlog((Integer) properties.get("backlog"));
		sessionIdleTimeout((Integer) properties.get("sessionIdleTimeout"));
		initCapacityOfChannelMap((Integer) properties
				.get("initCapacityOfChannelMap"));
	}

	public Integer backlog() {
		return m_backlog;
	}

	public void backlog(Integer backlog) {
		m_backlog = backlog;
	}

	public String bindAddr() {
		return ip();
	}

	public void bindAddr(String bindAddr) {
		ip(bindAddr);
	}

	public Integer sessionIdleTimeout() {
		return m_sessionIdleTimeout;
	}

	public void sessionIdleTimeout(Integer sessionIdleTimeout) {
		m_sessionIdleTimeout = sessionIdleTimeout == null ? 300
				: sessionIdleTimeout;
	}

	public Integer initCapacityOfChannelMap() {
		return m_initCapacityOfChannelMap;
	}

	public void initCapacityOfChannelMap(Integer initCapacityOfChannelMap) {
		m_initCapacityOfChannelMap = initCapacityOfChannelMap == null ? 2048
				: initCapacityOfChannelMap;
	}
}
