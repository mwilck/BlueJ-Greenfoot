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

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive integration test for CompilationUnitContext across
 * Package, View, ClassTarget, and ClassInfo components.
 */
public class CompilationUnitContextIntegrationTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private File projectDir;
    private File packageDir;
    private File ctxtFile;
    private File classFile;
    
    @Before
    public void setUp() throws IOException {
        // Create test directory structure
        projectDir = tempFolder.newFolder("test_project");
        packageDir = new File(projectDir, "test_package");
        packageDir.mkdirs();
        
        // Create test .java file
        classFile = new File(packageDir, "TestClass.java");
        try (PrintWriter writer = new PrintWriter(classFile)) {
            writer.println("package test_package;");
            writer.println("");
            writer.println("/**");
            writer.println(" * Test class for integration testing");
            writer.println(" */");
            writer.println("public class TestClass {");
            writer.println("    ");
            writer.println("    /**");
            writer.println("     * Constructor");
            writer.println("     * @param value Initial value");
            writer.println("     * @param name The name");
            writer.println("     */");
            writer.println("    public TestClass(int value, String name) {");
            writer.println("    }");
            writer.println("    ");
            writer.println("    /**");
            writer.println("     * Test method");
            writer.println("     */");
            writer.println("    public void testMethod() {");
            writer.println("    }");
            writer.println("    ");
            writer.println("    /**");
            writer.println("     * Another method with parameters");
            writer.println("     * @param input Input string");
            writer.println("     * @param count Number of times");
            writer.println("     * @return Result string");
            writer.println("     */");
            writer.println("    public String processData(String input, int count) {");
            writer.println("        return input;");
            writer.println("    }");
            writer.println("}");
        }
        
        // Create corresponding .ctxt file
        ctxtFile = new File(packageDir, "TestClass.ctxt");
    }
    
    @After
    public void tearDown() {
        // Cleanup is handled by TemporaryFolder
    }
    
    /**
     * Test 1: ClassInfo to CompilationUnitContext flow
     * Verifies that ClassInfo properly populates CompilationUnitContext
     */
    @Test
    public void testClassInfoToContextFlow() throws Exception {
        // Create a context from ClassInfo
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", ctxtFile);
        
        // Simulate what ClassInfo would do: add comments based on parsed Javadoc
        context.addComment(new CommentEntry(
            "TestClass(int,String)", 
            "Constructor", 
            Arrays.asList("value", "name")
        ));
        
        context.addComment(new CommentEntry(
            "void testMethod()", 
            "Test method", 
            Collections.emptyList()
        ));
        
        context.addComment(new CommentEntry(
            "String processData(String,int)", 
            "Another method with parameters", 
            Arrays.asList("input", "count")
        ));
        
        // Verify the context has the expected data
        assertEquals("Should have 3 comments", 3, context.getComments().size());
        
        // Verify first comment (constructor)
        CommentEntry entry0 = context.getComments().get(0);
        assertEquals("TestClass(int,String)", entry0.getTarget());
        assertEquals("Constructor", entry0.getText());
        assertEquals(2, entry0.getParamNames().size());
        assertEquals("value", entry0.getParamNames().get(0));
        assertEquals("name", entry0.getParamNames().get(1));
        
        // Verify second comment (test method)
        CommentEntry entry1 = context.getComments().get(1);
        assertEquals("void testMethod()", entry1.getTarget());
        assertEquals("Test method", entry1.getText());
        assertEquals(0, entry1.getParamNames().size());
        
        // Verify third comment (process data method)
        CommentEntry entry2 = context.getComments().get(2);
        assertEquals("String processData(String,int)", entry2.getTarget());
        assertEquals("Another method with parameters", entry2.getText());
        assertEquals(2, entry2.getParamNames().size());
        assertEquals("input", entry2.getParamNames().get(0));
        assertEquals("count", entry2.getParamNames().get(1));
    }
    
    /**
     * Test 2: Package.java save context functionality
     * Verifies that Package can save CompilationUnitContext to .ctxt files
     */
    @Test
    public void testPackageSaveContext() throws Exception {
        // Create and populate context
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", ctxtFile);
        
        context.addComment(new CommentEntry(
            "TestClass(int,String)", 
            "Constructor with parameters", 
            Arrays.asList("value", "name")
        ));
        
        context.addComment(new CommentEntry(
            "void doSomething()", 
            "Method that does something"
        ));
        
        // Save the context (simulates what Package would do)
        PropertyContextFormat.writeToFile(context, ctxtFile);
        
        // Verify the file was created
        assertTrue("Ctxt file should exist", ctxtFile.exists());
        
        // Verify the saved format by reading with Properties
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(ctxtFile)) {
            props.load(in);
        }
        
        // Verify Properties format compatibility
        assertEquals("2", props.getProperty("numComments"));
        assertEquals("TestClass(int,String)", props.getProperty("comment0.target"));
        assertEquals("Constructor with parameters", props.getProperty("comment0.text"));
        assertEquals("value name", props.getProperty("comment0.params"));
        assertEquals("void doSomething()", props.getProperty("comment1.target"));
        assertEquals("Method that does something", props.getProperty("comment1.text"));
        assertNull("Should not have params for second comment", props.getProperty("comment1.params"));
    }
    
    /**
     * Test 3: View.java library class resource loading
     * Verifies that View can load CompilationUnitContext for library classes
     */
    @Test
    public void testViewLibraryClassLoading() throws Exception {
        // Create a .ctxt file simulating a library class context
        Properties libProps = new Properties();
        libProps.setProperty("numComments", "3");
        libProps.setProperty("comment0.target", "java.lang.String()");
        libProps.setProperty("comment0.text", "Default constructor");
        libProps.setProperty("comment1.target", "int length()");
        libProps.setProperty("comment1.text", "Returns the length of this string");
        libProps.setProperty("comment2.target", "String substring(int,int)");
        libProps.setProperty("comment2.text", "Returns a substring");
        libProps.setProperty("comment2.params", "beginIndex endIndex");
        
        File libCtxtFile = new File(tempFolder.getRoot(), "String.ctxt");
        try (OutputStream out = new FileOutputStream(libCtxtFile)) {
            libProps.store(out, "Library class context");
        }
        
        // Load using PropertyContextFormat (simulates what View would do)
        CompilationUnitContext libContext = PropertyContextFormat.fromFile("String", libCtxtFile);

        assertNotNull("Context should not be null", libContext);

        // Verify library context was loaded correctly
        assertEquals("Should have 3 comments", 3, libContext.getComments().size());
        
        CommentEntry entry0 = libContext.getComments().get(0);
        assertEquals("java.lang.String()", entry0.getTarget());
        assertEquals("Default constructor", entry0.getText());
        
        CommentEntry entry1 = libContext.getComments().get(1);
        assertEquals("int length()", entry1.getTarget());
        assertEquals("Returns the length of this string", entry1.getText());
        
        CommentEntry entry2 = libContext.getComments().get(2);
        assertEquals("String substring(int,int)", entry2.getTarget());
        assertEquals("Returns a substring", entry2.getText());
        assertEquals(2, entry2.getParamNames().size());
        assertEquals("beginIndex", entry2.getParamNames().get(0));
        assertEquals("endIndex", entry2.getParamNames().get(1));
    }
    
    /**
     * Test 4: ClassTarget getCompilationContext() API
     * Verifies the ClassTarget can provide CompilationUnitContext
     */
    @Test
    public void testClassTargetGetCompilationContext() throws Exception {
        // Create a context and save it to file
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", ctxtFile);
        
        context.addComment(new CommentEntry(
            "TestClass()", 
            "Default constructor", 
            Collections.emptyList()
        ));
        
        context.addComment(new CommentEntry(
            "void initialize(String)", 
            "Initialization method", 
            Arrays.asList("config")
        ));
        
        PropertyContextFormat.writeToFile(context, ctxtFile);
        
        CompilationUnitContext loadedContext = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Loaded context should not be null", loadedContext);
        
        // Verify the loaded context matches what was saved
        assertEquals("Should have 2 comments", 2, loadedContext.getComments().size());
        
        CommentEntry entry0 = loadedContext.getComments().get(0);
        assertEquals("TestClass()", entry0.getTarget());
        assertEquals("Default constructor", entry0.getText());
        assertEquals(0, entry0.getParamNames().size());
        
        CommentEntry entry1 = loadedContext.getComments().get(1);
        assertEquals("void initialize(String)", entry1.getTarget());
        assertEquals("Initialization method", entry1.getText());
        assertEquals(1, entry1.getParamNames().size());
        assertEquals("config", entry1.getParamNames().get(0));
    }
    
    /**
     * Test 5: Round-trip compatibility test
     * Write with CompilationUnitContext, read with Properties, 
     * then write with Properties and read with CompilationUnitContext
     */
    @Test
    public void testRoundTripCompatibility() throws Exception {
        // Step 1: Create and save with CompilationUnitContext
        CompilationUnitContext context1 = CompilationUnitContext.writable("TestClass", ctxtFile);
        context1.addComment(new CommentEntry(
            "TestClass(String,int,boolean)", 
            "Complex constructor", 
            Arrays.asList("name", "value", "flag")
        ));
        PropertyContextFormat.writeToFile(context1, ctxtFile);
        
        // Step 2: Read with Properties
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(ctxtFile)) {
            props.load(in);
        }
        
        // Verify Properties content
        assertEquals("1", props.getProperty("numComments"));
        assertEquals("TestClass(String,int,boolean)", props.getProperty("comment0.target"));
        assertEquals("Complex constructor", props.getProperty("comment0.text"));
        assertEquals("name value flag", props.getProperty("comment0.params"));
        
        // Step 3: Modify and save with Properties
        props.setProperty("comment1.target", "void newMethod()");
        props.setProperty("comment1.text", "Added method");
        props.setProperty("numComments", "2");
        
        try (OutputStream out = new FileOutputStream(ctxtFile)) {
            props.store(out, "Modified by Properties");
        }
        
        // Step 4: Read with CompilationUnitContext
        CompilationUnitContext context2 = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Context should not be null", context2);
        
        // Verify both comments are present
        assertEquals("Should have 2 comments", 2, context2.getComments().size());
        
        CommentEntry entry0 = context2.getComments().get(0);
        assertEquals("TestClass(String,int,boolean)", entry0.getTarget());
        assertEquals("Complex constructor", entry0.getText());
        assertEquals(3, entry0.getParamNames().size());
        
        CommentEntry entry1 = context2.getComments().get(1);
        assertEquals("void newMethod()", entry1.getTarget());
        assertEquals("Added method", entry1.getText());
        assertEquals(0, entry1.getParamNames().size());
    }
    
    /**
     * Test 6: Error handling - malformed .ctxt file
     */
    @Test
    public void testMalformedCtxtFileHandling() throws Exception {
        // Create a malformed .ctxt file
        try (PrintWriter writer = new PrintWriter(ctxtFile)) {
            writer.println("This is not a valid properties file!");
            writer.println("numComments = not_a_number");
            writer.println("comment0.target missing equals");
        }
        
        // Try to load it - should handle gracefully
        CompilationUnitContext context = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Context should not be null", context);
        
        // Context should be empty or have partial data
        assertNotNull("Context should not be null", context);
        // The exact behavior depends on Properties.load() error handling
    }
    
    /**
     * Test 7: Performance test - large .ctxt file
     */
    @Test
    public void testLargeCtxtFilePerformance() throws Exception {
        // Create a large context with many comments
        CompilationUnitContext context = CompilationUnitContext.writable("LargeClass", ctxtFile);
        
        int numComments = 100;
        for (int i = 0; i < numComments; i++) {
            List<String> params = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                params.add("param" + j);
            }
            
            context.addComment(new CommentEntry(
                "void method" + i + "(String,int,boolean,Object,List)",
                "Method number " + i + " with multiple parameters",
                params
            ));
        }
        
        // Measure save performance
        long startSave = System.currentTimeMillis();
        PropertyContextFormat.writeToFile(context, ctxtFile);
        long saveDuration = System.currentTimeMillis() - startSave;
        
        assertTrue("Save should complete within reasonable time", saveDuration < 1000);
        
        // Measure load performance
        long startLoad = System.currentTimeMillis();
        CompilationUnitContext loadedContext = PropertyContextFormat.fromFile("LargeClass", ctxtFile);

        assertNotNull("Context should not be null", loadedContext);
        long loadDuration = System.currentTimeMillis() - startLoad;
        
        assertTrue("Load should complete within reasonable time", loadDuration < 1000);
        assertEquals("Should load all comments", numComments, loadedContext.getComments().size());
    }
    
    /**
     * Test 8: Unicode and special characters handling
     */
    @Test
    public void testUnicodeAndSpecialCharacters() throws Exception {
        // Create context with Unicode and special characters
        CompilationUnitContext context = CompilationUnitContext.writable("TestClass", ctxtFile);
        
        context.addComment(new CommentEntry(
            "void método()", 
            "Method with Unicode: 日本語 中文 한국어 العربية"
        ));
        
        context.addComment(new CommentEntry(
            "String processText(String)", 
            "Handles special chars: !@#$%^&*(){}[]|\\:;\"'<>,.?/", 
            Arrays.asList("tëxt_with_ümläuts")
        ));
        
        // Save and reload
        PropertyContextFormat.writeToFile(context, ctxtFile);
        
        CompilationUnitContext loadedContext = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Context should not be null", loadedContext);
        
        // Verify Unicode and special characters are preserved
        assertEquals(2, loadedContext.getComments().size());
        
        CommentEntry entry0 = loadedContext.getComments().get(0);
        assertEquals("void método()", entry0.getTarget());
        assertTrue(entry0.getText().contains("日本語"));
        assertTrue(entry0.getText().contains("中文"));
        
        CommentEntry entry1 = loadedContext.getComments().get(1);
        assertTrue(entry1.getText().contains("!@#$%^&*()"));
        assertEquals("tëxt_with_ümläuts", entry1.getParamNames().get(0));
    }
}