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

package org.eclipse.jetty.websocket.server.examples.echo;

import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Example Socket for echoing back Big data using the Annotation techniques along with stateless techniques.
 */
@WebSocket(maxMessageSize = 64 * 1024)
public class BigEchoSocket
{
    @OnWebSocketMessage
    public void onBinary(Session session, byte buf[], int offset, int length)
    {
        if (session.isOpen())
        {
            return;
        }
        session.getRemote().sendBytesByFuture(ByteBuffer.wrap(buf,offset,length));
    }

    @OnWebSocketMessage
    public void onText(Session session, String message)
    {
        if (session.isOpen())
        {
            return;
        }
        session.getRemote().sendStringByFuture(message);
    }
}
