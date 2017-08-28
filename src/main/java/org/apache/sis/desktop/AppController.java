package org.apache.sis.desktop;

import org.apache.sis.desktop.metadata.MetadataTable;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.sis.desktop.about.AboutController;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.desktop.dnd.DndController;
import org.apache.sis.desktop.metadata.SummaryView;
import org.opengis.metadata.Metadata;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class AppController implements Initializable {

    @FXML
    BorderPane borderPane;
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem openMenu;
    @FXML
    private MenuItem aboutMenu;
    @FXML
    private VBox about;
    @FXML
    private AboutController aboutController;
    private Stage aboutWindow;

    @FXML
    TabPane tabPane;
    @FXML
    Tab fileTab;
    @FXML
    DndController dndController;

    public static final List<FileChooser.ExtensionFilter> EXTENSION_FILTERS = Collections.unmodifiableList(Arrays.asList(
            new FileChooser.ExtensionFilter("Well Known Text", "*.wkt"),
            new FileChooser.ExtensionFilter("NetCDF", "*.nc"),
            new FileChooser.ExtensionFilter("GPS Exchange Format", "*.gpx"),
            new FileChooser.ExtensionFilter("Any type", "*")));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dndController.setFileNodeFactory(this::createDisplayNode);
        dndController.getExtensionFilters().setAll(EXTENSION_FILTERS);
        openMenu.setOnAction(ae -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().setAll(EXTENSION_FILTERS);
            List<File> files = fileChooser.showOpenMultipleDialog(borderPane.getScene().getWindow());
            if (files != null) {
                dndController.getDraggedFiles().addAll(files);
                tabPane.getSelectionModel().select(fileTab);
            }
        });

        aboutWindow = new Stage(StageStyle.DECORATED);
        aboutWindow.setTitle("About");
        Scene scene = new Scene(about, 500, 500);
        aboutWindow.setScene(scene);
        aboutWindow.initModality(Modality.APPLICATION_MODAL);
    }

    @FXML
    private void showAboutWindow() {
        if (aboutWindow.isShowing()) {
            aboutWindow.requestFocus();
            return;
        }
        aboutWindow.show();
    }

    public AppController() {
        loadPreferences();
    }

    @FXML
    void onDragEntered(DragEvent de) {
        if (de.getDragboard().hasFiles() && de.getSource() != tabPane) {
            tabPane.getSelectionModel().select(fileTab);
        }
    }

    @FXML
    void onDragExited(DragEvent de) {

    }

    public static final Predicate<TreeTable.Node> METADATA_EXPANSION = node -> {
        CharSequence name = node.getValue(TableColumn.NAME);
        switch (name.toString()) {
            case "Metadata":
            case "Spatial representation info":
            case "Identification info":
            case "Extent":
            case "Geographic element":
            case "Vertical element":
                return true;
        }
        return false;
    };

    public void openMetadataTab(File file) {
        Tab tab = new Tab(file.getName());
        tab.setClosable(true);
        tabPane.getTabs().add(tab);

        Task<Metadata> task = new Task<Metadata>() {
            @Override
            protected Metadata call() throws DataStoreException {
                return DataStores.open(file).getMetadata();
            }
        };
        task.setOnRunning(e -> {
            tab.setContent(new ProgressIndicator(-1));
        });
        task.setOnSucceeded(e -> {
            Metadata mt = task.getValue();
            MetadataTable table = new MetadataTable(mt);
            SummaryView summary = new SummaryView(mt);
            ToggleButton summaryToggle = new ToggleButton("Summary");

            Config config = new Config(Preferences.userNodeForPackage(MainApp.class), table);
            ComboBox<String> prefBox = new ComboBox<>();
            prefBox.setEditable(true);
            Bindings.bindContent(prefBox.getItems(), config.getAvailableConfigurations());
            prefBox.setValue(config.getCurrentConfig());

            Button save = new Button("Save");
            save.disableProperty().bind(config.configDirtyProperty().isEqualTo(false));
            save.setOnAction(ae -> {
                config.updateConfig();
                config.saveConfig(prefBox.getValue());
            });
            
            Button delete = new Button("Delete");
            delete.disableProperty().bind(prefBox.valueProperty().isEqualTo(Config.DEFAULT_CONFIG));
            delete.setOnAction(ae-> config.deleteConfig(prefBox.getValue()));
            
            prefBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (prefBox.getItems().contains(newValue)) {
                    config.loadConfig(newValue);
                    System.out.println("Loaded config " + newValue);
                } else {
                    config.updateConfig();
                    config.saveConfig(newValue);
                    System.out.println("new config " + newValue + " saved");
                }
            });
            HBox hBox = new HBox(summaryToggle, prefBox, save, delete);
            hBox.getStyleClass().add("metadata-controls");
            VBox pane = new VBox(hBox, summary);
            VBox.setVgrow(table, Priority.ALWAYS);

            summaryToggle.setSelected(true);
            summaryToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    pane.getChildren().remove(table);
                    pane.getChildren().add(1, summary);
                } else {
                    pane.getChildren().add(1, table);
                    pane.getChildren().remove(summary);
                }
            });
            tab.setContent(pane);
        });
        task.setOnFailed(e -> tab.setContent(new TextArea(task.getException().toString())));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void openFeatureEditorTab(File file) {
        Tab tab = new Tab(file.getName() + " features");
        tab.setClosable(true);
        FeatureEditor vectorEditor = new FeatureEditor(file);
        tab.setContent(vectorEditor);
        tabPane.getTabs().add(tab);
    }

    private void loadPreferences() {
        //Here we should create default configurations
    }

    /**
     * This function generates a graphical {@code Node} to represent
     * {@code file} in the drag and drop pane.
     *
     * @param file the file that needs to be represented
     * @return a new node to represent file
     */
    private Node createDisplayNode(File file) {
        final Label icon = AwesomeDude.createIconLabel(AwesomeIcon.FILE, "45");
        icon.getStyleClass().add("icon");
        Text filename = new Text(file.getName());
        filename.getStyleClass().add("filename");
//        filename.setWrappingWidth(200);
        String mime = "N/A";
        try {
            mime = DataStores.probeContentType(file);
            if (mime == null) {
                mime = Files.probeContentType(file.toPath()) + '*';
            }
        } catch (DataStoreException ex) {
            mime = "N/A";
        } catch (IOException ex) {
            Logger.getLogger(DndController.class.getName()).log(Level.SEVERE, null, ex);
        }
        Text type = new Text(mime);
        type.getStyleClass().add("mime");
        VBox vBox = new VBox(icon, filename, type);
        vBox.getStyleClass().add("file-vbox");
        final MenuItem openMeta = new MenuItem("Open metadata");
        openMeta.setOnAction(ae -> openMetadataTab(file));
        MenuItem openFeatures = new MenuItem("check for features");
        openFeatures.setOnAction(ae -> openFeatureEditorTab(file));
        ContextMenu cm = new ContextMenu(openMeta, openFeatures);

        vBox.setOnContextMenuRequested(cme -> cm.show(vBox, cme.getScreenX(), cme.getScreenY()));
        return vBox;
    }
}
