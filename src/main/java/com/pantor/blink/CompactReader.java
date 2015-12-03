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
   The {@code CompactReader} implements a decoder for the Blink
   compact binary format. In its most basic form it reads bytes from
   an instance of {@link ByteSource} and turns them into POJOs as
   defined by an {@code ObjectModel}. The result can either be
   appended to a {@link Block} or dispatched to an observer as
   specified by an {@link ObserverRegistry}, or both.

   <p>The reader supports incremental decoding in the sense that you do
   not have to worry about messages boundaries when you pass bytes to
   any of the {@code read} methods. If a byte source ends in a partial
   message, the reader will continue decoding that message when you
   supply the rest of the bytes in subsequent calls.</p>
 */

public final class CompactReader implements Reader
{
   /**
      The default maximum message size is 500M
   */
   
   public static final int DefaultMaxMsgSize = 500000000;

   /**
      Creates a reader for the compact binary format. It maps decoded
      messages POJOs as defined by the specified object model.

      @param om an object model
   */
   
   public CompactReader (ObjectModel om)
   {
      compiler = new CompactReaderCompiler (om);
      maxMsgSize = DefaultMaxMsgSize;
   }

   /**
      Creates a reader for the compact binary format. It maps decoded
      messages to POJOs as defined by the specified object model and
      dispatches them to any matching observers if available in the
      specified observer registry.

      @param om an object model
      @param oreg an observer registry
    */
   
   public CompactReader (ObjectModel om, ObserverRegistry oreg)
   {
      compiler = new CompactReaderCompiler (om, oreg);
      maxMsgSize = DefaultMaxMsgSize;
   }

   /**
      Decodes bytes specified in a byte array. It dispatches decoded
      messages to any matching observers if an observer registry has
      been specified.

      @param data the bytes to decode
      @throws BlinkException if a decoding, schema or binding problem occurs
   */

   @Override
   public void read (byte [] data) throws BlinkException
   {
      read (new ByteBuf (data));
   }

   /**
      Decodes bytes specified in a slice of a byte array. It
      dispatches decoded messages to any matching observers if an
      observer registry has been specified.

      @param data the bytes to decode
      @param from the index of the first byte to decode
      @param len the number of bytes to decode
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   @Override
   public void read (byte [] data, int from, int len) throws BlinkException
   {
      read (new ByteBuf (data, from, len));
   }

   /**
      Decodes bytes specified in a byte array. It appends decoded
      messages to the specified block and also dispatches them to any
      matching observers if an observer registry has been specified.

      <p>It also allocates objects as needed from the specified block.</p>

      @param data the bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   @Override
   public void read (byte [] data, Block block) throws BlinkException
   {
      read (new ByteBuf (data), block);
   }
   
   /**
      Decodes bytes specified in a slice of a byte array. It appendes
      decoded messages to the specified block and also dispatches them
      to any matching observers if an observer registry has been
      specified.

      <p>It also allocates objects as needed from the specified block.</p>

      @param data the bytes to decode
      @param from the index of the first byte to decode
      @param len the number of bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   @Override
   public void read (byte [] data, int from, int len, Block block)
      throws BlinkException
   {
      read (new ByteBuf (data, from, len), block);
   }

   /**
      Decodes bytes read from the specified byte source. It dispatches
      decoded messages to any matching observers if an observer
      registry has been specified.

      @param src the bytes to decode
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   @Override
   public void read (ByteSource src) throws BlinkException
   {
      read (src, blankBlock);
   }

   /**
      Decodes bytes read from the specified byte source. It appends decoded
      messages to the specified block and also dispatches them to any
      matching observers if an observer registry has been specified.

      <p>It also allocates objects as needed from the specified block.</p>

      <p>This is the most native form of the {@code read} methods. All
      other read methods will create temporary {@code ByteSource}
      objects and/or use a private {@link DefaultBlock} instance
      managed by this reader.</p>
      
      @param src the bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a decoding, schema or binding problem occurs
   */
   
   @Override
   public void read (ByteSource src, Block block) throws BlinkException
   {
      curBlock = block;
      
      try
      {
         if (missingData > 0)
         {
            if (fillPendData (src))
            {
               pendData.flip ();
               readMsg (pendData, pendData.size ());
               pendData.release (MaxLingeringScratchArea);
            }
            else
               return;
         }

         if (missingMsgSizeBytes > 0)
            if (! readOrSuspendMsg (src, fillPendMsgSize (src)))
               return;
   
         for (;;)
            if (! readOrSuspendMsg (src, readMsgSize (src)))
               return;
      }
      catch (IOException e)
      {
         throw error (e.getMessage (), pendData);
      }
      catch (BlinkException.Decode e)
      {
         throw error (e.getMessage (), e.getContext ());
      }
   }

   /**
      Sets the maximum message size. This reader will throw an
      exception if the maxium message size is exceeded.

      @param maxMsgSize maximum msg size in bytes
   */

   public void setMaxMessageSize (long maxMsgSize)
   {
      this.maxMsgSize = maxMsgSize;
   }

   /**
      Enables or disables in-band schema exchange. Disabled by default

      @param useSchemaExchange {@code true} enables and {@code false} disables
   */

   public void setUseSchemaExchange (boolean useSchemaExchange)
      throws BlinkException
   {
      compiler.setUseSchemaExchange (useSchemaExchange);
   }
   
   /**
      Returns {@code true} if there is no partial message pending

      @return {@code true} if there is no partial message pending
   */

   @Override
   public boolean isComplete ()
   {
      return missingData == 0 && missingMsgSizeBytes == 0;
   }

   /**
      Closes this reader

      @throws BlinkException.Decode if this reader is incomplete as
      indicated by {@code isComplete}
   */

   @Override
   public void close () throws BlinkException.Decode
   {
      if (! isComplete ())
         throw new BlinkException.Decode (
            "Incomplete compact blink message. The reader needs " +
            "more data to finish an incomplete trailing " +
            "message");
   }

   /**
      Resets this reader to an initial state, discarding any incompletely
      read messages
   */

   @Override
   public void reset ()
   {
      missingData = 0;
      missingMsgSizeBytes = 0;
      pendData.clear ();
      pendMsgSizePreamble.clear ();
   }
   
   // Primitive values
   //////////////////////////////////////////////////////////////////////
   
   public static byte readU8 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readU8 (src);
   }

   public static byte readI8 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readI8 (src);
   }

   public static short readU16 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readU16 (src);
   }

   public static short readI16 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readI16 (src);
   }

   public static int readU32 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readU32 (src);
   }

   public static int readI32 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readI32 (src);
   }
   
   public static long readU64 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readU64 (src);
   }

   public static long readI64 (ByteSource src) throws BlinkException.Decode
   {
      return Vlc.readI64 (src);
   }

   public static double readF64 (ByteSource src) throws BlinkException.Decode
   {
      return Double.longBitsToDouble (Vlc.readU64 (src));
   }

   public static Decimal readDecimal (ByteSource src)
      throws BlinkException.Decode
   {
      byte exp = Vlc.readI8 (src);
      long sig = Vlc.readI64 (src);
      return Decimal.valueOf (sig, exp);
   }

   public static long readFixedDec (ByteSource src)
      throws BlinkException.Decode
   {
      return readI64 (src);
   }

   public static FixedDec readFixedDec_0 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._0 (readI64 (src));
   }

   public static FixedDec readFixedDec_1 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._1 (readI64 (src));
   }

   public static FixedDec readFixedDec_2 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._2 (readI64 (src));
   }

   public static FixedDec readFixedDec_3 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._3 (readI64 (src));
   }

   public static FixedDec readFixedDec_4 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._4 (readI64 (src));
   }

   public static FixedDec readFixedDec_5 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._5 (readI64 (src));
   }

   public static FixedDec readFixedDec_6 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._6 (readI64 (src));
   }

   public static FixedDec readFixedDec_7 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._7 (readI64 (src));
   }

   public static FixedDec readFixedDec_8 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._8 (readI64 (src));
   }

   public static FixedDec readFixedDec_9 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._9 (readI64 (src));
   }

   public static FixedDec readFixedDec_10 (ByteSource src)
      throws BlinkException.Decode
   {
      return new FixedDec._10 (readI64 (src));
   }

   public static FixedDec readFixedDec_N (ByteSource src, int scale)
      throws BlinkException.Decode
   {
      return new FixedDec._N (readI64 (src), scale);
   }

   public static int readDate (ByteSource src)
      throws BlinkException.Decode
   {
      return Vlc.readU32 (src);
   }

   public static int readTimeOfDayMilli (ByteSource src)
      throws BlinkException.Decode
   {
      return Vlc.readU32 (src);
   }

   public static long readTimeOfDayNano (ByteSource src)
      throws BlinkException.Decode
   {
      return Vlc.readU64 (src);
   }

   public static long readNanotime (ByteSource src)
      throws BlinkException.Decode
   {
      return Vlc.readI64 (src);
   }

   public static long readMillitime (ByteSource src)
      throws BlinkException.Decode
   {
      return Vlc.readI64 (src);
   }

   public static boolean readBool (ByteSource src)
      throws BlinkException.Decode
   {
      return Vlc.readU8 (src) != 0;
   }

   public static String readString (ByteSource src)
      throws BlinkException.Decode
   {
      return src.readUtf8String (Vlc.readU32 (src));
   }

   public static byte [] readBinary (ByteSource src)
      throws BlinkException.Decode
   {
      int len = Vlc.readU32 (src);
      byte [] val = new byte [len];
      src.read (val);
      return val;
   }

   public static byte [] readFixed (ByteSource src, int len)
      throws BlinkException.Decode
   {
      byte [] val = new byte [len];
      src.read (val);
      return val;
   }

   public static byte [] readU8Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      byte [] v = new byte [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU8 (src);
      return v;
   }

   public static byte [] readI8Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      byte [] v = new byte [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readI8 (src);
      return v;
   }

   public static short [] readU16Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      short [] v = new short [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU16 (src);
      return v;
   }

   public static short [] readI16Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      short [] v = new short [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readI16 (src);
      return v;
   }

   public static int [] readU32Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU32 (src);
      return v;
   }

   public static int [] readI32Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readI32 (src);
      return v;
   }
   
   public static long [] readU64Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU64 (src);
      return v;
   }

   public static long [] readI64Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readI64 (src);
      return v;
   }

   public static long [] readFixedDecArray (ByteSource src)
      throws BlinkException.Decode
   {
      return readI64Array (src);
   }

   public static FixedDec [] readFixedDecArray_0 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._0 [] v = new FixedDec._0 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._0 (Vlc.readI64 (src));
      return v;
   }

   public static FixedDec [] readFixedDecArray_1 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._1 [] v = new FixedDec._1 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._1 (Vlc.readI64 (src));
      return v;
   }

   public static FixedDec [] readFixedDecArray_2 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._2 [] v = new FixedDec._2 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._2 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_3 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._3 [] v = new FixedDec._3 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._3 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_4 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._4 [] v = new FixedDec._4 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._4 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_5 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._5 [] v = new FixedDec._5 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._5 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_6 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._6 [] v = new FixedDec._6 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._6 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_7 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._7 [] v = new FixedDec._7 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._7 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_8 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._8 [] v = new FixedDec._8 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._8 (Vlc.readI64 (src));
      return v;

   }

   public static FixedDec [] readFixedDecArray_9 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._9 [] v = new FixedDec._9 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._9 (Vlc.readI64 (src));
      return v;
   }

   public static FixedDec [] readFixedDecArray_10 (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._10 [] v = new FixedDec._10 [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._10 (Vlc.readI64 (src));
      return v;
   }

   public static FixedDec [] readFixedDecArray_N (ByteSource src, int scale)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      FixedDec._N [] v = new FixedDec._N [size];
      for (int i = 0; i < size; ++ i)
         v [i] = new FixedDec._N (Vlc.readI64 (src), scale);
      return v;
   }
   
   public static double [] readF64Array (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      double [] v = new double [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Double.longBitsToDouble (Vlc.readU64 (src));
      return v;
   }
   
   public static Decimal [] readDecimalArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      Decimal [] v = new Decimal [size];
      for (int i = 0; i < size; ++ i)
         v [i] = readDecimal (src);
      return v;
   }

   public static int [] readDateArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU32 (src);
      return v;
   }

   public static int [] readTimeOfDayMilliArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU32 (src);
      return v;
   }

   public static long [] readTimeOfDayNanoArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readU64 (src);
      return v;
   }

   public static long [] readNanotimeArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readI64 (src);
      return v;
   }

   public static long [] readMillitimeArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
         v [i] = Vlc.readI64 (src);
      return v;
   }

   public static boolean [] readBoolArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      boolean [] v = new boolean [size];
      for (int i = 0; i < size; ++ i)
         v [i] = readBool (src);
      return v;
   }

   public static String [] readStringArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      String [] v = new String [size];
      for (int i = 0; i < size; ++ i)
         v [i] = readString (src);
      return v;
   }
   
   public static byte [][] readBinaryArray (ByteSource src)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      byte [][] v = new byte [size][];
      for (int i = 0; i < size; ++ i)
         v [i] = readBinary (src);
      return v;
   }

   public static byte [][] readFixedArray (ByteSource src, int fixedSize)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (src);
      byte [][] v = new byte [size][];
      for (int i = 0; i < size; ++ i)
         v [i] = readFixed (src, fixedSize);
      return v;
   }
   
   public Object [] readObjectArray (Object [] v, ByteSource src)
      throws BlinkException
   {
      for (int i = 0; i < v.length; ++ i)
         v [i] = readObject (src);
      return v;
   }

   public Object [] readObjectArray (ByteSource src) throws BlinkException
   {
      return readObjectArray (new Object [Vlc.readU32 (src)], src);
   }
   
   public static boolean readNull (ByteSource src) throws BlinkException.Decode
   {
      if (src.empty ())
         return true;
      else if (src.get () == Vlc.Null)
      {
         src.step ();
         return true;
      }
      else
         return false;
   }

   public static void skipByte (ByteSource src)
   {
      src.step ();
   }
   
   //////////////////////////////////////////////////////////////////////

   public abstract static class Decoder implements Creator
   {
      Decoder (Class<?> type, Schema.Group grp)
      {
         this.type = type;
         this.grp = grp;
      }

      public final void decodeMsg (ByteSource src, CompactReader rd, Block block)
         throws BlinkException
      {
         Object o = allocate (block);
         try
         {
            decode (src, o, rd);
            consume (o, block);
         }
         catch (BlinkException.Decode e)
         {
            block.reclaim (o);
            throw e;
         }
         catch (RuntimeException e)
         {
            block.reclaim (o);
            throw e;
         }
      }

      public final Object decodeGrp (ByteSource src, CompactReader rd,
                                     Block block)
         throws BlinkException
      {
         Object o = allocate (block);
         try
         {
            decode (src, o, rd);
            return o;
         }
         catch (BlinkException.Decode e)
         {
            block.reclaim (o);
            throw e;
         }
         catch (RuntimeException e)
         {
            block.reclaim (o);
            throw e;
         }
      }

      @Override
      public Class<?> getType ()
      {
         return type;
      }

      private Object allocate (Block block)
         throws BlinkException.Binding
      {
         if (pool == null || take >= pool.length)
         {
            pool = block.refill (this, pool);
            take = 0;
         }

         Object fresh = pool [take];
         pool [take ++] = null;
         return fresh;
      }

      abstract void consume (Object o, Block block) throws BlinkException;
      protected abstract void decode (ByteSource src, Object o, CompactReader rd)
         throws BlinkException;
      
      private final Class<?> type;
      final Schema.Group grp;
      private Object [] pool;
      private int take;
   }

   public abstract static class AppendingDecoder extends Decoder
   {
      protected AppendingDecoder (Class<?> type, Schema.Group grp)
      {
         super (type, grp);
      }

      @Override void consume (Object o, Block block)
      {
         block.append (o);
      }
   }

   public abstract static class DispatchingDecoder extends Decoder
   {
      protected DispatchingDecoder (Class<?> type, Schema.Group grp,
                                    Observer obs)
      {
         super (type, grp);
         this.obs = obs;
      }

      @Override void consume (Object o, Block block)
      {
         block.append (o);
         obs.onObj (o, grp);
      }

      private final Observer obs;
   }

   public abstract static class SchemaExchangeDecoder extends Decoder
   {
      protected SchemaExchangeDecoder (
         Class<?> type, Schema.Group grp,
         com.pantor.blink.SchemaExchangeDecoder dec)
      {
         super (type, grp);
         this.dec = dec;
      }

      @Override void consume (Object o, Block block)
         throws BlinkException
      {
         try
         {
            dec.decode (o);
         }
         finally
         {
            block.reclaim (o);
         }
      }

      private final com.pantor.blink.SchemaExchangeDecoder dec;
   }

   private static final int MaxLingeringScratchArea = 1000000;
   
   private boolean readOrSuspendMsg (ByteSource src, long msgSize)
      throws BlinkException
   {
      if (msgSize <= src.available ())
      {
         readMsg (src, (int)msgSize);
         return true;
      }
      else
      {
         if (msgSize < Long.MAX_VALUE)
            initPendData (src, msgSize);
      }

      return false;
   }

   private void readMsg (ByteSource src, int msgSize) throws BlinkException
   {
      int limit = src.getPos () + msgSize;
      Decoder dec = null;

      int saveSize = src.size ();
      src.setSize (limit);
      
      try
      {
         long tid = Vlc.readU64 (src);
         dec = compiler.getDecoder (tid);
         dec.decodeMsg (src, this, curBlock);
         if (src.getPos () > limit)
            throw msgOverflowError (src);
         src.setSize (saveSize);
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
         // FIXME: Localize any exception to the current decoder
         throw prematureEndOfMsg (src);
      }
      // FIXME, more catches
   }

   public Object readObject (ByteSource src) throws BlinkException
   {
      int size = (int)Util.u32ToLong (Vlc.readU32 (src));
      int limit = src.getPos () + size;

      int saveSize = src.size ();
      src.setSize (limit);

      long tid = Vlc.readU64 (src);
      Decoder dec = null;
      
      try
      {
         dec = compiler.getDecoder (tid);
         Object o = dec.decodeGrp (src, this, curBlock);
         if (src.getPos () > limit)
            throw msgOverflowError (src);
         src.setSize (saveSize);
         return o;
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
         // FIXME: Localize any exception to the current decoder
         throw prematureEndOfMsg (src);
      }
   }

   private long fillPendMsgSize (ByteSource src) throws BlinkException.Decode
   {
      int toMove = Math.min (missingMsgSizeBytes, src.available ());
      src.moveTo (pendMsgSizePreamble, toMove);
      missingMsgSizeBytes -= toMove;
      if (missingMsgSizeBytes == 0)
      {
         pendMsgSizePreamble.flip ();
         long msgSize = Util.u32ToLong (Vlc.readU32 (pendMsgSizePreamble));
         pendMsgSizePreamble.clear ();
         return msgSize;
      }
      else
         return Long.MAX_VALUE;
   }

   void initPendData (ByteSource src, long msgSize) throws BlinkException.Decode
   {
      if (msgSize <= maxMsgSize)
      {
         missingData = (int)msgSize;
         fillPendData (src);
      }
      else
         throw error (String.format ("Max blink message size exceeded: %d > %d",
                                     msgSize, maxMsgSize), src);
   }

   private boolean fillPendData (ByteSource src) throws BlinkException.Decode
   {
      try
      {
         int toMove = Math.min (missingData, src.available ());
         pendData.reserve (toMove);
         src.moveTo (pendData, toMove);
         missingData -= toMove;
         return missingData == 0;
      }
      catch (IOException e)
      {
         throw new BlinkException.Decode (e.getMessage (), src);
      }
   }

   private long readMsgSize (ByteSource src) throws BlinkException.Decode
   {
      if (src.available () >= Vlc.Int32MaxSize)
         return Util.u32ToLong (Vlc.readU32 (src));
      else
         return readMsgSizeIncremental (src);
   }
   
   private long readMsgSizeIncremental (ByteSource src)
      throws BlinkException.Decode
   {
      int available = src.available ();
      if (available == 0)
      {
         missingMsgSizeBytes = 0;
         return Long.MAX_VALUE;
      }
   
      int b = src.get ();
      if ((b & 0x80) == 0)
      {
         src.step ();
         return b;
      }
      else if ((b & 0x40) == 0)
      {
         if (available < 2)
         {
            pendMsgSizePreamble.write (b);
            missingMsgSizeBytes = 1;
            return Long.MAX_VALUE;
         }
         else
            return Util.u32ToLong (Vlc.readU32 (src));
      }
      else
      {
         int w = b & 0x3f;
         if (w > 4)
            throw Vlc.overflowError ("u32", src);
         
         if (available < w + 1)
         {
            src.moveTo (pendMsgSizePreamble, available);
            missingMsgSizeBytes = w + 1 - available;
            return Long.MAX_VALUE;
         }
         else
            return Util.u32ToLong (Vlc.readU32 (src));
      }
   }

   private BlinkException.Decode error (String msg, ByteSource context)
   {
      // FIXME
      return new BlinkException.Decode (msg, context);
   }

   private BlinkException.Decode msgOverflowError (ByteSource context)
   {
      // FIXME
      return new BlinkException.Decode ("Message length exceeded", context);
   }

   private BlinkException.Decode prematureEndOfMsg (ByteSource context)
   {
      // FIXME
      return new BlinkException.Decode ("Premature end of message", context);
   }
   
   private final CompactReaderCompiler compiler;
   private final Buf pendData = DirectBuf.newInstance ();
   private final Buf pendMsgSizePreamble = DirectBuf.newInstance (5);
   private long maxMsgSize;
   private final BlankBlock blankBlock = new BlankBlock ();
   private Block curBlock;
   private int missingData;
   private int missingMsgSizeBytes;
}
