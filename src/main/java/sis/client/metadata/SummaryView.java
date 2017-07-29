package sis.client.metadata;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.util.ControlledVocabulary;

/**
 *
 * @author Siddhesh Rane
 */
public class SummaryView extends AnchorPane implements Initializable {

    @FXML
    private Label titleText;

    @FXML
    private Label abstractText;

    @FXML
    private Label spatialReferenceSystemText;

    @FXML
    private ComboBox<ControlledVocabulary> spatialRepresentationType;

    @FXML
    private Label northBound;

    @FXML
    private Label westBound;

    @FXML
    private Label eastBound;

    @FXML
    private Label southBound;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMetadata();
    }

    private TreeTable metadata;

    public SummaryView(TreeTable metadata) {
        this.metadata = metadata;
        FXMLLoader loader = new FXMLLoader(GeographicExtentBox.class.getResource("Summary.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GeographicExtentBox.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void loadMetadata() {
        //Title in identificationInfo/citation/title
        TreeTable.Node titleNode = getNodeByIdentifierPath(metadata, identificationInfo, citation, "title");
        if (titleNode != null) {
            titleText.setText(titleNode.getValue(TableColumn.VALUE).toString());
        } else {
            titleText.setText("");
        }

        //Abstract in identificationInfo/abstract
        TreeTable.Node abstrct = getNodeByIdentifierPath(metadata, identificationInfo, "abstract");
        if (abstrct != null) {
            abstractText.setText(abstrct.getValue(TableColumn.VALUE).toString());
        } else {
            abstractText.setText("");
        }

        //GeographicExtent in identificationInfo/westBoundLongitude
        TreeTable.Node north = getNodeByIdentifierPath(metadata, identificationInfo, extent, geographicElement, "northBoundLatitude");
        TreeTable.Node south = getNodeByIdentifierPath(metadata, identificationInfo, extent, geographicElement, "southBoundLatitude");
        TreeTable.Node east = getNodeByIdentifierPath(metadata, identificationInfo, extent, geographicElement, "eastBoundLongitude");
        TreeTable.Node west = getNodeByIdentifierPath(metadata, identificationInfo, extent, geographicElement, "westBoundLongitude");
        if (north != null && south != null && east != null && west != null) {
            northBound.setText(north.getValue(TableColumn.VALUE).toString());
            southBound.setText(south.getValue(TableColumn.VALUE).toString());
            eastBound.setText(east.getValue(TableColumn.VALUE).toString());
            westBound.setText(west.getValue(TableColumn.VALUE).toString());
        } else {
            
        }
        
        
    }
    public final String geographicElement = "geographicElement";
    public final String extent = "extent";
    public final String citation = "citation";
    public String identificationInfo = "identificationInfo";

    public static TreeTable.Node getNodeByIdentifierPath(TreeTable metadata, String... steps) {
        if (metadata == null || metadata.getRoot() == null) {
            return null;
        }
        boolean containsID = metadata.getColumns().contains(TableColumn.IDENTIFIER);
        if (!containsID) {
            return null;
        }
        TreeTable.Node root = metadata.getRoot();
        for (String step : steps) {
            Optional<TreeTable.Node> node = root.getChildren().stream().filter((TreeTable.Node t) -> {
                String id = t.getValue(TableColumn.IDENTIFIER);
                return step.equals(id);
            }).findFirst();
            if (!node.isPresent()) {
                return null;
            }
            root = node.get();
        }
        return root;
    }
}
