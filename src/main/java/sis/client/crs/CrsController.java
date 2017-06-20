package sis.client.crs;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class CrsController implements Initializable {

    @FXML
    private TextField filter;
    @FXML
    private TableView<Code> table;
    @FXML
    private TableColumn<Code, String> code;
    @FXML
    private TableColumn<Code, String> description;

    private final ObservableList<Code> registeredCodes = FXCollections.observableArrayList();
    private final FilteredList<Code> filteredCodes;

    public CrsController() {
//        filteredCodes = registeredCodes.filtered((Code t) -> t.getCode().contains("49"));
        filteredCodes = new FilteredList(registeredCodes);
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        code.setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleStringProperty(param.getValue().getCode()));
        description.setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleStringProperty(param.getValue().getDescription()));

        Predicate<Code> predicate = (c) -> filter.getText().isEmpty() || c.getCode().contains(filter.getText());
        ObjectBinding<Predicate<Code>> predicateBinding
                = Bindings.<Predicate<Code>>createObjectBinding(() -> predicate, filter.textProperty());
        filteredCodes.predicateProperty().bind(predicateBinding);
        table.setItems(filteredCodes);

        new Thread() {
            @Override
            public void run() {
                try {
                    List<Code> codes = getCodes();
                    Platform.runLater(() -> {
                        registeredCodes.setAll(codes);
                        table.setPlaceholder(new Label(""));
                    });
                } catch (FactoryException ex) {
                }
            }
        }.start();
    }

    private List<Code> getCodes() throws FactoryException {
        final CRSAuthorityFactory factory = org.apache.sis.referencing.CRS.getAuthorityFactory(null);
        final Set<String> strs = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
        final List<Code> codes = new ArrayList<>();
        for (String str : strs) {
            codes.add(new Code(factory, str));
        }
        return codes;
    }

}
