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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;
import java.io.IOException;
import java.io.Reader;

/**
   The {@code DefaultObjectModel} class implements the {@code
   ObjectModel} interface and supports a set of mapping rules between
   POJO classes and the corresponding messages in the schema. In
   addition to the mapping rules, it manages a schema instance for
   convenience.

   <p>The mapping between Java classes and groups specified in the
   Blink schema can be parameterized in three ways: you can set a
   wrapper class using the {@code setWrapper} method, you can set a
   default package name using the {@code setPackage} method and you
   can set a package name per Blink namespace using the {@code
   setNamespacePackage} method. These three parameters are referred to
   as {@code <wrapper>}, {@code <package>} and {@code <package[ns]>} in
   the patterns below.</p>
   
   <p>The mapping process tries to form a fully qualified class name
   based on a list of patterns. Each pattern is tested in the order
   specified below until one results in a class that exists and should
   be <i>included</i>. A candidate class should be included if</p>

   <ul>
   <li>no <i>inclusive</i> class annotation is set in this object model,</li>
   <li>or an inclusive class annotation is set and the candidate class has this
   annotation,</li>
   </ul>

   <p>and</p>

   <ul>
   <li>no <i>exclusive</i> class annotation is set in this object model,</li>
   <li>or an exclusive class annotation is set and the candidate class does not
   have this annotation</li>
   </ul>

   <p>In the list of patterns {@code <ns>} refers to the namespace
   where the group was declared and {@code <name>} refers to the name
   of the group. {@code <alias>} refers to the value of the annotation
   {@code @blink:alias} if present on the group.</p>

   <p>If any component in the pattern is the empty string, or {@code
   null}, the pattern is skipped. All components except {@code <name>}
   can potentially be empty.</p>

   <p>The list of patterns is as follows:</p>

   <ol>
   <li>{@code <wrapper>$<name>}</li>
   <li>{@code <wrapper>$<alias>}</li>
   <li>{@code <package[ns]>.<ns>$<name>}</li>
   <li>{@code <package[ns]>.<ns>$<alias>}</li>
   <li>{@code <package[ns]>.<name>}</li>
   <li>{@code <package[ns]>.<alias>}</li>
   <li>{@code <package>.<camelbackToUnderscoreLower(ns)>.<name>}</li>
   <li>{@code <package>.<camelbackToUnderscoreLower(ns)>.<alias>}</li>
   <li>{@code <package>.<ns>$<name>}</li>
   <li>{@code <package>.<ns>$<alias>}</li>
   <li>{@code <package>.<name>}</li>
   <li>{@code <package>.<alias>}</li>
   <li>{@code <camelbackToUnderscoreLower(ns)>.<name>}</li>
   <li>{@code <camelbackToUnderscoreLower(ns)>.<alias>}</li>
   <li>{@code <ns>$<name>}</li>
   <li>{@code <ns>$<alias>}</li>
   <li>{@code <name>}</li>
   <li>{@code <alias>}</li>
   </ol>

   <p>In the patterns above, {@code camelbackToUnderscoreLower(s)}
   returns {@code s} splitted at any upper case letter immediately
   preceded by a lower case letter, then joined by '_' and finally
   converted to lowercase, for example, {@code QuickBrownFox}
   becomes {@code quick_brown_fox}</p>

   <p>Names of classes and packages that are reserved in Java will be
   quoted before matching. A name is quoted by appending an underscore
   to it. Also, names already ending with an underscore will be
   appended with another underscore. For example, {@code class}
   becomes {@code class_} and {@code class_} becomes {@code
   class__}.</p>

   <p>When the object model has found a matching class to a schema
   group, it maps the fields of the group to corresponding getter and
   setter methods based on the name or {@code @blink:alias} of the field.</p>

   <p>For each mandatory field it looks for a getter with one of the
   following signatures, in order:<p>

   <ol>
   <li>{@code T get<name> ()}</li>
   <li>{@code T get<alias> ()}</li>
   <li>{@code T get_<name> ()}</li>
   <li>{@code T get_<alias> ()}</li>
   <li>{@code T get<capitalize(name)> ()}</li>
   <li>{@code T get<capitalize(alias)> ()}</li>
   </ol>

   <p>and a setter with one of the following signatures, in order:</p>
   
   <ol>
   <li>{@code void set<name> (T val)}</li>
   <li>{@code void set<alias> (T val)}</li>
   <li>{@code void set_<name> (T val)}</li>
   <li>{@code void set_<alias> (T val)}</li>
   <li>{@code void set<capitalize(name)> (T val)}</li>
   <li>{@code void set<capitalize(alias)> (T val)}</li>
   </ol>

   <p>For each optional field it also looks for a presence method with
   one of the following signatures, in order:</p>

   <ol>
   <li>{@code boolean has<name> ()}</li>
   <li>{@code boolean has<alias> ()}</li>
   <li>{@code boolean has_<name> ()}</li>
   <li>{@code boolean has_<alias> ()}</li>
   <li>{@code boolean has<capitalize(name)> ()}</li>
   <li>{@code boolean has<capitalize(alias)> ()}</li>
   </ol>

   <p>In the method patterns above {@code capitalize(s)} returns
   {@code s} with the first first character converted to upper
   case.</p>

   <p>Reserved method names are quoted in the same way as group names
   by appending an underscore. The only reserved method name is {@code
   getClass}, but the escaping will be done symetrically for the
   setter and presence methods too. Thus if the field is named {@code
   class} in Blink, then it will be matched against {@code getclass},
   {@code get_class} and {@code getClass_}.</p>

   <p>For fields of the Blink type decimal, alternative signatures are
   accepted: [NOTE: This variant is not yet fully implemented in the
   codec]</p>

   <ol>
   <li>{@code void set... (long mantissa, int exponent)}</li>
   <li>{@code void get... (com.pantor.blink.DecimalResult result)}</li>
   </ol>

   <p>where {@code ...} means one of the six name variants used in the
   patterns above.</p>

   <p>This object model maps Blink enums to classes using the same
   lookup method as it uses for groups. It then maps the individual
   enum symbols, {@code <sym>}, or their aliases, to the first
   matching alternative in the following list:

   <ol>
   <li>{@code <sym>}</li>
   <li>{@code <alias>}</li>
   <li>{@code <toUpper(sym)>}</li>
   <li>{@code <toUpper(alias)>}</li>
   <li>{@code <camelbackToUnderscoreUpper(sym)>}</li>
   <li>{@code <camelbackToUnderscoreUpper(alias)>}</li>
   </ol>

   <p>where {@code toUpper(s)} returns {@code s} with all characters
   converted to upper case, and {@code camelbackToUnderscoreUpper(s)}
   returns {@code s} splitted at any upper case letter immediately
   preceded by a lower case letter, then joined by '_' and finally
   converted by {@code toUpper(s)}, for example, {@code QuickBrownFox}
   becomes {@code QUICK_BROWN_FOX}</p>

   <p>A symbol name is quoted in the same way as a class or package
   name, by appending an underscore, if the name would otherwise
   collide with a Java keyword.</p>

   <p>When the object model maps fields to methods it also considers
   inclusive and exclusive method annotations in the same way as they
   are used for the mapping of groups to classes.</p>

   <p>The default inclusive class and method annotation is {@code
   null}, and the default exclusive class and method annotation is
   {@code @NoBlink}.</p>
*/

public final class DefaultObjectModel implements ObjectModel
{
   /**
      Creates an object model containing an empty schema.
   */
   
   public DefaultObjectModel ()
   {
      this (new Schema ());
   }
   
   /**
      Creates an object model and loads schemas from files specified
      by one or more file names.

      @param schemas a list of schema file names
      @throws IOException if an input error occured
      @throws BlinkException if there was a schema problem
   */
   
   public DefaultObjectModel (String... schemas)
      throws IOException, BlinkException
   {
      this ();
      loadSchema (schemas);
   }

   /**
      Creates an object model and loads schemas from one or more
      character readers.

      @param schemas a list of readers
      @throws IOException if an input error occured
      @throws BlinkException if there was a schema problem
   */

   public DefaultObjectModel (Reader... schemas)
      throws IOException, BlinkException
   {
      this ();
      loadSchema (schemas);
   }
   
   /**
      Creates an object model using the specified schema instance

      @param schema a schema
   */

   public DefaultObjectModel (Schema schema)
   {
      this.schema = schema;
   }

   /**
      Creates an object model using the specified schema instance and
      sets the wrapper property as specified.

      @param schema a schema
      @param wrapper a wrapper class
   */

   public DefaultObjectModel (Schema schema, Class<?> wrapper)
   {
      this (schema);
      this.wrapper = wrapper;
   }

   /**
      Creates an object model using the specified schema instance and
      sets the package property as specified.

      @param schema a schema
      @param pkg a package name
   */

   public DefaultObjectModel (Schema schema, String pkg)
   {
      this (schema);
      this.pkg = pkg;
   }

   /**
      Returns the schema managed by this object model

      @return the schema managed by this object model
      @throws BlinkException if there was a schema problem
   */
   
   @Override
   public Schema getSchema () throws BlinkException
   {
      synchronized (monitor)
      {
         init ();
         return schema;
      }
   }

   /**
      Loads schemas from files specified by one or more file names.

      @param schemas a list of schema file names
      @throws IOException if an input error occured
      @throws BlinkException if there was a schema problem
   */
   
   public void loadSchema (String... schemas)
      throws IOException, BlinkException
   {
      synchronized (monitor)
      {
         for (String f : schemas)
            SchemaReader.read (f, schema);
      }
   }

   /**
      Loads schemas from one or more character readers.

      @param schemas a list of readers
      @throws IOException if an input error occured
      @throws BlinkException if there was a schema problem
   */
   
   public void loadSchema (Reader... schemas)
      throws IOException, BlinkException
   {
      synchronized (monitor)
      {
         for (Reader rd : schemas)
            SchemaReader.read (rd, "-", schema);
      }
   }

   /**
      Loads schemas from one or more strings containing schema
      syntax. Each string argument must be a syntactically complete
      schema in itself.

      @param literals a list of schema literals
      @throws IOException if an input error occured
      @throws BlinkException if there was a schema problem
   */
   
   public void loadSchemaFromString (String... literals)
      throws IOException, BlinkException
   {
      synchronized (monitor)
      {
         for (String lit : literals)
            SchemaReader.readFromString (lit, schema);
      }
   }

   /**
      Sets the wrapper property of this object model. See the general
      description of the {@link DefaultObjectModel} for its usage.

      @param wrapper a wrapper class containing inner classes to be mapped
   */
   
   public void setWrapper (Class<?> wrapper)
   {
      synchronized (monitor)
      {
         this.wrapper = wrapper;
      }
   }

   /**
      Sets the default package property of this object model. See the general
      description of the {@link DefaultObjectModel} for its usage.

      @param pkg a package name used to constructs candidate class names in
      the mapping process
   */
   
   public void setPackage (String pkg)
   {
      synchronized (monitor)
      {
         this.pkg = pkg;
      }
   }

   /**
      Sets a namespace specific package property. See the general
      description of the {@link DefaultObjectModel} for its usage.

      @param ns a Blink namespace name
      @param pkg a package name used to constructs candidate class names in
      the mapping process for groups in the specified namespace
   */
   
   public void setNamespacePackage (String ns, String pkg)
   {
      synchronized (monitor)
      {
         pkgByNs.put (ns, pkg);
      }
   }

   /**
      Sets the inclusive class annotation to {@code @Blink} if {@code
      state} is {@code true}, or to {@code null} if {@code state} is
      {@code false}.

      @param state a flag that enables or disables the default
      inclusive class annotation
    */
   
   public void setUseOnlyAnnotatedClasses (boolean state)
   {
      synchronized (monitor)
      {
         setInclusiveClassAnnot (state ? Blink.class : null);
      }
   }

   /**
      Sets the inclusive class annotation

      @param inclusiveClassAnnot an annotation class
    */
   
   public void setInclusiveClassAnnot (
      Class<? extends java.lang.annotation.Annotation> inclusiveClassAnnot)
   {
      synchronized (monitor)
      {
         this.inclusiveClassAnnot = inclusiveClassAnnot;
      }
   }

   /**
      Sets the exclusive class annotation

      @param exclusiveClassAnnot an annotation class
    */
   
   public void setExclusiveClassAnnot (
      Class<? extends java.lang.annotation.Annotation> exclusiveClassAnnot)
   {
      synchronized (monitor)
      {
         this.exclusiveClassAnnot = exclusiveClassAnnot;
      }
   }

   /**
      Sets the inclusive method annotation to {@code @Blink} if {@code
      state} is {@code true}, or to {@code null} if {@code state} is
      {@code false}.

      @param state a flag that enables or disables the default
      inclusive method annotation
    */
   
   public void setUseOnlyAnnotatedMethods (boolean state)
   {
      synchronized (monitor)
      {
         setInclusiveMethodAnnot (state ? Blink.class : null);
      }
   }

   /**
      Sets the inclusive method annotation

      @param inclusiveMethodAnnot an annotation class
    */
   
   public void setInclusiveMethodAnnot (
      Class<? extends java.lang.annotation.Annotation> inclusiveMethodAnnot)
   {
      synchronized (monitor)
      {
         this.inclusiveMethodAnnot = inclusiveMethodAnnot;
      }
   }

   /**
      Sets the exclusive method annotation

      @param exclusiveMethodAnnot an annotation class
    */
   
   public void setExclusiveMethodAnnot (
      Class<? extends java.lang.annotation.Annotation> exclusiveMethodAnnot)
   {
      synchronized (monitor)
      {
         this.exclusiveMethodAnnot = exclusiveMethodAnnot;
      }
   }

   @Override
   public GroupBinding getGroupBinding (long tid)
      throws BlinkException
   {
      // FIXME: use TLS
      synchronized (monitor)
      {
         init ();
         GroupBinding b = grpBndById.get (tid);
         if (b != null)
            return b;
         else
            throw noBindingError (tid);
      }
   }
   
   @Override
   public GroupBinding getGroupBinding (NsName name)
      throws BlinkException
   {
      synchronized (monitor)
      {
         // FIXME: use TLS
         init ();
         GroupBinding b = grpBndByName.get (name);
         if (b != null)
            return b;
         else
            throw noBindingError (name);
      }
   }

   @Override
   public GroupBinding getGroupBinding (Class<?> name)
      throws BlinkException
   {
      synchronized (monitor)
      {
         // FIXME: use TLS
         init ();
         GroupBinding b = grpBndByClass.get (name);
         if (b != null)
            return b;
         else
            throw noBindingError (name);
      }
   }

   @Override
   public EnumBinding getEnumBinding (NsName name)
      throws BlinkException
   {
      synchronized (monitor)
      {
         // FIXME: use TLS
         init ();
         EnumBinding b = enumBndByName.get (name);
         if (b != null)
            return b;
         else
            throw noBindingError (name);
      }
   }

   private boolean isIncludedClass (Class<?> c)
   {
      return isIncluded (c, inclusiveClassAnnot, exclusiveClassAnnot);
   }
   
   private boolean isIncludedMethod (Method m)
   {
      return isIncluded (m, inclusiveMethodAnnot, exclusiveMethodAnnot);
   }

   private boolean isIncluded (
      AnnotatedElement e,
      Class<? extends java.lang.annotation.Annotation> inc,
      Class<? extends java.lang.annotation.Annotation> ex)
   {
      return
         (ex == null || ! e.isAnnotationPresent (ex)) &&
         (inc == null || e.isAnnotationPresent (inc));
   }
   
   private void init () throws BlinkException.Binding, BlinkException.Schema
   {
      if (initialized)
         return;
      initialized = true;
      
      schema.finalizeSchema ();

      for (Schema.Group g : schema.getGroups ())
      {
         // Look for a matching binding to the group or one of its ancestor
         
         GroupBinding bnd = null;
         for (Schema.Group candidate = g; candidate != null && bnd == null;
              candidate = candidate.getSuperGroup ())
            bnd = createGroupBinding (candidate);
         
         if (bnd != null)
         {
            if (g.hasId ())
            {
               if (grpBndById.containsKey (g.getId ()))
             throw ambiguousTypeIdError (g);
               else
                  grpBndById.put (g.getId (), bnd);
            }
         }
         else
         {
            unboundByName.put (g.getName (), g);
            if (g.hasId ())
               unboundById.put (g.getId (), g);
         }
      }
   }

   private static class GroupBindingImpl implements GroupBinding
   {
      public GroupBindingImpl (Schema.Group grp, Class<?> tgtType,
                               ArrayList<Field> fields)
      {
         this.grp = grp;
         this.tgtType = tgtType;
         this.fields = fields;
      }

      @Override public EnumBinding toEnum () { return null; }
      @Override public GroupBinding toGroup () { return this; }
      @Override public Schema.Group getGroup () { return grp; }
      @Override public Class<?> getTargetType () { return tgtType; }
      @Override public List<Field> getFields () { return fields; }
      
      @Override
      public Iterator<Field> iterator ()
      {
         return fields.iterator ();
      }
      
      private final Schema.Group grp;
      private final Class<?> tgtType;
      private final ArrayList<Field> fields;
   }

   private static class EnumBindingImpl implements EnumBinding
   {
      public EnumBindingImpl (Schema.Define def, Class<?> tgtType,
                              ArrayList<Symbol> syms)
      {
         this.def = def;
         this.tgtType = tgtType;
         this.syms = syms;
      }
      
      @Override public GroupBinding toGroup () { return null; }
      @Override public EnumBinding toEnum () { return this; }
      @Override public Schema.Define getEnum () { return def; }
      @Override public Class<?> getTargetType () { return tgtType; }
      @Override public List<Symbol> getSymbols () { return syms; }
      
      @Override
      public Iterator<Symbol> iterator ()
      {
         return syms.iterator ();
      }
      
      private final Schema.Define def;
      private final Class<?> tgtType;
      private final ArrayList<Symbol> syms;
   }

   private static class FieldImpl implements Field
   {
      public FieldImpl (Schema.Field field, Schema.TypeInfo fieldType,
                        Method getter, Method setter, Method predicate,
                        Binding compBinding)
      {
         this.field = field;
         this.fieldType = fieldType;
         this.getter = getter;
         this.setter = setter;
         this.predicate = predicate;
         this.compBinding = compBinding;
      }

      @Override public Schema.Field getField () { return field; }
      @Override public Schema.TypeInfo getFieldType () { return fieldType; }
      @Override public Method getGetter () { return getter; }
      @Override public Method getSetter () { return setter; }
      @Override public Method getPredicate () { return predicate; }
      @Override public Binding getComponent () { return compBinding; }

      private final Schema.Field field;
      private final Schema.TypeInfo fieldType;
      private final Method getter;
      private final Method setter;
      private final Method predicate;
      private final Binding compBinding;
   }

   private static class SymbolImpl implements Symbol
   {
      public SymbolImpl (Schema.Symbol sym, String tgtName)
      {
         this.sym = sym;
         this.tgtName = tgtName;
      }

      @Override public Schema.Symbol getSymbol () { return sym; }
      @Override public String getTargetName () { return tgtName; }

      private final Schema.Symbol sym;
      private final String tgtName;
   }
   
   private GroupBinding createGroupBinding (Schema.Group g)
      throws BlinkException.Binding
   {
      NsName name = g.getName ();
      
      GroupBinding b = grpBndByName.get (name);
      if (b != null)
         return b;

      Class<?> tgtType = findMatchingClass (g.getName (), g);
      
      if (tgtType != null)
         return createGroupBinding (g, tgtType);
      else
         return null;
   }

   private Class<?> findMatchingClass (NsName name, Schema.Component comp)
   {
      Class<?> c = null;
      
      if (wrapper != null)
         // <wrapper>$<name|alias>
         c = getIncludedClass (wrapper.getName () + "$", name, comp);
      
      if (c == null)
      {
         if (name.isQualified ())
         {
            String ns = name.getNs ();
            String nsPkg = pkgByNs.get (ns);
            if (nsPkg != null)
            {
               // <package[ns]>.<ns>$<name|alias>
               c = getIncludedClass (nsPkg + "." + Util.escName (ns) + "$",
                                     name, comp);
               if (c == null)
                  // <package[ns]>.<name|alias>
                  c = getIncludedClass (nsPkg + ".", name, comp);
            }

            if (c == null)
            {
               nsPkg = Util.escName (splitCamelbackLower (ns));
               if (Util.isSet (pkg))
               {
                  // <package>.<ns>.<name|alias>
                  c = getIncludedClass (pkg + "." + nsPkg + ".",  name, comp);

                  if (c == null)
                     // <package>.<ns>$<name|alias>
                     c = getIncludedClass (pkg + "." + Util.escName (ns) + "$",
                                           name, comp);
                  
               }
               else
               {
                  // <ns>.<name|alias>
                  c = getIncludedClass (nsPkg + ".", name, comp);
                  
                  if (c == null)
                     // <ns>$<name|alias>
                     c = getIncludedClass (Util.escName (ns) + "$", name, comp);
               }
            }
         }
         else
         {
            if (Util.isSet (pkg))
               // <package>.<name|alias>
               c = getIncludedClass (pkg + ".", name, comp);
            else
               // <name|alias>
               c = getIncludedClass ("", name, comp);
         }
      }

      return c;
   }

   private final static NsName BlinkAlias = NsName.get ("blink", "alias");
   
   private Class<?> getIncludedClass (String stem, NsName name,
                                      Schema.Component comp)
   {
      Class<?> c = getIncludedClass (stem + Util.escName (name.getName ()));
      if (c == null)
      {
         String alias = comp.getAnnot (BlinkAlias);
         if (alias != null)
            c = getIncludedClass (stem + Util.escName (alias));
      }
      return c;
   }

   private Class<?> getIncludedClass (String name)
   {
      try
      {
         Class<?> c = Class.forName (name);
         if (isIncludedClass (c))
            return c;
      }
      catch (ClassNotFoundException e)
      {
      }

      return null;
   }

   private static enum MethodType { Getter, Setter, DecGetter, DecSetter }
   
   private static Method findMethod (HashMap<String, Method> allMethods,
                                     String prefix, String name,
                                     Schema.Component comp, MethodType mtype)
   {
      Method m = findMethod (allMethods, prefix, name, mtype);
      if (m == null)
      {
         String alias = comp.getAnnot (BlinkAlias);
         if (alias != null)
            m = findMethod (allMethods, prefix, alias, mtype);
      }
      return m;
   }

   private static Method findMethod (HashMap<String, Method> allMethods,
                                     String prefix, String name,
                                     MethodType mtype)
   {
      Method m = allMethods.get (prefix + name);
      if (m == null || ! signatureMatchesType (m, mtype))
      {
         m = allMethods.get (prefix + "_" + name);
         if (m == null || ! signatureMatchesType (m, mtype))
         {
            m = allMethods.get (prefix + Util.capitalize (name));
            if (m != null && ! signatureMatchesType (m, mtype))
               m = null;
         }
      }

      return m;
   }

   private static boolean signatureMatchesType (Method m, MethodType mtype)
   {
      Class<?> [] prms = m.getParameterTypes ();
      int len = prms.length;
      switch (mtype)
      {
       case Getter:
          return m.getReturnType () != void.class && len == 0;
       case Setter:
          return m.getReturnType () == void.class && len == 1;
       case DecGetter:
          return (m.getReturnType () != void.class && len == 0) ||
             (m.getReturnType () == void.class && len == 1 && prms [0] ==
              DecimalResult.class);
       case DecSetter:
          return m.getReturnType () == void.class &&
             (len == 1 || (len == 2 && prms [0] == long.class &&
                           prms [1] == int.class));
       default:
          return false;
      }
   }

   private EnumBinding createEnumBinding (Schema.Define d)
      throws BlinkException.Binding
   {
      EnumBinding b = enumBndByName.get (d.getName ());
      if (b != null)
         return b;
      
      Class<?> tgtType = findMatchingClass (d.getName (), d);
      if (tgtType != null && tgtType.isEnum ())
         return createEnumBinding (d, tgtType);
      else
         return null;
   }

   private GroupBinding createGroupBinding (Schema.Group g, Class<?> tgtType)
      throws BlinkException.Binding
   {
      // Must add binding to map early to allow circular references
      ArrayList<Field> bindingFields = new ArrayList<Field> ();
      GroupBinding b = new GroupBindingImpl (g, tgtType, bindingFields);
      grpBndByName.put (g.getName (), b);
      grpBndByClass.put (tgtType, b);
      
      HashMap<String, Method> allMethods = new HashMap<String, Method> ();
      getAllMethods (tgtType, allMethods);
      mapFields (g, allMethods, bindingFields);
      return b;
   }

   private EnumBinding createEnumBinding (Schema.Define d, Class<?> tgtType)
      throws BlinkException.Binding
   {
      ArrayList<Symbol> syms = new ArrayList<Symbol> ();

      for (Schema.Symbol s : d.getType ().toEnum ())
         syms.add (new SymbolImpl (s, findEnumConstant (tgtType, s)));
      
      EnumBinding b = new EnumBindingImpl (d, tgtType, syms);
      enumBndByName.put (d.getName (), b);
      return b;
   }

   private static String findEnumConstant (Class<?> c, Schema.Symbol s)
   {
      String name = s.getName ();
      String tgt = findEnumConstant (c, name);
      if (tgt == null)
      {
         String alias = s.getAnnot (BlinkAlias);
         if (alias != null)
            tgt = findEnumConstant (c, alias);
         if (tgt == null)
         {
            tgt = findEnumConstant (c, name.toUpperCase ());
            if (tgt == null)
            {
               if (alias != null)
                  tgt = findEnumConstant (c, alias.toUpperCase ());
               if (tgt == null)
               {
                  tgt = findEnumConstant (c, splitCamelbackUpper (name));
                  if (tgt == null && alias != null)
                     tgt = findEnumConstant (c, splitCamelbackUpper (alias));
               }
            }
         }
      }

      return tgt;
   }

   private static String splitCamelbackUpper (String s)
   {
      return Util.splitCamelback (s, "_").toUpperCase ();
   }
   
   private static String splitCamelbackLower (String s)
   {
      return Util.splitCamelback (s, "_").toLowerCase ();
   }
   
   private static String findEnumConstant (Class<?> c, String name)
   {
      try
      {
         name = Util.escName (name);
         if (c.getDeclaredField (name).isEnumConstant ())
            return name;
      }
      catch (NoSuchFieldException e)
      {
      }
      
      return null;
   }

   private void mapFields (Schema.Group g, HashMap<String, Method> allMethods,
                           ArrayList<Field> bindingFields)
      throws BlinkException.Binding
   {
      if (g.hasSuper ())
         mapFields (g.getSuperGroup (), allMethods, bindingFields);

      for (Schema.Field f : g)
      {
         Schema.TypeInfo t = schema.resolve (f.getType ());

         String name = Util.escMethodName (f.getName ());
         Method getter = null;
         Method setter = null;
         Method predicate = null;

         if (isDecimal (t))
         {
            getter = findMethod (allMethods, "get", name, f,
                                 MethodType.DecGetter);
            setter = findMethod (allMethods, "set", name, f,
                                 MethodType.DecSetter);
         }
         else
         {
            getter = findMethod (allMethods, "get", name, f, MethodType.Getter);
            setter = findMethod (allMethods, "set", name, f, MethodType.Setter);
         }
         
         if (f.isOptional ())
            predicate = findMethod (allMethods, "has", name, f,
                                    MethodType.Getter);
         
         Binding comp = null;
         if (t.isGroup ())
            comp = createGroupBinding (t.getGroup ());
         else
         {
            if (t.isEnum ())
               comp = createEnumBinding (t.getEnum ());
         }
         
         Field bf = new FieldImpl (f, t, getter, setter, predicate, comp);
         bindingFields.add (bf);
      }
   }

   private static boolean isDecimal (Schema.TypeInfo t)
   {
      return t.isPrimitive () &&
         t.getType ().getCode () == Schema.TypeCode.Decimal;
   }

   private void getAllMethods (Class <?> c, HashMap<String, Method> all)
   {
      for (Method m : c.getDeclaredMethods ())
         if (isIncludedMethod (m))
            all.put (m.getName (), m);

      Class<?> s = c.getSuperclass ();
      if (s != null)
         getAllMethods (s, all);
   }
   
   private BlinkException.Binding noBindingError (long tid)
   {
      Schema.Group g = unboundById.get (tid);
      if (g == null)
         return new BlinkException.Binding (
            String.format ("Unknown type id in blink message: %d", tid));
      else
         return noBindingError (g);
   }

   private BlinkException.Binding ambiguousTypeIdError (Schema.Group g)
   {
      Schema.Group other = grpBndById.get (g.getId ()).getGroup ();
      String id = Util.toU64Str (g.getId ().longValue ());
      String msg =
         String.format ("Ambiguous type identifier:%n  %s: %s/%s%n  %s: %s/%s",
                        g.getLocation (), g.getName (), id,
                        other.getLocation (), other.getName (), id);
      return new BlinkException.Binding (msg);
   }

   private BlinkException.Binding noBindingError (NsName name)
   {
      Schema.Group g = unboundByName.get (name);
      if (g == null)
         return new BlinkException.Binding (
            String.format ("No such blink type: %s", name));
      else
         return noBindingError (g);
   }

   private BlinkException.Binding noBindingError (Schema.Group g)
   {
      return new BlinkException.Binding (
         String.format ("No Java class found for Blink type %s", g.getName ()),
         g.getLocation ());
   }

   private BlinkException.Binding noBindingError (Class<?> cl)
   {
      return new BlinkException.Binding (
         String.format ("No matching blink group found when encoding object" +
                        " of Java class %s", cl.getName ()));
   }

   private final Object monitor = new Object ();
   private final HashMap<Long, GroupBinding> grpBndById =
      new HashMap<Long, GroupBinding> ();
   private final HashMap<NsName, GroupBinding> grpBndByName =
      new HashMap<NsName, GroupBinding> ();
   private final HashMap<NsName, EnumBinding> enumBndByName =
      new HashMap<NsName, EnumBinding> ();
   private final HashMap<Class<?>, GroupBinding> grpBndByClass =
      new HashMap<Class<?>, GroupBinding> ();
   private final HashMap<Long, Schema.Group> unboundById =
      new HashMap<Long, Schema.Group> ();
   private final HashMap<NsName, Schema.Group> unboundByName =
      new HashMap<NsName, Schema.Group> ();
   private final HashMap<String, String> pkgByNs =
      new HashMap<String, String> ();
   private final Schema schema;
   private String pkg;
   private Class<?> wrapper;
   private boolean initialized;
   private Class<? extends java.lang.annotation.Annotation> inclusiveClassAnnot;
   private Class<? extends java.lang.annotation.Annotation>
                   exclusiveClassAnnot = NoBlink.class;
   private Class<? extends java.lang.annotation.Annotation>
                   inclusiveMethodAnnot;
   private Class<? extends java.lang.annotation.Annotation>
                   exclusiveMethodAnnot = NoBlink.class;
}
