/**
 * Copyright 2012 JRuyi.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jruyi.clid;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.IService;
import org.jruyi.common.IntStack;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionEvent;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProducer;
import org.jruyi.me.IRoutingTable;
import org.jruyi.me.MeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service({ IEndpoint.class, IFilter.class })
@Component(name = "jruyi.clid")
@org.apache.felix.scr.annotations.Properties({
		@Property(name = MeConstants.EP_ID, value = "jruyi.clid.proc", propertyPrivate = true),
		@Property(name = IoConstants.FILTER_ID, value = "jruyi.clid.filter", propertyPrivate = true) })
@References({
		@Reference(name = "rt", referenceInterface = IRoutingTable.class, strategy = ReferenceStrategy.LOOKUP),
		@Reference(name = "tsf", referenceInterface = ComponentFactory.class, target = "(component.name=jruyi.io.tcpserver.factory)", strategy = ReferenceStrategy.LOOKUP) })
public final class CliProcessor implements IConsumer, IEndpoint, IFilter {

	private static final Logger c_logger = LoggerFactory
			.getLogger(CliProcessor.class);

	private static final String BRANDING_URL = "jruyi.clid.branding.url";
	private static final String BINDADDR = "jruyi.clid.bindAddr";
	private static final String PORT = "jruyi.clid.port";
	private static final String SESSIONIDLETIMEOUT = "jruyi.clid.sessionIdleTimeout";

	private static final String NOTIFY_SESSION_EVENTS = "notifySessionEvents";
	private static final String SRC_ID = "jruyi.clid.tcpsvr";
	private static final String THIS_ID = "jruyi.clid.proc";
	private static final String CLID_OUT = "jruyi.clid.out";
	private static final String WELCOME = "welcome";
	private static final String PROMPT = "prompt";

	private static final String P_BIND_ADDR = "bindAddr";
	private static final String P_PORT = "port";
	@Property(intValue = 300)
	private static final String P_SESSION_IDLE_TIMEOUT = "sessionIdleTimeout";
	@Property(boolValue = false)
	private static final String P_DEBUG = "debug";

	@Reference(name = "cp", bind = "setCommandProcessor", unbind = "unsetCommandProcessor")
	private CommandProcessor m_cp;

	private BundleContext m_context;
	private ComponentInstance m_tcpServer;
	private Properties m_conf;
	private ConcurrentHashMap<Long, CommandSession> m_css;
	private byte[] m_welcome;
	private String m_prompt;
	private IProducer m_producer;

	@Override
	public int tellBoundary(ISession session, IBuffer in) {
		int b;
		int n = 0;
		try {
			do {
				b = in.read();
				n = (n << 7) | (b & 0x7F);
			} while (b < 0);
			return n + in.position();
		} catch (BufferUnderflowException e) {
			in.rewind();
			return IFilter.E_UNDERFLOW;
		}
	}

	@Override
	public boolean onMsgArrive(ISession session, Object msg,
			IFilterOutput output) {
		((IBuffer) msg).compact();
		output.add(msg);
		return true;
	}

	@Override
	public boolean onMsgDepart(ISession session, Object msg,
			IFilterOutput output) {
		@SuppressWarnings("unchecked")
		LinkedQueue<IBuffer> queue = (LinkedQueue<IBuffer>) msg;
		IBuffer data = queue.poll();
		if (data == null)
			return false;

		IBuffer out = data;
		while ((data = queue.poll()) != null) {
			data.drainTo(out);
			data.close();
		}

		output.add(out);
		return true;
	}

	@Override
	public void producer(IProducer producer) {
		m_producer = producer;
	}

	@Override
	public IConsumer consumer() {
		return this;
	}

	@Override
	public void onMessage(IMessage message) {
		Object sessionEvent = message
				.removeProperty(IoConstants.MP_SESSION_EVENT);
		if (sessionEvent == SessionEvent.OPENED) {
			onSessionOpened(message);
			return;
		}

		if (sessionEvent == SessionEvent.CLOSED) {
			onSessionClosed(message);
			return;
		}

		IBuffer buffer = (IBuffer) message.detach();
		String cmdline = buffer.remaining() > 0 ? buffer.read(Codec.utf_8())
				: null;
		buffer.drain();

		ISession session = (ISession) message
				.getProperty(IoConstants.MP_PASSIVE_SESSION);
		CommandSession cs = m_css.get(session.id());

		BufferStream bs = (BufferStream) session.get(CLID_OUT);
		bs.message(message);
		bs.buffer(buffer);
		if (cmdline != null) {
			cmdline = filterProps(cmdline, cs, m_context);
			try {
				cs.execute(cmdline);
			} catch (Throwable t) {
				c_logger.warn(cmdline, t);
				String msg = t.getMessage();
				if (msg == null)
					msg = t.toString();
				bs.write(msg);
			}
		}
		bs.writeOut((String) cs.get(PROMPT));
	}

	protected void setCommandProcessor(CommandProcessor cp) {
		m_cp = cp;
	}

	protected void unsetCommandProcessor(CommandProcessor cp) {
		m_cp = null;
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		IService inst = (IService) m_tcpServer.getInstance();
		inst.update(normalizeConf(properties));
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {

		BundleContext bundleContext = context.getBundleContext();
		loadBrandingInfo(context.getBundleContext().getProperty(BRANDING_URL),
				bundleContext);

		Properties conf = normalizeConf(properties);
		if (conf.get(P_BIND_ADDR) == null) {
			String bindAddr = context.getBundleContext().getProperty(BINDADDR);
			if (bindAddr == null || (bindAddr = bindAddr.trim()).length() < 1)
				bindAddr = "localhost";
			conf.put(P_BIND_ADDR, bindAddr);
		}

		if (conf.get(P_PORT) == null) {
			String v = context.getBundleContext().getProperty(PORT);
			Integer port = v == null ? 6060 : Integer.valueOf(v);
			conf.put(P_PORT, port);
		}

		if (conf.get(P_SESSION_IDLE_TIMEOUT) == null) {
			String v = context.getBundleContext().getProperty(
					SESSIONIDLETIMEOUT);
			Integer sessionIdleTimeout = v == null ? 300 : Integer.valueOf(v);
			conf.put(P_SESSION_IDLE_TIMEOUT, sessionIdleTimeout);
		}

		ComponentFactory factory = (ComponentFactory) context
				.locateService("tsf");
		m_tcpServer = factory.newInstance(conf);

		IRoutingTable rt = (IRoutingTable) context.locateService("rt");
		rt.getRouteSet(SRC_ID).setRoute(THIS_ID);
		rt.getRouteSet(THIS_ID).setRoute(SRC_ID);

		m_css = new ConcurrentHashMap<Long, CommandSession>(10);
		m_context = bundleContext;
	}

	protected void deactivate(ComponentContext context) {
		m_context = null;

		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put(NOTIFY_SESSION_EVENTS,
				new String[] { SessionEvent.OPENED.name() });

		try {
			modified(properties);
		} catch (Exception e) {
			// ignore
		}

		m_tcpServer.dispose();
		m_tcpServer = null;
		m_conf = null;

		IRoutingTable rt = (IRoutingTable) context.locateService("rt");
		rt.getRouteSet(SRC_ID).clear();
		rt.getRouteSet(THIS_ID).clear();

		Collection<CommandSession> css = m_css.values();
		m_css = null;
		for (CommandSession cs : css)
			cs.close();
	}

	private Properties normalizeConf(Map<String, ?> properties) {
		Properties conf = m_conf;
		if (conf == null) {
			conf = new Properties(properties);
			m_conf = conf;
		} else
			conf.putAll(properties);

		String[] filters = (Boolean) conf.get(P_DEBUG) ? new String[] {
				"jruyi.clid.filter", "jruyi.io.msglog.filter" }
				: new String[] { "jruyi.clid.filter" };
		conf.put(MeConstants.EP_ID, SRC_ID);
		conf.put("initCapacityOfChannelMap", 8);
		conf.put("filters", filters);
		conf.put("reuseAddr", Boolean.TRUE);
		if (!conf.containsKey(NOTIFY_SESSION_EVENTS))
			conf.put(NOTIFY_SESSION_EVENTS,
					new String[] { SessionEvent.OPENED.name(),
							SessionEvent.CLOSED.name() });
		return conf;
	}

	private void onSessionOpened(IMessage message) {
		ISession session = (ISession) message
				.getProperty(IoConstants.MP_PASSIVE_SESSION);

		BufferStream bs = new BufferStream(m_producer);
		PrintStream out = new PrintStream(bs);
		CommandSession cs = m_cp.createSession(null, out, out);
		cs.put(PROMPT, m_prompt);

		session.put(CLID_OUT, bs);
		m_css.put(session.id(), cs);

		bs.buffer(session.createBuffer());
		bs.message(message);
		bs.write(m_welcome);
		bs.writeOut(m_prompt);
	}

	private void onSessionClosed(IMessage message) {
		ISession session = (ISession) message
				.getProperty(IoConstants.MP_PASSIVE_SESSION);
		CommandSession cs = m_css.remove(session.id());
		if (cs != null)
			cs.close();
	}

	private void loadBrandingInfo(String url, BundleContext context)
			throws Exception {
		java.util.Properties branding = loadBrandingProps(CliProcessor.class
				.getResourceAsStream("branding.properties"));
		if (url != null)
			branding.putAll(loadBrandingProps(new URL(url).openStream()));

		m_welcome = CharsetCodec.get(CharsetCodec.UTF_8).toBytes(
				StrUtil.filterProps(branding.getProperty(WELCOME), context));
		m_prompt = StrUtil.filterProps(branding.getProperty(PROMPT), context);
	}

	private static java.util.Properties loadBrandingProps(InputStream in)
			throws Exception {
		java.util.Properties props = new java.util.Properties();
		try {
			props.load(new BufferedInputStream(in));
		} finally {
			in.close();
		}
		return props;
	}

	private static String filterProps(String target, CommandSession cs,
			BundleContext context) {
		if (target.length() < 2)
			return target;

		StringBuilder builder = StringBuilder.get();
		IntStack stack = IntStack.get();
		String propValue = null;
		int j = target.length();
		for (int i = 0; i < j; ++i) {
			char c = target.charAt(i);
			switch (c) {
			case '\\':
				if (++i < j)
					c = target.charAt(i);
				break;
			case '$':
				builder.append(c);
				if (++i < j && (c = target.charAt(i)) == '{')
					stack.push(builder.length() - 1);
				break;
			case '}':
				if (!stack.isEmpty()) {
					int index = stack.pop();
					propValue = getPropValue(builder.substring(index + 2), cs,
							context);
					if (propValue != null) {
						builder.setLength(index);
						builder.append(propValue);
						continue;
					}
				}
			}

			builder.append(c);
		}
		stack.close();

		if (propValue != null || builder.length() != j)
			target = builder.toString();

		builder.close();

		return target;
	}

	private static String getPropValue(String name, CommandSession cs,
			BundleContext context) {
		Object value = cs.get(name);
		if (value != null)
			return value.toString();

		return context.getProperty(name);
	}
}
