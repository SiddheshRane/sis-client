package sis.client.dnd;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Callback;

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

    private final ObservableSet<File> draggedFiles = FXCollections.observableSet();
    private final ObservableMap<File, Node> fileNodeMap = FXCollections.observableHashMap();
    private final ObjectProperty<Callback<File, Node>> fileNodeFactory = new SimpleObjectProperty<>();

    public Callback<File, Node> getFileNodeFactory() {
        return fileNodeFactory.get();
    }

    public void setFileNodeFactory(Callback<File, Node> value) {
        fileNodeFactory.set(value);
    }

    public ObjectProperty fileNodeFactoryProperty() {
        return fileNodeFactory;
    }

    public DndController() {

        draggedFiles.addListener(new SetChangeListener<File>() {
            @Override
            public void onChanged(SetChangeListener.Change<? extends File> change) {
                if (change.wasAdded()) {
                    File added = change.getElementAdded();
                    fileNodeMap.put(added, fileNodeFactory.get().call(added));
                }
                if (change.wasRemoved()) {
                    File removed = change.getElementRemoved();
                    fileNodeMap.remove(removed);
                }
            }
        });
        fileNodeMap.addListener(new MapChangeListener<File, Node>() {
            @Override
            public void onChanged(MapChangeListener.Change<? extends File, ? extends Node> change) {
                if (change.wasAdded()) {
                    final Node valueAdded = change.getValueAdded();
                    itemListing.getChildren().add(valueAdded);
                }
                if (change.wasRemoved()) {
                    Node removedNode = change.getValueRemoved();
                    itemListing.getChildren().remove(removedNode);
                }
            }
        });
    }

    /**
     * Get an @code ObservableSet of files that have been dragged to this dnd
     * pane. The returned set is mutable and live.
     *
     * @return mutable observable set of files
     */
    public ObservableSet<File> getDraggedFiles() {
        return draggedFiles;
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
            getDraggedFiles().addAll(files);
            de.setDropCompleted(true);
            de.consume();
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addNew.setOnAction(me -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Well Known Text", "*.wkt"),
                    new FileChooser.ExtensionFilter("NetCDF", "*.nc"),
                    new FileChooser.ExtensionFilter("Any type", "*")
            );
            File file = fileChooser.showOpenDialog(dragPane.getScene().getWindow());
            if (file != null) {
                getDraggedFiles().add(file);
            }
        });
    }

}
