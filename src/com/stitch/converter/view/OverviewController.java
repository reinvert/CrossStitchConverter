package com.stitch.converter.view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.stitch.converter.GraphicsEngine;
import com.stitch.converter.LogPrinter;
import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;
import com.stitch.converter.UpdateChecker;
import com.stitch.converter.model.StitchImage;
import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.StitchColor;
import com.stitch.converter.model.StitchList;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.Observable;
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
	public MenuItem load, save, saveAs, exportConvertedImage, exportBlueprint;
	@FXML
	public CheckMenuItem showNumberItem, toggleColorTableItem, toggleLogItem;
	@FXML
	public SplitPane verticalSplitPane, horizontalSplitPane;
	@FXML
	public TextField zoom;

	private int x = -1, y = -1;
	
	//private String style;
	
    // Executor for managing threads
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void setStage(final Stage overviewStage) {
        setColorTable(Preferences.getBoolean("showColorTable", true));
        setupStageCloseEvent(overviewStage);

        initializeCsvFile();
        autoLoad();

        updateApplication();
        this.overviewStage = overviewStage;
    }

    private void setupStageCloseEvent(final Stage overviewStage) {
        Platform.runLater(() -> overviewStage.getScene().getWindow().addEventHandler(
                WindowEvent.WINDOW_CLOSE_REQUEST, 
                this::closeWindowEvent));
    }
    
    private HostServices hostServices;
        
    public void setHostServices(final HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void updateApplication() {
        if (Preferences.getBoolean("updateNeverRemind", false)) {
            return;
        }

        executorService.submit(() -> {
            if (UpdateChecker.hasUpdate()) {
                Platform.runLater(() -> UpdateDialog.show(hostServices));
            }
        });
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
						String dir = Preferences.getValue("autoLoadFile", "");
						if(dir.equals("")) {
							return;
						}
						loadDmc(new File(dir));
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
	    setupCanvasScrollPaneResizeHandlers();

	    canvas.requestFocus();
	}

	private void setupHighlightColumn() {
	    highlightColumn.setCellFactory(column -> new CheckBoxTableCell<>());
	    highlightColumn.setCellValueFactory(
	            cellData -> cellData.getValue().highlightProperty()
	    );
	}

	private void setupCompleteColumn() {
	    completeColumn.setCellFactory(column -> new CheckBoxTableCell<>());
	    completeColumn.setCellValueFactory(
	            cellData -> cellData.getValue().completeProperty()
	    );
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
	
	private void setupCanvasScrollPaneResizeHandlers() {
	    canvasScrollPane.widthProperty().addListener(
	            createSizeChangeListener()
	    );

	    canvasScrollPane.heightProperty().addListener(
	            createSizeChangeListener()
	    );
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
	    final double actualScale = canvasController.setScale(scaleValue);

	    if (scale.startsWith("MATCH_")) {
	        zoom.setText(scale);
	    } else {
	        zoom.setText(String.format("%.2f", actualScale));
	    }

	    Preferences.setValue("scale", zoom.getText());
	    canvas.requestFocus();
	    return true;
	}

	private boolean parseAndSetScale(String scale, double defaultRatio) {
	    try {
	        double scaleRatio = Double.parseDouble(scale.replace("X", ""));
	        scaleRatio = Math.max(2d, Math.min(15d, scaleRatio));

	        zoom.setText(String.format("%.2f", canvasController.setScale(scaleRatio)));
	    } catch (final NumberFormatException e) {
	        final double actualScale =
	                canvasController.setScale(defaultRatio);

	        zoom.setText(String.format("%.2f", actualScale));
	        return false;
	    }

	    Preferences.setValue("scale", zoom.getText());
	    canvas.requestFocus();
	    return true;
	}
	
	private final Map<Long, StitchList> stitchListByPixel = new HashMap<>();
	
	private long pixelKey(final int x, final int y) {
	    return ((long) x << 32) | (y & 0xffffffffL);
	}
	
	public void setImage(final StitchImage stitchImage) {
	    canvasController = new CanvasController(stitchImage, canvas);

	    ObservableList<StitchList> stitchListArrayList = FXCollections.observableArrayList(
	        stitchList -> new Observable[]{stitchList.highlightProperty(), stitchList.completeProperty()}
	    );

	    stitchListByPixel.clear();

	    stitchImage.getPixelLists().forEach(pixelList -> {
	        final StitchList stitchList = new StitchList(pixelList);

	        setupStitchListListeners(stitchList);

	        stitchListArrayList.add(stitchList);

	        for (final Pixel pixel : pixelList.getPixelSet()) {
	            stitchListByPixel.put(
	                    pixelKey(pixel.getX(), pixel.getY()),
	                    stitchList
	            );
	        }
	    });

	    stitchListArrayList.addListener((ListChangeListener<StitchList>) c -> {
	        if (c.next() && c.wasUpdated()) {
	            Platform.runLater(this::invalidate);
	        }
	    });

	    colorTable.setItems(stitchListArrayList);
	    setZoom(Preferences.getValue("scale", "MATCH_WIDTH"));

	    enableCanvasInteraction();
	}
	
	private void setupStitchListListeners(final StitchList stitchList) {
		stitchList.highlightProperty().addListener(
		        (observable, oldValue, newValue) -> {
		            stitchList.setHighlight(newValue);
		            canvasController.updateHighlighted(newValue);

		            if (newValue && stitchList.isCompleted()) {
		                stitchList.setCompleted(false);
		            }

		            setTitleChanged(true);
		        }
		);

	    stitchList.completeProperty().addListener(
	            (observable, oldValue, newValue) -> {
	                stitchList.setCompleted(newValue);

	                if (newValue && stitchList.isHighlighted()) {
	                    stitchList.setHighlight(false);
	                }

	                setTitleChanged(true);
	            }
	    );
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
	}
	
	private ChangeListener<Number> createSizeChangeListener() {
	    return (observable, oldValue, newValue) -> {
	        if (canvasController == null) {
	            return;
	        }

	        if (Preferences.getValue("scale", "MATCH_SCREEN").contains("MATCH")) {
	            setZoom(Preferences.getValue("scale", "MATCH_SCREEN"));
	            invalidate();
	        }
	    };
	}

	private void getClickedColor(
	        final double originalX,
	        final double originalY) {

	    final int x = (int) ((originalX - canvasController.getMargin())
	            / canvasController.getScale());

	    final int y = (int) ((originalY - canvasController.getMargin())
	            / canvasController.getScale());

	    final StitchList stitchList =
	            stitchListByPixel.get(pixelKey(x, y));

	    if (stitchList == null) {
	        return;
	    }

	    colorTable.requestFocus();
	    colorTable.getSelectionModel().select(stitchList);
	    colorTable.scrollTo(stitchList);
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
	    if (x == -1 || y == -1) {
	        return;
	    }

	    final StitchList stitchList =
	            stitchListByPixel.get(pixelKey(x, y));

	    if (stitchList == null) {
	        return;
	    }

	    if (this.x == x && this.y == y) {
	        x = -1;
	        y = -1;
	    }
	    
		this.x = x;
		this.y = y;

	    canvasController.setHighlightPixel(this.x, this.y);
	    canvasController.invalidate();
	}

	private void closeWindowEvent(final WindowEvent event) {
	    if (saveInProgress) {
	        event.consume();
	        return;
	    }

	    if (Preferences.getBoolean("autoLoad", false)) {
	        Preferences.setValue(
	                "scrollX",
	                canvasScrollPane.getHvalue()
	        );

	        Preferences.setValue(
	                "scrollY",
	                canvasScrollPane.getVvalue()
	        );
	    }

	    Preferences.store();

	    if (!confirmExit()) {
	        event.consume();
	        return;
	    }

	    executorService.shutdown();
	}

	public void setTitleChanged(final boolean changed) {
		canvasController.getImage().setChanged(changed);
		String title = dmcFile.getName();
		if (changed) {
			title = title + "(*)";
		}
		overviewStage.setTitle(title);
	}
	
	private File dmcFile, csvFile;
	private String name;
	private FileChooser loadFileChooser, saveToFileChooser, blueprintFileChooser, convertedImageFileChooser;
	private FileChooser.ExtensionFilter dmcFilter, pngExtensionFilter;
	private Alert confirmExitAlert;
	private ButtonType saveButton, notSaveButton;
	private Canvas blueprintCanvas;
	private Blueprint blueprint;
	private WritableImage blueprintWritableImage;
	
	@FXML
	public void loadMenu() {
	    if (!confirmExit(this::showLoadDialog)) {
	        return;
	    }

	    showLoadDialog();
	}

	private void showLoadDialog() {
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
	    Preferences.setValue(
                "autoLoadFile",
                dmcFile.getAbsolutePath()
        );
	}

	private void makeNewFile(File file) {
	    resetScrollPreferences();

	    GraphicsEngine.Builder builder = new GraphicsEngine.Builder(csvFile, file);
	    builder.setColorLimit(Preferences.getInteger("maximumColorLimit", 0))
	           .setBackground(
	               Preferences.getColor(
	                   "backgroundColor",
	                   new StitchColor(Color.WHITE, "")
	               )
	           )
	           .setThreadCount(Preferences.getInteger("workingThread", 0))
	           .setScaled(Preferences.getBoolean("resizeImage", true));

	    dmcFile = new File(
	        file.getParent(),
	        extractFileNameWithoutExtension(file) + ".dmc"
	    );

	    overviewStage.setTitle(dmcFile.getName() + "(*)");

	    main.load(builder, GraphicsEngine.Mode.NEW_FILE);

	    name = extractFileNameWithoutExtension(dmcFile);
	}

	private String extractFileNameWithoutExtension(File file) {
	    String fileName = file.getName();
	    int extensionIndex = fileName.lastIndexOf('.');

	    if (extensionIndex <= 0) {
	        return fileName;
	    }

	    return fileName.substring(0, extensionIndex);
	}

	private void resetScrollPreferences() {
	    Preferences.setValue("scrollX", "0");
	    Preferences.setValue("scrollY", "0");
	}

	private String getExtension(File file) {
	    String fileName = file.getName();
	    int extensionIndex = fileName.lastIndexOf('.');

	    if (extensionIndex <= 0) {
	        return "";
	    }

	    return fileName.substring(extensionIndex).toLowerCase(Locale.ROOT);
	}

	private boolean saveInProgress;

	@FXML
	public void saveMenu() {
	    saveDocumentAsync(dmcFile, null);
	}
	
	private void saveDocumentAsync(
	        final File targetFile,
	        final Runnable onSuccess) {

	    if (saveInProgress || save.isDisable()) {
	        return;
	    }

	    final StitchImage image = canvasController.getImage();

	    saveInProgress = true;
	    setDocumentBusy(true);

	    CompletableFuture
	            .supplyAsync(() ->
	                    Resources.writeObject(targetFile, image)
	            )
	            .whenComplete((saved, throwable) ->
	                    Platform.runLater(() -> {
	                        saveInProgress = false;
	                        setDocumentBusy(false);

	                        if (throwable != null ||
	                                !Boolean.TRUE.equals(saved)) {

	                            showSaveFailureAlert(targetFile);
	                            return;
	                        }

	                        // 실제 파일 저장이 성공한 뒤에만
	                        // dirty 상태를 해제한다.
	                        setTitleChanged(false);

	                        Preferences.setValue(
	                                "autoLoadFile",
	                                targetFile.getPath()
	                        );

	                        if (onSuccess != null) {
	                            onSuccess.run();
	                        }
	                    })
	            );
	}
	
	private void setDocumentBusy(final boolean busy) {
	    load.setDisable(busy);
	    save.setDisable(busy);
	    saveAs.setDisable(busy);
	    exportConvertedImage.setDisable(busy);
	    exportBlueprint.setDisable(busy);

	    canvas.setDisable(busy);
	    colorTable.setDisable(busy);
	    zoom.setDisable(busy);
	}
	
	private void showSaveFailureAlert(final File targetFile) {
	    final Alert alert = new Alert(AlertType.ERROR);

	    alert.getDialogPane()
	            .getStylesheets()
	            .add(Resources.getCSS());

	    alert.setTitle(Resources.getString("warning"));
	    alert.setHeaderText(
	            Resources.getString("error")
	    );
	    alert.setContentText(
	    		Resources.getString(
	                    "save_failed",
	                    targetFile.getAbsolutePath()
	            )
	    );

	    alert.showAndWait();
	}

	@FXML
	public void saveAsMenu() {
	    if (!saveAs.isDisable()) {
	        initializeSaveToFileChooser();

	        final File saveFile =
	                saveToFileChooser.showSaveDialog(overviewStage);

	        if (saveFile != null) {
	            saveDocumentAsync(saveFile, () -> {
	                dmcFile = saveFile;
	                name = extractFileNameWithoutExtension(saveFile);
	                overviewStage.setTitle(dmcFile.getName());
	            });
	        }
	    }
	}

	private void initializeSaveToFileChooser() {
	    if (saveToFileChooser == null) {
	        saveToFileChooser = new FileChooser();
	        saveToFileChooser.getExtensionFilters().add(dmcFilter);
	    }

	    saveToFileChooser.setInitialDirectory(
	            new File(dmcFile.getParent())
	    );
	}

	private boolean confirmExit() {
	    return confirmExit(this::closeOverview);
	}

	private boolean confirmExit(final Runnable onSaveSuccess) {
	    if (saveInProgress) {
	        return false;
	    }

	    if (canvasController != null
	            && canvasController.getImage().isChanged()) {

	        initializeConfirmExitAlert();

	        final Optional<ButtonType> result =
	                confirmExitAlert.showAndWait();

	        if (result.isPresent()) {
	            if (result.get() == saveButton) {
	                saveDocumentAsync(dmcFile, onSaveSuccess);
	                return false;
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
	        confirmExitAlert.getDialogPane().getStylesheets().add(Resources.getCSS());
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
	    initializePngExtensionFilter();

	    if (blueprintFileChooser == null) {
	        blueprintFileChooser = new FileChooser();
	        blueprintFileChooser.getExtensionFilters().add(pngExtensionFilter);
	    }

	    blueprintFileChooser.setInitialDirectory(
	            new File(dmcFile.getParent())
	    );

	    blueprintFileChooser.setInitialFileName(
	            name + "_blueprint.png"
	    );

	    // Always recreate the blueprint objects so they reference
	    // the currently loaded StitchImage.
	    blueprintCanvas = new Canvas(
	            canvasController.getCanvas().getWidth(),
	            canvasController.getCanvas().getHeight()
	    );
	    
	    blueprint = new Blueprint(
	            canvasController.getImage(),
	            blueprintCanvas
	    );

	    blueprint.setScale(
	            Preferences.getDouble("blueprintScale", 20d)
	    );

	    blueprint.setListScale(
	            Preferences.getDouble("blueprintListScale", 20d)
	    );

	    blueprintWritableImage = new WritableImage(
	            (int) blueprintCanvas.getWidth(),
	            (int) blueprintCanvas.getHeight()
	    );
	}

	private void exportBlueprintToFile(File blueprintFile) {
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
	    initializePngExtensionFilter();

	    if (convertedImageFileChooser == null) {
	        convertedImageFileChooser = new FileChooser();
	        convertedImageFileChooser.getExtensionFilters().add(
	                pngExtensionFilter
	        );
	    }

	    convertedImageFileChooser.setInitialDirectory(
	            new File(dmcFile.getParent())
	    );

	    convertedImageFileChooser.setInitialFileName(
	            name + ".png"
	    );
	}
	
	private void initializePngExtensionFilter() {
	    if (pngExtensionFilter == null) {
	        pngExtensionFilter = new FileChooser.ExtensionFilter(
	                Resources.getString("png_file"),
	                "*.png"
	        );
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
	        closeOverview();
	    }
	}

	public void close() {
	    if (confirmExit()) {
	        closeOverview();
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
				page.setStyle(Resources.getStyle());
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
				page.setStyle(Resources.getStyle());
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
	
	private void closeOverview() {
	    executorService.shutdown();
	    overviewStage.close();
	}
}
