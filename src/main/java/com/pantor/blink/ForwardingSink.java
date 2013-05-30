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

public abstract class ForwardingSink implements ByteSink
{
   protected ForwardingSink (ByteSink inner)
   {
      this.inner = inner;
   }

   @Override
   public void write (byte [] a)
   {
      inner.write (a);
   }
   
   @Override
   public void write (byte [] a, int from, int len)
   {
      inner.write (a, from, len);
   }

   @Override
   public void write (int b)
   {
      inner.write (b);
   }

   @Override
   public void write (int b0, int b1)
   {
      inner.write (b0, b1);
   }

   @Override
   public void write (int b0, int b1, int b2)
   {
      inner.write (b0, b1, b2);
   }

   @Override
   public void write (int b0, int b1, int b2, int b3)
   {
      inner.write (b0, b1, b2, b3);
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4)
   {
      inner.write (b0, b1, b2, b3, b4);
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5)
   {
      inner.write (b0, b1, b2, b3, b4, b5);
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5, int b6)
   {
      inner.write (b0, b1, b2, b3, b4, b5, b6);
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5, int b6,
		      int b7)
   {
      inner.write (b0, b1, b2, b3, b4, b5, b6, b7);
   }

   @Override
   public void write (int b0, int b1, int b2, int b3, int b4, int b5, int b6,
		      int b7, int b8)
   {
      inner.write (b0, b1, b2, b3, b4, b5, b6, b7, b8);
   }
   
   @Override
   public void put (byte b)
   {
      inner.put (b);
   }

   @Override
   public void put (int off, byte b)
   {
      inner.put (off, b);
   }

   @Override
   public void step ()
   {
      inner.step ();
   }

   @Override
   public void step (int delta)
   {
      inner.step (delta);
   }

   @Override
   public void shift (int from, int delta)
   {
      inner.shift (from, delta);
   }

   @Override
   public int getPos ()
   {
      return inner.getPos ();
   }

   @Override
   public void setPos (int pos)
   {
      inner.setPos (pos);
   }

   @Override
   public int available ()
   {
      return inner.available ();
   }

   @Override
   public void reserve (int additionalCapacity)
   {
      inner.reserve (additionalCapacity);
   }

   @Override
   public void flushTo (Object dst) throws IOException
   {
      inner.flushTo (dst);
   }

   protected final ByteSink inner;
}
