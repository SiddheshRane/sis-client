package sis.client;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
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

    public MetadataView(File file) {
        this.file = file;
        executor = Executors.newCachedThreadPool();
        getChildren().add(textArea);
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
//                    text = traverseMap(map);

                } catch (DataStoreException ex) {
                    Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
                    text = ex.getMessage();
                }
                final String finalText = text;
                Platform.runLater(() -> textArea.setText(finalText));
            }
        };
        executor.submit(metadataRetriever);
    }
    public final ExecutorService executor;

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
