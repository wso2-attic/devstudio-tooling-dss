/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.developerstudio.eclipse.ds.wizards.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wso2.developerstudio.eclipse.ds.editors.DSSMultiPageEditor;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;

/**
 * This class contains utility methods for the DSS editor.
 *
 */
public class DSSEditorUtils {
    
    private static volatile DSSEditorUtils instance;
    
    // Path to the properties file where editor-related meta data is saved.
    private static final String PROPERTIES_FILE_PATH = File.separator + ".metadata" + File.separator 
            + "integration-studio.dsseditor.properties";
    
    private static IDeveloperStudioLog log = Logger.getLog("org.wso2.developerstudio.eclipse.ds.editor");
    
    public static DSSEditorUtils getInstance() {
        if (instance != null) {
            return instance;
        }

        synchronized (DSSEditorUtils.class) {
            if (instance == null) {
                instance = new DSSEditorUtils();
            }
        }

        return instance;
    }
    
    private DSSEditorUtils() {};
    
    /**
     * Saves a property to the configured property file.
     * 
     * @param propertyName  Name of the property.
     * @param value Value of the property.
     * @param comments Description of the property.
     * @return  'True' if successfully saved, 'False' otherwise.
     * @throws IOException 
     */
    public boolean saveProperty(String propertyName, String value, String comments) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        File workspaceDirectory = workspace.getRoot().getLocation().toFile();
        String configFilePath = workspaceDirectory.getAbsolutePath() + PROPERTIES_FILE_PATH;
        File confFile = new File(configFilePath);
        OutputStream output = null;
        InputStream input = null;
        
        // Create properties file if does not exist
        if (!confFile.exists()) {
            try {
                confFile.createNewFile();
            } catch (IOException e) {
                log.error("Error occurred while creating the property file.", e);
                return false;
            }
        }
   
        try {
            // Load existing properties
            input = new FileInputStream(configFilePath);
            Properties properties = new Properties();
            properties.load(input);
            input.close();
            
            // If exists, delete
            if (properties.get(propertyName) != null) {
                properties.remove(propertyName);
            }
            
            // Add new property
            properties.setProperty(propertyName, value);
            
            // Write back to file
            output = new FileOutputStream(configFilePath);
            properties.store(output, comments);
            output.close();
            
            return true;
        } catch (IOException e) {
            log.error("Error occurred while saving the property.", e);
            return false;
        }
    }
    
    /**
     * Retrieves a property from the configured property file.
     * 
     * @param propertyName  Name of the property.
     * @return Property value | null if does not exist.
     */
    public String getPropertyValue(String propertyName) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        File workspaceDirectory = workspace.getRoot().getLocation().toFile();
        String configFilePath = workspaceDirectory.getAbsolutePath() + PROPERTIES_FILE_PATH;
        File confFile = new File(configFilePath);

        if (confFile.exists()) {
            try (InputStream input = new FileInputStream(configFilePath)) {
                Properties properties = new Properties();
                properties.load(input);
                input.close();
                return properties.getProperty(propertyName);
            } catch (IOException e) {
                log.error("Error occurred while retrieving the property.", e);
                return null;
            }
        } else {
            log.error("Property file does not exist.");
            return null;
        }
    }
    
    /**
     * Get data source details from properties.
     * 
     * @param datasourceId  Data source ID.
     * @return Data source details.
     */
    public String getDSDetails(String dataSourceId) {
        return getPropertyValue(dataSourceId);
    }
    
    /**
     * Saves data source details to the configured property file.
     * 
     * @param dataSourceId Data source ID.
     * @param details Data source details as a string.
     * @return 'True' of successfully saved, 'False' otherwise.
     */
    public boolean saveDSDetails(String dataSourceId, String details) {
        return saveProperty(dataSourceId, details, null);
    }
    
}