package it.casiraghi.swiftbat.ui;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Labeled;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

/** Content-sized controls share each row; narrow containers wrap whole controls. */
final class ResponsiveRow extends HBox {
    private static final String BASIS = ResponsiveRow.class.getName() + ".basis";

    ResponsiveRow(double gap, Node... children) {
        super(gap, children);
        getStyleClass().add("responsive-control-row");
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        setMinHeight(USE_PREF_SIZE);
    }

    static void basis(Node node, double width) { node.getProperties().put(BASIS, width); }

    @Override public Orientation getContentBias() { return Orientation.HORIZONTAL; }
    @Override protected double computeMinWidth(double h) { return 0; }

    private double basis(Node node) {
        Object saved = node.getProperties().get(BASIS);
        double width = saved instanceof Number n ? n.doubleValue() : node.prefWidth(-1);
        Labeled label = node instanceof Labeled labeled ? labeled : null;
        if (node instanceof ChoiceBox<?> choice) {
            Node rendered = choice.lookup(".label");
            if (rendered instanceof Labeled labeled) label = labeled;
        }
        if (label != null) {
            Text measure = new Text(label.getText());
            measure.setFont(label.getFont());
            double inset = node instanceof Region r ? r.getInsets().getLeft() + r.getInsets().getRight() : 0;
            double extra = node instanceof ChoiceBox<?> ? 42 : label.getGraphic() != null
                    ? label.getGraphic().prefWidth(-1) + label.getGraphicTextGap() : 0;
            if (node instanceof javafx.scene.control.CheckBox) extra += 28;
            width = Math.max(width, measure.getLayoutBounds().getWidth() + inset + extra + 8);
        }
        return Math.max(40, width);
    }

    private List<List<Node>> rows(double available) {
        List<List<Node>> rows = new ArrayList<>();
        List<Node> row = new ArrayList<>();
        double used = 0;
        for (Node child : getManagedChildren()) {
            double width = Math.min(available, basis(child));
            if (!row.isEmpty() && used + getSpacing() + width > available + 0.5) {
                rows.add(row); row = new ArrayList<>(); used = 0;
            }
            used += (row.isEmpty() ? 0 : getSpacing()) + width;
            row.add(child);
        }
        if (!row.isEmpty()) rows.add(row);
        return rows;
    }

    private double[] widths(List<Node> row, double available) {
        double sum = row.stream().mapToDouble(this::basis).sum();
        double usable = Math.max(0, available - getSpacing() * (row.size() - 1));
        return row.stream().mapToDouble(node -> usable * basis(node) / sum).toArray();
    }

    @Override protected double computePrefWidth(double h) {
        return snappedLeftInset() + snappedRightInset() + getManagedChildren().stream().mapToDouble(this::basis).sum()
                + getSpacing() * Math.max(0, getManagedChildren().size() - 1);
    }

    @Override protected double computePrefHeight(double w) {
        double available = Math.max(1, (w < 0 ? computePrefWidth(-1) : w) - snappedLeftInset() - snappedRightInset());
        double height = snappedTopInset() + snappedBottomInset();
        List<List<Node>> rows = rows(available);
        for (List<Node> row : rows) {
            double[] widths = widths(row, available);
            double h = 0;
            for (int i = 0; i < row.size(); i++) h = Math.max(h, row.get(i).prefHeight(widths[i]));
            height += h;
        }
        return height + getSpacing() * Math.max(0, rows.size() - 1);
    }

    @Override protected void layoutChildren() {
        double available = Math.max(1, getWidth() - snappedLeftInset() - snappedRightInset());
        double y = snappedTopInset();
        for (List<Node> row : rows(available)) {
            double[] widths = widths(row, available);
            double height = 0;
            for (int i = 0; i < row.size(); i++) height = Math.max(height, row.get(i).prefHeight(widths[i]));
            double x = snappedLeftInset();
            for (int i = 0; i < row.size(); i++) {
                Node node = row.get(i);
                node.resizeRelocate(snapPositionX(x), snapPositionY(y), snapSizeX(widths[i]), snapSizeY(height));
                x += widths[i] + getSpacing();
            }
            y += height + getSpacing();
        }
    }
}
