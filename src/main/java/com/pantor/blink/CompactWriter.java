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
import java.io.UnsupportedEncodingException;

/**
   The {@code CompactWriter} implements an encoder for the Blink
   compact binary format. It encodes POJOs to bytes that are written
   to an {@code OutputStream}. The mapping from POJOs to the
   corresponding messages in the Blink schema is handled through an
   {@code ObjectModel}.
*/

public final class CompactWriter
{
   /**
      Creates a writer for the compact binary format. It writes
      encoded messages to the specified {@code OutputStream}. This
      writer handles its own buffering so the specified {@code
      OutputStream} should not be buffered in itself.

      @param om an object model
      @param os an output stream that will receive the encoded bytes
   */
   
   public CompactWriter (ObjectModel om, OutputStream os)
   {
      compiler = new CompactWriterCompiler (om);
      this.os = os;
   }

   /**
      Encodes an object. It flushes the underlying buffer if
      necessary but you should call the {@code flush} method
      explicitly if you require a flush to the output stream after
      this write call.

      @param o the object to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   public void write (Object o) throws BlinkException, IOException
   {
      if (buf.getPos () >= AutoFlushThreshold)
	 flush ();
      writeObject (o);
   }

   /**
      Encodes an array of objects. It flushes the underlying buffer if
      necessary but you should call the {@code flush} method
      explicitly if you require a flush to the output stream after
      this write call.

      @param objs the objects to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   public void write (Object [] objs)
      throws BlinkException, IOException
   {
      for (Object o : objs)
	 write (o);
   }

   /**
      Encodes a slice of an array of objects. It flushes the
      underlying buffer if necessary but you should call the {@code
      flush} method explicitly if you require a flush to the output
      stream after this write call.

      @param objs the objects to write
      @param from the index of the first object to encode
      @param len the number of objects to encode
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is an output error
   */
   
   public void write (Object [] objs, int from, int len)
      throws BlinkException, IOException
   {
      for (int i = from; i < from + len; ++ i)
	 write (objs [i]);
   }

   /**
      Flushes any pending encoded messages in the interal buffer to
      the output stream. It also flushes the underlying output stream.

      @throws IOException if there is an output error
   */
   
   public void flush () throws IOException
   {
      buf.flushTo (os);
      os.flush ();
   }

   /**
      Flushes any pending encoded messages in the interal buffer to
      the output stream and then closes the underlying output stream.

      @throws IOException if there is an output error
   */
   
   public void close () throws IOException
   {
      buf.flushTo (os);
      os.close ();
   }

   // Primitive values
   //////////////////////////////////////////////////////////////////////
   
   public static void writeU8 (byte val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeU32 ((int)val, buf);
   }

   public static void writeI8 (byte val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeI32 ((int)val, buf);
   }

   public static void writeU16 (short val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeU32 ((int)val, buf);
   }

   public static void writeI16 (short val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeI32 ((int)val, buf);
   }

   public static void writeU32 (int val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeU32 (val, buf);
   }

   public static void writeI32 (int val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeI32 (val, buf);
   }

   public static void writeU64 (long val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeU64 (val, buf);
   }

   public static void writeI64 (long val, Buf buf) throws BlinkException.Encode
   {
      Vlc.writeI64 (val, buf);
   }

   public static void writeF64 (double val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU64 (Double.doubleToLongBits (val), buf);
   }

   public static void writeEnumVal (Integer val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeI32 (val != null ? val.intValue () : 0, buf);
   }
   
   public static void writeDecimal (Decimal val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeI32 ((int)val.getExponent (), buf);
      Vlc.writeI64 (val.getMantissa (), buf);
   }

   public static void writeDate (int val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val, buf);
   }

   public static void writeTimeOfDayMilli (int val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val, buf);
   }

   public static void writeTimeOfDayNano (long val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU64 (val, buf);
   }

   public static void writeNanotime (long val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeI64 (val, buf);
   }

   public static void writeMillitime (long val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeI64 (val, buf);
   }

   public static void writeBool (boolean val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val ? 1 : 0, buf);
   }

   public static void writeString (String val, Buf buf)
      throws BlinkException.Encode
   {
      try
      {
	 byte [] utf8 = val.getBytes ("UTF-8");
	 buf.reserve (utf8.length + Vlc.Int32MaxSize);
	 Vlc.writeU32 (utf8.length, buf);
	 buf.write (utf8);
      }
      catch (UnsupportedEncodingException e)
      {
	 // FIXME: Should we raise BlinkException.Encode instead?
	 throw new RuntimeException (e);
      }
   }

   public static void writeU8Array (byte [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int8MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU32 ((int)val [i], buf);
   }

   public static void writeI8Array (byte [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int8MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI32 ((int)val [i], buf);
   }

   public static void writeU16Array (short [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int16MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU32 ((int)val [i], buf);
   }

   public static void writeI16Array (short [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int16MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI32 ((int)val [i], buf);
   }

   public static void writeU32Array (int [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU32 (val [i], buf);
   }

   public static void writeI32Array (int [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI32 (val [i], buf);
   }

   public static void writeU64Array (long [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU64 (val [i], buf);
   }

   public static void writeI64Array (long [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI64 (val [i], buf);
   }

   public static void writeF64Array (double [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI64 (Double.doubleToLongBits (val [i]), buf);
   }

   public static void writeDecimalArray (Decimal [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * (Vlc.Int8MaxSize + Vlc.Int64MaxSize));
      for (int i = 0; i < val.length; ++ i)
	 writeDecimal (val [i], buf);
   }

   public static void writeDateArray (int [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU32 (val [i], buf);
   }

   public static void writeTimeOfDayMilliArray (int [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU32 (val [i], buf);
   }

   public static void writeTimeOfDayNanoArray (long [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeU64 (val [i], buf);
   }

   public static void writeNanotimeArray (long [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI64 (val [i], buf);
   }

   public static void writeMillitimeArray (long [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
	 Vlc.writeI64 (val [i], buf);
   }

   public static void writeBoolArray (boolean [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      buf.reserve (val.length);
      for (int i = 0; i < val.length; ++ i)
	 writeBool (val [i], buf);
   }

   public static void writeStringArray (String [] val, Buf buf)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, buf);
      for (int i = 0; i < val.length; ++ i)
	 writeString (val [i], buf);
   }

   public void writeObjectArray (Object [] val) throws BlinkException
   {
      Vlc.writeU32 (val.length, buf);
      for (int i = 0; i < val.length; ++ i)
	 writeObject (val [i]);
   }
   
   public static void writeSeqSize (Object [] val, Buf buf)
   {
      Vlc.writeU32 (val.length, buf);
   }
   
   public static void writeNull (Buf buf) throws BlinkException.Encode
   {
      buf.write (Vlc.Null);
   }

   public static void writeOne (Buf buf) throws BlinkException.Encode
   {
      buf.write (1);
   }

   public static void writeZero (Buf buf) throws BlinkException.Encode
   {
      buf.write (0);
   }

   private static final int TwoBytePreambleMax = (1 << 14) - 1;

   public void writeObject (Object o) throws BlinkException
   {
      Encoder enc = null;
      
      try
      {
	 enc = compiler.getEncoder (o.getClass ());
	 buf.reserve (enc.getMinSize () + 2 /* Size preamble */);
	 buf.step (2); // Reserve space for a two byte length preamble
	 int start = buf.getPos ();
	 enc.encode (o, buf, this);
	 int end = buf.getPos ();
	 int size = end - start;
	 if (size <= TwoBytePreambleMax)
	 {
	    buf.setPos (start - 2);
	    Vlc.write14 (size, buf);
	    buf.setPos (end);
	 }
	 else
	 {
	    // FIXME
	    throw new RuntimeException ("Not implemented yet: large messages");
	 }
      }
      catch (BlinkException.Encode e)
      {
	 // FIXME: augment
	 throw e;
      }
   }

   private static final int AutoFlushThreshold = 4096;

   //////////////////////////////////////////////////////////////////////

   public abstract static class Encoder
   {
      protected Encoder (byte [] tid, int minSize, Class<?> type,
			 Schema.Group grp)
      {
	 this.tid = tid;
	 this.minSize = minSize + (tid != null ? tid.length : 0);
	 this.type = type;
	 this.grp = grp;
      }

      public int getMinSize () { return minSize; }

      protected abstract void encode (Object o, Buf buf, CompactWriter wr)
	 throws BlinkException.Encode, BlinkException.Binding;
      
      protected final byte [] tid;
      private final int minSize;
      private final Class<?> type;
      private final Schema.Group grp;
   }

   private final Buf buf = DirectBuf.newInstance ();
   private final CompactWriterCompiler compiler;
   private final OutputStream os;
}
