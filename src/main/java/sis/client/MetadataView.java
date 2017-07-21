package sis.client;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.util.ControlledVocabulary;
import org.opengis.util.InternationalString;
import sis.client.metadata.ControlledVocabularyBox;

/**
 *
 * @author Siddhesh Rane
 */
public class MetadataView extends VBox {

    Preferences rootprefs;
    Preferences filters;
    Preferences sort;
    Preferences currentConfig;
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

    public MetadataView(TreeTable metadata) {
        this.metadata = metadata;
        expandNodeProperty = new SimpleObjectProperty<>(EXPAND_SINGLE_CHILD);
        expandNodeProperty.addListener((observable, oldValue, newValue) -> {
            expandNodes(treeTableView.getRoot());
        });
        this.rootprefs = Preferences.userNodeForPackage(MetadataView.class);
        String[] childrenNames = {};
        try {
            childrenNames = rootprefs.childrenNames();
        } catch (BackingStoreException ex) {
            Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
        }

        ComboBox<String> prefBox = new ComboBox<>(FXCollections.observableArrayList(childrenNames));
        prefBox.getSelectionModel().selectFirst();
        Button configSave = new Button("Save Config");
        configSave.setTooltip(new Tooltip("Save the current config info to preferences file. On linux it is available at ${user.home}/.java/.userPrefs/" + MetadataView.class.getPackage().getName()));
        configSave.setOnAction((event)
                -> saveCurrentConfig());
        getChildren().addAll(new HBox(configSave, prefBox), treeTableView);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        treeTableView.setTableMenuButtonVisible(true);
        setVgrow(treeTableView, Priority.ALWAYS);
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
                    tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.NON_EMPTY);
                    MetadataView.this.metadata = tree;
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
            TreeTableColumn<TreeTable.Node, TreeTable.Node> valueColumn = new TreeTableColumn<>(VALUE.getHeader().toString());
            valueColumn.setCellValueFactory((param) -> {
                TreeTable.Node value = param.getValue().getValue();
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
        if (!root.isLeaf()) {
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
            //TODO: Replace this hard coded switch case with preferences
            switch (id) {
                case "resolution":
                case "numberOfDimensions":
                case "dimensionName":
                case "dimensionSize":
                case "date":
                case "dateType":
                case "westBoundLongitude":
                case "eastBoundLongitude":
                case "northBoundLatitude":
                case "southBoundLatitude":
                case "minimumValue":
                case "maximumValue":
                case "code":
                case "codeSpace":
                case "version":

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

    private void loadConfigs() {
        this.rootprefs = Preferences.userNodeForPackage(MetadataView.class);
    }

    private void saveCurrentConfig() {
        Preferences node = rootprefs.node(metadata.getRoot().getValue(NAME).toString());
        addToPref(node, metadata.getRoot(), 0);
    }

    private void addToPref(Preferences pref, TreeTable.Node root, int count) {
        String id = root.getValue(IDENTIFIER);
        String name = root.getValue(NAME).toString();
        String key = id == null ? name : id;
        pref.putInt(key, count);
        if (!root.isLeaf()) {
            for (TreeTable.Node child : root.getChildren()) {
                count++;
                addToPref(pref, child, count);
            }
        }
    }

    private static class MetadataCell extends TreeTableCell<TreeTable.Node, TreeTable.Node> {

        public MetadataCell() {
            setWrapText(true);
        }

        @Override
        protected void updateItem(TreeTable.Node node, boolean empty) {
            super.updateItem(node, empty);

            if (empty) {
                setGraphic(null);
                setText("");
                return;
            }
            Object value = node.getValue(VALUE);
            Object userObject = node.getUserObject();
            Object item = value == null ? userObject == null ? "" : userObject : value;
            setText("");
            if (item instanceof ControlledVocabulary) {
                ControlledVocabulary vocab = (ControlledVocabulary) item;
                ControlledVocabularyBox comboBox = new ControlledVocabularyBox(vocab);
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
            } else if (item instanceof CitationDate) {
                CitationDate d = (CitationDate) item;
                LocalDate date = d.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                DatePicker datePicker = new DatePicker(date);

                //TODO: Refactor all widgets into separate classes
                //TODO: Transfer styling info like spacing to css files
                ControlledVocabulary vocab = d.getDateType();
                ControlledVocabularyBox comboBox = new ControlledVocabularyBox(vocab);
                HBox hBox = new HBox(datePicker, comboBox);
                hBox.setSpacing(2);
                setGraphic(hBox);
            } else if (item instanceof VerticalExtent) {
                VerticalExtent extent = (VerticalExtent) item;
                Spinner<Double> min = new Spinner<>(Double.MIN_VALUE, Double.MAX_VALUE, extent.getMinimumValue());
                Spinner<Double> max = new Spinner<>(Double.MIN_VALUE, Double.MAX_VALUE, extent.getMaximumValue());
                HBox hBox = new HBox(min, max);
                hBox.setSpacing(3);
                setGraphic(hBox);
            } else if (item instanceof GeographicBoundingBox) {
                GeographicBoundingBox extent = (GeographicBoundingBox) item;
                Spinner north = new Spinner(-90, 90, extent.getNorthBoundLatitude());
                Spinner south = new Spinner(-90, 90, extent.getSouthBoundLatitude());
                Spinner east = new Spinner(-180, 180, extent.getEastBoundLongitude());
                Spinner west = new Spinner(-180, 180, extent.getWestBoundLongitude());
                north.setMaxWidth(120);
                south.setMaxWidth(120);
                east.setMaxWidth(120);
                west.setMaxWidth(120);
                north.setMinWidth(20);
                south.setMinWidth(20);
                east.setMinWidth(20);
                west.setMinWidth(20);
                Button fullscreen = new Button("•");
                BorderPane borderPane = new BorderPane(fullscreen, north, east, south, west);
                BorderPane.setAlignment(north, Pos.CENTER);
                BorderPane.setAlignment(south, Pos.CENTER);
                setGraphic(borderPane);
            } else if (item instanceof Identifier) {
                Identifier id = (Identifier) item;
                final String codeSpace = id.getCodeSpace();
                TextField codespace = new TextField(codeSpace + "");
                codespace.setPromptText("code space");
                final String codeString = id.getCode();
                TextField code = new TextField(codeString + "");
                code.setPromptText("code");
                final String versionString = id.getVersion();
                TextField version = new TextField(versionString + "");
                version.setPromptText("version");
                final InternationalString descriptionString = id.getDescription();
                TextField description = new TextField(descriptionString + "");
                description.setPromptText("description");
                HBox hBox = new HBox(codespace, code, version);
                hBox.setSpacing(2);
                VBox vBox = new VBox(hBox, description);
                vBox.setFillWidth(true);
                vBox.setSpacing(2);
                VBox.setVgrow(description, Priority.ALWAYS);
                setGraphic(vBox);
            } else if (item instanceof OnlineResource) {
                OnlineResource resource = (OnlineResource) item;
                InternationalString name = resource.getName();
                URI link = resource.getLinkage();
                String linktext = name == null ? link.toString() : name.toString();
                InternationalString description = resource.getDescription();
                Hyperlink hyperlink = new Hyperlink(linktext);
                if (description != null) {
                    hyperlink.setTooltip(new Tooltip(description.toString()));
                }
                hyperlink.setUserData(link);
                hyperlink.setOnAction(ae -> {
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        Thread thread = new Thread(() -> {
                            try {
                                desktop.browse(link);
                            } catch (IOException ex) {
                                Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
                        thread.setDaemon(true);
                        thread.start();
                    }
                });
                setGraphic(hyperlink);
            } else if (item instanceof Dimension) {
                Dimension d = (Dimension) item;
                ControlledVocabularyBox name = new ControlledVocabularyBox(d.getDimensionName());
                Spinner size = new Spinner(0, Integer.MAX_VALUE, d.getDimensionSize());
                HBox hBox = new HBox(size, name);
                hBox.setSpacing(2);
                setGraphic(hBox);
            } else if (item instanceof Keywords) {
                Keywords keywords = (Keywords) item;
                TextFlow flow = new TextFlow();
                for (InternationalString keyword : keywords.getKeywords()) {
                    Label text = new Label(keyword.toString());
                    text.setPadding(new Insets(4));
                    flow.getChildren().add(text);
                }
                ControlledVocabularyBox type = new ControlledVocabularyBox(keywords.getType());
                HBox hBox = new HBox(flow, type);
                setGraphic(hBox);
            } else {
                setGraphic(null);
                if (value == null) {
                    setText("");
                } else {
                    setText(item.toString());
                }
            }
        }

    }
}
