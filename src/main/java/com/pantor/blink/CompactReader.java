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
import java.io.InputStream;

/**
   The {@code CompactReader} implements a decoder for the Blink
   compact binary format. In its most basic form it reads bytes from
   an instance of {@link Buf} and turns them into POJOs as defined by
   an {@code ObjectModel}. The result can either be appended to a
   {@link Block} or dispatched to an observer as specified by an
   {@link ObserverRegistry}, or both.

   <p>The reader supports incremental decoding in the sense that you do
   not have to worry about messages boundaries when you pass bytes to
   any of the {@code read} methods. If a buffer ends in a partial
   message, the reader will continue decoding that message when you
   supply the rest of the bytes in subsequent calls.</p>
 */

public final class CompactReader
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
      @throws BlinkException if a schema or binding problem occurs
   */
   
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
      @throws BlinkException if a schema or binding problem occurs
   */
   
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
      @throws BlinkException if a schema or binding problem occurs
   */
   
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
      @throws BlinkException if a schema or binding problem occurs
   */
   
   public void read (byte [] data, int from, int len, Block block)
      throws BlinkException
   {
      read (new ByteBuf (data, from, len), block);
   }

   /**
      Decodes bytes from the specified input stream. It dispatches
      decoded messages to any matching observers if an observer
      registry has been specified.

      <p>It also allocates objects as needed from the specified block.</p>

      @param is an input stream
      @throws BlinkException if a schema or binding problem occurs
      @throws IOException if an input error occurs
   */
   
   public void read (InputStream is) throws IOException, BlinkException
   {
      Buf b = DirectBuf.newInstance ();
      for (;;)
	 if (b.fillFrom (is))
	 {
	    b.flip ();
	    read (b);
	 }
	 else
	    break;
   }

   /**
      Decodes bytes specified in a buffer. It dispatches decoded
      messages to any matching observers if an observer registry has
      been specified.

      @param buf the bytes to decode
      @throws BlinkException if a schema or binding problem occurs
   */
   
   public void read (Buf buf) throws BlinkException
   {
      read (buf, blankBlock);
   }

   /**
      Decodes bytes specified in a buffer. It appends decoded
      messages to the specified block and also dispatches them to any
      matching observers if an observer registry has been specified.

      <p>It also allocates objects as needed from the specified block.</p>

      <p>This is the most native form of the {@code read} methods. All other
      read methods will create temporary {@code Buf} objects and/or use
      a private {@link DefaultBlock} instance managed by this reader.</p>
      
      @param buf the bytes to decode
      @param block the block that collects the decoded messages and is
      responsible for allocating new objects
      @throws BlinkException if a schema or binding problem occurs
   */
   
   public void read (Buf buf, Block block) throws BlinkException
   {
      curBlock = block;
      
      try
      {
	 if (missingData > 0)
	 {
	    if (fillPendData (buf))
	    {
	       pendData.flip ();
	       readMsg (pendData, pendData.size ());
	       pendData.release (MaxLingeringScratchArea);
	    }
	    else
	       return;
	 }

	 if (missingMsgSizeBytes > 0)
	    if (! readOrSuspendMsg (buf, fillPendMsgSize (buf)))
	       return;
   
	 for (;;)
	    if (! readOrSuspendMsg (buf, readMsgSize (buf)))
	       return;
      }
      catch (BlinkException.Decode e)
      {
	 throw error (e.getMessage (), e.getContext ());
      }
   }

   /**
      Sets the maximum message size. This reader will throw an
      exception if the maxium message size is exceeded.
   */

   public void setMaxMessageSize (long maxMsgSize)
   {
      this.maxMsgSize = maxMsgSize;
   }

   /**
      Returns {@code true} if there is no partial message pending

      @return {@code true} if there is no partial message pending
   */

   public boolean isComplete ()
   {
      return missingData == 0 && missingMsgSizeBytes == 0;
   }

   /**
      Closes this reader

      @throws BlinkException.Decode if this reader is incomplete as
      indicated by {@code isComplete}
   */

   public void close () throws BlinkException.Decode
   {
      if (! isComplete ())
	 throw new BlinkException.Decode (
	    "Incomplete compact blink message. The reader needs " +
	    "more data to finish an incomplete trailing " +
	    "message");
   }
   
   // Primitive values
   //////////////////////////////////////////////////////////////////////
   
   public static byte readU8 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readU8 (buf);
   }

   public static byte readI8 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readI8 (buf);
   }

   public static short readU16 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readU16 (buf);
   }

   public static short readI16 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readI16 (buf);
   }

   public static int readU32 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readU32 (buf);
   }

   public static int readI32 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readI32 (buf);
   }
   
   public static long readU64 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readU64 (buf);
   }

   public static long readI64 (Buf buf) throws BlinkException.Decode
   {
      return Vlc.readI64 (buf);
   }

   public static double readF64 (Buf buf) throws BlinkException.Decode
   {
      return Double.longBitsToDouble (Vlc.readU64 (buf));
   }

   public static Decimal readDecimal (Buf buf)
      throws BlinkException.Decode
   {
      byte exp = Vlc.readI8 (buf);
      long mant = Vlc.readI64 (buf);
      return Decimal.valueOf (mant, exp);
   }

   public static int readDate (Buf buf)
      throws BlinkException.Decode
   {
      return Vlc.readU32 (buf);
   }

   public static int readTimeOfDayMilli (Buf buf)
      throws BlinkException.Decode
   {
      return Vlc.readU32 (buf);
   }

   public static long readTimeOfDayNano (Buf buf)
      throws BlinkException.Decode
   {
      return Vlc.readU64 (buf);
   }

   public static long readNanotime (Buf buf)
      throws BlinkException.Decode
   {
      return Vlc.readI64 (buf);
   }

   public static long readMillitime (Buf buf)
      throws BlinkException.Decode
   {
      return Vlc.readI64 (buf);
   }

   public static boolean readBool (Buf buf)
      throws BlinkException.Decode
   {
      return Vlc.readU8 (buf) != 0;
   }

   public static String readString (Buf buf)
      throws BlinkException.Decode
   {
      return buf.readUtf8String (Vlc.readU32 (buf));
   }

   public static byte [] readU8Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      byte [] v = new byte [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU8 (buf);
      return v;
   }

   public static byte [] readI8Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      byte [] v = new byte [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readI8 (buf);
      return v;
   }

   public static short [] readU16Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      short [] v = new short [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU16 (buf);
      return v;
   }

   public static short [] readI16Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      short [] v = new short [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readI16 (buf);
      return v;
   }

   public static int [] readU32Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU32 (buf);
      return v;
   }

   public static int [] readI32Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readI32 (buf);
      return v;
   }
   
   public static long [] readU64Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU64 (buf);
      return v;
   }

   public static long [] readI64Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readI64 (buf);
      return v;
   }
   
   public static double [] readF64Array (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      double [] v = new double [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Double.longBitsToDouble (Vlc.readU64 (buf));
      return v;
   }
   
   public static Decimal [] readDecimalArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      Decimal [] v = new Decimal [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = readDecimal (buf);
      return v;
   }

   public static int [] readDateArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU32 (buf);
      return v;
   }

   public static int [] readTimeOfDayMilliArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      int [] v = new int [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU32 (buf);
      return v;
   }

   public static long [] readTimeOfDayNanoArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readU64 (buf);
      return v;
   }

   public static long [] readNanotimeArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readI64 (buf);
      return v;
   }

   public static long [] readMillitimeArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      long [] v = new long [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = Vlc.readI64 (buf);
      return v;
   }

   public static boolean [] readBoolArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      boolean [] v = new boolean [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = readBool (buf);
      return v;
   }

   public static String [] readStringArray (Buf buf)
      throws BlinkException.Decode
   {
      int size = Vlc.readU32 (buf);
      String [] v = new String [size];
      for (int i = 0; i < size; ++ i)
	 v [i] = readString (buf);
      return v;
   }
   
   public Object [] readObjectArray (Object [] v, Buf buf) throws BlinkException
   {
      for (int i = 0; i < v.length; ++ i)
	 v [i] = readObject (buf);
      return v;
   }

   public Object [] readObjectArray (Buf buf) throws BlinkException
   {
      return readObjectArray (new Object [Vlc.readU32 (buf)], buf);
   }
   
   public static boolean readNull (Buf buf) throws BlinkException.Decode
   {
      if (buf.empty ())
	 return true;
      else if (buf.get () == Vlc.Null)
      {
	 buf.step ();
	 return true;
      }
      else
	 return false;
   }

   public static void skipByte (Buf buf)
   {
      buf.step ();
   }

   //////////////////////////////////////////////////////////////////////

   public abstract static class Decoder implements Creator
   {
      protected Decoder (Class<?> type, Schema.Group grp, Observer obs)
      {
	 this.type = type;
	 this.grp = grp;
	 this.obs = obs;
      }
      
      public void decodeMsg (Buf buf, CompactReader rd, Block block)
	 throws BlinkException.Decode, BlinkException.Binding
      {
	 Object o = allocate (block);
	 try
	 {
	    decode (buf, o, rd);
	    block.append (o);
	    if (obs != null)
	       obs.onObj (o, grp);
	 }
	 catch (BlinkException.Decode e)
	 {
	    free (o);
	    throw e;
	 }
	 catch (RuntimeException e)
	 {
	    free (o);
	    throw e;
	 }
      }

      public Object decodeGrp (Buf buf, CompactReader rd, Block block)
	 throws BlinkException.Decode, BlinkException.Binding
      {
	 Object o = allocate (block);
	 try
	 {
	    decode (buf, o, rd);
	    return o;
	 }
	 catch (BlinkException.Decode e)
	 {
	    free (o);
	    throw e;
	 }
	 catch (RuntimeException e)
	 {
	    free (o);
	    throw e;
	 }
      }

      @Override public Class<?> getType () { return type; }

      protected abstract void decode (Buf buf, Object o, CompactReader rd)
	 throws BlinkException.Decode, BlinkException.Binding;

      private Object allocate (Block block)
	 throws BlinkException.Binding
      {
	 if (pool == null || take >= pool.length)
	 {
	    pool = block.refill (this, pool);
	    take = 0;
	 }

	 return pool [take ++];
      }

      private void free (Object o)
      {
	 assert pool != null;
	 -- take;
	 pool [take] = o;
      }
      
      private final Class<?> type;
      private final Schema.Group grp;
      private final Observer obs;
      private Object [] pool;
      private int take;
   }

   private static final int MaxLingeringScratchArea = 1000000;
   
   private boolean readOrSuspendMsg (Buf buf, long msgSize)
      throws BlinkException
   {
      if (msgSize <= buf.available ())
      {
	 readMsg (buf, (int)msgSize);
	 return true;
      }
      else
      {
	 if (msgSize < Long.MAX_VALUE)
	    initPendData (buf, msgSize);
      }

      return false;
   }

   private void readMsg (Buf buf, int msgSize) throws BlinkException
   {
      int limit = buf.getPos () + msgSize;
      Decoder dec = null;

      int saveSize = buf.size ();
      buf.setSize (limit);
      
      try
      {
	 long tid = Vlc.readU64 (buf);
	 dec = compiler.getDecoder (tid);
	 dec.decodeMsg (buf, this, curBlock);
	 if (buf.getPos () > limit)
	    throw msgOverflowError (buf);
	 buf.setSize (saveSize);
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
	 // FIXME: Localize any exception to the current decoder
	 throw prematureEndOfMsg (buf);
      }
      // FIXME, more catches
   }

   public Object readObject (Buf buf) throws BlinkException
   {
      int size = (int)Util.u32ToLong (Vlc.readU32 (buf));
      int limit = buf.getPos () + size;

      int saveSize = buf.size ();
      buf.setSize (limit);

      long tid = Vlc.readU64 (buf);
      Decoder dec = null;
      
      try
      {
	 dec = compiler.getDecoder (tid);
	 Object o = dec.decodeGrp (buf, this, curBlock);
	 if (buf.getPos () > limit)
	    throw msgOverflowError (buf);
	 buf.setSize (saveSize);
	 return o;
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
	 // FIXME: Localize any exception to the current decoder
	 throw prematureEndOfMsg (buf);
      }
   }

   private long fillPendMsgSize (Buf buf) throws BlinkException.Decode
   {
      int toMove = Math.min (missingMsgSizeBytes, buf.available ());
      buf.moveTo (pendMsgSizePreamble, toMove);
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

   void initPendData (Buf buf, long msgSize) throws BlinkException.Decode
   {
      if (msgSize <= maxMsgSize)
      {
	 missingData = (int)msgSize;
	 fillPendData (buf);
      }
      else
	 throw error (String.format ("Max blink message size exceeded: %d > %d",
				     msgSize, maxMsgSize), buf);
   }

   private boolean fillPendData (Buf buf)
   {
      int toMove = Math.min (missingData, buf.available ());
      pendData.reserve (toMove);
      buf.moveTo (pendData, toMove);
      missingData -= toMove;
      return missingData == 0;
   }

   private long readMsgSize (Buf buf) throws BlinkException.Decode
   {
      if (buf.available () >= Vlc.Int32MaxSize)
	 return Util.u32ToLong (Vlc.readU32 (buf));
      else
	 return readMsgSizeIncremental (buf);
   }
   
   private long readMsgSizeIncremental (Buf buf) throws BlinkException.Decode
   {
      int available = buf.available ();
      if (available == 0)
      {
	 missingMsgSizeBytes = 0;
	 return Long.MAX_VALUE;
      }
   
      int b = buf.get ();
      if ((b & 0x80) == 0)
      {
	 buf.step ();
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
	    return Util.u32ToLong (Vlc.readU32 (buf));
      }
      else
      {
	 int w = b & 0x3f;
	 if (w > 4)
	    throw Vlc.overflowError ("u32", buf);
	 
	 if (available < w + 1)
	 {
	    buf.moveTo (pendMsgSizePreamble, available);
	    missingMsgSizeBytes = w + 1 - available;
	    return Long.MAX_VALUE;
	 }
	 else
	    return Util.u32ToLong (Vlc.readU32 (buf));
      }
   }

   private BlinkException.Decode error (String msg, Buf context)
   {
      // FIXME
      return new BlinkException.Decode (msg, context);
   }

   private BlinkException.Decode msgOverflowError (Buf context)
   {
      // FIXME
      return new BlinkException.Decode ("Message length exceeded", context);
   }

   private BlinkException.Decode prematureEndOfMsg (Buf context)
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
