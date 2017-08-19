package org.apache.sis.desktop.metadata;

import org.apache.sis.desktop.metadata.NodeTreeTable;
import java.io.File;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import org.apache.sis.desktop.MetadataView;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.metadata.Metadata;

public class MetadataTable extends NodeTreeTable {

    public MetadataTable() {
        this((TreeTable) null);
    }

    public MetadataTable(Metadata metadata) {
        this(MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.NON_EMPTY));
    }

    public MetadataTable(File file) {
        this((TreeTable) null);
        Task<TreeTable> task = new Task<TreeTable>() {

            @Override
            protected TreeTable call() throws DataStoreException {
                DataStore ds = DataStores.open(file);
                return MetadataStandard.ISO_19115.asTreeTable(ds.getMetadata(), Metadata.class, ValueExistencePolicy.NON_EMPTY);
            }
        };
        task.setOnSucceeded(wse -> setTreeTable(task.getValue()));
        task.setOnRunning(wse -> {
            System.out.println("started extracting metadata");
            setPlaceholder(new ProgressIndicator(-1));
        });
        task.setOnFailed(wse -> {
            System.out.println("failed extracting metadata");
            final TextArea textArea = new TextArea(task.getException().toString());
            textArea.setEditable(false);
            setPlaceholder(textArea);
        });
        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }

    public MetadataTable(TreeTable treeTable) {
        super(treeTable);
        valueColumn.setCellFactory(cdf -> new MetadataView.MetadataCell());
        setTableMenuButtonVisible(true);
    }

}
