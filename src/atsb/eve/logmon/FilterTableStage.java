package atsb.eve.logmon;

import java.util.ArrayList;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FilterTableStage extends Stage {

	public FilterTableStage(ObservableList<Filter> filters) {
		BorderPane bp = new BorderPane();

		TableView<Filter> table = new TableView<Filter>(filters);
		table.setEditable(false);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		TableColumn tc0 = new TableColumn("Expression");
		tc0.prefWidthProperty().bind(table.widthProperty());
		tc0.setCellValueFactory(new PropertyValueFactory<Filter, String>("expression"));
		table.getColumns().add(tc0);
		bp.setCenter(table);

		HBox hb = new HBox();
		TextField addExpression = new TextField();
		addExpression.setPrefWidth(140);
		addExpression.setPromptText("regex");
		Button addButton = new Button("Add");
		addButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				String filter = addExpression.getText().trim().replaceAll(",", "");
				filters.add(new Filter(filter));
				ArrayList<String> gf = ConfigurationData.getInstance().getListProperty("GlobalFilters");
				gf.add(filter);
				ConfigurationData.getInstance().setListProperty("GlobalFilters", gf);
				addExpression.clear();
			}
		});
		Button removeButton = new Button("Remove");
		removeButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				ArrayList<String> gf = ConfigurationData.getInstance().getListProperty("GlobalFilters");
				ObservableList<Filter> selected = table.getSelectionModel().getSelectedItems();
				for (Filter f : selected) {
					gf.remove(f.getExpression());
				}
				table.getItems().removeAll(selected);
				ConfigurationData.getInstance().setListProperty("GlobalFilters", gf);
			}
		});
		hb.getChildren().addAll(addExpression, addButton, removeButton);
		bp.setBottom(hb);

		setOnCloseRequest(event -> {
			ConfigurationData.getInstance().save();
		});

		Scene scene = new Scene(bp, 300, 400);
		setTitle("Global Filters");
		setMinWidth(200);
		setMinHeight(200);
		initModality(Modality.APPLICATION_MODAL);
		setScene(scene);
	}

}
