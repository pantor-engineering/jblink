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

public final class Utf8Util
{
   public static int write (String val, Buf buf)
      throws BlinkException.Encode
   {
      return write (val, 0, buf);
   }

   public static int write (String val, int i, Buf buf)
      throws BlinkException.Encode
   {
      int len = val.length ();
      int start = buf.getPos ();
      for (; i < len; ++ i)
      {
	 char c = val.charAt (i);
	 if (c < 0x0080)
	    buf.write (c);
	 else if (c < 0x0800)
	    buf.write (0xc0 | ((c >> 6) & 0x1f),
		       0x80 |   c       & 0x3f);
	 else if (c < 0xd800 || c > 0xdfff)
	    buf.write (0xe0 | ((c >> 12) & 0x0f),
		       0x80 | ((c >> 6)  & 0x3f),
		       0x80 |   c        & 0x3f);
	 else
	 {
	    ++ i;
	    if (i < len)
	    {
	       char c2 = val.charAt (i);
	       int u = (int)c << 10 + (int)c2 + SurrogateOffset;

	       buf.write (0xf0 | ((u >> 18) & 0x07),
			  0x80 | ((u >> 12) & 0x3f),
			  0x80 | ((u >> 6)  & 0x3f),
			  0x80 |   u        & 0x3f);
	    }
	    else
	       throw new BlinkException.Encode (
		  "Incomplete UTF-16 surrogate pair");
	 }
      }

      return buf.getPos () - start;
   }

   public static int getConservativeSize (int size)
   {
      return size * 4;
   }

   private final static int SurrogateOffset = 0x10000 - (0xD800 << 10) - 0xDC00;
}
