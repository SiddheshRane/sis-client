package org.apache.sis.desktop.metadata;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.referencing.ReferenceSystem;
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
    private Label spatialRepresentationTypeText;

    @FXML
    private Label northBound;

    @FXML
    private Label westBound;

    @FXML
    private Label eastBound;

    @FXML
    private Label southBound;
    @FXML
    private GridPane axisDimensionsGrid;

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

    public SummaryView(Metadata metadata){
        this(MetadataStandard.ISO_19115.asTreeTable(metadata, Metadata.class, ValueExistencePolicy.NON_NIL));
    }
    
    void loadMetadata() {
        //Title in identificationInfo/citation/title
        TreeTable.Node titleNode = getNodeByIdentifierPath(metadata, identificationInfo, citation, "title");
        if (titleNode != null) {
            final Object value = titleNode.getValue(TableColumn.VALUE);
            titleText.setText(Objects.toString(value, ""));
        } else {
            titleText.setText("");
        }

        //Abstract in identificationInfo/abstract
        TreeTable.Node abstrct = getNodeByIdentifierPath(metadata, identificationInfo, "abstract");
        if (abstrct != null) {
            abstractText.setText(Objects.toString(abstrct.getValue(TableColumn.VALUE), ""));
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

        //SpatialRepresentationType
        TreeTable.Node type = getNodeByIdentifierPath(metadata, identificationInfo, "spatialRepresentationType");
        if (type != null) {
            ControlledVocabulary cv = (ControlledVocabulary) type.getValue(TableColumn.VALUE);
            spatialRepresentationTypeText.setText(cv == null ? "" : cv.name());
        }
        //SpatialReferenceSystem 
        TreeTable.Node refSys = getNodeByIdentifierPath(metadata, "referenceSystemInfo");
        if (refSys != null) {
            ReferenceSystem ref = (ReferenceSystem) refSys.getValue(TableColumn.VALUE);
            if (ref != null) {
                final Identifier name = ref.getName();
                spatialReferenceSystemText.setText(Objects.toString(name, ""));
            }
        }
        //AxisDimensions
        List<TreeTable.Node> srinfos = getNodesByIdentifierPath(metadata.getRoot(), "spatialRepresentationInfo");
        List<TreeTable.Node> axisDimensions = Collections.EMPTY_LIST;
        for (TreeTable.Node srinfo : srinfos) {
            List<TreeTable.Node> axd = getNodesByIdentifierPath(srinfo, "axisDimensionProperties");
            if (axd.size() > axisDimensions.size()) {
                axisDimensions = axd;
            }
        }
        Stream<Dimension> dimensions1 = axisDimensions.stream().map((t) -> {
            return (Dimension) t.getUserObject();
        });
        Stream<Dimension> dimensions2 = axisDimensions.stream().map((t) -> {
            return (Dimension) t.getUserObject();
        });
        List<Text> names = dimensions1.map(t -> t.getDimensionName().name()).peek(System.out::println).map(Text::new).collect(Collectors.toList());
        List<Text> sizes = dimensions2.map(t -> t.getDimensionSize().toString()).peek(System.out::println).map(Text::new).collect(Collectors.toList());
        
        for (Text name : names) {
            axisDimensionsGrid.addColumn(0, name);
        }
        for (Text size : sizes) {
            axisDimensionsGrid.addColumn(1, size);
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

    public static List<TreeTable.Node> getNodesByIdentifierPath(TreeTable.Node node, String... steps) {
        if (node == null) {
            return null;
        }

        TreeTable.Node root = node;
        for (int i = 0; i < steps.length - 1; i++) {
            String step = steps[i];
            Optional<TreeTable.Node> stepNode = root.getChildren().stream()
                    .filter((TreeTable.Node t) -> {
                        String id = t.getValue(TableColumn.IDENTIFIER);
                        return step.equals(id);
                    })
                    .findFirst();
            if (!stepNode.isPresent()) {
                return Collections.<TreeTable.Node>emptyList();
            }
            root = stepNode.get();
        }
        return root.getChildren().stream().filter((t) -> steps[steps.length - 1].equals(t.getValue(TableColumn.IDENTIFIER))).collect(Collectors.toList());
    }

}
