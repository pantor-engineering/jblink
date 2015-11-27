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
   The {@code BlankBlock} class provides a null implementation of
   the {@code Block} interface. This implementation will not store
   decoded objects.
*/

public class BlankBlock implements Block
{
   private final static int DefaultSlabSize = 128;

   /**
      Creates a block
   */
   
   public BlankBlock ()
   {
      this (DefaultSlabSize);
   }
   
   /**
      Creates a block using the specified {@code slabSize}. The slab
      size controls how many objects that are allocated by each call
      to the {@code fill} method.

      @param slabSize the number of objects to allocate at once
   */
   
   public BlankBlock (int slabSize)
   {
      this.slabSize = slabSize;
   }

   @Override
   public void append (Object o)
   {
      ++ count;
   }

   /**
      Fills the specified array with fresh objects allocated by the
      specified creator. If the specified array is {@code null} it
      creates a new array of the slab size specified for this block.

      @param ctor a creator for a specific POJO class
      @param o an object array to reuse, can be {@code null}
      @return an array filled with objects created with the specified
      creator
      @throws BlinkException.Binding if a binding problem occurs
   */

   @Override
   public Object [] refill (Creator ctor, Object [] o)
      throws BlinkException.Binding
   {
      if (o == null)
         o = new Object [slabSize];
      for (int i = 0; i < o.length; ++ i)
         o [i] = ctor.newInstance ();
      return o;
   }

   /**
      Sets all entries in the specified array to {@code null}
   */
   
   @Override
   public void reclaim (Object [] o, int from, int len)
   {
      for (int i = from; i < from + len; ++ i)
         o [i] = null;
   }

   /**
      Just ignores the specified object since no pooling is used here
   */
   
   @Override
   public void reclaim (Object o)
   {
   }

   public void reset ()
   {
      count = 0;
   }

   public int getCount ()
   {
      return count;
   }

   private int count;
   private final int slabSize;
}
