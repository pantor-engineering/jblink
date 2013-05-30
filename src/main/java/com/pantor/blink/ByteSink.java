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

import java.io.IOException;

/**
   The {@code ByteSink} interface is used by decoders for writing
   Blink encoded bytes. Logically a byte sink maintains a current
   position and an endpoint. The current position refers to the
   position where the next byte will be written. The endpoint refers
   to the position directly after the last location that can be
   written to.

   <p>The current position and the endpoint must not be negative.</p>
 */

public interface ByteSink
{
   /**
      Writes a byte array to the sink and advances the current
      position with the length of the array. The sink must have room
      for at least {@code a.length} bytes.

      @param a the array to write
    */
   
   void write (byte [] a);

   /**
      Writes a slice of a byte array to the sink and advances the
      current position with the size of the slice. The sink must have
      room for at least {@code len} bytes.

      @param a the array to write
      @param from the index of the first byte of the slice
      @param len the size of the slice
    */
   
   void write (byte [] a, int from, int len);
   
   /**
      Writes a single byte to the sink and advances the current
      position one step. The sink must have room for at least one byte
      at the current position.

      @param b the byte to write, only the eight least significant
             bits will be used
    */
   
   void write (int b);

   /**
      Writes two bytes to the sink and advances the current position
      by two. Only the eight least significant bits of each argument
      is used. The sink must have room for at least two bytes at the
      position.

      @param b0 a byte to write
      @param b1 a byte to write
    */
   
   void write (int b0, int b1);

   /**
      Writes three bytes to the sink and advances the current position
      by three. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
    */
   
   void write (int b0, int b1, int b2);

   /**
      Writes three bytes to the sink and advances the current position
      by four. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
    */
   
   void write (int b0, int b1, int b2, int b3);

   /**
      Writes three bytes to the sink and advances the current position
      by five. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
    */
   
   void write (int b0, int b1, int b2, int b3, int b4);

   /**
      Writes three bytes to the sink and advances the current position
      by six. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

      @param b0 a byte to write
      @param b1 a byte to write
      @param b2 a byte to write
      @param b3 a byte to write
      @param b4 a byte to write
      @param b5 a byte to write
    */
   
   void write (int b0, int b1, int b2, int b3, int b4, int b5);

   /**
      Writes three bytes to the sink and advances the current position
      by seven. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

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
      Writes three bytes to the sink and advances the current position
      by eight. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

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
      Writes three bytes to the sink and advances the current position
      by nine. Only the eight least significant bits of each argument
      is used. The sink must have room for at least three bytes at the
      position.

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
      Puts a single byte at the current position. The sink must have
      room for at least one byte at the current position.

      @param b the byte to put
    */
   
   void put (byte b);

   /**
      Puts a single byte at the current position offsetted by {@code
      off}. The sink must have room for at least one byte at the
      resulting position

      @param off offset relative to the position
      @param b the byte to put at the specified offset
    */
   
   void put (int off, byte b);

   /**
      Advances the current position by one
   */
   
   void step ();

   /**
      Moves the current position relative to its current location

      @param delta the distance to move, negative moves the current
      position before its current location
   */
   
   void step (int delta);

   /**
      Shifts a part of the sink a delta number of positions. The part
      to shift starts at from and ends with the byte just before the
      current position. The current position is adjusted by adding the
      delta to it.
      
      @param from the index of the first byte to shift
      @param delta a numer of positions to shift. A positive value results
      in a shift to the right and a negative value shifts to the left.
   */

   void shift (int from, int delta);
   
   /**
      Returns the current position for writing

      @return the current position
   */
   
   int getPos ();

   /**
      Sets the current position

      @param pos the current position
   */
   
   void setPos (int pos);

   /**
      Returns the distance in bytes between the curent position and
      the endpoint.

      @return the number of bytes available for writing
    */
   
   int available ();

   /**
      Makes sure there is room for writing at least {@code
      additionalCapacity} bytes at the current position. If the sink
      capacity is too small, it will grow accordingly.
    */
   
   void reserve (int additionalCapacity);

   /**
      Flushes the contents of the sink to any underlying output
      destination. After the flush, the current position is reset to
      zero.

      @throws IOException if there was an output error
   */
   
   void flush () throws IOException;

   /**
      Flushes the contents of the sink to any underlying output
      destination, and then closes the output destination. After the
      flush, the current position is reset to zero.

      @throws IOException if there was an output error
   */
   
   void close () throws IOException;

   /**
      Flushes the contents to the specified output destination.
      After the flush, the current position is reset to zero.

      <p>The implementation must dynamically discover if it knows how
      to use the actual type of the destination. Not all
      implementations can handle all possible destination types. It is
      recommended that a generic implementation at least supports the
      java.io.OutputStream type.</p>
      
      @param dst an output destination
      @throws IOException if there was an output error
    */

   void flushTo (Object dst) throws IOException;
}
