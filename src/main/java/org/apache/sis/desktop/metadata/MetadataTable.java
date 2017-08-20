package org.apache.sis.desktop.metadata;

import java.io.File;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import org.apache.sis.desktop.MetadataView;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.metadata.Metadata;

public class MetadataTable extends NodeTreeTable {

    Metadata metadata;
    DefaultMetadata editableMetadata;

    private MetadataTable() {
        valueColumn.setCellFactory(cdf -> new MetadataView.MetadataCell());
        setTableMenuButtonVisible(true);
        setEditable(true);
        setCreateTreeItemForNode(HIDE_EMPTY_LEAF.and(MetadataView.notCoveredByCustomWidget));
    }

    public MetadataTable(Metadata metadata) {
        this();
        this.metadata = metadata;
        editableMetadata = new DefaultMetadata(metadata);
        TreeTable tt = MetadataStandard.ISO_19115.asTreeTable(editableMetadata, Metadata.class, ValueExistencePolicy.NON_EMPTY);
        setTreeTable(tt);
    }

    public MetadataTable(File file) {
        this();
        Task<Metadata> task = new Task<Metadata>() {
            @Override
            protected Metadata call() throws DataStoreException {
                DataStore ds = DataStores.open(file);
                return ds.getMetadata();
            }
        };
        task.setOnSucceeded(wse -> {
            final Metadata value = task.getValue();
            metadata = value;
            editableMetadata = new DefaultMetadata(value);
            setTreeTable(MetadataStandard.ISO_19115.asTreeTable(editableMetadata, Metadata.class, ValueExistencePolicy.ALL));
        });
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

    @Override
    public void setTreeTable(TreeTable treeTable) {
        super.setTreeTable(treeTable);
        getColumns().remove(idColumn);
    }

}
