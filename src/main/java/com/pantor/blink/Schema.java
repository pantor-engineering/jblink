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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

public final class Schema extends AnnotatedBase
{
   public static enum Presence { Optional, Required };
   public static enum Rank { Single, Sequence };
   public static enum Layout { Static, Dynamic };
   public static enum PathType { NameStep, TypeStep };

   public static enum TypeCode
   {
      I8, U8, I16, U16, I32, U32, I64, U64, F64, Decimal, Date, TimeOfDayMilli,
      TimeOfDayNano, Nanotime, Millitime, Bool, String, Object, Ref, Enum
   }

   public static class Group extends Component implements Iterable<Field>
   {
      public Group (NsName name, Long id, NsName super_, AnnotSet annots,
                    Location loc)
      {
         super (annots, loc);
         this.name = name;
         this.id = id;
         this.super_ = super_;
      }

      public Iterator<Field> iterator () { return fields.iterator (); }

      public Field getField (String name) { return fieldMap.get (name); }
      public Field addField (String name, String id, Type type, Presence pres,
                             AnnotSet annots, Location loc)
         throws BlinkException.Schema
      {
         if (fieldMap.containsKey (name))
            throw error ("Duplicate field name in " + this.name + ": " + name, 
                         loc);
         Field f = new Field (name, id, type, pres, annots, loc);
         fields.add (f);
         fieldMap.put (name, f);
         return f;
      }
      
      public List<Field> getFields () { return fields; }

      @Override public String toString ()
      {
         StringBuilder sb = new StringBuilder ();
         sb.append (getAnnotStr ("\n"));
         sb.append (name);
         if (id != null)
            sb.append ("/" + id);
         if (super_ != null)
            sb.append (" : " + super_);
         if (! fields.isEmpty ())
         {
            sb.append (" ->");
            int pos = 0;
            for (Field f : fields)
            {
               if (pos > 0)
                  sb.append (',');
               sb.append ("\n  ");
               sb.append (f);
               ++ pos;
            }
         }
         
         return sb.toString ();
      }

      public NsName getName () { return name; }
      public Long getId () { return id; }
      public boolean hasId () { return id != null; }
      public void setId (Long id) { this.id = id; }

      public boolean hasSuper () { return super_ != null; }

      public NsName getSuper () { return super_; }
      public Group getSuperGroup () { return superGrp; }
      public void setSuperGroup (Group superGrp) { this.superGrp = superGrp; }
      
      private final NsName name;
      private Long id;
      private final NsName super_;
      private final ArrayList<Field> fields = new ArrayList <Field> ();
      private final HashMap<String, Field> fieldMap =
         new HashMap<String, Field> ();
      private Group superGrp;
   }

   public static class Field extends Component
   {
      public Field (String name, String id, Type type, Presence pres,
                    AnnotSet annots, Location loc)
      {
         super (annots, loc);
         this.name = name;
         this.type = type;
         this.presence = pres;
         this.id = id;
      }

      public Presence getPresence () { return presence; }
      public boolean isOptional () { return presence == Presence.Optional; }
      
      public String getName () { return name; }

      public Type getType () { return type; }
      public void setType (Type type) { this.type = type; }

      public void setId (String id) { this.id = id; }
      public String getId () { return id; }

      @Override
      public String toString ()
      {
         StringBuilder sb = new StringBuilder ();
         sb.append (type);
         sb.append (" ");
         sb.append (getAnnotStr (" "));
         sb.append (name);
         if (Util.isSet (id))
            sb.append ("/" + id);
         if (isOptional ())
            sb.append ('?');
         return sb.toString ();
      }
      
      private final String name;
      private String id;
      private final Presence presence;
      private Type type;
   }

   public static class Define extends Component
   {
      public Define (NsName name, String id, Type type, AnnotSet annots,
                     Location loc)
      {
         super (annots, loc);
         this.name = name;
         this.id = id;
         this.type = type;
      }

      public NsName getName () { return name; }
      
      public Type getType () { return type; }
      public void setType (Type type) { this.type = type; }
      public void setId (String id) { this.id = id; }
      public String getId () { return id; }

      @Override
      public String toString ()
      {
         StringBuilder sb = new StringBuilder ();
         sb.append (getAnnotStr (" "));
         sb.append (name);
         if (Util.isSet (id))
            sb.append ("/" + id);
         sb.append (" = ");
         sb.append (type);
         return sb.toString ();
      }

      private final NsName name;
      private String id;
      private Type type;
   }

   public static class TypeInfo
   {
      public boolean isGroup () { return group != null; }
      public boolean isObject ()
      {
         return type != null && type.getCode () == TypeCode.Object;
      }
      public boolean isEnum () { return enumDef != null; }
      public boolean isSequence () { return isSeq; }
      public boolean isDynamic () { return isDyn || isObject (); }
      public boolean isPrimitive () { return type != null && ! isObject (); }

      public void setDynamic (boolean d) { isDyn = isDyn || d; }
      public void setSequence (boolean s) { isSeq = isSeq || s; }
      public void setGroup (Group g) { group = g; }
      public void setEnum (Define d) { enumDef = d; }
      public void setType (Type t) { type = t; }

      public Group getGroup () { return group; }
      public Define getEnum () { return enumDef; }
      public Type getType () { return type; }
   
      private Group group;
      private Define enumDef;
      private Type type;
      private boolean isSeq;
      private boolean isDyn;
   }
   
   public static class Type extends Component
   {
      public Type (TypeCode code, Rank rank, AnnotSet annots, Location loc)
      {
         super (annots, loc);
         this.code = code;
         this.rank = rank;
      }

      public TypeCode getCode () { return code; }
      public Rank getRank () { return rank; }
      public boolean isSequence () { return rank == Rank.Sequence; }

      public void resolve (Schema s, TypeInfo t)
      {
         t.setSequence (isSequence ());
         t.setType (this);
      }
      
      @Override
      public String toString ()
      {
         return getAnnotStr (" ") + Util.decapitalize (code.name ()) +
            (isSequence () ? " []" : "");
      }

      public Enum toEnum () { return null; }
      public Ref toRef () { return null; }

      public boolean isEnum () { return toEnum () != null; }
      public boolean isRef () { return toRef () != null; }
      
      private final TypeCode code;
      private final Rank rank;
   }

   public static class StrType extends Type
   {
      public StrType (String ct, Rank rank, AnnotSet annots, Location loc)
      {
         super (TypeCode.String, rank, annots, loc);
         contentType = ct;
      }

      @Override
      public String toString ()
      {
         String ct = "";
         if (Util.isSet (contentType))
            ct = " (" + contentType + ")";
         return getAnnotStr (" ") + "string" + ct +
            (isSequence () ? " []" : "");
      }

      public String getContentType () { return contentType; }
      private final String contentType;
   }

   public static class Ref extends Type
   {
      public Ref (NsName name, String ns, Rank rank, Layout layout,
                  AnnotSet annots, Location loc)
      {
         super (TypeCode.Ref, rank, annots, loc);
         this.name = name;
         this.ns = ns;
         this.layout = layout;
      }

      public boolean isDynamic () { return layout == Layout.Dynamic; }
      public NsName getName () { return name; }
      public String getNs () { return ns; }

      @Override
      public void resolve (Schema s, TypeInfo t)
      {
         t.setDynamic (isDynamic ());
         t.setSequence (isSequence ());
         s.resolve (name, ns, t);
      }
     
      @Override
      public String toString ()
      {
         return getAnnotStr (" ") + name.toString () +
            (isDynamic () ? "*" : "") + (isSequence () ? " []" : "");
      }

      @Override
      public Ref toRef () { return this; }
      
      private final NsName name;
      private final String ns;
      private final Layout layout;
   }

   public static class Enum extends Type implements Iterable<Symbol>
   {
      Enum (Location loc)
      {
         super (TypeCode.Enum, Rank.Single, null /* No annots */, loc);
      }
      
      public Symbol addSymbol (String name, int val, AnnotSet annots,
                               Location loc)
         throws BlinkException.Schema
      {
         if (symMap.containsKey (name))
            throw error ("Duplicate symbol name in enum: " + name, loc);
         
         if (symByVal.containsKey (val))
            throw error (String.format ("Duplicate symbol value in enum: %d",
                                        val), loc);

         Symbol sym = new Symbol (name, val, annots, loc);
         symMap.put (name, sym);
         symByVal.put (val, sym);
         symbols.add (sym);
         return sym;
      }

      public void updateValue (Symbol sym, int val)
         throws BlinkException.Schema
      {
         symByVal.remove (sym.getValue ());
         if (symByVal.containsKey (val))
            throw error (String.format ("Duplicate symbol value in enum: %d",
                                        val), sym);
         sym.setValue (val);
         symByVal.put (val, sym);
      }

      public List<Symbol> getSymbols () { return symbols; }
      public Symbol getSymbol (String name) { return symMap.get (name); }
      public Symbol getSymbol (int val) { return symByVal.get (val); }
      
      public Iterator<Symbol> iterator () { return symbols.iterator (); }

      @Override public String toString ()
      {
         StringBuilder sb = new StringBuilder ();
         if (symbols.size () == 1)
            sb.append ("| " + symbols.get (0));
         else
         {
            int pos = 0;
            for (Symbol s : symbols)
            {
               if (pos > 0)
                  sb.append (" | ");
               sb.append (s);
               ++ pos;
            }
         }

         return sb.toString ();
      }

      @Override
      public Enum toEnum () { return this; }
      
      private final ArrayList<Symbol> symbols = new ArrayList<Symbol> ();
      private final HashMap<String, Symbol> symMap =
         new HashMap <String, Symbol> ();
      private final HashMap<Integer, Symbol> symByVal =
         new HashMap <Integer, Symbol> ();
   }

   public static class Symbol extends Component
   {
      public Symbol (String name, int value, AnnotSet annots, Location loc)
      {
         super (annots, loc);
         this.name = name;
         this.value = value;
      }

      public String getName () { return name; }
      public int getValue () { return value; }
      public void setValue (int value) { this.value = value; }
      
      @Override
      public String toString ()
      {
         return String.format ("%s%s/%d", getAnnotStr (" "), name, value);
      }
      
      private final String name;
      private int value;
   }

   public static class Component extends AnnotatedBase implements Located
   {
      protected Component (AnnotSet annots, Location loc)
      {
         super (annots);
         this.loc = loc;
      }

      protected Component ()
      {
         this (null, null);
      }

      @Override
      public Location getLocation ()
      {
         return loc;
      }

      @Override
      public void setLocation (Location loc)
      {
         this.loc = loc;
      }

      public List<String> getAnnotStrList ()
      {
         ArrayList<String> result = new ArrayList<String> ();
         if (isAnnotated ())
            for (Annotation a : getAnnotations ())
               result.add (a.toString ());
         return result;
      }

      public String getAnnotStr (String sep)
      {
         if (isAnnotated ())
            return Util.join (getAnnotStrList (), sep) + sep;
         else
            return "";
      }

      private Location loc;
   }

   public Group addGroup (NsName name, Long id, NsName super_,
                          AnnotSet annots, Location loc)
      throws BlinkException.Schema
   {
      if (isUniqueDef (name))
      {
         Group g = new Group (name, id, super_, annots, loc);
         grpMap.put (name, g);
         groups.add (g);
         return g;
      }
      else
         throw duplicateErr ("group", name, loc);
   }

   public Define addDefine (NsName name, String id, Type type, AnnotSet annots,
                            Location loc)
      throws BlinkException.Schema
   {
      if (isUniqueDef (name))
      {
         Define d = new Define (name, id, type, annots, loc);
         defMap.put (name, d);
         defines.add (d);
         return d;
      }
      else
         throw this.duplicateErr ("type", name, loc);
   }

   public Define addDefine (NsName name, String id, AnnotSet annots,
                            Location loc)
      throws BlinkException.Schema
   {
      return addDefine (name, id, null, annots, loc);
   }

   public Group getGroup (String s)
   {
      return getGroup (NsName.get (s));
   }
   
   public Group getGroup (NsName n)
   {
      return grpMap.get (n);
   }
   
   public Define getDefine (String s)
   {
      return getDefine (NsName.get (s));
   }
   
   public Define getDefine (NsName n)
   {
      return defMap.get (n);
   }

   public List<Define> getDefines () { return defines; }
   public List<Group> getGroups () { return groups; }
   
   public void addAnnotations (AnnotSet annots, String ns)
   {
      AnnotSet domain = annotsPerNs.get (ns);
      if (domain == null)
         annotsPerNs.put (ns, new AnnotSet (annots));
      else
         domain.add (annots);
      addAnnotations (annots);
   }

   public String getAnnotation (NsName name, String ns)
   {
      AnnotSet annots = getAnnotations (ns);
      return annots != null ? annots.get (name) : null;
   }

   public String getAnnotation (NsName name)
   {
      return getAnnotation (name, null);
   }

   public AnnotSet getAnnotations (String ns)
   {
      if (Util.isSet (ns))
         return annotsPerNs.get (ns);
      else
         return getAnnotations ();
   }

   public void addIncrAnnot (NsName name, String ns, String substep, PathType t,
                             String id, AnnotSet annots, Location loc)
   {
      pendIncrAnnots.add (
         new IncrAnnot (name, ns, substep, t, id, annots, loc));
   }

   public void finalizeSchema () throws BlinkException.Schema
   {
      for (IncrAnnot a : pendIncrAnnots)
         applyIncrAnnot (a);
      pendIncrAnnots.clear ();
      checkAndResolve ();
   }

   public void resolve (Type t, TypeInfo info)
   {
      t.resolve (this, info);
   }

   public TypeInfo resolve (Type t)
   {
      TypeInfo info = new TypeInfo ();
      t.resolve (this, info);
      return info;
   }

   public TypeInfo resolve (NsName name, String ns)
   {
      TypeInfo info = new TypeInfo ();
      resolve (name, ns, info);
      return info;
   }
   
   public void resolve (NsName name, String ns, TypeInfo t)
   {
      Object d = find (name, ns);
      if (d instanceof Group)
         t.setGroup ((Group)d);
      else if (d instanceof Define)
      {
         Define def = (Define)d;
         if (def.getType ().getCode () == TypeCode.Enum)
            t.setEnum (def);
         else
            def.getType ().resolve (this, t);
      }
   }

   public Object find (NsName name, String defaultNs)
   {
      Object d = defMap.get (name);
      if (d == null)
         d = grpMap.get (name);
      if (d == null && ! name.isQualified () && Util.isSet (defaultNs))
      {
         name = NsName.get (defaultNs, name.getName ());
         d = defMap.get (name);
         if (d == null)
            d = grpMap.get (name);
      }

      return d;
   }

   private static BlinkException.Schema error (String s, Location loc)
   {
      loc = loc != null ? loc : new Location ();
      return new BlinkException.Schema (s, loc);
   }

   private static BlinkException.Schema error (String s, Located comp)
   {
      return error (s, comp.getLocation ());
   }

   private static BlinkException.Schema recursionError (
      String s, NsName name, Located comp)
   {
      return error ("Illegal recursive reference: the " + s + " definition " + 
                    name + " directly or indirectly refers to itself", comp);
   }

   private static BlinkException.Schema refError (
      String s, NsName name, String ns, Located comp)
   {
      String msg = "No such definition in " + s + " reference: " + name;

      if (! name.isQualified () && Util.isSet (ns))
         msg += " or " + ns + ":" + name;
      return error (msg, comp);
   }

   private boolean isUniqueDef (NsName name)
   {
      return ! (grpMap.containsKey (name) || defMap.containsKey (name));
   }

   private BlinkException.Schema duplicateErr (String kind, NsName name,
                                               Location loc)
   {
      Component prev = grpMap.get (name);
      if (prev == null)
         prev = defMap.get (name);

      String prevKind = (prev instanceof Group) ? "group" : "type";
      String msg = String.format ("Conflicting blink %s definition: %s" +
                                  "\n  Previously defined as %s here: %s",
                                  kind, name, prevKind, prev.getLocation ());
      return error (msg, loc);
   }

   @Override public String toString ()
   {
      StringBuilder sb = new StringBuilder ();
      for (Define d : defines)
         sb.append (d + "\n");
      for (Group g : groups)
         sb.append (g + "\n");
      return sb.toString ();
   }

   private void applyIncrAnnot (IncrAnnot a) throws BlinkException.Schema
   {
      Object d = find (a.name, a.ns);
      if (d instanceof Define)
      {
         Define def = (Define)d;
         Type t = def.getType ();
         Enum enm = t.toEnum ();
         if (Util.isSet (a.substep))
         {
            if (a.type == PathType.TypeStep)
               throw error ("Cannot use a substep and the keyword 'type' " +
                            "together when referencing a type definition", a);
            if (enm != null)
            {
               Symbol sym = enm.getSymbol (a.substep);
               if (sym != null)
               {
                  if (Util.isSet (a.id))
                     enm.updateValue (sym, Util.parseI32IdAnnot (a.id));
                  if (a.isAnnotated ())
                     sym.addAnnotations (a.getAnnotations ());
               }
            }
            else
               throw error ("Cannot use a substep reference on a type " +
                            "definition that is not an enum", a);
         }
         else if (a.type == PathType.TypeStep)
         {
            if (enm != null)
               throw error ("Cannot apply incremental annotations to " +
                            "an enum type as a whole", a);
            if (a.isAnnotated ())
               t.addAnnotations (a.getAnnotations ());
         }
         else
         {
            if (Util.isSet (a.id))
               def.setId (a.id);
            if (a.isAnnotated ())
               def.addAnnotations (a.getAnnotations ());
         }
      }
      else if (d instanceof Group)
      {
         Group g = (Group)d;
         if (Util.isSet (a.substep))
         {
            Field f = g.getField (a.substep);
            if (f != null)
            {
               if (a.type == PathType.TypeStep)
               {
                  if (a.isAnnotated ())
                     f.getType ().addAnnotations (a.getAnnotations ());
               }
               else
               {
                  if (Util.isSet (a.id))
                     f.setId (a.id);
                  if (a.isAnnotated ())
                     f.addAnnotations (a.getAnnotations ());
               }
            }
            else
               log.warning (String.format (
                               "%s: warning: No such field in incremental " +
                               "annotation: %s.%s", a.getLocation (), a.name, a.substep));
         }
         else
         {
            if (a.type == PathType.TypeStep)
               throw error ("Cannot use keyword 'type' directly on a group " +
                            "reference", a);
            if (Util.isSet (a.id))
               g.setId (Util.parseU64IdAnnot (a.id));
            if (a.isAnnotated ())
               g.addAnnotations (a.getAnnotations ());
         }
      }
      else
         log.warning (String.format (
                         "%s: warning: No such group or define in incremental " +
                         "annotation: %s", a.getLocation (), a.name));
   }

   private void checkAndResolve () throws BlinkException.Schema
   {
      for (Define d : defines)
         resolveDefs (d);

      for (Group g : groups)
         resolveGrps (g);

      for (Group g : groups)
         resolveSuper (g);
   }

   private void resolveDefs (Define d) throws BlinkException.Schema
   {
      resolveDefs (d, d, false, new HashSet<NsName> ());
   }

   private void resolveDefs (Define d, Component referrer, boolean isSequence,
                             HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      if (! visited.add (d.getName ()))
         throw recursionError ("type", d.getName (), referrer);

      if (isSequence && d.getType ().isSequence ())
         throw error ("The sequence item type " + d.getName () +
                      " must not be a sequence in itself", referrer);

      resolveDefsType (d.getType (), isSequence, visited);
      
      visited.remove (d.getName ());
   }

   private void resolveDefsType (Type t, boolean isSequence,
                                 HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      Ref r = t.toRef ();
      if (r != null)
      {
         if (! resolveDefsRef (r, isSequence || r.isSequence (), visited))
            throw refError ("type", r.getName (), r.getNs (), r);
      }
   }

   private boolean resolveDefsRef (Ref r, boolean isSequence,
                                   HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      Object d = find (r.getName (), r.getNs ());
      if (d != null)
      {
         if (d instanceof Define)
            resolveDefs ((Define)d, r, isSequence, visited);
         return true;
      }
      else
         return false;
   }

   private void resolveGrps (Group g) throws BlinkException.Schema
   {
      resolveGrps (g, g, new HashSet<NsName> ());
   }

   private void resolveGrps (Group g, Component referrer,
                             HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      NsName name = g.getName ();

      if (! visited.add (name))
         throw recursionError ("group", name, referrer);

      for (Field f : g)
         resolveGrpsType (f.getType (), f, visited);

      if (g.hasSuper ())
      {
         if (! resolveGrpsRef (g.getSuper (), name.getNs (), g, null, visited))
            throw refError ("super", g.getSuper (), name.getNs (), g);
      }

      visited.remove (g.getName ());
   }

   private void resolveGrpsType (Type t, Field f, HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      if (f != null && t != f.getType () && t.isSequence () &&
          f.getType ().isSequence ())
         throw error ("The sequence item type of the field " + f.getName () +
                      " must not also be a sequence", f);

      Ref r = t.toRef ();
      if (r != null)
      {
         if (r.isDynamic ())
         {
            TypeInfo ti = resolve (r);
            if (! ti.isGroup ())
               throw error ("Dynamic reference to " + r.getName () +
                            " does not refer to a group definition", r);
         }
         else
         {
            
            if (! resolveGrpsRef (r.getName (), r.getNs (), r, f, visited))
               throw refError ("type", r.getName (), r.getNs (), r);
         }
      }
   }

   private boolean resolveGrpsRef (NsName name, String ns, Component r, Field f,
                                   HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      Object d = find (name, ns);
      if (d != null)
      {
         if (d instanceof Group)
            resolveGrps ((Group)d, r, visited);
         else
            resolveGrpsDef ((Define)d, r, f, visited);
         return true;
      }
      else
         return false;
   }

   private void resolveGrpsDef (Define d, Component referrer, Field f,
                                HashSet<NsName> visited)
      throws BlinkException.Schema
   {
      if (! visited.add (d.getName ()))
         throw recursionError ("type", d.getName (), referrer);
      resolveGrpsType (d.getType (), f, visited);
      visited.remove (d.getName ());
   }

   private static class FieldInfo
   {
      FieldInfo (Group g, Field f) { this.g = g; this.f = f; }
      final Group g;
      final Field f;
   }
   
   private void resolveSuper (Group g) throws BlinkException.Schema
   {
      if (g.hasSuper ())
         resolveSuper (g, new HashMap<String, FieldInfo> ());
   }

   private void resolveSuper (Group g, HashMap<String, FieldInfo> unique)
      throws BlinkException.Schema
   {
      if (g.hasSuper ())
      {
         if (g.getSuperGroup () == null)
         {
            TypeInfo i = resolve (g.getSuper (), g.getName ().getNs ());
            if (! i.isGroup ())
               throw error ("Supergroup reference to " + g.getSuper () +
                            " does not refer to a group definition", g);
            if (i.isDynamic ())
               throw error ("Supergroup reference to " + g.getSuper () +
                            " must not be dynamic", g);
            if (i.isSequence ())
               throw error ("Supergroup reference " + g.getSuper () +
                            " must not refer to a sequence", g);
            g.setSuperGroup (i.getGroup ());
         }

         resolveSuper (g.getSuperGroup (), unique);
      }
      
      for (Field f : g)
      {
         FieldInfo shadows = unique.put (f.getName (), new FieldInfo (g, f));
         if (shadows != null)
            throw error ("The field " + g.getName () + "." + f.getName () +
                         " shadows a field inherited from " +
                         shadows.g.getName () +
                         "\n  Defined here: " + shadows.f.getLocation (), f);
      }
   }
   
   private final static class IncrAnnot extends Component
   {
      IncrAnnot (NsName name, String ns, String substep, PathType type,
                 String id, AnnotSet annots, Location loc)
      {
         super (annots, loc);
         this.name = name;
         this.ns = ns;
         this.substep = substep;
         this.type = type;
         this.id = id;
      }
      
      final NsName name;
      final String ns;
      final String substep;
      final PathType type;
      final String id;
   };

   private final HashMap<NsName, Define> defMap =
      new HashMap<NsName, Define> ();
   private final HashMap<NsName, Group> grpMap =
      new HashMap<NsName, Group> ();
   private final ArrayList<Define> defines = new ArrayList<Define> ();
   private final ArrayList<Group> groups = new ArrayList<Group> ();
   
   private final HashMap<String, AnnotSet> annotsPerNs =
      new HashMap<String, AnnotSet> ();
   private final ArrayList<IncrAnnot> pendIncrAnnots =
      new ArrayList<IncrAnnot> ();

   private final static Logger log = Logger.getLogger (Schema.class.getName ());
}
