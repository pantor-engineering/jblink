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

import java.util.Iterator;
import java.util.TreeMap;

/**
   This class represents a set of annotations
 */

public final class AnnotSet implements Iterable<Annotation>
{
   /**
      Creates an empty set of annotations
    */
   
   public AnnotSet ()
   {
   }

   /**
      Creates a set that is a copy of the specified annotation set

      @param other a set of annotations to copy
    */
   
   public AnnotSet (AnnotSet other)
   {
      add (other);
   }

   /**
      Adds the specified annotation to the set. It replaces any
      annotation in the set that has the same name.
      
      @param a the annotation to add
      @return any replaced annotation, otherwise {@code null}
   */
   
   public Annotation add (Annotation a)
   {
      return map.put (a.getName (), a);
   }

   /**
      Sets the annotation specified by the name to the specified value. If
      there already is an annotation with the same name in the set, it will
      be replaced.

      @param n the name of the annotation
      @param v the value
      @return any replaced annotation, otherwise {@code null}
    */

   public Annotation set (NsName n, String v)
   {
      return add (new Annotation (n, v));
   }
   
   /**
      Sets the annotation specified by the name to the specified value.
      Same as {@code set (NsName.get (n), v)}

      @param n the name of the annotation
      @param v the value
      @return any replaced annotation, otherwise {@code null}
    */
   
   public Annotation set (String n, String v)
   {
      return set (NsName.get (n), v);
   }

   /**
      Sets the annotation specified by the namespace and name to the
      specified value. Same as {@code set (NsName.get (ns, n), v)}

      @param ns the namespace part of the name
      @param n the name of the annotation
      @param v the value
      @return any replaced annotation, otherwise {@code null}
    */
   
   public Annotation set (String ns, String n, String v)
   {
      return set (NsName.get (ns, n), v);
   }

   /**
      Returns the value of the annotation with the specified name.

      @param n the name of the annotation
      @return the annotation value if present, otherwise {@code null}
    */
   
   public String get (NsName n)
   {
      Annotation a = map.get (n);
      if (a != null)
         return a.getValue ();
      else
         return null;
   }
   
   /**
      Returns the value of the annotation with the specified name.
      Same as {@code get (NsName.get (n))}

      @param n the name of the annotation
      @return the annotation value if present, otherwise {@code null}
    */
   
   public String get (String n)
   {
      return get (NsName.get (n));
   }

   /**
      Returns the value of the annotation with the specified namespace and name.
      Same as {@code getAnnot (NsName.get (ns, n))}

      @param ns the namespace part of the name
      @param n the name of the annotation
      @return the annotation value if present, otherwise {@code null}
    */
   
   public String get (String ns, String n)
   {
      return get (NsName.get (ns, n));
   }

   /**
      Adds all annotations of the specified set to this set. Any
      preexisting annotations will be replaced if also part of the
      added set.

      @param other a the set of annotations to add
    */
   
   public void add (AnnotSet other)
   {
      map.putAll (other.map);
   }

   /**
      Returns {@code true} if the set contains an annotation with the
      specified name

      @return {@code true} if the annotation exists
    */

   public boolean has (NsName n)
   {
      return map.containsKey (n);
   }

   /**
      Returns {@code true} if the set contains an annotation with the
      specified name

      @return {@code true} if the annotation exists
    */

   public boolean has (String n)
   {
      return has (NsName.get (n));
   }

   /**
      Returns {@code true} if the set contains an annotation with the
      specified name and namespace

      @return {@code true} if the annotation exists
    */

   public boolean has (String ns, String n)
   {
      return has (NsName.get (ns, n));
   }

   /**
      Returns an iterator over the set of annotations

      @return an annotation iterator
    */
   
   @Override
   public Iterator<Annotation> iterator ()
   {
      return map.values ().iterator ();
   }

   /**
      Returns {@code true} if there are no annotations in the set

      @return {@code true} if the set is empty
   */
   
   public boolean empty ()
   {
      return map.isEmpty ();
   }

   /** Removes all annotations from this set */
   
   public void clear ()
   {
      map.clear ();
   }

   /** Returns the number of annotations in this set */

   public int size ()
   {
      return map.size ();
   }
   
   private final TreeMap<NsName, Annotation> map =
      new TreeMap<NsName, Annotation> ();
}
