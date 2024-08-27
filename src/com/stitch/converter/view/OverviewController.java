package com.stitch.converter.view;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class OverviewController extends Controller {
	private Stage overviewStage;
	@FXML
	public BorderPane borderPane;
	@FXML
	public Canvas canvas;
	private CanvasController canvasController;
	@FXML
	public ScrollPane canvasScrollPane;
	@FXML
	public TableColumn<StitchList, String> colorColumn, nameColumn;
	@FXML
	public TableView<StitchList> colorTable;
	@FXML
	public TableColumn<StitchList, Boolean> highlightColumn, completeColumn;
	@FXML
	public TableColumn<StitchList, Integer> indexColumn, totalNumberColumn;
	@FXML
	public MenuItem save, saveAs, exportConvertedImage, exportStitchList, exportBlueprint;
	@FXML
	public CheckMenuItem showNumberItem, toggleColorTableItem, toggleLogItem;
	@FXML
	public SplitPane verticalSplitPane, horizontalSplitPane;
	@FXML
	public TextField zoom;

	private int x = -1, y = -1;
	
	private String css, style;

	public void setStage(final Stage overviewStage) {
		try {
			css = new File("resources/Style.css").toURI().toURL().toExternalForm();
			final int fontSize = Preferences.getInteger("fontSize", 13);
			final String fontType = Preferences.getValue("fontType", "Malgun Gothic");
			style = new StringBuilder("-fx-font: ").append(fontSize).append("px \"").append(fontType).append("\";").toString();
		} catch (MalformedURLException e) {
			LogPrinter.print(e);
		}
		if (Preferences.getBoolean("showColorTable", true) == false) {
			setColorTable(false);
		}
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				overviewStage.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
						(event) -> closeWindowEvent(event));
				autoLoad();
			}
		});
		checkUpdate();
		this.overviewStage = overviewStage;
	}

	private void autoLoad() {
		if (Preferences.getBoolean("autoLoad", false) == true) {
			try {
				loadDmc(new File(Preferences.getValue("autoLoadFile", "")));
			} catch (final NoSuchElementException e) {
				LogPrinter.error(Resources.getString("auto_load_file_not_found"));
			}
		}
	}

	private void checkUpdate() {
		if (Preferences.getBoolean("updateNeverRemind", false) == false) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if(hasUpdate() == true) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								final ButtonType update = new ButtonType(Resources.getString("update"), ButtonData.YES);
								final ButtonType notUpdate = new ButtonType(Resources.getString("update_no"), ButtonData.NO);
								final ButtonType neverRemind = new ButtonType(Resources.getString("update_never_remind"),
										ButtonData.OTHER);
								final Alert alert = new Alert(AlertType.INFORMATION);
								alert.getDialogPane().getStylesheets().add(css);
								alert.setTitle(Resources.getString("information"));
								alert.setHeaderText(Resources.getString("update_header"));
								alert.setContentText(Resources.getString("update_content"));
								
								final Image icon = new Image("file:resources/icon/update.png");
								alert.setGraphic(new ImageView(icon));
								((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
								
								alert.getButtonTypes().setAll(update, notUpdate, neverRemind);
								final Optional<ButtonType> result = alert.showAndWait();
								if (result.get() == update) {
									final String url = new StringBuilder(Resources.getString("url")).append("/releases").toString();
									main.getHostServices().showDocument(url);
								} else if (result.get() == neverRemind) {
									Preferences.setValue("updateNeverRemind", true);
								}
							}
						});
					}
				}
			}).start();
		}
	}
	
	static boolean hasUpdate() {
		try {
			final URL url = new URL(Resources.getString("update_url"));
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
			try (final InputStreamReader inputStreamReader = new InputStreamReader(
					httpUrlConnection.getInputStream())) {
				try (final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
					final StringBuilder stringBuilder = new StringBuilder();
					String temp;
					while ((temp = bufferedReader.readLine()) != null) {
						stringBuilder.append(temp).append("\n");
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

		}
		return false;
	}
	
	@FXML
	public void initialize() {
	    highlightColumn.setCellFactory(column -> new CheckBoxTableCell<>());
	    highlightColumn.setCellValueFactory(cellData -> {
	        StitchList cellValue = cellData.getValue();
	        BooleanProperty property = cellValue.highlightProperty();
	        cellValue.setHighlight(property.get());
	        property.addListener((observable, oldValue, newValue) -> {
	            if (newValue.equals(cellValue.isHighlighted())) {
	                cellValue.setHighlight(newValue);
	                cellValue.getPixelList().setHighlighted(newValue);
	                if (newValue && cellValue.isCompleted()) {
	                    cellValue.setCompleted(false);
	                }
	                setTitleChanged(true);
	            }
	        });
	        return property;
	    });

	    completeColumn.setCellFactory(column -> new CheckBoxTableCell<>());
	    completeColumn.setCellValueFactory(cellData -> {
	        StitchList cellValue = cellData.getValue();
	        BooleanProperty property = cellValue.completeProperty();
	        cellValue.setCompleted(property.get());
	        property.addListener((observable, oldValue, newValue) -> {
	            cellValue.setCompleted(newValue);
	            cellValue.getPixelList().setCompleted(newValue);
	            if (newValue && cellValue.isHighlighted()) {
	                cellValue.setHighlight(false);
	            }
	            setTitleChanged(true);
	        });
	        return property;
	    });

	    indexColumn.setCellValueFactory(cellData -> cellData.getValue().indexProperty().asObject());

	    totalNumberColumn.setCellValueFactory(cellData -> cellData.getValue().totalNumberProperty().asObject());

	    colorColumn.setCellFactory(column -> new TableCell<StitchList, String>() {
	        @Override
	        protected void updateItem(String item, boolean empty) {
	            super.updateItem(item, empty);
	            if (!empty) {
	                this.setStyle("-fx-background-color:" + item);
	            }
	        }
	    });
	    colorColumn.setCellValueFactory(cellData -> cellData.getValue().colorStringProperty());

	    // Handle zoom input
	    zoom.setOnKeyPressed(event -> {
	        if (event.getCode() == KeyCode.ENTER) {
	            event.consume();
	            setZoom(zoom.getText());
	            invalidate();
	        }
	    });

	    // Mouse events on canvas
	    canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
	        if (event.getButton().equals(MouseButton.PRIMARY)) {
	            highlightPixel(event.getX(), event.getY());
	        } else if (event.getButton().equals(MouseButton.SECONDARY)) {
	            getClickedColor(event.getX(), event.getY());
	        }
	    });

	    // Handling distance circle drawing
	    if (Preferences.getBoolean("showDistanceCircle", false)) {
	        EventHandler<MouseEvent> drawHandler = event -> {
	            canvasController.startDrawDistanceCircle(event.getX(), event.getY());
	            invalidate();
	        };
	        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, drawHandler);
	        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, drawHandler);
	        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
	            canvasController.stopDrawDistanceCircle();
	            invalidate();
	        });
	    }

	    canvas.requestFocus();
	}

	
	public void setImage(final StitchImage stitchImage) {
	    canvasController = new CanvasController(stitchImage, canvas);

	    ObservableList<StitchList> stitchListArrayList = FXCollections.observableArrayList(
	        stitchList -> new Observable[]{stitchList.highlightProperty(), stitchList.completeProperty()}
	    );

	    stitchImage.getPixelLists().forEach(pixelList -> stitchListArrayList.add(new StitchList(pixelList)));

	    stitchListArrayList.addListener((ListChangeListener<StitchList>) c -> {
	        if (c.next() && c.wasUpdated()) {
	            Platform.runLater(this::invalidate);
	        }
	    });

	    colorTable.setItems(stitchListArrayList);
	    setZoom(Preferences.getValue("scale", "MATCH_WIDTH"));

	    enableCanvasInteraction();
	}
	
	private void enableCanvasInteraction() {
	    canvas.setDisable(false);
	    canvas.requestFocus();
	    colorTable.setDisable(false);
	    zoom.setDisable(false);
	    save.setDisable(false);
	    saveAs.setDisable(false);
	    exportConvertedImage.setDisable(false);
	    exportStitchList.setDisable(false);
	    exportBlueprint.setDisable(false);

	    showNumberItem.setSelected(Preferences.getBoolean("drawGridNumber", true));
	    invalidate();

	    if (Preferences.getBoolean("autoLoad", false)) {
	        canvasScrollPane.setHvalue(Preferences.getDouble("scrollX", 0d));
	        canvasScrollPane.setVvalue(Preferences.getDouble("scrollY", 0d));
	    }

	    canvasScrollPane.widthProperty().addListener(createSizeChangeListener());
	    canvasScrollPane.heightProperty().addListener(createSizeChangeListener());
	}
	
	private ChangeListener<Number> createSizeChangeListener() {
	    return (observable, oldValue, newValue) -> {
	        if (Preferences.getValue("scale", "MATCH_SCREEN").contains("MATCH")) {
	            setZoom(Preferences.getValue("scale", "MATCH_SCREEN"));
	            invalidate();
	        }
	    };
	}

	private void getClickedColor(double originalX, double originalY) {
	    int x = (int) ((originalX - canvasController.getMargin()) / canvasController.getScale());
	    int y = (int) ((originalY - canvasController.getMargin()) / canvasController.getScale());
	    Pixel pixel = new Pixel(x, y, null);

	    colorTable.getItems().stream()
	        .filter(stitchList -> stitchList.getPixelList().hasPixel(pixel))
	        .findFirst()
	        .ifPresent(stitchList -> Platform.runLater(() -> {
	            colorTable.requestFocus();
	            colorTable.getSelectionModel().select(stitchList);
	            colorTable.scrollTo(stitchList);
	        }));
	}
	
	public void invalidate() {
		canvasController.invalidate();
	}

	public void highlightPixel(KeyCode code) {
	    if (x == -1 || y == -1) return;

	    switch (code) {
	        case W -> highlightPixel(x, y - 1);
	        case S -> highlightPixel(x, y + 1);
	        case A -> highlightPixel(x - 1, y);
	        case D -> highlightPixel(x + 1, y);
	        default -> {}
	    }
	}

	private void highlightPixel(double originalX, double originalY) {
	    int x = (int) ((originalX - canvasController.getMargin()) / canvasController.getScale());
	    int y = (int) ((originalY - canvasController.getMargin()) / canvasController.getScale());
	    highlightPixel(x, y);
	}

	private void highlightPixel(int x, int y) {
	    if (x == -1 || y == -1) return;

	    Pixel pixel = new Pixel(x, y, new StitchColor(0, null));

	    colorTable.getItems().stream()
	        .filter(stitchList -> stitchList.getPixelList().hasPixel(pixel))
	        .findFirst()
	        .ifPresent(stitchList -> {
	            this.x = (this.x == x && this.y == y) ? -1 : x;
	            this.y = (this.x == x && this.y == y) ? -1 : y;

	            Platform.runLater(() -> {
	                canvasController.setHighlightPixel(this.x, this.y);
	                canvasController.invalidate();
	            });
	        });
	}

	private void closeWindowEvent(WindowEvent event) {
	    if (Preferences.getBoolean("autoLoad", false)) {
	        Preferences.setValue("scrollX", canvasScrollPane.getHvalue());
	        Preferences.setValue("scrollY", canvasScrollPane.getVvalue());
	    }
	    Preferences.store();

	    if (!confirmExit()) {
	        event.consume();
	    }
	}
	
	private File dmcFile, csvFile;
	private String name;
	private FileChooser loadFileChooser;
	private FileChooser.ExtensionFilter dmcFilter;
	
	@FXML
	public void loadMenu() {
		if (confirmExit() == false) {
			return;
		}

		if (csvFile == null) {
			csvFile = new File(Preferences.getValue("csvFile", "resources/dmc.csv"));
		}

		if (loadFileChooser == null) {
			loadFileChooser = new FileChooser();
			loadFileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
			dmcFilter = new FileChooser.ExtensionFilter(Resources.getString("filter_dmc"), "*.dmc");
			final FileChooser.ExtensionFilter allFileFilter = new FileChooser.ExtensionFilter(
					Resources.getString("filter_all"), "*.*");
			final FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
					Resources.getString("filter_image"), "*.bmp", "*.jpg", "*.jpeg", "*.gif", "*.png", "*.apng");
			loadFileChooser.getExtensionFilters().add(allFileFilter);
			loadFileChooser.getExtensionFilters().add(dmcFilter);
			loadFileChooser.getExtensionFilters().add(imageFilter);
		}
		final File file = loadFileChooser.showOpenDialog(overviewStage);

		if (file != null) {
			final String extension = getExtension(file);
			try {
				if (extension != null && extension.equals(".dmc")) {
					loadDmc(file);
				} else {
					makeNewFile(file);
				}
			} catch(Exception e) {
				LogPrinter.error(Resources.getString("cant_read_image"));
			}
		}
	}

	private void loadDmc(final File file) {
		dmcFile = file;
		overviewStage.setTitle(dmcFile.getName());
		main.load(new GraphicsEngine.Builder(csvFile, dmcFile));
		name = dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf("."));
	}

	private void makeNewFile(final File file) {
		Preferences.setValue("scrollX", "0");
		Preferences.setValue("scrollY", "0");
		final GraphicsEngine.Builder builder = new GraphicsEngine.Builder(csvFile, file);
		builder.setColorLimit(Preferences.getInteger("maximumColorLimit", 0));
		builder.setBackground(Preferences.getColor("backgroundColor", new StitchColor(Color.WHITE, "")));
		builder.setThreadCount(Preferences.getInteger("workingThread", 0));
		builder.setScaled(Preferences.getBoolean("resizeImage", true));
		dmcFile = new File(new StringBuilder(file.getParent()).append(File.separator)
				.append(file.getName().substring(0, file.getName().lastIndexOf("."))).append(".dmc").toString());
		overviewStage.setTitle(new StringBuilder(dmcFile.getName()).append("(*)").toString());
		main.startConversion(builder);
		name = dmcFile.getName().substring(0, dmcFile.getName().lastIndexOf("."));
	}
	
	private static String getExtension(final File file) {
		final String name = file.getName();
		final int lastIndexOf = name.lastIndexOf(".");
		if (lastIndexOf == -1) {
			return null;
		}
		return name.substring(lastIndexOf);
	}
	

	@FXML
	public boolean saveMenu() {
		if (save.isDisable() == true) {
			return false;
		}
		setTitleChanged(false);
		Resources.writeObject(dmcFile, canvasController.getImage());
		Preferences.setValue("autoLoadFile", dmcFile.getPath());
		return true;
	}

	private FileChooser saveToFileChooser;
	@FXML
	public void saveAsMenu() {
		if (saveAs.isDisable() == true) {
			return;
		}
		if(saveToFileChooser == null) {
			saveToFileChooser = new FileChooser();
			saveToFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
			saveToFileChooser.getExtensionFilters().add(dmcFilter);
		}
		final File saveFile = saveToFileChooser.showSaveDialog(overviewStage);
		if (saveFile != null) {
			dmcFile = saveFile;
			saveMenu();
		}
	}
	

	private Alert confirmExitAlert;
	private ButtonType saveButton, notSaveButton;

	private boolean confirmExit() {
		if (canvasController != null && canvasController.getImage().isChanged() == true) {
			if (confirmExitAlert == null) {
				saveButton = new ButtonType(Resources.getString("save"), ButtonData.YES);
				notSaveButton = new ButtonType(Resources.getString("save_no"), ButtonData.NO);
				final ButtonType cancelButton = new ButtonType(Resources.getString("cancel_button"),
						ButtonData.CANCEL_CLOSE);
				confirmExitAlert = new Alert(AlertType.CONFIRMATION);
				confirmExitAlert.getDialogPane().getStylesheets().add(css);
				confirmExitAlert.setTitle(Resources.getString("warning"));
				confirmExitAlert.setHeaderText(Resources.getString("file_changed_header"));
				confirmExitAlert.setContentText(Resources.getString("file_changed"));
				
				final Image icon = new Image("file:resources/icon/information.png");
				confirmExitAlert.setGraphic(new ImageView(icon));
				((Stage)confirmExitAlert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
				
				confirmExitAlert.getButtonTypes().setAll(saveButton, notSaveButton, cancelButton);
			}
			final Optional<ButtonType> result = confirmExitAlert.showAndWait();
			if (result.get() == saveButton) {
				saveMenu();
				return true;
			} else if (result.get() == notSaveButton) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	private FileChooser blueprintFileChooser;
	private FileChooser.ExtensionFilter pngExtensionFilter;
	private Canvas blueprintCanvas;
	private CanvasController blueprintController;
	private Blueprint blueprint;
	private WritableImage blueprintWritableImage;

	@FXML
	public void exportBlueprintMenu() {
		if (exportBlueprint.isDisable() == true) {
			return;
		}
		if (blueprintFileChooser == null) {
			blueprintFileChooser = new FileChooser();
			blueprintFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
			blueprintFileChooser.setInitialFileName(new StringBuilder(name).append("_blueprint.png").toString());
			if(pngExtensionFilter == null) {
				pngExtensionFilter = new FileChooser.ExtensionFilter(Resources.getString("png_file"), "*.png");
			}
			blueprintFileChooser.getExtensionFilters().add(pngExtensionFilter);

			blueprintCanvas = new Canvas(canvasController.getCanvas().getWidth(),
					canvasController.getCanvas().getHeight());
			blueprintController = new CanvasController(canvasController.getImage(), blueprintCanvas);
			blueprint = new Blueprint(canvasController.getImage(), blueprintCanvas);
			blueprint.setScale(Preferences.getDouble("blueprintScale", 20d));
			blueprint.setListScale(Preferences.getDouble("blueprintListScale", 20d));
			blueprintWritableImage = new WritableImage((int) blueprint.getCanvas().getWidth(),
					(int) blueprint.getCanvas().getHeight());
		}
		final File blueprintFile = blueprintFileChooser.showSaveDialog(overviewStage);
		if (blueprintFile != null) {
			blueprintController.invalidate();
			blueprint.invalidate();
			blueprint.getCanvas().snapshot(null, blueprintWritableImage);
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(blueprintWritableImage, null), "png", blueprintFile);
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("save_failed", Resources.getString("blueprint_file")));
			}
		}
	}
	
	private FileChooser convertedImageFileChooser;

	@FXML
	public void exportConvertedImageMenu() {
		if (exportConvertedImage.isDisable() == true) {
			return;
		}
		if(convertedImageFileChooser == null) {
			convertedImageFileChooser = new FileChooser();
			convertedImageFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
			convertedImageFileChooser.setInitialFileName(new StringBuilder(name).append(".png").toString());
			if(pngExtensionFilter == null) {
				pngExtensionFilter = new FileChooser.ExtensionFilter(Resources.getString("png_file"), "*.png");
			}
			convertedImageFileChooser.getExtensionFilters().add(pngExtensionFilter);
		}
		final File imageFile = convertedImageFileChooser.showSaveDialog(overviewStage);
		if (imageFile != null) {
			final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(canvasController.getImage().getFXImage(),
					null);
			try {
				ImageIO.write(bufferedImage, "png", imageFile);
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("save_failed", Resources.getString("image_file")));
			}
		}
	}

	private FileChooser exportStitchFileChooser;

	@FXML
	public void exportStitchListMenu() {
		if (exportStitchList.isDisable() == true) {
			return;
		}
		if (exportStitchFileChooser == null) {
			exportStitchFileChooser = new FileChooser();
			exportStitchFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
			exportStitchFileChooser.setInitialFileName(new StringBuilder(name).append(".act").toString());
			final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
					Resources.getString("filter_act"), "*.act");
			exportStitchFileChooser.getExtensionFilters().add(extensionFilter);
		}
		final File actFile = exportStitchFileChooser.showSaveDialog(overviewStage);
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
				LogPrinter.error(Resources.getString("save_failed", Resources.getString("act_file")));
			}
		}
	}

	@FXML
	public void onShowNumberItemClicked() {
		Preferences.setValue("drawGridNumber", Boolean.toString(showNumberItem.isSelected()));
		if(canvasController != null) {
			canvasController.invalidate();
		}
	}

	private Stage settingStage;

	@FXML
	public void setting() {
		if (settingStage == null) {
			ScrollPane page = null;
			try {
				final FXMLLoader loader = new FXMLLoader();
				loader.setLocation(new File("resources/Setting.fxml").toURI().toURL());
				loader.setResources(Resources.getBundle());
				page = (ScrollPane) loader.load();
				page.setStyle(style);
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("read_failed", Resources.getString("layout")));
				return;
			}
			settingStage = new Stage();
			settingStage.setTitle(Resources.getString("setting"));
			try {
				final Image icon = new Image("file:resources/icon/setting.png");
				settingStage.getIcons().add(icon);
				
			} catch (final Exception iconException) {
				LogPrinter.print(iconException);
				LogPrinter.error(Resources.getString("error_icon_load"));
			}
			settingStage.initModality(Modality.WINDOW_MODAL);
			settingStage.initOwner(overviewStage);
			settingStage.setResizable(false);
			final Scene scene = new Scene(page);
			settingStage.setScene(scene);
		}
		settingStage.showAndWait();
	}

	public void setTitleChanged(final boolean changed) {
		canvasController.getImage().setChanged(changed);
		if (changed == true) {
			overviewStage.setTitle(new StringBuilder(dmcFile.getName()).append("(*)").toString());
		} else {
			overviewStage.setTitle(dmcFile.getName());
		}
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
				if(scaleRatio <= 0) {
					scaleRatio = 2d;
				}
				if(scaleRatio >= 15) {
					scaleRatio = 15d;
				}
				canvasController.setScale(Double.parseDouble(scale));
				zoom.setText(Double.toString(scaleRatio));
			} catch (final NumberFormatException e) {
				canvasController.setScale(ratioByWidth);
				zoom.setText(Preferences.getValue("scale", "MATCH_WIDTH"));
				return false;
			}
		}
		Preferences.setValue("scale", zoom.getText());
		canvas.requestFocus();
		return true;
	}

	@FXML
	public void toggleWindowColorTable() {
		if (toggleColorTableItem.isSelected() == true) {
			setColorTable(true);
		} else {
			setColorTable(false);
		}
	}

	private void setColorTable(boolean enable) {
		if (enable == true) {
			toggleColorTableItem.setSelected(true);
			Preferences.setValue("showColorTable", "true");
			if (borderPane.getChildren().contains(colorTable) == false) {
				borderPane.setRight(colorTable);
			}
		} else {
			toggleColorTableItem.setSelected(false);
			Preferences.setValue("showColorTable", "false");
			if (borderPane.getChildren().contains(colorTable) == true) {
				borderPane.setRight(null);
			}
		}
	}

	private Stage authorStage;

	@FXML
	public void author() {
		if (authorStage == null) {
			final FXMLLoader loader;
			final AnchorPane page;
			try {
				loader = new FXMLLoader();
				loader.setLocation(new File("resources/Author.fxml").toURI().toURL());
				loader.setResources(Resources.getBundle());
				page = (AnchorPane) loader.load();
				page.setStyle(style);
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("read_failed", Resources.getString("layout")));
				return;
			}
			authorStage = new Stage();
			authorStage.setTitle(Resources.getString("about"));
			try {
				final Image icon = new Image("file:resources/icon/author.png");
				authorStage.getIcons().add(icon);
				
			} catch (final Exception iconException) {
				LogPrinter.print(iconException);
				LogPrinter.error(Resources.getString("error_icon_load"));
			}
			authorStage.initModality(Modality.WINDOW_MODAL);
			authorStage.initOwner(overviewStage);
			authorStage.setResizable(false);
			final Scene scene = new Scene(page);
			authorStage.setScene(scene);
			final AuthorController controller = loader.getController();
			controller.setApp(main);
		}
		authorStage.showAndWait();
	}
}
