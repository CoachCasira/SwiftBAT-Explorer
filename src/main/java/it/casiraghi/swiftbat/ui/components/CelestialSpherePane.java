package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.model.SkyPoint3D;
import it.casiraghi.swiftbat.service.SkyCoordinates;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Sfera celeste 3D interattiva.
 * I punti indicano direzioni RA/DEC sulla volta celeste, non distanze fisiche.
 */
public final class CelestialSpherePane extends Pane {
    private static final double RADIUS = 220.0;
    private static final double DEFAULT_CAMERA_Z = -860.0;

    private final Group world = new Group();
    private final Group markerGroup = new Group();
    private final Group galacticGroup = new Group();
    private final Group gridGroup = new Group();
    private final Rotate rotateX = new Rotate(-14, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-24, Rotate.Y_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
    private final Label zoomLabel = new Label("Zoom 100%");

    private final PhongMaterial longMaterial = material("#50d8ff", 0.95);
    private final PhongMaterial shortMaterial = material("#ffae4a", 0.98);
    private final PhongMaterial unknownMaterial = material("#9aa8bf", 0.78);
    private final PhongMaterial galacticMaterial = material("#bd82ff", 0.92);
    private final PhongMaterial gridMaterial = material("#345276", 0.50);
    private final PhongMaterial selectedMaterial = material("#ffffff", 1.0);
    private final Map<String, Sphere> markerNodes = new HashMap<>();

    private List<SkyBurst> bursts = List.of();
    private SkyBurst selectedBurst;
    private Consumer<SkyBurst> onSelect = burst -> { };
    private boolean showGalacticPlane = true;
    private double lastMouseX;
    private double lastMouseY;

    public CelestialSpherePane() {
        setMinHeight(500);
        setPrefHeight(650);
        getStyleClass().add("sky-map-surface");
        setCursor(Cursor.HAND);

        Sphere globe = new Sphere(RADIUS, 64);
        PhongMaterial globeMaterial = material("#193555", 0.18);
        globe.setMaterial(globeMaterial);
        globe.setDrawMode(DrawMode.LINE);
        globe.setCullFace(CullFace.NONE);
        globe.setMouseTransparent(true);

        world.getTransforms().addAll(rotateX, rotateY);
        world.getChildren().addAll(globe, gridGroup, galacticGroup, markerGroup);

        Group root3d = new Group(world);
        AmbientLight ambient = new AmbientLight(Color.web("#c7ddff", 0.78));
        PointLight key = new PointLight(Color.web("#dcecff"));
        key.setTranslateX(-320);
        key.setTranslateY(-260);
        key.setTranslateZ(-480);
        root3d.getChildren().addAll(ambient, key);

        subScene = new SubScene(root3d, 900, 600, true, SceneAntialiasing.BALANCED);
        subScene.setManaged(false);
        subScene.setFill(Color.web("#07101f"));

        /*
         * Con PerspectiveCamera(true) il centro di proiezione è già il centro del viewport.
         * Il mondo deve quindi restare attorno all'origine (0,0,0). La precedente traslazione
         * di width/2 e height/2 spostava la sfera realmente in basso a destra.
         */
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        camera.setFieldOfView(36.0);
        camera.setTranslateZ(DEFAULT_CAMERA_Z);
        subScene.setCamera(camera);

        zoomLabel.getStyleClass().add("sky-zoom-label");
        zoomLabel.setMouseTransparent(true);
        getChildren().addAll(subScene, zoomLabel);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        installInteraction();
        rebuildGrid();
        rebuildGalacticPlane();
    }

    public void setBursts(List<SkyBurst> bursts) {
        this.bursts = bursts == null ? List.of() : List.copyOf(bursts);
        rebuildMarkers();
    }

    public void setOnSelect(Consumer<SkyBurst> onSelect) {
        this.onSelect = onSelect == null ? burst -> { } : onSelect;
    }

    public void setShowGalacticPlane(boolean show) {
        this.showGalacticPlane = show;
        galacticGroup.setVisible(show);
    }

    public void select(SkyBurst burst) {
        if (selectedBurst != null) {
            Sphere previous = markerNodes.get(selectedBurst.grbName());
            if (previous != null) {
                previous.setScaleX(1.0);
                previous.setScaleY(1.0);
                previous.setScaleZ(1.0);
                previous.setMaterial(materialFor(selectedBurst));
            }
        }
        selectedBurst = burst;
        if (burst != null) {
            Sphere current = markerNodes.get(burst.grbName());
            if (current != null) {
                current.setScaleX(1.9);
                current.setScaleY(1.9);
                current.setScaleZ(1.9);
                current.setMaterial(selectedMaterial);
            }
        }
    }

    public void resetView() {
        rotateX.setAngle(-14.0);
        rotateY.setAngle(-24.0);
        camera.setTranslateZ(DEFAULT_CAMERA_Z);
        updateZoomLabel();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        zoomLabel.autosize();
        double y = Math.max(12.0, getHeight() - zoomLabel.prefHeight(-1) - 14.0);
        zoomLabel.relocate(14.0, y);
    }

    private void updateZoomLabel() {
        double factor = Math.abs(DEFAULT_CAMERA_Z / camera.getTranslateZ());
        zoomLabel.setText("Zoom " + Math.round(factor * 100.0) + "%");
    }

    private void rebuildMarkers() {
        markerGroup.getChildren().clear();
        markerNodes.clear();
        for (SkyBurst burst : bursts) {
            SkyPoint3D p = SkyCoordinates.onSphere(burst.raDeg(), burst.decDeg(), RADIUS + 5.0);
            double markerRadius = burst.isShort() ? 3.8 : burst.isLong() ? 2.9 : 3.0;
            Sphere marker = new Sphere(markerRadius, 8);
            marker.setTranslateX(p.x());
            marker.setTranslateY(p.y());
            marker.setTranslateZ(p.z());
            marker.setMaterial(materialFor(burst));
            marker.setUserData(burst);
            Tooltip.install(marker, new Tooltip(tooltipText(burst)));
            marker.setOnMouseEntered(event -> setCursor(Cursor.CROSSHAIR));
            marker.setOnMouseExited(event -> setCursor(Cursor.HAND));
            marker.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    onSelect.accept(burst);
                    event.consume();
                }
            });
            markerGroup.getChildren().add(marker);
            markerNodes.put(burst.grbName(), marker);
        }
        if (selectedBurst != null) {
            select(selectedBurst);
        }
    }

    private PhongMaterial materialFor(SkyBurst burst) {
        return burst.isShort() ? shortMaterial : burst.isLong() ? longMaterial : unknownMaterial;
    }

    private void rebuildGrid() {
        gridGroup.getChildren().clear();

        // Equatore celeste.
        SkyPoint3D previous = null;
        for (int ra = 0; ra <= 360; ra += 5) {
            SkyPoint3D current = SkyCoordinates.onSphere(ra % 360, 0, RADIUS + 0.5);
            if (previous != null) {
                gridGroup.getChildren().add(cylinderBetween(previous, current, 0.34, gridMaterial));
            }
            previous = current;
        }

        // Due paralleli di declinazione per aiutare l'orientamento senza appesantire la vista.
        for (double dec : new double[]{-45.0, 45.0}) {
            previous = null;
            for (int ra = 0; ra <= 360; ra += 8) {
                SkyPoint3D current = SkyCoordinates.onSphere(ra % 360, dec, RADIUS + 0.35);
                if (previous != null) {
                    gridGroup.getChildren().add(cylinderBetween(previous, current, 0.22, gridMaterial));
                }
                previous = current;
            }
        }
    }

    private void rebuildGalacticPlane() {
        galacticGroup.getChildren().clear();
        SkyPoint3D previous = null;
        for (int l = 0; l <= 360; l += 4) {
            double[] eq = SkyCoordinates.galacticPlaneRaDec(l % 360);
            SkyPoint3D current = SkyCoordinates.onSphere(eq[0], eq[1], RADIUS + 2.0);
            if (previous != null) {
                galacticGroup.getChildren().add(cylinderBetween(previous, current, 0.9, galacticMaterial));
            }
            previous = current;
        }
        galacticGroup.setVisible(showGalacticPlane);
    }

    private Cylinder cylinderBetween(SkyPoint3D a, SkyPoint3D b, double radius, PhongMaterial material) {
        Point3D start = new Point3D(a.x(), a.y(), a.z());
        Point3D end = new Point3D(b.x(), b.y(), b.z());
        Point3D diff = end.subtract(start);
        double height = diff.magnitude();
        Cylinder cylinder = new Cylinder(radius, height, 8);
        cylinder.setMaterial(material);
        cylinder.setMouseTransparent(true);
        Point3D midpoint = start.midpoint(end);
        cylinder.setTranslateX(midpoint.getX());
        cylinder.setTranslateY(midpoint.getY());
        cylinder.setTranslateZ(midpoint.getZ());

        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D direction = diff.normalize();
        double dot = Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(direction)));
        double angle = Math.toDegrees(Math.acos(dot));
        Point3D axis = yAxis.crossProduct(direction);
        if (axis.magnitude() > 1e-9) {
            cylinder.setRotationAxis(axis);
            cylinder.setRotate(angle);
        } else if (dot < 0.0) {
            cylinder.setRotationAxis(Rotate.X_AXIS);
            cylinder.setRotate(180.0);
        }
        return cylinder;
    }

    private void installInteraction() {
        subScene.setOnMousePressed(event -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
            if (event.getButton() == MouseButton.PRIMARY) {
                setCursor(Cursor.MOVE);
            }
        });
        subScene.setOnMouseReleased(event -> setCursor(Cursor.HAND));
        subScene.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            double dx = event.getSceneX() - lastMouseX;
            double dy = event.getSceneY() - lastMouseY;
            rotateY.setAngle(rotateY.getAngle() + dx * 0.38);
            rotateX.setAngle(clamp(rotateX.getAngle() - dy * 0.38, -88.0, 88.0));
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
            event.consume();
        });
        subScene.setOnScroll(event -> {
            // Delta positivo = avvicinamento, negativo = allontanamento.
            double next = camera.getTranslateZ() + event.getDeltaY() * 0.85;
            camera.setTranslateZ(clamp(next, -1550.0, -470.0));
            updateZoomLabel();
            event.consume();
        });
        subScene.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            // Robust selection guard for the embedded 3D scene. A click on a GRB
            // is a data-selection action, never a request to open the parent card
            // fullscreen. Consume it here before it can bubble to the card handler.
            Node picked = event.getPickResult() == null ? null : event.getPickResult().getIntersectedNode();
            if (picked != null && picked.getUserData() instanceof SkyBurst burst) {
                onSelect.accept(burst);
                event.consume();
                return;
            }

            if (event.getClickCount() == 2) {
                resetView();
                event.consume();
            }
        });
    }

    private String tooltipText(SkyBurst burst) {
        return burst.grbName()
                + "\nRA: " + String.format(Locale.ITALY, "%.4f°", burst.raDeg())
                + "\nDEC: " + String.format(Locale.ITALY, "%+.4f°", burst.decDeg())
                + "\nT90: " + burst.formattedT90();
    }

    private static PhongMaterial material(String color, double opacity) {
        Color c = Color.web(color, opacity);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(c);
        material.setSpecularColor(c.brighter());
        return material;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
