package sis.client;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Accordion;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTableFormat;
import org.opengis.metadata.Metadata;

/**
 *
 * @author Siddhesh Rane
 */
public class MetadataView extends StackPane {

    TableView<String> table = new TableView<>();
    TextArea textArea = new TextArea();
    File file;

    Accordion accordion = new Accordion();
    TreeTableView<TreeTable.Node> treeTableView = new TreeTableView<>();

    public MetadataView(File file) {
        this.file = file;
        executor = Executors.newCachedThreadPool();

        treeTableView.setTableMenuButtonVisible(true);
        TitledPane textPane = new TitledPane("Text", textArea);
        TitledPane ttvPane = new TitledPane("Tree Table", treeTableView);
        ttvPane.setExpanded(true);
        accordion.getPanes().addAll(textPane, ttvPane);
        accordion.setExpandedPane(ttvPane);

        getChildren().add(accordion);
        textArea.setEditable(false);
        textArea.setPromptText("Loading metadata...");
        
        Runnable metadataRetriever = new Runnable() {
            @Override
            public void run() {
                String text;
                try (DataStore ds = DataStores.open(file)) {
                    Metadata metadata = ds.getMetadata();
                    Map<String, Object> map = MetadataStandard.ISO_19115.asValueMap(metadata, Metadata.class, KeyNamePolicy.UML_IDENTIFIER, ValueExistencePolicy.NON_EMPTY);
                    TreeTable tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.COMPACT);
                    final TreeTableFormat tf = new TreeTableFormat(Locale.getDefault(), TimeZone.getDefault());
                    tf.setColumns(TableColumn.NAME, TableColumn.VALUE);
                    text = tf.format(tree);
                } catch (DataStoreException ex) {
                    Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
                    text = ex.getMessage();
                }
                final String finalText = text;
                Platform.runLater(() -> textArea.setText(finalText));
            }
        };
        executor.submit(metadataRetriever);
        Runnable metadataRetriever1 = new Runnable() {
            @Override
            public void run() {
                TreeTable tree;
                try (DataStore ds = DataStores.open(file)) {
                    Metadata metadata = ds.getMetadata();
                    tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.COMPACT);
                    Platform.runLater(() -> populateTreeTableView(tree));
                } catch (DataStoreException ex) {
                    Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        executor.submit(metadataRetriever1);
    }
    public final ExecutorService executor;


    private void populateTreeTableView(TreeTable treeTable) {
        List<TableColumn<?>> columns = treeTable.getColumns();
        for (TableColumn<?> column : columns) {
            TreeTableColumn<TreeTable.Node, String> treeTableColumn = new TreeTableColumn<>(column.getHeader().toString());
            treeTableColumn.setCellValueFactory((param) -> {
                Object value = param.getValue().getValue().getValue(column);
                if (value == null) {
                    value = "";
                }
                return new SimpleStringProperty(value.toString());
            });
            treeTableView.getColumns().add(treeTableColumn);
        }
        TreeItem<TreeTable.Node> rootItem = createTreeItem(treeTable.getRoot());
        treeTableView.setRoot(rootItem);
    }

    private TreeItem<TreeTable.Node> createTreeItem(TreeTable.Node root) {
        TreeItem<TreeTable.Node> rootItem = new TreeItem<>(root);
        if (!root.isLeaf()) {
            Collection<TreeTable.Node> children = root.getChildren();
            for (TreeTable.Node child : children) {
                TreeItem<TreeTable.Node> childItem = createTreeItem(child);
                rootItem.getChildren().add(childItem);
            }
        }
        return rootItem;
    }

    public static String traverseMap(Map<String, Object> map) {
        String str = "";
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            str += key + "\t\t" + value.getClass().getSimpleName() + "\t" + value.toString() + "\n";
        }
        return str;
    }
}
