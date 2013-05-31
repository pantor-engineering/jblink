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

public final class Vlc
{
   public final static int Null = 0xc0;
   public final static int Int8MaxSize = 2;
   public final static int Int16MaxSize = 3;
   public final static int Int32MaxSize = 5;
   public final static int Int64MaxSize = 9;

   public final static int OneByteUintMax = (1 << 7) - 1;
   public final static int TwoByteUintMax = (1 << 14) - 1;
   public final static int ThreeByteUintMax = (1 << 16) - 1;
   public final static int FourByteUintMax = (1 << 24) - 1;

   public static int getUintSize (int x)
   {
      if (x <= OneByteUintMax)
         return 1;
      else if (x <= TwoByteUintMax)
         return 2;
      else if (x <= ThreeByteUintMax)
         return 3;
      else if (x <= FourByteUintMax)
         return 4;
      else
         return 5;
   }
   
   public static void write7 (int val, ByteSink sink)
   {
      sink.write (val & 0x7f);
   }

   public static void write14 (int val, ByteSink sink)
   {
      sink.write (0x80 | (val & 0x3f), (val >> 6) & 0xff);
   }

   public static void write16 (int val, ByteSink sink)
   {
      sink.write (0xc2,
                  val & 0xff,
                  (val & 0xff00) >> 8);
   }

   public static void write24 (int val, ByteSink sink)
   {
      sink.write (0xc3,
                  val & 0xff,
                  (val & 0xff00) >> 8,
                  (val & 0xff0000) >> 16);
   }
   
   public static void write32 (int val, ByteSink sink)
   {
      sink.write (0xc4,
                  val & 0xff,
                  (val & 0xff00) >> 8,
                  (val & 0xff0000) >> 16,
                  (val & 0xff000000) >> 24);
   }

   public static void write40 (long val, ByteSink sink)
   {
      sink.write (0xc5,
                  (int)(val & 0xffL),
                  (int)((val & 0xff00L) >> 8),
                  (int)((val & 0xff0000L) >> 16),
                  (int)((val & 0xff000000L) >> 24),
                  (int)((val & 0xff00000000L) >> 32));
   }

   public static void write48 (long val, ByteSink sink)
   {
      sink.write (0xc6,
                  (int)((val & 0xffL)),
                  (int)((val & 0xff00L) >> 8),
                  (int)((val & 0xff0000L) >> 16),
                  (int)((val & 0xff000000L) >> 24),
                  (int)((val & 0xff00000000L) >> 32),
                  (int)((val & 0xff0000000000L) >> 40));
   }

   public static void write56 (long val, ByteSink sink)
   {
      sink.write (0xc7,
                  (int)((val & 0xffL)),
                  (int)((val & 0xff00L) >> 8),
                  (int)((val & 0xff0000L) >> 16),
                  (int)((val & 0xff000000L) >> 24),
                  (int)((val & 0xff00000000L) >> 32),
                  (int)((val & 0xff0000000000L) >> 40),
                  (int)((val & 0xff000000000000L) >> 48));
   }

   public static void write64 (long val, ByteSink sink)
   {
      sink.write (0xc8,
                  (int)((val & 0xffL)),
                  (int)((val & 0xff00L) >> 8),
                  (int)((val & 0xff0000L) >> 16),
                  (int)((val & 0xff000000L) >> 24),
                  (int)((val & 0xff00000000L) >> 32),
                  (int)((val & 0xff0000000000L) >> 40),
                  (int)((val & 0xff000000000000L) >> 48),
                  (int)((val & 0xff00000000000000L) >> 56));
   }
   
   public static int writeU32 (int val, ByteSink sink)
   {
      if (val < 0)
      {
         write32 (val, sink);
         return 5;
      }
      else if (val < 0x00000080)
      {
         write7 (val, sink);
         return 1;
      }
      else if (val < 0x00004000)
      {
         write14 (val, sink);
         return 2;
      }
      else if (val < 0x00010000)
      {
         write16 (val, sink);
         return 3;
      }
      else if (val < 0x01000000)
      {
         write24 (val, sink);
         return 4;
      }
      else
      {
         write32 (val, sink);
         return 5;
      }
   }

   private static int writeNegative (int val, ByteSink sink)
   {
      if (val >= -64)
      {
         write7 (val, sink);
         return 1;
      }
      else if (val >= -8192)
      {
         write14 (val, sink);
         return 2;
      }
      else if (val >= -32768)
      {
         write16 (val, sink);
         return 3;
      }
      else if (val >= -8388608)
      {
         write24 (val, sink);
         return 4;
      }
      else
      {
         write32 (val, sink);
         return 5;
      }
   }

   public static int writeI32 (int val, ByteSink sink)
   {
      if (val >= 0)
      {
         if (val < 0x00000040)
         {
            write7 (val, sink);
            return 1;
         }
         else if (val < 0x00002000)
         {
            write14 (val, sink);
            return 2;
         }
         else if (val < 0x00008000)
         {
            write16 (val, sink);
            return 3;
         }
         else if (val < 0x00800000)
         {
            write24 (val, sink);
            return 4;
         }
         else
         {
            write32 (val, sink);
            return 5;
         }
      }
      else
         return writeNegative (val, sink);
   }

   public static int writeU64 (long val, ByteSink sink)
   {
      if (val < 0)
      {
         write64 (val, sink);
         return 9;
      }
      else if (val < 0x0000000000000080L)
      {
         write7 ((int)val, sink);
         return 1;
      }
      else if (val < 0x0000000000004000L)
      {
         write14 ((int)val, sink);
         return 2;
      }
      else if (val < 0x0000000000010000L)
      {
         write16 ((int)val, sink);
         return 3;
      }
      else if (val < 0x0000000001000000L)
      {
         write24 ((int)val, sink);
         return 4;
      }
      else if (val < 0x0000000100000000L)
      {
         write32 ((int)val, sink);
         return 5;
      }
      else if (val < 0x0000010000000000L)
      {
         write40 (val, sink);
         return 6;
      }
      else if (val < 0x0001000000000000L)
      {
         write48 (val, sink);
         return 7;
      }
      else if (val < 0x0100000000000000L)
      {
         write56 (val, sink);
         return 8;
      }
      else
      {
         write64 (val, sink);
         return 9;
      }
   }

   private static int writeNegative (long val, ByteSink sink)
   {
      if (val >= -64)
      {
         write7 ((int)val, sink);
         return 1;
      }
      else if (val >= -8192)
      {
         write14 ((int)val, sink);
         return 2;
      }
      else if (val >= -32768)
      {
         write16 ((int)val, sink);
         return 3;
      }
      else if (val >= -8388608)
      {
         write24 ((int)val, sink);
         return 4;
      }
      else if (val >= -2147483648L)
      {
         write32 ((int)val, sink);
         return 5;
      }
      else if (val >= -549755813888L)
      {
         write40 (val, sink);
         return 6;
      }
      else if (val >= -140737488355328L)
      {
         write48 (val, sink);
         return 7;
      }
      else if (val >= -36028797018963968L)
      {
         write56 (val, sink);
         return 8;
      }
      else
      {
         write64 (val, sink);
         return 9;
      }
   }
   
   public static int writeI64 (long val, ByteSink sink)
   {
      if (val >= 0)
      {
         if (val < 0x00000040L)
         {
            write7 ((int)val, sink);
            return 1;
         }
         else if (val < 0x0000000000002000L)
         {
            write14 ((int)val, sink);
            return 2;
         }
         else if (val < 0x0000000000008000L)
         {
            write16 ((int)val, sink);
            return 3;
         }
         else if (val < 0x0000000000800000L)
         {
            write24 ((int)val, sink);
            return 4;
         }
         else if (val < 0x0000000080000000L)
         {
            write32 ((int)val, sink);
            return 5;
         }
         else if (val < 0x0000008000000000L)
         {
            write40 (val, sink);
            return 6;
         }
         else if (val < 0x0000800000000000L)
         {
            write48 (val, sink);
            return 7;
         }
         else if (val < 0x0080000000000000L)
         {
            write56 (val, sink);
            return 8;
         }
         else
         {
            write64 (val, sink);
            return 9;
         }
      }
      else
         return writeNegative (val, sink);
   }

   public static byte readU8 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return (byte)b;
      }
      else if ((b & 0x40) == 0)
      {
         byte val = (byte)((src.get (1) << 6) | (b & 0x3f));
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 1)
            throw overflowError ("u8", src);
         int val = src.get (1);
         src.step (2);
         return (byte)val;
      }
   }

   public static short readU16 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return (short)b;
      }
      else if ((b & 0x40) == 0)
      {
         short val = (short)((src.get (1) << 6) | (b & 0x3f));
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 2)
            throw overflowError ("u16", src);
         int val = 0;
         for (int i = 0; i < w; ++ i)
            val |= src.get (i + 1) << (i << 3);
         src.step (w + 1);
         return (short)val;
      }
   }

   public static int readU32 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return b;
      }
      else if ((b & 0x40) == 0)
      {
         int val = (src.get (1) << 6) | (b & 0x3f);
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 4)
            throw overflowError ("u32", src);
         int val = 0;
         for (int i = 0; i < w; ++ i)
            val |= src.get (i + 1) << (i << 3);
         src.step (w + 1);
         return val;
      }
   }

   public static long readU64 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return b;
      }
      else if ((b & 0x40) == 0)
      {
         long val = (src.get (1) << 6) | (b & 0x3f);
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 8)
            throw overflowError ("u64", src);
         long val = 0;
         for (int i = 0; i < w; ++ i)
            val |= (long)src.get (i + 1) << (i << 3);
         src.step (w + 1);
         return val;
      }
   }

   public static byte readI8 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return (byte)((b << 25) >> 25);
      }
      else if ((b & 0x40) == 0)
      {
         byte val = (byte)((src.get (1) << 6) | (b & 0x3f));
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 1)
            throw overflowError ("i8", src);
         int val = src.get (1);
         src.step (2);
         return (byte)val;
      }
   }

   public static short readI16 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return (short)((b << 25) >> 25);
      }
      else if ((b & 0x40) == 0)
      {
         short val = (short)((((src.get (1) << 6) | (b & 0x3f)) << 18) >> 18);
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 2)
            throw overflowError ("i16", src);
         int val = 0;
         for (int i = 0; i < w; ++ i)
            val |= src.get (i + 1) << (i << 3);
         src.step (w + 1);
         int bits = (4 - w) << 3;
         return (short)((val << bits) >> bits);
      }
   }

   public static int readI32 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return (b << 25) >> 25;
      }
      else if ((b & 0x40) == 0)
      {
         int val = (((src.get (1) << 6) | (b & 0x3f)) << 18) >> 18;
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 4)
            throw overflowError ("i32", src);
         int val = 0;
         for (int i = 0; i < w; ++ i)
            val |= src.get (i + 1) << (i << 3);
         src.step (w + 1);
         int bits = (4 - w) << 3;
         return (val << bits) >> bits;
      }
   }
   
   public static long readI64 (ByteSource src) throws BlinkException.Decode
   {
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return ((long)b << 57) >> 57;
      }
      else if ((b & 0x40) == 0)
      {
         long val =
            (((long)(src.get (1) << 6) | (long)(b & 0x3f)) << 50) >> 50;
         src.step (2);
         return val;
      }
      else
      {
         int w = b & 0x3f;
         if (w > 8)
            throw overflowError ("i64", src);
         long val = 0;

         for (int i = 0; i < w; ++ i)
            val |= (long)src.get (i + 1) << (i << 3);
 
         src.step (w + 1);
         int bits = (8 - w) << 3;
         return (val << bits) >> bits;
      }
   }
   
   public static BlinkException.Decode overflowError (
      String type, ByteSource src)
   {
      return new BlinkException.Decode (
         "VLC entity overflow (" + type + ")", src);
   }
}
