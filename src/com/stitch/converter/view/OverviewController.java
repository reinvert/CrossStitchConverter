package com.stitch.converter.view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.imageio.ImageIO;

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
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
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
	public SplitPane verticalSplitPane, horizontalSplitPane;
	@FXML
	public MenuItem save, saveTo, exportPng, exportCsv, exportBlueprint;
	@FXML
	public TableView<StitchList> colorTable;
	@FXML
	public BorderPane borderPane, colorTableBorderPane;
	@FXML
	public TableColumn<StitchList, Boolean> highlightColumn, completeColumn;
	@FXML
	public TableColumn<StitchList, Integer> indexColumn, totalNumberColumn;
	@FXML
	public TableColumn<StitchList, String> colorColumn, nameColumn;
	@FXML
	public Canvas canvas;
	@FXML
	public ScrollPane canvasScrollPane;
	@FXML
	public TextField zoom;
	@FXML
	public CheckBox showNumberCheckbox;
	@FXML
	public CheckMenuItem toggleColorTableItem, toggleLogItem;

	private Stage overviewStage;

	private File dmcFile;

	private CanvasController canvasController;

	public void setStage(final Stage overviewStage) {
		this.overviewStage = overviewStage;
		this.overviewStage.widthProperty().addListener((obs, oldVal, newVal) -> {
			setDividerPosition();
		});

		this.overviewStage.heightProperty().addListener((obs, oldVal, newVal) -> {
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
			}
		});
	}

	@FXML
	public void load() {
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
		File file = fileChooser.showOpenDialog(overviewStage);
		if (file != null) {
			String extension = "";
			{
				String name = file.getName();
				int lastIndexOf = name.lastIndexOf(".");
				if (lastIndexOf == -1) {
					LogPrinter.error(Resources.getString("cant_read_image"));
					return;
				}
				extension = name.substring(lastIndexOf);
			}
			if (extension.equals(".dmc")) {
				dmcFile = file;
				fileName = dmcFile.getName();
				overviewStage.setTitle(dmcFile.getName());
				main.load(new GraphicsEngine.Builder(new File("dmc.csv"), dmcFile));
				System.out.println(file.getPath() + file.getName());
				Preferences.setValue("autoLoadFile", file.getPath());
			} else {
				Preferences.setValue("scrollX", "0");
				Preferences.setValue("scrollY", "0");
				final GraphicsEngine.Builder builder = new GraphicsEngine.Builder(
						new File(Preferences.getString("csvFile", "resources/dmc.csv")), file);
				builder.setColorLimit(Preferences.getInteger("maximumColorLimit", 0));
				builder.setBackground(Preferences.getColor("backgroundColor", new StitchColor(Color.WHITE, "")));
				builder.setThread(Preferences.getInteger("workingThread", 0));
				builder.setResize(Preferences.getBoolean("resizeImage", true));

				try {
					dmcFile = new File(file.getParent() + File.separator
							+ file.getName().substring(0, file.getName().lastIndexOf(".")) + ".dmc");
					fileName = dmcFile.getName();
					overviewStage.setTitle(fileName + "(*)");
					main.startConversion(builder);
				} catch (final IllegalArgumentException | NullPointerException e) {
					LogPrinter.print(e);
					LogPrinter.error(Resources.getString("cant_read_image"));
					return;
				}
			}
		}
	}

	public void loadDmc(File file) {
		dmcFile = file;
		fileName = dmcFile.getName();
		overviewStage.setTitle(dmcFile.getName());
		main.load(new GraphicsEngine.Builder(new File(Preferences.getString("csvFile", "resources/dmc.csv")), dmcFile));
	}

	@FXML
	public void save() {
		if (save.isDisable() == true) {
			return;
		}
		setTitleChanged(false);
		try {
			Resources.writeObject(dmcFile, canvasController.getImage());
			LogPrinter.alert(Resources.getString("file_saved", dmcFile.getName(), Resources.getString("dmc_file")));
			Preferences.setValue("autoLoadFile", dmcFile.getPath());
		} catch (IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("save_failed", Resources.getString("dmc_file")));
		}
	}

	@FXML
	public void saveTo() {
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
			save();
		}
	}

	@FXML
	public void exportImage() {
		if (exportPng.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf(".")) + ".png");
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
	public void exportColor() {
		if (exportCsv.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf(".")) + ".act");
		final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_act"), "*.act");
		fileChooser.getExtensionFilters().add(extensionFilter);
		final File actFile = fileChooser.showSaveDialog(overviewStage);
		if (actFile != null) {
			if (actFile.exists()) {
				actFile.delete();
			}
			try (OutputStream outputStream = new FileOutputStream(actFile)) {
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

	@FXML
	public void exportBlueprint() {
		if (exportBlueprint.isDisable() == true) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(
				dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf(".")) + "_blueprint.png");
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
	public void setting() {
		ScrollPane page = null;
		try {
			final FXMLLoader loader = new FXMLLoader(new File("resources/Setting.fxml").toURI().toURL(),
					Resources.getBundle());
			page = (ScrollPane) loader.load();
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

	public void setDividerPosition() {
		horizontalSplitPane.setDividerPositions((double) (1 - 314 / borderPane.getWidth()));
	}

	@FXML
	public void author() {
		FXMLLoader loader = null;
		AnchorPane page = null;
		try {
			loader = new FXMLLoader(new File("resources/Author.fxml").toURI().toURL(), Resources.getBundle());
			page = (AnchorPane) loader.load();
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

	private void closeWindowEvent(WindowEvent event) {
		if (confirmExit() == false) {
			event.consume();
		} else {
			if(Preferences.getBoolean("autoLoad", false) == true) {
				Preferences.setValue("scrollX", String.valueOf(canvasScrollPane.getHvalue()));
				Preferences.setValue("scrollY", String.valueOf(canvasScrollPane.getVvalue()));
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
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == save) {
				save();
				return true;
			} else if (result.get() == notSave) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	@FXML
	public void onShowNumberCheckboxClicked() {
		Preferences.setValue("drawGridNumber", Boolean.toString(showNumberCheckbox.isSelected()));
		canvasController.invalidate();
	}

	private String fileName = "";

	public void setTitleChanged(final boolean changed) {
		canvasController.getImage().setChanged(changed);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (changed == true) {
					overviewStage.setTitle(fileName + " (*)");
				} else {
					overviewStage.setTitle(fileName);
				}
			}
		});
	}

	@FXML
	public void initialize() {
		highlightColumn.setCellFactory(column -> new CheckBoxTableCell<>());
		highlightColumn.setCellValueFactory(cellData -> {
			final StitchList cellValue = cellData.getValue();
			final BooleanProperty property = cellValue.highlightProperty();
			cellValue.setHighlight(property.get());
			property.addListener((observable, oldValue, newValue) -> {
				cellValue.setHighlight(newValue);
				cellValue.getPixelList().setHighlighted(newValue);
				if (newValue == true && cellValue.isCompleted() == true) {
					cellValue.setCompleted(false);
				}
				setTitleChanged(true);
				invalidate();
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
				invalidate();
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
						this.setStyle("-fx-background-color:" + item);
					}
				}
			};
		});
		colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorStringProperty());
		nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

		zoom.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				event.consume();
				final String zoomScale = zoom.getText().replace("%", "");
				if(setZoom(zoomScale) == true) {
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
		if (Preferences.getBoolean("showColorTable", true) == false) {
			toggleColorTableItem.setSelected(false);
			toggleWindowColorTable();
		}

	}

	public void setImage(final StitchImage stitchImage) {
		canvasController = new CanvasController(stitchImage, canvas);
		final ObservableList<StitchList> stitchListArrayList = FXCollections.observableArrayList();
		final Collection<PixelList> pixelListCollection = stitchImage.getPixelLists();
		for (final PixelList pixelList : pixelListCollection) {
			stitchListArrayList.add(new StitchList(pixelList));
		}
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
		exportPng.setDisable(false);
		exportCsv.setDisable(false);
		exportBlueprint.setDisable(false);
		showNumberCheckbox.setSelected(Preferences.getBoolean("drawGridNumber", true));
		invalidate();
		if(Preferences.getBoolean("autoLoad", false) == true) {
			canvasScrollPane.setHvalue(Preferences.getDouble("scrollX", 0d));
			canvasScrollPane.setVvalue(Preferences.getDouble("scrollY", 0d));
		}
	}

	public boolean setZoom(String scale) {
		double scaleRatio = 0d;

		if (scale.equals("MATCH_WIDTH")) {
			double screenWidth = canvasScrollPane.getWidth() - canvasController.getMargin() * 2 - 12;
			double imageWidth = canvasController.getImage().getWidth();
			double ratioByWidth = screenWidth / imageWidth;

			canvasController.setScale(ratioByWidth);
			zoom.setText(scale);
		} else if (scale.equals("MATCH_SCREEN")) {
			double screenWidth = canvasScrollPane.getWidth() - canvasController.getMargin() * 2 - 12;
			double imageWidth = canvasController.getImage().getWidth();
			double ratioByWidth = screenWidth / imageWidth;

			double screenHeight = canvasScrollPane.getHeight() - canvasController.getMargin() * 2 - 12;
			double imageHeight = canvasController.getImage().getHeight();
			double ratioByHeight = screenHeight / imageHeight;

			canvasController.setScale(Math.min(ratioByWidth, ratioByHeight));
			zoom.setText(scale);
		} else {
			try {
				scaleRatio = Double.parseDouble(scale);
				canvasController.setScale(Double.parseDouble(scale));
			} catch (final NumberFormatException e) {
				LogPrinter.alert(Resources.getString("zoom_number_cant_read"));
				zoom.setText(Preferences.getValue("scale", "MATCH_WIDTH"));
				return false;
			}
			zoom.setText(scaleRatio + "x");
		}
		return true;
	}

	public void invalidate() {
		canvasController.invalidate();
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
		/*
		final ExecutorService executorService = Executors
				.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
		final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(16);
		final Runnable poisonPill = new Runnable() {
			@Override
			public void run() {

			}
		};
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						final Runnable runnable = blockingQueue.take();
						if (runnable == poisonPill) {
							executorService.shutdown();
							return;
						}
						executorService.submit(runnable);
					} catch (final InterruptedException e) {

					}
				}
			}
		});
		thread.start();
		try {
			for (StitchList stitchList : colorTable.getItems()) {
				blockingQueue.put(new Runnable() {
					@Override
					public void run() {
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
				});
			}
			blockingQueue.put(poisonPill);
		} catch (final InterruptedException e) {

		}*/
	}
}
