package org.apache.sis.desktop.crs;

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
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.apache.sis.referencing.CRS;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.desktop.metadata.GeographicExtentBox;
import org.apache.sis.measure.AngleFormat;
import org.controlsfx.control.textfield.TextFields;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class CRSEditor extends AnchorPane implements Initializable {

    private static final List<Code> CRS_CODES = new ArrayList();
    private static final List<CoordinateSystem> COORDINATE_SYSTEMS = new ArrayList<>();
    private static final List<PrimeMeridian> PRIME_MERIDIANS = new ArrayList();
    public static final StringConverter<AxisDirection> AXIS_DIRECTION_STRING_CONVERTER = new StringConverter<AxisDirection>() {
        @Override
        public String toString(AxisDirection axis) {
            return axis.name();
        }

        @Override
        public AxisDirection fromString(String string) {
            return AxisDirection.valueOf(string);
        }
    };
    public static final StringConverter<CoordinateSystem> CS_STRING_CONVERTER = new StringConverter<CoordinateSystem>() {
        @Override
        public String toString(CoordinateSystem cs) {
            return cs.getName().getCode();
        }

        @Override
        public CoordinateSystem fromString(String string) {
            throw new UnsupportedOperationException();
        }
    };

    static {
        Runnable pmLoader = () -> {
            try {
                CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
                //Populate Prime Meridians
                if (factory instanceof DatumAuthorityFactory) {
                    DatumAuthorityFactory df = (DatumAuthorityFactory) factory;
                    Set<String> codes = df.getAuthorityCodes(PrimeMeridian.class);
                    PRIME_MERIDIANS.clear();
                    for (String code : codes) {
                        PrimeMeridian pm = df.createPrimeMeridian(code);
                        PRIME_MERIDIANS.add(pm);
                    }
                }
            } catch (FactoryException ex) {
                Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        };

        Runnable csLoader = () -> {
            try {
                //Populate Coordinate Systems
                CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
                if (factory instanceof CSAuthorityFactory) {
                    CSAuthorityFactory csf = (CSAuthorityFactory) factory;
                    Set<String> codes = csf.getAuthorityCodes(CoordinateSystem.class);
                    COORDINATE_SYSTEMS.clear();
                    for (String code : codes) {
                        CoordinateSystem cs;
                        try {
                            cs = csf.createCoordinateSystem(code);
                            COORDINATE_SYSTEMS.add(cs);
                            System.out.println("cs = " + cs);
                        } catch (FactoryException ex) {
                            Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            } catch (FactoryException ex) {
                Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        Runnable crsLoader = () -> {
            try {
                //Populate CRS Codes
                final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory(null);
                final Set<String> strs = crsFactory.getAuthorityCodes(CoordinateReferenceSystem.class);
                CRS_CODES.clear();
                for (String str : strs) {
                    CRS_CODES.add(new Code(crsFactory, str));
                }
            } catch (FactoryException ex) {
                Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        Thread pmThread = new Thread(pmLoader);
        pmThread.setName("PrimeMeridian codes loader");
        pmThread.setDaemon(true);
        pmThread.start();

        Thread csThread = new Thread(csLoader);
        csThread.setName("Coordinate systems loader");
        csThread.setDaemon(true);
        csThread.start();

        Thread crsThread = new Thread(crsLoader);
        crsThread.setName("CRS codes loader");
        crsThread.setDaemon(true);
        crsThread.start();
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
    private ComboBox<String> crsName;
    @FXML
    private TextField datumName;
    @FXML
    private ComboBox<PrimeMeridian> primeMeridian;
    @FXML
    private Text meridianLongitude;
    @FXML
    private Spinner<Double> semiMajor;
    @FXML
    private Spinner<Double> semiMinor;
    @FXML
    private ComboBox<CoordinateSystem> coordSystemType;
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
        crsName.getItems().setAll(CRS_CODES.stream().map(Code::toString).collect(Collectors.toList()));
        TextFields.bindAutoCompletion(crsName.getEditor(), crsName.getItems()).prefWidthProperty().bind(crsName.widthProperty());
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
        ObjectProperty<PrimeMeridian> pm = primeMeridian.valueProperty();
        ObjectBinding<PrimeMeridian> pmOr0 = Bindings.when(pm.isNotNull()).then(pm).otherwise(PRIME_MERIDIANS.get(0));
        StringBinding pmDegrees = Bindings.createStringBinding(() -> AngleFormat.getInstance().format(pmOr0.get().getGreenwichLongitude()), pm);
        meridianLongitude.textProperty().bind(pmDegrees);
        TextFields.bindAutoCompletion(coordSystemType.getEditor(), p -> COORDINATE_SYSTEMS, CS_STRING_CONVERTER).prefWidthProperty().bind(coordSystemType.widthProperty());
        coordSystemType.getItems().setAll(COORDINATE_SYSTEMS);
        coordSystemType.setConverter(CS_STRING_CONVERTER);
        axisNameColumn.setCellValueFactory((param) -> {
            return new SimpleObjectProperty<>(Objects.toString(param.getValue().getName().getCode(), ""));
        });
        axisDirectionColumn.setCellValueFactory((param) -> {
            return new SimpleObjectProperty<>(param.getValue().getDirection());
        });
        axisDirectionColumn.setCellFactory(param -> new ChoiceBoxTableCell<>(AXIS_DIRECTION_STRING_CONVERTER, AxisDirection.values()));
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
        crsName.setValue(Objects.toString(crs.getName(), ""));
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
        coordSystemType.valueProperty().addListener((observable, oldValue, newValue) -> setAxes(newValue));
        coordSystemType.setValue(crs.getCoordinateSystem());
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
    }

    private void setAxes(CoordinateSystem cs) throws IndexOutOfBoundsException {
        axesTable.getItems().clear();
        ObservableList<CoordinateSystemAxis> axes = axesTable.getItems();
        for (int i = 0; i < cs.getDimension(); i++) {
            CoordinateSystemAxis axis = cs.getAxis(i);
            axes.add(axis);
        }
    }

    private void setEditable(boolean enable) {
        datumName.setEditable(enable);
        primeMeridian.setDisable(!enable);
        semiMajor.setEditable(enable);
        semiMinor.setEditable(enable);
        coordSystemType.setDisable(!enable);
    }
}
