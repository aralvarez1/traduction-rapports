import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XliffTranslatorApp extends Application {

    private static final String JAVA_8     = "sapmachine-11.0.25/bin/java.exe";
    private static final String TMT_JAR    = "lib/TmtBridge.jar";
    private static final File   OUTPUT_DIR = new File("xliff_output");

    private static final Preferences PREFS = Preferences.userNodeForPackage(XliffTranslatorApp.class);
    private static final String P_CMS    = "cms";
    private static final String P_USER   = "user";
    private static final String P_CUID   = "cuid";
    private static final String P_SOURCE = "source";
    private static final String P_BASE   = "base";
    private static final String P_STRICT = "strict";
    private static final String P_FORCE  = "force";
    private static final String P_THEME  = "theme";

    private String cms, user, pass;
    private Map<String, Map<String, String>> translationBase = new HashMap<>();
    private File    baseFile;
    private boolean darkMode;

    // =========================================================
    // CODES TECHNIQUES EN DUR
    // =========================================================

    private static final Map<String, Map<String, String>> CODE_MAPPING = new HashMap<>();
    static {
        Map<String, String> htlp = new HashMap<>();
        htlp.put("en", "ex TFD");
        htlp.put("es", "sin ITM");
        htlp.put("de", "OALM");
        htlp.put("fr", "HTLP");
        CODE_MAPPING.put("HTLP", htlp);

        Map<String, String> ca = new HashMap<>();
        ca.put("en", "Sales");
        ca.put("es", "Ventas");
        ca.put("de", "Umsatz");
        ca.put("fr", "CA");
        CODE_MAPPING.put("CA", ca);
    }

    // =========================================================
    // THEME — palettes dark / light
    // =========================================================

    private static final String D_BG     = "#1a1a2e";
    private static final String D_ACCENT = "#0f3460";
    private static final String D_BTN    = "#e94560";
    private static final String D_TEXT   = "#eaeaea";
    private static final String D_DIM    = "#8a8a9a";
    private static final String D_FIELD  = "#0f1a30";
    private static final String D_BORDER = "#2a2a4a";
    private static final String D_BAR    = "#111122";

    private static final String L_BG     = "#f0f4ff";
    private static final String L_ACCENT = "#d0d8f0";
    private static final String L_BTN    = "#e94560";
    private static final String L_TEXT   = "#1a1a2e";
    private static final String L_DIM    = "#5a5a7a";
    private static final String L_FIELD  = "#f7f9ff";
    private static final String L_BORDER = "#c8d0e8";
    private static final String L_BAR    = "#e0e6f8";

    private String bg()     { return darkMode ? D_BG     : L_BG;     }
    private String accent() { return darkMode ? D_ACCENT : L_ACCENT; }
    private String btn()    { return darkMode ? D_BTN    : L_BTN;    }
    private String text()   { return darkMode ? D_TEXT   : L_TEXT;   }
    private String dim()    { return darkMode ? D_DIM    : L_DIM;    }
    private String field()  { return darkMode ? D_FIELD  : L_FIELD;  }
    private String border() { return darkMode ? D_BORDER : L_BORDER; }
    private String bar()    { return darkMode ? D_BAR    : L_BAR;    }

    // =========================================================
    // CSS — styles inline
    // =========================================================

    private String fieldStyle() {
        return "-fx-background-color:" + field() + ";"
                + "-fx-background-radius:8;-fx-border-color:" + border() + ";"
                + "-fx-border-radius:8;-fx-border-width:1.5;-fx-text-fill:" + text() + ";"
                + "-fx-prompt-text-fill:" + dim() + ";-fx-padding:8 12 8 12;"
                + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    }
    private String labelFieldStyle() {
        return "-fx-background-color:" + field() + ";"
                + "-fx-background-radius:8;-fx-border-color:" + border() + ";"
                + "-fx-border-radius:8;-fx-border-width:1.5;-fx-text-fill:" + text() + ";"
                + "-fx-padding:8 36 8 12;";
    }
    private String labelSmallStyle() { return "-fx-text-fill:" + dim() + ";-fx-font-size:12px;"; }
    private String sectionStyle()    { return "-fx-text-fill:" + btn() + ";-fx-font-size:13px;-fx-font-weight:bold;"; }
    private String infoStyle()       { return "-fx-text-fill:" + dim() + ";-fx-font-size:12px;"; }
    private String cbStyle()         { return "-fx-text-fill:" + text() + ";-fx-font-size:12px;-fx-focus-color:transparent;-fx-faint-focus-color:transparent;"; }
    private String helpBadgeStyle()  {
        return "-fx-background-color:" + btn() + ";-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;"
                + "-fx-background-radius:10;-fx-padding:1 5 2 5;-fx-cursor:hand;";
    }
    private String btnPrimaryStyle() {
        return "-fx-background-color:" + btn() + ";-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-background-radius:8;-fx-padding:9 22 9 22;-fx-cursor:hand;"
                + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    }
    private String btnSecondaryStyle() {
        return "-fx-background-color:" + accent() + ";-fx-text-fill:" + text() + ";"
                + "-fx-background-radius:8;-fx-padding:8 14 8 14;-fx-cursor:hand;"
                + "-fx-border-color:" + border() + ";-fx-border-radius:8;-fx-border-width:1;"
                + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    }
    private String btnIconStyle() {
        return "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:4 8 4 8;"
                + "-fx-text-fill:" + text() + ";-fx-font-size:15px;"
                + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    }
    private String topBarStyle() {
        return "-fx-background-color:" + bar() + ";"
                + "-fx-border-color:" + border() + ";-fx-border-width:0 0 1 0;";
    }

    /**
     * Génère une feuille CSS complète pour le TabPane.
     * -fx-focus-color et -fx-faint-focus-color neutralisent le halo bleu
     * que JavaFX applique automatiquement quand on clique un onglet.
     */
    private String buildTabPaneCss() {
        return
                ".tab-pane {"
                        + "  -fx-focus-color: transparent;"
                        + "  -fx-faint-focus-color: transparent;"
                        + "  -fx-background-color: " + bg() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area {"
                        + "  -fx-background-color: " + bar() + ";"
                        + "  -fx-padding: 4 8 0 8;"
                        + "}"
                        + ".tab-pane > .tab-header-area > .tab-header-background {"
                        + "  -fx-background-color: " + bar() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab {"
                        + "  -fx-background-color: " + accent() + ";"
                        + "  -fx-background-radius: 6 6 0 0;"
                        + "  -fx-background-insets: 0;"
                        + "  -fx-padding: 4 14 4 14;"
                        + "  -fx-focus-color: transparent;"
                        + "  -fx-faint-focus-color: transparent;"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:selected {"
                        + "  -fx-background-color: " + bg() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab .tab-label {"
                        + "  -fx-text-fill: " + dim() + ";"
                        + "  -fx-font-size: 12px;"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:selected .tab-label {"
                        + "  -fx-text-fill: " + text() + ";"
                        + "  -fx-font-weight: bold;"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:hover .tab-label {"
                        + "  -fx-text-fill: " + text() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:focused {"
                        + "  -fx-background-color: " + accent() + ";"
                        + "  -fx-focus-color: transparent;"
                        + "  -fx-faint-focus-color: transparent;"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:focused:selected {"
                        + "  -fx-background-color: " + bg() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:focused .tab-label {"
                        + "  -fx-text-fill: " + dim() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area > .headers-region > .tab:focused:selected .tab-label {"
                        + "  -fx-text-fill: " + text() + ";"
                        + "  -fx-font-weight: bold;"
                        + "}"
                        + ".tab-pane > .tab-header-area > .tab-down-button {"
                        + "  -fx-background-color: " + bar() + ";"
                        + "}"
                        + ".tab-pane > .tab-header-area > .control-buttons-tab {"
                        + "  -fx-background-color: " + bar() + ";"
                        + "}";
    }

    /** Applique le CSS du TabPane via un fichier temporaire. */
    private void applyTabPaneCss(TabPane tabPane) {
        tabPane.getStylesheets().clear();
        try {
            java.nio.file.Path tmp = Files.createTempFile("xliff-tabs-", ".css");
            Files.writeString(tmp, buildTabPaneCss());
            tmp.toFile().deleteOnExit();
            tabPane.getStylesheets().add(tmp.toUri().toString());
        } catch (Exception ignored) {}
        tabPane.setStyle("-fx-background-color:" + bg() + ";");
    }

    // =========================================================
    // OEIL VECTORIEL
    // =========================================================

    private StackPane createEyeIcon() {
        String c = darkMode ? D_DIM : L_DIM;
        Path eye = new Path();
        eye.getElements().add(new MoveTo(0, 0));
        eye.getElements().add(new QuadCurveTo(7, -6, 14, 0));
        eye.getElements().add(new QuadCurveTo(7, 6, 0, 0));
        eye.setFill(Color.TRANSPARENT);
        eye.setStroke(Color.web(c));
        eye.setStrokeWidth(1.5);
        Circle pupil = new Circle(7, 0, 2.8);
        pupil.setFill(Color.web(c));
        StackPane icon = new StackPane(eye, pupil);
        icon.setPrefSize(22, 16);
        return icon;
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private Object[] makeTopBar(String title, Button btnTheme) {
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + text() + ";");
        btnTheme.setFocusTraversable(false);
        btnTheme.setStyle(btnIconStyle());
        BorderPane bar = new BorderPane();
        bar.setLeft(lblTitle);
        bar.setRight(btnTheme);
        bar.setPadding(new Insets(10, 16, 10, 16));
        BorderPane.setAlignment(lblTitle, Pos.CENTER_LEFT);
        BorderPane.setAlignment(btnTheme, Pos.CENTER_RIGHT);
        bar.setStyle(topBarStyle());
        return new Object[]{bar, lblTitle};
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        darkMode = PREFS.getBoolean(P_THEME, true);
        showLogin(stage);
    }

    // =========================================================
    // ECRAN 1 : CONNEXION
    // =========================================================

    private void showLogin(Stage stage) {
        Button btnTheme = new Button(darkMode ? "☀" : "☾");
        Object[] tbr       = makeTopBar("Connexion SAP BO", btnTheme);
        BorderPane topBar  = (BorderPane) tbr[0];
        Label lblBarTitle  = (Label)      tbr[1];

        VBox content = new VBox(14);
        content.setPadding(new Insets(24, 28, 24, 28));

        Label     lblCms = new Label("CMS");
        TextField tfCms  = new TextField(PREFS.get(P_CMS, ""));
        tfCms.setPromptText("ex: BOserver:6400");
        content.getChildren().addAll(lblCms, tfCms);

        Label     lblUser = new Label("Utilisateur");
        TextField tfUser  = new TextField(PREFS.get(P_USER, ""));
        content.getChildren().addAll(lblUser, tfUser);

        Label         lblPwd    = new Label("Mot de passe");
        PasswordField tfPass    = new PasswordField();
        Label         lblPassVis = new Label();
        lblPassVis.setVisible(false);
        lblPassVis.setFocusTraversable(false);
        lblPassVis.setMaxWidth(Double.MAX_VALUE);
        lblPassVis.setMaxHeight(Double.MAX_VALUE);
        tfPass.textProperty().addListener((obs, o, n) -> lblPassVis.setText(n));

        Button btnReveal = new Button();
        btnReveal.setGraphic(createEyeIcon());
        btnReveal.setFocusTraversable(false);
        btnReveal.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:0 6 0 6;"
                + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");

        StackPane passStack = new StackPane(tfPass, lblPassVis, btnReveal);
        passStack.setAlignment(Pos.CENTER_RIGHT);
        StackPane.setAlignment(btnReveal, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnReveal, new Insets(0, 4, 0, 0));
        tfPass.setPadding(new Insets(8, 36, 8, 12));
        content.getChildren().addAll(lblPwd, passStack);

        Label  lblError   = new Label();
        lblError.setWrapText(true);
        Button btnConnect = new Button("Se connecter");
        btnConnect.setMaxWidth(Double.MAX_VALUE);
        btnConnect.setFocusTraversable(false);
        VBox.setMargin(btnConnect, new Insets(8, 0, 0, 0));
        content.getChildren().addAll(btnConnect, lblError);

        VBox root = new VBox(topBar, content);
        root.setFocusTraversable(true);

        btnReveal.setOnMousePressed(e -> {
            root.requestFocus();
            tfPass.setVisible(false);
            lblPassVis.setVisible(true);
        });
        btnReveal.setOnMouseReleased(e -> {
            lblPassVis.setVisible(false);
            tfPass.setVisible(true);
        });

        Runnable applyStyles = () -> {
            root.setStyle("-fx-background-color:" + bg() + ";");
            content.setStyle("-fx-background-color:" + bg() + ";");
            topBar.setStyle(topBarStyle());
            lblBarTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + text() + ";");
            btnTheme.setStyle(btnIconStyle());
            btnTheme.setText(darkMode ? "☀" : "☾");
            for (Label l : new Label[]{lblCms, lblUser, lblPwd}) l.setStyle(labelSmallStyle());
            tfCms.setStyle(fieldStyle());
            tfUser.setStyle(fieldStyle());
            tfPass.setStyle(fieldStyle() + "-fx-padding:8 36 8 12;");
            lblPassVis.setStyle(labelFieldStyle());
            lblError.setStyle("-fx-text-fill:" + btn() + ";-fx-font-size:11px;");
            btnConnect.setStyle(btnPrimaryStyle());
            btnReveal.setGraphic(createEyeIcon());
        };
        applyStyles.run();

        btnTheme.setOnAction(e -> {
            darkMode = !darkMode;
            PREFS.putBoolean(P_THEME, darkMode);
            applyStyles.run();
        });

        Scene scene = new Scene(root, 460, 390);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) btnConnect.fire();
        });

        btnConnect.setOnAction(e -> {
            String c = tfCms.getText().trim(), u = tfUser.getText().trim(), p = tfPass.getText();
            if (c.isEmpty() || u.isEmpty() || p.isEmpty()) {
                lblError.setText("Tous les champs sont obligatoires");
                return;
            }
            btnConnect.setDisable(true);
            btnConnect.setText("Connexion...");
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ProcessBuilder pb = new ProcessBuilder(JAVA_8, "-jar", TMT_JAR,
                            "--action", "test", "--cms", c, "--user", u, "--pass", p);
                    pb.redirectErrorStream(true);
                    Process proc = pb.start();
                    String out = new String(proc.getInputStream().readAllBytes());
                    if (proc.waitFor() != 0) throw new RuntimeException(out.trim().split("\n")[0]);
                    return null;
                }
            };
            task.setOnSucceeded(ev -> {
                cms = c; user = u; pass = p;
                PREFS.put(P_CMS, c); PREFS.put(P_USER, u);
                showMain(stage);
            });
            task.setOnFailed(ev -> {
                btnConnect.setDisable(false);
                btnConnect.setText("Se connecter");
                lblError.setText("Erreur : " + task.getException().getMessage());
            });
            new Thread(task).start();
        });

        stage.setTitle("Connexion");
        stage.setScene(scene);
        stage.show();
        Platform.runLater(root::requestFocus);
    }

    // =========================================================
    // ECRAN 2 : PRINCIPAL (2 onglets)
    // =========================================================

    private void showMain(Stage stage) {
        Button btnTheme = new Button(darkMode ? "☀" : "☾");
        Object[] tbr      = makeTopBar("Traduction XLIFF", btnTheme);
        BorderPane topBar = (BorderPane) tbr[0];
        Label lblBarTitle = (Label)      tbr[1];

        // --------------------------------------------------
        // ONGLET 1 : TRADUCTION
        // --------------------------------------------------

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 20, 24));

        Label lblBo = new Label("Rapport BI");
        content.getChildren().add(lblBo);

        Label     lblCuidL    = new Label("CUID");
        Label     lblCuidHelp = new Label("?");
        Tooltip   cuidTip     = new Tooltip("Clic droit sur le rapport → Propriétés → CUID");
        cuidTip.setShowDelay(Duration.millis(150));
        Tooltip.install(lblCuidHelp, cuidTip);
        HBox cuidLabelRow = new HBox(6, lblCuidL, lblCuidHelp);
        cuidLabelRow.setAlignment(Pos.CENTER_LEFT);
        TextField tfCuid = new TextField(PREFS.get(P_CUID, ""));
        tfCuid.setPromptText("CUID du rapport");
        VBox cuidCol = new VBox(4, cuidLabelRow, tfCuid);
        HBox.setHgrow(cuidCol, Priority.ALWAYS);

        Label     lblSrcL = new Label("Langue source");
        TextField tfSrc   = new TextField(PREFS.get(P_SOURCE, "en_GB"));
        tfSrc.setPromptText("ex: en_GB");
        tfSrc.setPrefWidth(110); tfSrc.setMaxWidth(110);
        VBox srcCol = new VBox(4, lblSrcL, tfSrc);

        HBox cuidRow = new HBox(16, cuidCol, srcCol);
        cuidRow.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(cuidRow, new Insets(0, 0, 4, 0));
        content.getChildren().add(cuidRow);

        Label lblTrad = new Label("Traduction");
        content.getChildren().add(lblTrad);

        Button btnBase = new Button("Charger base");
        btnBase.setFocusTraversable(false);
        Label lblBase = new Label(shortenPath(PREFS.get(P_BASE, "(aucune base)")));
        lblBase.setMaxWidth(Double.MAX_VALUE);
        lblBase.setMinWidth(0);
        lblBase.setEllipsisString("…");
        if (!PREFS.get(P_BASE, "").isEmpty()) {
            lblBase.setTooltip(new Tooltip(PREFS.get(P_BASE, "")));
            btnBase.setText("Base chargée");
        }
        HBox baseBox = new HBox(10, btnBase, lblBase);
        baseBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblBase, Priority.ALWAYS);
        content.getChildren().add(baseBox);

        CheckBox cbStrict = new CheckBox("Translated seulement si issu de la base");
        CheckBox cbForce  = new CheckBox("Tout passer à Translated (sans traduire)");
        cbStrict.setSelected(PREFS.getBoolean(P_STRICT, false));
        cbForce.setSelected(PREFS.getBoolean(P_FORCE, false));
        cbStrict.setFocusTraversable(false);
        cbForce.setFocusTraversable(false);
        cbStrict.setOnAction(e -> PREFS.putBoolean(P_STRICT, cbStrict.isSelected()));
        cbForce.setOnAction(e  -> PREFS.putBoolean(P_FORCE,  cbForce.isSelected()));
        content.getChildren().addAll(cbStrict, cbForce);

        ProgressBar progressBar   = new ProgressBar(0);
        Label       progressLabel = new Label("");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setFocusTraversable(false);
        progressBar.setVisible(false);   progressBar.setManaged(false);
        progressLabel.setVisible(false); progressLabel.setManaged(false);
        content.getChildren().addAll(progressBar, progressLabel);

        Button btnTranslate = new Button("Traduire");
        btnTranslate.setFocusTraversable(false);
        Label lblStatus = new Label("");
        HBox translateBox = new HBox(12, btnTranslate, lblStatus);
        translateBox.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(translateBox);

        // --------------------------------------------------
        // ONGLET 2 : ALIMENTATION BASE
        // --------------------------------------------------

        VBox content2 = new VBox(12);
        content2.setPadding(new Insets(20, 24, 20, 24));

        Label lbl2Bo = new Label("Rapport BI source");
        content2.getChildren().add(lbl2Bo);

        Label     lbl2CuidL = new Label("CUID");
        TextField tf2Cuid   = new TextField(PREFS.get(P_CUID, ""));
        tf2Cuid.setPromptText("CUID du rapport");
        VBox col2Cuid = new VBox(4, lbl2CuidL, tf2Cuid);
        HBox.setHgrow(col2Cuid, Priority.ALWAYS);

        Label     lbl2SrcL = new Label("Langue source");
        TextField tf2Src   = new TextField(PREFS.get(P_SOURCE, "en_GB"));
        tf2Src.setPromptText("ex: en_GB");
        tf2Src.setPrefWidth(110); tf2Src.setMaxWidth(110);
        VBox col2Src = new VBox(4, lbl2SrcL, tf2Src);

        HBox row2Cuid = new HBox(16, col2Cuid, col2Src);
        row2Cuid.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(row2Cuid, new Insets(0, 0, 4, 0));
        content2.getChildren().add(row2Cuid);

        Label lbl2Section = new Label("Alimentation de la base");
        content2.getChildren().add(lbl2Section);

        Label lbl2BaseInfo = new Label("Base : " + shortenPath(PREFS.get(P_BASE, "(aucune base chargée)")));
        content2.getChildren().add(lbl2BaseInfo);

        Button btn2Import = new Button("Extraire et ajouter à la base");
        btn2Import.setFocusTraversable(false);
        content2.getChildren().add(btn2Import);

        ProgressBar pb2     = new ProgressBar(0);
        Label       lbl2Msg = new Label("");
        pb2.setMaxWidth(Double.MAX_VALUE);
        pb2.setFocusTraversable(false);
        pb2.setVisible(false);    pb2.setManaged(false);
        lbl2Msg.setVisible(false); lbl2Msg.setManaged(false);
        content2.getChildren().addAll(pb2, lbl2Msg);

        Label lbl2Result = new Label("");
        lbl2Result.setWrapText(true);
        content2.getChildren().add(lbl2Result);

        Button btn2Save = new Button("Enregistrer la base");
        btn2Save.setDisable(true);
        btn2Save.setFocusTraversable(false);
        content2.getChildren().add(btn2Save);

        // --------------------------------------------------
        // TABPANE
        // --------------------------------------------------

        Tab tab1 = new Tab("Traduction",        content);  tab1.setClosable(false);
        Tab tab2 = new Tab("Alimentation base", content2); tab2.setClosable(false);
        TabPane tabPane = new TabPane(tab1, tab2);
        tabPane.setFocusTraversable(false);
        tabPane.setTabMinHeight(30);

        BorderPane root = new BorderPane();
        root.setFocusTraversable(true);
        root.setTop(topBar);
        root.setCenter(tabPane);

        // --------------------------------------------------
        // STYLES — tout centralisé ici
        // --------------------------------------------------

        Runnable applyStyles = () -> {
            root.setStyle("-fx-background-color:" + bg() + ";");
            content.setStyle("-fx-background-color:" + bg() + ";");
            content2.setStyle("-fx-background-color:" + bg() + ";");
            topBar.setStyle(topBarStyle());
            lblBarTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + text() + ";");
            btnTheme.setStyle(btnIconStyle());
            btnTheme.setText(darkMode ? "☀" : "☾");

            // TabPane via feuille CSS (seule façon de toucher les sous-composants)
            applyTabPaneCss(tabPane);

            // Onglet 1
            lblBo.setStyle(sectionStyle());
            lblTrad.setStyle(sectionStyle());
            lblCuidL.setStyle(labelSmallStyle());
            lblSrcL.setStyle(labelSmallStyle());
            lblCuidHelp.setStyle(helpBadgeStyle());
            tfCuid.setStyle(fieldStyle());
            tfSrc.setStyle(fieldStyle());
            btnBase.setStyle(btnSecondaryStyle());
            lblBase.setStyle(infoStyle());
            btnTranslate.setStyle(btnPrimaryStyle());
            cbStrict.setStyle(cbStyle());
            cbForce.setStyle(cbStyle());
            progressBar.setStyle("-fx-accent:" + btn() + ";");
            progressLabel.setStyle("-fx-text-fill:" + dim() + ";-fx-font-size:11px;");
            lblStatus.setStyle("-fx-text-fill:#2ecc71;-fx-font-size:12px;");

            // Onglet 2
            lbl2Bo.setStyle(sectionStyle());
            lbl2CuidL.setStyle(labelSmallStyle());
            lbl2SrcL.setStyle(labelSmallStyle());
            tf2Cuid.setStyle(fieldStyle());
            tf2Src.setStyle(fieldStyle());
            lbl2Section.setStyle(sectionStyle());
            lbl2BaseInfo.setStyle(infoStyle());
            btn2Import.setStyle(btnPrimaryStyle());
            btn2Save.setStyle(btnSecondaryStyle());
            pb2.setStyle("-fx-accent:" + btn() + ";");
            lbl2Msg.setStyle("-fx-text-fill:" + dim() + ";-fx-font-size:11px;");
            lbl2Result.setStyle("-fx-text-fill:#2ecc71;-fx-font-size:12px;");
        };
        applyStyles.run();

        btnTheme.setOnAction(e -> {
            darkMode = !darkMode;
            PREFS.putBoolean(P_THEME, darkMode);
            applyStyles.run();
        });

        // --------------------------------------------------
        // CHARGEMENT BASE au démarrage
        // --------------------------------------------------

        String savedBase = PREFS.get(P_BASE, "");
        if (!savedBase.isEmpty()) {
            File f = new File(savedBase);
            if (f.exists()) try {
                translationBase = loadTsvBase(f);
                baseFile = f;
                lbl2BaseInfo.setText("Base : " + shortenPath(f.getAbsolutePath()));
            } catch (Exception ignored) {}
        }

        btnBase.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Charger la base de traduction");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV/TXT", "*.tsv", "*.txt"));
            File f = fc.showOpenDialog(stage);
            if (f == null) return;
            try {
                translationBase = loadTsvBase(f);
                baseFile = f;
                lblBase.setText(shortenPath(f.getAbsolutePath()));
                lblBase.setTooltip(new Tooltip(f.getAbsolutePath()));
                PREFS.put(P_BASE, f.getAbsolutePath());
                lblBase.setStyle(infoStyle());
                btnBase.setText("Base chargée");
                btnBase.setStyle(btnSecondaryStyle());
                lbl2BaseInfo.setText("Base : " + shortenPath(f.getAbsolutePath()));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Impossible de charger la base : " + ex.getMessage()).showAndWait();
            }
        });

        // --------------------------------------------------
        // TRADUCTION
        // --------------------------------------------------

        btnTranslate.setOnAction(e -> {
            String cuid   = tfCuid.getText().trim();
            String source = tfSrc.getText().trim();
            if (cuid.isEmpty() || source.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "CUID et Langue source sont obligatoires").showAndWait();
                return;
            }
            PREFS.put(P_CUID, cuid); PREFS.put(P_SOURCE, source);
            btnTranslate.setDisable(true);
            lblStatus.setText("");
            progressBar.setVisible(true);   progressBar.setManaged(true);
            progressLabel.setVisible(true); progressLabel.setManaged(true);
            progressBar.progressProperty().unbind(); progressBar.setProgress(-1);
            progressLabel.textProperty().unbind();

            File exportDir     = new File(OUTPUT_DIR, cuid);
            File translatedDir = new File(exportDir, "translated");

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    updateMessage("Export depuis BO...");
                    exportDir.mkdirs();
                    runProcess(JAVA_8, "-jar", TMT_JAR,
                            "--action", "export",
                            "--cms", cms, "--user", user, "--pass", pass,
                            "--cuid", cuid, "--source", source,
                            "--file", exportDir.getAbsolutePath());

                    File[] xliffFiles = exportDir.listFiles(
                            (d, n) -> n.toLowerCase().endsWith(".xliff"));
                    if (xliffFiles == null || xliffFiles.length == 0)
                        throw new RuntimeException("Aucun fichier XLIFF export\u00e9 dans : " + exportDir);

                    translatedDir.mkdirs();
                    List<XliffWork> works = new ArrayList<>();
                    for (File xlf : xliffFiles) {
                        XliffWork w = new XliffWork();
                        w.doc        = parseXmlDoc(xlf);
                        w.items      = extractItems(w.doc);
                        w.outputFile = new File(translatedDir, xlf.getName());
                        works.add(w);
                    }

                    updateMessage("Traduction en cours...");
                    int total = works.stream().mapToInt(w -> w.items.size()).sum();
                    int done  = 0;

                    for (XliffWork work : works) {
                        for (TranslationItem item : work.items) {
                            if (isCancelled()) return null;

                            if ("translated".equals(item.state)
                                    && item.existingTarget != null
                                    && !item.existingTarget.trim().isEmpty()) {
                                item.translatedText = item.existingTarget;
                                item.fromBase = true;
                                updateProgress(++done, total);
                                updateMessage(done + "/" + total + " (skip)");
                                continue;
                            }

                            if (cbForce.isSelected()) {
                                item.translatedText = item.existingTarget != null ? item.existingTarget : "";
                                item.fromBase = true;
                            } else {
                                String src = item.sourceText != null ? item.sourceText.trim() : "";
                                if (src.isEmpty()) {
                                    item.translatedText = item.existingTarget != null ? item.existingTarget : "";
                                } else {
                                    String fromBase = lookupBase(translationBase, item.sourceText, item.targetLang);
                                    if (fromBase != null) {
                                        item.translatedText = fromBase;
                                        item.fromBase = true;
                                    } else {
                                        Thread.sleep(50);
                                        item.translatedText = translateWithCodes(
                                                item.sourceText, item.sourceLang, item.targetLang);
                                    }
                                }
                            }
                            updateProgress(++done, total);
                            updateMessage(done + "/" + total);
                        }
                    }

                    for (XliffWork work : works)
                        updateXliff(work.doc, work.items, work.outputFile, cbStrict.isSelected());

                    updateMessage("Import dans BO...");
                    updateProgress(-1, 1);
                    runProcess(JAVA_8, "-jar", TMT_JAR,
                            "--action", "import",
                            "--cms", cms, "--user", user, "--pass", pass,
                            "--cuid", cuid,
                            "--file", translatedDir.getAbsolutePath());
                    // Nettoyage des .glf générés par BO à la racine
                    File root = new File(".");
                    File[] glfFiles = root.listFiles((d, n) -> n.toLowerCase().endsWith(".glf"));
                    if (glfFiles != null) for (File g : glfFiles) g.delete();
                    return null;
                }
            };

            task.progressProperty().addListener((obs, o, n) -> {
                double v = n.doubleValue();
                if (v >= 0) progressBar.progressProperty().bind(task.progressProperty());
                else { progressBar.progressProperty().unbind(); progressBar.setProgress(-1); }
            });
            progressLabel.textProperty().bind(task.messageProperty());

            task.setOnSucceeded(ev -> {
                btnTranslate.setDisable(false);
                btnTranslate.setStyle(btnPrimaryStyle());
                progressBar.progressProperty().unbind(); progressBar.setProgress(1);
                progressLabel.textProperty().unbind();
                progressLabel.setText("Terminé");
            });
            task.setOnFailed(ev -> {
                btnTranslate.setDisable(false);
                btnTranslate.setStyle(btnPrimaryStyle());
                progressBar.progressProperty().unbind(); progressBar.setProgress(0);
                new Alert(Alert.AlertType.ERROR, "Erreur : " + task.getException().getMessage()).showAndWait();
            });
            new Thread(task).start();
        });

        // --------------------------------------------------
        // ALIMENTATION BASE
        // --------------------------------------------------

        btn2Import.setOnAction(e -> {
            String cuid2 = tf2Cuid.getText().trim();
            String src2  = tf2Src.getText().trim();
            if (cuid2.isEmpty() || src2.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "CUID et Langue source sont obligatoires").showAndWait();
                return;
            }
            if (baseFile == null) {
                new Alert(Alert.AlertType.ERROR,
                        "Aucune base chargée. Chargez-en une dans l’onglet Traduction.").showAndWait();
                return;
            }
            btn2Import.setDisable(true);
            lbl2Result.setText("");
            pb2.setVisible(true);    pb2.setManaged(true);   pb2.setProgress(-1);
            lbl2Msg.setVisible(true); lbl2Msg.setManaged(true);

            Task<int[]> task2 = new Task<>() {
                @Override protected int[] call() throws Exception {
                    updateMessage("Export depuis BO...");
                    File dir2 = new File(OUTPUT_DIR, cuid2);
                    dir2.mkdirs();
                    runProcess(JAVA_8, "-jar", TMT_JAR,
                            "--action", "export",
                            "--cms", cms, "--user", user, "--pass", pass,
                            "--cuid", cuid2, "--source", src2,
                            "--file", dir2.getAbsolutePath());
                    // Nettoyage des .glf générés par BO à la racine
                    File root = new File(".");
                    File[] glfFiles = root.listFiles((d, n) -> n.toLowerCase().endsWith(".glf"));
                    if (glfFiles != null) for (File g : glfFiles) g.delete();

                    File[] xliffFiles = dir2.listFiles(
                            (d, n) -> n.toLowerCase().endsWith(".xliff"));
                    if (xliffFiles == null || xliffFiles.length == 0)
                        throw new RuntimeException("Aucun fichier XLIFF exporté.");

                    updateMessage("Extraction des traductions...");
                    int added = 0, updated = 0;
                    for (File xlf : xliffFiles) {
                        Document doc = parseXmlDoc(xlf);
                        NodeList fileNodes = doc.getElementsByTagNameNS("*", "file");
                        if (fileNodes.getLength() == 0) continue;
                        String tl = ((Element) fileNodes.item(0)).getAttribute("target-language");
                        if (tl == null || tl.isEmpty()) continue;
                        String tlShort = shortLang(tl);

                        NodeList units = doc.getElementsByTagNameNS("*", "trans-unit");
                        for (int i = 0; i < units.getLength(); i++) {
                            Element tu    = (Element) units.item(i);
                            Element srcEl = findDirectChild(tu, "source");
                            Element tgtEl = findDirectChild(tu, "target");
                            if (srcEl == null || tgtEl == null) continue;
                            if (!"translated".equals(tgtEl.getAttribute("state"))) continue;
                            String srcTxt = srcEl.getTextContent().trim();
                            String tgtTxt = tgtEl.getTextContent().trim();
                            if (srcTxt.isEmpty() || tgtTxt.isEmpty()) continue;

                            Map<String, String> row = translationBase.computeIfAbsent(srcTxt, k -> new HashMap<>());
                            String prev = row.get(tlShort);
                            row.put(tlShort, tgtTxt);
                            if (prev == null || prev.isEmpty()) added++; else updated++;
                        }
                    }
                    return new int[]{added, updated};
                }
            };
            lbl2Msg.textProperty().bind(task2.messageProperty());

            task2.setOnSucceeded(ev -> {
                int[] c = task2.getValue();
                lbl2Msg.textProperty().unbind(); lbl2Msg.setText("Terminé");
                pb2.setProgress(1);
                lbl2Result.setText(c[0] + " entrée(s) ajoutée(s)  |  " + c[1] + " mise(s) à jour");
                btn2Import.setDisable(false);
                btn2Save.setDisable(false);
            });
            task2.setOnFailed(ev -> {
                lbl2Msg.textProperty().unbind(); lbl2Msg.setText("Erreur");
                pb2.setProgress(0);
                btn2Import.setDisable(false);
                new Alert(Alert.AlertType.ERROR, "Erreur : " + task2.getException().getMessage()).showAndWait();
            });
            new Thread(task2).start();
        });

        btn2Save.setOnAction(e -> {
            try {
                saveTsvBase();
                lbl2Result.setText(lbl2Result.getText() + "  —  Base enregistrée.");
                btn2Save.setDisable(true);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
            }
        });

        Scene scene = new Scene(root, 660, 520);
        stage.setTitle("Traduction XLIFF");
        stage.setScene(scene);
        stage.show();
        Platform.runLater(root::requestFocus);
    }

    // =========================================================
    // SAUVEGARDE BASE TSV — UTF-8 avec BOM pour Excel
    // =========================================================

    private void saveTsvBase() throws Exception {
        if (baseFile == null) throw new RuntimeException("Aucune base chargée.");

        Set<String> langSet = new LinkedHashSet<>();
        for (Map<String, String> row : translationBase.values()) langSet.addAll(row.keySet());
        List<String> langs = new ArrayList<>(langSet);

        StringBuilder sb = new StringBuilder("source");
        for (String l : langs) sb.append("\t").append(l);
        sb.append("\n");
        for (Map.Entry<String, Map<String, String>> entry : translationBase.entrySet()) {
            sb.append(entry.getKey());
            for (String l : langs) sb.append("\t").append(entry.getValue().getOrDefault(l, ""));
            sb.append("\n");
        }

        byte[] bom     = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);

        // 1. Écriture dans un fichier temporaire (évite le verrou sur l'original)
        File tmp = new File(baseFile.getParent(), baseFile.getName() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(bom);
            fos.write(content);
            fos.flush();
            fos.getFD().sync(); // force la flush sur disque avant le remplacement
        }

        // 2. Remplacement atomique de l'original
        Files.move(tmp.toPath(), baseFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    // =========================================================
    // CODES TECHNIQUES — masquage / remplacement
    // =========================================================

    private static String translateWithCodes(String text, String sl, String tl) throws Exception {
        if (CODE_MAPPING.isEmpty()) return googleTranslate(text, sl, tl);
        List<String> sortedKeys = new ArrayList<>(CODE_MAPPING.keySet());
        sortedKeys.sort((a, b) -> b.length() - a.length());
        List<String[]> found  = new ArrayList<>();
        String         masked = text;
        for (String code : sortedKeys) {
            Pattern p = Pattern.compile("([ \t]*)\\b" + Pattern.quote(code) + "\\b([ \t]*)");
            Matcher m = p.matcher(masked);
            if (m.find()) {
                int idx = found.size();
                found.add(new String[]{code, m.group(1), m.group(2)});
                masked = m.replaceAll(Matcher.quoteReplacement(m.group(1) + "※" + idx + "※" + m.group(2)));
            }
        }
        if (found.isEmpty()) return googleTranslate(text, sl, tl);
        String textOnly   = masked.replaceAll("※\\d+※", "").trim();
        String translated = textOnly.isEmpty() ? masked : googleTranslate(masked, sl, tl);
        for (int i = 0; i < found.size(); i++) {
            String[] entry    = found.get(i);
            String targetCode = lookupCodeTarget(CODE_MAPPING.get(entry[0]), tl);
            if (targetCode == null) targetCode = entry[0];
            translated = translated.replaceAll(
                    "[ \t]*※\\s*" + i + "\\s*※[ \t]*",
                    Matcher.quoteReplacement(entry[1] + targetCode + entry[2]));
        }
        return translated;
    }

    private static String lookupCodeTarget(Map<String, String> targets, String tl) {
        if (targets == null || tl == null) return null;
        String r = targets.get(tl);
        if (r != null && !r.isEmpty()) return r;
        String s = tl.contains("_") ? tl.split("_")[0] : tl.contains("-") ? tl.split("-")[0] : tl;
        r = targets.get(s);
        if (r != null && !r.isEmpty()) return r;
        String alt = tl.contains("_") ? tl.replace("_", "-") : tl.replace("-", "_");
        r = targets.get(alt);
        return (r != null && !r.isEmpty()) ? r : null;
    }

    // =========================================================
    // PROCESS
    // =========================================================

    private static void runProcess(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String  out  = new String(proc.getInputStream().readAllBytes());
        if (proc.waitFor() != 0) throw new RuntimeException(out.trim().split("\n")[0]);
    }

    // =========================================================
    // XLIFF — PARSING
    // =========================================================

    private static Document parseXmlDoc(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file);
    }

    private static List<TranslationItem> extractItems(Document doc) {
        List<TranslationItem> items = new ArrayList<>();
        NodeList fileNodes = doc.getElementsByTagNameNS("*", "file");
        String defaultSL = "auto", defaultTL = null;
        if (fileNodes.getLength() > 0) {
            Element fe = (Element) fileNodes.item(0);
            defaultSL = coalesce(fe.getAttribute("source-language"));
            defaultTL = fe.getAttribute("target-language");
        }
        NodeList units = doc.getElementsByTagNameNS("*", "trans-unit");
        for (int i = 0; i < units.getLength(); i++) {
            Element tu  = (Element) units.item(i);
            String  src = getChildText(tu);
            Element mainTarget = findDirectChild(tu, "target");
            if (mainTarget != null && defaultTL != null && !defaultTL.isEmpty())
                items.add(makeItem(mainTarget, src, defaultSL, defaultTL));
            NodeList children = tu.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE && "alt-trans".equals(child.getLocalName())) {
                    Element altTrans  = (Element) child;
                    Element altTarget = findDirectChild(altTrans, "target");
                    if (altTarget == null) continue;
                    String lang = altTarget.getAttribute("xml:lang");
                    if (lang == null || lang.isEmpty()) lang = altTrans.getAttribute("xml:lang");
                    if (lang != null && !lang.isEmpty())
                        items.add(makeItem(altTarget, src, defaultSL, lang));
                }
            }
        }
        return items;
    }

    private static TranslationItem makeItem(Element el, String src, String sl, String tl) {
        TranslationItem item = new TranslationItem();
        item.targetElement  = el;
        item.sourceText     = src;
        item.sourceLang     = sl;
        item.targetLang     = tl;
        item.existingTarget = el.getTextContent();
        item.state          = el.getAttribute("state");
        return item;
    }

    // =========================================================
    // XLIFF — UPDATE
    // =========================================================

    private static void updateXliff(Document doc, List<TranslationItem> items,
                                    File output, boolean strictMode) {
        for (TranslationItem item : items) {
            item.targetElement.setTextContent(item.translatedText != null ? item.translatedText : "");
            if (!strictMode || item.fromBase)
                item.targetElement.setAttribute("state", "translated");
        }
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(output));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // XML HELPERS
    // =========================================================

    private static String getChildText(Element parent) {
        Element child = findDirectChild(parent, "source");
        return child != null ? child.getTextContent() : "";
    }

    private static Element findDirectChild(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName()))
                return (Element) child;
        }
        return null;
    }

    private static String coalesce(String s) {
        return (s == null || s.isEmpty()) ? "auto" : s;
    }

    // =========================================================
    // TSV — chargement avec détection d'encodage
    // =========================================================

    private static Map<String, Map<String, String>> loadTsvBase(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content;

        // BOM UTF-8 ?
        if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        } else {
            // Essayer UTF-8, si on trouve des → c'était du Windows-1252
            String utf8 = new String(bytes, StandardCharsets.UTF_8);
            content = utf8.contains("\uFFFD") ? new String(bytes, "windows-1252") : utf8;
        }

        Map<String, Map<String, String>> base = new HashMap<>();
        String[] lines = content.split("\n");
        if (lines.length == 0) return base;

        String[] headers = lines[0].split("\t");
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split("\t", -1);
            if (parts.length == 0 || parts[0].isEmpty()) continue;
            Map<String, String> values = new HashMap<>();
            for (int c = 1; c < headers.length && c < parts.length; c++)
                if (!parts[c].isEmpty()) values.put(headers[c].trim(), parts[c].trim());
            base.put(parts[0].trim(), values);
        }
        return base;
    }

    // =========================================================
    // BASE LOOKUP
    // =========================================================

    private static String lookupBase(Map<String, Map<String, String>> base, String src, String tl) {
        Map<String, String> entry = base.get(src);
        if (entry == null) return null;
        String r = entry.get(tl); if (r != null && !r.isEmpty()) return r;
        String s = tl.contains("_") ? tl.split("_")[0] : tl.contains("-") ? tl.split("-")[0] : tl;
        r = entry.get(s); if (r != null && !r.isEmpty()) return r;
        String sw = tl.contains("_") ? tl.replace("_", "-") : tl.replace("-", "_");
        r = entry.get(sw); return (r != null && !r.isEmpty()) ? r : null;
    }

    // =========================================================
    // GOOGLE TRANSLATE avec retry
    // =========================================================

    private static String googleTranslate(String text, String sl, String tl) throws Exception {
        String s = shortLang(sl), t = shortLang(tl);
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String url = "https://translate.googleapis.com/translate_a/single?client=gtx"
                        + "&sl=" + s + "&tl=" + t + "&dt=t&q="
                        + URLEncoder.encode(text, StandardCharsets.UTF_8);
                String json = getString(url);
                int    start = json.indexOf("[[[");         if (start < 0) return text;
                int    q1    = json.indexOf("\"", start+3); if (q1 < 0)    return text;
                int    q2    = json.indexOf("\"", q1+1);    if (q2 < 0)    return text;
                return json.substring(q1 + 1, q2);
            } catch (Exception ex) {
                last = ex;
                if (attempt < 2) Thread.sleep(1000L * (attempt + 1));
            }
        }
        throw new RuntimeException("Traduction échouée après 3 tentatives : " + last.getMessage(), last);
    }

    private static String getString(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String shortLang(String locale) {
        if (locale == null || locale.isEmpty() || "auto".equals(locale)) return "auto";
        if (locale.contains("_")) return locale.split("_")[0];
        if (locale.contains("-")) return locale.split("-")[0];
        return locale;
    }

    private static String shortenPath(String path) {
        if (path == null || path.isEmpty()) return path;
        File f = new File(path); return f.exists() ? f.getName() : path;
    }

    // =========================================================
    // TRANSLATION ITEM
    // =========================================================

    private static class TranslationItem {
        Element targetElement;
        String  sourceText, sourceLang, targetLang, existingTarget, translatedText, state;
        boolean fromBase;
    }

    // =========================================================
    // XLIFF WORK
    // =========================================================

    private static class XliffWork {
        Document              doc;
        List<TranslationItem> items;
        File                  outputFile;
    }
}