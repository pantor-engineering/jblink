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

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import static com.pantor.blink.DynClass.getDescriptor;
import static com.pantor.blink.CodegenUtil.mapType;
import static com.pantor.blink.CodegenUtil.mapArrayType;

public final class CompactWriterCompiler
{
   public CompactWriterCompiler (ObjectModel om)
   {
      this.om = om;
   }

   public CompactWriter.Encoder getEncoder (Class<?> cl) throws BlinkException
   {
      return getEncoder (cl, null);
   }
   
   public CompactWriter.Encoder getEncoder (
      Class<?> cl, SchemaExchangeEncoder schemaExEnc) throws BlinkException
   {
      CompactWriter.Encoder e = encByClass.get (cl);
      if (e != null)
         return e;
      else
         return compile (cl, schemaExEnc);
   }

   public CompactWriter.Encoder getEncoder (NsName name) throws BlinkException
   {
      return getEncoder (name, null);
   }
   
   public CompactWriter.Encoder getEncoder (
      NsName name, SchemaExchangeEncoder schemaExEnc) throws BlinkException
   {
      CompactWriter.Encoder d = encByName.get (name);
      if (d != null)
         return d;
      else
         return compile (om.getGroupBinding (name), schemaExEnc);
   }

   public void primeGroup (NsName name) throws BlinkException
   {
      getEncoder (name);
   }

   public void primeEnum (ObjectModel.EnumBinding bnd)
      throws BlinkException.Binding
   {
      if (! enumEncs.contains (bnd.getEnum ().getName ()))
         compileEnum (bnd);
   }

   public ObjectModel getObjectModel ()
   {
      return om;
   }
   
   private CompactWriter.Encoder compile (
      Class<?> cl, SchemaExchangeEncoder schemaExEnc) throws BlinkException
   {
      ObjectModel.GroupBinding bnd = om.getGroupBinding (cl);
      CompactWriter.Encoder e = getEncoder (bnd.getGroup ().getName (),
                                            schemaExEnc);
      encByClass.put (cl, e);
      return e;
   }

   // Generates an encoder for the specified binding. The encoder has
   // the following general layout

   //   package com.pantor.blink.dyn.compact;
   //
   //   public final class <Ns>+<Name>_enc extends CompactWriter.Encoder
   //   {
   //      public <Ns>+<Name>_enc (byte [] tid, Class type, Schema.Group grp)
   //      {
   //         super (tid, type, grp);
   //      }
   //
   //      @Override
   //      public void encode (Object src, ByteSink sink, CompactWriter wr)
   //      {
   //         sink.write (tid);
   //         innerEncode ((T)src, sink, wr);
   //      }
   //  
   //      public static void encodeArray (T [] objs, ByteSink sink,
   //                                      CompactWriter wr)
   //      {
   //         CompactWriter.writeU32 (objs.length, sink);
   //         for (int i = 0; i < size; ++ i)
   //           innerEncode (objs [i], sink, wr);
   //      }
   //  
   //      public static void innerEncode (T src, ByteSink sink,
   //                                      CompactWriter wr)
   //      {
   //         ... encode src to sink ...
   //      }
   //
   //      public static void encodeBlank (ByteSink sink)
   //      {
   //         ... encode blank fields to sink ...
   //      }
   //   }

   private final static class SizeContext
   {
      private final static class Guard
      {
         Guard (int pos, int size)
         {
            this.pos = pos;
            this.size = size;
         }
         
         final int pos;
         final int size;
      }

      SizeContext (int sinkReg) { this.sinkReg = sinkReg; }
      
      void addGuard (DynClass dc)
      {
         dc.aload (sinkReg);
         int pos = dc.reserveIns ();
         dc.invokeInterface ("com.pantor.blink.ByteSink", "reserve", "(I)V");
         guards.add (new Guard (pos, size));
      }

      void addSize (int size)
      {
         this.size += size;
      }

      void patchGuards (DynClass dc)
      {
         for (Guard g : guards)
            dc.ldc (g.pos, size - g.size);
      }
      
      private final ArrayList<Guard> guards = new ArrayList<Guard> ();
      private final int sinkReg;
      private int size;
   }
   
   private CompactWriter.Encoder compile (ObjectModel.GroupBinding bnd,
                                          SchemaExchangeEncoder schemaExEnc)
      throws BlinkException
   {
      Schema.Group g = bnd.getGroup ();

      if (schemaExEnc != null)
         schemaExEnc.declare (g);

      String encoderName = getEncoderClassName (g.getName ());

      // Generate encoder class
      
      DynClass dc = new DynClass (encoderName);
      dc.setFlags (DynClass.ClassFlag.Final);

      String ctorSig =
         "([BLjava/lang/Class;Lcom/pantor/blink/Schema$Group;)V";
      
      dc.setSuper ("com.pantor.blink.CompactWriter$Encoder");

      // Constructor
      
      dc.startPublicMethod ("<init>", ctorSig)
         .aload0 ().aload1 ().aload2 ().aload3 ()
         .invokeSpecial ("com/pantor/blink/CompactWriter$Encoder",
                         "<init>", ctorSig)
         .return_ ().setMaxStack (4).endMethod ();

      // void encode (src, sink, wr)

      String encSig = "(Ljava/lang/Object;Lcom/pantor/blink/ByteSink;" +
                      "Lcom/pantor/blink/CompactWriter;)V";

      String srcName = bnd.getTargetType ().getName ();

      String innerSig = getInnerEncodeSignature (bnd);
      
      dc.startPublicMethod ("encode", encSig)
         .aload2 () // sink
         .aload0 () // this
         .getField ("com.pantor.blink.CompactWriter$Encoder", "tid", "[B")
         .invokeInterface ("com.pantor.blink.ByteSink", "write", "([B)V")
         .aload1 ().checkCast (srcName).aload2 ().aload3 ()
         .invokeStatic (encoderName, "innerEncode", innerSig)
         .return_ ().setMaxStack (3).endMethod ();

      // public static void encodeArray (T [] objs, sink, rd)

      int loop = dc.declareLabel ();
      int loopEnd = dc.declareLabel ();

      dc.startPublicStaticMethod ("encodeArray",
                                  getEncodeArraySignature (srcName));

      dc.aload0 (); // objs
      dc.arrayLength ();
      dc.dup ();
      dc.istore3 (); // size
      dc.aload1 (); // sink
      invokeWriter (dc, "writeU32", "I");
      dc.iconst0 ()
         .istore (4) // i = 0
         .label (loop)
         .iload (4) // i
         .iload3 () // size
         .ifIcmpGe (loopEnd) // jump if i >= size
         .aload0 () // objs
         .iload (4) // i
         .aaload () // objs [i]
         .aload1 () // sink
         .aload2 () // wr
         .invokeStatic (encoderName, "innerEncode", innerSig)
         .iinc (4, 1) // ++ i
         .goto_ (loop)
         .label (loopEnd)
         .return_ ()
         .setMaxStack (3)
         .endMethod ();
      
      // static void innerEncode (src, sink, wr)
      
      dc.startPublicStaticMethod ("innerEncode", innerSig);

      SizeContext scx = new SizeContext (1);
      scx.addGuard (dc);
      
      // Emit encoding instructions for each field
      
      for (ObjectModel.Field f : bnd)
         compile (bnd, f, dc, scx);

      scx.patchGuards (dc);

      dc.return_ ().setMaxStack (3).endMethod ();

      // static void encodeBlank (sink)
      
      dc.startPublicStaticMethod ("encodeBlank",
                                  "(Lcom/pantor/blink/ByteSink;)V");

      SizeContext blankScx = new SizeContext (0);
      blankScx.addGuard (dc);

      // Emit blank encoding instructions for each field
      
      for (ObjectModel.Field f : bnd)
         compileBlankField (f.getField (), 0, dc, blankScx);

      blankScx.patchGuards (dc);
      
      dc.return_ ().setMaxStack (2).endMethod ();

      // Create an instance of the generated encoder
      
      byte [] tid = null;

      Buf tidBuf = new ByteBuf (Vlc.Int64MaxSize);
      Vlc.writeU64 (bnd.getCompactTypeId (), tidBuf);
      tidBuf.flip ();
      tid = new byte [tidBuf.size ()];
      tidBuf.read (tid);
      
      CompactWriter.Encoder enc = createInstance (tid, dc, bnd);

      // Store this instance for future lookups
      
      encByName.put (g.getName (), enc);
      
      return enc;
   }

   private static String getClassName (NsName nm)
   {
      if (nm.isQualified ())
         return nm.getNs () + "+" + nm.getName ();
      else
         return nm.getName ();
   }
   
   private static String getEncoderClassName (NsName nm)
   {
      return "com.pantor.blink.dyn.compact." + getClassName (nm) + "_enc";
   }

   private static String getEncodeArraySignature (String src)
   {
      return "([L" + DynClass.toInternal (src) +
         ";Lcom/pantor/blink/ByteSink;" +
         "Lcom/pantor/blink/CompactWriter;)V";
   }

   private static String getEncodeEnumSignature (ObjectModel.Binding bnd)
   {
      return "(L" + DynClass.toInternal (bnd.getTargetType ()) +
         ";Lcom/pantor/blink/ByteSink;)V";
   }

   private static String getInnerEncodeSignature (ObjectModel.Binding bnd)
   {
      return "(L" + DynClass.toInternal (bnd.getTargetType ()) +
         ";Lcom/pantor/blink/ByteSink;Lcom/pantor/blink/CompactWriter;)V";
   }

   private static String getEncodeEnumArraySignature (ObjectModel.Binding bnd)
   {
      return "([L" + DynClass.toInternal (bnd.getTargetType ()) +
         ";Lcom/pantor/blink/ByteSink;)V";
   }

   private static void invokeWriter (DynClass dc, String m, String t)
   {
      dc.invokeStatic ("com/pantor/blink/CompactWriter", m,
                       "(" + t + "Lcom/pantor/blink/ByteSink;)V");
   }

   private static void invokeWriter (DynClass dc, String m)
   {
      invokeWriter (dc, m, "");
   }

   private void compile (ObjectModel.Binding bnd, ObjectModel.Field f,
                         DynClass dc, SizeContext scx)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      int putNull = dc.declareLabel ();
      int end = dc.declareLabel ();
      Method getter = f.getGetter ();

      // FIXME: Handle compound decimal getter

      if (getter != null)
      {
         if (sf.isOptional ())
         {
            dc.aload0 (); // src, #depth: 1
            Method pred = f.getPredicate ();
            if (pred != null)
               dc.invoke (pred);
            else
            {
               // FIXME: Handle:
               //    boxed types
               //    compound decimal getter
               throw new RuntimeException ("missing has" + sf.getName ());
            }
            
            dc.ifEq (putNull); // Jump if not present
         }
         
         dc.aload0 (); // src, #depth: 1
         dc.invoke (getter);

         if (! t.isSequence ())
         {
            if (t.isPrimitive ())
            {
               if (t.getType ().getCode () == Schema.TypeCode.Fixed)
                  compileFixed (f, dc, scx);
               else if (t.getType ().getCode () == Schema.TypeCode.FixedDec)
                  compileFixedDec (f, dc, scx);
               else
                  compilePrim (f, dc, scx);
            }
            else if (t.isEnum ())
               compileEnum (f, dc, scx);
            else // Object or Group
               compileGroupField (f, dc, scx);
         }
         else
         {
            scx.addSize (Vlc.Int32MaxSize);
            if (t.isPrimitive ())
            {
               if (t.getType ().getCode () == Schema.TypeCode.Fixed)
                  compileFixedSeq (f, dc);
               else if (t.getType ().getCode () == Schema.TypeCode.FixedDec)
                  compileFixedDecSeq (f, dc);
               else
                  compilePrimSeq (f, dc);
            }
            else if (t.isEnum ())
               compileEnumSeq (f, dc);
            else // Object or Group
               compileGroupSeqField (f, dc);
            scx.addGuard (dc);
         }

         if (sf.isOptional ())
         {
            dc.goto_ (end);
            dc.label (putNull);
            dc.aload1 (); // sink, #depth 1
            invokeWriter (dc, "writeNull");
         }
      
         dc.label (end);
      }
      else
         compileBlankField (sf, t, 1, dc, scx);
   }

   private static void compilePrim (ObjectModel.Field f, DynClass dc,
                                    SizeContext scx)
      throws BlinkException
   {
      dc.aload1 (); // sink, #depth: 2
      Schema.TypeInfo t = f.getFieldType ();
      Schema.TypeCode code = t.getType ().getCode ();
      Class<?> argType = mapType (code);
      requireGetterRetType (f, argType);
      invokeWriter (dc, "write" + code.toString (), getDescriptor (argType));
      if (code == Schema.TypeCode.String || code == Schema.TypeCode.Binary)
         scx.addGuard (dc);
      else
         scx.addSize (getMaxVlcSize (t.getType ().getCode ()));
   }

   private static void compileFixedDec (ObjectModel.Field f, DynClass dc,
                                        SizeContext scx)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      if (FixedDec.class.isAssignableFrom (f.getGetter ().getReturnType ()))
      {
         Schema.FixedDecType ft = (Schema.FixedDecType)t.getType ();
         dc.ldc (ft.getScale ()); // #depth: 2
         dc.aload1 (); // sink, #depth: 3

         dc.invokeStatic ("com/pantor/blink/CompactWriter", "writeBoxedFixedDec",
                          "(Lcom/pantor/blink/FixedDec;" +
                          "ILcom/pantor/blink/ByteSink;)V");
         
         scx.addSize (getMaxVlcSize (Schema.TypeCode.I64));
      }
      else
         compilePrim (f, dc, scx);
   }
   
   private static void compileFixed (ObjectModel.Field f, DynClass dc,
                                     SizeContext scx)
      throws BlinkException
   {
      requireGetterRetType (f, byte [].class);
      
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      Schema.FixedType ft = (Schema.FixedType)t.getType ();

      int fixedSize = ft.getSize ();
      if (sf.isOptional ())
      {
         scx.addSize (1);
         dc.aload1 (); // sink, #depth: 2
         invokeWriter (dc, "writeOne"); // Presence byte
      }

      dc.ldc (fixedSize); // #depth: 2
      dc.aload1 (); // sink, #depth: 3
      Schema.TypeCode code = t.getType ().getCode ();

      dc.invokeStatic ("com/pantor/blink/CompactWriter", "writeFixed",
                       "(" + CodegenUtil.mapTypeDescr (code) +
                       "ILcom/pantor/blink/ByteSink;)V");

      scx.addSize (fixedSize);
   }

   private static void compileFixedSeq (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      requireGetterRetType (f, byte [][].class);

      Schema.TypeInfo t = f.getFieldType ();
      Schema.FixedType ft = (Schema.FixedType)t.getType ();
      Schema.TypeCode code = t.getType ().getCode ();
      int fixedSize = ft.getSize ();
      dc.ldc (fixedSize); // #depth: 2
      dc.aload1 (); // sink, #depth: 3
      dc.invokeStatic ("com/pantor/blink/CompactWriter", "writeFixedArray",
                       "([" + CodegenUtil.mapTypeDescr (code) +
                       "ILcom/pantor/blink/ByteSink;)V");
   }

   private static void compileFixedDecSeq (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Class<?> compType = f.getGetter ().getReturnType ().getComponentType ();
      if (FixedDec.class.isAssignableFrom (compType))
      {
         Schema.FixedDecType ft = (Schema.FixedDecType)t.getType ();
         dc.ldc (ft.getScale ()); // #depth: 2
         dc.aload1 (); // sink, #depth: 3

         dc.invokeStatic ("com/pantor/blink/CompactWriter",
                          "writeBoxedFixedDecArray",
                          "([Lcom/pantor/blink/FixedDec;" +
                          "ILcom/pantor/blink/ByteSink;)V");
      }
      else
         compilePrimSeq (f, dc);
   }
   
   private void compileEnum (ObjectModel.Field f, DynClass dc, SizeContext scx)
      throws BlinkException
   {
      ObjectModel.EnumBinding comp = f.getComponent ().toEnum ();
      Schema.TypeInfo t = f.getFieldType ();
      requireGetterRetType (f, comp.getTargetType ());
      
      primeEnum (comp);
      dc.aload1 (); // sink, #depth: 2
      dc.invokeStatic (getEncoderClassName (t.getEnum ().getName ()),
                       "encode", getEncodeEnumSignature (comp));
      scx.addSize (Vlc.Int32MaxSize);
   }

   private static void compilePrimSeq (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.TypeCode c = t.getType ().getCode ();

      Class<?> argType = mapArrayType (c);
      requireGetterRetType (f, argType);
      
      String encMtod = "write" + c.toString () + "Array";
      dc.aload1 (); // sink, #depth: 2
      invokeWriter (dc, encMtod, getDescriptor (argType));
   }

   private void compileEnumSeq (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      ObjectModel.EnumBinding comp = f.getComponent ().toEnum ();
      Schema.TypeInfo t = f.getFieldType ();      
      requireGetterRetType (f, DynClass.getArrayClass (comp.getTargetType ()));
      primeEnum (comp);
      dc.aload1 (); // sink, #depth: 2
      dc.invokeStatic (getEncoderClassName (t.getEnum ().getName ()),
                       "encodeArray",
                       getEncodeEnumArraySignature (comp));
   }
   
   private void compileGroupField (ObjectModel.Field f, DynClass dc,
                                   SizeContext scx)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      
      if (t.isDynamic () || t.isObject ())
      {
         dc.aload2 (); // Writer, #depth: 2
         dc.swap ();
         dc.invokeVirtual ("com.pantor.blink.CompactWriter", "writeObject",
                           "(Ljava/lang/Object;)V");
         scx.addGuard (dc);
      }
      else
      {
         ObjectModel.GroupBinding comp = f.getComponent ().toGroup ();
         primeGroup (comp.getGroup ().getName ());
         if (sf.isOptional ())
         {
            scx.addSize (1);
            dc.aload1 (); // sink, #depth: 2
            invokeWriter (dc, "writeOne"); // Presence byte
         }
         dc.aload1 (); // sink, #depth: 2
         dc.aload2 (); // Writer, #depth: 3
         dc.invokeStatic (getEncoderClassName (comp.getGroup ().getName ()),
                          "innerEncode", getInnerEncodeSignature (comp));
         scx.addGuard (dc);
      }
   }

   private void compileGroupSeqField (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      
      if (t.isObject () || t.isDynamic ())
      {
         dc.aload2 (); // Writer, #depth: 2
         dc.swap ();
         dc.invokeVirtual ("com.pantor.blink.CompactWriter", "writeObjectArray",
                           "([Ljava/lang/Object;)V");
      }
      else
      {
         ObjectModel.GroupBinding comp = f.getComponent ().toGroup ();
         primeGroup (comp.getGroup ().getName ());
         String sig =
            getEncodeArraySignature (comp.getTargetType ().getName ());
         dc.aload1 (); // sink, #depth: 2
         dc.aload2 (); // Writer, #depth: 3
         dc.invokeStatic (getEncoderClassName (comp.getGroup ().getName ()),
                          "encodeArray", sig);
      }
   }

   private void compileBlankField (Schema.Field sf, int sinkReg, DynClass dc,
                                   SizeContext scx)
      throws BlinkException
   {
      Schema.TypeInfo t = om.getSchema ().resolve (sf.getType ());
      compileBlankField (sf, t, sinkReg, dc, scx);
   }
   
   private void compileBlankField (Schema.Field sf, Schema.TypeInfo t,
                                   int sinkReg, DynClass dc, SizeContext scx)
      throws BlinkException
   {
      // If optional, write null, otherwise, write a blank value. All
      // types except enums and static subgroups can be represented by
      // a single zero: integers becomes zero, strings and sequences
      // becomes empty, bool becomes false etc. Dynamic subgroups of
      // length zero are not fully kosher since they are considered
      // weak errors. But it's about the best we can do, and a decoder
      // is at least allowed to recover from it in an implementation
      // specific way.

      // FIXME: Log a warning if not optional
      
      if (sf.isOptional ())
      {
         dc.aload (sinkReg); // sink, #depth 1
         invokeWriter (dc, "writeNull");
         scx.addSize (1);
      }
      else if (t.isEnum ())
      {
         // Write the value of the first symbol
         Schema.Enum e = t.getEnum ().getType ().toEnum ();
         dc.ldc (e.getSymbols ().get (0).getValue ());
         dc.aload (sinkReg); // sink, #depth 2
         invokeWriter (dc, "writeI32", "I");
         scx.addSize (Vlc.Int32MaxSize);
      }
      else if (t.isGroup () && ! t.isSequence () && ! t.isDynamic ())
      {
         primeGroup (t.getGroup ().getName ());
         dc.aload (sinkReg); // sink, #depth: 2
         dc.invokeStatic (getEncoderClassName (t.getGroup ().getName ()),
                          "encodeBlank", "(Lcom/pantor/blink/ByteSink;)V");
         scx.addGuard (dc);
      }
      else
      {
         dc.aload (sinkReg); // sink, #depth 1
         invokeWriter (dc, "writeZero");
         scx.addSize (1);
      }
   }

   private static int getMaxVlcSize (Schema.TypeCode code)
   {
      switch (code)
      {
       case U8:  case I8:    return Vlc.Int8MaxSize;
       case I16: case U16:   return Vlc.Int16MaxSize;
       case I32: case U32:   return Vlc.Int32MaxSize;
       case I64: case U64:   return Vlc.Int64MaxSize;
       case F64:             return Vlc.Int64MaxSize;
       case Decimal:         return Vlc.Int8MaxSize + Vlc.Int64MaxSize;
       case FixedDec:        return Vlc.Int64MaxSize;
       case Date:            return Vlc.Int32MaxSize;
       case TimeOfDayMilli:  return Vlc.Int32MaxSize;
       case TimeOfDayNano:   return Vlc.Int64MaxSize;
       case Nanotime:        return Vlc.Int64MaxSize;
       case Millitime:       return Vlc.Int64MaxSize;
       case Bool:            return 1;
       case String:          return Vlc.Int32MaxSize;
       case Object:          return Vlc.Int32MaxSize;
       case Ref:             return Vlc.Int32MaxSize;
       case Enum:            return Vlc.Int32MaxSize;
       default:              return Vlc.Int32MaxSize;
      }
   }

   // Generates an enum encoder for the specified binding. The encoder has
   // the following general layout

   //   package com.pantor.blink.dyn.compact;
   //
   //   public final class <Ns>+<Name>_enc
   //   {
   //      public static void encode (T sym, ByteSink sink)
   //      {
   //         CompactWriter.writeI32 (map.get (sym), sink);
   //      }
   //  
   //      public static void encodeArray (T [] syms, ByteSink sink)
   //      {
   //         CompactWriter.writeU32 (syms.length, sink);
   //         for (int i = 0; i < syms.length; ++ i)
   //           encode (src [i], sink);
   //      }
   //
   //      private final static java.util.EnumMap<T, Integer> map;
   //      
   //      static
   //      {
   //         map = new java.util.EnumMap<T, Integer> (T.class);
   //         map.put (T.Red, 0);
   //         map.put (T.Green, 1);
   //         map.put (T.Blue, 2);
   //      }
   //      
   //   }
   
   private void compileEnum (ObjectModel.EnumBinding bnd)
      throws BlinkException.Binding
   {
      Schema.Define d = bnd.getEnum ();
      String encoderName = getEncoderClassName (d.getName ());

      // Generate encoder class
      
      DynClass dc = new DynClass (encoderName);
      dc.setFlags (DynClass.ClassFlag.Final);

      dc.addField ("map", "Ljava/util/EnumMap;", DynClass.FieldFlag.Private,
                   DynClass.FieldFlag.Final, DynClass.FieldFlag.Static);

      Class<?> enumType = bnd.getTargetType ();

      // static static void encode (sym, sink)

      dc.startPublicStaticMethod ("encode", getEncodeEnumSignature (bnd))
         .getStatic (encoderName, "map", "Ljava/util/EnumMap;")
         .aload0 () // sym
         .invokeVirtual ("java.util.EnumMap", "get",
                         "(Ljava/lang/Object;)Ljava/lang/Object;")
         .checkCast ("java.lang.Integer")
         .aload1 (); // sink
      invokeWriter (dc, "writeEnumVal", "Ljava/lang/Integer;");
      dc.return_ ().setMaxStack (2).endMethod ();
      
      // public static void encodeArray (syms, sink)

      int loop = dc.declareLabel ();
      int loopEnd = dc.declareLabel ();

      dc.startPublicStaticMethod ("encodeArray",
                                  getEncodeEnumArraySignature (bnd));
      dc.aload0 (); // syms
      dc.arrayLength ();
      dc.dup ();
      dc.istore2 (); // size
      dc.aload1 (); // sink
      invokeWriter (dc, "writeU32", "I");
      dc.iconst0 ()
         .istore3 () // i = 0
         .label (loop)
         .iload3 () // i
         .iload2 () // size
         .ifIcmpGe (loopEnd) // jump if i >= size
         .aload0 () // syms
         .iload3 () // i
         .aaload () // syms [i]
         .aload1 () // sink
         .invokeStatic (encoderName, "encode", getEncodeEnumSignature (bnd))
         .iinc (3, 1) // ++ i
         .goto_ (loop)
         .label (loopEnd)
         .return_ ()
         .setMaxStack (2)
         .endMethod ();
      
      // static init
      
      dc.startMethod ("<clinit>", "()V", DynClass.MtodFlag.Static);

      dc.new_ ("java.util.EnumMap");
      dc.dup ();
      dc.ldcClass (enumType);
      dc.invokeSpecial ("java.util.EnumMap", "<init>", "(Ljava/lang/Class;)V");
      dc.dup ();
      dc.putStatic (encoderName, "map", "Ljava/util/EnumMap;");
      dc.astore0 ();

      for (ObjectModel.Symbol sym : bnd)
         if (sym.getTargetName () != null)
         {
            dc.aload0 ();
            dc.ldc (sym.getTargetName ());
            dc.invokeStatic (enumType, "valueOf", "(Ljava/lang/String;)L" +
                             DynClass.toInternal (enumType) + ";");
            dc.ldc (sym.getSymbol ().getValue ());
            dc.invokeStatic ("java.lang.Integer", "valueOf",
                             "(I)Ljava/lang/Integer;");
            dc.invokeVirtual ("java.util.EnumMap", "put",
                              "(Ljava/lang/Object;Ljava/lang/Object;" +
                              ")Ljava/lang/Object;");
            dc.pop ();
         }

      dc.return_ ();
      dc.setMaxStack (3);
      dc.endMethod ();

      // Load the class

      dload.loadPrivileged (dc, enumType);

      enumEncs.add (d.getName ());
   }

   private CompactWriter.Encoder createInstance (byte [] tid, DynClass dc,
                                                 ObjectModel.GroupBinding bnd)
      throws BlinkException.Binding
   {
      try
      {
         Class<?> tgtType = bnd.getTargetType ();
         Class<?> encClass = dload.loadPrivileged (dc, tgtType);
         Constructor<?> ctor = encClass.getConstructor (
            byte [].class, Class.class, Schema.Group.class);
         
         return (CompactWriter.Encoder)ctor.newInstance (
            tid, tgtType, bnd.getGroup ());
      }
      catch (NoSuchMethodException e)
      {
         throw new BlinkException.Binding (e);
      }
      catch (InstantiationException e)
      {
         throw new BlinkException.Binding (e);
      }
      catch (IllegalAccessException e)
      {
         throw new BlinkException.Binding (e);
      }
      catch (InvocationTargetException e)
      {
         throw new BlinkException.Binding (e);
      }
   }

   private static BlinkException.Binding typeMismatch (ObjectModel.Field f)
   {
      return new BlinkException.Binding (
         "Cannot use '" + f.getGetter () + "' to get field '" + f +
         "': type mismatch", f.getLocation ());
   }
   
   private static boolean hasGetterRetType (ObjectModel.Field f, Class<?> t)
   {
      return t == getGetterRetType (f);
   }

   private static void requireGetterRetType (ObjectModel.Field f, Class<?> t)
      throws BlinkException.Binding
   {
      if (! hasGetterRetType (f, t))
         throw typeMismatch (f);
   }

   private static Class<?> getGetterRetType (ObjectModel.Field f)
   {
      Method getter = f.getGetter ();
      Class<?> argType = null;
      if (getter != null)
         return getter.getReturnType ();
      else
         return null;
   }

   
   private final HashMap<Class<?>, CompactWriter.Encoder> encByClass =
      new HashMap<Class<?>, CompactWriter.Encoder> ();
   private final HashMap<NsName, CompactWriter.Encoder> encByName =
      new HashMap<NsName, CompactWriter.Encoder> ();
   private final ObjectModel om;
   private final DynClassLoader dload = new DynClassLoader ();
   private final HashSet<NsName> enumEncs = new HashSet <NsName> ();
}
