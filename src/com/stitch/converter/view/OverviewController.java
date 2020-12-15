package com.stitch.converter.view;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.stitch.converter.GraphicsEngine;
import com.stitch.converter.LogPrinter;
import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;
import com.stitch.converter.model.StitchImage;
import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;
import com.stitch.converter.model.StitchList;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class OverviewController extends Controller {
	@FXML
	public BorderPane borderPane, colorTableBorderPane;
	@FXML
	public Canvas canvas;
	private CanvasController canvasController;
	@FXML
	public ScrollPane canvasScrollPane;
	@FXML
	public TableColumn<StitchList, String> colorColumn, nameColumn;
	@FXML
	public TableView<StitchList> colorTable;
	private File dmcFile;
	private String name;
	@FXML
	public TableColumn<StitchList, Boolean> highlightColumn, completeColumn;
	@FXML
	public TableColumn<StitchList, Integer> indexColumn, totalNumberColumn;
	private Stage overviewStage;
	@FXML
	public MenuItem save, saveTo, exportConvertedImage, exportCsv, exportBlueprint;
	@FXML
	public CheckBox showNumberCheckbox;
	@FXML
	public CheckMenuItem toggleColorTableItem, toggleLogItem;
	@FXML
	public SplitPane verticalSplitPane, horizontalSplitPane;
	@FXML
	public TextField zoom;

	@FXML
	public void author() {
		final FXMLLoader loader;
		final AnchorPane page;
		try {
			loader = new FXMLLoader(new File("resources/Author.fxml").toURI().toURL(), Resources.getBundle());
			page = (AnchorPane) loader.load();
			page.setStyle(new StringBuilder("-fx-font: ").append(Preferences.getString("fontSize", "11")).append("px ")
					.append(Preferences.getString("fontType", "Dotum")).append(";").toString());
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("read_failed", Resources.getString("layout")));
			return;
		}
		final Stage authorStage = new Stage();
		authorStage.setTitle(Resources.getString("about"));
		authorStage.initModality(Modality.WINDOW_MODAL);
		authorStage.initOwner(overviewStage);
		authorStage.setResizable(false);
		final Scene scene = new Scene(page);
		authorStage.setScene(scene);
		final AuthorController controller = loader.getController();
		controller.setApp(main);
		authorStage.showAndWait();
	}

	private void clickCanvas(final double originalX, final double originalY) {
		final int x = (int) ((originalX - canvasController.getMargin()) / canvasController.getScale());
		final int y = (int) ((originalY - canvasController.getMargin()) / canvasController.getScale());
		final Pixel pixel = new Pixel(x, y, null);
		for (final StitchList stitchList : colorTable.getItems()) {
			if (stitchList.getPixelList().hasPixel(pixel)) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						colorTable.requestFocus();
						colorTable.getSelectionModel().select(stitchList);
						colorTable.scrollTo(stitchList);
					}
				});
			}
		}
	}

	private void closeWindowEvent(final WindowEvent event) {
		if (confirmExit() == false) {
			event.consume();
		} else {
			if (Preferences.getBoolean("autoLoad", false) == true) {
				Preferences.setValue("scrollX", canvasScrollPane.getHvalue());
				Preferences.setValue("scrollY", canvasScrollPane.getVvalue());
			}
		}
	}

	private boolean confirmExit() {
		if (canvasController != null && canvasController.getImage().isChanged() == true) {
			final ButtonType save = new ButtonType(Resources.getString("save"), ButtonData.YES);
			final ButtonType notSave = new ButtonType(Resources.getString("save_no"), ButtonData.NO);
			final ButtonType cancel = new ButtonType(Resources.getString("cancel_button"), ButtonData.CANCEL_CLOSE);
			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle(Resources.getString("warning"));
			alert.setHeaderText(Resources.getString("file_changed_header"));
			alert.setContentText(Resources.getString("file_changed"));
			alert.getButtonTypes().setAll(save, notSave, cancel);
			final Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == save) {
				try {
					save(dmcFile, canvasController.getImage());
					return true;
				} catch (final IOException e) {
					LogPrinter.print(e);
					return false;
				}
			} else if (result.get() == notSave) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	@FXML
	public void exportBlueprintMenu() {
		if (exportBlueprint.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(new StringBuilder(name).append("_blueprint.png").toString());
		final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
				Resources.getString("png_file"), "*.png");
		fileChooser.setSelectedExtensionFilter(extensionFilter);
		final File blueprintFile = fileChooser.showSaveDialog(overviewStage);

		final StitchImage image = canvasController.getImage();
		final Canvas canvas = new Canvas(canvasController.getCanvas().getWidth(),
				canvasController.getCanvas().getHeight());
		final CanvasController blueprintController = new CanvasController(image, canvas);
		blueprintController.invalidate();
		final Blueprint blueprint = new Blueprint(canvasController.getImage(), canvas);
		blueprint.setScale(Preferences.getDouble("blueprintScale", 20d));
		blueprint.setListScale(Preferences.getDouble("blueprintListScale", 20d));
		blueprint.invalidate();
		final WritableImage writableImage = new WritableImage((int) blueprint.getCanvas().getWidth(),
				(int) blueprint.getCanvas().getHeight());
		blueprint.getCanvas().snapshot(null, writableImage);
		final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
		try {
			ImageIO.write(bufferedImage, "png", blueprintFile);
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("save_failed", Resources.getString("blueprint_file")));
		}
	}

	@FXML
	public void exportConvertedImageMenu() {
		if (exportConvertedImage.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(new StringBuilder(name).append(".png").toString());
		final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
				Resources.getString("png_file"), "*.png");
		fileChooser.getExtensionFilters().add(extensionFilter);
		final File imageFile = fileChooser.showSaveDialog(overviewStage);
		if (imageFile != null) {
			final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(canvasController.getImage().getFXImage(),
					null);
			try {
				ImageIO.write(bufferedImage, "png", imageFile);
				LogPrinter.alert(
						Resources.getString("file_saved", Resources.getString("image_file"), imageFile.getName()));
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(
						Resources.getString("save_failed", Resources.getString("image_file"), imageFile.getName()));
			}
		}
	}

	@FXML
	public void exportStitchListMenu() {
		if (exportCsv.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(new StringBuilder(name).append(".act").toString());
		final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_act"), "*.act");
		fileChooser.getExtensionFilters().add(extensionFilter);
		final File actFile = fileChooser.showSaveDialog(overviewStage);
		if (actFile != null) {
			if (actFile.exists()) {
				actFile.delete();
			}
			try (final OutputStream outputStream = new FileOutputStream(actFile)) {
				actFile.createNewFile();
				int totalNumber = 0;
				for (final StitchColor color : canvasController.getImage().getColorList()) {
					if (totalNumber == 256) {
						break;
					}
					totalNumber++;
					byte red = (byte) (color.getRed() & 0xff);
					byte green = (byte) (color.getGreen() & 0xff);
					byte blue = (byte) (color.getBlue() & 0xff);
					outputStream.write(red);
					outputStream.write(green);
					outputStream.write(blue);

				}
				for (final StitchColor color : canvasController.getImage().getAlternate()) {
					if (totalNumber == 256) {
						break;
					}
					totalNumber++;
					byte red = (byte) (color.getRed() & 0xff);
					byte green = (byte) (color.getGreen() & 0xff);
					byte blue = (byte) (color.getBlue() & 0xff);
					outputStream.write(red);
					outputStream.write(green);
					outputStream.write(blue);
				}
				for (; totalNumber <= 255; totalNumber++) {
					outputStream.write(0x00);
					outputStream.write(0x00);
					outputStream.write(0x00);
				}
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("save_failed", Resources.getString("txt_file")));
			}
		}
	}

	private boolean hasUpdates() throws Exception {
		try {
			final URL url = new URL(Resources.getString("update_url"));
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
			try (final InputStreamReader inputStreamReader = new InputStreamReader(
					httpUrlConnection.getInputStream())) {
				try (final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
					final StringBuilder stringBuilder = new StringBuilder();
					String temp;
					while ((temp = bufferedReader.readLine()) != null) {
						stringBuilder.append(temp);
					}
					final JSONParser jsonParser = new JSONParser();
					final JSONObject jsonObject = (JSONObject) jsonParser.parse(stringBuilder.toString());
					final int version = Integer.parseInt(jsonObject.get("tag_name").toString());
					return version > Integer.parseInt(Resources.getString("version"));
				}
			} finally {
				httpUrlConnection.disconnect();
			}
		} catch (final Exception e) {
			throw e;
		}
	}

	@FXML
	public void initialize() {
		highlightColumn.setCellFactory(column -> new CheckBoxTableCell<>());
		highlightColumn.setCellValueFactory(cellData -> {
			final StitchList cellValue = cellData.getValue();
			final BooleanProperty property = cellValue.highlightProperty();
			cellValue.setHighlight(property.get());
			property.addListener((observable, oldValue, newValue) -> {
				if (newValue == cellValue.isHighlighted()) {
					cellValue.setHighlight(newValue);
					cellValue.getPixelList().setHighlighted(newValue);
					if (newValue == true && cellValue.isCompleted() == true) {
						cellValue.setCompleted(false);
					}
					setTitleChanged(true);
				}
			});

			return property;
		});
		completeColumn.setCellFactory(column -> new CheckBoxTableCell<>());
		completeColumn.setCellValueFactory(cellData -> {
			final StitchList cellValue = cellData.getValue();
			final BooleanProperty property = cellValue.completeProperty();
			cellValue.setCompleted(property.get());
			property.addListener((observable, oldValue, newValue) -> {
				cellValue.setCompleted(newValue);
				cellValue.getPixelList().setCompleted(newValue);
				if (newValue == true && cellValue.isHighlighted() == true) {
					cellValue.setHighlight(false);
				}
				setTitleChanged(true);
			});
			return property;
		});
		indexColumn.setCellValueFactory(cellData -> cellData.getValue().indexProperty().asObject());
		totalNumberColumn.setCellValueFactory(cellData -> cellData.getValue().totalNumberProperty().asObject());
		colorColumn.setCellFactory(column -> {
			return new TableCell<StitchList, String>() {
				@Override
				protected void updateItem(final String item, final boolean empty) {
					super.updateItem(item, empty);
					if (!isEmpty()) {
						this.setStyle(new StringBuilder("-fx-background-color:").append(item).toString());
					}
				}
			};
		});
		colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorStringProperty());
		nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

		zoom.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				event.consume();
				final String zoomScale = zoom.getText();
				if (setZoom(zoomScale) == true) {
					Preferences.setValue("scale", zoomScale);
				}
				invalidate();
			}
		});

		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent event) {
				clickCanvas(event.getX(), event.getY());
			}
		});
	}

	public void invalidate() {
		canvasController.invalidate();
	}

	public void loadDmc(File file) {
		dmcFile = file;
		overviewStage.setTitle(dmcFile.getName());
		main.load(new GraphicsEngine.Builder(new File(Preferences.getString("csvFile", "resources/dmc.csv")), dmcFile));
		name = dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf("."));
	}

	@FXML
	public void loadMenu() {
		if (confirmExit() == false) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		final FileChooser.ExtensionFilter dmcFilter = new FileChooser.ExtensionFilter(Resources.getString("filter_dmc"),
				"*.dmc");
		final FileChooser.ExtensionFilter allFileFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_all"), "*.*");
		final FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_image"), "*.bmp", "*.jpg", "*.jpeg", "*.gif", "*.png", "*.apng");
		fileChooser.getExtensionFilters().add(allFileFilter);
		fileChooser.getExtensionFilters().add(dmcFilter);
		fileChooser.getExtensionFilters().add(imageFilter);
		final File file = fileChooser.showOpenDialog(overviewStage);
		if (file != null) {
			String extension = "";
			{
				final String name = file.getName();
				int lastIndexOf = name.lastIndexOf(".");
				if (lastIndexOf == -1) {
					LogPrinter.error(Resources.getString("cant_read_image"));
					return;
				}
				extension = name.substring(lastIndexOf);
			}
			if (extension.equals(".dmc")) {
				dmcFile = file;
				overviewStage.setTitle(dmcFile.getName());
				main.load(new GraphicsEngine.Builder(new File("dmc.csv"), dmcFile));
				Preferences.setValue("autoLoadFile", file.getPath());
			} else {
				Preferences.setValue("scrollX", "0");
				Preferences.setValue("scrollY", "0");
				final GraphicsEngine.Builder builder = new GraphicsEngine.Builder(
						new File(Preferences.getString("csvFile", "resources/dmc.csv")), file);
				builder.setColorLimit(Preferences.getInteger("maximumColorLimit", 0));
				builder.setBackground(Preferences.getColor("backgroundColor", new StitchColor(Color.WHITE, "")));
				builder.setThread(Preferences.getInteger("workingThread", 0));
				builder.setScaled(Preferences.getBoolean("resizeImage", true));

				try {
					dmcFile = new File(new StringBuilder(file.getParent()).append(File.separator)
							.append(file.getName().substring(0, file.getName().lastIndexOf("."))).toString());
					overviewStage.setTitle(new StringBuilder(dmcFile.getName()).append("(*)").toString());
					main.startConversion(builder);
				} catch (final IllegalArgumentException | NullPointerException e) {
					LogPrinter.print(e);
					LogPrinter.error(Resources.getString("cant_read_image"));
					return;
				}
			}
		}
	}

	@FXML
	public void onShowNumberCheckboxClicked() {
		Preferences.setValue("drawGridNumber", Boolean.toString(showNumberCheckbox.isSelected()));
		canvasController.invalidate();
	}

	public boolean save(File file, StitchImage image) throws IOException {
		try {
			Resources.writeObject(file, image);
			return true;
		} catch (final IOException e) {
			throw e;
		}
	}

	@FXML
	public boolean saveMenu() {
		if (save.isDisable() == true) {
			return false;
		}
		setTitleChanged(false);
		try {
			save(dmcFile, canvasController.getImage());
			Preferences.setValue("autoLoadFile", dmcFile.getPath());
			LogPrinter.alert(Resources.getString("file_saved", dmcFile.getName(), Resources.getString("dmc_file")));
			return true;
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("save_failed", Resources.getString("dmc_file")));
			return false;
		}
	}

	@FXML
	public void saveToMenu() {
		if (saveTo.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
				Resources.getString("dmc_file"), "*.dmc");
		fileChooser.getExtensionFilters().add(extensionFilter);
		final File saveFile = fileChooser.showSaveDialog(overviewStage);
		if (saveFile != null) {
			dmcFile = saveFile;
			saveMenu();
		}
	}

	public void setDividerPosition() {
		horizontalSplitPane.setDividerPositions((double) (1 - 314 / borderPane.getWidth()));
	}

	public void setImage(final StitchImage stitchImage) {
		canvasController = new CanvasController(stitchImage, canvas);
		final ObservableList<StitchList> stitchListArrayList = FXCollections.observableArrayList(
				stitchList -> new Observable[] { stitchList.highlightProperty(), stitchList.completeProperty() });
		for (final PixelList pixelList : stitchImage.getPixelLists()) {
			stitchListArrayList.add(new StitchList(pixelList));
		}
		stitchListArrayList.addListener(new ListChangeListener<StitchList>() {
			@Override
			public void onChanged(Change<? extends StitchList> c) {
				while (c.next()) {
					if (c.wasUpdated()) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								invalidate();
							}
						});
					}
				}
			}
		});
		colorTable.setItems(stitchListArrayList);
		if (setZoom(Preferences.getValue("scale", "MATCH_WIDTH")) == false) {
			Preferences.setValue("scale", "MATCH_WIDTH");
			setZoom(Preferences.getValue("scale", "MATCH_WIDTH"));
		}
		canvas.setDisable(false);
		colorTable.setDisable(false);
		zoom.setDisable(false);
		showNumberCheckbox.setDisable(false);
		save.setDisable(false);
		saveTo.setDisable(false);
		exportConvertedImage.setDisable(false);
		exportCsv.setDisable(false);
		exportBlueprint.setDisable(false);
		showNumberCheckbox.setSelected(Preferences.getBoolean("drawGridNumber", true));
		invalidate();
		if (Preferences.getBoolean("autoLoad", false) == true) {
			canvasScrollPane.setHvalue(Preferences.getDouble("scrollX", 0d));
			canvasScrollPane.setVvalue(Preferences.getDouble("scrollY", 0d));
		}
		
		final ChangeListener<Number> sizeChangeListener = (observable, oldValue, newValue) -> {
			final String zoomRatio = Preferences.getString("scale");
			if(zoomRatio.contains("MATCH")) {
				setZoom(Preferences.getString("scale"));
				invalidate();
			}
		};
		
		canvasScrollPane.widthProperty().addListener(sizeChangeListener);
		canvasScrollPane.heightProperty().addListener(sizeChangeListener);
	}

	public void setStage(final Stage overviewStage) {
		this.overviewStage = overviewStage;
		if (Preferences.getBoolean("showColorTable", true) == false) {
			horizontalSplitPane.getItems().remove(1);
			toggleColorTableItem.setSelected(false);
		}
		this.overviewStage.widthProperty().addListener((obs, oldVal, newVal) -> {
			setDividerPosition();
		});
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				
				setDividerPosition();
				overviewStage.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
						(event) -> closeWindowEvent(event));
				if (Preferences.getBoolean("autoLoad", false) == true) {
					try {
						loadDmc(new File(Preferences.getString("autoLoadFile")));
					} catch (final NoSuchElementException e) {
						Preferences.setValue("autoLoad", "false");
					}
				}
				try {
					if (hasUpdates() == true) {
						final ButtonType update = new ButtonType(Resources.getString("update"), ButtonData.YES);
						final ButtonType notUpdate = new ButtonType(Resources.getString("update_no"), ButtonData.NO);
						final Alert alert = new Alert(AlertType.INFORMATION);
						alert.setTitle(Resources.getString("information"));
						alert.setHeaderText(Resources.getString("update_header"));
						alert.setContentText(Resources.getString("update_content"));
						alert.getButtonTypes().setAll(update, notUpdate);
						final Optional<ButtonType> result = alert.showAndWait();
						if (result.get() == update) {
							main.getHostServices().showDocument(
									new StringBuilder(Resources.getString("url")).append("/releases").toString());
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
					LogPrinter.print(e);
					LogPrinter.error(Resources.getString("error_has_occurred"));
				}
			}
		});
	}

	@FXML
	public void setting() {
		ScrollPane page = null;
		try {
			final FXMLLoader loader = new FXMLLoader(new File("resources/Setting.fxml").toURI().toURL(),
					Resources.getBundle());
			page = (ScrollPane) loader.load();
			page.setStyle(new StringBuilder("-fx-font: ").append(Preferences.getString("fontSize", "11"))
					.append("px ").append(Preferences.getString("fontType", "Dotum")).append(";").toString());
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("read_failed", Resources.getString("layout")));
			return;
		}
		final Stage settingStage = new Stage();
		settingStage.setTitle(Resources.getString("setting"));
		settingStage.initModality(Modality.WINDOW_MODAL);
		settingStage.initOwner(overviewStage);
		settingStage.setResizable(false);
		final Scene scene = new Scene(page);
		settingStage.setScene(scene);
		settingStage.showAndWait();
	}

	public void setTitleChanged(final boolean changed) {
		canvasController.getImage().setChanged(changed);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (changed == true) {
					overviewStage.setTitle(new StringBuilder(dmcFile.getName()).append("(*)").toString());
				} else {
					overviewStage.setTitle(dmcFile.getName());
				}
			}
		});
	}

	public boolean setZoom(final String scale) {
		double scaleRatio = 0d;

		final double screenWidth = canvasScrollPane.getWidth() - canvasController.getMargin() * 2 - 12;
		final double imageWidth = canvasController.getImage().getWidth();
		final double ratioByWidth = Math.max(2.0, screenWidth / imageWidth);
		final double screenHeight = canvasScrollPane.getHeight() - canvasController.getMargin() * 2 - 12;
		final double imageHeight = canvasController.getImage().getHeight();
		final double ratioByHeight = Math.max(2.0, screenHeight / imageHeight);

		if (scale.equals("MATCH_WIDTH")) {
			canvasController.setScale(ratioByWidth);
			zoom.setText(scale);
		} else if (scale.equals("MATCH_HEIGHT")) {
			canvasController.setScale(ratioByHeight);
			zoom.setText(scale);
		} else if (scale.equals("MATCH_SCREEN")) {
			canvasController.setScale(Math.min(ratioByWidth, ratioByHeight));
			zoom.setText(scale);
		} else {
			try {
				scaleRatio = Double.parseDouble(scale.replace("x", ""));
				canvasController.setScale(Double.parseDouble(scale));
			} catch (final NumberFormatException e) {
				LogPrinter.alert(Resources.getString("zoom_number_cant_read"));
				zoom.setText(Preferences.getValue("scale", "MATCH_WIDTH"));
				return false;
			}
			zoom.setText(new StringBuilder().append(scaleRatio).append("x").toString());
		}
		return true;
	}

	@FXML
	public void toggleWindowColorTable() {
		if (toggleColorTableItem.isSelected() == true) {
			Preferences.setValue("showColorTable", "true");
			horizontalSplitPane.getItems().add(1, colorTableBorderPane);
			setDividerPosition();
		} else {
			Preferences.setValue("showColorTable", "false");
			horizontalSplitPane.getItems().remove(1);
		}
	}
}
