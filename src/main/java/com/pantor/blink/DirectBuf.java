// Copyright (c) 2013, Pantor Engineering AB
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
//  * Redistributions in binary form must reproduce the above
//    copyright notice, this list of conditions and the following
//    disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
//  * Neither the name of Pantor Engineering AB nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//
// IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
// OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.

package com.pantor.blink;

import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.lang.reflect.Field;

/**
   The {@code DirectBuf} provides an implementaion of the {@code Buf}
   interface utilizing the {@code sun.misc.Unsafe} API.
 */

public final class DirectBuf implements Buf
{
   /**
      Creates a buffer with the specified capacity

      @param capacity the capacity
    */
   
   public static Buf newInstance (int capacity)
   {
      if (unsafe != null)
	 return new DirectBuf (capacity);
      else
	 // FIXME: Fallback to nio-based impl
	 return new ByteBuf (capacity);
   }

   /**
      Creates a buffer with a {@code DEFAULT_CAPACITY} capacity
    */
   
   public static Buf newInstance ()
   {
      return newInstance (DEFAULT_CAPACITY);
   }
   
   /**
      The default buffer capacity
    */
   
   public static int DEFAULT_CAPACITY = 4096;

   private DirectBuf (int capacity)
   {
      this.capacity = capacity;
      buf = ByteBuffer.allocateDirect (capacity);
      buf = buf.order (ByteOrder.LITTLE_ENDIAN);
      pos = start = ((sun.nio.ch.DirectBuffer)buf).address ();
      end = start + capacity;
   }

   @Override
   public void write (byte [] a)
   {
      write (a, 0, a.length);
   }

   @Override
   public void write (byte [] a, int from, int len)
   {
      unsafe.copyMemory (a, ByteArrayOff + from, null, pos, len);
      step (len);
   }

   @Override
   public void write (int b)
   {
      unsafe.putByte (pos ++, (byte)b);
   }
   
   @Override
   public void write (int b0, int b1)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      pos += 2;
   }
   
   @Override
   public void write (int b0, int b1, int b2)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      pos += 3;
   }
   
   @Override
   public void write (int b0, int b1, int b2, int b3)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      unsafe.putByte (pos + 3, (byte)b3);
      pos += 4;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      unsafe.putByte (pos + 3, (byte)b3);
      unsafe.putByte (pos + 4, (byte)b4);
      pos += 5;
   }
   
   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      unsafe.putByte (pos + 3, (byte)b3);
      unsafe.putByte (pos + 4, (byte)b4);
      unsafe.putByte (pos + 5, (byte)b5);
      pos += 6;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5,
		      int b6)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      unsafe.putByte (pos + 3, (byte)b3);
      unsafe.putByte (pos + 4, (byte)b4);
      unsafe.putByte (pos + 5, (byte)b5);
      unsafe.putByte (pos + 6, (byte)b6);
      pos += 7;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5,
		      int b6, int b7)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      unsafe.putByte (pos + 3, (byte)b3);
      unsafe.putByte (pos + 4, (byte)b4);
      unsafe.putByte (pos + 5, (byte)b5);
      unsafe.putByte (pos + 6, (byte)b6);
      unsafe.putByte (pos + 7, (byte)b7);
      pos += 8;
   }
   
   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5,
		      int b6, int b7, int b8)
   {
      unsafe.putByte (pos, (byte)b0);
      unsafe.putByte (pos + 1, (byte)b1);
      unsafe.putByte (pos + 2, (byte)b2);
      unsafe.putByte (pos + 3, (byte)b3);
      unsafe.putByte (pos + 4, (byte)b4);
      unsafe.putByte (pos + 5, (byte)b5);
      unsafe.putByte (pos + 6, (byte)b6);
      unsafe.putByte (pos + 7, (byte)b7);
      unsafe.putByte (pos + 8, (byte)b8);
      pos += 9;
   }

   @Override
   public int read ()
   {
      return (int)unsafe.getByte (pos ++) & 0xff;
   }

   @Override
   public void read (byte [] dst, int from, int len)
   {
      unsafe.copyMemory (null, pos, dst, ByteArrayOff + from, len);
      pos += len;
   }
   
   @Override
   public void read (byte [] dst)
   {
      read (dst, 0, dst.length);
   }

   @Override
   public void put (byte b)
   {
      unsafe.putByte (pos, b);
   }

   @Override
   public void put (int off, byte b)
   {
      unsafe.putByte (pos + off, b);
   }
   
   @Override
   public int get ()
   {
      return unsafe.getByte (pos) & 0xff;
   }
   
   @Override
   public int get (int off)
   {
      return unsafe.getByte (pos + off) & 0xff;
   }

   @Override
   public void step () { ++ pos; }

   @Override
   public void step (int delta) { pos += delta; }

   @Override
   public void shift (int from, int delta)
   {
      // FIXME: Verify that copyMemory handles overlapping src and dst
      unsafe.copyMemory (null, start + from, null, start + from + delta,
			 pos - from - start);
      pos += delta;
   }

   @Override
   public boolean empty () { return pos >= end; }

   @Override
   public int size () { return (int)(end - start); }
   
   @Override
   public void setSize (int end) { this.end = start + end; }
   
   @Override
   public int getPos () { return (int)(pos - start); }
   
   @Override
   public void setPos (int pos) { this.pos = start + pos; }
   
   @Override
   public int available () { return (int)(end - pos); }

   @Override
   public String readUtf8String (int size)
   {
      try
      {
	 // FIXME: Find something more efficient here
	 byte [] tmp = new byte [size];
	 read (tmp);
	 return new String (tmp, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
	 // FIXME: Should we raise BlinkException.Decode instead?
	 throw new RuntimeException (e);
      }
   }

   @Override
   public void flushTo (Object dst) throws IOException
   {
      if (pos > start)
      {
	 if (dst instanceof WritableByteChannel)
	    flushToChannel ((WritableByteChannel)dst);
	 else if (dst instanceof OutputStream)
	    flushToChannel (Channels.newChannel ((OutputStream)dst));
	 else
	    throw new IOException ("Unsupported output destination: " + dst);
      }
   }

   public void flushToChannel (WritableByteChannel ch) throws IOException
   {
      buf.clear ();
      buf.position ((int)(pos - start));
      buf.flip ();
      ch.write (buf);
      clear ();
   }
      
   @Override
   public void moveTo (ByteSink sink, int len)
   {
      // FIXME: Check if other is DirectBuf too and do away with tmp
      byte [] tmp = new byte [len];
      unsafe.copyMemory (null, pos, tmp, ByteArrayOff, len);
      sink.write (tmp);
      step (len);
   }

   @Override
   public void flip ()
   {
      end = pos;
      pos = start;
   }

   @Override
   public void clear () { pos = start; end = start + capacity; }

   @Override
   public void clearAndFillZero ()
   {
      clear ();
      unsafe.setMemory (start, capacity, (byte)0);
   }

   @Override
   public void reserve (int additionalCapacity)
   {
      int size = (int)(pos - start);
      int required = size + additionalCapacity;
      if (required > capacity)
      {
	 capacity = (int)(required * 1.5);
	 ByteBuffer newBuf = ByteBuffer.allocateDirect (capacity);
	 newBuf = newBuf.order (ByteOrder.LITTLE_ENDIAN);
	 long newStart = ((sun.nio.ch.DirectBuffer)newBuf).address ();
	 unsafe.copyMemory (null, start, null, newStart, size);
	 buf = newBuf;
	 start = newStart;
	 pos = start + size;
	 end = start + capacity;
      }
   }

   @Override
   public void release (int limit)
   {
      if (capacity > limit)
      {
	 capacity = 0;
	 buf = null;
	 start = pos = end = 0;
      }
      else
	 clear ();
   }

   @Override
   public void release ()
   {
      release (0);
   }

   @Override
   public boolean fillFrom (Object src) throws IOException
   {
      if (src instanceof ReadableByteChannel)
	 return fillFromChannel ((ReadableByteChannel)src);
      else if (src instanceof InputStream)
	 return fillFromChannel (Channels.newChannel ((InputStream)src));
      else
	 throw new IOException ("Unsupported input src: " + src);
   }

   /**
      Clears the buffer and fills it with bytes from the specified
      channel. It will at most read as many bytes from the channel as
      there is capacity in this buffer. If the source is exhausted, no
      bytes are read.

      @return {@code true} if there possible are more bytes to read
      from the channel, and {@code false} if the channel is exhausted.
      @throws IOException if there was an input error
   */

   public boolean fillFromChannel (ReadableByteChannel ch) throws IOException
   {
      buf.clear ();
      end = start + capacity;
      int n = ch.read (buf);
      if (n >= 0)
      {
	 pos = start + n;
	 return true;
      }
      else
      {
	 pos = start;
	 return false;
      }
   }

   @Override
   public void close ()
   {
      clear ();
   }

   @Override
   public void flush ()
   {
      clear ();
   }

   @Override
   public String toString ()
   {
      return "[" + toHexString ().replace (" ", ", ") + "]";
   }

   @Override
   public String toHexString ()
   {
      StringBuilder s = new StringBuilder ();
      for (int i = 0; i < size (); ++ i)
      {
	 if (i > 0)
	    s.append (' ');
	 s.append (String.format ("%02x", unsafe.getByte (start + i)));
      }
      return s.toString ();
   }
   
   private final static sun.misc.Unsafe unsafe = getUnsafe ();
   private final static long ByteArrayOff =
      unsafe != null ? unsafe.arrayBaseOffset (byte [].class) : 0;

   private static sun.misc.Unsafe getUnsafe ()
   {
      // FIXME: Disabled for now. Works but no significant gain in
      // overall performance, something else is stealing the
      // cycles. Enable when the difference here matters. Some
      // microbenchmarks suggest that byte level handling can be 3-4
      // times more efficient using the approach used in DirectBuf.
      
      if (false)
	 try
	 {
	    Field f = sun.misc.Unsafe.class.getDeclaredField ("theUnsafe");
	    f.setAccessible (true);
	    return (sun.misc.Unsafe) f.get (null);
	 }
	 catch (Exception e)
	 {
	 }

      return null;
   }

   private ByteBuffer buf;
   private long start;
   private long pos;
   private long end;
   private int capacity;
}
