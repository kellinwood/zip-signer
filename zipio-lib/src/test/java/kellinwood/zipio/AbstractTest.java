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
import java.util.Properties;

import kellinwood.logging.Logger;
import kellinwood.logging.log4j.Log4jLoggerFactory;

import org.apache.log4j.PropertyConfigurator;

public abstract class AbstractTest {

    Logger log = null;

    protected boolean debug = false;
    
    protected Logger getLogger() {
        return Logger.getLogger(this.getClass());
    }

    public void setupLogging() {
        
        try {
            Properties log4jProperties = new Properties();
            log4jProperties.load ( new FileReader( "log4j.properties" ));
            PropertyConfigurator.configure( log4jProperties);

            Log4jLoggerFactory.activate();

            debug = getLogger().isDebugEnabled();
        }
        catch (RuntimeException x) {
            throw x;
        }
        catch (Throwable x) {
            throw new IllegalStateException( x.getMessage(), x);
        }
    }

}