package sis.client;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TableColumn;
import static org.apache.sis.util.collection.TableColumn.NAME;
import static org.apache.sis.util.collection.TableColumn.TYPE;
import static org.apache.sis.util.collection.TableColumn.VALUE;
import static org.apache.sis.util.collection.TableColumn.VALUE_AS_TEXT;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.metadata.Metadata;
import org.opengis.util.ControlledVocabulary;

/**
 *
 * @author Siddhesh Rane
 */
public class MetadataView extends StackPane {

    TreeTable metadata;
    TreeTableView<TreeTable.Node> treeTableView = new TreeTableView<>();

    public MetadataView(TreeTable metadata) {
        this.metadata = metadata;
        getChildren().add(treeTableView);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        treeTableView.setTableMenuButtonVisible(true);
        if (metadata != null) {
            populateTreeTableView(metadata);
        }
    }

    /**
     * Create MetadataView from metadata contained in {@linkplain File}.
     * Metadata support depends on available storage providers at runtime.
     *
     * @param file The file containing metadata
     */
    public MetadataView(File file) {
        this((TreeTable) null);
        Runnable metadataRetriever = new Runnable() {
            @Override
            public void run() {
                TreeTable tree;
                try (DataStore ds = DataStores.open(file)) {
                    Metadata metadata = ds.getMetadata();
                    tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.COMPACT);
                    Platform.runLater(() -> populateTreeTableView(tree));
                } catch (DataStoreException ex) {
                }
            }
        };
        new Thread(metadataRetriever).start();
    }

    private void populateTreeTableView(TreeTable treeTable) {
        List<TableColumn<?>> columns = treeTable.getColumns();

        //NAME column
        TreeTableColumn<TreeTable.Node, Object> nameColumn = new TreeTableColumn<>(NAME.getHeader().toString());
        nameColumn.setCellValueFactory((param) -> {
            Object value = param.getValue().getValue().getValue(NAME);
            value = value == null ? "" : value;
            return new SimpleObjectProperty(value);
        });
        treeTableView.getColumns().add(nameColumn);

        //TEXT column
        if (columns.contains(VALUE_AS_TEXT)) {
            TreeTableColumn<TreeTable.Node, Object> textColumn = new TreeTableColumn<>(VALUE_AS_TEXT.getHeader().toString());
            textColumn.setCellValueFactory((param) -> {
                Object value = param.getValue().getValue().getValue(VALUE_AS_TEXT);
                value = value == null ? "" : value;
                return new SimpleObjectProperty(value);
            });
            treeTableView.getColumns().add(textColumn);
        }

        //VALUE column
        if (columns.contains(VALUE)) {
            TreeTableColumn<TreeTable.Node, Object> valueColumn = new TreeTableColumn<>(VALUE.getHeader().toString());
            valueColumn.setCellValueFactory((param) -> {
                Object value = param.getValue().getValue().getValue(VALUE);
                value = value == null ? "" : value;
                return new SimpleObjectProperty(value);
            });
            valueColumn.setCellFactory((param) -> {
                return new MetadataCell();
            });
            treeTableView.getColumns().add(valueColumn);
        }

        if (columns.contains(TYPE)) {
            TreeTableColumn<TreeTable.Node, Object> typeColumn = new TreeTableColumn<>(TYPE.getHeader().toString());
            typeColumn.setCellValueFactory((param) -> {
                String type = param.getValue().getValue().getValue(TYPE).toString();
                Object value = param.getValue().getValue().getUserObject();
                if (value != null) {
                    type += '\n'+value.getClass().toString();
                }
                return new SimpleObjectProperty(type);
            });
            treeTableView.getColumns().add(typeColumn);
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

    private static class MetadataCell extends TreeTableCell<TreeTable.Node, Object> {

        public MetadataCell() {
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                setText("");
                return;
            }
            setText("");
            if (item instanceof ControlledVocabulary) {
                ControlledVocabulary vocab = (ControlledVocabulary) item;
                final ComboBox<ControlledVocabulary> comboBox
                        = new ComboBox(FXCollections.<ControlledVocabulary>observableArrayList(vocab.family()));
                comboBox.setConverter(new StringConverter<ControlledVocabulary>() {
                    @Override
                    public String toString(ControlledVocabulary object) {
                        return object.name();
                    }

                    @Override
                    public ControlledVocabulary fromString(String string) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                });
                comboBox.getSelectionModel().select(vocab);
                setGraphic(comboBox);
            } else if (item instanceof Boolean) {
                final CheckBox checkBox = new CheckBox();
                checkBox.setSelected((Boolean) item);
                setGraphic(checkBox);
            } else if (item instanceof Double) {
                setGraphic(new Spinner(Double.MIN_VALUE, Double.MAX_VALUE, (double) item));
            } else if (item instanceof Integer) {
                setGraphic(new Spinner(Integer.MIN_VALUE, Integer.MAX_VALUE, (int) item));
            } else if (item instanceof Date) {
                Date d = (Date) item;
                LocalDate date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                DatePicker datePicker = new DatePicker(date);
                setGraphic(datePicker);
            } else {
                setGraphic(null);
                setText(item.toString());
            }
        }

    }
}
