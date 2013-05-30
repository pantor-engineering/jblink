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

public final class CompactReaderCompiler
{
   public CompactReaderCompiler (ObjectModel om)
   {
      this (om, null);
   }

   public CompactReaderCompiler (ObjectModel om, ObserverRegistry oreg)
   {
      this.om = om;
      this.oreg = oreg;
   }

   public CompactReader.Decoder getDecoder (long tid)
      throws BlinkException
   {
      CompactReader.Decoder d = decByTid.get (tid);
      if (d != null)
	 return d;
      else
	 return compile (tid);
   }

   public CompactReader.Decoder getDecoder (NsName name)
      throws BlinkException
   {
      CompactReader.Decoder d = decByName.get (name);
      if (d != null)
	 return d;
      else
	 return compile (om.getGroupBinding (name));
   }

   public void prime (NsName name)
      throws BlinkException
   {
      getDecoder (name);
   }

   public void primeEnum (ObjectModel.EnumBinding bnd)
      throws BlinkException
   {
      if (! enumDecs.contains (bnd.getEnum ().getName ()))
	 compileEnum (bnd);
   }

   private CompactReader.Decoder compile (long tid)
      throws BlinkException
   {
      ObjectModel.GroupBinding bnd = om.getGroupBinding (tid);
      CompactReader.Decoder d = getDecoder (bnd.getGroup ().getName ());
      if (d == null)
	 d = compile (bnd);
      decByTid.put (tid, d);
      return d;
   }

   // Generates a decoder for the specified binding. The decoder has
   // the following general layout

   //   package com.pantor.blink.dyn.compact;
   //
   //   public final class <Ns>+<Name>_dec extends CompactReader.Decoder
   //   {
   //      public <Ns>+<Name>_dec (Class type, Schema.Group grp, Observer obs)
   //      {
   //         super (type, grp, obs);
   //      }
   //
   //      @Override
   //      public void decode (ByteSource src, Object tgt, CompactReader rd)
   //      {
   //         innerDecode (src, (T)tgt, rd);
   //      }
   //  
   //      @Override
   //      public Object newInstance ()
   //      {
   //         return new T ();
   //      }
   //  
   //      public static T read (ByteSource src, CompactReader rd)
   //      {
   //         T tgt = new T ();
   //         innerDecode (src, tgt, rd);
   //         return tgt;
   //      }
   //  
   //      public static T [] readArray (ByteSource src, CompactReader rd)
   //      {
   //         int size = CompactReader.readU32 (src);
   //         T [] tgt = new T [size];
   //         for (int i = 0; i < size; ++ i)
   //           tgt [i] = read (src, rd);
   //         return tgt;
   //      }
   //  
   //      public static void innerDecode (ByteSource src, T tgt,
   //                                      CompactReader rd)
   //      {
   //         ... decode src and populate tgt ...
   //      }
   //   }
   
   private CompactReader.Decoder compile (ObjectModel.GroupBinding bnd)
      throws BlinkException
   {
      Schema.Group g = bnd.getGroup ();
      Observer obs = oreg != null ? oreg.findObserver (g) : null;

      String decoderName = getDecoderClassName (g.getName ());

      // Generate decoder class
      
      DynClass dc = new DynClass (decoderName);
      dc.setFlags (DynClass.ClassFlag.Final);

      String ctorSig = "(Ljava/lang/Class;Lcom/pantor/blink/Schema$Group;" +
	 "Lcom/pantor/blink/Observer;)V";
      
      dc.setSuper ("com.pantor.blink.CompactReader$Decoder");

      // Constructor
      
      dc.startPublicMethod ("<init>", ctorSig)
	 .aload0 ().aload1 ().aload2 ().aload3 ()
	 .invokeSpecial ("com/pantor/blink/CompactReader$Decoder",
			 "<init>", ctorSig)
	 .return_ ().setMaxStack (4).endMethod ();

      // void decode (src, tgt, rd)

      String decSig = "(Lcom/pantor/blink/ByteSource;Ljava/lang/Object;" +
	 "Lcom/pantor/blink/CompactReader;)V";

      String tgtName = bnd.getTargetType ().getName ();

      String innerSig = "(Lcom/pantor/blink/ByteSource;L" +
	 DynClass.toInternal (tgtName) + ";Lcom/pantor/blink/CompactReader;)V";
      
      dc.startPublicMethod ("decode", decSig)
	 .aload1 ().aload2 ().checkCast (tgtName).aload3 ()
	 .invokeStatic (decoderName, "innerDecode", innerSig)
	 .return_ ().setMaxStack (3).endMethod ();

      // Object newInstance ()

      dc.startPublicMethod ("newInstance", "()Ljava/lang/Object;")
	 .new_ (tgtName)
	 .dup ()
	 .invokeSpecial (tgtName, "<init>", "()V")
	 .areturn ().setMaxStack (2).endMethod ();
      
      // static T read (src, rd)

      dc.startPublicStaticMethod ("read", getReadSignature (tgtName))
	 .new_ (tgtName)
	 .dup ()
	 .invokeSpecial (tgtName, "<init>", "()V")
	 .astore2 ()
	 .aload0 () // src
	 .aload2 () // Target
	 .aload1 () // Reader
	 .invokeStatic (decoderName, "innerDecode", innerSig)
	 .aload2 () // Target
	 .areturn ().setMaxStack (3).endMethod ();

      // public static T [] readArray (src, rd)

      int loop = dc.declareLabel ();
      int loopEnd = dc.declareLabel ();

      dc.startPublicStaticMethod ("readArray", getReadArraySignature (tgtName));

      dc.aload0 (); // src
      invokeReader (dc, "readU32", "I"); // size
      dc.dup ()
	 .istore2 ()
	 .anewArray (tgtName)
	 .astore3 () // tgt
	 .iconst0 ()
	 .istore (4) // i = 0
	 .label (loop)
	 .iload (4) // i
	 .iload2 () // size
	 .ifIcmpGe (loopEnd) // jump if i >= size
	 .aload3 () // tgt
	 .iload (4) // i
	 .aload0 () // src
	 .aload1 () // rd
	 .invokeStatic (decoderName, "read", getReadSignature (tgtName))
	 .aastore () // tgt [i] = <returned value>
	 .iinc (4, 1) // ++ i
	 .goto_ (loop)
	 .label (loopEnd)
	 .aload3 ()
	 .areturn ()
	 .setMaxStack (4)
	 .endMethod ();
      
      // static void innerDecode (src, tgt, rd)
      
      dc.startPublicStaticMethod ("innerDecode", innerSig);

      // Emit decoding instructions for each field
      
      for (ObjectModel.Field f : bnd)
	 compile (bnd, f, dc);

      dc.return_ ();
      dc.setMaxStack (4);
      dc.endMethod ();

      // Create an instance of the generated decoder
      
      CompactReader.Decoder d = createInstance (dc, bnd, obs);

      // Store this instance for future lookups
      
      decByName.put (g.getName (), d);
	 
      return d;
   }

   private static String getClassName (NsName nm)
   {
      if (nm.isQualified ())
	 return nm.getNs () + "+" + nm.getName ();
      else
	 return nm.getName ();
   }
   
   private static String getDecoderClassName (NsName nm)
   {
      return "com.pantor.blink.dyn.compact." + getClassName (nm) + "_dec";
   }

   private static String getReadSignature (String tgt)
   {
      return
	 "(Lcom/pantor/blink/ByteSource;Lcom/pantor/blink/CompactReader;)L" +
	 DynClass.toInternal (tgt) + ";";
   }

   private static String getReadArraySignature (String tgt)
   {
      return
	 "(Lcom/pantor/blink/ByteSource;Lcom/pantor/blink/CompactReader;)[L" +
	 DynClass.toInternal (tgt) + ";";
   }

   private static String getReadEnumSignature (ObjectModel.Binding bnd)
   {
      return "(Lcom/pantor/blink/ByteSource;)L" +
	 DynClass.toInternal (bnd.getTargetType ().getName ()) + ";";
   }

   private static String getReadEnumArraySignature (ObjectModel.Binding bnd)
   {
      return "(Lcom/pantor/blink/ByteSource;)[L" +
	 DynClass.toInternal (bnd.getTargetType ().getName ()) + ";";
   }

   private void invokeReader (DynClass dc, String m, String t)
   {
      dc.invokeStatic ("com/pantor/blink/CompactReader", m,
		       "(Lcom/pantor/blink/ByteSource;)" + t);
   }

   private void compile (ObjectModel.Binding bnd, ObjectModel.Field f,
			 DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      Method setter = f.getSetter ();

      // FIXME: Handle compound decimal setter
      
      int end = dc.declareLabel ();
      if (sf.isOptional ())
      {
	 dc.aload0 (); // src, #depth: 1
	 invokeReader (dc, "readNull", "Z");
	 dc.ifNe (end); // Jump if null
      }

      dc.aload1 (); // target, #depth: 1

      if (! t.isSequence ())
      {
	 if (t.isPrimitive ())
	 {
	    dc.aload0 (); // src, #depth: 2
	    String decMtod = "read" + t.getType ().getCode ().toString ();
	    String retType = CodegenUtil.mapTypeDescr (t.getType ().getCode ());
	    invokeReader (dc, decMtod, retType);
	 }
	 else if (t.isEnum ())
	 {
	    ObjectModel.EnumBinding comp = f.getComponent ().toEnum ();
	    primeEnum (comp);
	    dc.aload0 (); // src, #depth: 2
	    dc.invokeStatic (getDecoderClassName (t.getEnum ().getName ()),
			     "read",
			     getReadEnumSignature (comp));
	 }
	 else // Object or Group
	    compileGroupField (f, dc);
      }
      else
      {
	 if (t.isPrimitive ())
	 {
	    Schema.TypeCode c = t.getType ().getCode ();
	    String decMtod = "read" + c.toString () + "Array";
	    String retType = "[" + CodegenUtil.mapTypeDescr (c);
	    dc.aload0 (); // src, #depth: 2
	    dc.invokeStatic ("com/pantor/blink/CompactReader", decMtod,
			     "(Lcom/pantor/blink/ByteSource;)" + retType);
	 }
	 else if (t.isEnum ())
	 {
	    ObjectModel.EnumBinding comp = f.getComponent ().toEnum ();
	    primeEnum (comp);
	    dc.aload0 (); // src, #depth: 2
	    dc.invokeStatic (getDecoderClassName (t.getEnum ().getName ()),
			     "readArray",
			     getReadEnumArraySignature (comp));
	 }
	 else // Object or Group
	    compileGroupArrayField (f, dc);
      }

      if (setter != null)
	 dc.invoke (setter);
      else
	 // FIXME: Replace with skip instructions above instead since we know
	 // we're not going to use the result anyway
	 dc.pop ();
      dc.label (end);
   }

   private void compileGroupField (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      Method setter = f.getSetter ();
      Class<?> argType = null;
      if (setter != null)
	 argType = setter.getParameterTypes () [0];
      
      if (t.isDynamic () || t.isObject ())
      {
	 dc.aload2 (); // Reader, #depth: 2
	 dc.aload0 (); // src, #depth: 3
	 dc.invokeVirtual ("com.pantor.blink.CompactReader", "readObject",
			   "(Lcom/pantor/blink/ByteSource;)Ljava/lang/Object;");
	 if (argType != null && argType != Object.class)
	    dc.checkCast (argType);
      }
      else
      {
	 ObjectModel.GroupBinding comp = f.getComponent ().toGroup ();
	 NsName compName = comp.getGroup ().getName ();
	 prime (compName);
	 if (sf.isOptional ())
	    invokeReader (dc, "skipByte", "V"); // Skip presence byte
	 String sig = getReadSignature (comp.getTargetType ().getName ());
	 dc.aload0 (); // src, #depth: 2
	 dc.aload2 (); // Reader, #depth: 3
	 dc.invokeStatic (getDecoderClassName (compName), "read", sig);
      }
   }

   private void compileGroupArrayField (ObjectModel.Field f, DynClass dc)
      throws BlinkException
   {
      Schema.TypeInfo t = f.getFieldType ();
      Schema.Field sf = f.getField ();
      Method setter = f.getSetter ();
      Class<?> argType = null;
      if (setter != null)
	 argType = setter.getParameterTypes () [0];
      
      if (t.isObject ())
      {
	 dc.aload2 (); // Reader, #depth: 2
	 dc.aload0 (); // src, #depth: 3
	 dc.invokeVirtual (
	    "com.pantor.blink.CompactReader", "readObjectArray",
	    "(Lcom/pantor/blink/ByteSource;)[Ljava/lang/Object;");
	 if (argType != null && argType != Object [].class)
	    dc.checkCast (argType);
      }
      else if (t.isDynamic ())
      {
	 ObjectModel.GroupBinding comp = f.getComponent ().toGroup ();
	 dc.aload2 (); // Reader, #depth: 2
	 dc.aload0 (); // src, #depth: 3
	 invokeReader (dc, "readU32", "I"); // size
	 dc.anewArray (comp.getTargetType ());
	 dc.aload0 (); // src, #depth: 4
	 dc.invokeVirtual (
	    "com.pantor.blink.CompactReader", "readObjectArray",
	    "([Ljava/lang/Object;Lcom/pantor/blink/ByteSource;)" +
	    "[Ljava/lang/Object;");
	 if (argType != null && argType != Object [].class)
	    dc.checkCast (argType);
      }
      else
      {
	 ObjectModel.GroupBinding comp = f.getComponent ().toGroup ();
	 NsName compName = comp.getGroup ().getName ();
	 prime (compName);
	 String sig = getReadArraySignature (comp.getTargetType ().getName ());
	 dc.aload0 (); // src, #depth: 2
	 dc.aload2 (); // Reader, #depth: 3
	 dc.invokeStatic (getDecoderClassName (compName), "readArray", sig);
      }
   }

   // Generates an enum decoder for the specified binding. The decoder has
   // the following general layout

   //   package com.pantor.blink.dyn.compact;
   //
   //   public final class <Ns>+<Name>_dec
   //   {
   //      public static T read (ByteSource src)
   //      {
   //         return map.get (CompactReader.readI32 (src));
   //      }
   //  
   //      public static T [] readArray (ByteSource src)
   //      {
   //         int size = CompactReader.readU32 (src);
   //         T [] tgt = new T [size];
   //         for (int i = 0; i < size; ++ i)
   //           tgt [i] = read (src);
   //         return tgt;
   //      }
   //
   //      private final static HashMap<Integer, T> map;
   //
   //      static
   //      {
   //         map = new HashMap<Integer, T> ();
   //         map.put (0, T.Red);
   //         map.put (1, T.Green);
   //         map.put (2, T.Blue);
   //      }
   //      
   //   }
   
   private void compileEnum (ObjectModel.EnumBinding bnd)
      throws BlinkException
   {
      Schema.Define d = bnd.getEnum ();
      String decoderName = getDecoderClassName (d.getName ());

      // Generate decoder class
      
      DynClass dc = new DynClass (decoderName);
      dc.setFlags (DynClass.ClassFlag.Final);

      Class<?> enumType = bnd.getTargetType ();

      dc.addField ("map", "Ljava/util/HashMap;", DynClass.FieldFlag.Private,
		   DynClass.FieldFlag.Final, DynClass.FieldFlag.Static);
      
      // static T read (src)

      dc.startPublicStaticMethod ("read", getReadEnumSignature (bnd));
      dc.getStatic (decoderName, "map", "Ljava/util/HashMap;");
      dc.aload0 (); // src
      invokeReader (dc, "readI32", "I");
      dc.invokeStatic ("java.lang.Integer", "valueOf",
		       "(I)Ljava/lang/Integer;");
      dc.invokeVirtual ("java.util.HashMap", "get",
			"(Ljava/lang/Object;)Ljava/lang/Object;")
	 .checkCast (enumType).areturn ().setMaxStack (2).endMethod ();

      // public static T [] readArray (src)

      int loop = dc.declareLabel ();
      int loopEnd = dc.declareLabel ();

      dc.startPublicStaticMethod ("readArray", getReadEnumArraySignature (bnd));
      dc.aload0 (); // src
      invokeReader (dc, "readU32", "I"); // size
      dc.dup ()
	 .istore1 ()
	 .anewArray (enumType)
	 .astore2 () // tgt
	 .iconst0 ()
	 .istore3 () // i = 0
	 .label (loop)
	 .iload3 () // i
	 .iload1 () // size
	 .ifIcmpGe (loopEnd) // jump if i >= size
	 .aload2 () // tgt
	 .iload3 () // i
	 .aload0 () // src
	 .invokeStatic (decoderName, "read", getReadEnumSignature (bnd))
	 .aastore () // tgt [i] = <returned value>
	 .iinc (3, 1) // ++ i
	 .goto_ (loop)
	 .label (loopEnd)
	 .aload2 ()
	 .areturn ()
	 .setMaxStack (3)
	 .endMethod ();
      
      // static init
      
      dc.startMethod ("<clinit>", "()V", DynClass.MtodFlag.Static);

      dc.new_ ("java.util.HashMap");
      dc.dup ();
      dc.invokeSpecial ("java.util.HashMap", "<init>", "()V");
      dc.dup ();
      dc.putStatic (decoderName, "map", "Ljava/util/HashMap;");
      dc.astore0 ();
      
      for (ObjectModel.Symbol sym : bnd)
      {
	 dc.aload0 ();
	 dc.ldc (sym.getSymbol ().getValue ());
	 dc.invokeStatic ("java.lang.Integer", "valueOf",
			  "(I)Ljava/lang/Integer;");
	 if (sym.getTargetName () != null)
	 {
	    dc.ldc (sym.getTargetName ());
	    // FIXME: Replace with static field access
	    dc.invokeStatic (enumType, "valueOf", "(Ljava/lang/String;)L" +
			     DynClass.toInternal (enumType) + ";");
	 }
	 else
	    dc.aconstNull ();

	 dc.invokeVirtual (
	    "java.util.HashMap", "put",
	    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	 dc.pop ();
      }

      dc.return_ ();
      dc.setMaxStack (3);
      dc.endMethod ();

      // Load the class

      dload.loadPrivileged (dc, enumType);

      enumDecs.add (d.getName ());
   }
   
   private CompactReader.Decoder createInstance (
      DynClass dc, ObjectModel.GroupBinding bnd, Observer obs)
      throws BlinkException
   {
      try
      {
	 Class<?> tgtType = bnd.getTargetType ();
	 Class<?> decClass = dload.loadPrivileged (dc, tgtType);
	 Constructor<?> ctor = decClass.getConstructor (
	    Class.class, Schema.Group.class, Observer.class);

	 return (CompactReader.Decoder)ctor.newInstance (
	    tgtType, bnd.getGroup (), obs);
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
   
   private final HashMap<Long, CompactReader.Decoder> decByTid =
      new HashMap<Long, CompactReader.Decoder> ();
   private final HashMap<NsName, CompactReader.Decoder> decByName =
      new HashMap<NsName, CompactReader.Decoder> ();
   private final ObserverRegistry oreg;
   private final ObjectModel om;
   private final DynClassLoader dload = new DynClassLoader ();
   private final HashSet<NsName> enumDecs = new HashSet <NsName> ();
}
