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
   The {@code Block} class is the interface a decoder uses for
   collecting decoded objects. It is also used for allocating new
   objects before populating them. This enables object pooling
   strategies.
*/

public interface Block
{
   /**
      Appends a decoded object to this block. The object has
      previously been allocated through the {@code refill} method.

      @param o the object to be appended
    */

   void append (Object o);

   /**
      Fills an array with fresh objects created by the specified
      {@code Creator}. If the specified array is not null it may be
      reused. However, the filled and returned array is not guaranteed
      to be the same as the array specified for reuse.

      @param ctor a creator to create new instances with
      @param o an array to be reused or null
      @return an array filled with objects created by the specified creator
    */
   
   Object [] refill (Creator ctor, Object [] o) throws BlinkException.Binding;

   /**
      Reclaims superfluous objects. This will typically happen when a
      decoder will not be used anymore and there are still unused
      object. This makes it possible to not leak objects when using an object
      pooling strategy.
    */
   
   void reclaim (Object [] o, int from, int len);
}
