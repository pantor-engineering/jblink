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

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class TestCases
{
   @Test public void vlcRead () throws BlinkException
   {
      assertEquals ((byte)-17, Vlc.readI8  (toBuf ("6f")));
      assertEquals ((short)-4711, Vlc.readI16 (toBuf ("99 b6")));
      assertEquals (-100000, Vlc.readI32 (toBuf ("c3 60 79 fe")));
      assertEquals (-1111111111111L,
                    Vlc.readI64 (toBuf ("c6 39 ee 9d 4c fd fe")));

      assertEquals ((byte)17, Vlc.readU8  (toBuf ("11")));
      assertEquals ((short)4711, Vlc.readU16 (toBuf ("a7 49")));
      assertEquals (100000, Vlc.readU32 (toBuf ("c3 a0 86 01")));
      assertEquals (1111111111111L,
                    Vlc.readU64 (toBuf ("c6 c7 11 62 b3 02 01")));
      
      assertEquals ((byte)-128, Vlc.readI8  (toBuf ("80 fe")));
      assertEquals ((byte)127, Vlc.readI8  (toBuf ("bf 01")));

      assertEquals ((short)-32768, Vlc.readI16 (toBuf ("c2 00 80")));
      assertEquals ((short)32767, Vlc.readI16 (toBuf ("c2 ff 7f")));

      assertEquals (-2147483648, Vlc.readI32 (toBuf ("c4 00 00 00 80")));
      assertEquals (2147483647, Vlc.readI32 (toBuf ("c4 ff ff ff 7f")));

      assertEquals (Vlc.readI64 (toBuf ("c8 00 00 00 00 00 00 00 80")),
                    -9223372036854775808L);
      assertEquals (Vlc.readI64 (toBuf ("c8 ff ff ff ff ff ff ff 7f")),
                    9223372036854775807L);
   }

   @Test public void vlcSign () throws BlinkException
   {
      assertEquals ((byte)   64,   Vlc.readU8  (toBuf ("40")));
      assertEquals ((byte)  -64,   Vlc.readI8  (toBuf ("40")));
      assertEquals ((byte)   128,  Vlc.readU8  (toBuf ("c1 80")));
      assertEquals ((byte)  -128,  Vlc.readI8  (toBuf ("c1 80")));
      assertEquals ((byte)   64,   Vlc.readU8  (toBuf ("80 01")));
      assertEquals ((byte)   64,   Vlc.readI8  (toBuf ("80 01")));
      assertEquals ((short)  8192, Vlc.readU16 (toBuf ("80 80")));
      assertEquals ((short) -8192, Vlc.readI16 (toBuf ("80 80")));
      assertEquals ((short)  8192, Vlc.readU16 (toBuf ("c2 00 20")));
      assertEquals ((short)  8192, Vlc.readI16 (toBuf ("c2 00 20")));
      assertEquals ((int)    128,  Vlc.readU32 (toBuf ("c1 80")));
      assertEquals ((int)   -128,  Vlc.readI32 (toBuf ("c1 80")));
   }

   @Test public void vlcRoundtrip () throws BlinkException
   {
      roundtrip ((byte)0);

      roundtrip ((byte)64);
      roundtrip ((byte)-64);

      roundtrip ((byte)127);
      roundtrip ((byte)-128);

      roundtrip ((short)127);
      roundtrip ((short)-128);
      roundtrip ((short)8192);
      roundtrip ((short)-8192);
      roundtrip ((short)32767);
      roundtrip ((short)-32768);

      roundtrip ((int)127);
      roundtrip ((int)-128);
      roundtrip ((int)32767);
      roundtrip ((int)-32768);
      roundtrip ((int)2147483647);
      roundtrip ((int)-2147483648);

      roundtrip ((long)127);
      roundtrip ((long)-128);
      roundtrip ((long)32767);
      roundtrip ((long)-32768);
      roundtrip ((long)2147483647);
      roundtrip ((long)-2147483648);
      roundtrip ((long)9223372036854775807L);
      roundtrip ((long)-9223372036854775808L);
   }

   @Test public void readSimpleSchema ()
      throws BlinkException, IOException
   {
      Schema s = toSchema ("Foo -> string Bar");
      Schema.Group g = s.getGroup ("Foo");
      assertNotNull (g);
      Schema.Field f = g.getField ("Bar");
      assertNotNull (f);
      assertNotNull (f.getType ());
      assertEquals (Schema.TypeCode.String, f.getType ().getCode ());
      assertEquals ("782f6f9db8677919", Long.toString (g.getTypeId (), 16));
   }

   @Test public void schemaStrings ()
      throws BlinkException, IOException
   {
      schemaRoundtrip ("@x:y='z' Foo -> string @v:s='t' Bar, i32 [] Baz?");
      schemaRoundtrip ("Foo = Bar* Bar");
      schemaRoundtrip ("Colors = @rgb='ff0000' Red/0 | " +
                       "@rgb='00ff00' Green/1 | @rgb='0000ff' Blue/2");
   }

   public static class Foo
   {
      public int getBar () { return bar; }
      public void setBar (int val) { bar = val; }
      public String getBaz () { return baz; }
      public void setBaz (String val) { baz = val; }

      private int bar;
      private String baz;
   }
    
   @Test public void simpleCompactDecode ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Foo|Bar=17
      
      decodeCompact ("Foo/1 -> u32 Bar", "02 01 11", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Foo);
      Foo f = (Foo)result.getObjects ().get (0);
      assertEquals (17, f.getBar ());
   }

   @Test public void simpleCompactEncode ()
      throws BlinkException, IOException
   {
      Foo foo = new Foo ();
      foo.setBar (17);
      foo.setBaz ("Hello");
      assertEquals ("88 00 01 11 05 48 65 6c 6c 6f",
                    encodeCompact ("Foo/1 -> u32 Bar, string Baz", foo));
   }

   @Test public void simpleCompactRoundtrip ()
      throws BlinkException, IOException
   {
      Foo foo = new Foo ();
      foo.setBar (17);
      foo.setBaz ("Hello");
      Foo result = (Foo)compactRoundtrip ("Foo/1 -> u32 Bar, string Baz", foo);
      assertEquals (17, result.getBar ());
      assertEquals ("Hello", result.getBaz ());
   }

   @Test public void simpleCompactRoundtripDefaultTypeId ()
      throws BlinkException, IOException
   {
      Foo foo = new Foo ();
      foo.setBar (17);
      foo.setBaz ("Hello");
      Foo result = (Foo)compactRoundtrip ("Foo -> u32 Bar, string Baz", foo);
      assertEquals (17, result.getBar ());
      assertEquals ("Hello", result.getBaz ());
   }

   @Test public void stringCompactRoundtrip ()
      throws BlinkException, IOException
   {
      ObjectModel om = toModel ("Foo/1 -> string Baz");
      Foo foo = new Foo ();

      foo.setBaz ("räksmörgås");
      assertEquals (((Foo)compactRoundtrip (om, foo)).getBaz (), foo.getBaz ());

      foo.setBaz ("ööööööööööööööööööööööööööööööööööööööööööööööö" +
                  "ööööööööööööööööööööööööööööööööööööööööööööööö" +
                  "ööööööööööööööööööööööööööööööööö");
      assertEquals (((Foo)compactRoundtrip (om, foo)).getBaz (), foo.getBaz ());

      foo.setBaz ("ööööööööööööööööööööööööööööööööööööööööööööööö" +
                  "ööööööööööööööööööööööööööööööööööööööööööööööö" +
                  "ööööööööööööööööööööööööööööööööööööööööööööööö" +
                  "ööööööööööööööööööööööööööööööööööööööööööööööö" +
                  "ööööööööööööööööööööööööööööööööööööööööööööööö");
      assertEquals (((Foo)compactRoundtrip (om, foo)).getBaz (), foo.getBaz ());
   }

   @Test public void largeMsgCompactRoundtrip ()
      throws BlinkException, IOException
   {
      ObjectModel om = toModel ("Foo/1 -> string Baz");
      Foo foo = new Foo ();

      char [] chars = new char [0x100000];
      java.util.Arrays.fill (chars, 'x');
      String s = new String (chars);
      
      foo.setBaz (s);
      assertEquals (((Foo)compactRoundtrip (om, foo)).getBaz (), foo.getBaz ());
   }

   @Test public void compactDecode1 ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Foo|Bar=17|Baz=Hello
      
      decodeCompact ("Foo/1 -> u32 Bar, string Baz",
                     "08 01 11 05 48 65 6c 6c 6f", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Foo);
      Foo f = (Foo)result.getObjects ().get (0);
      assertEquals (17, f.getBar ());
      assertEquals ("Hello", f.getBaz ());
   }

   public static class Baz
   {
      public int getFoo () { return foo; }
      public void setFoo (int val) { foo = val; has_Foo = true; }
      public String getBar () { return bar; }
      public void setBar (String val) { bar = val; }
      public boolean hasFoo () { return has_Foo; }

      private boolean has_Foo;
      private int foo;
      private String bar;
   }

   @Test public void compactDecode2 ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Baz|Foo=17|Bar=Hello
      
      decodeCompact ("Baz/1 -> u32 Foo?, string Bar",
                     "08 01 11 05 48 65 6c 6c 6f", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Baz);
      Baz f = (Baz)result.getObjects ().get (0);
      assertTrue (f.hasFoo ());
      assertEquals (17, f.getFoo ());
      assertEquals ("Hello", f.getBar ());
   }

   private final static String ShapeSchema = 
      "Shape -> " +
      "  string Descr? " +

      "Point -> " +
      "  u32 X, u32 Y " +

      "Rect/1 : Shape -> " +
      "  Point Pos, u32 Width, u32 Height " +

      "Circle/2 : Shape -> " +
      "  u32 Radius " +

      "Polygon/3 : Shape -> " +
      "  Point [] Points " +

      "Canvas/4 -> " +
      "  Shape* [] Shapes " +

      "Transform/5 -> " +
      "  i32 [] Matrix ";
      

   public static class Shape
   {
      public String getDescr () { return descr; }
      public void setDescr (String d) { descr = d; }
      public boolean hasDescr () { return descr != null; }
      private String descr;
   }

   public static class Point
   {
      public int getX () { return x; }
      public int getY () { return y; }
      public void setX (int v) { x = v; }
      public void setY (int v) { y = v; }
      private int x;
      private int y;
   }

   public static class Rect extends Shape
   {
      public Point getPos () { return pos; }
      public void setPos (Point v) { pos = v; }
      public int getWidth () { return width; }
      public void setWidth (int v) { width = v; }
      public int getHeight () { return height; }
      public void setHeight (int v) { height = v; }
      private Point pos;
      private int width;
      private int height;
   }

   public static class Canvas
   {
      public Shape [] getShapes () { return shapes; }
      public void setShapes (Shape [] v) { shapes = v; }
      private Shape [] shapes;
   }

   public static class Polygon extends Shape
   {
      public Point [] getPoints () { return points; }
      public void setPoints (Point [] v) { points = v; }
      private Point [] points;
   }

   public static class Circle extends Shape
   {
      public int getRadius () { return radius; }
      public void setRadius (int v) { radius = v; }
      private int radius;
   }

   public static class Transform
   {
      public int [] getMatrix () { return matrix; }
      public void setMatrix (int [] v) { matrix = v; }
      private int [] matrix;
   }
   
   @Test public void compactDecode3 ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Rect|Pos={X=1|Y=2}|Width=10|Height=20|Descr=Test

      decodeCompact (ShapeSchema,
                     "0a 01 04 54 65 73 74 01 02 0a 14", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Rect);
      Rect r = (Rect)result.getObjects ().get (0);
      assertTrue (r.hasDescr ());
      assertEquals ("Test", r.getDescr ());
      assertNotNull (r.getPos ());
      assertEquals (1, r.getPos ().getX ());
      assertEquals (2, r.getPos ().getY ());
      assertEquals (10, r.getWidth ());
      assertEquals (20, r.getHeight ());
   }
   
   @Test public void compactDecodeInnerObjects ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Canvas|Shapes=[@Rect|Pos={X=1|Y=2}|Width=1|Height=2;
      //                 @Polygon|Points=[X=1|Y=2;X=17|Y=18]|Descr=Elephant]

      decodeCompact (ShapeSchema,
                     "1b 04 02 86 00 01 c0 01 02 01 02 8f 00 03 08 45" +
                     "6c 65 70 68 61 6e 74 02 01 02 11 12", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Canvas);
      Canvas c = (Canvas)result.getObjects ().get (0);
      Shape [] shapes = c.getShapes ();
      assertNotNull (shapes);
      assertEquals (2, shapes.length);
      assertTrue (shapes [0] instanceof Rect);
      assertTrue (shapes [1] instanceof Polygon);
      assertTrue (shapes [1].hasDescr ());
      Polygon p = (Polygon)shapes [1];
      assertEquals ("Elephant", p.getDescr ());
      assertNotNull (p.getPoints ());
      assertEquals (2, p.getPoints ().length);
      assertEquals (17, p.getPoints () [1].getX ());
   }

   @Test public void compactRoundtripInnerObjects ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Canvas|Shapes=[@Rect|Pos={X=1|Y=2}|Width=1|Height=2;
      //                 @Polygon|Points=[X=1|Y=2;X=17|Y=18]|Descr=Elephant]

      decodeCompact (ShapeSchema,
                     "1b 04 02 86 00 01 c0 01 02 01 02 8f 00 03 08 45" +
                     "6c 65 70 68 61 6e 74 02 01 02 11 12", result);
      
      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Canvas);
      Canvas c = (Canvas)result.getObjects ().get (0);

      Canvas c2 = (Canvas)compactRoundtrip (ShapeSchema, c);
      
      Shape [] shapes = c2.getShapes ();
      assertNotNull (shapes);
      assertEquals (2, shapes.length);
      assertTrue (shapes [0] instanceof Rect);
      assertTrue (shapes [1] instanceof Polygon);
      assertTrue (shapes [1].hasDescr ());
      Polygon p = (Polygon)shapes [1];
      assertEquals ("Elephant", p.getDescr ());
      assertNotNull (p.getPoints ());
      assertEquals (2, p.getPoints ().length);
      assertEquals (17, p.getPoints () [1].getX ());
   }

   
   @Test public void compactDecodePrimitiveArray ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Transform|Matrix=[1;2;3;4;5;6]

      decodeCompact (ShapeSchema, "08 05 06 01 02 03 04 05 06", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Transform);
      Transform t = (Transform)result.getObjects ().get (0);
      int [] matrix = t.getMatrix ();
      assertNotNull (matrix);
      assertEquals (6, matrix.length);
      assertEquals (1, matrix [0]);
      assertEquals (6, matrix [5]);
   }

   @Test public void compactRoundtripPrimitiveArray ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Transform|Matrix=[1;2;3;4;5;6]

      decodeCompact (ShapeSchema, "08 05 06 01 02 03 04 05 06", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Transform);
      Transform t = (Transform)result.getObjects ().get (0);

      Transform t2 = (Transform)compactRoundtrip (ShapeSchema, t);
      
      int [] matrix = t2.getMatrix ();
      assertNotNull (matrix);
      assertEquals (6, matrix.length);
      assertEquals (1, matrix [0]);
      assertEquals (6, matrix [5]);
   }

   public static class Car
   {
      public void setColor (Color v) { color = v; }
      public Color getColor () { return color; }
      private Color color;
   }

   public static enum Color { Red, Green, Blue }
   
   @Test public void compactDecodeEnum ()
      throws BlinkException, IOException
   {
      DefaultBlock result = new DefaultBlock ();

      // @Car|Color=Blue

      decodeCompact ("Car/1 -> Color Color " +
                     "Color = Red | Green | Blue",
                     "02 01 02", result);

      assertEquals (1, result.size ());
      assertTrue (result.getObjects ().get (0) instanceof Car);
      Car c = (Car)result.getObjects ().get (0);
      assertEquals (Color.Blue, c.getColor ());
   }
   
   @Test public void compactRoundtripEnum ()
      throws BlinkException, IOException
   {
      Car car = new Car ();
      car.setColor (Color.Blue);
      Car result = (Car)compactRoundtrip ("Car/1 -> Color Color " +
                                          "Color = Red | Green | Blue", car);
      assertEquals (Color.Blue, result.getColor ());
   }

   public static interface Bar
   {
      String get ();
   }

   @Test public void dynClass () throws Exception
   {
      DynClass c = new DynClass ("com.pantor.blink.test.Foo");
      c.addInterface ("com.pantor.blink.TestCases$Bar");
      c.addDefaultConstructor ();
      c.startPublicMethod ("get", "()Ljava/lang/String;")
         .ldc ("Hello").areturn ().setMaxStack (1).endMethod ();
      DynClassLoader loader = new DynClassLoader ();
      Bar bar = (Bar)loader.load (c).newInstance ();
      assertEquals ("Hello", bar.get ());
   }

   public static class A
   {
      public String getValue () { return value; }
      public void setValue (String v) { value = v; }
      private String value;
   }

   public static class B extends A
   {
      public int getNum () { return num; }
      public void setNum (int v) { num = v; }
      private int num;
   }

   public static class C extends A
   {
      public String getExtra () { return extra; }
      public void setExtra (String v) { extra = v; }
      private String extra;
   }

   public static class D
   {
   }

   public static class MyObs
   {
      public void onA (A a, Schema.Group g)
      {
         result = "onA: " + a.getValue () + " (" + g.getName () + ")";
      }

      public void onB (B a)
      {
         result = "onB: " + a.getNum ();
      }

      public void onAny (Object o, Schema.Group g)
      {
         result = "onAny: (" + g.getName () + ")";
      }

      String result = "";
   }
   
   private final static String TreeSchema =
      "A/1     -> string Value " +
      "B/2 : A -> i32 Num " +
      "C/3 : A -> string Extra " +
      "D/4";
   
   @Test public void dynamicObserver ()
      throws BlinkException, IOException
   {
      ObjectModel om = toModel (TreeSchema);
      DefaultObsRegistry oreg = new DefaultObsRegistry (om);

      MyObs obs = new MyObs ();
      oreg.addObserver (obs);

      A a = new A ();
      a.setValue ("Foo");

      B b = new B ();
      b.setValue ("Bar");
      b.setNum (17);

      C c = new C ();
      c.setValue ("Baz");
      c.setExtra ("Hello");

      obs.result = "";
      compactRoundtrip (om, a, oreg);
      assertEquals ("onA: Foo (A)", obs.result);

      obs.result = "";
      compactRoundtrip (om, b, oreg);
      assertEquals ("onB: 17", obs.result);

      obs.result = "";
      compactRoundtrip (om, c, oreg);
      assertEquals ("onA: Baz (C)", obs.result);

      obs.result = "";
      compactRoundtrip (om, new D (), oreg);
      assertEquals ("onAny: (D)", obs.result);
   }

   @Test public void anonDynObs ()
      throws BlinkException, IOException
   {
      ObjectModel om = toModel ("Foo/1 -> u32 Bar");
      DefaultObsRegistry oreg = new DefaultObsRegistry (om);
      oreg.setLoadMode (DefaultObsRegistry.LoadMode.Privileged);
         ;
      final Foo [] sink = new Foo [1];
      oreg.addObserver (
         new Object() { public void onFoo (Foo foo) { sink [0] = foo; } });

      compactRoundtrip (om, new Foo (), oreg);
      assertTrue (sink [0] instanceof Foo);
   }

   private static class FooObs
   {
      public String result = "";
      public void onFoo (Foo f) { result = "got Foo"; }
   }
   
   @Test public void dynObsReset () throws BlinkException, IOException
   {
      ObjectModel om = toModel ("Foo/1 -> u32 Bar");
      DefaultObsRegistry oreg = new DefaultObsRegistry (om);
      oreg.setLoadMode (DefaultObsRegistry.LoadMode.Privileged);
      FooObs obs = new FooObs ();
      oreg.addObserver (obs);
      oreg.addObserver (obs); // Overrides the previous observer
      compactRoundtrip (om, new Foo (), oreg);
      assertEquals ("got Foo", obs.result);
   }

   public static class Price
   {
      public Price () { }
      public Price (double val) { this.val = val; }
      public double getValue () { return val; }
      public void setValue (double val) { this.val = val; }
      private double val;
   }
   
   @Test public void f64field ()
      throws BlinkException, IOException
   {
      double in = 123.456789;
      Price out = (Price)compactRoundtrip ("Price/1 -> f64 Value",
                                           new Price (in));
      assertEquals (in, out.getValue (), 0 /* Exact match */);
   }

   @Test public void schemaConstraints ()
      throws BlinkException, IOException
   {
      assertInvalid ("Foo = Bar " +
                     "Bar = Foo ",
                     
                     "-:1:19: error: Illegal " +
                     "recursive reference: the type definition Foo " +
                     "directly or indirectly refers to itself");

      assertInvalid ("Foo -> Bar F1 " +
                     "Bar = Foo",

                     "-:1:23: error: Illegal " +
                     "recursive reference: the group definition Foo " +
                     "directly or indirectly refers to itself");

      assertInvalid ("Foo -> Bar F1 " +
                     "Bar : Foo ",

                     "-:1:23: error: Illegal " +
                     "recursive reference: the group definition Foo " +
                     "directly or indirectly refers to itself");

      assertInvalid ("Foo : Bar " +
                     "Bar = string ",

                     "-:1:9: error: Supergroup " +
                     "reference to Bar does not refer to a group definition");

      assertInvalid ("Foo = string " +
                     "Foo = i32 ",

                     "-:1:18: error: Conflicting " +
                     "blink type definition: Foo\n" +
                     "  Previously defined as type here: -:1:5");

      assertInvalid ("Foo\n" +
                     "Foo ",

                     "-:2:4: error: Conflicting " +
                     "blink group definition: Foo\n" +
                     "  Previously defined as group here: -:1:3");

      assertInvalid ("Foo -> u32 x, string x",
                     "-:1:20: error: Duplicate " +
                     "field name in Foo: x");
      
      assertInvalid ("Foo = Bar | Bar",
                     "-:1:15: error: Duplicate " +
                     "symbol name in enum: Bar");

      assertInvalid ("Foo = Bar/1 | Baz/1",
                     "-:1:19: error: Duplicate " +
                     "symbol value in enum: 1");

      assertInvalid ("Foo -> Bar x",
                     "-:1:10: error: No such " +
                     "definition in type reference: Bar");

      assertInvalid ("Foo -> Bar* x " + 
                     "Bar = string",
                     "-:1:11: error: Dynamic " + 
                     "reference to Bar does not refer to a group definition");

      assertInvalid ("Foo -> u32 x " + 
                     "Bar : Foo -> u32 x ",

                     "-:1:29: error: The field " + 
                     "Bar.x shadows a field inherited from Foo\n" +
                     "  Defined here: -:1:10");

      assertInvalid ("Foo\n" + 
                     "Bar = Foo * " + 
                     "Baz : Bar ",

                     "-:2:22: error: Supergroup " + 
                     "reference to Bar must not be dynamic");

      assertInvalid ("Foo\n" + 
                     "Bar = Foo [] " + 
                     "Baz : Bar ",

                     "-:2:23: error: Supergroup " + 
                     "reference Bar must not refer to a sequence");

      assertInvalid ("Foo = string [][]",

                     "-:1:17: error: Expected group " + 
                     "or type definition name, or an incremental annotation " + 
                     "but got '['");

      assertInvalid ("Foo = string [] " + 
                     "Bar = Foo [] ",

                     "-:1:28: error: The sequence " + 
                     "item type Foo must not be a sequence in itself");

      assertInvalid ("Foo = string [] " + 
                     "Bar -> Foo [] x ",

                     "-:1:26: error: The sequence " + 
                     "item type of the field x must not also be a sequence");

      assertValid ("Foo -> Bar* x " + 
                   "Bar = Foo");
      
      assertInvalid ("Foo -> Bar F1? " + 
                     "Bar : Foo ", "-:1:24: error: " + 
                     "Illegal recursive reference: the group definition " + 
                     "Foo directly or indirectly refers to itself");

      assertValid ("Foo -> Bar* F1 " + 
                   "Bar : Foo ");
   }

   public static class Foz
   {
      public void setValue (String val) { this.val = val; }
      public String getValue () { return val; }
      private String val;
   }
   
   @Test public void camelback ()
   {
      assertEquals ("FOO_BAR",
                    Util.splitCamelback ("FooBar", "_").toUpperCase ());
      assertEquals ("FOO_BAR",
                    Util.splitCamelback ("fooBar", "_").toUpperCase ());
      assertEquals ("BAR", Util.splitCamelback ("Bar", "_").toUpperCase ());
      assertEquals ("MY_ID", Util.splitCamelback ("MyID", "_").toUpperCase ());
   } 

   public static class MyObsA
   {
      public void onA (A a, Schema.Group g)
      {
         result = "onA: " + a.getValue () + " (" + g.getName () + ")";
      }

      String result = "";
   }

   public static class MyObsB
   {
      public void onB (B a)
      {
         result = "onB: " + a.getNum ();
      }

      String result = "";
   }
   
   @Test public void combinedOregAnddispatcher ()
      throws BlinkException, IOException
   {
      ObjectModel om = toModel (TreeSchema);
      DefaultObsRegistry oregA = new DefaultObsRegistry (om);
      DefaultObsRegistry oregB = new DefaultObsRegistry (om);

      MyObsA obsA = new MyObsA ();
      oregA.addObserver (obsA);

      MyObsB obsB = new MyObsB ();
      oregB.addObserver (obsB);

      A a = new A ();
      a.setValue ("Foo");

      B b = new B ();
      b.setValue ("Bar");
      b.setNum (17);

      C c = new C ();
      c.setValue ("Baz");
      c.setExtra ("Hello");

      Dispatcher disp = new Dispatcher (om, oregA, oregB);
      
      obsA.result = "";
      obsB.result = "";
      disp.dispatch (a);
      assertEquals ("onA: Foo (A)", obsA.result);
      assertEquals ("", obsB.result);

      obsA.result = "";
      obsB.result = "";
      disp.dispatch (b);
      assertEquals ("", obsA.result);
      assertEquals ("onB: 17", obsB.result);

      obsA.result = "";
      obsB.result = "";
      disp.dispatch (c);
      assertEquals ("onA: Baz (C)", obsA.result);
      assertEquals ("", obsB.result);
   }

   public static class Msg
   {
      public void setHost (byte [] host) { this.host = host; }
      public byte [] getHost () { return host; }

      public void setId (byte [] id) { this.id = id; }
      public byte [] getId () { return id; }
      public boolean hasId () { return id != null; }

      public void setData (byte [] data) { this.data = data; }
      public byte [] getData () { return data; }

      private byte [] host;
      private byte [] id;
      private byte [] data;
   }
   
   @Test public void binaryAndFixed () throws BlinkException, IOException
   {
      String host = "3e 6d 3c ea";
      String data = "01 02 03";

      Msg msg = new Msg ();
      msg.setHost (hexToBytes (host));
      msg.setData (hexToBytes (data));

      Msg result = (Msg)compactRoundtrip (
         "Msg/1 ->" +
         "  fixed (4) Host," +
         "  fixed (16) Id?," +
         "  binary Data",
         msg);

      assertFalse (result.hasId ());
      assertNotNull (result.getHost ());
      assertNotNull (result.getData ());
      assertEquals (host, bytesToHex (result.getHost ()));
      assertEquals (data, bytesToHex (result.getData ()));
   }

   @Test public void binaryAndFixedWithId () throws BlinkException, IOException
   {
      String host = "3e 6d 3c ea";
      String data = "01 02 03";
      String id = "28 85 c0 3d 55 2c 48 75 a5 43 c4 7a 1c d1 64 9a";

      Msg msg = new Msg ();
      msg.setHost (hexToBytes (host));
      msg.setId (hexToBytes (id));
      msg.setData (hexToBytes (data));

      Msg result = (Msg)compactRoundtrip (
         "Msg/1 ->" +
         "  fixed (4) Host," +
         "  fixed (16) Id?," +
         "  binary Data",
         msg);

      assertTrue (result.hasId ());
      assertNotNull (result.getHost ());
      assertNotNull (result.getId ());
      assertNotNull (result.getData ());
      assertEquals (host, bytesToHex (result.getHost ()));
      assertEquals (id, bytesToHex (result.getId ()));
      assertEquals (data, bytesToHex (result.getData ()));
   }   

   //////////////////////////////////////////////////////////////////////
   
   private static void decodeCompact (String schema, String data, Block result)
      throws BlinkException, IOException
   {
      CompactReader rd = new CompactReader (toModel (schema));
      rd.read (toBuf (data), result);
   }   

   private static Object compactRoundtrip (String schema, Object in)
      throws BlinkException, IOException
   {
      return compactRoundtrip (toModel (schema), in);
   }

   private static Object compactRoundtrip (ObjectModel om, Object in)
      throws BlinkException, IOException
   {
      return compactRoundtrip (om, in, null);
   }

   private static Object compactRoundtrip (ObjectModel om, Object in,
                                           ObserverRegistry oreg)
      throws BlinkException, IOException
   {
      ByteArrayOutputStream os = new ByteArrayOutputStream ();
      CompactWriter wr = new CompactWriter (om, os);

      wr.write (in);
      wr.close ();

      DefaultBlock result = new DefaultBlock ();
      CompactReader rd = new CompactReader (om, oreg);
      rd.read (new ByteBuf (os.toByteArray ()), result);
      assertEquals (1, result.size ());
      Object o = result.getObjects ().get (0);  
      assertEquals (in.getClass (), o.getClass ());
      return o;
   }
   
   private static String encodeCompact (String schema, Object in)
      throws BlinkException, IOException
   {
      ByteArrayOutputStream os = new ByteArrayOutputStream ();
      CompactWriter wr = new CompactWriter (toModel (schema), os);
      wr.write (in);
      wr.close ();
      ByteBuf result = new ByteBuf (os.toByteArray ());
      return result.toHexString ();
   }
   
   private static void schemaRoundtrip (String lit)
      throws BlinkException, IOException
   {
      Schema s = toSchema (lit);
      assertEquals (Util.normalizeSpace (lit).replace ("\"", "'"),
                    Util.normalizeSpace (s.toString ()).replace ("\"", "'"));
   }

   private static ObjectModel toModel (String lit)
      throws BlinkException, IOException
   {
      return new DefaultObjectModel (toSchema (lit), TestCases.class);
   }
   
   private static Schema toSchema (String lit)
      throws BlinkException, IOException
   {
      Schema s = new Schema ();
      SchemaReader.readFromString (lit, s);
      s.finalizeSchema ();
      return s;
   }
   
   public static Buf toBuf (String hex)
   {
      Buf b = new ByteBuf ();
      b.write (hexToBytes (hex));
      b.flip ();
      return b;
   }

   public static byte [] hexToBytes (String hex)
   {
      hex = hex.replace (" ", "").replace ("\n", "");
      int len = hex.length ();
      if (len % 2 != 0)
         throw new IllegalArgumentException (
            "The input is not an even multiple of hex digit pairs");
      byte[] data = new byte [len / 2];

      for (int i = 0; i < len; i += 2)
      {
         data [i / 2] = (byte) ((Character.digit (hex.charAt (i), 16) << 4) +
                                Character.digit (hex.charAt (i + 1), 16));
      }

      return data;
   }

   public static String bytesToHex (byte [] val)
   {
      StringBuilder sb = new StringBuilder ();
      int pos = 0;
      for (byte b : val)
      {
         if (pos ++ > 0)
            sb.append (' ');
         sb.append (String.format ("%02x", b));
      }
      return sb.toString ();
   }

   private static void roundtrip (byte in) throws BlinkException
   {
      Buf b = new ByteBuf ();
      Vlc.writeI32 (in, b);
      b.flip ();
      byte out = Vlc.readI8 (b);
      assertEquals (0, b.available ());
      assertEquals (in, out);
   }

   private static void roundtrip (short in) throws BlinkException
   {
      Buf b = new ByteBuf ();
      Vlc.writeI32 (in, b);
      b.flip ();
      short out = Vlc.readI16 (b);
      assertEquals (0, b.available ());
      assertEquals (in, out);
   }

   private static void roundtrip (int in) throws BlinkException
   {
      Buf b = new ByteBuf ();
      Vlc.writeI32 (in, b);
      b.flip ();
      int out = Vlc.readI32 (b);
      assertEquals (0, b.available ());
      assertEquals (in, out);
   }

   private static void roundtrip (long in) throws BlinkException
   {
      Buf b = new ByteBuf ();
      Vlc.writeI64 (in, b);
      b.flip ();
      long out = Vlc.readI64 (b);
      assertEquals (0, b.available ());
      assertEquals (in, out);
   }

   private static void assertInvalid (String s, String msg)
   {
      try
      {
         toSchema (s);
         fail ("Expected invalid schema");
      }
      catch (BlinkException e)
      {
         assertEquals (msg, e.toString ());
      }
      catch (IOException e)
      {
         assertEquals (msg, e.toString ());
      }
   }

   private static void assertValid (String s)
      throws BlinkException, IOException
   {
      toSchema (s);
   }
}
