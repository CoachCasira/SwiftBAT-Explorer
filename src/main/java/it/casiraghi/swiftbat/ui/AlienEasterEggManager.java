package it.casiraghi.swiftbat.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Lightweight ambient alien Easter egg.
 *
 * <p>For the review phase automatic appearances are intentionally disabled.
 * The small test button runs all available behaviours in a shuffled, compact
 * sequence. The manager scans the visible UI only when an event is requested;
 * it has no pulse timer and no permanent scene-graph observer.</p>
 */
public final class AlienEasterEggManager {
    private static final String INSTALLED = AlienEasterEggManager.class.getName() + ".installed";
    private static final String TEST_BUTTON = AlienEasterEggManager.class.getName() + ".testButton";
    private static final Duration QUICK = Duration.millis(520);
    private static final Duration NORMAL = Duration.millis(820);

    private final Parent root;
    private boolean showcaseRunning;

    private AlienEasterEggManager(Parent root) {
        this.root = root;
    }

    public static void install(Parent root) {
        if (root == null || Boolean.TRUE.equals(root.getProperties().get(INSTALLED))) return;
        root.getProperties().put(INSTALLED, Boolean.TRUE);
        AlienEasterEggManager manager = new AlienEasterEggManager(root);
        Platform.runLater(manager::installTestButton);
    }

    private void installTestButton() {
        HBox topBar = findTopBar(root);
        if (topBar == null) {
            Platform.runLater(this::installTestButton);
            return;
        }
        if (topBar.getChildren().stream().anyMatch(n -> Boolean.TRUE.equals(n.getProperties().get(TEST_BUTTON)))) return;

        Button test = UiFactory.iconButton("👽", "Alien test sequence");
        test.getProperties().put(TEST_BUTTON, Boolean.TRUE);
        test.getStyleClass().add("top-square-action");
        test.setFocusTraversable(false);
        test.setOnAction(event -> playShowcase());
        topBar.getChildren().add(test);
    }

    private void playShowcase() {
        if (showcaseRunning || owner() == null) return;
        showcaseRunning = true;

        List<AlienAction> actions = new ArrayList<>(List.of(AlienAction.values()));
        Collections.shuffle(actions);

        Timeline timeline = new Timeline();
        double stepSeconds = 1.18;
        for (int i = 0; i < actions.size(); i++) {
            AlienAction action = actions.get(i);
            timeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                    Duration.seconds(i * stepSeconds), event -> play(action)));
        }
        timeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                Duration.seconds(actions.size() * stepSeconds + 1.0), event -> showcaseRunning = false));
        timeline.play();
    }

    private void play(AlienAction action) {
        switch (action) {
            case PEEK_LEFT -> peek(Side.LEFT);
            case PEEK_RIGHT -> peek(Side.RIGHT);
            case PEEK_TOP -> peek(Side.TOP);
            case PEEK_BOTTOM -> peek(Side.BOTTOM);
            case SCURRY_EDGE -> scurryEdge();
            case UFO_FLYBY -> ufoFlyby();
            case STEAL_LETTER -> stealLetter();
            case DROP_FROM_TOP -> dropFromTop();
            case TELEPORT -> teleport();
            case ORBIT_CONTROL -> orbitControl();
            case WATCH_CHART -> watchChart();
            case HIDE_BY_SCROLLBAR -> hideByScrollbar();
            case RIDE_SCROLLBAR -> rideScrollbar();
            case DANCE -> dance();
            case BEAM_UP -> beamUp();
            case SNEAK_ALONG_TEXT -> sneakAlongText();
            case TABLE_PEEK -> tablePeek();
            case NAP_ON_CARD -> napOnCard();
            case STAR_SPRAY -> starSpray();
            case CORNER_BLINK -> cornerBlink();
        }
    }

    private void peek(Side side) {
        Node target = randomTarget(node -> node instanceof ButtonBase
                || node instanceof TableView<?> || hasCardStyle(node));
        Bounds b = target == null ? randomSyntheticBounds(120, 70) : screenBounds(target);
        if (b == null) return;

        Group alien = buildAlien(1.0);
        double x = switch (side) {
            case LEFT -> b.getMinX() - 13;
            case RIGHT -> b.getMaxX() - 13;
            default -> b.getMinX() + Math.max(10, b.getWidth() * 0.35);
        };
        double y = switch (side) {
            case TOP -> b.getMinY() - 13;
            case BOTTOM -> b.getMaxY() - 14;
            default -> b.getMinY() + Math.max(8, b.getHeight() * 0.28);
        };

        Popup popup = showPopup(alien, x, y);
        if (popup == null) return;
        double dx = side == Side.LEFT ? 16 : side == Side.RIGHT ? -16 : 0;
        double dy = side == Side.TOP ? 16 : side == Side.BOTTOM ? -16 : 0;
        alien.setTranslateX(-dx);
        alien.setTranslateY(-dy);
        SequentialTransition seq = new SequentialTransition(
                translate(alien, dx, dy, QUICK),
                new PauseTransition(Duration.millis(460)),
                translate(alien, -dx, -dy, QUICK));
        finish(seq, popup);
    }

    private void scurryEdge() {
        Node target = randomTarget(node -> node instanceof Region && hasCardStyle(node));
        Bounds b = target == null ? randomSyntheticBounds(360, 120) : screenBounds(target);
        if (b == null) return;
        Pane lane = transparentPane(Math.max(160, b.getWidth()), 48);
        Group alien = buildAlien(0.78);
        alien.setLayoutY(8);
        lane.getChildren().add(alien);
        Popup popup = showPopup(lane, b.getMinX(), b.getMinY() - 18);
        if (popup == null) return;
        TranslateTransition run = new TranslateTransition(Duration.millis(1250), alien);
        run.setFromX(5);
        run.setToX(Math.max(40, lane.getPrefWidth() - 42));
        run.setInterpolator(Interpolator.EASE_BOTH);
        finish(run, popup);
    }

    private void ufoFlyby() {
        Window window = owner();
        if (window == null) return;
        Pane lane = transparentPane(window.getWidth(), 82);
        Group ufo = buildUfo();
        ufo.setLayoutY(18);
        lane.getChildren().add(ufo);
        double y = window.getY() + 90 + rnd(Math.max(80, window.getHeight() - 260));
        Popup popup = showPopup(lane, window.getX(), y);
        if (popup == null) return;
        TranslateTransition fly = new TranslateTransition(Duration.millis(1550), ufo);
        fly.setFromX(-90);
        fly.setToX(window.getWidth() + 80);
        fly.setInterpolator(Interpolator.EASE_BOTH);
        RotateTransition tilt = new RotateTransition(Duration.millis(420), ufo);
        tilt.setByAngle(8);
        tilt.setAutoReverse(true);
        tilt.setCycleCount(4);
        finish(new ParallelTransition(fly, tilt), popup);
    }

    private void stealLetter() {
        Label label = (Label) randomTarget(node -> node instanceof Label l
                && l.getText() != null && l.getText().trim().length() >= 6
                && l.getWidth() > 80 && !l.getText().contains("GRB"));
        if (label == null) {
            sneakAlongText();
            return;
        }
        Bounds b = screenBounds(label);
        if (b == null) return;
        String original = label.getText();
        int index = stealableIndex(original);
        if (index < 0) return;
        char stolen = original.charAt(index);
        String shortened = original.substring(0, index) + original.substring(index + 1);
        label.setText(shortened);

        Group alien = buildAlien(0.9);
        Text letter = new Text(String.valueOf(stolen));
        letter.setFill(Color.web("#d8ecff"));
        letter.setFont(Font.font(Math.max(12, label.getFont().getSize())));
        letter.setLayoutX(27);
        letter.setLayoutY(15);
        Group thief = new Group(alien, letter);
        Popup popup = showPopup(thief, b.getMinX() + Math.min(b.getWidth() - 20, 24 + index * 5.5), b.getMinY() - 12);
        if (popup == null) {
            label.setText(original);
            return;
        }
        TranslateTransition escape = new TranslateTransition(Duration.millis(1050), thief);
        escape.setByX(105 + rnd(55));
        escape.setByY(-45 - rnd(40));
        RotateTransition spin = new RotateTransition(Duration.millis(1050), thief);
        spin.setByAngle(24);
        ParallelTransition animation = new ParallelTransition(escape, spin);
        animation.setOnFinished(event -> {
            if (Objects.equals(label.getText(), shortened)) label.setText(original);
            popup.hide();
        });
        animation.play();
    }

    private void dropFromTop() {
        Window window = owner();
        if (window == null) return;
        Group alien = buildAlien(0.92);
        double x = window.getX() + 80 + rnd(Math.max(40, window.getWidth() - 180));
        Popup popup = showPopup(alien, x, window.getY() + 42);
        if (popup == null) return;
        alien.setTranslateY(-45);
        TranslateTransition fall = translate(alien, 0, 230 + rnd(150), Duration.millis(900));
        fall.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition squash = new ScaleTransition(Duration.millis(170), alien);
        squash.setToY(0.72);
        squash.setAutoReverse(true);
        squash.setCycleCount(2);
        finish(new SequentialTransition(fall, squash, new PauseTransition(Duration.millis(280))), popup);
    }

    private void teleport() {
        Window window = owner();
        if (window == null) return;
        Pane field = transparentPane(window.getWidth(), window.getHeight());
        Group alien = buildAlien(0.95);
        field.getChildren().add(alien);
        Popup popup = showPopup(field, window.getX(), window.getY());
        if (popup == null) return;

        Timeline timeline = new Timeline();
        for (int i = 0; i < 4; i++) {
            double t = i * 0.33;
            double x = 60 + rnd(Math.max(40, window.getWidth() - 140));
            double y = 70 + rnd(Math.max(40, window.getHeight() - 150));
            timeline.getKeyFrames().add(new javafx.animation.KeyFrame(Duration.seconds(t), event -> {
                alien.setOpacity(0);
                alien.setLayoutX(x);
                alien.setLayoutY(y);
            }));
            timeline.getKeyFrames().add(new javafx.animation.KeyFrame(Duration.seconds(t + 0.08), event -> alien.setOpacity(1)));
            timeline.getKeyFrames().add(new javafx.animation.KeyFrame(Duration.seconds(t + 0.23), event -> alien.setOpacity(0)));
        }
        timeline.setOnFinished(event -> popup.hide());
        timeline.play();
    }

    private void orbitControl() {
        Node target = randomTarget(node -> node instanceof ButtonBase || node instanceof TabPane);
        Bounds b = target == null ? randomSyntheticBounds(100, 55) : screenBounds(target);
        if (b == null) return;
        Pane field = transparentPane(130, 110);
        Group alien = buildAlien(0.72);
        field.getChildren().add(alien);
        Popup popup = showPopup(field, b.getMinX() + b.getWidth() / 2 - 65, b.getMinY() + b.getHeight() / 2 - 55);
        if (popup == null) return;

        Timeline orbit = new Timeline();
        for (int i = 0; i <= 16; i++) {
            double angle = Math.PI * 2 * i / 16.0;
            double x = 54 + Math.cos(angle) * 45;
            double y = 39 + Math.sin(angle) * 30;
            orbit.getKeyFrames().add(new javafx.animation.KeyFrame(Duration.millis(i * 55), event -> {
                alien.setLayoutX(x);
                alien.setLayoutY(y);
            }));
        }
        orbit.setOnFinished(event -> popup.hide());
        orbit.play();
    }

    private void watchChart() {
        Node chart = randomTarget(node -> node instanceof Chart);
        Bounds b = chart == null ? randomSyntheticBounds(420, 240) : screenBounds(chart);
        if (b == null) return;
        Group scene = new Group();
        Group alien = buildAlien(0.9);
        alien.setLayoutX(0);
        alien.setLayoutY(0);
        Line tripod = new Line(28, 23, 40, 34);
        tripod.setStroke(Color.web("#80cfff"));
        tripod.setStrokeWidth(2);
        Line scope = new Line(24, 17, 43, 12);
        scope.setStroke(Color.web("#b893ff"));
        scope.setStrokeWidth(3);
        scene.getChildren().addAll(alien, tripod, scope);
        Popup popup = showPopup(scene, b.getMinX() + Math.max(18, b.getWidth() * 0.68), b.getMaxY() - 52);
        if (popup == null) return;
        finish(new SequentialTransition(new PauseTransition(Duration.millis(1350)), fade(scene, 1, 0, QUICK)), popup);
    }

    private void hideByScrollbar() {
        Node target = randomTarget(node -> node instanceof ScrollPane || node instanceof TableView<?>);
        Bounds b = target == null ? randomSyntheticBounds(300, 300) : screenBounds(target);
        if (b == null) return;
        Group alien = buildAlien(0.78);
        Popup popup = showPopup(alien, b.getMaxX() - 17, b.getMinY() + 30 + rnd(Math.max(20, b.getHeight() - 90)));
        if (popup == null) return;
        alien.setTranslateX(18);
        finish(new SequentialTransition(
                translate(alien, -19, 0, QUICK),
                new PauseTransition(Duration.millis(520)),
                translate(alien, 20, 0, QUICK)), popup);
    }

    private void rideScrollbar() {
        Node target = randomTarget(node -> node instanceof ScrollPane || node instanceof TableView<?>);
        Bounds b = target == null ? randomSyntheticBounds(300, 340) : screenBounds(target);
        if (b == null) return;
        Group alien = buildAlien(0.7);
        Popup popup = showPopup(alien, b.getMaxX() - 17, b.getMinY() + 18);
        if (popup == null) return;
        TranslateTransition ride = translate(alien, 0, Math.max(55, b.getHeight() - 75), Duration.millis(1200));
        ride.setAutoReverse(true);
        ride.setCycleCount(2);
        finish(ride, popup);
    }

    private void dance() {
        Bounds b = randomSyntheticBounds(40, 40);
        if (b == null) return;
        Group alien = buildAlien(1.0);
        Popup popup = showPopup(alien, b.getMinX(), b.getMinY());
        if (popup == null) return;
        RotateTransition rotate = new RotateTransition(Duration.millis(180), alien);
        rotate.setByAngle(24);
        rotate.setAutoReverse(true);
        rotate.setCycleCount(6);
        ScaleTransition bounce = new ScaleTransition(Duration.millis(180), alien);
        bounce.setToY(1.22);
        bounce.setToX(0.9);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(6);
        finish(new ParallelTransition(rotate, bounce), popup);
    }

    private void beamUp() {
        Bounds b = randomSyntheticBounds(80, 120);
        if (b == null) return;
        Pane field = transparentPane(80, 150);
        Polygon beam = new Polygon(24.0, 10.0, 55.0, 10.0, 72.0, 145.0, 8.0, 145.0);
        beam.setFill(Color.web("#62f5c4", 0.10));
        Group alien = buildAlien(0.86);
        alien.setLayoutX(26);
        alien.setLayoutY(105);
        field.getChildren().addAll(beam, alien);
        Popup popup = showPopup(field, b.getMinX(), b.getMinY() - 80);
        if (popup == null) return;
        TranslateTransition rise = translate(alien, 0, -102, Duration.millis(980));
        FadeTransition vanish = fade(alien, 1, 0, Duration.millis(980));
        finish(new ParallelTransition(rise, vanish), popup);
    }

    private void sneakAlongText() {
        Node target = randomTarget(node -> node instanceof Label l && l.getText() != null && l.getText().length() >= 5);
        Bounds b = target == null ? randomSyntheticBounds(220, 35) : screenBounds(target);
        if (b == null) return;
        Pane lane = transparentPane(Math.max(120, b.getWidth()), 44);
        Group alien = buildAlien(0.62);
        alien.setLayoutY(9);
        lane.getChildren().add(alien);
        Popup popup = showPopup(lane, b.getMinX(), b.getMaxY() - 8);
        if (popup == null) return;
        TranslateTransition sneak = translate(alien, Math.max(35, lane.getPrefWidth() - 34), 0, Duration.millis(1250));
        sneak.setInterpolator(Interpolator.EASE_BOTH);
        finish(sneak, popup);
    }

    private void tablePeek() {
        Node target = randomTarget(node -> node instanceof TableView<?>);
        Bounds b = target == null ? randomSyntheticBounds(360, 240) : screenBounds(target);
        if (b == null) return;
        Group alien = buildAlien(0.83);
        double x = b.getMinX() + 45 + rnd(Math.max(20, b.getWidth() - 110));
        Popup popup = showPopup(alien, x, b.getMinY() + 12);
        if (popup == null) return;
        alien.setTranslateY(-26);
        finish(new SequentialTransition(
                translate(alien, 0, 25, QUICK),
                new PauseTransition(Duration.millis(560)),
                translate(alien, 0, -25, QUICK)), popup);
    }

    private void napOnCard() {
        Node target = randomTarget(this::hasCardStyle);
        Bounds b = target == null ? randomSyntheticBounds(300, 140) : screenBounds(target);
        if (b == null) return;
        Group alien = buildAlien(0.68);
        alien.setRotate(90);
        Text zzz = new Text("zZz");
        zzz.setFill(Color.web("#8bbcff", 0.75));
        zzz.setFont(Font.font(11));
        zzz.setLayoutX(28);
        zzz.setLayoutY(-2);
        Group nap = new Group(alien, zzz);
        Popup popup = showPopup(nap, b.getMinX() + 35 + rnd(Math.max(25, b.getWidth() - 100)), b.getMinY() - 7);
        if (popup == null) return;
        FadeTransition pulse = fade(zzz, 0.25, 1, Duration.millis(360));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(4);
        finish(new SequentialTransition(pulse, new PauseTransition(Duration.millis(220))), popup);
    }

    private void starSpray() {
        Bounds b = randomSyntheticBounds(110, 90);
        if (b == null) return;
        Pane field = transparentPane(120, 100);
        Group alien = buildAlien(0.82);
        alien.setLayoutX(42);
        alien.setLayoutY(38);
        field.getChildren().add(alien);
        List<Circle> stars = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            Circle star = new Circle(2.0 + rnd(1.7), i % 2 == 0 ? Color.web("#00ddff") : Color.web("#c76cff"));
            star.setLayoutX(55);
            star.setLayoutY(45);
            stars.add(star);
            field.getChildren().add(star);
        }
        Popup popup = showPopup(field, b.getMinX(), b.getMinY());
        if (popup == null) return;
        ParallelTransition burst = new ParallelTransition();
        for (int i = 0; i < stars.size(); i++) {
            Circle star = stars.get(i);
            double angle = Math.PI * 2 * i / stars.size();
            TranslateTransition move = translate(star, Math.cos(angle) * (32 + rnd(18)), Math.sin(angle) * (28 + rnd(16)), NORMAL);
            FadeTransition fade = fade(star, 1, 0, NORMAL);
            burst.getChildren().add(new ParallelTransition(move, fade));
        }
        finish(new SequentialTransition(new PauseTransition(Duration.millis(180)), burst), popup);
    }

    private void cornerBlink() {
        Window window = owner();
        if (window == null) return;
        Group alien = buildAlien(0.74);
        boolean right = ThreadLocalRandom.current().nextBoolean();
        double x = right ? window.getX() + window.getWidth() - 35 : window.getX() + 8;
        double y = window.getY() + 45 + rnd(Math.max(30, window.getHeight() - 120));
        Popup popup = showPopup(alien, x, y);
        if (popup == null) return;
        FadeTransition blink = fade(alien, 0.15, 1, Duration.millis(130));
        blink.setAutoReverse(true);
        blink.setCycleCount(6);
        finish(blink, popup);
    }

    private Group buildAlien(double scale) {
        Ellipse head = new Ellipse(14, 10, 12, 10);
        head.setFill(Color.web("#63f5c3"));
        head.setStroke(Color.web("#00bfe8"));
        head.setStrokeWidth(1.1);

        Ellipse eyeLeft = new Ellipse(10, 9, 3.2, 4.5);
        Ellipse eyeRight = new Ellipse(18, 9, 3.2, 4.5);
        eyeLeft.setFill(Color.web("#06111f"));
        eyeRight.setFill(Color.web("#06111f"));
        Circle glintLeft = new Circle(9.2, 7.9, 0.8, Color.web("#dfffff"));
        Circle glintRight = new Circle(17.2, 7.9, 0.8, Color.web("#dfffff"));

        Rectangle body = new Rectangle(8, 19, 12, 12);
        body.setArcWidth(7);
        body.setArcHeight(7);
        body.setFill(Color.web("#2b8fae"));
        Line armLeft = new Line(8, 22, 3, 26);
        Line armRight = new Line(20, 22, 25, 26);
        Line legLeft = new Line(11, 30, 9, 35);
        Line legRight = new Line(17, 30, 19, 35);
        for (Line limb : List.of(armLeft, armRight, legLeft, legRight)) {
            limb.setStroke(Color.web("#63f5c3"));
            limb.setStrokeWidth(2.1);
            limb.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        }

        Group alien = new Group(body, armLeft, armRight, legLeft, legRight,
                head, eyeLeft, eyeRight, glintLeft, glintRight);
        alien.setScaleX(scale);
        alien.setScaleY(scale);
        alien.setEffect(new DropShadow(7, Color.web("#00d7ff", 0.55)));
        alien.setMouseTransparent(true);
        return alien;
    }

    private Group buildUfo() {
        Ellipse dome = new Ellipse(39, 16, 15, 10);
        dome.setFill(Color.web("#63f5c3", 0.75));
        dome.setStroke(Color.web("#00d7ff"));
        Ellipse saucer = new Ellipse(39, 24, 34, 9);
        saucer.setFill(Color.web("#6b43cc"));
        saucer.setStroke(Color.web("#00d7ff"));
        Circle light1 = new Circle(22, 25, 2.2, Color.web("#ff4bd8"));
        Circle light2 = new Circle(39, 27, 2.2, Color.web("#63f5c3"));
        Circle light3 = new Circle(56, 25, 2.2, Color.web("#ff4bd8"));
        Group ufo = new Group(dome, saucer, light1, light2, light3);
        ufo.setEffect(new DropShadow(10, Color.web("#6c4dff", 0.65)));
        ufo.setMouseTransparent(true);
        return ufo;
    }

    private Popup showPopup(Node content, double screenX, double screenY) {
        Window window = owner();
        if (window == null || content == null) return null;
        content.setMouseTransparent(true);
        Popup popup = new Popup();
        popup.setAutoFix(false);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.getContent().add(content);
        double x = clamp(screenX, window.getX() + 2, window.getX() + Math.max(2, window.getWidth() - 42));
        double y = clamp(screenY, window.getY() + 2, window.getY() + Math.max(2, window.getHeight() - 45));
        popup.show(window, x, y);
        return popup;
    }

    private void finish(Animation animation, Popup popup) {
        animation.setOnFinished(event -> popup.hide());
        animation.play();
    }

    private TranslateTransition translate(Node node, double dx, double dy, Duration duration) {
        TranslateTransition transition = new TranslateTransition(duration, node);
        transition.setByX(dx);
        transition.setByY(dy);
        return transition;
    }

    private FadeTransition fade(Node node, double from, double to, Duration duration) {
        FadeTransition transition = new FadeTransition(duration, node);
        transition.setFromValue(from);
        transition.setToValue(to);
        return transition;
    }

    private Pane transparentPane(double width, double height) {
        Pane pane = new Pane();
        pane.setMinSize(width, height);
        pane.setPrefSize(width, height);
        pane.setMaxSize(width, height);
        pane.setMouseTransparent(true);
        return pane;
    }

    private Node randomTarget(Predicate<Node> predicate) {
        Window window = owner();
        if (window == null) return null;
        List<Node> candidates = new ArrayList<>();
        collectTargets(root, predicate, candidates, window);
        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private void collectTargets(Node node, Predicate<Node> predicate, List<Node> out, Window window) {
        if (node == null || !node.isVisible() || node.getOpacity() < 0.15 || node.getScene() == null) return;
        Bounds screen = screenBounds(node);
        if (screen != null && screen.getWidth() > 18 && screen.getHeight() > 10
                && intersectsWindow(screen, window) && predicate.test(node)) {
            out.add(node);
        }
        if (node instanceof Control || node instanceof Chart) return;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectTargets(child, predicate, out, window);
        }
    }

    private boolean hasCardStyle(Node node) {
        if (!(node instanceof Region)) return false;
        for (String style : node.getStyleClass()) {
            String s = style.toLowerCase();
            if (s.contains("card") || s.contains("panel") || s.contains("shell")) return true;
        }
        return false;
    }

    private HBox findTopBar(Node node) {
        if (node instanceof HBox box && box.getStyleClass().contains("top-bar")) return box;
        if (node instanceof Control) return null;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findTopBar(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Bounds screenBounds(Node node) {
        try {
            return node.localToScreen(node.getBoundsInLocal());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Bounds randomSyntheticBounds(double width, double height) {
        Window window = owner();
        if (window == null) return null;
        double x = window.getX() + 70 + rnd(Math.max(20, window.getWidth() - width - 140));
        double y = window.getY() + 80 + rnd(Math.max(20, window.getHeight() - height - 150));
        return new javafx.geometry.BoundingBox(x, y, width, height);
    }

    private boolean intersectsWindow(Bounds b, Window window) {
        return b.getMaxX() >= window.getX() && b.getMinX() <= window.getX() + window.getWidth()
                && b.getMaxY() >= window.getY() && b.getMinY() <= window.getY() + window.getHeight();
    }

    private Window owner() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    private int stealableIndex(String text) {
        if (text == null || text.length() < 4) return -1;
        List<Integer> indexes = new ArrayList<>();
        for (int i = 1; i < text.length() - 1; i++) {
            if (Character.isLetter(text.charAt(i))) indexes.add(i);
        }
        return indexes.isEmpty() ? -1 : indexes.get(ThreadLocalRandom.current().nextInt(indexes.size()));
    }

    private static double rnd(double max) {
        return max <= 0 ? 0 : ThreadLocalRandom.current().nextDouble(max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum AlienAction {
        PEEK_LEFT,
        PEEK_RIGHT,
        PEEK_TOP,
        PEEK_BOTTOM,
        SCURRY_EDGE,
        UFO_FLYBY,
        STEAL_LETTER,
        DROP_FROM_TOP,
        TELEPORT,
        ORBIT_CONTROL,
        WATCH_CHART,
        HIDE_BY_SCROLLBAR,
        RIDE_SCROLLBAR,
        DANCE,
        BEAM_UP,
        SNEAK_ALONG_TEXT,
        TABLE_PEEK,
        NAP_ON_CARD,
        STAR_SPRAY,
        CORNER_BLINK
    }

    private enum Side { LEFT, RIGHT, TOP, BOTTOM }
}
