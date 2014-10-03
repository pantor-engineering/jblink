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
   The {@code CompactWriter} implements an encoder for the Blink
   compact binary format. It encodes POJOs to bytes that are written
   to an {@code OutputStream}. The mapping from POJOs to the
   corresponding messages in the Blink schema is handled through an
   {@code ObjectModel}.
*/

public final class CompactWriter implements Writer
{
   /**
      Creates a writer for the compact binary format. It writes
      encoded messages to the specified {@code ByteSink}.

      @param om an object model
      @param sink a sink that will receive the encoded bytes
   */
   
   public CompactWriter (ObjectModel om, ByteSink sink)
   {
      compiler = new CompactWriterCompiler (om);
      this.sink = sink;
   }

   /**
      Creates a writer for the compact binary format. It writes
      encoded messages to the specified {@code OutputStream}. This
      writer handles its own buffering so the specified {@code
      OutputStream} need not be buffered in itself.

      @param om an object model
      @param os an output stream that will receive the encoded bytes
   */
   
   public CompactWriter (ObjectModel om, OutputStream os)
   {
      this (om, new OutputStreamSink (os));
   }

   /**
      Encodes an object. It flushes the underlying sink if
      necessary but you should call the {@code flush} method
      explicitly if you require a flush to the output stream after
      this write call.

      @param o the object to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there was an output error
   */

   @Override
   public void write (Object o) throws BlinkException, IOException
   {
      if (sink.getPos () >= AutoFlushThreshold)
         flush ();
      writeObject (o);
   }

   /**
      Encodes an array of objects. It flushes the underlying sink if
      necessary but you should call the {@code flush} method
      explicitly if you require a flush to the output stream after
      this write call.

      @param objs the objects to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there was an output error
   */
   
   @Override
   public void write (Object [] objs) throws BlinkException, IOException
   {
      for (Object o : objs)
         write (o);
   }

   /**
      Encodes a slice of an array of objects. It flushes the
      underlying sink if necessary but you should call the {@code
      flush} method explicitly if you require a flush to the output
      stream after this write call.

      @param objs the objects to write
      @param from the index of the first object to encode
      @param len the number of objects to encode
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there was an output error
   */
   
   @Override
   public void write (Object [] objs, int from, int len)
      throws BlinkException, IOException
   {
      for (int i = from; i < from + len; ++ i)
         write (objs [i]);
   }

   /**
      Encodes an iterable collection of objects. It flushes the
      underlying sink if necessary but you should call the {@code
      flush} method explicitly if you require a flush to the output
      stream after this write call.

      @param objs the objects to write
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there was an output error
   */
   
   @Override
   public void write (Iterable<?> objs) throws BlinkException, IOException
   {
      for (Object o : objs)
         write (o);
   }

   /**
      Flushes any pending encoded messages in the underlying sink

      @throws IOException if there was an output error
   */
   
   @Override
   public void flush () throws IOException
   {
      sink.flush ();
   }

   /**
      Flushes any pending encoded messages and closes the underlying sink

      @throws IOException if there was an output error
   */
   
   @Override
   public void close () throws IOException
   {
      sink.close ();
   }

   // Primitive values
   //////////////////////////////////////////////////////////////////////
   
   public static void writeU8 (byte val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 ((int)val, sink);
   }

   public static void writeI8 (byte val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI32 ((int)val, sink);
   }

   public static void writeU16 (short val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 ((int)val, sink);
   }

   public static void writeI16 (short val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI32 ((int)val, sink);
   }

   public static void writeU32 (int val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val, sink);
   }

   public static void writeI32 (int val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI32 (val, sink);
   }

   public static void writeU64 (long val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU64 (val, sink);
   }

   public static void writeI64 (long val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI64 (val, sink);
   }

   public static void writeFixedDec (long val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI64 (val, sink);
   }

   public static void writeF64 (double val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU64 (Double.doubleToLongBits (val), sink);
   }

   public static void writeEnumVal (Integer val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI32 (val != null ? val.intValue () : 0, sink);
   }
   
   public static void writeDecimal (Decimal val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI32 ((int)val.getExponent (), sink);
      Vlc.writeI64 (val.getMantissa (), sink);
   }

   public static void writeDate (int val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val, sink);
   }

   public static void writeTimeOfDayMilli (int val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val, sink);
   }

   public static void writeTimeOfDayNano (long val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU64 (val, sink);
   }

   public static void writeNanotime (long val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI64 (val, sink);
   }

   public static void writeMillitime (long val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeI64 (val, sink);
   }

   public static void writeBool (boolean val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val ? 1 : 0, sink);
   }

   public static void writeString (String val, ByteSink sink)
      throws BlinkException.Encode
   {
      // Optimize for ASCII strings shorter than 128 UTF-8 encoded bytes
      
      int start = sink.getPos ();
      int len = val.length ();
      int allocatedPreamble = 0;
      int i = 0;
      int size = 0;

     LONG:
      if (len < 128)
      {
         allocatedPreamble = 1;
         reserve (sink, allocatedPreamble + len);
         sink.step ();
         for (; i < len; ++ i)
         {
            char c = val.charAt (i);
            if (c < 0x0080)
               sink.write (c);
            else
            {
               size = i;
               reserve (sink, Utf8Util.getConservativeSize (len - i));
               break LONG;
            }
         }

         int save = sink.getPos ();
         sink.setPos (start);
         Vlc.write7 (len, sink);
         sink.setPos (save);
         
         return;
      }
      else
      {
         allocatedPreamble = 2;
         reserve (sink, allocatedPreamble + Utf8Util.getConservativeSize (len));
         sink.step (allocatedPreamble);
      }

      size += Utf8Util.write (val, i, sink);

      int toShift = Vlc.getUintSize (size) - allocatedPreamble;
      if (toShift > 0)
         sink.shift (start + allocatedPreamble, toShift);
      int save = sink.getPos ();
      sink.setPos (start);
      Vlc.writeU32 (size, sink);
      sink.setPos (save);
   }

   public static void writeBinary (byte [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length);
      sink.write (val);
   }

   public static void writeFixed (byte [] val, int fixedSize, ByteSink sink)
      throws BlinkException.Encode
   {
      reserve (sink, fixedSize);
      if (val.length >= fixedSize)
         sink.write (val, 0, fixedSize);
      else
      {
         sink.write (val);
         for (int i = 0, pad = fixedSize - val.length; i < pad; ++ i)
            sink.write (0);
      }
   }

   public static void writeU8Array (byte [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int8MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU32 ((int)val [i], sink);
   }

   public static void writeI8Array (byte [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int8MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI32 ((int)val [i], sink);
   }

   public static void writeU16Array (short [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int16MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU32 ((int)val [i], sink);
   }

   public static void writeI16Array (short [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int16MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI32 ((int)val [i], sink);
   }

   public static void writeU32Array (int [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU32 (val [i], sink);
   }

   public static void writeI32Array (int [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI32 (val [i], sink);
   }

   public static void writeU64Array (long [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU64 (val [i], sink);
   }

   public static void writeI64Array (long [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI64 (val [i], sink);
   }

   public static void writeFixedDecArray (long [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      writeI64Array (val, sink);
   }

   public static void writeF64Array (double [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI64 (Double.doubleToLongBits (val [i]), sink);
   }

   public static void writeDecimalArray (Decimal [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * (Vlc.Int8MaxSize + Vlc.Int64MaxSize));
      for (int i = 0; i < val.length; ++ i)
         writeDecimal (val [i], sink);
   }

   public static void writeDateArray (int [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU32 (val [i], sink);
   }

   public static void writeTimeOfDayMilliArray (int [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int32MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU32 (val [i], sink);
   }

   public static void writeTimeOfDayNanoArray (long [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeU64 (val [i], sink);
   }

   public static void writeNanotimeArray (long [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI64 (val [i], sink);
   }

   public static void writeMillitimeArray (long [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length * Vlc.Int64MaxSize);
      for (int i = 0; i < val.length; ++ i)
         Vlc.writeI64 (val [i], sink);
   }

   public static void writeBoolArray (boolean [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      reserve (sink, val.length);
      for (int i = 0; i < val.length; ++ i)
         writeBool (val [i], sink);
   }

   public static void writeStringArray (String [] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      for (int i = 0; i < val.length; ++ i)
         writeString (val [i], sink);
   }

   public static void writeBinaryArray (byte [][] val, ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      for (int i = 0; i < val.length; ++ i)
         writeBinary (val [i], sink);
   }

   public static void writeFixedArray (byte [][] val, int fixedSize,
                                       ByteSink sink)
      throws BlinkException.Encode
   {
      Vlc.writeU32 (val.length, sink);
      for (int i = 0; i < val.length; ++ i)
         writeFixed (val [i], fixedSize, sink);
   }

   public void writeObjectArray (Object [] val) throws BlinkException
   {
      Vlc.writeU32 (val.length, sink);
      for (int i = 0; i < val.length; ++ i)
         writeObject (val [i]);
   }
   
   public static void writeSeqSize (Object [] val, ByteSink sink)
   {
      Vlc.writeU32 (val.length, sink);
   }
   
   public static void writeNull (ByteSink sink) throws BlinkException.Encode
   {
      sink.write (Vlc.Null);
   }

   public static void writeOne (ByteSink sink) throws BlinkException.Encode
   {
      sink.write (1);
   }

   public static void writeZero (ByteSink sink) throws BlinkException.Encode
   {
      sink.write (0);
   }

   public void writeObject (Object o) throws BlinkException
   {
      Encoder enc = null;
      
      try
      {
         enc = compiler.getEncoder (o.getClass ());
         enc.requireTid ();
         reserve (sink, enc.getTidSize () + 2 /* Size preamble */);
         sink.step (2); // Reserve space for a two byte length preamble
         int start = sink.getPos ();
         enc.encode (o, sink, this);
         int end = sink.getPos ();
         int size = end - start;
         if (size <= Vlc.TwoByteUintMax)
         {
            sink.setPos (start - 2);
            Vlc.write14 (size, sink);
            sink.setPos (end);
         }
         else
         {
            int width = Vlc.getUintSize (size);
            int toShift = width - 2;
            reserve (sink, toShift);
            sink.shift (start, toShift);
            sink.setPos (start - 2);
            Vlc.writeU32 (size, sink);
            sink.setPos (end + toShift);
         }
      }
      catch (BlinkException.Encode e)
      {
         // FIXME: augment
         throw e;
      }
   }

   private static final int AutoFlushThreshold = 4096 - 256;

   //////////////////////////////////////////////////////////////////////

   public abstract static class Encoder
   {
      protected Encoder (byte [] tid, Class<?> type, Schema.Group grp)
      {
         this.tid = tid;
         this.type = type;
         this.grp = grp;
      }

      protected abstract void encode (Object o, ByteSink sink, CompactWriter wr)
         throws BlinkException.Encode, BlinkException.Binding;
      
      public int getTidSize () { return tid.length; }

      public void requireTid () throws BlinkException
      {
         if (tid == null)
            throw new BlinkException ("No type identifier specified for " +
                                      grp.getName () +
                                      " when encoding into compact binary");
      }
      
      protected final byte [] tid;
      private final Class<?> type;
      private final Schema.Group grp;
   }

   private static void reserve (ByteSink sink, int size)
      throws BlinkException.Encode
   {
      try
      {
         sink.reserve (size);
      }
      catch (IOException e)
      {
         throw new BlinkException.Encode (e);
      }
   }
   
   private final ByteSink sink;
   private final CompactWriterCompiler compiler;
}
