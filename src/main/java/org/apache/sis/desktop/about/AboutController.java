package org.apache.sis.desktop.about;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.sis.desktop.MetadataView;
import org.apache.sis.desktop.metadata.NodeTreeTable;
import org.apache.sis.setup.About;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;

/**
 * About Window Controller class
 *
 * @author Siddhesh Rane
 */
public class AboutController implements Initializable {

    @FXML
    private VBox vBox;
    @FXML
    private Label title;
    @FXML
    private Hyperlink website;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        website.setOnAction(ae -> {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                Thread thread = new Thread(() -> {
                    try {
                        desktop.browse(new URI("http://sis.apache.org"));
                    } catch (IOException ex) {
                        Logger.getLogger(MetadataView.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(AboutController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                thread.setDaemon(true);
                thread.start();
            }

        });
        loadAbout();
    }

    private void loadAbout() {
        final TreeTable configuration = About.configuration();
        NodeTreeTable sisabout = new NodeTreeTable(configuration);
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
        vBox.getChildren().add(sisabout);
        VBox.setVgrow(sisabout, Priority.ALWAYS);
    }
}
