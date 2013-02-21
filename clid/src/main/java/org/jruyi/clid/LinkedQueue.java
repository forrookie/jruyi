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

import org.jruyi.common.ListNode;

public final class LinkedQueue<E> {

	private ListNode<E> m_head;
	private volatile ListNode<E> m_tail;

	public LinkedQueue() {
		m_head = m_tail = ListNode.create();
	}

	public void put(E e) {
		ListNode<E> node = ListNode.create();
		node.set(e);
		m_tail.next(node);
		m_tail = node;
	}

	public E poll() {
		if (m_head == m_tail)
			return null;

		ListNode<E> head = m_head;
		ListNode<E> node = head.next();
		E e = node.get();
		node.set(null);
		m_head = node;
		head.close();
		return e;
	}
}
