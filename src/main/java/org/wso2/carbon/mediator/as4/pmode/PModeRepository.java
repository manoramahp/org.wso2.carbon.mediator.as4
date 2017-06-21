/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.mediator.as4.pmode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.mediator.as4.AS4Constants;
import org.wso2.carbon.mediator.as4.pmode.impl.PMode;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PMode repository implementation which will store current PModes. This will work as file listner as well. So adding
 * and editing PModes will get automatically synced with this in memory repository.
 */
public class PModeRepository implements FileListener {
    private static final Log log = LogFactory.getLog(PModeRepository.class);
    //key - agreement.name value - pMode
    private Map<String, PMode> pModeMap;
    //key - fileName, value - agreementRef
    private Map<String, String> fileNameRefMap;
    //key - fileName, value - agreementRef
    private Map<String, String> possibleNameChangeMap;
    private Unmarshaller pModeUnmarshaller;
    private int basePathLength;

    /**
     * Constructor for PMode repository implementation.
     *
     * @param pModeLocation
     */
    public PModeRepository(String pModeLocation) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing PMode repository for the location - " + pModeLocation);
        }
        try {
            this.pModeUnmarshaller = JAXBContext.newInstance(PMode.class).createUnmarshaller();
        } catch (JAXBException e) {
            log.error("Unable to create JAXB unmarshaller for PModes - " + e.getMessage(), e);
            throw new SynapseException("Unable to create JAXB unmarshaller for PModes - " + e.getMessage(), e);
        }
        this.pModeMap = new HashMap<String, PMode>(1);
        this.fileNameRefMap = new HashMap<String, String>(1);
        this.possibleNameChangeMap = new HashMap<String, String>(1);
        File pModesFolder;
        if (pModeLocation != null) {
            pModesFolder = new File(pModeLocation);
            if (!pModesFolder.exists() || !pModesFolder.isDirectory()) {
                log.warn("Provided PMode directory is invalid, falling back to default PMode Directory - "
                         + AS4Constants.INBOUND_ENDPOINT_PARAMETER_AS4_PMODE_DEFAULT_LOCATION);
                pModesFolder = new File(AS4Constants.INBOUND_ENDPOINT_PARAMETER_AS4_PMODE_DEFAULT_LOCATION);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("PMode directory not provided, falling back to default PMode Directory - "
                          + AS4Constants.INBOUND_ENDPOINT_PARAMETER_AS4_PMODE_DEFAULT_LOCATION);
            }
            pModesFolder = new File(AS4Constants.INBOUND_ENDPOINT_PARAMETER_AS4_PMODE_DEFAULT_LOCATION);
        }
        traversePModeDirectory(pModesFolder);
        try {
            FileSystemManager fsManager = VFS.getManager();
            FileObject listendir = fsManager.resolveFile(pModesFolder.getAbsolutePath());
            this.basePathLength = listendir.getName().getPathDecoded().length() + 1;
            DefaultFileMonitor fileMonitor = new DefaultFileMonitor(this);
            fileMonitor.addFile(listendir);
            fileMonitor.start();
        } catch (FileSystemException e) {
            log.warn("Error registering PMode watcher, hence needs to restart the server when PModes " +
                     "change or added, error - " + e.getMessage(), e);
        }

    }

    /**
     * Helper method which traverse PMode directory and add PModes for the first time.
     *
     * @param folder
     */
    private void traversePModeDirectory(final File folder) {
        if (folder.listFiles() == null || folder.listFiles().length == 0) {
            log.warn("No PMode files folders found in the directory, directory - " + folder.getName());
            return;
        }
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
//                log.info("PMode directory has sub directories, traversing through them as well, directory - "
//                         + file.getName());
//                traversePModeDirectory(file);
                log.warn("PMode directory has sub directories, skipping those sub directories, directory - "
                         + file.getName());
            } else {
                if (!file.getName().endsWith("~")) {
                    processPModeFile(file);
                } else if (log.isDebugEnabled()) {
                    log.debug("Skip processing backup file, file - " + file.getName());
                }

            }
        }
    }

    /**
     * Helper method to process PMode files.
     *
     * @param pModeFile
     */
    private void processPModeFile(File pModeFile) {
        log.info("Processing PMode file - " + pModeFile.getName());
        try {
            PMode pMode = (PMode) this.pModeUnmarshaller.unmarshal(pModeFile);
            if (pMode.getAgreement() == null || pMode.getAgreement().getName() == null
                || pMode.getAgreement().getName().isEmpty()) {
                log.warn("PMode file doesn't contain agreement element, hence ignoring the file, file - "
                         + pModeFile.getName());
                return;
            }
            validatePMode(pMode);
            addUpdateRemovePMode(Operation.ADD, pModeFile.getAbsolutePath(), pMode);
        } catch (JAXBException e) {
            log.warn("Unable to unmarshal PMode file - " + pModeFile.getName() + " error - " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to add, update or remove PMode files from this repository.
     *
     * @param operation
     * @param filePath
     * @param pMode
     */
    private synchronized void addUpdateRemovePMode(Operation operation, String filePath, PMode pMode) {
        switch (operation) {
            case ADD:
                addPMode(filePath, pMode);
                break;
            case UPDATE:
                updatePMode(filePath, pMode);
                break;
            case REMOVE:
                removePMode(filePath);
                break;
        }
    }

    /**
     * Helper method to add PMode to the repository.
     *
     * @param filePath
     * @param pMode
     */
    private void addPMode(String filePath, PMode pMode) {
        log.info("Adding PMode file - " + filePath);
        if (!pModeMap.containsKey(pMode.getAgreement().getName()) && !fileNameRefMap.containsKey(filePath)) {
            //not in both maps, implies new PMode
            pModeMap.put(pMode.getAgreement().getName(), pMode);
            fileNameRefMap.put(filePath, pMode.getAgreement().getName());
            if (log.isDebugEnabled()) {
                log.debug("PMode added with agreement - " + pMode.getAgreement().getName() + ", file - " + filePath);
            }
        } else if (!fileNameRefMap.containsKey(filePath)) {
            //in PMode map but not in name map, implies possible file name change
            String existingFilePath = getExistingPath(pMode.getAgreement().getName());
            if (existingFilePath != null && !existingFilePath.isEmpty()) {
                File existingFile = new File(existingFilePath);
                if (existingFile.exists()) {
                    log.warn("Duplicate PMode agreements found in two files, agreement - "
                             + pMode.getAgreement().getName() + ", ignoring " + filePath);
                    return;
                }
            }
            fileNameRefMap.remove(existingFilePath);
            fileNameRefMap.put(filePath, pMode.getAgreement().getName());
            possibleNameChangeMap.put(existingFilePath, pMode.getAgreement().getName());
            if (log.isDebugEnabled()) {
                log.debug("File path updated for the renamed PMode, agreement - " + pMode.getAgreement().getName()
                          + ", early file - " + existingFilePath + ", new file - " + filePath);
            }
        } else {
            /**
             * Comes to this for two conditions
             * 1 - not in PMode map and in fileMap
             * 2 - in PMode map and in fileMap
             *
             * Those two scenarios cannot happen with this implementation
             */
            log.warn("Duplicate PMode agreements found in two files, agreement - "
                     + pMode.getAgreement().getName());
        }
    }

    /**
     * Helper method to update PMode in the repository.
     *
     * @param filePath
     * @param pMode
     */
    private void updatePMode(String filePath, PMode pMode) {
        log.info("Updating PMode File - " + filePath);
        if (pModeMap.containsKey(pMode.getAgreement().getName())) {
            pModeMap.put(pMode.getAgreement().getName(), pMode);
            if (log.isDebugEnabled()) {
                log.debug("Updating already existing PMode - " + pMode.getAgreement().getName());
            }
        } else {
            pModeMap.remove(fileNameRefMap.get(filePath));
            fileNameRefMap.put(filePath, pMode.getAgreement().getName());
            pModeMap.put(pMode.getAgreement().getName(), pMode);
            if (log.isDebugEnabled()) {
                log.debug("PMode agreement changed, updating PMode - " + pMode.getAgreement().getName());
            }
        }
    }

    /**
     * Helper method to remove PModes.
     *
     * @param filePath
     */
    private void removePMode(String filePath) {
        log.info("Removing PMode file - " + filePath);
        if (!possibleNameChangeMap.containsKey(filePath)) {
            if (fileNameRefMap.containsKey(filePath)) {
                pModeMap.remove(fileNameRefMap.get(filePath));
                fileNameRefMap.remove(filePath);
                if (log.isDebugEnabled()) {
                    log.debug("File removed, hence removing PMode, removedFile - " + filePath);
                }
            } else {
                //directory deletion will come to this or files within inner directories will come to this, hence ignore
            }
        } else {
            possibleNameChangeMap.remove(filePath);
            if (log.isDebugEnabled()) {
                log.debug("File name changed - " + filePath);
            }
        }
    }

    /**
     * Helper method to get early file path.
     *
     * @param agreement
     * @return earlyPath
     */
    private String getExistingPath(String agreement) {
        for (Map.Entry entry : fileNameRefMap.entrySet()) {
            if (entry.getValue().equals(agreement)) {
                return entry.getKey().toString();
            }
        }
        return null;
    }

    /**
     * API method to get PMode using agreementRef.
     *
     * @param agreement
     * @return pMode
     */
    public PMode findPModeFromAgreement(String agreement) {
        return pModeMap.get(agreement);
    }

    @Override
    public void fileCreated(FileChangeEvent fileChangeEvent) throws Exception {
        try {
            if (shouldProcess(fileChangeEvent)) {
                InputStream inputStream = fileChangeEvent.getFile().getContent().getInputStream();
                PMode pMode = (PMode) this.pModeUnmarshaller.unmarshal(inputStream);
                validatePMode(pMode);
                addUpdateRemovePMode(Operation.ADD, fileChangeEvent.getFile().getName().getPathDecoded(), pMode);
            }
        } catch (FileSystemException e) {
            log.warn("File system exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (JAXBException e) {
            log.warn("JAXB exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (SynapseException e) {
            log.warn("SynapseException occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        }
    }

    @Override
    public void fileDeleted(FileChangeEvent fileChangeEvent) throws Exception {
        try {
            addUpdateRemovePMode(Operation.REMOVE, fileChangeEvent.getFile().getName().getPathDecoded(), null);
        } catch (SynapseException e) {
            log.warn("SynapseException occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        }
    }

    @Override
    public void fileChanged(FileChangeEvent fileChangeEvent) throws Exception {
        try {
            if (shouldProcess(fileChangeEvent)) {
                InputStream inputStream = fileChangeEvent.getFile().getContent().getInputStream();
                PMode pMode = (PMode) this.pModeUnmarshaller.unmarshal(inputStream);
                validatePMode(pMode);
                addUpdateRemovePMode(Operation.UPDATE, fileChangeEvent.getFile().getName().getPathDecoded(), pMode);
            }
        } catch (FileSystemException e) {
            log.warn("File system exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (JAXBException e) {
            log.warn("JAXB exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (SynapseException e) {
            log.warn("SynapseException occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("exception occurred while dynamically updating PModes, error - " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to deted whether file relevant to file listner event needs to be processed.
     *
     * @param fileChangeEvent
     * @return true if needs to process, false otherwise.
     * @throws FileSystemException
     */
    private boolean shouldProcess(FileChangeEvent fileChangeEvent) throws FileSystemException {
        String path = fileChangeEvent.getFile().getName().getPathDecoded();
        File file = new File(path);
        if (file.isDirectory()) {
            return false;
        }
        String fileName = path.substring(basePathLength);
        if (fileName.contains(File.separator)) {
            return false;
        }
        if (fileChangeEvent.getFile().getName().getPath().endsWith("~")) {
            return false;
        }
        return true;
    }

    /**
     * Validation method to validate PMode files.
     *
     * @param pMode
     */
    private void validatePMode(PMode pMode) { //todo check whether it needs logging and double check validations
        if (pMode == null) {
            throw new SynapseException("PMode is null");
        }
        if (pMode.getAgreement() == null || pMode.getAgreement().getName() == null
            || pMode.getAgreement().getName().isEmpty()) {
            throw new SynapseException("Invalid PMode Agreement");
        }
        if (pMode.getInitiator() == null) {
            throw new SynapseException("Initiator null in pMode");
        }
        if (pMode.getInitiator().getParty() == null || pMode.getInitiator().getParty().isEmpty()) {
            throw new SynapseException("Invalid Initiator Party in pMode");
        }
    }

    /**
     * Operation type for PMode.
     */
    enum Operation {
        ADD,
        REMOVE,
        UPDATE
    }
}
