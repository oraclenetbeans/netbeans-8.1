/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.cnd.makeproject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.netbeans.modules.cnd.makeproject.NativeProjectRelocationMapperProviderImpl.ProjectMapper;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.makeproject.spi.ProjectMetadataFactory;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author mtishkov
 */
@ServiceProvider(service = ProjectMetadataFactory.class, path = "Projects/org-netbeans-modules-cnd-makeproject/" + ProjectMetadataFactory.LAYER_PATH, position = 90)
public class PathMapperProjectMetadataFactory implements ProjectMetadataFactory {

    public static final String NAME = "path_mapper.properties"; //NOI18N

    @Override
    public void read(FileObject projectDir) {
        FileObject nbproject = projectDir.getFileObject(MakeConfiguration.NBPROJECT_FOLDER);
        FileChangeListenerImpl fileChangeListener = new FileChangeListenerImpl(projectDir);
        nbproject.addFileChangeListener(fileChangeListener);
        initListeners(fileChangeListener, projectDir);
        reload(projectDir);


    }

    private void initListeners(FileChangeListener fileChangeListener, FileObject projectDir) {
        FileObject nbproject = projectDir.getFileObject(MakeConfiguration.NBPROJECT_FOLDER);
        FileObject publicPathMapper = nbproject.getFileObject(NAME);
        if (publicPathMapper != null) {
            publicPathMapper.removeFileChangeListener(fileChangeListener);
            publicPathMapper.addFileChangeListener(fileChangeListener);
        }        
        final FileObject privateNbFolder = projectDir.getFileObject(MakeConfiguration.NBPROJECT_PRIVATE_FOLDER);
        if (privateNbFolder != null && privateNbFolder.isValid()) {
            privateNbFolder.removeFileChangeListener(fileChangeListener);
            privateNbFolder.addFileChangeListener(fileChangeListener);
            FileObject privatePathMapper = privateNbFolder.getFileObject(NAME);
            if (privatePathMapper != null) {
                privatePathMapper.removeFileChangeListener(fileChangeListener);
                privatePathMapper.addFileChangeListener(fileChangeListener);
            }
        }
    }

    @Override
    public void write(FileObject projectDir) {
    }

    private static void reload(FileObject projectDir) {
        ProjectMapper projectMapper = NativeProjectRelocationMapperProviderImpl.get(projectDir);
        Properties properties = new Properties();
        final FileObject nbProjectFolder = projectDir.getFileObject(MakeConfiguration.NBPROJECT_FOLDER);
        if (nbProjectFolder == null) {  
            return;
        }
        FileObject publicMappers = nbProjectFolder.getFileObject(NAME);
        final FileObject privateNbFolder = projectDir.getFileObject(MakeConfiguration.NBPROJECT_PRIVATE_FOLDER);
        FileObject privateMappers = null;
        if (privateNbFolder != null && privateNbFolder.isValid()) {
            privateMappers = privateNbFolder.getFileObject(NAME);
        }
        try {
            if (publicMappers != null && publicMappers.isValid()) {
                final InputStream inputStream = publicMappers.getInputStream();
                properties.load(inputStream);
                inputStream.close();
            }
            if (privateMappers != null && privateMappers.isValid()) {
                final InputStream inputStream = privateMappers.getInputStream();
                properties.load(inputStream);
                inputStream.close();
            }
        } catch (IOException ex) {
            //Exceptions.printStackTrace(ex);
        }
        projectMapper.load(properties, projectDir);
    }

    private class FileChangeListenerImpl implements FileChangeListener {

        private final FileObject projectDir;

        public FileChangeListenerImpl(FileObject projectDir) {
            this.projectDir = projectDir;
        }

        @Override
        public void fileFolderCreated(FileEvent fe) {
            //private folder could be created here, in this case: let's listen 
            initListeners(this, projectDir);
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            //listen for the new file: attach listener to the file to listen for changes
            initListeners(this, projectDir);
            reload(projectDir);
        }

        @Override
        public void fileChanged(FileEvent fe) {
            reload(projectDir);
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            fe.getFile().removeFileChangeListener(this);
            reload(projectDir);
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            reload(projectDir);
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fe) {
        }
    }
}
