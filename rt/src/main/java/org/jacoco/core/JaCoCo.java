/*******************************************************************************
 * Copyright (c) 2009, 2022 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core;

/**
 * Static Meta information about JaCoCo.
 */
public final class JaCoCo {

	/** Qualified version of JaCoCo core. */
	public static final String VERSION = "0.8.8";

	/** Absolute URL of the current JaCoCo home page */
	public static final String HOMEURL = "http://www.jacoco.org/jacoco";

	/** Name of the runtime package of this build */
	public static final String RUNTIMEPACKAGE = "org.jacoco.agent.rt.internal";

	private JaCoCo() {
	}

}
