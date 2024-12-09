package com.stitch.converter.view;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.stitch.converter.GraphicsEngine;
import com.stitch.converter.LogPrinter;
import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;
import com.stitch.converter.model.StitchImage;
import com.stitch.converter.model.Pixel;
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
	private Stage overviewStage, settingStage;
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
	public MenuItem save, saveAs, exportConvertedImage, exportBlueprint;
	@FXML
	public CheckMenuItem showNumberItem, toggleColorTableItem, toggleLogItem;
	@FXML
	public SplitPane verticalSplitPane, horizontalSplitPane;
	@FXML
	public TextField zoom;

	private int x = -1, y = -1;
	
	private String css, style;
	
    // Executor for managing threads
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void setStage(final Stage overviewStage) {
        try {
            initializeStyle();
        } catch (MalformedURLException e) {
            LogPrinter.print(e);
        }

        setColorTable(Preferences.getBoolean("showColorTable", true));
        setupStageCloseEvent(overviewStage);
        autoLoad();

        updateApplication();
        this.overviewStage = overviewStage;
    }

    private void initializeStyle() throws MalformedURLException {
        css = new File("resources/Style.css").toURI().toURL().toExternalForm();
        int fontSize = Preferences.getInteger("fontSize", 13);
        String fontType = Preferences.getValue("fontType", "Malgun Gothic");
        style = String.format("-fx-font: %dpx \"%s\";", fontSize, fontType);
    }

    private void setupStageCloseEvent(final Stage overviewStage) {
        Platform.runLater(() -> overviewStage.getScene().getWindow().addEventHandler(
                WindowEvent.WINDOW_CLOSE_REQUEST, 
                this::closeWindowEvent));
    }

    private void updateApplication() {
        UpdateChecker updateChecker = new UpdateChecker();
        updateChecker.checkUpdate();
    }

	
	private void setColorTable(boolean enable) {
	    toggleColorTableItem.setSelected(enable);
	    Preferences.setValue("showColorTable", Boolean.toString(enable));
	    if (enable) {
	        if (!borderPane.getChildren().contains(colorTable)) {
	            borderPane.setRight(colorTable);
	        }
	    } else {
	        if (borderPane.getChildren().contains(colorTable)) {
	            borderPane.setRight(null);
	        }
	    }
	}

	private void autoLoad() {
		if (Preferences.getBoolean("autoLoad", false)) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
					try {
						loadDmc(new File(Preferences.getValue("autoLoadFile", "")));
					} catch (final NoSuchElementException e) {
						LogPrinter.error(Resources.getString("auto_load_file_not_found"));
					}
				}
			
		});
		}
	}
	
	@FXML
	public void initialize() {
	    setupHighlightColumn();
	    setupCompleteColumn();
	    setupIndexColumn();
	    setupTotalNumberColumn();
	    setupColorColumn();
	    setupNameColumn();
	    setupZoomHandler();
	    setupCanvasMouseHandlers();
	    setupDistanceCircleDrawing();

	    canvas.requestFocus();
	}

	private void setupHighlightColumn() {
	    highlightColumn.setCellFactory(column -> new CheckBoxTableCell<>());
	    highlightColumn.setCellValueFactory(cellData -> {
	        StitchList cellValue = cellData.getValue();
	        BooleanProperty property = cellValue.highlightProperty();
	        cellValue.setHighlight(property.get());
	        property.addListener((observable, oldValue, newValue) -> {
	            cellValue.setHighlight(newValue);
	            cellValue.getPixelList().setHighlighted(newValue);
	            if (newValue && cellValue.isCompleted()) {
	                cellValue.setCompleted(false);
	            }
	            setTitleChanged(true);
	        });
	        return property;
	    });
	}

	private void setupCompleteColumn() {
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
	}

	private void setupIndexColumn() {
	    indexColumn.setCellValueFactory(cellData -> cellData.getValue().indexProperty().asObject());
	}

	private void setupTotalNumberColumn() {
	    totalNumberColumn.setCellValueFactory(cellData -> cellData.getValue().totalNumberProperty().asObject());
	}

	private void setupColorColumn() {
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
	}
	
	private void setupNameColumn() {
	    nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
	}

	private void setupZoomHandler() {
	    zoom.setOnKeyPressed(event -> {
	        if (event.getCode() == KeyCode.ENTER) {
	            event.consume();
	            setZoom(zoom.getText());
	            invalidate();
	        }
	    });
	}

	private void setupCanvasMouseHandlers() {
	    canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
	        if (event.getButton().equals(MouseButton.PRIMARY)) {
	            highlightPixel(event.getX(), event.getY());
	        } else if (event.getButton().equals(MouseButton.SECONDARY)) {
	            getClickedColor(event.getX(), event.getY());
	        }
	    });
	}

	private void setupDistanceCircleDrawing() {
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
	}

	
	public boolean setZoom(String scale) {
	    double ratioByWidth = calculateWidthRatio();
	    double ratioByHeight = calculateHeightRatio();

	    scale = scale.toUpperCase();

	    switch (scale) {
	        case "MATCH_WIDTH":
	            return applyScale(ratioByWidth, scale);
	        case "MATCH_HEIGHT":
	            return applyScale(ratioByHeight, scale);
	        case "MATCH_SCREEN":
	            return applyScale(Math.min(ratioByWidth, ratioByHeight), scale);
	        default:
	            return parseAndSetScale(scale, ratioByWidth);
	    }
	}

	private double calculateWidthRatio() {
	    double screenWidth = canvasScrollPane.getWidth() - canvasController.getMargin() * 2 - 12;
	    double imageWidth = canvasController.getImage().getWidth();
	    return Math.max(2.0, screenWidth / imageWidth);
	}

	private double calculateHeightRatio() {
	    double screenHeight = canvasScrollPane.getHeight() - canvasController.getMargin() * 2 - 12;
	    double imageHeight = canvasController.getImage().getHeight();
	    return Math.max(2.0, screenHeight / imageHeight);
	}

	private boolean applyScale(double scaleValue, String scale) {
	    canvasController.setScale(scaleValue);
	    zoom.setText(scale);
	    Preferences.setValue("scale", zoom.getText());
	    canvas.requestFocus();
	    return true;
	}

	private boolean parseAndSetScale(String scale, double defaultRatio) {
	    try {
	        double scaleRatio = Double.parseDouble(scale.replace("X", ""));
	        scaleRatio = Math.max(2d, Math.min(15d, scaleRatio));
	        canvasController.setScale(scaleRatio);
	        zoom.setText(Double.toString(scaleRatio));
	    } catch (NumberFormatException e) {
	        canvasController.setScale(defaultRatio);
	        zoom.setText(Preferences.getValue("scale", "MATCH_WIDTH"));
	        return false;
	    }
	    
	    Preferences.setValue("scale", zoom.getText());
	    canvas.requestFocus();
	    return true;
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
	    System.exit(0);
	}

	public void setTitleChanged(final boolean changed) {
		canvasController.getImage().setChanged(changed);
		if (changed == true) {
			overviewStage.setTitle(new StringBuilder(dmcFile.getName()).append("(*)").toString());
		} else {
			overviewStage.setTitle(dmcFile.getName());
		}
	}
	
	private File dmcFile, csvFile;
	private String name;
	private FileChooser loadFileChooser, saveToFileChooser, blueprintFileChooser, convertedImageFileChooser;
	private FileChooser.ExtensionFilter dmcFilter, pngExtensionFilter;
	private Alert confirmExitAlert;
	private ButtonType saveButton, notSaveButton;
	private Canvas blueprintCanvas;
	private CanvasController blueprintController;
	private Blueprint blueprint;
	private WritableImage blueprintWritableImage;
	
	@FXML
	public void loadMenu() {
	    if (!confirmExit()) return;

	    initializeCsvFile();
	    initializeLoadFileChooser();

	    File file = loadFileChooser.showOpenDialog(overviewStage);

	    if (file != null) {
	        handleFileLoad(file);
	    }
	}

	private void initializeCsvFile() {
	    if (csvFile == null) {
	        csvFile = new File(Preferences.getValue("csvFile", "resources/dmc.csv"));
	    }
	}

	private void initializeLoadFileChooser() {
	    if (loadFileChooser == null) {
	        loadFileChooser = new FileChooser();
	        loadFileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
	        dmcFilter = new FileChooser.ExtensionFilter(Resources.getString("filter_dmc"), "*.dmc");
	        
	        FileChooser.ExtensionFilter allFileFilter = new FileChooser.ExtensionFilter(Resources.getString("filter_all"), "*.*");
	        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(Resources.getString("filter_image"), "*.bmp", "*.jpg", "*.jpeg", "*.gif", "*.png", "*.apng");

	        loadFileChooser.getExtensionFilters().addAll(allFileFilter, dmcFilter, imageFilter);
	    }
	}

	private void handleFileLoad(File file) {
	    String extension = getExtension(file);

	    try {
	        if (".dmc".equals(extension)) {
	            loadDmc(file);
	        } else {
	            makeNewFile(file);
	        }
	    } catch (Exception e) {
	        LogPrinter.error(Resources.getString("cant_read_image"));
	    }
	}

	private void loadDmc(File file) {
	    dmcFile = file;
	    overviewStage.setTitle(dmcFile.getName());
	    main.load(new GraphicsEngine.Builder(csvFile, dmcFile), GraphicsEngine.Mode.LOAD);
	    name = extractFileNameWithoutExtension(dmcFile);
	}

	private void makeNewFile(File file) {
	    resetScrollPreferences();

	    GraphicsEngine.Builder builder = new GraphicsEngine.Builder(csvFile, file);
	    builder.setColorLimit(Preferences.getInteger("maximumColorLimit", 0))
	           .setBackground(Preferences.getColor("backgroundColor", new StitchColor(Color.WHITE, "")))
	           .setThreadCount(Preferences.getInteger("workingThread", 0))
	           .setScaled(Preferences.getBoolean("resizeImage", true));

	    dmcFile = new File(file.getParent(), extractFileNameWithoutExtension(file) + ".dmc");
	    overviewStage.setTitle(dmcFile.getName() + "(*)");
	    main.load(new GraphicsEngine.Builder(csvFile, file), GraphicsEngine.Mode.NEW_FILE);

	    name = extractFileNameWithoutExtension(dmcFile);
	}

	private String extractFileNameWithoutExtension(File file) {
	    return file.getName().substring(0, file.getName().lastIndexOf("."));
	}

	private void resetScrollPreferences() {
	    Preferences.setValue("scrollX", "0");
	    Preferences.setValue("scrollY", "0");
	}

	private String getExtension(File file) {
	    String name = file.getName();
	    int lastIndexOf = name.lastIndexOf(".");
	    return (lastIndexOf == -1) ? null : name.substring(lastIndexOf);
	}

	@FXML
	public boolean saveMenu() {
	    if (!save.isDisable()) {
	        setTitleChanged(false);
	        Resources.writeObject(dmcFile, canvasController.getImage());
	        Preferences.setValue("autoLoadFile", dmcFile.getPath());
	        return true;
	    }
	    return false;
	}

	@FXML
	public void saveAsMenu() {
	    if (!saveAs.isDisable()) {
	        initializeSaveToFileChooser();
	        File saveFile = saveToFileChooser.showSaveDialog(overviewStage);
	        if (saveFile != null) {
	            dmcFile = saveFile;
	            saveMenu();
	        }
	    }
	}

	private void initializeSaveToFileChooser() {
	    if (saveToFileChooser == null) {
	        saveToFileChooser = new FileChooser();
	        saveToFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
	        saveToFileChooser.getExtensionFilters().add(dmcFilter);
	    }
	}

	private boolean confirmExit() {
	    if (canvasController != null && canvasController.getImage().isChanged()) {
	        initializeConfirmExitAlert();
	        Optional<ButtonType> result = confirmExitAlert.showAndWait();

	        if (result.isPresent()) {
	            if (result.get() == saveButton) {
	                saveMenu();
	                return true;
	            } else if (result.get() == notSaveButton) {
	                return true;
	            }
	        }
	        return false;
	    }
	    return true;
	}

	private void initializeConfirmExitAlert() {
	    if (confirmExitAlert == null) {
	        saveButton = new ButtonType(Resources.getString("save"), ButtonData.YES);
	        notSaveButton = new ButtonType(Resources.getString("save_no"), ButtonData.NO);
	        ButtonType cancelButton = new ButtonType(Resources.getString("cancel_button"), ButtonData.CANCEL_CLOSE);

	        confirmExitAlert = new Alert(AlertType.CONFIRMATION);
	        confirmExitAlert.getDialogPane().getStylesheets().add(css);
	        confirmExitAlert.setTitle(Resources.getString("warning"));
	        confirmExitAlert.setHeaderText(Resources.getString("file_changed_header"));
	        confirmExitAlert.setContentText(Resources.getString("file_changed"));

	        Image icon = new Image("file:resources/icon/information.png");
	        confirmExitAlert.setGraphic(new ImageView(icon));
	        ((Stage) confirmExitAlert.getDialogPane().getScene().getWindow()).getIcons().add(icon);

	        confirmExitAlert.getButtonTypes().setAll(saveButton, notSaveButton, cancelButton);
	    }
	}

	@FXML
	public void exportBlueprintMenu() {
	    if (!exportBlueprint.isDisable()) {
	        initializeBlueprintFileChooser();
	        File blueprintFile = blueprintFileChooser.showSaveDialog(overviewStage);
	        
	        if (blueprintFile != null) {
	            exportBlueprintToFile(blueprintFile);
	        }
	    }
	}

	private void initializeBlueprintFileChooser() {
	    if (blueprintFileChooser == null) {
	        blueprintFileChooser = new FileChooser();
	        blueprintFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
	        blueprintFileChooser.setInitialFileName(name + "_blueprint.png");

	        if (pngExtensionFilter == null) {
	            pngExtensionFilter = new FileChooser.ExtensionFilter(Resources.getString("png_file"), "*.png");
	        }
	        blueprintFileChooser.getExtensionFilters().add(pngExtensionFilter);
	    }

	    if (blueprintCanvas == null) {
	        blueprintCanvas = new Canvas(canvasController.getCanvas().getWidth(), canvasController.getCanvas().getHeight());
	        blueprintController = new CanvasController(canvasController.getImage(), blueprintCanvas);
	        blueprint = new Blueprint(canvasController.getImage(), blueprintCanvas);
	        blueprint.setScale(Preferences.getDouble("blueprintScale", 20d));
	        blueprint.setListScale(Preferences.getDouble("blueprintListScale", 20d));
	        blueprintWritableImage = new WritableImage((int) blueprint.getCanvas().getWidth(), (int) blueprint.getCanvas().getHeight());
	    }
	}

	private void exportBlueprintToFile(File blueprintFile) {
	    blueprintController.invalidate();
	    blueprint.invalidate();
	    blueprint.getCanvas().snapshot(null, blueprintWritableImage);

	    try {
	        ImageIO.write(SwingFXUtils.fromFXImage(blueprintWritableImage, null), "png", blueprintFile);
	    } catch (IOException e) {
	        LogPrinter.print(e);
	        LogPrinter.error(Resources.getString("save_failed", Resources.getString("blueprint_file")));
	    }
	}

	@FXML
	public void exportConvertedImageMenu() {
	    if (!exportConvertedImage.isDisable()) {
	        initializeConvertedImageFileChooser();
	        File imageFile = convertedImageFileChooser.showSaveDialog(overviewStage);

	        if (imageFile != null) {
	            exportConvertedImageToFile(imageFile);
	        }
	    }
	}

	private void initializeConvertedImageFileChooser() {
	    if (convertedImageFileChooser == null) {
	        convertedImageFileChooser = new FileChooser();
	        convertedImageFileChooser.setInitialDirectory(new File(dmcFile.getParent()));
	        convertedImageFileChooser.setInitialFileName(name + ".png");
	        convertedImageFileChooser.getExtensionFilters().add(pngExtensionFilter);
	    }
	}

	private void exportConvertedImageToFile(File imageFile) {
	    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(canvasController.getImage().getFXImage(), null);

	    try {
	        ImageIO.write(bufferedImage, "png", imageFile);
	    } catch (IOException e) {
	        LogPrinter.print(e);
	        LogPrinter.error(Resources.getString("save_failed", Resources.getString("image_file")));
	    }
	}
	
	@FXML
	public void exitMenu() {
	    if (confirmExit()) {
	        overviewStage.close();
	    }
	}

	public void close() {
	    if (confirmExit()) {
	        overviewStage.close();
	    }
	}
	
	@FXML
	public void toggleWindowColorTable() {
		if (toggleColorTableItem.isSelected() == true) {
			setColorTable(true);
		} else {
			setColorTable(false);
		}
	}
	
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
	
	@FXML
	public void onShowNumberItemClicked() {
		Preferences.setValue("drawGridNumber", Boolean.toString(showNumberItem.isSelected()));
		if(canvasController != null) {
			canvasController.invalidate();
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
	
	private class UpdateChecker {
		
	    private static final String TAG_NAME_KEY = "tag_name";
	    private static final String VERSION_KEY = "version";

	    private void checkUpdate() {
	        if (!Preferences.getBoolean("updateNeverRemind", false)) {
	            executorService.submit(() -> {
	                if (hasUpdate()) {
	                    Platform.runLater(this::showUpdateDialog);
	                }
	            });
	        }
	    }
	    
	    private boolean hasUpdate() {
	        try {
	            // Step 1: Retrieve update URL from resources
	            final URL updateUrl = getUpdateURL();
	            // Step 2: Fetch latest version number from the server
	            final Optional<Integer> latestVersion = fetchLatestVersion(updateUrl);

	            // Step 3: Compare with current version
	            if (latestVersion.isPresent()) {
	                int currentVersion = Integer.parseInt(Resources.getString(VERSION_KEY));
	                return latestVersion.get() > currentVersion;
	            }
	        } catch (Exception e) {
	        }
	        return false;
	    }
	    
	    private URL getUpdateURL() throws Exception {
	        URI uri = new URI(Resources.getString("update_url"));
	        return uri.toURL();
	    }

	    // Fetches the latest version from the server and parses the response
	    private static Optional<Integer> fetchLatestVersion(URL url) {
	        HttpURLConnection httpUrlConnection = null;
	        try {
	            httpUrlConnection = (HttpURLConnection) url.openConnection();
	            httpUrlConnection.setRequestMethod("GET");
	            httpUrlConnection.setConnectTimeout(1000);  // Set timeout for the connection
	            httpUrlConnection.setReadTimeout(1000);

	            if (httpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
	                try (InputStreamReader inputStreamReader = new InputStreamReader(httpUrlConnection.getInputStream());
	                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
	                    String jsonResponse = readResponse(bufferedReader);
	                    return parseVersionFromResponse(jsonResponse);
	                }
	            } else {
	                System.err.println("Failed to fetch update, HTTP code: " + httpUrlConnection.getResponseCode());
	            }
	        } catch (Exception e) {
	        } finally {
	            if (httpUrlConnection != null) {
	                httpUrlConnection.disconnect();
	            }
	        }
	        return Optional.empty();
	    }

	    // Reads the full response from the BufferedReader
	    private static String readResponse(BufferedReader bufferedReader) throws Exception {
	        StringBuilder stringBuilder = new StringBuilder();
	        String line;
	        while ((line = bufferedReader.readLine()) != null) {
	            stringBuilder.append(line).append("\n");
	        }
	        return stringBuilder.toString();
	    }

	    // Parses the version number from the JSON response
	    private static Optional<Integer> parseVersionFromResponse(String jsonResponse) {
	        try {
	            JSONParser jsonParser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonResponse);
	            return Optional.of(Integer.parseInt(jsonObject.get(TAG_NAME_KEY).toString()));
	        } catch (ParseException | NumberFormatException e) {
	            System.err.println("Error parsing version from response: " + e.getMessage());
	        }
	        return Optional.empty();
	    }



	    // Method to show the update dialog
	    private void showUpdateDialog() {
	        final ButtonType update = new ButtonType(Resources.getString("update"), ButtonData.YES);
	        final ButtonType notUpdate = new ButtonType(Resources.getString("update_no"), ButtonData.NO);
	        final ButtonType neverRemind = new ButtonType(Resources.getString("update_never_remind"), ButtonData.OTHER);

	        final Alert alert = createAlert(update, notUpdate, neverRemind);
	        final Optional<ButtonType> result = alert.showAndWait();

	        result.ifPresent(buttonType -> handleDialogResult(buttonType, update, neverRemind));
	    }

	    // Method to create the update alert dialog
	    private Alert createAlert(ButtonType update, ButtonType notUpdate, ButtonType neverRemind) {
	        final Alert alert = new Alert(AlertType.INFORMATION);
	        alert.getDialogPane().getStylesheets().add(css);
	        alert.setTitle(Resources.getString("information"));
	        alert.setHeaderText(Resources.getString("update_header"));
	        alert.setContentText(Resources.getString("update_content"));

	        // Set icon for the alert
	        final Image icon = new Image("file:resources/icon/update.png");
	        alert.setGraphic(new ImageView(icon));
	        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);

	        // Set available button types
	        alert.getButtonTypes().setAll(update, notUpdate, neverRemind);

	        return alert;
	    }

	    // Handle the result of the dialog
	    private void handleDialogResult(ButtonType result, ButtonType update, ButtonType neverRemind) {
	        if (result == update) {
	            openUpdatePage();
	        } else if (result == neverRemind) {
	            Preferences.setValue("updateNeverRemind", true);
	        }
	    }

	    // Opens the update page in the user's default browser
	    private void openUpdatePage() {
	        final String url = new StringBuilder(Resources.getString("url")).append("/releases").toString();
	        main.getHostServices().showDocument(url);
	    }
	}
}
