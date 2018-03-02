package com.stitch.converter.view;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.stitch.converter.GraphicsEngine;
import com.stitch.converter.LogPrinter;
import com.stitch.converter.Main;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
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
	public BorderPane imageContainer;
	@FXML
	public MenuItem save, saveTo, exportPng, exportCsv, exportBlueprint;
	@FXML
	public TableView<StitchList> colorTable;
	@FXML
	public TableColumn<StitchList, Boolean> highlightColumn, completeColumn;
	@FXML
	public TableColumn<StitchList, Integer> indexColumn;
	@FXML
	public TableColumn<StitchList, String> colorColumn, nameColumn;
	@FXML
	public Canvas canvas;
	@FXML
	public TextArea log;
	@FXML
	private TextField zoom;
	@FXML
	private CheckBox showNumberCheckbox;

	private Stage overviewStage;

	private File dmcFile;

	private CanvasController canvasController;

	public void setStage(final Stage overviewStage) {
		this.overviewStage = overviewStage;
		this.overviewStage.setOnCloseRequest(confirmCloseEventHandler);
	}

	private EventHandler<WindowEvent> confirmCloseEventHandler = event -> {
		if (confirmExit() == false) {
			event.consume();
		}
	};

	@FXML
	public void newfile() {
		if (confirmExit() == false) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		final FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_image"), "*.bmp", "*.jpg", "*.jpeg", "*.gif", "*.png", "*.apng");
		final FileChooser.ExtensionFilter allFileFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_all"), "*.*");
		fileChooser.getExtensionFilters().add(imageFilter);
		fileChooser.getExtensionFilters().add(allFileFilter);

		final File file = fileChooser.showOpenDialog(overviewStage);

		if (file != null) {
			LogPrinter.print(Resources.getString("start", file.getName()));

			final GraphicsEngine.Builder builder = new GraphicsEngine.Builder(
					new File(Preferences.getString("csvFile", "dmc.csv")), file);
			builder.setMaximumColor(Preferences.getInteger("maximumColorLimit", 0));
			builder.setBackground(Preferences.getColor("backgroundColor", Color.WHITE));
			builder.setThread(Preferences.getInteger("workingThread", 0));
			builder.setResize(Preferences.getBoolean("resizeImage", true));
			
			try {
				dmcFile = new File(file.getParent() + File.separator
						+ file.getName().substring(0, file.getName().lastIndexOf(".")) + ".dmc");
				fileName = dmcFile.getName();
				main.startConversion(builder);
			} catch (final IllegalArgumentException | NullPointerException e) {
				LogPrinter.print(e);
				LogPrinter.print(Resources.getString("cant_read_image"));
				LogPrinter.print(Resources.getString("cancel", Resources.getString("file_open")));
				return;
			}
		} else {
			LogPrinter.print(Resources.getString("cancel", Resources.getString("file_open")));
			return;
		}
	}

	@FXML
	public void load() {
		if (confirmExit() == false) {
			return;
		}
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		final FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_dmc"), "*.dmc");
		final FileChooser.ExtensionFilter allFileFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_all"), "*.*");
		fileChooser.getExtensionFilters().add(imageFilter);
		fileChooser.getExtensionFilters().add(allFileFilter);
		dmcFile = fileChooser.showOpenDialog(overviewStage);
		if (dmcFile != null) {
			overviewStage.setTitle(dmcFile.getName());
			LogPrinter.print(Resources.getString("load_file", dmcFile.getName()));
			main.load(new GraphicsEngine.Builder(new File("dmc.csv"), dmcFile));
		} else {
			LogPrinter.print(Resources.getString("cancel", Resources.getString("load")));
		}
	}

	@FXML
	public void save() {
		setTitleChanged(false);
		try {
			Resources.write(dmcFile, canvasController.getImage());
			LogPrinter.print(Resources.getString("file_saved", dmcFile.getName(), Resources.getString("dmc_file")));
		} catch (IOException e) {
			e.printStackTrace();
			LogPrinter.print(Resources.getString("save_failed", Resources.getString("dmc_file")));
		}
	}

	@FXML
	public void saveTo() {
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
				LogPrinter.print(Resources.getString("file_saved", Resources.getString("image_file")));
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.print(Resources.getString("save_failed", Resources.getString("image_file")));
			}
		}
	}

	@FXML
	public void exportColor() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(dmcFile.getParent()));
		fileChooser.setInitialFileName(dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf(".")) + ".csv");
		final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
				Resources.getString("filter_csv"), "*.csv");
		fileChooser.setSelectedExtensionFilter(extensionFilter);
		final File textFile = fileChooser.showSaveDialog(overviewStage);
		if (textFile != null) {
			if (textFile.exists()) {
				textFile.delete();
			}
			try (final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(textFile))) {
				final String tabSeparator = Resources.getString("tab_separator");
				final String lineSeparator = Resources.getString("line_separator");
				textFile.createNewFile();
				bufferedWriter.write(Resources.getString("color_name") + tabSeparator + Resources.getString("red")
						+ tabSeparator + Resources.getString("green") + tabSeparator + Resources.getString("blue")
						+ lineSeparator);
				for (final StitchColor color : canvasController.getImage().getColorList()) {
					bufferedWriter.write(color.getName() + tabSeparator + color.getRed() + tabSeparator
							+ color.getGreen() + tabSeparator + color.getBlue() + lineSeparator);
				}
				bufferedWriter.flush();
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.print(Resources.getString("save_failed", Resources.getString("txt_file")));
			}
		}
	}

	@FXML
	public void exportBlueprint() {
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
			LogPrinter.print(Resources.getString("save_failed", Resources.getString("blueprint_file")));
		}
	}
	
	@FXML
	public void setting() {
		final FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/Setting.fxml"),
				Resources.getBundle());
		ScrollPane page = null;
		try {
			page = (ScrollPane) loader.load();
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.print(Resources.getString("read_failed", Resources.getString("layout")));
			return;
		}
		final Stage settingStage = new Stage();
		settingStage.setTitle(Resources.getString("setting_button"));
		settingStage.initModality(Modality.WINDOW_MODAL);
		settingStage.initOwner(overviewStage);
		settingStage.setResizable(false);
		final Scene scene = new Scene(page);
		settingStage.setScene(scene);
		settingStage.showAndWait();
	}
	
	@FXML
	public void author() {
		final FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/Author.fxml"),
				Resources.getBundle());
		
		AnchorPane page = null;
		try {
			page = (AnchorPane) loader.load();
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.print(Resources.getString("read_failed", Resources.getString("layout")));
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

	public boolean confirmExit() {
		if (canvasController != null && canvasController.getImage().isChanged() == true) {
			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle(Resources.getString("warning"));
			alert.setHeaderText(Resources.getString("file_changed_header"));
			alert.setContentText(Resources.getString("file_changed"));

			final ButtonType save = new ButtonType(Resources.getString("save"), ButtonData.YES);
			final ButtonType notSave = new ButtonType(Resources.getString("save_no"), ButtonData.NO);
			final ButtonType cancel = new ButtonType(Resources.getString("cancel_button"), ButtonData.CANCEL_CLOSE);

			alert.getButtonTypes().setAll(save, notSave, cancel);

			final Optional<ButtonType> result = alert.showAndWait();
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
	private void onShowNumberCheckboxClicked() {
		canvasController.setShowIndex(showNumberCheckbox.isSelected());
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
	private void initialize() {
		LogPrinter.setPrinter(new LogPrinter.Logger() {
			@Override
			public void print(final String str) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						try {
							log.appendText(str + "\n");
						} catch(final NullPointerException e) {
							System.out.println(str);
						}
					}
				});
			}

			@Override
			public void print(final Throwable throwable) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						try {
							final StringWriter errors = new StringWriter();
							throwable.printStackTrace(new PrintWriter(errors));
							log.appendText(errors.toString() + "\n");
						} catch(final NullPointerException e) {
							throwable.printStackTrace();
						}
					}
				});
			}
		});
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
				try {
					final double scale = Double.parseDouble(zoom.getText().replace("%", "")) / 100d;
					if (scale < 2) {
						throw new IllegalArgumentException();
					}
					setZoom(scale);
					zoom.setText((scale * 100) + "%");
				} catch (final NumberFormatException e) {
					LogPrinter.print(Resources.getString("zoom_number_cant_read"));
					zoom.setText((canvasController.getScale() * 100) + "%");
				} catch (final IllegalArgumentException e) {
					LogPrinter.print(Resources.getString("cant_zoomout_more"));
					zoom.setText((canvasController.getScale() * 100) + "%");
				}
			}
		});

		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent event) {
				clickCanvas(event.getX(), event.getY());
			}
		});
	}
	
	public void setImage(final StitchImage stitchImage, final double scale) {
		canvasController = new CanvasController(stitchImage, canvas);
		canvasController.setScale(scale);
		zoom.setText((scale * 100) + "%");
		final ObservableList<StitchList> stitchListArrayList = FXCollections.observableArrayList();
		final Collection<PixelList> pixelListCollection = stitchImage.getPixelLists();
		for (final PixelList pixelList : pixelListCollection) {
			stitchListArrayList.add(new StitchList(pixelList));
		}
		colorTable.setItems(stitchListArrayList);
		invalidate();
		canvas.setDisable(false);
		colorTable.setDisable(false);
		zoom.setDisable(false);
		showNumberCheckbox.setDisable(false);
		save.setDisable(false);
		saveTo.setDisable(false);
		exportPng.setDisable(false);
		exportCsv.setDisable(false);
		exportBlueprint.setDisable(false);
		showNumberCheckbox.setSelected(stitchImage.isNumberVisible());
	}

	public void setZoom(double scale) {
		canvasController.setScale(scale);
		invalidate();
	}

	public void invalidate() {
		canvasController.invalidate();
	}

	private void clickCanvas(final double originalX, final double originalY) {
		final int x = (int) (originalX / canvasController.getScale());
		final int y = (int) (originalY / canvasController.getScale());
		final Pixel pixel = new Pixel(x, y, null);
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
			for (PixelList pixelList : canvasController.getImage().getPixelLists()) {
				blockingQueue.put(new Runnable() {
					@Override
					public void run() {
						if (pixelList.hasPixel(pixel)) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									colorTable.requestFocus();
									colorTable.getSelectionModel().select(pixelList.getIndex());
									colorTable.getFocusModel().focus(pixelList.getIndex());
									colorTable.scrollTo(pixelList.getIndex());
								}
							});
						}
					}
				});
			}
			blockingQueue.put(poisonPill);
		} catch (final InterruptedException e) {

		}
	}
}