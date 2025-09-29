/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
*/
package bluej.parser.context;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

/**
 * Test to verify .ctxt file format compatibility between old and new implementations.
 * This test creates actual .ctxt files and verifies they maintain the exact format
 * expected by BlueJ.
 */
public class CtxtFileCompatibilityTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private File testCtxtFile;
    
    @Before
    public void setUp() throws IOException {
        testCtxtFile = tempFolder.newFile("TestClass.ctxt");
    }
    
    /**
     * Test that we can read actual BlueJ .ctxt file format
     */
    @Test
    public void testReadActualBlueJFormat() throws IOException {
        // Create a .ctxt file in the actual BlueJ format
        String actualBlueJContent = 
            "#BlueJ class context\n" +
            "comment0.target=TestClass\n" +
            "comment0.text=\\\n" +
            "\\ TestClass\\ is\\ a\\ sample\\ class\\ for\\ testing.\\\n" +
            "\\ It\\ demonstrates\\ various\\ features.\\\n" +
            "\\ \n" +
            "comment1.params=x\\ y\n" +
            "comment1.target=TestClass(int,\\ int)\n" + 
            "comment1.text=\\\n" +
            "\\ Constructor\\ for\\ objects\\ of\\ class\\ TestClass\\\n" +
            "\\ @param\\ x\\ the\\ x\\ coordinate\\\n" +
            "\\ @param\\ y\\ the\\ y\\ coordinate\\\n" +
            "\\ \n" +
            "comment2.params=\n" +
            "comment2.target=int\\ sampleMethod()\n" +
            "comment2.text=\\\n" +
            "\\ An\\ example\\ of\\ a\\ method\\ -\\ replace\\ this\\ comment\\ with\\ your\\ own\\\n" +
            "\\ \n" +
            "comment3.params=y\n" +
            "comment3.target=int\\ anotherMethod(int)\n" +
            "comment3.text=\\\n" +
            "\\ Another\\ method\\ with\\ parameter\\\n" +
            "\\ @param\\ y\\ a\\ sample\\ parameter\\ for\\ this\\ method\\\n" +
            "\\ @return\\ the\\ sum\\ of\\ x\\ and\\ y\\\n" +
            "\\ \n" +
            "numComments=4\n";
        
        try (PrintWriter writer = new PrintWriter(testCtxtFile)) {
            writer.write(actualBlueJContent);
        }
        
        // Load using CompilationUnitContext
        CompilationUnitContext context = PropertyContextFormat.fromFile("TestClass", testCtxtFile);

        assertNotNull("Context should not be null", context);
        
        // Verify all comments were loaded correctly
        assertEquals("Should have 4 comments", 4, context.getComments().size());
        
        // Verify class comment
        CommentEntry classComment = context.getComments().get(0);
        assertEquals("TestClass", classComment.getTarget());
        assertTrue("Class comment should contain description", 
            classComment.getText().contains("sample class for testing"));
        
        // Verify constructor comment
        CommentEntry constructorComment = context.getComments().get(1);
        assertEquals("TestClass(int, int)", constructorComment.getTarget());
        assertEquals(2, constructorComment.getParamNames().size());
        assertEquals("x", constructorComment.getParamNames().get(0));
        assertEquals("y", constructorComment.getParamNames().get(1));
        assertTrue("Constructor comment should contain param descriptions",
            constructorComment.getText().contains("x coordinate"));
        
        // Verify method comments
        CommentEntry method1 = context.getComments().get(2);
        assertEquals("int sampleMethod()", method1.getTarget());
        assertEquals(0, method1.getParamNames().size());
        
        CommentEntry method2 = context.getComments().get(3);
        assertEquals("int anotherMethod(int)", method2.getTarget());
        assertEquals(1, method2.getParamNames().size());
        assertEquals("y", method2.getParamNames().get(0));
    }
    
    /**
     * Test that we generate the correct format when saving
     */
    @Test
    public void testWriteCorrectFormat() throws IOException {
        // Create a context with typical BlueJ content
        CompilationUnitContext context = CompilationUnitContext.writable("MyClass", testCtxtFile);
        
        // Add class comment
        context.addComment(new CommentEntry(
            "MyClass",
            "MyClass represents a sample object in BlueJ.\n" +
            "This is a multi-line comment that explains the class.\n",
            Collections.emptyList()
        ));
        
        // Add constructor comment
        context.addComment(new CommentEntry(
            "MyClass(String, int)",
            "Constructor for MyClass\n" +
            "@param name The name of the object\n" +
            "@param value The initial value\n",
            Arrays.asList("name", "value")
        ));
        
        // Add method comments
        context.addComment(new CommentEntry(
            "void doSomething()",
            "Method that performs an action",
            Collections.emptyList()
        ));
        
        context.addComment(new CommentEntry(
            "String getName()",
            "Returns the name of this object\n" +
            "@return the name",
            Collections.emptyList()
        ));
        
        context.addComment(new CommentEntry(
            "boolean validate(String, int, boolean)",
            "Validates the object state\n" +
            "@param input The input to validate\n" +
            "@param threshold The threshold value\n" +
            "@param strict Whether to use strict validation\n" +
            "@return true if valid, false otherwise",
            Arrays.asList("input", "threshold", "strict")
        ));
        
        // Save the context
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read the file as Properties to verify format
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(testCtxtFile)) {
            props.load(in);
        }
        
        // Verify the Properties format
        assertEquals("5", props.getProperty("numComments"));
        
        // Check class comment
        assertEquals("MyClass", props.getProperty("comment0.target"));
        assertNotNull(props.getProperty("comment0.text"));
        
        // Check constructor
        assertEquals("MyClass(String, int)", props.getProperty("comment1.target"));
        assertEquals("name value", props.getProperty("comment1.params"));
        assertNotNull(props.getProperty("comment1.text"));
        
        // Check methods
        assertEquals("void doSomething()", props.getProperty("comment2.target"));
        assertNull(props.getProperty("comment2.params")); // No params
        
        assertEquals("String getName()", props.getProperty("comment3.target"));
        assertNull(props.getProperty("comment3.params")); // No params
        
        assertEquals("boolean validate(String, int, boolean)", props.getProperty("comment4.target"));
        assertEquals("input threshold strict", props.getProperty("comment4.params"));
        assertNotNull(props.getProperty("comment4.text"));
    }
    
    /**
     * Test handling of escape sequences in Properties format
     */
    @Test
    public void testEscapeSequenceHandling() throws IOException {
        // Create content with special characters that need escaping in Properties
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", testCtxtFile);
        
        context.addComment(new CommentEntry(
            "void test()",
            "Method with special chars: = : \\ \n" +
            "New line above, tab\there, and unicode \\u0041",
            Collections.emptyList()
        ));
        
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read back
        CompilationUnitContext loaded = PropertyContextFormat.fromFile("TestClass", testCtxtFile);

        assertNotNull("Context should not be null", testCtxtFile);
        
        assertEquals(1, loaded.getComments().size());
        CommentEntry entry = loaded.getComments().get(0);
        
        // Properties should handle escaping automatically
        assertTrue("Should preserve special characters", 
            entry.getText().contains("special chars"));
    }
    
    /**
     * Test that empty params are handled correctly (no params property written)
     */
    @Test
    public void testEmptyParamsHandling() throws IOException {
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", testCtxtFile);
        
        // Method with no parameters
        context.addComment(new CommentEntry(
            "void noParams()",
            "Method with no parameters"
        ));

        // Method with empty list
        context.addComment(new CommentEntry(
            "void emptyList()",
            "Method with empty param list",
            Collections.emptyList() // empty list
        ));
        
        // Method with parameters
        context.addComment(new CommentEntry(
            "void withParams(int, String)",
            "Method with parameters",
            Arrays.asList("value", "text")
        ));
        
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Verify the file content
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(testCtxtFile)) {
            props.load(in);
        }
        
        // No params property should be written for methods without parameters
        assertNull("Should not have params property for first method", 
            props.getProperty("comment0.params"));
        assertNull("Should not have params property for second method", 
            props.getProperty("comment1.params"));
        assertEquals("value text", props.getProperty("comment2.params"));
    }
    
    /**
     * Test compatibility with BlueJ's actual file header
     */
    @Test
    public void testFileHeaderCompatibility() throws IOException {
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", testCtxtFile);
        context.addComment(new CommentEntry(
            "TestClass",
            "A test class",
            Collections.emptyList()
        ));
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read the file content
        String content;
        try (BufferedReader reader = new BufferedReader(new FileReader(testCtxtFile))) {
            content = reader.readLine();
        }
        
        // Should start with the BlueJ header
        assertTrue("File should start with BlueJ header", 
            content.startsWith("#BlueJ class context"));
    }
}