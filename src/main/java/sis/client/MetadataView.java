package sis.client;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
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
import static org.apache.sis.util.collection.TableColumn.IDENTIFIER;
import static org.apache.sis.util.collection.TableColumn.NAME;
import static org.apache.sis.util.collection.TableColumn.TYPE;
import static org.apache.sis.util.collection.TableColumn.VALUE;
import static org.apache.sis.util.collection.TableColumn.VALUE_AS_TEXT;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.filter.capability.ComparisonOperators;
import org.opengis.metadata.Metadata;
import org.opengis.util.ControlledVocabulary;

/**
 *
 * @author Siddhesh Rane
 */
public class MetadataView extends StackPane {

    TreeTable metadata;
    TreeTableView<TreeTable.Node> treeTableView = new TreeTableView<>();

    public static final Predicate<TreeTable.Node> EXPAND_SINGLE_CHILD = node -> {
        return node.getChildren().size() == 1 || node.getParent() == null;
    };

    private SimpleObjectProperty<Predicate<TreeTable.Node>> expandNodeProperty;

    /**
     * A property containing predicate that returns true if the given
     * {@link TreeTable.Node} must be expanded in the {@link TreeTableView} to
     * show its children by default.
     *
     * @return The property containing the expansion predicate
     */
    public ObjectProperty<Predicate<TreeTable.Node>> expandNodeProperty() {
        return expandNodeProperty;
    }

    public Predicate<TreeTable.Node> getExpandNode() {
        return expandNodeProperty.get();
    }

    public void setExpandNode(Predicate<TreeTable.Node> expandNode) {
        expandNodeProperty.set(expandNode);
    }

    /**
     * Returns true if a TreeTable.Node's children must also be added to the
     * tree table.
     */
    Predicate<TreeTable.Node> createChildNodes = node -> true;

    Predicate<TreeTable.Node> coalesceSingleChildren = node -> true;

    public Predicate<TreeTable.Node> getCreateChildNodes() {
        return createChildNodes;
    }

    public void setCreateChildNodes(Predicate<TreeTable.Node> createChildNodes) {
        this.createChildNodes = createChildNodes;
    }

    public MetadataView(TreeTable metadata) {
        expandNodeProperty = new SimpleObjectProperty<>(EXPAND_SINGLE_CHILD);
        expandNodeProperty.addListener((observable, oldValue, newValue) -> {
            expandNodes(treeTableView.getRoot());
        });
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
                    tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.ALL);
                    Platform.runLater(() -> populateTreeTableView(tree));
                } catch (DataStoreException ex) {
                }
            }
        };
        final Thread thread = new Thread(metadataRetriever);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateTreeTableView(TreeTable treeTable) {
        List<TableColumn<?>> columns = treeTable.getColumns();
        //IDENTIFIER column
        if (columns.contains(IDENTIFIER)) {
            TreeTableColumn<TreeTable.Node, Object> idColumn = new TreeTableColumn<>(IDENTIFIER.getHeader().toString());
            idColumn.setCellValueFactory((param) -> {
                Object value = param.getValue().getValue().getValue(IDENTIFIER);
                value = value == null ? "" : value;
                return new SimpleObjectProperty(value);
            });
            treeTableView.getColumns().add(idColumn);
        }

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
                    type += '\n' + value.getClass().toString();
                }
                return new SimpleObjectProperty(type);
            });
            treeTableView.getColumns().add(typeColumn);
        }

        TreeItem<TreeTable.Node> rootItem = createTreeItem(treeTable.getRoot());
        rootItem.setExpanded(true);
        treeTableView.setRoot(rootItem);
    }

    private TreeItem<TreeTable.Node> createTreeItem(TreeTable.Node root) {
        TreeItem<TreeTable.Node> rootItem = new TreeItem<>(root);
        rootItem.setExpanded(getExpandNode().test(root));
        if (!root.isLeaf() && createChildNodes.test(root)) {
            Collection<TreeTable.Node> children = root.getChildren();
            List<TreeTable.Node> transformChildren = transformChildren(rootItem);
            for (TreeTable.Node child : transformChildren) {
                TreeItem<TreeTable.Node> childItem = createTreeItem(child);
                rootItem.getChildren().add(childItem);
            }
        }
        return rootItem;
    }

    /**
     * Given a root node return a list of its filtered and sorted children. The
     * original list should not be modified. A new list must be returned. This
     * list will not be modified and so an immutable or read only list can be
     * returned
     *
     * @param root
     * @return a list of transformed children
     */
    private List<TreeTable.Node> transformChildren(TreeItem<TreeTable.Node> root) {
        List<TreeTable.Node> transformed = root.getValue().getChildren().stream().filter(notCoveredByCustomWidget).filter(nonEmptyLeaf).collect(Collectors.toList());
        return transformed;
    }

    Predicate<TreeTable.Node> nonEmptyLeaf = (t) -> {
        return !t.isLeaf() || t.getValue(VALUE) != null;
    };
    Predicate<TreeTable.Node> notCoveredByCustomWidget = new Predicate<TreeTable.Node>() {
        @Override
        public boolean test(TreeTable.Node t) {
            String id = t.getValue(IDENTIFIER);
            if (id == null) {
                return true;
            }
            switch (id) {
                case "resolution":
                case "numberOfDimensions":
                    return false;
            }
            return true;
        }
    };

    private void expandNodes(TreeItem<TreeTable.Node> root) {
        if (root == null || root.isLeaf()) {
            return;
        }
        root.setExpanded(getExpandNode().test(root.getValue()));
        for (TreeItem<TreeTable.Node> child : root.getChildren()) {
            expandNodes(child);
        }
    }

    private static class MetadataCell extends TreeTableCell<TreeTable.Node, Object> {

        public MetadataCell() {
            setWrapText(true);
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
                final Spinner spinner = new Spinner(Double.MIN_VALUE, Double.MAX_VALUE, (double) item);
                spinner.setEditable(true);
                setGraphic(spinner);
            } else if (item instanceof Integer) {
                final Spinner spinner = new Spinner(Integer.MIN_VALUE, Integer.MAX_VALUE, (int) item);
                spinner.setEditable(true);
                setGraphic(spinner);
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
