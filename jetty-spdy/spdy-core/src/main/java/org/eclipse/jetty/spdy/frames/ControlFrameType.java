//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.spdy.frames;

public class ControlFrameType {
    public static final short SYN_STREAM = 1;
    public static final short SYN_REPLY = 2;
    public static final short RST_STREAM = 3;
    public static final short SETTINGS = 4;
    public static final short NOOP = 5;
    public static final short PING = 6;
    public static final short GO_AWAY = 7;
    public static final short HEADERS = 8;
    public static final short WINDOW_UPDATE = 9;
    public static final short CREDENTIAL = 10;

	// The number of control frames in SPDY
	public static final short TYPE_NUM = 10;
}
