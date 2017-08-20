package org.apache.sis.desktop.crs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
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
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class CRSEditor extends AnchorPane implements Initializable {

    public static final AngleFormat ANGLE_FORMAT = new AngleFormat("DD°MM′SS.#″");

    private static final ConcurrentHashMap<String, String> CRS_CODE = new ConcurrentHashMap<>();
    private static final List<CoordinateSystem> COORDINATE_SYSTEMS = new ArrayList<>();
    private static final List<PrimeMeridian> PRIME_MERIDIANS = new ArrayList();
    
    public static final StringConverter<PrimeMeridian> PRIME_MERIDIAN_STRING_CONVERTER = new StringConverter<PrimeMeridian>() {
        @Override
        public String toString(PrimeMeridian object) {
            return object.getName().getCode();
        }

        @Override
        public PrimeMeridian fromString(String string) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
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
            if (cs == null) {
                return "";
            }
            return cs.getName().getCode();
        }

        @Override
        public CoordinateSystem fromString(String string) {
            throw new UnsupportedOperationException();
        }
    };
    public static final StringConverter<String> CRS_CODE_TO_DESCRIPTION_CONVERTER = new StringConverter<String>() {
        @Override
        public String toString(String code) {
            final String desc = CRS_CODE.get(code);
            if (desc != null) {
                return code + ' ' + desc;
            }
            return code;
        }

        @Override
        public String fromString(String codeSpaceDescription) {
            final String trimmed = codeSpaceDescription.trim();
            String[] split = trimmed.split("\\s+");

            if (CRS_CODE.containsKey(split[0])) {
                System.out.println("code found [" + split[0] + ']');
                return split[0];
            } else {
                System.out.println("code not found [" + split[0] + "]");
            }
            return trimmed;
        }
    };
    private static final CountDownLatch allDefsLoaded = new CountDownLatch(3);
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
            allDefsLoaded.countDown();
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
                        } catch (FactoryException ex) {
                            Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            } catch (FactoryException ex) {
                Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
            allDefsLoaded.countDown();
        };
        Runnable crsLoader = () -> {
            try {
                //Populate CRS Codes
                final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory(null);
                final Set<String> codes = crsFactory.getAuthorityCodes(CoordinateReferenceSystem.class);
                for (String code : codes) {
                    final String desc = crsFactory.getDescriptionText(code).toString();
                    CRS_CODE.put(code, desc);
                }
            } catch (FactoryException ex) {
                Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
            allDefsLoaded.countDown();
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
    private final StringProperty crsCode = new SimpleStringProperty();
    private Service<CoordinateReferenceSystem> fetchCrsService;

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
        this((CoordinateReferenceSystem) null);
        crsCode.set(code);
//        ProgressIndicator loading = new ProgressIndicator(-1);
//        Task<CoordinateReferenceSystem> task = new Task<CoordinateReferenceSystem>() {
//            @Override
//            protected CoordinateReferenceSystem call() throws Exception {
//                return CRS.forCode(code);
//            }
//        };
//        loading.progressProperty().bind(task.progressProperty());
//        AnchorPane.setLeftAnchor(loading, 20.0);
//        AnchorPane.setRightAnchor(loading, 20.0);
//        AnchorPane.setTopAnchor(loading, 20.0);
//        AnchorPane.setBottomAnchor(loading, 20.0);
//        getChildren().add(loading);
//
//        task.setOnSucceeded((event) -> {
//            getChildren().clear();
//            loadFXML();
//            setValues(task.getValue());
//        });
//        Thread thread = new Thread(task);
//        thread.setDaemon(true);
////        thread.start();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        crsName.setConverter(CRS_CODE_TO_DESCRIPTION_CONVERTER);

        semiMajor.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        semiMinor.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));

        primeMeridian.setConverter(PRIME_MERIDIAN_STRING_CONVERTER);
        ObjectProperty<PrimeMeridian> pm = primeMeridian.valueProperty();
        StringBinding pmDegrees = Bindings.createStringBinding(() -> pm.get() == null ? "NA" : ANGLE_FORMAT.format(pm.get().getGreenwichLongitude()), pm);
        meridianLongitude.textProperty().bind(pmDegrees);

        coordSystemType.setConverter(CS_STRING_CONVERTER);

        axisNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(Objects.toString(param.getValue().getName().getCode(), "")));
        axisDirectionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDirection()));
        axisDirectionColumn.setCellFactory(param -> new ChoiceBoxTableCell<>(AXIS_DIRECTION_STRING_CONVERTER, AxisDirection.values()));
        axisUnitColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getUnit().getName()));

        fetchCrsService = new Service<CoordinateReferenceSystem>() {
            {
                crsName.valueProperty().addListener((observable, oldValue, newValue) -> restart());
                setOnFailed(ae -> System.out.println("Failed " + crsName.getValue()));
                valueProperty().addListener((observable, oldValue, newValue) -> {
                    System.out.println("newValue = " + newValue);
                    if (newValue != null) {
                        setValues(newValue);
                    }
                });
            }
            @Override
            protected Task<CoordinateReferenceSystem> createTask() {
                return new Task<CoordinateReferenceSystem>() {
                    @Override
                    protected CoordinateReferenceSystem call() throws Exception {
                        System.out.println("fetching new CRS forCode(" + crsName.getValue() + ")");
                        return CRS.forCode(crsName.getValue());
                    }
                };
            }

        };
        crsCode.addListener((observable, oldValue, newValue) -> {
            System.out.println("oldValue + newValue = " + oldValue + " " + newValue);
        });
        crsName.valueProperty().bindBidirectional(crsCode);
        
        Thread loadChoices = new Thread(() -> {
            try {
                allDefsLoaded.await();
                Platform.runLater(()->populateChoices());
            } catch (InterruptedException ex) {
                Logger.getLogger(CRSEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        loadChoices.setDaemon(true);
        loadChoices.start();
    }

    private void populateChoices() {
        crsName.getItems().setAll(CRS_CODE.keySet());
        TextFields.bindAutoCompletion(crsName.getEditor(), CRS_CODE.keySet().stream().map(en -> CRS_CODE_TO_DESCRIPTION_CONVERTER.toString(en)).collect(Collectors.toList())).prefWidthProperty().bind(crsName.widthProperty());
        primeMeridian.getItems().setAll(PRIME_MERIDIANS);
        coordSystemType.getItems().setAll(COORDINATE_SYSTEMS);
        TextFields.bindAutoCompletion(coordSystemType.getEditor(), COORDINATE_SYSTEMS.stream().map(cs -> CS_STRING_CONVERTER.toString(cs)).collect(Collectors.toList())).prefWidthProperty().bind(coordSystemType.widthProperty());
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
        if (crs == this.crs) {
            return;
        }
        this.crs = crs;
        Identifier id = crs.getIdentifiers().iterator().next();
        crsName.setValue(id.getCodeSpace() + ':' + id.getCode());
        if (crs instanceof SingleCRS) {
            Datum d = ((SingleCRS) crs).getDatum();
            if (d instanceof GeodeticDatum) {
                GeodeticDatum datum = (GeodeticDatum) d;
                datumName.setText(Objects.toString(datum.getName(), ""));
                PrimeMeridian meridian = datum.getPrimeMeridian();
                primeMeridian.setValue(meridian);
                Ellipsoid ellipsoid = datum.getEllipsoid();
                double semiMajorAxis = ellipsoid.getSemiMajorAxis();
                double semiMinorAxis = ellipsoid.getSemiMinorAxis();
                semiMinor.getValueFactory().setValue(semiMinorAxis);
                semiMajor.getValueFactory().setValue(semiMajorAxis);
            }
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
