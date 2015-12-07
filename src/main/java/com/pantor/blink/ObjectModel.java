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
import java.util.List;
import java.lang.reflect.Method;
import java.io.IOException;

/**
   The {@code ObjectModel} interface provides methods for mapping a
   class to a blink schema group, a binding, and in the reverse
   direction. It also provides a method for looking up a binding based
   in a numerical type identifier.
*/

public interface ObjectModel extends Dependee
{
   /**
      Returns a binding for the specified type identifier

      @param tid a type identifier
      @return a group binding that corresponds to the type identifier
      @throws BlinkException if no binding was found or if there was
      another binding problem
   */
   
   GroupBinding getGroupBinding (long tid) throws BlinkException;

   /**
      Returns a binding for the specified class

      @param cl a class
      @return a group binding that corresponds to the specified class
      @throws BlinkException if no binding was found or if there was
      another binding problem
   */
   
   GroupBinding getGroupBinding (Class<?> cl) throws BlinkException;

   /**
      Returns a binding for the specified name of a group in a Blink schema

      @param name a group name
      @return a group binding that corresponds to the specified group
      @throws BlinkException if no binding was found or if there was
      another binding problem
   */
   
   GroupBinding getGroupBinding (NsName name) throws BlinkException;

   /**
      Returns a binding for the specified name of an enum in a Blink schema

      @param name an enum name
      @return a enum binding that corresponds to the specified Blink enum
      @throws BlinkException if no binding was found or if there was
      another binding problem
   */

   EnumBinding getEnumBinding (NsName name) throws BlinkException;

   /**
      Loads builtin schemas for schema exchange
    */
   
   void loadBuiltinSchemas () throws IOException, BlinkException;

   /**
      Returns the schema used by this object model

      @return the schema used by this object model
      @throws BlinkException if there was a schema problem
    */
   
   Schema getSchema () throws BlinkException;
   
   public static interface Binding extends Dependee, Located
   {
      GroupBinding toGroup ();
      EnumBinding toEnum ();
      Class<?> getTargetType ();
   }
   
   public static interface GroupBinding extends Binding, Iterable<Field>
   {
      Schema.Group getGroup ();
      long getCompactTypeId ();
      List<Field> getFields ();
      Iterator<Field> iterator ();
   }

   public static interface Field extends Located
   {
      Schema.Field getField ();
      Schema.TypeInfo getFieldType ();
      Method getSetter ();
      Method getGetter ();
      Method getPredicate ();
      Binding getComponent ();
   }

   public static interface EnumBinding extends Binding, Iterable<Symbol>
   {
      Schema.Define getEnum ();
      List<Symbol> getSymbols ();
      Iterator<Symbol> iterator ();
   }

   public static interface Symbol
   {
      Schema.Symbol getSymbol ();
      String getTargetName ();
   }
}
