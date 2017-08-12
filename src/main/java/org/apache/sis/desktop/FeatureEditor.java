package org.apache.sis.desktop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import org.apache.sis.desktop.crs.CRSEditor;
import org.apache.sis.internal.storage.FeatureStore;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

/**
 *
 * @author Siddhesh Rane
 */
public class FeatureEditor extends TabPane implements Initializable {

    @FXML
    private TableView waypoints;
    @FXML
    private WaypointTableController waypointsController;

    public FeatureEditor(File file) {
        loadFXML();
        Runnable featureLoader = () -> {
            try (DataStore datastore = DataStores.open(file);) {

                if (!(datastore instanceof FeatureStore)) {
                    System.out.println(file.getName() + " not a feature store");
                    return;
                }
                FeatureStore store = (FeatureStore) datastore;
                Stream<Feature> features = store.features();
                List<Feature> list = features.filter((t) -> {
                    return t.getType().getName().toString().contains("WayPoint");
                }).collect(Collectors.toList());
                System.out.println("list = " + list);
                Platform.runLater(() -> waypointsController.getWaypoints().setAll(list));
            } catch (DataStoreException ex) {
                System.out.println("ex = " + ex);
                Logger.getLogger(FeatureEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        Thread thread = new Thread(featureLoader);
        thread.setDaemon(true);
        thread.start();
    }

    public FeatureEditor(FeatureStore store) {
        loadFXML();
    }

    protected void loadFXML() {
        FXMLLoader loader = new FXMLLoader(FeatureEditor.class.getResource("FeatureEditor.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            getChildren().add(new Text("Could not load"));
        }
    }

    public static void loadFile(File file) {
        try (DataStore datastore = DataStores.open(file);) {

            if (!(datastore instanceof FeatureStore)) {
                System.out.println(file.getName() + " not a feature store");
                return;
            }
            FeatureStore store = (FeatureStore) datastore;
            Stream<Feature> features = store.features();
            for (Iterator<Feature> iterator = features.iterator(); iterator.hasNext();) {
                Feature f = iterator.next();
                FeatureType type = f.getType();
                Collection<? extends PropertyType> columns = type.getProperties(true);
                System.out.println("{" + type.getName());
                for (PropertyType column : columns) {
                    Object propertyValue = f.getPropertyValue(column.getName().toString());
                    if (propertyValue != null) {
                        System.out.println(" " + column.getName().toString() + " === " + propertyValue + " " + propertyValue.getClass());
                    }
                }
                System.out.println("}");
            }
        } catch (DataStoreException ex) {
            Logger.getLogger(FeatureEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        waypoints.setPlaceholder(new ProgressIndicator(-1));
    }
}
