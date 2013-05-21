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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
   The {@code Buf} interface is used by encoders and decoders for
   managing a sequence of bytes to be read or written.
 */

public interface Buf
{
   /**
      Writes a byte array to the buffer and advances the position
      with the length of the array. The buffer must have room for
      at least {@code a.length} bytes.

      @param a the array to write
    */
   
   void write (byte [] a);

   /**
      Writes a slice of a byte array to the buffer and advances the
      position with the size of the slice. The buffer must have room for
      at least {@code len} bytes.

      @param a the array to write
      @param from the index of the first byte of the slice
      @param len the size of the slice
    */
   
   void write (byte [] a, int from, int len);
   
   /**
      Writes a single byte to the buffer and advances the position one step.
      The buffer must have room for at least one byte at the position.

      @param b the byte to write, only the eight least significant
             bits will be used
    */
   
   void write (int b);

   /**
      Writes two bytes to the buffer and advances the position by two.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least two bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
    */
   
   void write (int b0, int b1);

   /**
      Writes three bytes to the buffer and advances the position by three.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least three bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
    */
   
   void write (int b0, int b1, int b2);

   /**
      Writes four bytes to the buffer and advances the position by four.
      Only the eight least significant bits of each argument is used.
      The buffer must have room for at least four bytes at the position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
    */
   
   void write (int b0, int b1, int b2, int b3);

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
   
   void write (int b0, int b1, int b2, int b3, int b4);

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
   
   void write (int b0, int b1, int b2, int b3, int b4, int b5);

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
   
   void write (int b0, int b1, int b2, int b3, int b4, int b5, int b6);

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
   
   void write (int b0, int b1, int b2, int b3, int b4, int b5, int b6, int b7);

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
   
   void write (int b0, int b1, int b2, int b3, int b4, int b5, int b6, int b7,
	       int b8);

   /**
      Reads a byte and advances the position by once. The buffer
      must contain at least one byte at the position

      @return the byte at the position
    */
   
   int read ();

   /**
      Reads into a slice of an byte array. The buffer must contain
      at least {@code len} bytes at the position.

      @param dst the byte array to fill
      @param from the index of the first byte in the slice
      @param len the size of the slice to fill
    */
   
   void read (byte [] dst, int from, int len);

   /**
      Reads into a byte array. The buffer must contain at least {@code
      dst.length} bytes at the position.

      @param dst the byte array to fill
   */
   
   void read (byte [] dst);

   /**
      Puts a byte at the position

      @param b the byte to put
    */
   
   void put (byte b);

   /**
      Puts a byte at the position offsetted by {@code off}

      @param off offset relative to the position
      @param b the byte to put at the specified offset
    */
   
   void put (int off, byte b);

   /**
      Returns the byte at the position

      @return the byte at the position
   */
   
   int get ();

   /**
      Returns the byte at the position offsetted by {@code off}
      @return the byte at the specified offset
   */
   
   int get (int off);

   /**
      Advances the position by one
   */
   
   void step ();

   /**
      Moves the position relative to the current position

      @param delta the distance to move, negative moves the position before
      its current location
   */
   
   void step (int delta);

   /**
      Returns {@code true} if the buffer contains no bytes

      @return {@code true} if the buffer is empty
   */
   
   boolean empty ();

   /**
      Returns the distance in bytes between the start of the buffer
      and the endpoint.
      
      @return the size of the buffer
   */

   int size ();

   /**
      Sets the endpoint

      @param end the endpoint
   */
   
   void setSize (int end);

   /**
      Returns the position for reading or writing

      @return the position
   */
   
   int getPos ();

   /**
      Sets the current position

      @param pos the position
   */
   
   void setPos (int pos);

   /**
      Returns the distance in bytes between the position and the endpoint.

      @return the number of bytes available for reading or writing
    */
   
   int available ();

   /**
      Reads {@code size} UTF-8 bytes from the current position to
      create a {@code String} object.
    */
   
   String readUtf8String (int size);

   /**
      Writes any data from the start of the buffer upto the current
      position to the specified output stream. Then clears the buffer
      using {@code clear}.

      @param os the stream to write to
      @throws IOException if an output exception occured
    */
   
   void flushTo (OutputStream os) throws IOException;

   /**
      Reads {@code len} bytes from this buffer and writes them to
      the other buffer.

      @param other the buffer to move to
      @param len the number of bytes to move
    */
   
   void moveTo (Buf other, int len);

   /**
      Sets the endpoint to the value of the position and then sets the
      position to zero. You typically use this if you first fill a buffer
      through writes and then want to read those bytes.
    */
   
   void flip ();

   /**
      Clears the buffer by setting the position to zero and the endpoint
      to the capacity of the underlying byte array
    */
   
   void clear ();

   /**
      Clears the buffer and fills the underlying byte array with zeros.
    */
   
   void clearAndFillZero ();

   /**
      Makes sure there are at least room for writing {@code
      additionalCapacity} bytes to this buffer. If the buffer
      capacity is too small, it will grow accordingly.
    */
   
   void reserve (int additionalCapacity);

   /**
      Releases the underlying byte array if it is larger than {@code
      limit}. If it is released, the buffer will effectively have a
      zero capacity. A call to {@code release} should therefore always
      be follwed by a call to {@code reserve} before writing to the
      buffer again.

      @param limit the maximum number of bytes to retain after this call
    */
   
   void release (int limit);

   /**
      Unconditionally releases the underlying byte array. It has the
      same effect as calling {@code release (0)}.
    */
   
   void release ();

   /**
      Reads a chunk of bytes from the specified input stream and writes
      them to this buffer. The buffer is treated as if cleared just before
      this call. That is, it will start writing to the beginning of the
      buffer.

      @param is the stream to read from
      @return {@code true} if bytes were read or {@code false} if the
      end of file was reached
    */
   
   boolean fillFrom (InputStream is) throws IOException;

   /**
      Returns a string representaion of this buffer where each byte is
      represented by a two-digit hexadecimal number. The bytes are
      separated by space. Bytes are included from the start of the
      buffer up to the endpoint.

      @return a string with hex numbers
    */
   
   String toHexString ();
}
