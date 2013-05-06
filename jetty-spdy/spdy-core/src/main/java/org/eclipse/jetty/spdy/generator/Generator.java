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

package org.eclipse.jetty.spdy.generator;

import java.nio.ByteBuffer;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.spdy.CompressionFactory;
import org.eclipse.jetty.spdy.api.DataInfo;
import org.eclipse.jetty.spdy.frames.ControlFrame;
import org.eclipse.jetty.spdy.frames.ControlFrameType;

public class Generator
{
	private final ControlFrameGenerator[] generators = new ControlFrameGenerator[ControlFrameType.TYPE_NUM + 1];
    private final DataFrameGenerator dataFrameGenerator;

    public Generator(ByteBufferPool bufferPool, CompressionFactory.Compressor compressor)
    {
        HeadersBlockGenerator headersBlockGenerator = new HeadersBlockGenerator(compressor);
        generators[ControlFrameType.SYN_STREAM] = new SynStreamGenerator(bufferPool, headersBlockGenerator);
        generators[ControlFrameType.SYN_REPLY] = new SynReplyGenerator(bufferPool, headersBlockGenerator);
        generators[ControlFrameType.RST_STREAM] = new RstStreamGenerator(bufferPool);
        generators[ControlFrameType.SETTINGS] = new SettingsGenerator(bufferPool);
        generators[ControlFrameType.NOOP] = new NoOpGenerator(bufferPool);
        generators[ControlFrameType.PING] = new PingGenerator(bufferPool);
        generators[ControlFrameType.GO_AWAY] = new GoAwayGenerator(bufferPool);
        generators[ControlFrameType.HEADERS] = new HeadersGenerator(bufferPool, headersBlockGenerator);
        generators[ControlFrameType.WINDOW_UPDATE] = new WindowUpdateGenerator(bufferPool);
        generators[ControlFrameType.CREDENTIAL] = new CredentialGenerator(bufferPool);

        dataFrameGenerator = new DataFrameGenerator(bufferPool);
    }

    public ByteBuffer control(ControlFrame frame)
    {
        ControlFrameGenerator generator = generators[frame.getType()];
        return generator.generate(frame);
    }

    public ByteBuffer data(int streamId, int length, DataInfo dataInfo)
    {
        return dataFrameGenerator.generate(streamId, length, dataInfo);
    }
}
