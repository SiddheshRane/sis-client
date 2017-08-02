package sis.client.crs;

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
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.util.InternationalString;
import sis.client.metadata.GeographicExtentBox;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class CRSEditor extends AnchorPane implements Initializable {

    @FXML
    private GridPane grid;
    @FXML
    private Label northBound;
    @FXML
    private Label westBound;
    @FXML
    private Label eastBound;
    @FXML
    private Label southBound;
    @FXML
    private TextField crsName;
    @FXML
    private TextField datumName;
    @FXML
    private ComboBox<String> primeMeridian;
    @FXML
    private Spinner<Double> semiMajor;
    @FXML
    private Spinner<Double> semiMinor;
    @FXML
    private ComboBox<String> coordSystemType;
    @FXML
    private TextArea areaDescription;
    @FXML
    private TableView<?> axesTable;
    @FXML
    private TableColumn<?, ?> axisNameColumn;
    @FXML
    private TableColumn<?, ?> axisUnitColumn;
    @FXML
    private TableColumn<?, ?> axisDirectionColumn;

    private CoordinateReferenceSystem crs;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    public CRSEditor(CoordinateReferenceSystem crs) {
        this.crs = crs;
        FXMLLoader loader = new FXMLLoader(CRSEditor.class.getResource("CRSEditor.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            getChildren().add(new Text("Could not load"));
        }
        setValues();
    }

    private void setValues() {
        Identifier name = crs.getName();
        if (name != null) {
            crsName.setText(name.toString());
        }
        if (crs instanceof GeographicCRS) {
            GeographicCRS gcrs = (GeographicCRS) crs;
            GeodeticDatum datum = gcrs.getDatum();
            if (datum.getName() != null) {
                datumName.setText(datum.getName().toString());
            }
            PrimeMeridian meridian = datum.getPrimeMeridian();
            primeMeridian.setValue(meridian.getName().toString());
            Ellipsoid ellipsoid = datum.getEllipsoid();
            double semiMajorAxis = ellipsoid.getSemiMajorAxis();
            double semiMinorAxis = ellipsoid.getSemiMinorAxis();
            semiMinor.getValueFactory().setValue(semiMinorAxis);
            semiMajor.getValueFactory().setValue(semiMajorAxis);
        }
        coordSystemType.setValue(crs.getCoordinateSystem().getName().toString());
        final Extent domainOfValidity = crs.getDomainOfValidity();
        if (domainOfValidity != null) {
            InternationalString description = domainOfValidity.getDescription();
            areaDescription.setText(description == null ? "" : description.toString());
            Optional<? extends GeographicExtent> boundingBox = domainOfValidity.getGeographicElements().stream().filter(el -> {
                return el instanceof GeographicBoundingBox;
            }).findAny();
            if (boundingBox.isPresent()) {
                GeographicBoundingBox box = (GeographicBoundingBox) boundingBox.get();
                GeographicExtentBox geographicExtentBox = new GeographicExtentBox(box);
                grid.getChildren().add(geographicExtentBox);
                GridPane.setConstraints(geographicExtentBox, 1, 7);
            }
        }
    }

}
