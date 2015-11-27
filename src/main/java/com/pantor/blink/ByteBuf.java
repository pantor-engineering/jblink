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

/**
   The {@code ByteBuf} provides an implementaion of the {@code Buf}
   interface backed by a byte array.
 */

public final class ByteBuf implements Buf
{
   public enum ResizeMode
   {
      Fixed, Expandable
   }

   /**
      The default buffer capacity
    */
   
   public static int DEFAULT_CAPACITY = 4096;

   /**
      Creates an expandable buffer with the {@code DEFAULT_CAPACITY}
    */
   
   public ByteBuf () { this (DEFAULT_CAPACITY); }

   
   /**
      Creates an buffer with the specified capacity and the specified
      resize mode

      @param capacity the capacity
      @param rmode {@code ResizeMode.Fixed} or {@code ResizeMode.Expandable}
    */

   public ByteBuf (int capacity, ResizeMode rmode)
   {
      data_ = new byte [capacity]; clear ();
      isFixed = rmode == ResizeMode.Fixed;
   }
   
   /**
      Creates an expandable buffer with the specified initial capacity.

      @param capacity the capacity
    */

   public ByteBuf (int capacity)
   {
      this (capacity, ResizeMode.Expandable);
   }

   /**
      Creates a fixed buffer for reading bytes from a specified slice of a
      byte array.

      @param underlying the byte array to read from
      @param takeFrom the index of the first byte of the slice
      @param len the size of the slice
    */

   public ByteBuf (byte [] underlying, int takeFrom, int len)
   {
      data_ = underlying;
      isFixed = true;
      pos = takeFrom;
      end = pos + len;
   }

   /**
      Creates a fixed buffer for reading bytes from a specified byte array.

      @param underlying the byte array to read
    */
   
   public ByteBuf (byte [] underlying)
   {
      this (underlying, 0, underlying.length);
   }

   @Override
   public void write (byte [] a)
   {
      write (a, 0, a.length);
   }
   
   @Override
   public void write (byte [] a, int from, int len)
   {
      System.arraycopy (a, from, data_, pos, len);
      step (len);
   }
   
   @Override
   public void prepend (byte [] a)
   {
      prepend (a, 0, a.length);
   }
   
   @Override
   public void prepend (byte [] a, int from, int len)
   {
      shift (0, len);
      System.arraycopy (a, from, data_, 0, len);
   }
   
   @Override
   public void write (int b) { data_ [pos ++] = (byte)b; }

   @Override
   public void write (int b0, int b1)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      pos += 2;
   }

   @Override
   public void write (int b0, int b1, int b2)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      pos += 3;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      pos += 4;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      data_ [pos + 4] = (byte)b4;
      pos += 5;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      data_ [pos + 4] = (byte)b4;
      data_ [pos + 5] = (byte)b5;
      pos += 6;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5,
                      int b6)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      data_ [pos + 4] = (byte)b4;
      data_ [pos + 5] = (byte)b5;
      data_ [pos + 6] = (byte)b6;
      pos += 7;
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5,
                      int b6, int b7)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      data_ [pos + 4] = (byte)b4;
      data_ [pos + 5] = (byte)b5;
      data_ [pos + 6] = (byte)b6;
      data_ [pos + 7] = (byte)b7;
      pos += 8;
   }
   
   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5,
                      int b6, int b7, int b8)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      data_ [pos + 4] = (byte)b4;
      data_ [pos + 5] = (byte)b5;
      data_ [pos + 6] = (byte)b6;
      data_ [pos + 7] = (byte)b7;
      data_ [pos + 8] = (byte)b8;
      pos += 9;
   }

   @Override
   public int read () { return (int)data_ [pos ++] & 0xff; }

   @Override
   public void read (byte [] dst, int from, int len)
   {
      System.arraycopy (data_, pos, dst, from, len);
      pos += len;
   }

   @Override
   public void read (byte [] dst)
   {
      read (dst, 0, dst.length);
   }
   
   @Override
   public void put (byte b) { data_ [pos] = b; }
   
   @Override
   public void put (int off, byte b) { data_ [pos + off] = b; }

   @Override
   public int get () { return (int)data_ [pos] & 0xff; }

   @Override
   public int get (int off) { return (int)data_ [pos + off] & 0xff; }

   @Override
   public void step () { ++ pos; }

   @Override
   public void step (int delta) { pos += delta; }

   @Override
   public void shift (int from, int delta)
   {
      System.arraycopy (data_, from, data_, from + delta, pos - from);
      pos += delta;
   }
   
   @Override
   public boolean empty () { return pos >= end; }

   @Override
   public int size () { return end; }

   @Override
   public void setSize (int end) { this.end = end; }

   @Override
   public int getPos () { return pos; }

   @Override
   public void setPos (int pos) { this.pos = pos; }

   @Override
   public int available () { return end - pos; }

   @Override
   public String readUtf8String (int size)
   {
      try
      {
         String s = new String (data_, pos, size, "UTF-8");
         pos += size;
         return s;
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
      if (pos > 0)
      {
         if (dst instanceof OutputStream)
            ((OutputStream)dst).write (data_, 0, pos);
         else
            throw new IOException ("Unsupported output destination: " + dst);
         
         clear ();
      }
   }

   @Override
   public void moveTo (ByteSink sink, int len)
   {
      sink.write (data_, pos, len);
      step (len);
   }

   @Override
   public void prependTo (ByteSink sink, int len)
   {
      sink.prepend (data_, pos, len);
      step (len);
   }

   @Override
   public void flip ()
   {
      end = pos;
      pos = 0;
   }

   @Override
   public void clear () { pos = 0; end = data_.length; }

   @Override
   public void clearAndFillZero () { clear (); Arrays.fill (data_, (byte)0); }
   
   @Override
   public void reserve (int additionalCapacity) throws IOException
   {
      int capacity = pos + additionalCapacity;
      if (capacity > data_.length)
      {
         if (isFixed)
            throw new IOException (
               "Cannot resize fixed size buffer from " + data_.length +
               " to " + capacity + " bytes");
         else
         {
            byte [] newData = new byte [(int)(capacity * 1.5)];
            System.arraycopy (data_, 0, newData, 0, pos);
            data_ = newData;
            bbView = null;
         }
      }
   }
   
   @Override
   public void release (int limit) throws IOException
   {
      if (data_.length > limit)
      {
         if (isFixed)
            throw new IOException ("Cannot release a fixed size buffer");
         else
         {
            data_ = emptyData;
            bbView = null;
            pos = 0;
            end = 0;
         }
      }
      else
         clear ();
   }

   @Override
   public void release () throws IOException
   {
      release (0);
   }

   @Override
   public boolean fillFrom (Object src) throws IOException
   {
      if (src instanceof InputStream)
         return fillFromStream ((InputStream)src);
      else
         throw new IOException ("Unsupported input src: " + src);
   }

   /**
      Clears the buffer and fills it with bytes from the specified
      stream. It will at most read as many bytes from the stream as
      there is capacity in this buffer. If end of file is reached,
      no bytes are read.


      @return {@code true} if there possible are more bytes to read
      from the stream, and {@code false} if end of file is reached.
      @throws IOException if there was an input error
   */

   public boolean fillFromStream (InputStream is) throws IOException
   {
      pos = is.read (data_);
      end = data_.length;
      if (pos != -1)
         return true;
      else
      {
         pos = 0;
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
         s.append (String.format ("%02x", data_ [i]));
      }
      return s.toString ();
   }

   public ByteBuffer getByteBuffer ()
   {
      if (bbView == null)
         bbView = ByteBuffer.wrap (data_);
      bbView.limit (size ());
      bbView.position (0);
      return bbView;
   }
   
   private int pos;
   private int end;
   private byte [] data_;
   private ByteBuffer bbView;
   private final boolean isFixed;
   private final static byte [] emptyData = new byte [0];
}
