package org.apache.sis.desktop;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.Preferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.sis.desktop.metadata.NodeTreeTable;
import static org.apache.sis.util.collection.TableColumn.IDENTIFIER;
import static org.apache.sis.util.collection.TableColumn.NAME;
import org.apache.sis.util.collection.TreeTable;

/**
 * Stores configuration of {@link NodeTreeTable } like sort order, hidden nodes,
 * default expanded nodes.
 *
 * @author Siddhesh Rane
 */
public class Config implements NodeChangeListener {

    /**
     * When the configuration for a node does not exist it is by default added
     * to the end of the table, expanded by default.
     */
    private static final Data SHOW_ALL = new Data(Integer.MAX_VALUE, true, true);
    private static final Data HIDE_MISSING = new Data(Integer.MAX_VALUE, false, false);
    private Data DEFAULT_BEHAVIOUR = SHOW_ALL;
    
    /*Default empty configuration that displays all data*/
    public static final String DEFAULT_CONFIG = "Default";

    /**
     * All configuration will be stored under org.apache.sis.desktop namespace.
     */
    private final Preferences rootPreferences;
    private final NodeTreeTable table;

    private final Map<String, Data> map = new HashMap<>();

    private final Predicate<TreeTable.Node> createTreeItemForNode = t -> map.getOrDefault(getNodePath(t), DEFAULT_BEHAVIOUR).createTreeItem;
    private ObjectProperty<Predicate<TreeTable.Node>> createTreeItemForNodeProperty = new SimpleObjectProperty<>(createTreeItemForNode);
    public ObjectProperty<Predicate<TreeTable.Node>> createTreeItemForNodeProperty() {
        return createTreeItemForNodeProperty;
    }

    private final Comparator<TreeTable.Node> userSortOrder = Comparator.comparingInt(node -> map.getOrDefault(getNodePath(node), DEFAULT_BEHAVIOUR).position);
    private ObjectProperty<Comparator<TreeTable.Node>> comparator = new SimpleObjectProperty<>(userSortOrder);

    private final Predicate<TreeTable.Node> expandNodeByDefault = t -> map.getOrDefault(getNodePath(t), DEFAULT_BEHAVIOUR).expandBydefault;
    private SimpleObjectProperty<Predicate<TreeTable.Node>> expandNodeProperty = new SimpleObjectProperty<>(expandNodeByDefault);

    private final ReadOnlyObjectWrapper<Boolean> configDirty = new ReadOnlyObjectWrapper<>(false);
    public ReadOnlyObjectProperty<Boolean> configDirtyProperty() {
        return configDirty.getReadOnlyProperty();
    }
    public boolean isConfigDirty() {
        return configDirty.get();
    }

    private ObservableList<String> configurations = FXCollections.observableArrayList(DEFAULT_CONFIG);
    /**
     * Returns an unmodifiable list of configurations under the root preferences
     * by their name.
     *
     * @return
     */
    public ObservableList<String> getAvailableConfigurations() {
        return (configurations);
    }

    private ReadOnlyObjectWrapper<String> currentConfig = new ReadOnlyObjectWrapper<>(DEFAULT_CONFIG);
    public ReadOnlyObjectProperty<String> currentConfigProperty() {
        return currentConfig.getReadOnlyProperty();
    }
    public String getCurrentConfig() {
        return currentConfig.get();
    }

    /**
     * @param rootPreferences existing and new configurations will be stored as
     *                        children of this
     * @param table           the table whose state is to be observed
     */
    public Config(Preferences rootPreferences, NodeTreeTable table) {
        this.rootPreferences = rootPreferences;
        this.table = table;
        loadChildren();
        rootPreferences.addNodeChangeListener(this);
        table.setCreateTreeItemForNode(createTreeItemForNode);
        table.setExpandNode(expandNodeByDefault);
        table.setOrder(userSortOrder);
        table.getRoot().addEventHandler(TreeItem.treeNotificationEvent(), e -> configDirty.set(true));
    }

    private void loadChildren() {
        try {
            String[] childrenNames = rootPreferences.childrenNames();
            for (String childrenName : childrenNames) {
                configurations.add(childrenName);
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void loadConfig(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        if (name == DEFAULT_CONFIG) {
            DEFAULT_BEHAVIOUR = SHOW_ALL;
            map.clear();
            table.updateRoot();
            configDirty.set(Boolean.FALSE);
            return;
        }
        Preferences pref = rootPreferences.node(name);
        DEFAULT_BEHAVIOUR = HIDE_MISSING;
        map.clear();
        try {
            String[] keys = pref.keys();
            for (String key : keys) {
                Data data = new Data();
                String value = pref.get(key, "+ v 0");
                String[] split = value.split("\\s+");
                if ("+".equals(split[0])) {
                    data.expandBydefault = true;
                }
                if ("v".equals(split[1])) {
                    data.createTreeItem = true;
                }
                int sortPosn = Integer.parseInt(split[2]);
                data.position = sortPosn;
                map.put(key, data);
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        currentConfig.set(name);
        table.updateRoot();
        configDirty.set(Boolean.FALSE);
    }

    /**
     * Updates an existing preferences file or creates a new one with data from
     * current config.
     *
     * @param name Name of the preference file
     */
    public void saveConfig(String name) {
        if (name == null || name.isEmpty() || name == DEFAULT_CONFIG) {
            return;
        }
        Preferences pref = rootPreferences.node(name);
        try {
            pref.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Map.Entry<String, Data> entry : map.entrySet()) {
            String key = entry.getKey();
            char expandByDefault = entry.getValue().expandBydefault ? '+' : '-';
            char createTreeItem = entry.getValue().createTreeItem ? 'v' : 'h';
            int pos = entry.getValue().position;
            pref.put(key, expandByDefault + " " + createTreeItem + " " + pos);
        }
    }

    public void deleteConfig(String name) {
        if (name == null || name.isEmpty() || name == DEFAULT_CONFIG) {
            return;
        }
        try {
            rootPreferences.node(name).removeNode();
        } catch (BackingStoreException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        loadConfig(DEFAULT_CONFIG);
    }

    /**
     * Updates the current configuration with new state of TreeTableView.
     *
     */
    public void updateConfig() {
        map.clear();
        addVisibleNodesToMap(map, table.getRoot(), 0);
        addHiddenNodesToMap(map, table.getRoot().getValue(), 0);
        configDirty.set(Boolean.FALSE);
    }

    private void addVisibleNodesToMap(Map<String, Data> map, TreeItem<TreeTable.Node> rootItem, int count) {
        TreeTable.Node root = rootItem.getValue();
        String key = getNodePath(root);
        Data data = new Data(count, rootItem.isExpanded(), true);
        map.put(key, data);
        if (!rootItem.isLeaf()) {
            for (TreeItem<TreeTable.Node> child : rootItem.getChildren()) {
                count++;
                addVisibleNodesToMap(map, child, count);
            }
        }
    }

    private void addHiddenNodesToMap(Map<String, Data> map, TreeTable.Node root, int count) {
        String path = getNodePath(root);
        if (!map.containsKey(path)) {
            map.put(path, new Data(count, false, false));
        }
        if (root.isLeaf()) {
            return;
        }
        for (TreeTable.Node child : root.getChildren()) {
            addHiddenNodesToMap(map, child, ++count);
        }
    }

    /**
     * Returns the {@code IDENTIFIER} of the node, otherwise its {@code NAME}.
     *
     * @param node {@linkplain TreeTable.Node} having at least NAME field
     * @return identifier, otherwise name
     */
    public static final String getIdentifierElseName(TreeTable.Node node) {
        String name = node.getValue(NAME).toString();
        String id = node.getValue(IDENTIFIER);
        return id == null ? name : id;
    }

    public static final String getNodePath(TreeTable.Node node) {
        String path = "";
        do {
            path = getIdentifierElseName(node) + " " + path;
            node = node.getParent();
        } while (node != null && node.getParent() != null);
        return path;
    }

    @Override
    public void childAdded(NodeChangeEvent evt) {
        if (evt.getParent() == rootPreferences) {
            String name = evt.getChild().name();
            System.out.println("new node " + name);
            configurations.add(name);
        }
    }
    @Override
    public void childRemoved(NodeChangeEvent evt) {
        if (evt.getParent() == rootPreferences) {
            configurations.remove(evt.getChild().name());
        }
    }

    private static class Data {

        int position;
        boolean expandBydefault, createTreeItem;

        public Data() {
        }

        public Data(int position, boolean expandBydefault, boolean createTreeItem) {
            this.position = position;
            this.expandBydefault = expandBydefault;
            this.createTreeItem = createTreeItem;
        }
        @Override
        public String toString() {
            return (expandBydefault ? "+" : "-") + (createTreeItem ? "v" : "h") + position;
        }

    }
}
