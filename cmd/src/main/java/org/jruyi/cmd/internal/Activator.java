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
package org.jruyi.cmd.internal;

import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.common.Properties;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Properties properties = new Properties();

		// ruyi command
		RuyiCmd.INST.context(context);
		properties.put(CommandProcessor.COMMAND_SCOPE, "jruyi");
		properties.put(CommandProcessor.COMMAND_FUNCTION, new String[] {
				"help", "echo", "grep", "shutdown" });
		context.registerService(RuyiCmd.class.getName(), RuyiCmd.INST,
				properties);

		// bundle command
		properties.put(CommandProcessor.COMMAND_SCOPE, "bundle");
		properties.put(CommandProcessor.COMMAND_FUNCTION, new String[] {
				"list", "inspect", "start", "stop", "install", "uninstall",
				"update", "refresh" });
		context.registerService(BundleCmd.class.getName(), new BundleCmd(
				context), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		RuyiCmd.INST.context(null);
	}
}
