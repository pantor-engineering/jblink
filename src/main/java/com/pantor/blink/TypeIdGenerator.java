// Copyright (c) 2015, Pantor Engineering AB
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

public final class TypeIdGenerator
{
   private TypeIdGenerator (Schema schema)
   {
      this.schema = schema;
      this.sig = new StringBuilder ();
   }

   private static long hashTid (String s)
   {
      try
      {
         MessageDigest md = MessageDigest.getInstance ("SHA-1");
         md.update (s.getBytes ("UTF-8"));
         byte [] bits = Arrays.copyOfRange (md.digest (), 0, 8);
         return (new BigInteger (bits)).longValue ();
      }
      catch (Exception e)
      {
         return 0;
      }
   }
   
   public static long getTypeId (Schema.Group g, Schema s)
      throws BlinkException
   {
      TypeIdGenerator gen = new TypeIdGenerator (s);
      return hashTid (gen.getSignature (g));
   }

   public static long getTypeId (Schema.Define d, Schema s)
      throws BlinkException
   {
      TypeIdGenerator gen = new TypeIdGenerator (s);
      return hashTid (gen.getSignature (d));
   }

   public static String getSignature (Schema.Group g, Schema s)
      throws BlinkException
   {
      TypeIdGenerator gen = new TypeIdGenerator (s);
      return gen.getSignature (g);
   }

   public static String getSignature (Schema.Define d, Schema s)
      throws BlinkException
   {
      TypeIdGenerator gen = new TypeIdGenerator (s);
      return gen.getSignature (d);
   }

   private void addField (Schema.Field f) throws BlinkException
   {
      Schema.Type t = f.getType ();
      addType (t);
      sig.append (f.getName ());
      sig.append (f.isOptional () ? '?' : '!');
   }

   private String getSignature (Schema.Group g) throws BlinkException
   {
      sig.append (g.getName ());
      sig.append ('>');
      if (g.hasSuper ())
         addRefSig (g.getSuper (), g.getDefaultNs (), g);
      sig.append ('>');
      for (Schema.Field f : g)
         addField (f);
      return sig.toString ();
   }

   private String getSignature (Schema.Define d) throws BlinkException
   {
      sig.append (d.getName ());
      sig.append ('=');
      addType (d.getType ());
      return sig.toString ();
   }
   
   private static final char [] TypeToken = {
      'c' /* I8 */, 'C' /* U8 */, 's' /* I16 */, 'S' /* U16 */, 'i' /* I32 */,
      'I' /* U32 */, 'l' /* I64 */, 'L' /* U64 */, 'f' /* F64 */, 'd' /* Dec */,
      'F' /* FixedDec */, 'D' /* Date */, 'm' /* TimeOfDayMilli */,
      'n' /* TimeOfDayNano */, 'N' /* Nanotime */, 'M' /* Millitime */,
      'B' /* Bool */, 'U' /* String */, 'V' /* Binary */, 'X' /* Fixed */,
      'O' /* Object */, 'R' /* Ref */, 'E' /* Enum */
   };

   private static char getTypeToken (Schema.TypeCode t)
   {
      int pos = t.ordinal ();
      assert pos >= 0 && pos < TypeToken.length;
      return TypeToken [pos];
   }

   private void addRefSig (NsName name, String defaultNs, Schema.Component comp)
      throws BlinkException
   {
      Schema.DefBase d = (Schema.DefBase)schema.find (name, defaultNs);
      if (d != null)
         sig.append (Util.toU64HexStr (d.getOrCreateTypeId (schema)));
      else
         throw new BlinkException.Schema (
            "Cannot resolve reference " + name +
            " when creating type signature", comp.getLocation ());
   }

   private void addType (Schema.Type t) throws BlinkException
   {
      switch (t.getCode ())
      {
       case Ref:
         {
            Schema.Ref ref = t.toRef ();
            if (ref.isDynamic ())
            {
               sig.append ('Y');
               Schema.DefBase d =
                  (Schema.DefBase)schema.find (ref.getName (),
                                               ref.getDefaultNs ());
               if (d != null)
                  sig.append (d.getName ());
               else
                  sig.append (ref.getName ());
               sig.append (';');
            }
            else
            {
               sig.append ('R');
               addRefSig (ref.getName (), ref.getDefaultNs (), ref);
               sig.append (';');
            }
         }
         break;

       case String: case Binary:
         {
            sig.append (getTypeToken (t.getCode ()));
            Schema.StrType str = (Schema.StrType)t;
            if (str.hasMaxSize ())
               sig.append (str.getMaxSize ());
         }
         break;

       case Fixed:
         {
            sig.append (getTypeToken (t.getCode ()));
            Schema.FixedType fixed = (Schema.FixedType)t;
            sig.append (fixed.getSize ());
         }
         break;

       case FixedDec:
         {
            sig.append (getTypeToken (t.getCode ()));
            Schema.FixedDecType fixedDec = (Schema.FixedDecType)t;
            sig.append (fixedDec.getScale ());
         }
         break;

       default:
         sig.append (getTypeToken (t.getCode ()));
         break;
      }
   
      if (t.isSequence ())
         sig.append ('*');
   }

   private final Schema schema;
   private final StringBuilder sig;
}
