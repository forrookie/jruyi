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
package org.jruyi.common.internal;

import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ThreadLocalCache.IFactory;

public final class ThreadLocalCacheProvider implements IFactory {

	private static final ThreadLocalCacheProvider c_inst = new ThreadLocalCacheProvider();

	@Override
	public <E> IThreadLocalCache<E> softArrayCache() {
		return new SoftThreadLocalArrayCache<E>();
	}

	@Override
	public <E> IThreadLocalCache<E> softArrayCache(int initialCapacity) {
		return new SoftThreadLocalArrayCache<E>(initialCapacity);
	}

	@Override
	public <E> IThreadLocalCache<E> softLinkedCache() {
		return new SoftThreadLocalLinkedCache<E>();
	}

	@Override
	public <E> IThreadLocalCache<E> weakArrayCache() {
		return new WeakThreadLocalArrayCache<E>();
	}

	@Override
	public <E> IThreadLocalCache<E> weakArrayCache(int initialCapacity) {
		return new WeakThreadLocalArrayCache<E>(initialCapacity);
	}

	@Override
	public <E> IThreadLocalCache<E> weakLinkedCache() {
		return new WeakThreadLocalLinkedCache<E>();
	}

	private ThreadLocalCacheProvider() {
	}

	public static ThreadLocalCacheProvider getInstance() {
		return c_inst;
	}

	public IFactory getFactory() {
		return this;
	}
}
