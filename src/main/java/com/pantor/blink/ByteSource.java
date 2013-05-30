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

/**
   The {@code ByteSource} interface is used for specifying
   implementation independent sequences of bytes to Blink decoders.
   Logically a byte sorce maintains a current position and an
   endpoint. The current position refers to the next byte to be read
   and the endpoint refers to the position directly after the last
   byte available for reading.

   <p>The position and the endpoint must not be negative.</p>
 */

public interface ByteSource
{
   /**
      Reads a byte and advances the position by once. The source
      must contain at least one byte at the current position

      @return the byte at the current position
    */
   
   int read ();

   /**
      Reads into a slice of a byte array. The source must contain
      at least {@code len} bytes at the position.

      @param dst the byte array to fill
      @param from the index of the first byte in the slice
      @param len the size of the slice to fill
    */
   
   void read (byte [] dst, int from, int len);

   /**
      Reads into a byte array. The source must contain at least {@code
      dst.length} bytes at the current position.

      @param dst the byte array to fill
   */
   
   void read (byte [] dst);

   /**
      Returns the byte at the current position

      @return the byte at the current position
   */
   
   int get ();

   /**
      Returns the byte at the position offsetted by {@code off}
      @return the byte at the specified offset
   */
   
   int get (int off);

   /**
      Advances the current position by one
   */
   
   void step ();

   /**
      Moves the current position relative to its current location.

      @param delta the distance to move, negative moves the current
      position before its current location
   */
   
   void step (int delta);

   /**
      Returns {@code true} if the current position is greater than or
      equal to the endpoint.

      @return {@code true} if there are no bytes available for reading
   */
   
   boolean empty ();

   /**
      Returns the distance in bytes between the start of the byte source
      and its endpoint.
      
      @return the size of the byte source
   */

   int size ();

   /**
      Sets the endpoint

      @param end the endpoint
   */
   
   void setSize (int end);

   /**
      Returns the position of the next byte available for reading

      @return the current position
   */
   
   int getPos ();

   /**
      Sets the current position

      @param pos the new value for the current position
   */
   
   void setPos (int pos);

   /**
      Returns the number of bytes available for reading from the
      position to the end of the byte source.

      @return the number of bytes available for reading
    */
   
   int available ();

   /**
      Reads {@code size} UTF-8 bytes from the current position and
      creates a {@code String} object.
    */
   
   String readUtf8String (int size);

   /**
      Reads {@code len} bytes from this source and writes them to
      the specified sink.

      @param sink the sink to move to
      @param len the number of bytes to move
    */
   
   void moveTo (ByteSink sink, int len);
}
