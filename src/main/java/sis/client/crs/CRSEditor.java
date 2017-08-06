package sis.client.crs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import javafx.util.converter.FormatStringConverter;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.referencing.CRS;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import sis.client.metadata.GeographicExtentBox;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class CRSEditor extends AnchorPane implements Initializable {

    private static final List<PrimeMeridian> PRIME_MERIDIANS = new ArrayList();
    public static final StringConverter<AxisDirection> axisDirectionStringConverter = new StringConverter<AxisDirection>() {
        @Override
        public String toString(AxisDirection axis) {
            return axis.name();
        }

        @Override
        public AxisDirection fromString(String string) {
            return AxisDirection.valueOf(string);
        }
    };

    static {
        CRSAuthorityFactory factory;
        try {
            factory = CRS.getAuthorityFactory("EPSG");
            if (factory instanceof DatumAuthorityFactory) {
                System.out.println("factory = " + factory);
                DatumAuthorityFactory df = (DatumAuthorityFactory) factory;
                Set<String> codes = df.getAuthorityCodes(PrimeMeridian.class);
                for (String code : codes) {
                    PrimeMeridian pm = df.createPrimeMeridian(code);
                    PRIME_MERIDIANS.add(pm);
                    System.out.println("pm = " + pm);
                }
            }
            if (factory instanceof CSAuthorityFactory) {
                CSAuthorityFactory csf = (CSAuthorityFactory) factory;
                Set<String> codes = csf.getAuthorityCodes(CoordinateSystem.class);
                for (String code : codes) {
                    CoordinateSystem cs = csf.createCoordinateSystem(code);
                    System.out.println("cs = " + cs);
                }
            }
        } catch (FactoryException ex) {
            Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
    private ComboBox<PrimeMeridian> primeMeridian;
    @FXML
    private Spinner<Double> semiMajor;
    @FXML
    private Spinner<Double> semiMinor;
    @FXML
    private ComboBox<String> coordSystemType;
    @FXML
    private TextArea areaDescription;
    @FXML
    private TableView<CoordinateSystemAxis> axesTable;
    @FXML
    private TableColumn<CoordinateSystemAxis, String> axisNameColumn;
    @FXML
    private TableColumn<CoordinateSystemAxis, String> axisUnitColumn;
    @FXML
    private TableColumn<CoordinateSystemAxis, AxisDirection> axisDirectionColumn;

    private CoordinateReferenceSystem crs;

    public CRSEditor(CoordinateReferenceSystem crs) {
        loadFXML();
        setValues(crs);
    }

    /**
     * Creates a CRSEditor from the given code string. This calls
     * {@code  CRS.forCode(code)} on a background thread
     *
     * @param code
     */
    public CRSEditor(String code) {
        ProgressIndicator loading = new ProgressIndicator(-1);
        Task<CoordinateReferenceSystem> task = new Task<CoordinateReferenceSystem>() {
            @Override
            protected CoordinateReferenceSystem call() throws Exception {
                return CRS.forCode(code);
            }
        };
        loading.progressProperty().bind(task.progressProperty());
        AnchorPane.setLeftAnchor(loading, 20.0);
        AnchorPane.setRightAnchor(loading, 20.0);
        AnchorPane.setTopAnchor(loading, 20.0);
        AnchorPane.setBottomAnchor(loading, 20.0);
        getChildren().add(loading);

        task.setOnSucceeded((event) -> {
            getChildren().clear();
            loadFXML();
            setValues(task.getValue());
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        semiMajor.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        semiMinor.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        primeMeridian.getItems().setAll(PRIME_MERIDIANS);
        primeMeridian.setConverter(new StringConverter<PrimeMeridian>() {
            @Override
            public String toString(PrimeMeridian object) {
                return object.getName().getCode();
            }

            @Override
            public PrimeMeridian fromString(String string) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        axisNameColumn.setCellValueFactory((param) -> {
            return new SimpleObjectProperty<>(Objects.toString(param.getValue().getName().getCode(), ""));
        });
        axisDirectionColumn.setCellValueFactory((param) -> {
            return new SimpleObjectProperty<>(param.getValue().getDirection());
        });
        axisDirectionColumn.setCellFactory(param -> new ChoiceBoxTableCell<>(axisDirectionStringConverter, AxisDirection.values()));
        axisUnitColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getUnit().getName()));
    }

    protected void loadFXML() {
        FXMLLoader loader = new FXMLLoader(CRSEditor.class.getResource("CRSEditor.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            getChildren().add(new Text("Could not load"));
        }
    }

    private void setValues(CoordinateReferenceSystem crs) {
        this.crs = crs;
        crsName.setText(Objects.toString(crs.getName(), ""));
        if (crs instanceof GeographicCRS) {
            GeographicCRS gcrs = (GeographicCRS) crs;
            GeodeticDatum datum = gcrs.getDatum();
            datumName.setText(Objects.toString(datum.getName(), ""));
            PrimeMeridian meridian = datum.getPrimeMeridian();
            primeMeridian.setValue(meridian);
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
            areaDescription.setText(Objects.toString(description, ""));
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
        ObservableList<CoordinateSystemAxis> axes = FXCollections.<CoordinateSystemAxis>observableArrayList();
        for (int i = 0; i < crs.getCoordinateSystem().getDimension(); i++) {
            CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(i);
            axes.add(axis);
        }
        axesTable.setItems(axes);
    }

}
