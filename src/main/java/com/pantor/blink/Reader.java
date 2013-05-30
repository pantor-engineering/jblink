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
   The {@code Reader} interface is implemented by format specific
   decoders. The basic operation of a decoder is to decode sequences
   of bytes encoded in one of the Blink formats, into objects.

   <p>This interface provides {@code read} methods that collect
   decoded objects into instances of {@link Block}. The {@code read}
   methods that do not take a {@code Block} argument relies on the
   implementation for further processing. Typically this means that
   the read objects get dispatched to a set of type specific
   observers.</p>
 */

public interface Reader
{
   /**
      Decodes bytes specified in a byte array.

      @param data the bytes to decode
      @throws BlinkException if a decoding, schema or binding problem occurs
   */

   void read (byte [] data) throws BlinkException;

   /**
      Decodes bytes specified in a slice of a byte array.

      @param data the bytes to decode
      @param from the index of the first byte to decode
      @param len the number of bytes to decode
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   void read (byte [] data, int from, int len) throws BlinkException;

   /**
      Decodes bytes specified in a byte array. It appends decoded
      messages to the specified block.

      <p>It also allocates objects as needed from the specified block.</p>

      @param data the bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   public void read (byte [] data, Block block) throws BlinkException;
   
   /**
      Decodes bytes specified in a slice of a byte array.

      <p>It also allocates objects as needed from the specified block.</p>

      @param data the bytes to decode
      @param from the index of the first byte to decode
      @param len the number of bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   void read (byte [] data, int from, int len, Block block)
      throws BlinkException;

   /**
      Decodes bytes read from the specified byte source

      @param src the bytes to decode
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   void read (ByteSource src) throws BlinkException;

   /**
      Decodes bytes read from the specified byte source. It appends decoded
      messages to the specified block.

      <p>It also allocates objects as needed from the specified block.</p>

      <p>This is the most native form of the {@code read} methods. All other
      read methods will create temporary {@code ByteSource} objects and/or use
      a private {@link DefaultBlock} instance managed by this reader.</p>
      
      @param src the bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a decoding, schema or binding problem occurs
   */

   void read (ByteSource src, Block block) throws BlinkException;

   /**
      Returns {@code true} if there is no partial message pending

      @return {@code true} if there is no partial message pending
   */

   boolean isComplete ();
   
   /**
      Closes this reader

      @throws BlinkException.Decode if this reader is incomplete as
      indicated by {@code isComplete}
   */

   void close () throws BlinkException;
}
