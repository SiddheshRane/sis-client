package sis.client.dnd;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class DndController implements Initializable {

    @FXML
    private StackPane dragPane;
    @FXML
    private Pane itemListing;
    @FXML
    private Text message;
    @FXML
    Button addNew;
    FadeTransition fadeTransition;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fadeTransition = new FadeTransition(Duration.millis(200), message);
        addNew.setOnAction(me->{
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Well KNown Text", "*.wkt"));
            File file = fileChooser.showOpenDialog(dragPane.getScene().getWindow());
            addFiles(Collections.singletonList(file));
        });
    }

    @FXML
    void onDragEntered(DragEvent de) {
        dragPane.getStyleClass().add("drag-hover");
        de.consume();
    }

    @FXML
    void onDragExited(DragEvent de) {
        dragPane.getStyleClass().remove("drag-hover");
        de.consume();
    }

    @FXML
    void onDragOver(DragEvent de) {
        Dragboard dragboard = de.getDragboard();
        if (dragboard.hasFiles()) {
            de.acceptTransferModes(TransferMode.ANY);
            de.consume();
        }
    }

    @FXML
    void onDragDropped(DragEvent de) {
        Dragboard dragboard = de.getDragboard();
        if (dragboard.hasFiles()) {
            List<File> files = dragboard.getFiles();
            addFiles(files);
            de.setDropCompleted(true);
            de.consume();
        }
    }

    public void addFiles(List<File> files) {
        for (File file : files) {
            String mime = "N/A";
            try {
                mime = DataStores.probeContentType(file);
                if (mime == null) {
                    mime = Files.probeContentType(file.toPath());
                }
            } catch (DataStoreException ex) {
                mime = "N/A";
            } catch (IOException ex) {
                Logger.getLogger(DndController.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            Node vBox = getFileNode(file, mime);
            itemListing.getChildren().add(vBox);
        }
    }

    private Node getFileNode(File file, String mime) {
        final Label icon = AwesomeDude.createIconLabel(AwesomeIcon.FILE, "45");
        icon.getStyleClass().add("icon");
        Text filename = new Text(file.getName());
        filename.getStyleClass().add("filename");
        Text type = new Text(mime);
        type.getStyleClass().add("mime");
        VBox vBox = new VBox(icon, filename, type);
        vBox.getStyleClass().add("file-vbox");
        return vBox;
    }

}
