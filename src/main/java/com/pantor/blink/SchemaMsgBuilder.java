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
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public final class SchemaMsgBuilder
{
   public static ArrayList<Object> buildTransitive (NsName name, Schema s,
                                                    HashSet<Long> visited)
      throws BlinkException
   {
      ArrayList<Object> result = new ArrayList<Object> ();
      SchemaMsgBuilder builder = new SchemaMsgBuilder (s);
      builder.buildTransitive (name, visited, result);
      return result;
   }

   public static ArrayList<Object> buildTransitive (NsName name, Schema s)
      throws BlinkException
   {
      SchemaMsgBuilder builder = new SchemaMsgBuilder (s);
      ArrayList<Object> result = new ArrayList<Object> ();
      builder.buildTransitive (name, result);
      return result;
   }

   public SchemaMsgBuilder (Schema schema)
   {
      this.schema = schema;
   }

   public void buildTransitive (NsName name, HashSet<Long> visited,
                                ArrayList<Object> result)
      throws BlinkException
   {
      Collector c = new Collector (schema, visited);
      c.collect (name);
      for (Schema.Define d : c.defs)
         result.add (build (d));
      for (Schema.Group g : c.grps)
         result.add (build (g));
   }

   public void buildTransitive (NsName name, ArrayList<Object> result)
      throws BlinkException
   {
      buildTransitive (name, new HashSet<Long> (), result);
   }
   
   public GroupDef build (Schema.Group g) throws BlinkException
   {
      GroupDef def = new GroupDef ();
      setAnnotations (g, def);
      def.setName (makeName (g.getName ()));

      if (g.hasId ())
         def.setId (g.getId ());
      
      if (g.hasSuper ())
         def.setSuper (makeName (g.getSuperGroup ().getName ()));

      List<Schema.Field> fromFields = g.getFields ();
      FieldDef [] toFields = new FieldDef [fromFields.size ()];
      def.setFields (toFields);
      int pos = 0;
      for (Schema.Field f : fromFields)
         toFields [pos ++] = build (f);

      return def;
   }

   public Define build (Schema.Define d) throws BlinkException
   {
      Define def = new Define ();
      setAnnotations (d, def);
      def.setName (makeName (d.getName ()));

      if (d.getId () != null)
         def.setId (Integer.parseInt (d.getId ()));

      def.setType (build (d.getType ()));
      
      return def;
   }

   private FieldDef build (Schema.Field f) throws BlinkException
   {
      FieldDef def = new FieldDef ();
      setAnnotations (f, def);

      if (f.getId () != null)
         def.setId (Integer.parseInt (f.getId ()));

      def.setName (f.getName ());
      def.setType (build (f.getType ()));
      def.setOptional (f.isOptional ());
      return def;
   }

   private TypeDef build (Schema.Type t) throws BlinkException
   {
      switch (t.getCode ())
      {
       case I8: return build (new I8 (), t);
       case U8: return build (new U8 (), t);
       case I16: return build (new I16 (), t);
       case U16: return build (new U16 (), t);
       case I32: return build (new I32 (), t);
       case U32: return build (new U32 (), t);
       case I64: return build (new I64 (), t);
       case U64: return build (new U64 (), t);
       case F64: return build (new F64 (), t);
       case Decimal: return build (new Decimal (), t);
       case Date: return build (new com.pantor.blink.msg.blink.Date (), t);
       case TimeOfDayMilli: return build (new TimeOfDayMilli (), t);
       case TimeOfDayNano: return build (new TimeOfDayNano (), t);
       case Nanotime: return build (new NanoTime (), t);
       case Millitime: return build (new MilliTime (), t);
       case Bool: return build (new Bool (), t);
       case Object: return build (
          new com.pantor.blink.msg.blink.Object (), t);

       case FixedDec:
         {
            FixedDec def = new FixedDec ();
            def.setScale ((byte)((Schema.FixedDecType)t).getScale ());
            return build (def, t);
         }
         
       case String:
         {
            com.pantor.blink.msg.blink.String def =
               new com.pantor.blink.msg.blink.String ();
            Schema.StrType st = (Schema.StrType)t;
            if (st.hasMaxSize ())
               def.setMaxSize ((byte)((int)st.getMaxSize ()));
            return build (def, t);
         }
            
       case Binary:
         {
            Binary def = new Binary ();
            Schema.StrType st = (Schema.StrType)t;
            if (st.hasMaxSize ())
               def.setMaxSize ((byte)((int)st.getMaxSize ()));
            return build (def, t);
         }
            
       case Fixed:
         {
            Fixed def = new Fixed ();
            def.setSize (((Schema.FixedType)t).getSize ());
            return build (def, t);
         }
         
       case Ref:
         {
            Schema.Ref rt = t.toRef ();
            Schema.DefBase d = schema.find (rt.getName (), rt.getDefaultNs ());
            if (d != null)
            {
               if (rt.isDynamic ())
               {
                  DynRef def = new DynRef ();
                  def.setType (makeName (d.getName ()));
                  return build (def, t);
               }
               else
               {
                  Ref def = new Ref ();
                  def.setType (makeName (d.getName ()));
                  return build (def, t);
               }
            }
            else
               throw new BlinkException (
                  "No such blink definition: " + rt.getName ());
         }
         
       case Enum:
         {
            Enum def = new Enum ();
            Schema.Enum et = t.toEnum ();
            List<Schema.Symbol> fromSyms = et.getSymbols ();
            Symbol [] toSyms = new Symbol [fromSyms.size ()];
            def.setSymbols (toSyms);
            int pos = 0;
            for (Schema.Symbol fromSym : fromSyms)
            {
               Symbol toSym = new Symbol ();
               setAnnotations (fromSym, toSym);
               toSym.setName (fromSym.getName ());
               toSym.setValue (fromSym.getValue ());
               toSyms [pos ++] = toSym;
            }

            return build (def, t);
         }

       default:
         return null;
      }
   }

   private TypeDef build (TypeDef def, Schema.Type t)
   {
      setAnnotations (t, def);
      if (t.isSequence ())
      {
         Sequence seq = new Sequence ();
         seq.setType (def);
         return seq;
      }
      else
         return def;
   }

   private static com.pantor.blink.msg.blink.NsName makeName (NsName name)
   {
      com.pantor.blink.msg.blink.NsName nm =
         new com.pantor.blink.msg.blink.NsName ();
      nm.setName (name.getName ());
      if (name.isQualified ())
         nm.setNs (name.getNs ());
      return nm;
   }

   private static void setAnnotations (Annotated from,
                                       com.pantor.blink.msg.blink.Annotated to)
   {
      AnnotSet fromAnnots = from.getAnnotations ();
      if (fromAnnots != null)
      {
         com.pantor.blink.msg.blink.Annotation [] toAnnots =
            new com.pantor.blink.msg.blink.Annotation [fromAnnots.size ()];
         to.setAnnotations (toAnnots);
         int pos = 0;
         for (Annotation a : fromAnnots)
         {
            com.pantor.blink.msg.blink.Annotation a_ =
               new com.pantor.blink.msg.blink.Annotation ();
            a_.setName (makeName (a.getName ()));
            a_.setValue (a.getValue ());
            toAnnots [pos ++] = a_;
         }
      }
   }
   
   private static final class Collector
   {
      private Collector (Schema schema, HashSet<Long> visited)
      {
         this.schema = schema;
         this.visited = visited;
      }

      void collect (NsName name, String defaultNs) throws BlinkException
      {
         Schema.DefBase d = schema.find (name, defaultNs);
         if (d instanceof Schema.Group)
         {
            collect ((Schema.Group)d);
         }
         else if (d instanceof Schema.Define)
         {
            Schema.Define d_ = (Schema.Define)d;
            if (visited.add (d_.getTypeId ()))
            {
               defs.add (d_);
               collect (d_.getType ());
            }
         }
         else
            throw new BlinkException ("No such blink definition: " + name);
      }

      void collect (NsName name) throws BlinkException
      {
         collect (name, null);
      }

      void collect (Schema.Group g) throws BlinkException
      {
         if (visited.add (g.getTypeId ()))
         {
            grps.add (g);

            if (g.hasSuper ())
               collect (g.getSuperGroup ());

            for (Schema.Field f : g.getFields ())
               collect (f.getType ());
         }
      }

      void collect (Schema.Type t) throws BlinkException
      {
         Schema.Ref r = t.toRef ();
         if (r != null)
            collect (r.getName (), r.getDefaultNs ());
      }
      
      private final Schema schema;
      private final HashSet<Long> visited;
      private final ArrayList<Schema.Define> defs =
         new ArrayList<Schema.Define> ();
      private final ArrayList<Schema.Group> grps =
         new ArrayList<Schema.Group> ();
   }

   private final Schema schema;
}
