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
package org.jruyi.me.mq;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.jruyi.common.BiListNode;
import org.jruyi.common.IService;
import org.jruyi.common.IServiceHolderManager;
import org.jruyi.common.ServiceHolderManager;
import org.jruyi.common.StrUtil;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IPostHandler;
import org.jruyi.me.IPreHandler;
import org.jruyi.me.IProcessor;
import org.jruyi.me.MeConstants;
import org.jruyi.me.route.IRouter;
import org.jruyi.me.route.IRouterManager;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutEvent;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.me.mq", createPid = false)
@References({
		@Reference(name = "endpoint", referenceInterface = IEndpoint.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "setEndpoint", unbind = "unsetEndpoint", updated = "updatedEndpoint"),
		@Reference(name = "processor", referenceInterface = IProcessor.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "setProcessor", unbind = "unsetProcessor", updated = "updatedProcessor") })
public final class MessageQueue implements ITimeoutListener {

	static final PreHandlerDelegator[] EMPTY_PREHANDLERS = new PreHandlerDelegator[0];
	static final PostHandlerDelegator[] EMPTY_POSTHANDLERS = new PostHandlerDelegator[0];
	private static final Logger c_logger = LoggerFactory
			.getLogger(MessageQueue.class);

	@Property(intValue = 10)
	private static final String P_MSG_TIMEOUT = "msgTimeout";

	private final ConcurrentHashMap<String, Endpoint> m_endpoints;
	private ConcurrentHashMap<String, BiListNode<MsgNotifier>> m_nodes;
	private final ReentrantLock m_lock;
	private final HashMap<Object, Endpoint> m_refEps;
	private IServiceHolderManager<IPreHandler> m_preHandlerManager;
	private IServiceHolderManager<IPostHandler> m_postHandlerManager;

	@Reference(name = "rm", bind = "setRouterManager", unbind = "unsetRouterManager")
	private IRouterManager m_rm;

	@Reference(name = "worker", bind = "setWorker", unbind = "unsetWorker")
	private IWorker m_worker;

	@Reference(name = "ta", bind = "setTimeoutAdmin", unbind = "unsetTimeoutAdmin")
	private ITimeoutAdmin m_ta;

	private ComponentContext m_context;
	private int m_msgTimeout = 10;

	static final class MsgNotifier {

		private Message m_msg;
		private ITimeoutNotifier m_notifier;

		MsgNotifier(Message msg, ITimeoutNotifier notifier) {
			m_msg = msg;
			m_notifier = notifier;
		}

		void msg(Message msg) {
			m_msg = msg;
		}

		Message msg() {
			return m_msg;
		}

		void notifier(ITimeoutNotifier notifier) {
			m_notifier = notifier;
		}

		ITimeoutNotifier notifier() {
			return m_notifier;
		}
	}

	public MessageQueue() {
		m_endpoints = new ConcurrentHashMap<String, Endpoint>();
		m_nodes = new ConcurrentHashMap<String, BiListNode<MsgNotifier>>();
		m_refEps = new HashMap<Object, Endpoint>();
		m_lock = new ReentrantLock();
	}

	@Override
	public void onTimeout(ITimeoutEvent event) {
		@SuppressWarnings("unchecked")
		BiListNode<MsgNotifier> node = (BiListNode<MsgNotifier>) event
				.getSubject();
		Message msg = removeNode(node);
		c_logger.warn(StrUtil.buildString("Message timed out:", msg));
		msg.close();
	}

	protected void setRouterManager(IRouterManager rm) {
		m_rm = rm;
	}

	protected void unsetRouterManager(IRouterManager rm) {
		m_rm = null;
	}

	protected void setWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unsetWorker(IWorker worker) {
		m_worker = null;
	}

	protected void setTimeoutAdmin(ITimeoutAdmin ta) {
		m_ta = ta;
	}

	protected void unsetTimeoutAdmin(ITimeoutAdmin ta) {
		m_ta = null;
	}

	protected void setEndpoint(IEndpoint original, Map<String, ?> props)
			throws Exception {
		String id = getId(props);
		if (id == null)
			return;

		Map<String, Endpoint> endpoints = m_endpoints;
		Endpoint endpoint = endpoints.get(id);
		if (endpoint != null) {
			c_logger.error(StrUtil.buildString(endpoint,
					" has already been registered"));
			return;
		}

		IConsumer consumer = original.consumer();
		endpoint = consumer == null ? new Endpoint(id, this) : new Consumer(id,
				this, consumer);
		setHandlers(endpoint, props);

		original.producer(endpoint);
		if (original instanceof IService)
			((IService) original).start();

		m_refEps.put(original, endpoint);
		endpoints.put(id, endpoint);
		wakeMsgs(endpoint);
	}

	protected void unsetEndpoint(IEndpoint original) {
		Endpoint endpoint = m_refEps.remove(original);
		if (endpoint != null) {
			m_endpoints.remove(endpoint.id());

			if (original instanceof IService)
				((IService) original).stop();
			endpoint.closeProducer();
		}
	}

	protected void updatedEndpoint(IEndpoint original, Map<String, ?> props)
			throws Exception {
		Endpoint endpoint = m_refEps.get(original);
		if (endpoint != null) {
			updated(endpoint, original, props);
			return;
		}

		setEndpoint(original, props);
	}

	protected void setProcessor(ServiceReference<IProcessor> reference) {
		String id = getId(reference);
		if (id == null)
			return;

		Map<String, Endpoint> endpoints = m_endpoints;
		Endpoint endpoint = endpoints.get(id);
		if (endpoint != null) {
			c_logger.error(StrUtil.buildString(endpoint,
					" has already been registered"));
			return;
		}

		endpoint = new Processor(id, this, reference);
		setHandlers(endpoint, reference);

		m_refEps.put(reference, endpoint);
		endpoints.put(id, endpoint);
		wakeMsgs(endpoint);
	}

	protected void unsetProcessor(ServiceReference<IProcessor> reference) {
		Endpoint endpoint = m_refEps.remove(reference);
		if (endpoint != null) {
			m_endpoints.remove(endpoint.id());
			endpoint.closeProducer();
		}
	}

	protected void updatedProcessor(ServiceReference<IProcessor> reference) {
		Endpoint endpoint = m_refEps.get(reference);
		if (endpoint != null) {
			updated(endpoint, reference);
			return;
		}

		setProcessor(reference);
	}

	@Modified
	protected void modified(Map<String, ?> properties) {
		m_msgTimeout = (Integer) properties.get(P_MSG_TIMEOUT);
	}

	protected void activate(ComponentContext context, Map<String, ?> properties) {
		m_context = context;

		BundleContext bc = context.getBundleContext();
		IServiceHolderManager<IPreHandler> preHandlerManager = ServiceHolderManager
				.newInstance(bc, IPreHandler.class, MeConstants.HANDLER_ID);
		IServiceHolderManager<IPostHandler> postHandlerManager = ServiceHolderManager
				.newInstance(bc, IPostHandler.class, MeConstants.HANDLER_ID);

		preHandlerManager.open();
		postHandlerManager.open();

		m_preHandlerManager = preHandlerManager;
		m_postHandlerManager = postHandlerManager;

		modified(properties);

		c_logger.info("MessageQueue activated");
	}

	@SuppressWarnings("resource")
	protected void deactivate() {
		Collection<BiListNode<MsgNotifier>> nodes = m_nodes.values();
		for (BiListNode<MsgNotifier> head : nodes) {
			BiListNode<MsgNotifier> node = head.next();
			while (node != head) {
				node.get().notifier().close();
				node = node.next();
			}
		}
		m_nodes.clear();

		m_postHandlerManager.close();
		m_preHandlerManager.close();

		m_postHandlerManager = null;
		m_preHandlerManager = null;

		c_logger.info("MessageQueue deactivated");
	}

	IProcessor locateProcessor(ServiceReference<IProcessor> reference) {
		return m_context.getBundleContext().getService(reference);
	}

	void dispatch(Message message) {
		if (message.isToNull()) {
			message.close();
			return;
		}

		String dst = message.to();
		try {
			Endpoint mqProxy = m_endpoints.get(dst);
			if (mqProxy != null) {
				message.setEndpoint(mqProxy);
				m_worker.run(message);
			} else {
				BiListNode<MsgNotifier> node = schedule(message);
				mqProxy = m_endpoints.get(dst);
				if (mqProxy != null && node.get().notifier().cancel()) {
					removeNode(node);
					message.setEndpoint(mqProxy);
					m_worker.run(message);
				}
			}
		} catch (Exception e) {
			c_logger.error(StrUtil.buildString("Endpoint[", dst,
					"] failed to consume: ", message), e);
			message.close();
		}
	}

	IRouter getRouter(String id) {
		return m_rm.getRouter(id);
	}

	IPreHandler[] getPreHandlers(String[] preHandlerIds) {
		int n = preHandlerIds.length;
		if (n < 1)
			return EMPTY_PREHANDLERS;

		IServiceHolderManager<IPreHandler> manager = m_preHandlerManager;
		IPreHandler[] preHandlers = new IPreHandler[n];
		for (int i = 0; i < n; ++i)
			preHandlers[i] = new PreHandlerDelegator(
					manager.getServiceHolder(preHandlerIds[i]));

		return preHandlers;
	}

	void ungetPreHandlers(String[] preHandlerIds) {
		IServiceHolderManager<IPreHandler> manager = m_preHandlerManager;
		for (String preHandlerId : preHandlerIds)
			manager.ungetServiceHolder(preHandlerId);
	}

	IPostHandler[] getPostHandlers(String[] postHandlerIds) {
		int n = postHandlerIds.length;
		if (n < 1)
			return EMPTY_POSTHANDLERS;

		IServiceHolderManager<IPostHandler> manager = m_postHandlerManager;
		IPostHandler[] postHandlers = new IPostHandler[n];
		for (int i = 0; i < n; ++i)
			postHandlers[i] = new PostHandlerDelegator(
					manager.getServiceHolder(postHandlerIds[i]));

		return postHandlers;
	}

	void ungetPostHandlers(String[] postHandlerIds) {
		IServiceHolderManager<IPostHandler> manager = m_postHandlerManager;
		for (String postHandlerId : postHandlerIds)
			manager.ungetServiceHolder(postHandlerId);
	}

	private String getId(Map<String, ?> props) {
		String id = (String) props.get(MeConstants.EP_ID);
		if (id == null) {
			c_logger.error(StrUtil.buildString(
					props.get(Constants.SERVICE_PID), ": Missing "
							+ MeConstants.EP_ID));
			return null;
		}

		id = id.trim();
		if (id.length() < 1) {
			c_logger.error(StrUtil.buildString(
					props.get(Constants.SERVICE_PID), ": Empty "
							+ MeConstants.EP_ID));
			return null;
		}

		return id;
	}

	private String getId(ServiceReference<?> reference) {
		String id = (String) reference.getProperty(MeConstants.EP_ID);
		if (id == null) {
			c_logger.error(StrUtil.buildString(
					reference.getProperty(Constants.SERVICE_PID), ": Missing "
							+ MeConstants.EP_ID));
			return null;
		}

		id = id.trim();
		if (id.length() < 1) {
			c_logger.error(StrUtil.buildString(
					reference.getProperty(Constants.SERVICE_PID), ": Empty "
							+ MeConstants.EP_ID));
			return null;
		}

		return id.intern();
	}

	private void setHandlers(Endpoint endpoint, Map<String, ?> props) {
		String[] v = (String[]) props.get(MeConstants.EP_PREHANDLERS);
		if (v != null)
			endpoint.setPreHandlers(v);
		else
			endpoint.setPreHandlers(StrUtil.getEmptyStringArray());

		v = (String[]) props.get(MeConstants.EP_POSTHANDLERS);
		if (v != null)
			endpoint.setPostHandlers(v);
		else
			endpoint.setPostHandlers(StrUtil.getEmptyStringArray());
	}

	private void setHandlers(Endpoint endpoint, ServiceReference<?> reference) {
		String[] v = (String[]) reference
				.getProperty(MeConstants.EP_PREHANDLERS);
		if (v != null)
			endpoint.setPreHandlers(v);
		else
			endpoint.setPreHandlers(StrUtil.getEmptyStringArray());

		v = (String[]) reference.getProperty(MeConstants.EP_POSTHANDLERS);
		if (v != null)
			endpoint.setPostHandlers(v);
		else
			endpoint.setPostHandlers(StrUtil.getEmptyStringArray());
	}

	private void unregister(Endpoint endpoint, Object ref) {
		endpoint.closeProducer();
		m_endpoints.remove(endpoint.id());
		m_refEps.remove(ref);
	}

	private void updated(Endpoint endpoint, IEndpoint original,
			Map<String, ?> props) {
		setHandlers(endpoint, props);

		String id = getId(props);
		if (id == null) {
			unregister(endpoint, original);
			c_logger.error(StrUtil.buildString(endpoint,
					" is unregistered: Illegal " + MeConstants.EP_ID));
		} else if (!endpoint.id().equals(id)) {
			if (m_endpoints.containsKey(id)) {
				unregister(endpoint, original);
				c_logger.error(StrUtil
						.buildString(endpoint, " is unregistered: Existing "
								+ MeConstants.EP_ID + "=", id));
			} else {
				String oldId = endpoint.id();
				endpoint.id(id);
				m_endpoints.put(id, endpoint);
				m_endpoints.remove(oldId);

				c_logger.info(StrUtil.buildString(endpoint,
						" is reregistered from ", oldId));
			}
		}
	}

	private void updated(Endpoint endpoint, ServiceReference<?> reference) {
		setHandlers(endpoint, reference);

		String id = getId(reference);
		if (id == null) {
			unregister(endpoint, reference);
			c_logger.error(StrUtil.buildString(endpoint,
					" is unregistered: Illegal " + MeConstants.EP_ID));
		} else if (!endpoint.id().equals(id)) {
			if (m_endpoints.containsKey(id)) {
				unregister(endpoint, reference);
				c_logger.error(StrUtil
						.buildString(endpoint, " is unregistered: Existing "
								+ MeConstants.EP_ID + "=", id));
			} else {
				String oldId = endpoint.id();
				endpoint.id(id);
				m_endpoints.put(id, endpoint);
				m_endpoints.remove(oldId);

				c_logger.info(StrUtil.buildString(endpoint,
						" is reregistered from ", oldId));
			}
		}
	}

	private BiListNode<MsgNotifier> schedule(Message message) {
		BiListNode<MsgNotifier> node = BiListNode.create();
		ITimeoutNotifier notifier = m_ta.createNotifier(node);
		notifier.setListener(this);
		MsgNotifier mn = new MsgNotifier(message, notifier);
		node.set(mn);

		BiListNode<MsgNotifier> head = getHead(message.to());
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			BiListNode<MsgNotifier> previous = head.previous();
			previous.next(node);
			node.previous(previous);
			node.next(head);
			head.previous(node);
		} finally {
			lock.unlock();
		}

		notifier.schedule(m_msgTimeout);
		return node;
	}

	private BiListNode<MsgNotifier> getHead(String endpointId) {
		BiListNode<MsgNotifier> head = m_nodes.get(endpointId);
		if (head == null) {
			head = BiListNode.<MsgNotifier> create();
			head.previous(head);
			head.next(head);
			BiListNode<MsgNotifier> node = m_nodes
					.putIfAbsent(endpointId, head);
			if (node != null) {
				head.close();
				head = node;
			}
		}

		return head;
	}

	private Message removeNode(BiListNode<MsgNotifier> node) {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			final BiListNode<MsgNotifier> previous = node.previous();
			final BiListNode<MsgNotifier> next = node.next();
			previous.next(next);
			next.previous(previous);
		} finally {
			lock.unlock();
		}
		Message msg = node.get().msg();
		node.close();
		return msg;
	}

	private void wakeMsgs(Endpoint endpoint) {
		final BiListNode<MsgNotifier> head = m_nodes.get(endpoint.id());
		if (head == null)
			return;

		final IWorker worker = m_worker;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			BiListNode<MsgNotifier> node = head.next();
			while (node != head) {
				if (!node.get().notifier().cancel())
					continue;

				final BiListNode<MsgNotifier> previous = node.previous();
				final BiListNode<MsgNotifier> next = node.next();
				previous.next(next);
				next.previous(previous);

				Message msg = node.get().msg();
				msg.setEndpoint(endpoint);
				worker.run(msg);

				node.close();
				node = next;
			}
		} finally {
			lock.unlock();
		}
	}
}
