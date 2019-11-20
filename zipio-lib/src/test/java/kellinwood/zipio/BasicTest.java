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


import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;
import kellinwood.logging.log4j.Log4jLoggerFactory;


import org.apache.log4j.PropertyConfigurator;
import org.junit.* ;
import static org.junit.Assert.* ;

public class BasicTest extends AbstractTest {


    
    @Test
    public void firstTest() {
        
        try {
            setupLogging();
            
            String inputFile = getClass().getResource("/simple_test.zip").getFile(); 
            if (debug) getLogger().debug("Loading " + inputFile);
            
            ZipInput zipInput = ZipInput.read( inputFile);
            if (debug) getLogger().debug("Entry count: " + zipInput.getEntries().size());

            // Check that we got two entries.
            assertEquals( 2, zipInput.getEntries().size());
            
            // Fetch an entry
            ZioEntry entry = zipInput.getEntries().values().iterator().next();
            
            // Check setTime(), getTime() by using identity transform:  setTime(date), new Date(getTime()) == date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            String inputDate = "2010-12-25 02:59:42";
            Date date = dateFormat.parse( inputDate);
            
            entry.setTime(date.getTime());
            
            date = new Date( entry.getTime());
            
            String testDate = dateFormat.format( date);
            
            if (debug) getLogger().debug( String.format("Input date: %s, test date: %s", inputDate, testDate));
            
            assertEquals( inputDate, testDate);
            
        }
        catch (Exception x) {
            getLogger().error( x.getMessage(), x);
        }      
        
    }
}