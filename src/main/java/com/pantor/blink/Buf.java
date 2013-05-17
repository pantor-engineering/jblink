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

/**
   The {@code Buf} class is used by encoders and decoders as an interface
   to a sequence of bytes to be read or written. When reading the buffer
   can represent a slice of an already existing byte array.
 */

public final class Buf
{
   /**
      The default buffer size
    */
   
   public static int DEFAULT_SIZE = 4096;

   /**
      Creates a buffer with the {@code DEFAULT_SIZE}
    */
   
   public Buf () { this (DEFAULT_SIZE); }

   /**
      Creates a buffer with the specified capacity

      @param size the capacity
    */
   
   public Buf (int size) { data_ = new byte [size]; clear (); }

   /**
      Creates a buffer for reading bytes from a specified slice of a
      byte array.

      @param underlying the byte array to read from
      @param takeFrom the index of the first byte of the slice
      @param len the size of the slice
    */

   public Buf (byte [] underlying, int takeFrom, int len)
   {
      data_ = underlying;
      pos = takeFrom;
      end = pos + len;
   }

   /**
      Creates a buffer for reading bytes from a specified byte array.

      @param underlying the byte array to read
    */
   
   public Buf (byte [] underlying)
   {
      this (underlying, 0, underlying.length);
   }

   /**
      Writes a byte array to the buffer and advances the position
      with the lenght of the array. The buffer must have room for
      at least {@code a.length} bytes.

      @param a the array to write
    */
   
   public void write (byte [] a)
   {
      write (a, 0, a.length);
   }

   /**
      Writes a slice of a byte array to the buffer and advances the
      position with the size of the slice. The buffer must have room for
      at least {@code len} bytes.

      @param a the array to write
      @param from the index of the first byte of the slice
      @param len the size of the slice
    */
   
   public void write (byte [] a, int from, int len)
   {
      System.arraycopy (a, from, data_, pos, len);
      step (len);
   }
   
   /**
      Writes a single byte to the buffer and advances the position one step.
      The buffer must have room for at least one byte at the position.

      @param b the byte to write, only the eight least significant
             bits will be used
    */
   
   public void write (int b) { data_ [pos ++] = (byte)b; }

   /**
      Writes two bytes to the buffer and advances the position by two.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least two bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
    */
   
   public void write (int b0, int b1)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      pos += 2;
   }

   /**
      Writes three bytes to the buffer and advances the position by three.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least three bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
    */
   
   public void write (int b0, int b1, int b2)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      pos += 3;
   }

   /**
      Writes four bytes to the buffer and advances the position by four.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least four bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
    */
   
   public void write (int b0, int b1, int b2, int b3)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      pos += 4;
   }

   /**
      Writes five bytes to the buffer and advances the position by five.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least five bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
    */
   
   public void write (int b0, int b1, int b2, int b3, int b4)
   {
      data_ [pos    ] = (byte)b0;
      data_ [pos + 1] = (byte)b1;
      data_ [pos + 2] = (byte)b2;
      data_ [pos + 3] = (byte)b3;
      data_ [pos + 4] = (byte)b4;
      pos += 5;
   }

   /**
      Writes six bytes to the buffer and advances the position by six.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least six bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
      @param b5 a byte to write
    */
   
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

   /**
      Writes seven bytes to the buffer and advances the position by seven.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least seven bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
      @param b5 a byte to write
      @param b6 a byte to write
    */
   
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

   /**
      Writes eight bytes to the buffer and advances the position by eight.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least eight bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
      @param b5 a byte to write
      @param b6 a byte to write
      @param b7 a byte to write
    */
   
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

   /**
      Writes nine bytes to the buffer and advances the position by nine.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least nine bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
      @param b5 a byte to write
      @param b6 a byte to write
      @param b7 a byte to write
      @param b8 a byte to write
    */
   
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

   /**
      Reads a byte and advances the position by once. The buffer
      must contain at least one byte at the position

      @return the byte at the position
    */
   
   public int read () { return (int)data_ [pos ++] & 0xff; }

   /**
      Reads into a slice of an byte array. The buffer must contain
      at least {@code len} bytes at the position.

      @param dst the byte array to fill
      @param from the index of the first byte in the slice
      @param len the size of the slice to fill
    */
   
   public void read (byte [] dst, int from, int len)
   {
      System.arraycopy (data_, pos, dst, from, len);
      pos += len;
   }

   /**
      Reads into a byte array. The buffer must contain at least {@code
      dst.length} bytes at the position.

      @param dst the byte array to fill
   */
   
   public void read (byte [] dst)
   {
      read (dst, 0, dst.length);
   }

   /**
      Puts a byte at the position

      @param b the byte to put
    */
   
   public void put (byte b) { data_ [pos] = b; }

   /**
      Puts a byte at the position offsetted by {@code off}

      @param off offset relative to the position
      @param b the byte to put at the specified offset
    */
   
   public void put (int off, byte b) { data_ [pos + off] = b; }

   /**
      Returns the byte at the position

      @return the byte at the position
   */
   
   public int get () { return (int)data_ [pos] & 0xff; }

   /**
      Returns the byte at the position offsetted by {@code off}
      @return the byte at the specified offset
   */
   
   public int get (int off) { return (int)data_ [pos + off] & 0xff; }

   /**
      Advances the position by one
   */
   
   public void step () { ++ pos; }

   /**
      Moves the position relative to the current position

      @param delta the distance to move, negative moves the position before
      its current location
   */
   
   public void step (int delta) { pos += delta; }

   /**
      Returns the underlying byte array

      @return the underlying byte array
    */
   
   public byte [] data () { return data_; }

   /**
      Returns {@code true} if the buffer contains no bytes

      @return {@code true} if the buffer is empty
   */
   
   public boolean empty () { return pos >= end; }

   /**
      Returns the distance in bytes between the start of the buffer
      and the endpoint.
      
      @return the size of the buffer
   */

   public int size () { return end; }

   /**
      Sets the endpoint

      @param end the endpoint
   */
   
   public void setSize (int end) { this.end = end; }

   /**
      Returns the position for reading or writing

      @return the position
   */
   
   public int getPos () { return pos; }

   /**
      Sets the current position

      @param pos the position
   */
   
   public void setPos (int pos) { this.pos = pos; }

   /**
      Returns the distance in bytes between the position and the endpoint.

      @return the number of bytes available for reading or writing
    */
   
   public int available () { return end - pos; }

   /**
      Reads {@code size} UTF-8 bytes from the current position to
      create a {@code String} object.
    */
   
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

   /**
      Writes any data from the start of the buffer upto the current
      position to the specified output stream. Then clears the buffer
      using {@code clear}.

      @param os the stream to write to
      @throws IOException if an output exception occured
    */
   
   public void flushTo (OutputStream os) throws IOException
   {
      if (pos > 0)
      {
	 os.write (data_, 0, pos);
	 clear ();
      }
   }

   /**
      Reades {@code len} bytes from the specified buffer and writes
      them to this buffer.

      @param other the buffer to move from
      @param len the number of bytes to move
    */
   
   public void moveFrom (Buf other, int len)
   {
      System.arraycopy (other.data_, other.pos, data_, pos, len);
      step (len);
      other.step (len);
   }

   /**
      Sets the endpoint to the value of the position and then sets the
      position to zero. You typically use this if you first fill a buffer
      through writes and then want to read those bytes.
    */
   
   public void flip ()
   {
      end = pos;
      pos = 0;
   }

   /**
      Clears the buffer by setting the position to zero and the endpoint
      to the capacity of the underlying byte array
    */
   
   public void clear () { pos = 0; end = data_.length; }


   /**
      Clears the buffer and fills the underlying byte array with zeros.
    */
   
   public void clearAndFillZero () { clear (); Arrays.fill (data_, (byte)0); }

   /**
      Makes sure there are at least room for writing {@code
      additionalCapacity} bytes to this buffer. If the buffer
      capacity is too small, it will grow accordingly.
    */
   
   public void reserve (int additionalCapacity)
   {
      int capacity = pos + additionalCapacity;
      if (capacity > data_.length)
      {
	 byte [] newData = new byte [(int)(capacity * 1.5)];
	 System.arraycopy (data_, 0, newData, 0, pos);
	 data_ = newData;
      }
   }

   /**
      Releases the underlying byte array if it is larger than {@code
      limit}. If it is released, the buffer will effectively have a
      zero capacity. A call to {@code release} should therefore always
      be follwed by a call to {@code reserve} before writing to the
      buffer again.

      @param limit the maximum number of bytes to retain after this call
    */
   
   public void release (int limit)
   {
      if (data_.length > limit)
      {
	 data_ = emptyData;
	 pos = 0;
	 end = 0;
      }
      else
	 clear ();
   }

   /**
      Unconditionally releases the underlying byte array. It has the
      same effect as calling {@code release (0)}.
    */
   
   public void release ()
   {
      release (0);
   }

   /**
      Reads a chunk of bytes from the specified input stream and writes
      them to this buffer. The buffer is treated as if cleared just before
      this call. That is, it will start writing to the beginning of the
      buffer.

      @param is the stream to read from
      @return {@code true} if bytes were read or {@code false} if the
      end of file was reached
    */
   
   public boolean fillFrom (InputStream is) throws IOException
   {
      pos = is.read (data_);
      end = 0;
      if (pos != -1)
	 return true;
      else
      {
	 pos = 0;
	 return false;
      }
   }

   @Override
   public String toString ()
   {
      return "[" + toHexString ().replace (" ", ", ") + "]";
   }

   /**
      Returns a string representaion of this buffer where each byte is
      represented by a two-digit hexadecimal number. The bytes are
      separated by space. Bytes are included from the start of the
      buffer up to the endpoint.

      @return a string with hex numbers
    */
   
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
   
   private int pos;
   private int end;
   private byte [] data_;
   private final static byte [] emptyData = new byte [0];
}
