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
 * An output holder for a filter to pass the output to the next filter as an
 * input.
 */
public interface IFilterOutput {

	/**
	 * Add the specified {@code out} to the output queue whose elements are to
	 * be passed to the next filter one by one.
	 * 
	 * @param out
	 *            the output
	 * @exception NullPointerException
	 *                if the specified {@code out} is null
	 */
	public void add(Object out);
}
