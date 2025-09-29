/*
 This file is part of the BlueJ program.
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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

import bluej.classmgr.BPClassLoader;

import java.io.File;
import java.net.URL;

/**
 * Test implementation of ClassLoaderProvider for unit testing.
 * 
 * This helper class allows tests to inject custom ClassLoader and project directory
 * configurations without requiring a full Project instance.
 */
public class TestClassLoaderProvider implements ClassLoaderProvider
{
    private final BPClassLoader classLoader;
    private final File projectDir;
    
    /**
     * Creates a test ClassLoaderProvider with the specified ClassLoader and project directory.
     * 
     * @param classLoader The ClassLoader to use for class resolution
     * @param projectDir  The project directory root
     */
    public TestClassLoaderProvider(BPClassLoader classLoader, File projectDir)
    {
        if (classLoader == null) {
            throw new IllegalArgumentException("ClassLoader cannot be null");
        }
        if (projectDir == null) {
            throw new IllegalArgumentException("Project directory cannot be null");
        }
        
        this.classLoader = classLoader;
        this.projectDir = projectDir;
    }
    
    /**
     * Creates a test ClassLoaderProvider with a default ClassLoader using the specified
     * project directory and system classpath.
     * 
     * @param projectDir The project directory root
     */
    public TestClassLoaderProvider(File projectDir)
    {
        this(createDefaultClassLoader(projectDir), projectDir);
    }
    
    @Override
    public BPClassLoader getClassLoader()
    {
        return classLoader;
    }
    
    @Override
    public File getProjectDir()
    {
        return projectDir;
    }
    
    /**
     * Creates a default BPClassLoader for testing purposes.
     * 
     * @param projectDir The project directory to use as a classpath root
     * @return A new BPClassLoader instance configured for testing
     */
    private static BPClassLoader createDefaultClassLoader(File projectDir)
    {
        if (projectDir == null) {
            throw new IllegalArgumentException("Project directory cannot be null");
        }
        
        // Create a basic BPClassLoader with the project directory in the classpath
        URL[] urls = new URL[] { };  // Empty URLs for basic testing
        return new BPClassLoader(urls, TestClassLoaderProvider.class.getClassLoader());
    }
    
    /**
     * Builder class for creating TestClassLoaderProvider instances with various configurations.
     */
    public static class Builder
    {
        private BPClassLoader classLoader;
        private File projectDir;
        private File[] additionalClasspaths;
        
        public Builder projectDir(File projectDir)
        {
            this.projectDir = projectDir;
            return this;
        }
        
        public Builder projectDir(String projectDirPath)
        {
            this.projectDir = new File(projectDirPath);
            return this;
        }
        
        /**
         * Configures the Builder to use a ClassLoader that can load resources from the specified path.
         * Creates a BPClassLoader internally with the given path as a classpath entry.
         *
         * @param resourcePath Path to the resource directory (will be converted to a URL)
         * @return This Builder instance for method chaining
         */
        public Builder withResource(String resourcePath)
        {
            return withResource(new File(resourcePath));
        }
        
        /**
         * Configures the Builder to use a ClassLoader that can load resources from the specified path.
         * Creates a BPClassLoader internally with the given path as a classpath entry.
         *
         * @param resourcePath Path to the resource directory (as a File)
         * @return This Builder instance for method chaining
         */
        public Builder withResource(File resourcePath)
        {
            try {
                URL resourceUrl = resourcePath.toURI().toURL();
                this.classLoader = new BPClassLoader(
                    new URL[] { resourceUrl },
                    TestClassLoaderProvider.class.getClassLoader()
                );
                return this;
            } catch (java.net.MalformedURLException e) {
                throw new IllegalArgumentException("Invalid resource path: " + resourcePath, e);
            }
        }
        
        public Builder withAdditionalClasspaths(File... paths)
        {
            this.additionalClasspaths = paths;
            return this;
        }
        
        public TestClassLoaderProvider build()
        {
            if (projectDir == null) {
                throw new IllegalStateException("Project directory must be specified");
            }
            
            if (classLoader == null) {
                // Create a ClassLoader with additional classpaths if specified
                // For test purposes, create a simple BPClassLoader
                classLoader = createDefaultClassLoader(projectDir);
            }
            
            return new TestClassLoaderProvider(classLoader, projectDir);
        }
    }
}