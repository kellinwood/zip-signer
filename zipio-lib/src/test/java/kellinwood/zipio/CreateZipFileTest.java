/*
 * Copyright (C) 2010 Ken Ellinwood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kellinwood.zipio;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.Properties;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;
import kellinwood.logging.log4j.Log4jLoggerFactory;


import org.apache.log4j.PropertyConfigurator;
import org.junit.* ;
import static org.junit.Assert.* ;

public class CreateZipFileTest extends AbstractTest {


    
    @Test
    public void createZipTest() {
        
        try {
            setupLogging();

            // Sibling "simple_test.zip" is not read, just used to create the output in the same directory.
            String siblingFile = getClass().getResource("/simple_test.zip").getFile(); 
            File sfile = new File(siblingFile);
            File outputFile = new File(sfile.getParent(), "test_create.zip");
            
            ZipOutput zipOutput = new ZipOutput( outputFile);
            
            ZioEntry entry = new ZioEntry( "B.txt");
            OutputStream entryOut = entry.getOutputStream();
            String bContentText = "The answer to the ultimate question of life, the universe, and everything is 42.";
            entryOut.write( bContentText.getBytes());
            zipOutput.write(entry);
            
            entry = new ZioEntry( "A.txt");
            entry.setCompression(0);
            entryOut = entry.getOutputStream();
            String aContentText = "The name of the computer used to calculate the answer to the ultimate question is \"Earth\".";
            entryOut.write( aContentText.getBytes());
            zipOutput.write(entry);
            
            zipOutput.close();

            // verify the result
            ZipInput zipInput = ZipInput.read( outputFile.getAbsolutePath());
            
            entry = zipInput.getEntry("A.txt");
            String content = new String(entry.getData());
            assertEquals( aContentText, content);
            
            entry = zipInput.getEntry("B.txt");
            content = new String(entry.getData());
            assertEquals( bContentText, content);

            
        }
        catch (Exception x) {
            getLogger().error( x.getMessage(), x);
            fail( x.getClass().getName() + ": " + x.getMessage());
        }      
    }
    
    @Test
    public void createZipTest2() {
        
        try {
            setupLogging();

            // Test the two argument variation of the ZipEntry constructor which is used to add an existing file to the zip.
            
            String siblingFile = getClass().getResource("/simple_test.zip").getFile(); 
            File sfile = new File(siblingFile);
            File outputFile = new File(sfile.getParent(), "test_create2.zip");
            
            ZipOutput zipOutput = new ZipOutput( outputFile);
            
            ZioEntry entry = new ZioEntry( "simple_test.zip", siblingFile);
            zipOutput.write(entry);
            
            zipOutput.close();

            // verify the result
            ZipInput zipInput = ZipInput.read( outputFile.getAbsolutePath());
            
            entry = zipInput.getEntry("simple_test.zip");
            assertNotNull( entry);
        }
        catch (Exception x) {
            getLogger().error( x.getMessage(), x);
            fail( x.getClass().getName() + ": " + x.getMessage());
        }      
    }
    
    @Test
    public void mergeZipTest() {
        
        try {
            setupLogging();
            
            String siblingFile = getClass().getResource("/simple_test.zip").getFile();
            ZipInput zipInput = ZipInput.read( siblingFile);

            ZioEntry testEntry = zipInput.getEntry("test.txt");
            // Change the name of the file, so it becomes to test2.txt in the output
            testEntry.setName("test2.txt");
            
            File sfile = new File(siblingFile);
            File outputFile = new File(sfile.getParent(), "test_merged.zip");
            
            ZipOutput zipOutput = new ZipOutput( outputFile);
            
            ZioEntry entry = new ZioEntry( "answer.txt");
            OutputStream entryOut = entry.getOutputStream();
            String bContentText = "The answer to the ultimate question of life, the universe, and everything is 42.";
            entryOut.write( bContentText.getBytes());
            zipOutput.write(entry);
            
            entry = new ZioEntry( "A.txt");
            entry.setCompression(0);
            entryOut = entry.getOutputStream();
            String aContentText = "The name of the computer used to calculate the answer to the ultimate question is \"Earth\".";
            entryOut.write( aContentText.getBytes());
            zipOutput.write(entry);
            
            for (ZioEntry e : zipInput.zioEntries.values()) {
                zipOutput.write(e);
            }
            
            zipOutput.close();

            // verify the result
            zipInput = ZipInput.read( outputFile.getAbsolutePath());
            
            entry = zipInput.getEntry("A.txt");
            String content = new String(entry.getData());
            assertEquals( aContentText, content);
            
            entry = zipInput.getEntry("answer.txt");
            content = new String(entry.getData());
            assertEquals( bContentText, content);

            
            
            
        }
        catch (Exception x) {
            getLogger().error( x.getMessage(), x);
            fail( x.getClass().getName() + ": " + x.getMessage());
        }      
    }    
}