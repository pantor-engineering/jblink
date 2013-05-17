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

/**
   The {@code Annotated} interface is implemeneted by any schema
   component that can have annotations.
 */

public interface Annotated
{
   /**
      Returns a set of annotations

      @return a set of annotations, or {@code null} if no annotations
      has been set.
    */
   
   AnnotSet getAnnotations ();

   /**
      Returns {@code true} if at least one annotation is present

      @return {@code true} if at least one annotation is present
   */
      
   boolean isAnnotated ();

   /**
      Adds the specified annotation to the set of annotations. It
      replaces any annotation in the set that has the same name.
      
      @param a the annotation to add
      @return any replaced annotation, otherwise {@code null}
   */
   
   Annotation addAnnot (Annotation a);

   /**
      Sets the annotation specified by the name to the specified value. If
      there already is an annotation with the same name in the set, it will
      be replaced.

      @param n the name of the annotation
      @param v the value
      @return any replaced annotation, otherwise {@code null}
    */
   
   Annotation setAnnot (NsName n, String v);

   /**
      Sets the annotation specified by the name to the specified value.
      Same as {@code setAnnot (NsName.get (n), v)}

      @param n the name of the annotation
      @param v the value
      @return any replaced annotation, otherwise {@code null}
    */
   
   Annotation setAnnot (String n, String v);

   /**
      Sets the annotation specified by the namespace and name to the
      specified value. Same as {@code setAnnot (NsName.get (ns, n), v)}

      @param ns the namespace part of the name
      @param n the name of the annotation
      @param v the value
      @return any replaced annotation, otherwise {@code null}
    */
   
   Annotation setAnnot (String ns, String n, String v);

   /**
      Returns the value of the annotation with the specified name.

      @param n the name of the annotation
      @return the annotation value if present, otherwise {@code null}
    */
   
   String getAnnot (NsName n);

   /**
      Returns the value of the annotation with the specified name.
      Same as {@code getAnnot (NsName.get (n))}

      @param n the name of the annotation
      @return the annotation value if present, otherwise {@code null}
    */
   
   String getAnnot (String n);

   /**
      Returns the value of the annotation with the specified namespace and name.
      Same as {@code getAnnot (NsName.get (ns, n))}

      @param ns the namespace part of the name
      @param n the name of the annotation
      @return the annotation value if present, otherwise {@code null}
    */
   
   String getAnnot (String ns, String n);

   /**
      Adds all annotations of the specified set to the set of this
      component. Any preexisting annotations will be replaced if also
      part of the added set.

      @param a the set of annotations to add
    */
     
   void addAnnotations (AnnotSet a);
}
