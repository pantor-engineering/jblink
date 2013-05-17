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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public final class CompactWriterCompiler
{
   public CompactWriterCompiler (ObjectModel om)
   {
      this.om = om;
   }

   public CompactWriter.Encoder getEncoder (Class<?> cl) throws BlinkException
   {
      CompactWriter.Encoder e = encByClass.get (cl);
      if (e != null)
	 return e;
      else
	 return compile (cl);
   }

   public CompactWriter.Encoder getEncoder (NsName name) throws BlinkException
   {
      CompactWriter.Encoder d = encByName.get (name);
      if (d != null)
	 return d;
      else
	 return compile (om.getGroupBinding (name));
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

   private CompactWriter.Encoder compile (Class<?> cl) throws BlinkException
   {
      ObjectModel.GroupBinding bnd = om.getGroupBinding (cl);
      CompactWriter.Encoder e = getEncoder (bnd.getGroup ().getName ());
      if (e == null)
	 e = compile (bnd);
      encByClass.put (cl, e);
      return e;
   }

   // Generates an encoder for the specified binding. The encoder has
   // the following general layout

   //   package com.pantor.blink.dyn.compact;
   //
   //   public final class <Ns>+<Name>_enc extends CompactWriter.Encoder
   //   {
   //      public <Ns>+<Name>_enc (byte [] tid, int minSize, Class type,
   //                             Schema.Group grp)
   //      {
   //         super (tid, minSize, type, grp);
   //      }
   //
   //      @Override
   //      public void encode (Object src, Buf buf, CompactWriter wr)
   //      {
   //         buf.write (tid);
   //         innerEncode ((T)src, buf, wr);
   //      }
   //  
   //      public static void encodeArray (T [] objs, Buf buf, CompactWriter wr)
   //      {
   //         CompactWriter.writeU32 (objs.length, buf);
   //         for (int i = 0; i < size; ++ i)
   //           innerEncode (objs [i], buf, wr);
   //      }
   //  
   //      public static void innerEncode (T src, Buf buf, CompactWriter wr)
   //      {
   //         ... encode src to buf ...
   //      }
   //
   //      public static void encodeBlank (Buf buf)
   //      {
   //         ... encode blank fields to buf ...
   //      }
   //   }
   
   private CompactWriter.Encoder compile (ObjectModel.GroupBinding bnd)
      throws BlinkException
   {
      Schema.Group g = bnd.getGroup ();

      String encoderName = getEncoderClassName (g.getName ());

      // Generate encoder class
      
      DynClass dc = new DynClass (encoderName);
      dc.setFlags (DynClass.ClassFlag.Final);

      String ctorSig =
	 "([BILjava/lang/Class;Lcom/pantor/blink/Schema$Group;)V";
      
      dc.setSuper ("com.pantor.blink.CompactWriter$Encoder");

      // Constructor
      
      dc.startPublicMethod ("<init>", ctorSig)
	 .aload0 ().aload1 ().iload2 ().aload3 ().aload (4)
	 .invokeSpecial ("com/pantor/blink/CompactWriter$Encoder",
			 "<init>", ctorSig)
	 .return_ ().setMaxStack (5).endMethod ();

      // void encode (src, buf, wr)

      String encSig = "(Ljava/lang/Object;Lcom/pantor/blink/Buf;" +
	 "Lcom/pantor/blink/CompactWriter;)V";

      String srcName = bnd.getTargetType ().getName ();

      String innerSig = getInnerEncodeSignature (bnd);
      
      dc.startPublicMethod ("encode", encSig)
	 .aload2 () // buf
	 .aload0 () // this
	 .getField ("com.pantor.blink.CompactWriter$Encoder", "tid", "[B")
	 .invokeVirtual ("com.pantor.blink.Buf", "write", "([B)V")
	 .aload1 ().checkCast (srcName).aload2 ().aload3 ()
	 .invokeStatic (encoderName, "innerEncode", innerSig)
	 .return_ ().setMaxStack (3).endMethod ();

      // public static void encodeArray (T [] objs, buf, rd)

      int loop = dc.declareLabel ();
      int loopEnd = dc.declareLabel ();

      dc.startPublicStaticMethod ("encodeArray",
				  getEncodeArraySignature (srcName));

      dc.aload0 (); // objs
      dc.arrayLength ();
      dc.dup ();
      dc.istore3 (); // size
      dc.aload1 (); // buf
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
	 .aload1 () // buf
	 .aload2 () // wr
	 .invokeStatic (encoderName, "innerEncode", innerSig)
	 .iinc (4, 1) // ++ i
	 .goto_ (loop)
	 .label (loopEnd)
	 .return_ ()
	 .setMaxStack (3)
	 .endMethod ();
      
      // static void innerEncode (src, buf, wr)
      
      dc.startPublicStaticMethod ("innerEncode", innerSig);

      // Emit encoding instructions for each field
      
      int minAllocSize = 0;
      
      for (ObjectModel.Field f : bnd)
	 minAllocSize += compile (bnd, f, dc);

      dc.return_ ().setMaxStack (3).endMethod ();

      // static void encodeBlank (buf)
      
      dc.startPublicStaticMethod ("encodeBlank", "(Lcom/pantor/blink/Buf;)V");

      // Emit blank encoding instructions for each field
      
      for (ObjectModel.Field f : bnd)
	 compileBlankField (f.getField (), 0, dc);

      dc.return_ ().setMaxStack (2).endMethod ();

      // Create an instance of the generated encoder
      
      byte [] tid = null;
      if (g.hasId ())
      {
	 Buf tidBuf = new Buf (Vlc.Int64MaxSize);
	 Vlc.writeU64 (g.getId (), tidBuf);
	 tidBuf.flip ();
	 tid = new byte [tidBuf.size ()];
	 tidBuf.read (tid);
	 minAllocSize += tid.length;
      }
      
      CompactWriter.Encoder enc = createInstance (tid, minAllocSize, dc, bnd);

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
      return "([L" + DynClass.toInternal (src) + ";Lcom/pantor/blink/Buf;" +
	 "Lcom/pantor/blink/CompactWriter;)V";
   }

   private static String getEncodeEnumSignature (ObjectModel.Binding bnd)
   {
      return "(L" + DynClass.toInternal (bnd.getTargetType ()) +
	 ";Lcom/pantor/blink/Buf;)V";
   }

   private static String getInnerEncodeSignature (ObjectModel.Binding bnd)
   {
      return "(L" + DynClass.toInternal (bnd.getTargetType ()) +
	 ";Lcom/pantor/blink/Buf;Lcom/pantor/blink/CompactWriter;)V";
   }

   private static String getEncodeEnumArraySignature (ObjectModel.Binding bnd)
   {
      return "([L" + DynClass.toInternal (bnd.getTargetType ()) +
	 ";Lcom/pantor/blink/Buf;)V";
   }

   private static void invokeWriter (DynClass dc, String m, String t)
   {
      dc.invokeStatic ("com/pantor/blink/CompactWriter", m,
		       "(" + t + "Lcom/pantor/blink/Buf;)V");
   }

   private static void invokeWriter (DynClass dc, String m)
   {
      invokeWriter (dc, m, "");
   }

   private int compile (ObjectModel.Binding bnd, ObjectModel.Field f,
			DynClass dc)
      throws BlinkException
   {
      int maxSize = 0;
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
	       maxSize = compilePrim (t, dc);
	    else if (t.isEnum ())
	       maxSize = compileEnum (t, f.getComponent ().toEnum (), dc);
	    else // Object or Group
	       maxSize = compileGroupField (f, dc);
	 }
	 else
	 {
	    maxSize = Vlc.Int32MaxSize;
	    if (t.isPrimitive ())
	       compilePrimSeq (t, dc);
	    else if (t.isEnum ())
	       compileEnumSeq (t, f.getComponent ().toEnum (), dc);
	    else // Object or Group
	       compileGroupArrayField (f, dc);
	 }

	 if (sf.isOptional ())
	 {
	    dc.goto_ (end);
	    dc.label (putNull);
	    dc.aload1 (); // Buf, #depth 1
	    invokeWriter (dc, "writeNull");
	 }
      
	 dc.label (end);
      }
      else
	 maxSize = compileBlankField (sf, t, 1, dc);
      return maxSize;
   }

   private static int compilePrim (Schema.TypeInfo t, DynClass dc)
   {
      dc.aload1 (); // Buf, #depth: 2
      invokeWriter (dc, "write" + t.getType ().getCode ().toString (),
		    CodecUtil.mapTypeDescr (t.getType ().getCode ()));
      return getMaxVlcSize (t.getType ().getCode ());
   }

   private int compileEnum (Schema.TypeInfo t, ObjectModel.EnumBinding comp,
			    DynClass dc)
      throws BlinkException
   {
      primeEnum (comp);
      dc.aload1 (); // Buf, #depth: 2
      dc.invokeStatic (getEncoderClassName (t.getEnum ().getName ()),
		       "encode", getEncodeEnumSignature (comp));
      return Vlc.Int32MaxSize;
   }

   private static void compilePrimSeq (Schema.TypeInfo t, DynClass dc)
   {
      Schema.TypeCode c = t.getType ().getCode ();
      String encMtod = "write" + c.toString () + "Array";
      dc.aload1 (); // Buf, #depth: 2
      invokeWriter (dc, encMtod, "[" + CodecUtil.mapTypeDescr (c));
   }

   private void compileEnumSeq (Schema.TypeInfo t, ObjectModel.EnumBinding comp,
				DynClass dc)
      throws BlinkException
   {
      primeEnum (comp);
      dc.aload1 (); // Buf, #depth: 2
      dc.invokeStatic (getEncoderClassName (t.getEnum ().getName ()),
		       "encodeArray",
		       getEncodeEnumArraySignature (comp));
   }
   
   private int compileGroupField (ObjectModel.Field f, DynClass dc)
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
	 return Vlc.Int32MaxSize;
      }
      else
      {
	 ObjectModel.GroupBinding comp = f.getComponent ().toGroup ();
	 primeGroup (comp.getGroup ().getName ());
	 if (sf.isOptional ())
	 {
	    dc.aload1 (); // Buf, #depth: 2
	    invokeWriter (dc, "writeOne"); // Presence byte
	 }
	 dc.aload1 (); // Buf, #depth: 2
	 dc.aload2 (); // Writer, #depth: 3
	 dc.invokeStatic (getEncoderClassName (comp.getGroup ().getName ()),
			  "innerEncode", getInnerEncodeSignature (comp));

	 return getMaxSize (comp) + (sf.isOptional () ? 1 : 0);
      }
   }

   private void compileGroupArrayField (ObjectModel.Field f, DynClass dc)
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
	 dc.aload1 (); // Buf, #depth: 2
	 dc.aload2 (); // Writer, #depth: 3
	 dc.invokeStatic (getEncoderClassName (comp.getGroup ().getName ()),
			  "encodeArray", sig);
      }
   }

   private int compileBlankField (Schema.Field sf, int bufLocal, DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = om.getSchema ().resolve (sf.getType ());
      return compileBlankField (sf, t, bufLocal, dc);
   }
   
   private int compileBlankField (Schema.Field sf, Schema.TypeInfo t,
				  int bufLocal, DynClass dc)
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
	 dc.aload (bufLocal); // Buf, #depth 1
	 invokeWriter (dc, "writeNull");
	 return 1;
      }
      else if (t.isEnum ())
      {
	 // Write the value of the first symbol
	 Schema.Enum e = t.getEnum ().getType ().toEnum ();
	 dc.ldc (e.getSymbols ().get (0).getValue ());
	 dc.aload (bufLocal); // Buf, #depth 2
	 invokeWriter (dc, "writeI32", "I");
	 return Vlc.Int32MaxSize;
      }
      else if (t.isGroup () && ! t.isSequence () && ! t.isDynamic ())
      {
	 primeGroup (t.getGroup ().getName ());
	 dc.aload (bufLocal); // Buf, #depth: 2
	 dc.invokeStatic (getEncoderClassName (t.getGroup ().getName ()),
			  "encodeBlank", "(Lcom/pantor/blink/Buf;)V");
	 return getMaxBlankSize (t.getGroup ());
      }
      else
      {
	 dc.aload (bufLocal); // Buf, #depth 1
	 invokeWriter (dc, "writeZero");
	 return 1;
      }
   }

   private static int getMaxSize (ObjectModel.GroupBinding bnd)
   {
      int maxSize = 0;
      for (ObjectModel.Field f : bnd)
      {
	 Schema.TypeInfo t = f.getFieldType ();
	 Schema.Field sf = f.getField ();
	 if (t.isSequence () || t.isEnum () || t.isDynamic () || t.isObject ())
	    maxSize += Vlc.Int32MaxSize;
	 else if (t.isPrimitive ())
	    maxSize += getMaxVlcSize (t.getType ().getCode ());
	 else
	    maxSize += getMaxSize (f.getComponent ().toGroup ()) +
	       (sf.isOptional () ? 1 : 0);
      }
      return maxSize;
   }

   private int getMaxBlankSize (Schema.Group g)
      throws BlinkException
   {
      int maxSize = 0;
      for (Schema.Field f : g)
      {
	 if (f.isOptional ())
	    maxSize += 1;
	 else
	 {
	    Schema.TypeInfo t = om.getSchema ().resolve (f.getType ());
	    if (t.isEnum ())
	       maxSize += Vlc.Int32MaxSize;
	    else if (t.isGroup () && ! t.isSequence () && ! t.isDynamic ())
	       maxSize += getMaxBlankSize (t.getGroup ());
	    else
	       maxSize += 1;
	 }
      }
      return maxSize;
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
   //      public static void encode (T sym, Buf buf)
   //      {
   //         CompactWriter.writeI32 (map.get (sym), buf);
   //      }
   //  
   //      public static void encodeArray (T [] syms, Buf buf)
   //      {
   //         CompactWriter.writeU32 (syms.length, buf);
   //         for (int i = 0; i < syms.length; ++ i)
   //           encode (src [i], buf);
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

      // static static void encode (sym, buf)

      dc.startPublicStaticMethod ("encode", getEncodeEnumSignature (bnd))
	 .getStatic (encoderName, "map", "Ljava/util/EnumMap;")
	 .aload0 () // sym
	 .invokeVirtual ("java.util.EnumMap", "get",
			 "(Ljava/lang/Object;)Ljava/lang/Object;")
	 .checkCast ("java.lang.Integer")
	 .aload1 (); // buf
      invokeWriter (dc, "writeEnumVal", "Ljava/lang/Integer;");
      dc.return_ ().setMaxStack (2).endMethod ();
      
      // public static void encodeArray (syms, buf)

      int loop = dc.declareLabel ();
      int loopEnd = dc.declareLabel ();

      dc.startPublicStaticMethod ("encodeArray",
				  getEncodeEnumArraySignature (bnd));
      dc.aload0 (); // syms
      dc.arrayLength ();
      dc.dup ();
      dc.istore2 (); // size
      dc.aload1 (); // buf
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
	 .aload1 () // buf
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

   private CompactWriter.Encoder createInstance (byte [] tid, int minSize,
      DynClass dc, ObjectModel.GroupBinding bnd)
      throws BlinkException.Binding
   {
      try
      {
	 Class<?> tgtType = bnd.getTargetType ();
	 Class<?> encClass = dload.loadPrivileged (dc, tgtType);
	 Constructor<?> ctor = encClass.getConstructor (
	    byte [].class, int.class, Class.class, Schema.Group.class);
	 
	 return (CompactWriter.Encoder)ctor.newInstance (
	    tid, minSize, tgtType, bnd.getGroup ());
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
   
   private final HashMap<Class<?>, CompactWriter.Encoder> encByClass =
      new HashMap<Class<?>, CompactWriter.Encoder> ();
   private final HashMap<NsName, CompactWriter.Encoder> encByName =
      new HashMap<NsName, CompactWriter.Encoder> ();
   private final ObjectModel om;
   private final DynClassLoader dload = new DynClassLoader ();
   private final HashSet<NsName> enumEncs = new HashSet <NsName> ();
}
