package org.apache.sis.desktop;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import org.apache.sis.internal.storage.FeatureStore;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TableColumn;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

/**
 *
 * @author Siddhesh Rane
 */
public class VectorEditor extends StackPane {

    TableView<Feature> table;

    public VectorEditor() {
        table = new TableView<>();
        getChildren().add(table);
    }

    private void getColumnFor(PropertyType prop) {

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
                System.out.println("{"+type.getName());
                for (PropertyType column : columns) {
                    Object propertyValue = f.getPropertyValue(column.getName().toString());
                    System.out.println(" "+column.getName().toString() + " === " + propertyValue);
                }
                System.out.println("}");
            }
        } catch (DataStoreException ex) {
            Logger.getLogger(VectorEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
