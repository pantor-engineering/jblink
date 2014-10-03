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
   The {@code Buf} interface is the combination of a byte source and a
   byte sink. Logically a buffer maintains a current position, an
   endpoint and a capacity.
 */

public interface Buf extends ByteSource, ByteSink
{
   /**
      Sets the endpoint to the value of the current position and then
      sets the current position to zero. You typically use this if you
      first fill a buffer through writes and then want to read those
      bytes.
    */
   
   void flip ();

   /**
      Clears the buffer by setting the current position to zero and
      the endpoint to the capacity.
    */

   void clear ();

   /**
      Clears the buffer and fills it with bytes from the specified source.
      It will at most read as many bytes as there is room in the buffer.
      If the source is exhausted, no bytes are read.

      <p>The implementation must dynamically discover if it knows how
      to use the actual type of the source. Not all implementations
      can handle all possible source types. It is recommended that a
      generic implementation at least supports the java.io.InputStream
      type.</p>

      @return {@code true} if there possible are more bytes to read
      from the srouce, and {@code false} if the source is exhausted.
      @throws IOException if there was an input error
   */
   
   boolean fillFrom (Object src) throws IOException;

   /**
      Clears the buffer and fills the underlying byte storage with zeros.
    */
   
   void clearAndFillZero ();

   /**
      Releases the underlying byte storage if it is larger than {@code
      limit}. If it is released, the buffer will effectively have a
      zero capacity. A call to {@code release} should therefore always
      be follwed by a call to {@code reserve} before writing to the
      buffer again.

      @param limit the maximum number of bytes to retain after this call
      @throws IOException if the buffer has fixed size
    */
   
   void release (int limit) throws IOException;

   /**
      Unconditionally releases the underlying byte storage. It has the
      same effect as calling {@code release (0)}.

      @throws IOException if the buffer has fixed size
    */
   
   void release () throws IOException;

   /**
      Returns a string representaion of this buffer where each byte is
      represented by a two-digit hexadecimal number. The bytes are
      separated by space. Bytes are included from the start of the
      buffer up to the endpoint.

      <p>The first byte included has position zero, and the last byte
      has position {@code endpoint - 1}.</p>

      @return a string with hex numbers
    */
   
   String toHexString ();
}
