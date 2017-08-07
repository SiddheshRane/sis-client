package org.apache.sis.desktop.crs;

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
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    private VBox root;
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
        filteredCodes = new FilteredList(registeredCodes);
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        table.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
                         if (ke.getCode().isLetterKey() || ke.getCode().isDigitKey()) {
                             filter.requestFocus();
                             filter.fireEvent(ke.copyFor(filter, filter));
                             ke.consume();
                         }
                     }
        );
        code.setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleStringProperty(param.getValue().getCode()));
        description.setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleStringProperty(param.getValue().getDescription()));

        Predicate<Code> predicate = (c) -> filter.getText().isEmpty() || c.getCode().toLowerCase().contains(filter.getText().toLowerCase());
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

        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(ae -> editCrs());
        table.setContextMenu(new ContextMenu(edit));
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

    private void editCrs() {
        Code code = table.getSelectionModel().getSelectedItem();
        if (code == null) {
            return;
        }
        CoordinateReferenceSystem crs;

        CRSEditor crsEditor = new CRSEditor(code.getCode());
        VBox.setVgrow(crsEditor, Priority.ALWAYS);
        Button backButton = new Button("Back to CRS list");
        backButton.setOnAction(ae -> {
            root.getChildren().clear();
            root.getChildren().addAll(filter, table);
        });
        root.getChildren().clear();
        root.getChildren().addAll(backButton, crsEditor);
    }
}
