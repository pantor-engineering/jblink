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

import com.pantor.blink.msg.blink.GroupDef;
import com.pantor.blink.msg.blink.Define;
import com.pantor.blink.msg.blink.FieldDef;
import com.pantor.blink.msg.blink.TypeDef;
import com.pantor.blink.msg.blink.Sequence;
import com.pantor.blink.msg.blink.Ref;
import com.pantor.blink.msg.blink.DynRef;
import com.pantor.blink.msg.blink.Bool;
import com.pantor.blink.msg.blink.I8;
import com.pantor.blink.msg.blink.U8;
import com.pantor.blink.msg.blink.I16;
import com.pantor.blink.msg.blink.U16;
import com.pantor.blink.msg.blink.I32;
import com.pantor.blink.msg.blink.U32;
import com.pantor.blink.msg.blink.I64;
import com.pantor.blink.msg.blink.U64;
import com.pantor.blink.msg.blink.F64;
import com.pantor.blink.msg.blink.Decimal;
import com.pantor.blink.msg.blink.FixedDec;
import com.pantor.blink.msg.blink.Binary;
import com.pantor.blink.msg.blink.Fixed;
import com.pantor.blink.msg.blink.Symbol;
import com.pantor.blink.msg.blink.NanoTime;
import com.pantor.blink.msg.blink.MilliTime;
import com.pantor.blink.msg.blink.TimeOfDayMilli;
import com.pantor.blink.msg.blink.TimeOfDayNano;
import com.pantor.blink.msg.blink.SchemaAnnotation;
import com.pantor.blink.msg.blink.Enum;

import java.io.IOException;

public final class SchemaExchangeDecoder
{
   public SchemaExchangeDecoder (ObjectModel om)
      throws BlinkException
   {
      this.om = om;

      try
      {
         om.loadBuiltinSchemas ();
      }
      catch (IOException e)
      {
         throw new BlinkException (e);
      }
      
      this.schema = om.getSchema ();
      this.oreg = new DefaultObsRegistry (om);
      this.disp = new Dispatcher (om, oreg);
      this.oreg.addObserver (this);
   }
   
   public void decode (Object def)
      throws BlinkException
   {
      if (! disp.dispatch (def))
         throw new BlinkException ("Unsupported schema exchange type: " +
                                   def.getClass ());
   }
   
   public static boolean isSchemaExchangeTypeId (long tid)
   {
      return 
         tid == 0x092727dd2168be26L     // Blink:GroupDecl
         || tid == 0xaa83769fdd82c0d5L  // Blink:GroupDef
         || tid == 0x9605ca62c4b7c3c6L  // Blink:Define
         || tid == 0xfbdf6cfddba7c1daL; // Blink:SchemaAnnotation
   }

   public void onGroupDef (GroupDef def)
   {
      try
      {
         Schema.Group g = schema.replaceGroup (makeName (def.getName ()));
         setAnnotations (def, g);
         if (def.hasId ())
            g.setId (def.getId ());

         if (def.hasSuper ())
            g.setSuper (makeName (def.getSuper ()));

         for (FieldDef extf : def.getFields ())
            addField (extf, g);
      }
      catch (BlinkException e)
      {
         throw new RuntimeException (e);
      }
   }

   private void addField (FieldDef extf, Schema.Group g)
      throws BlinkException
   {
      Schema.Presence pres =
         extf.getOptional () ?
         Schema.Presence.Optional : Schema.Presence.Required;
      Schema.Field intf =
         g.addField (extf.getName (), makeType (extf.getType ()), pres);
      setAnnotations (extf, intf);
      if (extf.hasId ())
         intf.setId (String.valueOf (extf.getId ()));
   }

   public void onDefine (Define def)
   {
      try
      {
         Schema.Define d =
            schema.replaceDefine (makeName (def.getName ()),
                                  makeType (def.getType ()));
         setAnnotations (def, d);
         if (def.hasId ())
            d.setId (String.valueOf (def.getId ()));
      }
      catch (BlinkException e)
      {
         throw new RuntimeException (e);
      }
   }

   public void onSchemaAnnotation (SchemaAnnotation sa)
   {
      AnnotSet annots = new AnnotSet ();
      for (com.pantor.blink.msg.blink.Annotation a : sa.getAnnotations ())
         annots.set (makeName (a.getName ()), a.getValue ());
      String ns = sa.getNs ();
      if (ns != null)
         schema.addAnnotations (annots, ns);
      else
         schema.addAnnotations (annots);
   }
   
   private Schema.Type makeType (TypeDef t)
      throws BlinkException
   {
      if (t instanceof Sequence)
      {
         disp.dispatch (((Sequence)t).getType ());
         assert curType != null;
         curType.setRank (Schema.Rank.Sequence);
      }
      else
         disp.dispatch (t);

      assert curType != null;
      
      setAnnotations (t, curType);
      return curType;
   }
   
   public void onRef (Ref ref)
   {
      curType = new Schema.Ref (makeName (ref.getType ()), Schema.Layout.Static);
   }

   public void onDynRef (DynRef ref)
   {
      curType =
         new Schema.Ref (makeName (ref.getType ()), Schema.Layout.Dynamic);
   }

   public void onString (com.pantor.blink.msg.blink.String str)
   {
      Schema.StrType s = new Schema.StrType ();
      curType = s;
      if (str.hasMaxSize ())
         s.setMaxSize (str.getMaxSize ());
   }

   public void onBinary (Binary str)
   {
      Schema.StrType s = new Schema.StrType (Schema.TypeCode.Binary);
      curType = s;
      if (str.hasMaxSize ())
         s.setMaxSize (str.getMaxSize ());
   }

   public void onFixed (Fixed f)
   {
      curType = new Schema.FixedType (f.getSize ());
   }

   public void onFixedDec (FixedDec fd)
   {
      curType = new Schema.FixedDecType (fd.getScale ());
   }

   public void onEnum (Enum from)
   {
      try
      {
         Schema.Enum to = new Schema.Enum ();
         curType = to;
         for (Symbol sym : from.getSymbols ())
         {
            Schema.Symbol s = to.addSymbol (sym.getName (), sym.getValue ());
            setAnnotations (sym, s);
         }
      }
      catch (BlinkException e)
      {
         throw new RuntimeException (e);
      }
   }

   public void onU8 (U8 t) { curType = new Schema.Type (Schema.TypeCode.U8); }
   public void onI8 (I8 t) { curType = new Schema.Type (Schema.TypeCode.I8); }
   public void onU16 (U16 t) { curType = new Schema.Type (Schema.TypeCode.U16); }
   public void onI16 (I16 t) { curType = new Schema.Type (Schema.TypeCode.I16); }
   public void onU32 (U32 t) { curType = new Schema.Type (Schema.TypeCode.U32); }
   public void onI32 (I32 t) { curType = new Schema.Type (Schema.TypeCode.I32); }
   public void onU64 (U64 t) { curType = new Schema.Type (Schema.TypeCode.U64); }
   public void onI64 (I64 t) { curType = new Schema.Type (Schema.TypeCode.I64); }
   public void onF64 (I64 t) { curType = new Schema.Type (Schema.TypeCode.F64); }

   public void onBool (Bool t)
   {
      curType = new Schema.Type (Schema.TypeCode.Bool);
   }

   public void onDecimal (Decimal t)
   {
      curType = new Schema.Type (Schema.TypeCode.Decimal);
   }

   public void onNanoTime (NanoTime t)
   {
      curType = new Schema.Type (Schema.TypeCode.Nanotime);
   }

   public void onMilliTime (MilliTime t)
   {
      curType = new Schema.Type (Schema.TypeCode.Millitime);
   }

   public void onDate (com.pantor.blink.msg.blink.Date t)
   {
      curType = new Schema.Type (Schema.TypeCode.Millitime);
   }

   public void onTimeOfDayMilli (TimeOfDayMilli t)
   {
      curType = new Schema.Type (Schema.TypeCode.TimeOfDayMilli);
   }

   public void onTimeOfDayNano (TimeOfDayNano t)
   {
      curType = new Schema.Type (Schema.TypeCode.TimeOfDayNano);
   }

   public void onDate (com.pantor.blink.msg.blink.Object t)
   {
      curType = new Schema.Type (Schema.TypeCode.Object);
   }

   public void onAny (Object any)
   {
      throw new RuntimeException ("Not a schema exchange type: " +
                                  any.getClass ());
   }

   private static NsName makeName (com.pantor.blink.msg.blink.NsName nm)
   {
      if (nm.getNs () != null)
         return NsName.get (nm.getNs (), nm.getName ());
      else
         return NsName.get (nm.getName ());
   }

   private static void
   setAnnotations (com.pantor.blink.msg.blink.Annotated from, Annotated to)
   {
      com.pantor.blink.msg.blink.Annotation [] fromAnnots =
         from.getAnnotations ();
      if (fromAnnots != null)
         for (com.pantor.blink.msg.blink.Annotation a : fromAnnots)
            to.setAnnot (makeName (a.getName ()), a.getValue ());
   }

   private final ObjectModel om;
   private final Schema schema;
   private final DefaultObsRegistry oreg;
   private final Dispatcher disp;
   private Schema.Type curType;
}
