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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.reflect.Modifier;

public final class DynClass
{
   private static interface Flag { int getVal (); }
   
   public static enum ClassFlag implements Flag
   {
      Public (0x0001),
      Final (0x0010),
      Super (0x0020),
      Interface (0x0200),
      Abstract (0x0400),
      Synthetic (0x1000),
      Annotation (0x2000),
      Enum (0x4000);

      ClassFlag (int val) { this.val = val; }
      @Override public int getVal () { return val; }
      private int val;
   }

   public static enum MtodFlag implements Flag
   {
      Public (0x0001),
      Private (0x0002),
      Protected (0x0004),
      Static (0x0008),
      Final (0x0010),
      Synchronized (0x0020),
      Bridge (0x0040),
      Varargs (0x0080),
      Native (0x0100),
      Abstract (0x0400),
      Strict (0x0800),
      Synthetic (0x1000);

      MtodFlag (int val) { this.val = val; }
      @Override public int getVal () { return val; }
      private int val;
   }

   public static enum FieldFlag implements Flag
   {
      Public (0x0001),
      Private (0x0002),
      Protected (0x0004),
      Static (0x0008),
      Final (0x0010),
      Volatile (0x0040),
      Transient (0x0080),
      Synthetic (0x1000),
      Enum (0x4000);
      
      FieldFlag (int val) { this.val = val; }
      @Override public int getVal () { return val; }
      private int val;
   }

   public static enum Type
   {
      Boolean (4),
      Char (5),
      Float (6),
      Double (7),
      Byte (8),
      Short (9),
      Int (10),
      Long (11);

      Type (int val) { this.val = val; }
      public byte getVal () { return (byte)val; }
      private int val;
   }

   public DynClass (String name)
   {
      this (name, DefaultMajorVer, DefaultMinorVer);
   }

   public DynClass (String name, int majorVer)
   {
      this (name, DefaultMajorVer, 0);
   }

   public DynClass (String name, int majorVer, int minorVer)
   {
      this.name = name;
      this.minorVer = minorVer;
      this.majorVer = majorVer;
   }

   public DynClass addInterface (String... ifaces)
   {
      interfaces.addAll (Arrays.asList (ifaces));
      return this;
   }

   public DynClass setSuper (String superName)
   {
      this.superName = superName;
      return this;
   }

   public DynClass startMethod (String name, String sig, MtodFlag... flags)
   {
      if (curMtod != null)
         endMethod ();
      curMtod = new Method (name, sig, mergeFlags (flags));
      methods.add (curMtod);
      return this;
   }

   public DynClass addField (String name, String type, FieldFlag... flags)
   {
      fields.add (new Field (name, type, mergeFlags (flags)));
      return this;
   }

   public DynClass startPublicMethod (String name, String sig,
                                      MtodFlag... flags)
   {
      MtodFlag [] extended = new MtodFlag [flags.length + 1];
      System.arraycopy (flags, 0, extended, 0, flags.length);
      extended [flags.length] = MtodFlag.Public;
      startMethod (name, sig, extended);
      return this;
   }

   public DynClass startPublicStaticMethod (String name, String sig)
   {
      startPublicMethod (name, sig, MtodFlag.Static);
      return this;
   }

   public DynClass endMethod ()
   {
      curMtod = null;
      return this;
   }

   public DynClass addDefaultConstructor ()
   {
      startPublicMethod ("<init>", "()V")
         .aload0 ().invokeSpecial ("java.lang.Object", "<init>", "()V")
         .return_ ().setMaxStack (1).endMethod ();
      return this;
   }
   
   public String getName ()
   {
      return name;
   }

   public void setFlags (ClassFlag... flags)
   {
      this.flags |= mergeFlags (flags);
   }

   public void resetFlags ()
   {
      flags = ClassFlag.Super.getVal ();
   }
   
   public byte [] render ()
   {
      try
      {
         nextConst = 1;
         utf8ConstPool.clear ();
         classConstPool.clear ();
         constPoolBs = new ByteArrayOutputStream ();
         constPoolOs = new DataOutputStream (constPoolBs);
         
         ByteArrayOutputStream bs = new ByteArrayOutputStream ();
         DataOutputStream os = new DataOutputStream (bs);
         byte [] body = renderBody ();
         os.writeInt (Cookie);
         os.writeShort (minorVer);
         os.writeShort (majorVer);
         os.writeShort (nextConst);
         constPoolOs.flush ();
         os.write (constPoolBs.toByteArray ());
         os.write (body);
         os.flush ();
         return bs.toByteArray ();
      }
      catch (IOException e)
      {
         throw new RuntimeException (e);
      }
   }

   public int declareLabel ()
   {
      return nextLabel ++;
   }

   public int declareLabel (String sym)
   {
      int l = nextLabel ++;
      labelBySym.put (sym, l);
      return l;
   }
   
   // Instructions
   //////////////////////////////////////////////////////////////////////

   public DynClass aaload ()
   {
      curMtod.addIns (0x32);
      return this;
   }

   public DynClass aastore ()
   {
      curMtod.addIns (0x53);
      return this;
   }

   public DynClass aconstNull ()
   {
      curMtod.addIns (0x01);
      return this;
   }

   public DynClass aload (int index)
   {
      if (index >= 0 && index < 4)
      {
         curMtod.addIns (0x2a + index);
         curMtod.setMaxLocal (index);
         return this;
      }
      else
         return addPossiblyWideIns (0x19, index);
   }

   public DynClass aload0 ()
   {
      curMtod.addIns (0x2a);
      curMtod.setMaxLocal (0);
      return this;
   }

   public DynClass aload1 ()
   {
      curMtod.addIns (0x2b);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass aload2 ()
   {
      curMtod.addIns (0x2c);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass aload3 ()
   {
      curMtod.addIns (0x2d);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass anewArray (String className)
   {
      curMtod.addInsClassOp (0xbd, className);
      return this;
   }

   public DynClass anewArray (Class<?> c)
   {
      curMtod.addInsClassOp (0xbd, c.getName ());
      return this;
   }

   public DynClass areturn ()
   {
      curMtod.addIns (0xb0);
      return this;
   }

   public DynClass arrayLength ()
   {
      curMtod.addIns (0xbe);
      return this;
   }

   public DynClass astore (int index)
   {
      return addPossiblyWideIns (0x3a, index);
   }

   public DynClass astore0 ()
   {
      curMtod.addIns (0x4b);
      curMtod.setMaxLocal (0);
      return this;
   }

   public DynClass astore1 ()
   {
      curMtod.addIns (0x4c);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass astore2 ()
   {
      curMtod.addIns (0x4d);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass astore3 ()
   {
      curMtod.addIns (0x4e);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass athrow ()
   {
      curMtod.addIns (0xbf);
      return this;
   }

   public DynClass baload ()
   {
      curMtod.addIns (0x33);
      return this;
   }

   public DynClass bastore ()
   {
      curMtod.addIns (0x54);
      return this;
   }

   public DynClass bipush (int b)
   {
      curMtod.addIns (0x10, (byte)b);
      return this;
   }

   public DynClass caload ()
   {
      curMtod.addIns (0x34);
      return this;
   }

   public DynClass castore ()
   {
      curMtod.addIns (0x55);
      return this;
   }

   public DynClass checkCast (String className)
   {
      curMtod.addInsClassOp (0xc0, className);
      return this;
   }

   public DynClass checkCast (Class<?> c)
   {
      curMtod.addInsClassOp (0xc0, c.getName ());
      return this;
   }

   public DynClass d2f ()
   {
      curMtod.addIns (0x90);
      return this;
   }

   public DynClass d2i ()
   {
      curMtod.addIns (0x8e);
      return this;
   }

   public DynClass d2l ()
   {
      curMtod.addIns (0x8f);
      return this;
   }

   public DynClass dadd ()
   {
      curMtod.addIns (0x63);
      return this;
   }

   public DynClass daload ()
   {
      curMtod.addIns (0x31);
      return this;
   }

   public DynClass dastore ()
   {
      curMtod.addIns (0x52);
      return this;
   }

   public DynClass dcmpg ()
   {
      curMtod.addIns (0x98);
      return this;
   }

   public DynClass dcmpl ()
   {
      curMtod.addIns (0x97);
      return this;
   }

   public DynClass dconst0 ()
   {
      curMtod.addIns (0x0e);
      return this;
   }

   public DynClass dconst1 ()
   {
      curMtod.addIns (0x0f);
      return this;
   }

   public DynClass ddiv ()
   {
      curMtod.addIns (0x6f);
      return this;
   }

   public DynClass dload (int index)
   {
      if (index >= 0 && index < 4)
      {
         curMtod.addIns (0x26 + index);
         curMtod.setMaxLocal (index + 1);
         return this;
      }
      else
         return addPossiblyWideIns (0x18, index, 2);
   }

   public DynClass dload0 ()
   {
      curMtod.addIns (0x26);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass dload1 ()
   {
      curMtod.addIns (0x27);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass dload2 ()
   {
      curMtod.addIns (0x28);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass dload3 ()
   {
      curMtod.addIns (0x29);
      curMtod.setMaxLocal (4);
      return this;
   }

   public DynClass dmul ()
   {
      curMtod.addIns (0x6b);
      return this;
   }

   public DynClass dneg ()
   {
      curMtod.addIns (0x77);
      return this;
   }

   public DynClass drem ()
   {
      curMtod.addIns (0x73);
      return this;
   }

   public DynClass dreturn ()
   {
      curMtod.addIns (0xaf);
      return this;
   }

   public DynClass dstore (int index)
   {
      return addPossiblyWideIns (0x39, index, 2);
   }

   public DynClass dstore0 ()
   {
      curMtod.addIns (0x47);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass dstore1 ()
   {
      curMtod.addIns (0x48);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass dstore2 ()
   {
      curMtod.addIns (0x49);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass dstore3 ()
   {
      curMtod.addIns (0x4a);
      curMtod.setMaxLocal (4);
      return this;
   }

   public DynClass dsub ()
   {
      curMtod.addIns (0x67);
      return this;
   }

   public DynClass dup ()
   {
      curMtod.addIns (0x59);
      return this;
   }

   public DynClass dupX1 ()
   {
      curMtod.addIns (0x5a);
      return this;
   }

   public DynClass dupX2 ()
   {
      curMtod.addIns (0x5b);
      return this;
   }

   public DynClass dup2 ()
   {
      curMtod.addIns (0x5c);
      return this;
   }

   public DynClass dup2X1 ()
   {
      curMtod.addIns (0x5d);
      return this;
   }

   public DynClass dup2X2 ()
   {
      curMtod.addIns (0x5e);
      return this;
   }

   public DynClass f2d ()
   {
      curMtod.addIns (0x8d);
      return this;
   }

   public DynClass f2i ()
   {
      curMtod.addIns (0x8b);
      return this;
   }

   public DynClass f2l ()
   {
      curMtod.addIns (0x8c);
      return this;
   }

   public DynClass fadd ()
   {
      curMtod.addIns (0x62);
      return this;
   }

   public DynClass faload ()
   {
      curMtod.addIns (0x30);
      return this;
   }

   public DynClass fastore ()
   {
      curMtod.addIns (0x51);
      return this;
   }

   public DynClass fcmpg ()
   {
      curMtod.addIns (0x96);
      return this;
   }

   public DynClass fcmpl ()
   {
      curMtod.addIns (0x95);
      return this;
   }

   public DynClass fconst0 ()
   {
      curMtod.addIns (0x0b);
      return this;
   }

   public DynClass fconst1 ()
   {
      curMtod.addIns (0x0c);
      return this;
   }

   public DynClass fconst2 ()
   {
      curMtod.addIns (0x0d);
      return this;
   }

   public DynClass fdiv ()
   {
      curMtod.addIns (0x6e);
      return this;
   }

   public DynClass fload (int index)
   {
      if (index >= 0 && index < 4)
      {
         curMtod.addIns (0x22 + index);
         curMtod.setMaxLocal (index);
         return this;
      }
      else
         return addPossiblyWideIns (0x17, index);
   }

   public DynClass fload0 ()
   {
      curMtod.addIns (0x22);
      curMtod.setMaxLocal (0);
      return this;
   }

   public DynClass fload1 ()
   {
      curMtod.addIns (0x23);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass fload2 ()
   {
      curMtod.addIns (0x24);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass fload3 ()
   {
      curMtod.addIns (0x25);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass fmul ()
   {
      curMtod.addIns (0x6a);
      return this;
   }

   public DynClass fneg ()
   {
      curMtod.addIns (0x76);
      return this;
   }

   public DynClass frem ()
   {
      curMtod.addIns (0x72);
      return this;
   }

   public DynClass freturn ()
   {
      curMtod.addIns (0xae);
      return this;
   }

   public DynClass fstore (int index)
   {
      return addPossiblyWideIns (0x38, index);
   }

   public DynClass fstore0 ()
   {
      curMtod.addIns (0x43);
      curMtod.setMaxLocal (0);
      return this;
   }

   public DynClass fstore1 ()
   {
      curMtod.addIns (0x44);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass fstore2 ()
   {
      curMtod.addIns (0x45);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass fstore3 ()
   {
      curMtod.addIns (0x46);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass fsub ()
   {
      curMtod.addIns (0x66);
      return this;
   }

   public DynClass getField (String className, String field, String type)
   {
      curMtod.addInsFieldOp (0xb4, className, field, type);
      return this;
   }

   public DynClass getField (Class<?> c, String field, String type)
   {
      curMtod.addInsFieldOp (0xb4, c.getName (), field, type);
      return this;
   }

   public DynClass getStatic (String className, String field, String type)
   {
      curMtod.addInsFieldOp (0xb2, className, field, type);
      return this;
   }

   public DynClass getStatic (Class<?> c, String field, String type)
   {
      curMtod.addInsFieldOp (0xb2, c.getName (), field, type);
      return this;
   }

   public DynClass goto_ (int label)
   {
      curMtod.addJmpIns (0xa7, label);
      return this;
   }

   public DynClass goto_ (String label)
   {
      return goto_ (toLabel (label));
   }
   
   public DynClass i2b ()
   {
      curMtod.addIns (0x91);
      return this;
   }

   public DynClass i2c ()
   {
      curMtod.addIns (0x92);
      return this;
   }

   public DynClass i2d ()
   {
      curMtod.addIns (0x87);
      return this;
   }

   public DynClass i2f ()
   {
      curMtod.addIns (0x86);
      return this;
   }

   public DynClass i2l ()
   {
      curMtod.addIns (0x85);
      return this;
   }

   public DynClass i2s ()
   {
      curMtod.addIns (0x93);
      return this;
   }

   public DynClass iadd ()
   {
      curMtod.addIns (0x60);
      return this;
   }

   public DynClass iaload ()
   {
      curMtod.addIns (0x2e);
      return this;
   }

   public DynClass iand ()
   {
      curMtod.addIns (0x7e);
      return this;
   }

   public DynClass iastore ()
   {
      curMtod.addIns (0x4f);
      return this;
   }

   public DynClass iconstM1 ()
   {
      curMtod.addIns (0x02);
      return this;
   }

   public DynClass iconst0 ()
   {
      curMtod.addIns (0x03);
      return this;
   }

   public DynClass iconst1 ()
   {
      curMtod.addIns (0x04);
      return this;
   }

   public DynClass iconst2 ()
   {
      curMtod.addIns (0x05);
      return this;
   }

   public DynClass iconst3 ()
   {
      curMtod.addIns (0x06);
      return this;
   }

   public DynClass iconst4 ()
   {
      curMtod.addIns (0x07);
      return this;
   }

   public DynClass iconst5 ()
   {
      curMtod.addIns (0x08);
      return this;
   }

   public DynClass idiv ()
   {
      curMtod.addIns (0x6c);
      return this;
   }

   public DynClass ifAcmpEq (int label)
   {
      curMtod.addJmpIns (0xa5, label);
      return this;
   }

   public DynClass ifAcmpEq (String label)
   {
      return ifAcmpEq (toLabel (label));
   }

   public DynClass ifAcmpNe (int label)
   {
      curMtod.addJmpIns (0xa6, label);
      return this;
   }

   public DynClass ifAcmpNe (String label)
   {
      return ifAcmpNe (toLabel (label));
   }

   public DynClass ifIcmpEq (int label)
   {
      curMtod.addJmpIns (0x9f, label);
      return this;
   }

   public DynClass ifIcmpEq (String label)
   {
      return ifIcmpEq (toLabel (label));
   }

   public DynClass ifIcmpNe (int label)
   {
      curMtod.addJmpIns (0xa0, label);
      return this;
   }

   public DynClass ifIcmpNe (String label)
   {
      return ifIcmpNe (toLabel (label));
   }

   public DynClass ifIcmpLt (int label)
   {
      curMtod.addJmpIns (0xa1, label);
      return this;
   }

   public DynClass ifIcmpLt (String label)
   {
      return ifIcmpLt (toLabel (label));
   }

   public DynClass ifIcmpGe (int label)
   {
      curMtod.addJmpIns (0xa2, label);
      return this;
   }

   public DynClass ifIcmpGe (String label)
   {
      return ifIcmpGe (toLabel (label));
   }

   public DynClass ifIcmpGt (int label)
   {
      curMtod.addJmpIns (0xa3, label);
      return this;
   }

   public DynClass ifIcmpGt (String label)
   {
      return ifIcmpGt (toLabel (label));
   }

   public DynClass ifIcmpLe (int label)
   {
      curMtod.addJmpIns (0xa4, label);
      return this;
   }

   public DynClass ifIcmpLe (String label)
   {
      return ifIcmpLe (toLabel (label));
   }

   public DynClass ifEq (int label)
   {
      curMtod.addJmpIns (0x99, label);
      return this;
   }

   public DynClass ifEq (String label)
   {
      return ifEq (toLabel (label));
   }

   public DynClass ifNe (int label)
   {
      curMtod.addJmpIns (0x9a, label);
      return this;
   }

   public DynClass ifNe (String label)
   {
      return ifNe (toLabel (label));
   }

   public DynClass ifLt (int label)
   {
      curMtod.addJmpIns (0x9b, label);
      return this;
   }

   public DynClass ifLt (String label)
   {
      return ifLt (toLabel (label));
   }

   public DynClass ifGe (int label)
   {
      curMtod.addJmpIns (0x9c, label);
      return this;
   }

   public DynClass ifGe (String label)
   {
      return ifGe (toLabel (label));
   }

   public DynClass ifGt (int label)
   {
      curMtod.addJmpIns (0x9d, label);
      return this;
   }

   public DynClass ifGt (String label)
   {
      return ifGt (toLabel (label));
   }

   public DynClass ifLe (int label)
   {
      curMtod.addJmpIns (0x9e, label);
      return this;
   }

   public DynClass ifLe (String label)
   {
      return ifLe (toLabel (label));
   }

   public DynClass ifNonNull (int label)
   {
      curMtod.addJmpIns (0xc7, label);
      return this;
   }

   public DynClass ifNonNull (String label)
   {
      return ifNonNull (toLabel (label));
   }

   public DynClass ifNull (int label)
   {
      curMtod.addJmpIns (0xc6, label);
      return this;
   }

   public DynClass ifNull (String label)
   {
      return ifNull (toLabel (label));
   }

   public DynClass iinc (int index, int amt)
   {
      if (index > 0xff || amt > 0xff)
         curMtod.addIns (0xc4, 0x84, index, amt);
      else
         curMtod.addIns (0x84, (byte)index, (byte)amt);
      curMtod.setMaxLocal (index);
      return this;
   }

   public DynClass iload (int index)
   {
      if (index >= 0 && index < 4)
      {
         curMtod.addIns (0x1a + index);
         curMtod.setMaxLocal (index);
         return this;
      }
      else
         return addPossiblyWideIns (0x15, index);
   }

   public DynClass iload0 ()
   {
      curMtod.addIns (0x1a);
      curMtod.setMaxLocal (0);
      return this;
   }

   public DynClass iload1 ()
   {
      curMtod.addIns (0x1b);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass iload2 ()
   {
      curMtod.addIns (0x1c);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass iload3 ()
   {
      curMtod.addIns (0x1d);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass imul ()
   {
      curMtod.addIns (0x68);
      return this;
   }

   public DynClass ineg ()
   {
      curMtod.addIns (0x74);
      return this;
   }

   public DynClass instanceOf (String name)
   {
      curMtod.addInsClassOp (0xc1, name);
      return this;
   }
   
   public DynClass invokeInterface (String className, String method,
                                    String type)
   {
      curMtod.addIns (new IfaceIns (0xb9, className, method, type));
      return this;
   }

   public DynClass invokeInterface (Class<?> c, String method, String type)
   {
      curMtod.addIns (new IfaceIns (0xb9, c.getName (), method, type));
      return this;
   }

   public DynClass invokeDynamic ()
   {
      // FIXME: implement at some point for completeness
      throw new RuntimeException ("DynClass: invokeDynamic not " +
                                  "implemented yet");
   }

   public DynClass invokeSpecial (String className, String method,
                                  String type)
   {
      curMtod.addInsMethodOp (0xb7, className, method, type);
      return this;
   }

   public DynClass invokeSpecial (Class<?> c, String method,
                                  String type)
   {
      curMtod.addInsMethodOp (0xb7, c.getName (), method, type);
      return this;
   }

   public DynClass invokeStatic (String className, String method,
                                 String type)
   {
      curMtod.addInsMethodOp (0xb8, className, method, type);
      return this;
   }

   public DynClass invokeStatic (Class<?> c, String method, String type)
   {
      curMtod.addInsMethodOp (0xb8, c.getName (), method, type);
      return this;
   }

   public DynClass invokeVirtual (String className, String method,
                                  String type)
   {
      curMtod.addInsMethodOp (0xb6, className, method, type);
      return this;
   }

   public DynClass invokeVirtual (Class<?> c, String method, String type)
   {
      curMtod.addInsMethodOp (0xb6, c.getName (), method, type);
      return this;
   }

   public DynClass invoke (java.lang.reflect.Method m)
   {
      Class<?> c = m.getDeclaringClass ();
      if (Modifier.isStatic (m.getModifiers ()))
         return invokeStatic (c, m.getName (),  getDescriptor (m));
      else if (c.isInterface ())
         return invokeInterface (c, m.getName (), getDescriptor (m));
      else
         return invokeVirtual (c, m.getName (), getDescriptor (m));
   }

   public DynClass ior ()
   {
      curMtod.addIns (0x80);
      return this;
   }

   public DynClass irem ()
   {
      curMtod.addIns (0x70);
      return this;
   }

   public DynClass ireturn ()
   {
      curMtod.addIns (0xac);
      return this;
   }

   public DynClass ishl ()
   {
      curMtod.addIns (0x78);
      return this;
   }

   public DynClass ishr ()
   {
      curMtod.addIns (0x7a);
      return this;
   }

   public DynClass istore (int index)
   {
      return addPossiblyWideIns (0x36, index);
   }

   public DynClass istore0 ()
   {
      curMtod.addIns (0x3b);
      curMtod.setMaxLocal (0);
      return this;
   }

   public DynClass istore1 ()
   {
      curMtod.addIns (0x3c);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass istore2 ()
   {
      curMtod.addIns (0x3d);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass istore3 ()
   {
      curMtod.addIns (0x3e);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass isub ()
   {
      curMtod.addIns (0x64);
      return this;
   }

   public DynClass iushr ()
   {
      curMtod.addIns (0x7c);
      return this;
   }

   public DynClass ixor ()
   {
      curMtod.addIns (0x82);
      return this;
   }

   public DynClass l2d ()
   {
      curMtod.addIns (0x8a);
      return this;
   }

   public DynClass l2f ()
   {
      curMtod.addIns (0x89);
      return this;
   }

   public DynClass l2i ()
   {
      curMtod.addIns (0x88);
      return this;
   }

   public DynClass ladd ()
   {
      curMtod.addIns (0x61);
      return this;
   }

   public DynClass laload ()
   {
      curMtod.addIns (0x2f);
      return this;
   }

   public DynClass land ()
   {
      curMtod.addIns (0x7f);
      return this;
   }

   public DynClass lastore ()
   {
      curMtod.addIns (0x50);
      return this;
   }

   public DynClass lcmp ()
   {
      curMtod.addIns (0x94);
      return this;
   }

   public DynClass lconst0 ()
   {
      curMtod.addIns (0x09);
      return this;
   }

   public DynClass lconst1 ()
   {
      curMtod.addIns (0x0a);
      return this;
   }
   
   public DynClass ldc (int val)
   {
      curMtod.addIns (new LdcIntIns (val));
      return this;
   }

   public DynClass ldc (float val)
   {
      curMtod.addIns (new LdcFloatIns (val));
      return this;
   }

   public DynClass ldc (long val)
   {
      curMtod.addIns (new LdcLongIns (val));
      return this;
   }

   public DynClass ldc (double val)
   {
      curMtod.addIns (new LdcDoubleIns (val));
      return this;
   }

   public DynClass ldc (String name)
   {
      curMtod.addIns (new LdcStrIns (name));
      return this;
   }

   public DynClass ldcClass (String name)
   {
      curMtod.addIns (new LdcClassIns (name));
      return this;
   }

   public DynClass ldcClass (Class<?> c)
   {
      curMtod.addIns (new LdcClassIns (c.getName ()));
      return this;
   }

   public DynClass ldc (int pos, int val)
   {
      curMtod.setIns (pos, new LdcIntIns (val));
      return this;
   }

   public DynClass ldc (int pos, float val)
   {
      curMtod.setIns (pos, new LdcFloatIns (val));
      return this;
   }

   public DynClass ldc (int pos, long val)
   {
      curMtod.setIns (pos, new LdcLongIns (val));
      return this;
   }

   public DynClass ldc (int pos, double val)
   {
      curMtod.setIns (pos, new LdcDoubleIns (val));
      return this;
   }

   public DynClass ldc (int pos, String name)
   {
      curMtod.setIns (pos, new LdcStrIns (name));
      return this;
   }

   public DynClass ldcClass (int pos, String name)
   {
      curMtod.setIns (pos, new LdcClassIns (name));
      return this;
   }

   public DynClass ldcClass (int pos, Class<?> c)
   {
      curMtod.setIns (pos, new LdcClassIns (c.getName ()));
      return this;
   }

   public int reserveIns ()
   {
      int pos = curMtod.instructions.size ();
      curMtod.addIns (null);
      return pos;
   }
   
   public DynClass ldiv ()
   {
      curMtod.addIns (0x6d);
      return this;
   }

   public DynClass lload (int index)
   {
      if (index >= 0 && index < 4)
      {
         curMtod.addIns (0x1e + index);
         curMtod.setMaxLocal (index + 1);
         return this;
      }
      else
         return addPossiblyWideIns (0x16, index, 2);
   }

   public DynClass lload0 ()
   {
      curMtod.addIns (0x1e);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass lload1 ()
   {
      curMtod.addIns (0x1f);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass lload2 ()
   {
      curMtod.addIns (0x20);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass lload3 ()
   {
      curMtod.addIns (0x21);
      curMtod.setMaxLocal (4);
      return this;
   }

   public DynClass lmul ()
   {
      curMtod.addIns (0x69);
      return this;
   }

   public DynClass lneg ()
   {
      curMtod.addIns (0x75);
      return this;
   }

   public DynClass lookupswitch ()
   {
      // FIXME: implement at some point for completeness
      throw new RuntimeException ("DynClass: lookupswitch not implemented yet");
   }

   public DynClass lor ()
   {
      curMtod.addIns (0x81);
      return this;
   }

   public DynClass lrem ()
   {
      curMtod.addIns (0x71);
      return this;
   }

   public DynClass lreturn ()
   {
      curMtod.addIns (0xad);
      return this;
   }

   public DynClass lshl ()
   {
      curMtod.addIns (0x79);
      return this;
   }

   public DynClass lshr ()
   {
      curMtod.addIns (0x7b);
      return this;
   }

   public DynClass lstore (int index)
   {
      return addPossiblyWideIns (0x37, index, 2);
   }

   public DynClass lstore0 ()
   {
      curMtod.addIns (0x3f);
      curMtod.setMaxLocal (1);
      return this;
   }

   public DynClass lstore1 ()
   {
      curMtod.addIns (0x40);
      curMtod.setMaxLocal (2);
      return this;
   }

   public DynClass lstore2 ()
   {
      curMtod.addIns (0x41);
      curMtod.setMaxLocal (3);
      return this;
   }

   public DynClass lstore3 ()
   {
      curMtod.addIns (0x42);
      curMtod.setMaxLocal (4);
      return this;
   }

   public DynClass lsub ()
   {
      curMtod.addIns (0x65);
      return this;
   }

   public DynClass lushr ()
   {
      curMtod.addIns (0x7d);
      return this;
   }

   public DynClass lxor ()
   {
      curMtod.addIns (0x83);
      return this;
   }

   public DynClass monitorEnter ()
   {
      curMtod.addIns (0xc2);
      return this;
   }

   public DynClass monitorExit ()
   {
      curMtod.addIns (0xc3);
      return this;
   }

   public DynClass multiAnewArray (String className)
   {
      curMtod.addInsClassOp (0xc5, className);
      return this;
   }

   public DynClass new_ (String className)
   {
      curMtod.addInsClassOp (0xbb, className);
      return this;
   }

   public DynClass new_ (Class<?> c)
   {
      curMtod.addInsClassOp (0xbb, c.getName ());
      return this;
   }

   public DynClass newArray (Type t)
   {
      curMtod.addIns (0xbc, t.getVal ());
      return this;
   }

   public DynClass nop ()
   {
      curMtod.addIns (0x00);
      return this;
   }

   public DynClass pop ()
   {
      curMtod.addIns (0x57);
      return this;
   }

   public DynClass pop2 ()
   {
      curMtod.addIns (0x58);
      return this;
   }

   public DynClass putField (String className, String field, String type)
   {
      curMtod.addInsFieldOp (0xb5, className, field, type);
      return this;
   }

   public DynClass putStatic (String className, String field, String type)
   {
      curMtod.addInsFieldOp (0xb3, className, field, type);
      return this;
   }

   public DynClass return_ ()
   {
      curMtod.addIns (0xb1);
      return this;
   }

   public DynClass saload ()
   {
      curMtod.addIns (0x35);
      return this;
   }

   public DynClass sastore ()
   {
      curMtod.addIns (0x56);
      return this;
   }

   public DynClass sipush (int b)
   {
      curMtod.addIns (0x11, (short)b);
      return this;
   }

   public DynClass swap ()
   {
      curMtod.addIns (0x5f);
      return this;
   }

   public DynClass tableswitch ()
   {
      // FIXME: implement at some point for completeness
      throw new RuntimeException ("DynClass: tableswitch not implemented yet");
   }

   //////////////////////////////////////////////////////////////////////
   
   public DynClass label (int tgt)
   {
      curMtod.addLabel (tgt);
      return this;
   }

   public DynClass label (String sym)
   {
      return label (toLabel (sym));
   }

   public DynClass setMaxStack (int i)
   {
      curMtod.setMaxStack (i);
      return this;
   }

   public static String toInternal (String name)
   {
      return name.replace (".", "/");
   }

   public static String toInternal (Class<?> c)
   {
      return toInternal (c.getName ());
   }

   private final static HashMap<String, String> typeMap =
      new HashMap <String, String> ();
   
   static
   {
      typeMap.put ("byte", "B");
      typeMap.put ("short", "S");
      typeMap.put ("int", "I");
      typeMap.put ("long", "J");
      typeMap.put ("float", "F");
      typeMap.put ("double", "D");
      typeMap.put ("boolean", "Z");
      typeMap.put ("void", "V");
   }

   public static String getDescriptor (java.lang.reflect.Method m)
   {
      StringBuilder descr = new StringBuilder ();
      descr.append ('(');
      for (Class<?> prm : m.getParameterTypes ())
         descr.append (getDescriptor (prm));
      descr.append (')');
      descr.append (getDescriptor (m.getReturnType ()));
      return descr.toString (); 
   }
   
   public static String getDescriptor (Class<?> type)
   {
      String name = type.getName ();
      String descr = typeMap.get (name);
      if (descr != null)
         return descr;
      else if (name.charAt (0) == '[')
         return DynClass.toInternal (name);
      else
         return "L" + DynClass.toInternal (name) + ";";
   }

   public static Class<?> getArrayClass (Class<?> comp)
   {
      try
      {
         return Class.forName ("[" + getDescriptor (comp));
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException (e);
      }
   }
   
   //////////////////////////////////////////////////////////////////////

   private static <T extends Flag> int mergeFlags (T [] flags)
   {
      int merged = 0;
      for (T f : flags)
         merged |= f.getVal ();
      return merged;
   }
   
   private int toLabel (String sym)
   {
      Integer lbl = labelBySym.get (sym);
      if (lbl != null)
         return lbl.intValue ();
      else
         throw new RuntimeException ("DynClass: no such label" + sym);
   }

   private byte [] renderBody () throws IOException
   {
      ByteArrayOutputStream bs = new ByteArrayOutputStream ();
      DataOutputStream os = new DataOutputStream (bs);
      
      os.writeShort ((short)flags);

      // this

      os.writeShort ((short)getConstClass (toInternal (name)));

      // super

      if (superName == null || superName.equals (""))
         superName = "java.lang.Object";
      
      os.writeShort ((short)getConstClass (toInternal (superName)));

      writeInterfaces (os);

      // fields

      os.writeShort ((short)fields.size ());
      for (Field f : fields)
         writeField (f, os);

      // methods

      os.writeShort ((short)methods.size ());
      for (Method m : methods)
         writeMethod (m, os);

      // attributes

      os.writeShort (0); // FIXME: no attributes for now
      
      os.flush ();

      return bs.toByteArray ();
   }

   private int getConstClass (String val) throws IOException
   {
      Integer i = classConstPool.get (val);
      if (i != null)
         return (short)i.intValue ();
      else
      {
         int strRef = getConstUtf8 (val);
         int ref = nextConst ++;
         classConstPool.put (val, ref);
         constPoolOs.write (Const.Class.getVal ());
         constPoolOs.writeShort ((short)strRef);
         return ref;
      }
   }

   private int getConstStr (String val) throws IOException
   {
      Integer i = strConstPool.get (val);
      if (i != null)
         return (short)i.intValue ();
      else
      {
         int strRef = getConstUtf8 (val);
         int ref = nextConst ++;
         strConstPool.put (val, ref);
         constPoolOs.write (Const.String.getVal ());
         constPoolOs.writeShort ((short)strRef);
         return ref;
      }
   }

   private int getConstInt (int val) throws IOException
   {
      Integer i = intConstPool.get (val);
      if (i != null)
         return i.intValue ();
      else
      {
         int ref = nextConst ++;
         intConstPool.put (val, ref);
         constPoolOs.write (Const.Integer.getVal ());
         constPoolOs.writeInt (val);
         return ref;
      }
   }

   private int getConstUtf8 (String val) throws IOException
   {
      Integer i = utf8ConstPool.get (val);
      if (i != null)
         return i.intValue ();
      else
      {
         int ref = nextConst ++;
         utf8ConstPool.put (val, ref);
         constPoolOs.write (Const.Utf8.getVal ());
         constPoolOs.writeUTF (val);
         return ref;
      }
   }

   private int getConstIfaceMethod (String className, String name, String type)
      throws IOException
   {
      String key = className + ";" + name + ";" + type;
      Integer i = ifaceConstPool.get (key);
      if (i != null)
         return i.intValue ();
      else
      {
         int classRef = getConstClass (className);
         int nameAndTypeRef = getConstNameAndType (name, type);
         int ref = nextConst ++;
         ifaceConstPool.put (key, ref);
         constPoolOs.write (Const.InterfaceMethodRef.getVal ());
         constPoolOs.writeShort ((short)classRef);
         constPoolOs.writeShort ((short)nameAndTypeRef);
         return ref;
      }
   }

   private int getConstMethod (String className, String name, String type)
      throws IOException
   {
      String key = className + ";" + name + ";" + type;
      Integer i = methodConstPool.get (key);
      if (i != null)
         return i.intValue ();
      else
      {
         int classRef = getConstClass (className);
         int nameAndTypeRef = getConstNameAndType (name, type);
         int ref = nextConst ++;
         methodConstPool.put (key, ref);
         constPoolOs.write (Const.MethodRef.getVal ());
         constPoolOs.writeShort ((short)classRef);
         constPoolOs.writeShort ((short)nameAndTypeRef);
         return ref;
      }
   }

   private int getConstField (String className, String name, String type)
      throws IOException
   {
      String key = className + ";" + name + ";" + type;
      Integer i = fieldConstPool.get (key);
      if (i != null)
         return i.intValue ();
      else
      {
         int classRef = getConstClass (className);
         int nameAndTypeRef = getConstNameAndType (name, type);
         int ref = nextConst ++;
         fieldConstPool.put (key, ref);
         constPoolOs.write (Const.FieldRef.getVal ());
         constPoolOs.writeShort ((short)classRef);
         constPoolOs.writeShort ((short)nameAndTypeRef);
         return ref;
      }
   }

   private int getConstNameAndType (String name, String type)
      throws IOException
   {
      String key = name + ";" + type;
      Integer i = nameAndTypeConstPool.get (key);
      if (i != null)
         return i.intValue ();
      else
      {
         int nameRef = getConstUtf8 (name);
         int typeRef = getConstUtf8 (type);
         int ref = nextConst ++;
         nameAndTypeConstPool.put (key, ref);
         constPoolOs.write (Const.NameAndType.getVal ());
         constPoolOs.writeShort ((short)nameRef);
         constPoolOs.writeShort ((short)typeRef);
         return ref;
      }
   }
   
   private void writeInterfaces (DataOutputStream os) throws IOException
   {
      os.writeShort ((short)interfaces.size ());
      for (String i : interfaces)
         os.writeShort (getConstClass (toInternal (i)));
   }

   private void writeField (Field f, DataOutputStream os) throws IOException
   {
      os.writeShort (f.flags);
      os.writeShort (getConstUtf8 (f.name));
      os.writeShort (getConstUtf8 (f.type));
      os.writeShort (0);
   }

   private void writeMethod (Method m, DataOutputStream os) throws IOException
   {
      os.writeShort (m.flags);
      os.writeShort (getConstUtf8 (m.name));
      os.writeShort (getConstUtf8 (m.type));
      os.writeShort (1); // Code
      os.writeShort (getConstUtf8 ("Code")); // Attr name

      ByteArrayOutputStream mbs = new ByteArrayOutputStream ();
      DataOutputStream mos = new DataOutputStream (mbs);

      writeMethodBody (m, mos);
      
      mos.flush ();
      byte [] body = mbs.toByteArray ();
      os.writeInt (body.length); // Attr len
      os.write (body);
   }

   private void writeMethodBody (Method m, DataOutputStream os)
      throws IOException
   {
      os.writeShort (m.maxStack);
      os.writeShort (m.maxLocals);

      ByteArrayOutputStream cbs = new ByteArrayOutputStream ();
      DataOutputStream cos = new DataOutputStream (cbs);

      writeMethodCode (m, cos);
      
      cos.flush ();
      byte [] code = cbs.toByteArray ();
      os.writeInt (code.length); // Code len
      os.write (code); // Code
      os.writeShort (0); // No exceptions
      os.writeShort (0); // No attributes
   }

   private void writeMethodCode (Method m, DataOutputStream os)
      throws IOException
   {
      // Resolve constants

      for (Ins i : m.instructions)
         i.resolveConst (this);

      // Resolve jumps

      HashMap<Integer, Integer> jmpMap = new HashMap<Integer, Integer> ();

      int addr = 0;
      for (Ins i : m.instructions)
      {
         i.resolveLabel (addr, jmpMap);
         addr += i.getSize ();
      }

      addr = 0;
      for (Ins i : m.instructions)
      {
         i.resolveJmp (addr, jmpMap);
         addr += i.getSize ();
      }
      
      // Write result
      
      for (Ins i : m.instructions)
         i.write (os);
   }
   
   private static enum Const
   {
      Class (7),
      FieldRef (9),
      MethodRef (10),
      InterfaceMethodRef (11),
      String (8),
      Integer (3),
      Float (4),
      Long (5),
      Double (6),
      NameAndType (12),
      Utf8 (1),
      MethodHandle (15),
      MethodType (16),
      InvokeDynamic (18);

      Const (int val) { this.val = val; }
      public byte getVal () { return (byte)val; }
      private int val;
   }
   
   private static final int Cookie = 0xcafebabe;
   private static final short DefaultMinorVer = 0;
   // Stay on 1.5 to avoid stack map frames for now
   private static final short DefaultMajorVer = 49;

   private static class Ins
   {
      void resolveConst (DynClass self) throws IOException { }
      void resolveLabel (int addr, HashMap<Integer, Integer> jmpMap) { }
      void resolveJmp (int addr, HashMap<Integer, Integer> jmpMap) { }
      int getSize () { return 0; }
      void write (DataOutputStream os) throws IOException { }
   }

   private static class Ins1 extends Ins
   {
      Ins1 (int opc) { this.opc = (byte)opc; }
      int getSize () { return 1; }
      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
      }
      final byte opc;
   }

   private static class Ins2 extends Ins
   {
      Ins2 (int opc, byte op) { this.opc = (byte)opc; this.op = op; }
      int getSize () { return 2; }
      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.write (op);
      }
      final byte opc;
      final byte op;
   }
   
   private static class Ins3 extends Ins
   {
      Ins3 (int opc, byte op1, byte op2)
      {
         this.opc = (byte)opc;
         this.op1 = op1;
         this.op2 = op2;
      }

      int getSize () { return 3; }
      
      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.write (op1);
         os.write (op2);
      }
      
      final byte opc;
      final byte op1;
      final byte op2;
   }
   
   private static class Ins4 extends Ins
   {
      Ins4 (int opc, byte op1, byte op2, byte op3)
      {
         this.opc = (byte)opc;
         this.op1 = op1;
         this.op2 = op2;
         this.op3 = op3;
      }

      int getSize () { return 4; }
      
      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.write (op1);
         os.write (op2);
         os.write (op3);
      }

      final byte opc;
      final byte op1;
      final byte op2;
      final byte op3;
   }

   private static class Ins6 extends Ins
   {
      Ins6 (int opc, byte op1, byte op2, byte op3, byte op4, byte op5)
      {
         this.opc = (byte)opc;
         this.op1 = op1;
         this.op2 = op2;
         this.op3 = op3;
         this.op4 = op4;
         this.op5 = op5;
      }

      int getSize () { return 6; }
      
      final byte opc;
      final byte op1;
      final byte op2;
      final byte op3;
      final byte op4;
      final byte op5;

      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.write (op1);
         os.write (op2);
         os.write (op3);
         os.write (op4);
         os.write (op5);
      }
   }
   
   private static class IfaceIns extends Ins
   {
      IfaceIns (int opc, String className, String mtod, String type)
      {
         this.opc = (byte)opc;
         this.className = toInternal (className);
         this.mtod = mtod;
         this.type = type;
      }

      int getSize () { return 5; }

      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.writeShort ((short)ref);
         os.write ((byte)countArgs (type));
         os.write (0);
      }

      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstIfaceMethod (className, mtod, type);
      }

      final byte opc;
      final String className;
      final String mtod;
      final String type;
      int ref;
   }
   
   private static class ClassIns extends Ins
   {
      ClassIns (int opc, String className)
      {
         this.opc = (byte)opc;
         this.className = toInternal (className);
      }

      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstClass (className);
      }

      int getSize () { return 3; }

      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.writeShort ((short)ref);
      }

      final byte opc;
      final String className;
      int ref;
   }
   
   private static class FieldIns extends Ins
   {
      FieldIns (int opc, String className, String field, String type)
      {
         this.opc = (byte)opc;
         this.className = toInternal (className);
         this.field = field;
         this.type = type;
      }

      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstField (className, field, type);
      }

      int getSize () { return 3; }

      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.writeShort ((short)ref);
      }

      final byte opc;
      final String className;
      final String field;
      final String type;
      int ref;
   }

   private static class MethodIns extends Ins
   {
      MethodIns (int opc, String className, String mtod, String type)
      {
         this.opc = (byte)opc;
         this.className = toInternal (className);
         this.mtod = mtod;
         this.type = type;
      }

      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstMethod (className, mtod, type);
      }

      int getSize () { return 3; }

      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.writeShort ((short)ref);
      }
      
      final byte opc;
      final String className;
      final String mtod;
      final String type;
      int ref;
   }

   private static class LdcBase extends Ins
   {
      int getSize ()
      {
         if (ref <= 255)
            return 2;
         else
            return 3;
      }

      void write (DataOutputStream os) throws IOException
      {
         if (getSize () == 2)
         {
            os.write (0x12); // ldc
            os.write ((byte)ref);
         }
         else
         {
            os.write (0x13); // ldc_w
            os.writeShort ((short)ref);
         }
      }
      
      int ref;
   }

   private static class Ldc2Base extends Ins
   {
      int getSize () { return 3; }

      void write (DataOutputStream os) throws IOException
      {
         os.write (0x14); // ldc2_w
         os.writeShort ((short)ref);
      }
      
      int ref;
   }
   
   private static class LdcIntIns extends LdcBase
   {
      LdcIntIns (int val) { this.val = val; }
      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstInt (val);
      }
      final int val;
   }
   
   private static class LdcFloatIns extends  LdcBase
   {
      LdcFloatIns (float val) { this.val = val; }
      // FIXME: resolveConst
      final float val;
   }
   
   private static class LdcLongIns extends Ldc2Base
   {
      LdcLongIns (long val) { this.val = val; }
      // FIXME: resolveConst
      final long val;
   }

   private static class LdcDoubleIns extends Ldc2Base
   {
      LdcDoubleIns (double val) { this.val = val; }
      // FIXME: resolveConst
      final double val;
   }

   private static class LdcClassIns extends  LdcBase
   {
      LdcClassIns (String name) { this.name = toInternal (name); }
      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstClass (name);
      }
      final String name;
   }
   
   private static class LdcStrIns extends  LdcBase
   {
      LdcStrIns (String val) { this.val = val; }

      void resolveConst (DynClass self) throws IOException
      {
         ref = self.getConstStr (val);
      }

      final String val;
   }

   private static class LabelIns extends Ins
   {
      LabelIns (int label) { this.label = label; }
      void resolveLabel (int addr, HashMap<Integer, Integer> jmpMap)
      {
         jmpMap.put (label, addr);
      }
      final int label;
   }

   private static class JmpIns extends Ins
   {
      JmpIns (int opc, int label)
      {
         this.opc = (byte)opc;
         this.label = label;
      }

      void resolveJmp (int addr, HashMap<Integer, Integer> jmpMap)
      {
         Integer a = jmpMap.get (label);
         if (a == null)
            throw new RuntimeException ("DynClass: Dangling label: " + label);
         this.addr = a.intValue ();
         this.addr -= addr; // Relative
         if (this.addr > 0xFFFF)
            throw new RuntimeException ("DynClass: Far jumps not " +
                                        "implemented yet");
      }

      int getSize () { return 3; }

      void write (DataOutputStream os) throws IOException
      {
         os.write (opc);
         os.writeShort ((short)addr);
      }

      final byte opc;
      final int label;
      int addr;
   }

   private static boolean isStatic (int flags)
   {
      return (flags & MtodFlag.Static.getVal ()) != 0;
   }

   private static int countArgs (String type)
   {
      int count = 0;
     Loop:
      for (int i = 0, len = type.length (); i < len;)
         switch (type.charAt (i))
         {
         case '(':
            ++ i;
            break;
            
         case 'B': case 'C': case 'D': case 'F': case 'I': case 'J': case 'S':
         case 'Z':
            ++ count;
            ++ i;
            break;
            
         case 'L':
            ++ count;
            i = type.indexOf (';', i);
            if (i != -1)
               ++ i;
            else
               break Loop;
            break;
            
         case '[':
            ++ count;
            i = skipFieldType (type, i + 1);
            break;
            
         case ')':
            return count + 1 /* this */;
            
         default:
            break Loop;
         }
      
      throw new RuntimeException ("DynClass: Bad method specifier syntax: " +
                                  type);
   }

   private static int skipFieldType (String type, int i)
   {
      switch (type.charAt (i))
      {
       case 'B': case 'C': case 'D': case 'F': case 'I': case 'J': case 'S':
       case 'Z':
          return i + 1;

       case 'L':
          i = type.indexOf (';', i);
          if (i != -1)
             return i + 1;
          else
             break;
          
      case '[':
         return skipFieldType (type, i + 1);
         
      default:
         break;
      }

      throw new RuntimeException ("DynClass: Bad method specifier syntax: " +
                                  type);
   }
   
   private static class Method
   {
      Method (String name, String type, int flags)
      {
         this.name = name;
         this.type = type;
         this.flags = (short)flags;
         
         maxLocals = countArgs (type) + (isStatic (flags) ? 0 : 1);
      }

      void addIns (int opc)
      {
         addIns (new Ins1 (opc));
      }

      void addIns (int opc, byte op)
      {
         addIns (new Ins2 (opc, op));
      }

      void addIns (int opc, short op)
      {
         addIns (new Ins3 (opc, (byte)(op >> 8), (byte)(op & 0xff)));
      }

      void addIns (int opc, byte op1, byte op2)
      {
         addIns (new Ins3 (opc, op1, op2));
      }

      void addIns (int opc1, int opc2, int op)
      {
         addIns (new Ins4 (opc1, (byte)opc2, (byte)(op >> 8),
                           (byte)(op & 0xff)));
      }

      void addIns (int opc1, int opc2, int op1, int op2)
      {
         addIns (new Ins6 (opc1, (byte)opc2, (byte)(op1 >> 8),
                           (byte)(op1 & 0xff),
                           (byte)(op2 >> 8), (byte)(op2 & 0xff)));
      }

      void addInsClassOp (int opc, String name)
      {
         addIns (new ClassIns (opc, name));
      }
      
      void addInsFieldOp (int opc, String className, String field, String type)
      {
         addIns (new FieldIns (opc, className, field, type));
      }
      
      void addInsMethodOp (int opc, String className, String mtod, String type)
      {
         addIns (new MethodIns (opc, className, mtod, type));
      }

      void addJmpIns (int opc, int label)
      {
         addIns (new JmpIns (opc, label));
      }

      void addIns (Ins ins)
      {
         instructions.add (ins);
      }
      
      void addLabel (int label)
      {
         addIns (new LabelIns (label));
      }

      void setMaxLocal (int index)
      {
         maxLocals = Math.max (maxLocals, index + 1);
      }

      void setMaxStack (int depth)
      {
         maxStack = Math.max (maxStack, depth);
      }

      void setIns (int pos, Ins ins)
      {
         instructions.set (pos, ins);
      }

      final String name;
      final String type;
      final short flags;
      final ArrayList<Ins> instructions = new ArrayList<Ins> ();
      int maxLocals = 0;
      int maxStack = 0;
   }

   private static class Field
   {
      Field (String name, String type, int flags)
      {
         this.name = name;
         this.type = type;
         this.flags = (short)flags;
      }

      final String name;
      final String type;
      final short flags;
   }
   
   private DynClass addPossiblyWideIns (int opc, int index, int localSize)
   {
      if (index > 0xff)
         curMtod.addIns (0xc4, opc, index);
      else
         curMtod.addIns (opc, (byte)index);
      curMtod.setMaxLocal (index + localSize - 1);
      return this;
   }

   private DynClass addPossiblyWideIns (int opc, int index)
   {
      return addPossiblyWideIns (opc, index, 1);
   }
   
   private final String name;
   private final int majorVer;
   private final int minorVer;
   private final ArrayList<String> interfaces = new ArrayList<String> ();
   private String superClass;
   private final ArrayList<Field> fields = new ArrayList<Field> ();
   private final ArrayList<Method> methods = new ArrayList<Method> ();
   private Method curMtod;
   private int flags = ClassFlag.Public.getVal () | ClassFlag.Super.getVal ();
   private String superName;
   private int nextConst = 1;
   private final HashMap<String, Integer> utf8ConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<Integer, Integer> intConstPool =
      new HashMap<Integer, Integer> ();
   private final HashMap<String, Integer> classConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<String, Integer> strConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<String, Integer> ifaceConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<String, Integer> nameAndTypeConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<String, Integer> methodConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<String, Integer> fieldConstPool =
      new HashMap<String, Integer> ();
   private final HashMap<String, Integer> labelBySym =
      new HashMap<String, Integer> ();
   private ByteArrayOutputStream constPoolBs;
   private DataOutputStream constPoolOs;
   private int nextLabel = 0;
}
