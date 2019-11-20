/*
 * Copyright (C) 2011 Ken Ellinwood
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
import java.util.Collection;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;
import kellinwood.logging.log4j.Log4jLoggerFactory;


import org.apache.log4j.PropertyConfigurator;
import org.junit.* ;
import static org.junit.Assert.* ;

public class ListTest extends AbstractTest {


    
    @Test
    public void listZipTest() {
        
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
            
            entry = new ZioEntry( "C/A.txt");
            entry.setCompression(0);
            entryOut = entry.getOutputStream();
            aContentText = "The name of the computer used to calculate the answer to the ultimate question is \"Earth\".";
            entryOut.write( aContentText.getBytes());
            zipOutput.write(entry);
            
            entry = new ZioEntry( "C/B.txt");
            entryOut = entry.getOutputStream();
            bContentText = "The answer to the ultimate question of life, the universe, and everything is 42.";
            entryOut.write( bContentText.getBytes());
            zipOutput.write(entry);
            

            zipOutput.close();

            // verify the result
            ZipInput zipInput = ZipInput.read( outputFile.getAbsolutePath());

            Collection<String> list = zipInput.list("/");

            assertEquals( 3, list.size());
            assertTrue( list.contains( "A.txt"));
            assertTrue( list.contains( "B.txt"));
            assertTrue( list.contains( "C/"));

            list = zipInput.list("C/");
            assertEquals( 2, list.size());
            assertTrue( list.contains( "A.txt"));
            assertTrue( list.contains( "B.txt"));
            
        }
        catch (Exception x) {
            getLogger().error( x.getMessage(), x);
            fail( x.getClass().getName() + ": " + x.getMessage());
        }      
    }
    
}