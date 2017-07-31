package sis.client;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyEvent;
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
import sis.client.metadata.GeographicExtentBox;
import sis.client.metadata.IdentifierBox;
import sis.client.metadata.SummaryView;
import sis.client.metadata.VerticalExtentBox;

/**
 *
 * @author Siddhesh Rane
 */
public class MetadataView extends VBox {

    public static final Predicate<TreeTable.Node> NON_EMPTY_LEAF = (t) -> {
        return !t.isLeaf() || t.getValue(VALUE) != null;
    };
    public static final Predicate<TreeTable.Node> EXPAND_SINGLE_CHILD = node -> {
        return node.getChildren().size() == 1 || node.getParent() == null;
    };

    public static final String getIdentifierElseName(TreeTable.Node node) {
        String name = node.getValue(NAME).toString();
        String id = node.getValue(IDENTIFIER);
        return id == null ? name : id;
    }

    Preferences rootprefs;
    Preferences currentConfig;
    TreeTable metadata;
    TreeTableView<TreeTable.Node> treeTableView = new TreeTableView<>();
    private Button configSave;
    private ComboBox<String> prefBox;
    private ToggleButton showEmptyFields;
    private  HBox controlsBox;

    private ContextMenu contextMenu;

    private TreeTableColumn<TreeTable.Node, Object> typeColumn;
    private TreeTableColumn<TreeTable.Node, String> nameColumn;
    private TreeTableColumn<TreeTable.Node, Object> idColumn;

    /**
     * Returns true if a {@linkplain TreeTable.NODE} is already covered by a
     * custom widget, hence a separate {@link TreeItem} need not be created for
     * it. You can chain this predicate to add your extra rules.
     */
    public final Predicate<TreeTable.Node> notCoveredByCustomWidget = new Predicate<TreeTable.Node>() {
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

    private final Set<String> shownNodes = new HashSet<>();
    private Predicate<TreeTable.Node> showNode = (t) -> {
        return shownNodes.contains(getIdentifierElseName(t));
    };
    private ObjectProperty<Predicate<TreeTable.Node>> showNodeProperty = new SimpleObjectProperty<>((t) -> true);

    private Map<String, Integer> userSortTable = new HashMap<>();
    Comparator<TreeTable.Node> userSortOrder = Comparator.comparingInt((node) -> {
        String key = getIdentifierElseName(node);
        return userSortTable.getOrDefault(key, Integer.MAX_VALUE);
    });
    private ObjectProperty<Comparator<TreeTable.Node>> comparator = new SimpleObjectProperty<>(userSortOrder);
    private final Set<String> expandSet = new HashSet<>();
    private SimpleObjectProperty<Predicate<TreeTable.Node>> expandNodeProperty;

    public MetadataView(TreeTable metadata) {
        this.metadata = metadata;
        rootprefs = Preferences.userNodeForPackage(MetadataView.class);
        expandNodeProperty = new SimpleObjectProperty<>(EXPAND_SINGLE_CHILD);
        expandNodeProperty.addListener((observable, oldValue, newValue) -> {
            expandNodes(treeTableView.getRoot());
        });

        prefBox = new ComboBox<>();
        prefBox.setEditable(true);
        configSave = new Button("Save Config");
        configSave.setTooltip(new Tooltip("Save the current config info to preferences file. On linux it is available at ${user.home}/.java/.userPrefs/" + MetadataView.class.getPackage().getName()));
        controlsBox = new HBox(configSave, prefBox);
        controlsBox.setSpacing(5);
        controlsBox.setPadding(new Insets(5));
        getChildren().addAll(controlsBox, treeTableView);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        treeTableView.setTableMenuButtonVisible(true);
        treeTableView.addEventHandler(KeyEvent.KEY_PRESSED,
                                      ke -> {
                                          //Row reordering by shift+arrow keys
                                          if (!ke.isShiftDown() || !ke.getCode().isArrowKey()) {
                                              return;
                                          }
                                          TreeItem<TreeTable.Node> item = treeTableView.getSelectionModel().getSelectedItem();
                                          TreeItem<TreeTable.Node> parent = item.getParent();
                                          if (parent == null) {
                                              return;
                                          }
                                          switch (ke.getCode()) {
                                              case UP:
                                              case KP_UP:
                                                  TreeItem<TreeTable.Node> previousSibling = item.previousSibling();
                                                  if (previousSibling == null) {
                                                      return;
                                                  }
                                                  parent.getChildren().remove(previousSibling);
                                                  int insertHere = parent.getChildren().indexOf(item);
                                                  parent.getChildren().add(insertHere + 1, previousSibling);
                                                  treeTableView.getFocusModel().focusPrevious();
                                                  break;
                                              case DOWN:
                                              case KP_DOWN:
                                                  TreeItem<TreeTable.Node> nextSibling = item.nextSibling();
                                                  if (nextSibling == null) {
                                                      return;
                                                  }
                                                  parent.getChildren().remove(item);
                                                  int index = parent.getChildren().indexOf(nextSibling);
                                                  parent.getChildren().add(index + 1, item);
                                                  treeTableView.getFocusModel().focusNext();
                                                  break;
                                          }
//                                             int itemIndex = treeTableView.getSelectionModel().getSelectedIndex();
                                          ke.consume();
                                      });
        MenuItem flatten = new MenuItem("Flatten sub tree");
        flatten.setOnAction(ae -> {
            TreeItem<TreeTable.Node> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
            if (selectedItem.getParent() == null || selectedItem.getChildren().isEmpty()) {
                return;
            }
            int indexOf = selectedItem.getParent().getChildren().indexOf(selectedItem);
            selectedItem.getParent().getChildren().addAll(indexOf, selectedItem.getChildren());
            selectedItem.getParent().getChildren().remove(selectedItem);
        });
        MenuItem hide = new MenuItem("Hide");
        hide.setOnAction(ae -> {
            TreeItem<TreeTable.Node> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
            if (selectedItem.getParent() != null) {
                selectedItem.getParent().getChildren().remove(selectedItem);
            }
        });
        contextMenu = new ContextMenu(flatten, hide);
        treeTableView.setContextMenu(contextMenu);

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
                    tree = MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.ALL);
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
            idColumn = new TreeTableColumn<>(IDENTIFIER.getHeader().toString());
            idColumn.setCellValueFactory((param) -> {
                Object value = param.getValue().getValue().getValue(IDENTIFIER);
                value = value == null ? "" : value;
                return new SimpleObjectProperty(value);
            });
            treeTableView.getColumns().add(idColumn);
        }

        //NAME column
        nameColumn = new TreeTableColumn<>(NAME.getHeader().toString());
        nameColumn.setCellValueFactory((param) -> {
            String value = param.getValue().getValue().getValue(NAME).toString();
            value = value == null ? "" : value;
            return new SimpleStringProperty(value);
        });
        nameColumn.setCellFactory((param) -> {
            return new NameCell();
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
            typeColumn = new TreeTableColumn<>(TYPE.getHeader().toString());
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
        loadConfigs();
        if (metadata.getRoot().getUserObject() instanceof Metadata) {
            SummaryView summaryView = new SummaryView(metadata);

            ToggleButton summaryToggle = new ToggleButton("Summary");
            summaryToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    getChildren().remove(treeTableView);
                    getChildren().add(1, summaryView);
                } else {
                    getChildren().remove(summaryView);
                    getChildren().add(1, treeTableView);
                }
            });
            
            controlsBox.getChildren().add(summaryToggle);
            summaryToggle.setSelected(true);
        }
    }

    private void updateRoot(TreeTable treeTable) {
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
                if (childItem != null) {
                    rootItem.getChildren().add(childItem);
                }
            }
        }
        return rootItem;
    }

    private void createTreeItems(TreeItem<TreeTable.Node> rootItem) {
        Collection<TreeTable.Node> children = rootItem.getValue().getChildren();

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
        List<TreeTable.Node> transformed = root.getValue().getChildren().stream()
                //                .filter(notCoveredByCustomWidget)
                .filter(NON_EMPTY_LEAF)
                .filter(showNodeProperty().get())
                .collect(Collectors.toList());
        return transformed;
    }

    /**
     * A property containing predicate that returns true if the given
     * {@link TreeTable.Node} must be shown as a {@link TreeItem} in the
     * {@link TreeTableView}. By default return true for all nodes unless
     * preferences are loaded
     *
     * @return The property containing the expansion predicate
     */
    public ObjectProperty<Predicate<TreeTable.Node>> showNodeProperty() {
        return showNodeProperty;
    }

    public Predicate<TreeTable.Node> getShowNode() {
        return showNodeProperty.get();
    }

    public void setShowNode(Predicate<TreeTable.Node> showNode) {
        showNodeProperty.set(showNode);
    }

    public ObjectProperty<Comparator<TreeTable.Node>> comparatorProperty() {
        return comparator;
    }

    public Comparator<TreeTable.Node> getComparator() {
        return comparatorProperty().get();
    }

    public void setComparator(Comparator<TreeTable.Node> com) {
        comparatorProperty().set(com);
    }

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
        String[] childrenNames = new String[]{};
        try {
            childrenNames = rootprefs.childrenNames();
        } catch (BackingStoreException ex) {
            Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (childrenNames.length == 0) {
            updateRoot(metadata);
            String rootName = metadata.getRoot().getValue(NAME).toString();
            saveConfig(rootName);
            childrenNames = new String[]{rootName};
        }
        prefBox.getItems().setAll(childrenNames);
        prefBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (prefBox.getItems().contains(newValue)) {
                loadConfig(newValue);
            } else {
                saveConfig(newValue);
            }
        });
        String rootName = metadata.getRoot().getValue(NAME).toString();
        if (metadata != null && prefBox.getItems().contains(rootName)) {
            prefBox.getSelectionModel().select(rootName);
        } else {
            prefBox.getSelectionModel().selectFirst();
        }
        configSave.setOnAction((event) -> saveConfig(prefBox.getValue()));
    }

    private void loadConfig(String name) {
        if (name == null) {
            return;
        }
        currentConfig = rootprefs.node(name);
        try {
            final String[] keys = currentConfig.keys();
            if (keys.length == 0) {

            }
            expandSet.clear();
            userSortTable.clear();
            shownNodes.clear();
            for (String key : keys) {
                String value = currentConfig.get(key, "+ v 0");
                String[] split = value.split(" ");
                if ("+".equals(split[0])) {
                    expandSet.add(key);
                }
                if ("v".equals(split[1])) {
                    shownNodes.add(key);
                }
                int sortPosn = Integer.parseInt(split[2]);
                userSortTable.put(key, sortPosn);
            }
            updateRoot(metadata);
        } catch (BackingStoreException ex) {
            Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveConfig(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Preferences pref = rootprefs.node(name);
        currentConfig = pref;
        try {
            pref.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
        }
        addToPref(pref, treeTableView.getRoot(), 0);
    }

    private void addToPref(Preferences pref, TreeItem<TreeTable.Node> rootItem, int count) {
        TreeTable.Node root = rootItem.getValue();
        char expanded = rootItem.isExpanded() ? '+' : '-';
        String key = getIdentifierElseName(root);
        pref.put(key, expanded + " v " + count);
        if (!rootItem.isLeaf()) {
            for (TreeItem<TreeTable.Node> child : rootItem.getChildren()) {
                count++;
                addToPref(pref, child, count);
            }
        }
    }

    private void recordConfig() {

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
                VerticalExtentBox verticalExtent = new VerticalExtentBox(extent);
                setGraphic(verticalExtent);
            } else if (item instanceof GeographicBoundingBox) {
                GeographicBoundingBox extent = (GeographicBoundingBox) item;
                GeographicExtentBox geoExtent = new GeographicExtentBox(extent);
                setGraphic(geoExtent);
            } else if (item instanceof Identifier) {
                Identifier id = (Identifier) item;
                IdentifierBox identifierBox = new IdentifierBox(id);
                setGraphic(identifierBox);
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
                    final TextArea textArea = new TextArea(item.toString());
                    long newLines = item.toString().chars().filter((ch) -> {
                        return ch == '\n';
                    }).count();
                    textArea.setPrefRowCount((int) (newLines));
                    textArea.setWrapText(true);
                    textArea.setMaxHeight(200);
                    textArea.setMinHeight(20);
                    textArea.setPrefHeight(USE_COMPUTED_SIZE);
                    setGraphic(textArea);
                }
            }
        }

    }

    private static class NameCell extends TreeTableCell<TreeTable.Node, String> {

        private final Label drag;
        private final Label visible;

        private HBox hBox;

        public NameCell() {
            visible = AwesomeDude.createIconLabel(AwesomeIcon.EYE);
            visible.getStyleClass().add("eye");
            drag = AwesomeDude.createIconLabel(AwesomeIcon.ARROWS);
            drag.getStyleClass().add("hand");
            hBox = new HBox(visible, drag);
            hBox.setAlignment(Pos.CENTER_RIGHT);
            setGraphic(hBox);
            setContentDisplay(ContentDisplay.RIGHT);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(hBox);
            }
            setText(item);
        }
    }
}
