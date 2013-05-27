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
import java.io.OutputStream;

/**
   The {@code Writer} interface is implemented by format specific
   encoders. The basic operation of an encoder is to encode objects
   into sequences of bytes in one of the Blink formats.
*/

public interface Writer
{
   /**
      Encodes an object.

      @param o the object to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   void write (Object o) throws BlinkException, IOException;

   /**
      Encodes an array of objects.

      @param objs the objects to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   void write (Object [] objs) throws BlinkException, IOException;

   /**
      Encodes a slice of an array of objects.

      @param objs the objects to write
      @param from the index of the first object to encode
      @param len the number of objects to encode
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   void write (Object [] objs, int from, int len)
      throws BlinkException, IOException;

   /**
      Encodes an iterable collection of objects

      @param objs the objects to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   void write (Iterable<?> objs) throws BlinkException, IOException;

   /**
      Flushes any pending encoded messages and any underlying output stream
      @throws IOException if there is an output error
   */
   
   void flush () throws IOException;

   /**
      Flushes any pending encoded messages and closes any underlying
      output stream.

      @throws IOException if there is an output error
   */
   
   void close () throws IOException;
}
