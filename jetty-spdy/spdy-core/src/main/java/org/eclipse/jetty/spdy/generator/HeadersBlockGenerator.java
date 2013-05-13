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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

import org.eclipse.jetty.spdy.CompressionDictionary;
import org.eclipse.jetty.spdy.CompressionFactory;
import org.eclipse.jetty.spdy.api.SPDY;
import org.eclipse.jetty.util.Fields;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HeadersBlockGenerator
{
    private final CompressionFactory.Compressor compressor;
    private boolean needsDictionary = true;

	private final Logger LOG = Log.getLogger(HeadersBlockGenerator.class);

    public HeadersBlockGenerator(CompressionFactory.Compressor compressor)
    {
        this.compressor = compressor;
    }

    public ByteBuffer generate(short version, Fields headers)
    {
		LOG.info("[minglin] - HeadersBlockGenerator.generate(...) starts"); 
        // TODO: ByteArrayOutputStream is quite inefficient, but grows on demand; optimize using ByteBuffer ?
        Charset iso1 = Charset.forName("ISO-8859-1");
		
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(headers.size() * 64);
        writeCount(version, buffer, headers.size());
        for (Fields.Field header : headers)
        {
            String name = header.name().toLowerCase(Locale.ENGLISH);
            byte[] nameBytes = name.getBytes(iso1);
            writeNameLength(version, buffer, nameBytes.length);
            buffer.write(nameBytes, 0, nameBytes.length);

            // Most common path first
            String value = header.value();
            byte[] valueBytes = value.getBytes(iso1);
            if (header.hasMultipleValues())
            {
                String[] values = header.values();
                for (int i = 1; i < values.length; ++i)
                {
                    byte[] moreValueBytes = values[i].getBytes(iso1);
                    byte[] newValueBytes = new byte[valueBytes.length + 1 + moreValueBytes.length];
                    System.arraycopy(valueBytes, 0, newValueBytes, 0, valueBytes.length);
                    newValueBytes[valueBytes.length] = 0;
                    System.arraycopy(moreValueBytes, 0, newValueBytes, valueBytes.length + 1, moreValueBytes.length);
                    valueBytes = newValueBytes;
                }
            }

            writeValueLength(version, buffer, valueBytes.length);
            buffer.write(valueBytes, 0, valueBytes.length);
        }

		LOG.info("[minglin] - HeadersBlockGenerator.generate(...) finishes"); 

        return compress(version, buffer.toByteArray());
		/*
		int index = 0;
		int allocSize;
		int headersBlockSize = headers.size();
		if(headersBlockSize == 0)
			allocSize = 4;
		else
			allocSize = headersBlockSize * 64;
		if(headersBlockSize != 0)
			System.out.println("headersBlockSize: " + headersBlockSize);
		ByteBuffer buffer = ByteBuffer.allocate(allocSize);
		writeCount(version, buffer, headersBlockSize);
		//System.out.println("position: " + buffer.position());
		if(version == SPDY.V3)
			index += 4;
		else
			index += 2;
		for(Fields.Field header: headers) {
			String name = header.name().toLowerCase(Locale.ENGLISH);
			System.out.println("name: " + name);
			byte[] nameBytes = name.getBytes(iso1);
			// write "Length of name"
			writeCount(version, buffer, nameBytes.length);
			//System.out.println("position: " + buffer.position());
			if(version == SPDY.V3)
				index += 4;
			else
				index += 2;
			// write "name"
			buffer.put(nameBytes);
			// record the index of the "Length of value"
			index += nameBytes.length;
			String singleValue = header.value();
			System.out.println("singleValue: " + singleValue);
			byte[] singleValueBytes = singleValue.getBytes(iso1);
			// write "Length of value" if only a single value
			writeCount(version, buffer, singleValueBytes.length);
			//System.out.println("position: " + buffer.position());

			// If header "name" has more than one value, write values, then backpatch "Length of value".
			// Otherwise write "Length of value" directly.
            if (header.hasMultipleValues()) {
				int valuesLength = 0;
				String[] values = header.values();
				for(String value: values) {
					System.out.println("value: " + value);
					byte[] valueBytes = value.getBytes(iso1);
					buffer.put(valueBytes);
	
					valuesLength += valueBytes.length;
				}
				
				writeCount(version, buffer, index, valuesLength);
			}else {
				buffer.put(singleValueBytes);
				//System.out.println("position: " + buffer.position());
			}
		}
		
		LOG.info("[minglin] - HeadersBlockGenerator.generate(...) finishes"); 

        return compress(version, buffer.array());
		*/
    }

    private ByteBuffer compress(short version, byte[] bytes)
    {
		LOG.info("[minglin] - HeadersBlockGenerator.compress(...) starts"); 

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytes.length);

        // The headers compression context is per-session, so we need to synchronize
        synchronized (compressor)
        {
            if (needsDictionary)
            {
                compressor.setDictionary(CompressionDictionary.get(version));
                needsDictionary = false;
            }

            compressor.setInput(bytes);

            // Compressed bytes may be bigger than input bytes, so we need to loop and accumulate them
            // Beware that the minimum amount of bytes generated by the compressor is few bytes, so we
            // need to use an output buffer that is big enough to exit the compress loop
            buffer.reset();
            int compressed;
            byte[] output = new byte[Math.max(256, bytes.length)];
            while (true)
            {
                // SPDY uses the SYNC_FLUSH mode
                compressed = compressor.compress(output);
                buffer.write(output, 0, compressed);
                if (compressed < output.length)
                    break;
            }
        }

		LOG.info("[minglin] - HeadersBlockGenerator.compress(...) finishes"); 

        return ByteBuffer.wrap(buffer.toByteArray());
    }

    private void writeCount(short version, ByteBuffer buffer, int value)
    {
		//System.out.println("writeCount - value: " + value);

        switch (version)
        {
            case SPDY.V2:
            {
				//System.out.println("position: " + buffer.position());
				buffer.putShort((short)value);
				System.out.println("position: " + buffer.position());
				//System.out.println(buffer.getShort(buffer.position()-2));
				/*
                buffer.put((byte)((value & 0xFF_00) >>> 8));
                buffer.put((byte)(value & 0x00_FF));
				*/

                break;
            }
            case SPDY.V3:
            {
				//System.out.println("position: " + buffer.position());
				buffer.putInt(value);
				//System.out.println("position: " + buffer.position());
				
				/*
				byte[] size = {(byte)((value & 0xFF_00_00_00) >>> 24), (byte)((value & 0x00_FF_00_00) >>> 16), 
					(byte)((value & 0x00_00_FF_00) >>> 8), (byte)(value & 0x00_00_00_FF)};
				buffer.put(size);
				*/

				/*
                buffer.put((value & 0xFF_00_00_00) >>> 24);
                buffer.put((value & 0x00_FF_00_00) >>> 16);
                buffer.put((value & 0x00_00_FF_00) >>> 8);
                buffer.put(value & 0x00_00_00_FF);
				*/
                break;
            }
            default:
            {
                // Here the version is trusted to be correct; if it's not
                // then it's a bug rather than an application error
                throw new IllegalStateException();
            }
        }
    }

    private void writeCount(short version, ByteBuffer buffer, int index, int value)
    {
		//System.out.println("index: " + index + ", value: " + value);
        switch (version)
        {
            case SPDY.V2:
            {
				buffer.putShort(index, (short)value);
				/*
                buffer.put(index, (byte)((value & 0xFF_00) >>> 8));
                buffer.put(index+1, (byte)(value & 0x00_FF));
				*/
                break;
            }
            case SPDY.V3:
            {
				buffer.putInt(index, value);
				/*
                buffer.put(index, (byte)((value & 0xFF_00_00_00) >>> 24));
                buffer.put(index+1, (byte)((value & 0x00_FF_00_00) >>> 16));
                buffer.put(index+2, (byte)((value & 0x00_00_FF_00) >>> 8));
                buffer.put(index+3, (byte)(value & 0x00_00_00_FF));
				*/
                break;
            }
            default:
            {
                // Here the version is trusted to be correct; if it's not
                // then it's a bug rather than an application error
                throw new IllegalStateException();
            }
        }
    }

    private void writeCount(short version, ByteArrayOutputStream buffer, int value)
    {
		//System.out.println("writeCount - value: " + value);

        switch (version)
        {
            case SPDY.V2:
            {
				//System.out.println("position: " + buffer.size());
                buffer.write((value & 0xFF_00) >>> 8);
                buffer.write(value & 0x00_FF);
				//System.out.println("position: " + buffer.size());

                break;
            }
            case SPDY.V3:
            {
				//System.out.println("position: " + buffer.size());
                buffer.write((value & 0xFF_00_00_00) >>> 24);
                buffer.write((value & 0x00_FF_00_00) >>> 16);
                buffer.write((value & 0x00_00_FF_00) >>> 8);
                buffer.write(value & 0x00_00_00_FF);
				//System.out.println("position: " + buffer.size());

                break;
            }
            default:
            {
                // Here the version is trusted to be correct; if it's not
                // then it's a bug rather than an application error
                throw new IllegalStateException();
            }
        }
    }

    private void writeNameLength(short version, ByteArrayOutputStream buffer, int length)
    {
        writeCount(version, buffer, length);
    }

    private void writeValueLength(short version, ByteArrayOutputStream buffer, int length)
    {
        writeCount(version, buffer, length);
    }
}
