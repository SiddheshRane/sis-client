package org.apache.sis.desktop;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.sis.setup.About;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.desktop.dnd.DndController;

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
    TabPane tabPane;
    @FXML
    Tab homeTab;
    @FXML
    Tab fileTab;

    Pane dndPane;
    @FXML
    DndController dndController;

    @FXML
    StackPane homePane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dndController.setFileNodeFactory(this::createDisplayNode);
    }

    @FXML
    private void showAboutWindow() {
        final TreeTable configuration = About.configuration();
        MetadataView sisabout = new MetadataView(configuration);
        sisabout.setExpandNode(new Predicate<TreeTable.Node>() {
            @Override
            public boolean test(TreeTable.Node node) {
                if (node.getParent() == null) {
                    return true;
                }
                CharSequence name = node.getValue(TableColumn.NAME);
                switch (name.toString()) {
                    case "Versions":
                    case "Localization":
                    case "Locale":
                    case "Timezone":
                        return true;
                }
                return false;
            }
        });
        
        Stage aboutWindow = new Stage(StageStyle.DECORATED);
        aboutWindow.setTitle("About");
        aboutWindow.setScene(new Scene(sisabout));
        aboutWindow.initModality(Modality.APPLICATION_MODAL);
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

    public void openMetadataTab(File file) {
        Tab tab = new Tab(file.getName());
        tab.setClosable(true);
        final MetadataView metadataView = new MetadataView(file);
        final Predicate<TreeTable.Node> METADATA_EXPANSION = node -> {
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
        metadataView.setExpandNode(METADATA_EXPANSION.or(MetadataView.EXPAND_SINGLE_CHILD));
        tab.setContent(metadataView);
        tabPane.getTabs().add(tab);
    }

    public void openFeatureEditorTab(File file){
        Tab tab = new Tab(file.getName()+" features");
        tab.setClosable(true);
        FeatureEditor vectorEditor = new FeatureEditor(file);
        tab.setContent(vectorEditor);
        tabPane.getTabs().add(tab);
    }
    
    private void loadPreferences() {
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
        filename.setWrappingWidth(100);
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
        openMeta.setOnAction(ae -> {
            openMetadataTab(file);
        });
        MenuItem openFeatures = new MenuItem("check for features");
        openFeatures.setOnAction(ae-> openFeatureEditorTab(file));
        ContextMenu cm = new ContextMenu(openMeta, openFeatures);

        vBox.setOnContextMenuRequested(cme -> {
            cm.show(vBox, cme.getScreenX(), cme.getScreenY());
        });
        return vBox;
    }

//    private void createCRSTable() {
//        FXCRSTable table = new FXCRSTable();
//        Tab tab = new Tab("CRS");
//        tab.setContent(table);
//        tabPane.getTabs().add(tab);
//    }
}
