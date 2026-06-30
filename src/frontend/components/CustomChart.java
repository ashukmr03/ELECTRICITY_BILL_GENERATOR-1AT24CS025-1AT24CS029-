package frontend.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Arrays;

/**
 * CustomChart is a high-fidelity rendering Swing component that draws 
 * Line, Bar, and Donut charts using pure AWT/Swing Graphics2D.
 * Adapts to Dark and Light modes dynamically and uses vector graphics with gradient fills.
 */
public class CustomChart extends JPanel {
    public enum Type {
        LINE_CHART,
        BAR_CHART,
        DONUT_CHART
    }

    private final Type type;
    
    // Data fields
    private double[] values;
    private String[] labels;
    private Color[] customColors;
    
    // Design styles
    private Color primaryColor = new Color(52, 152, 219); // Blue
    private Color accentColor = new Color(230, 126, 34);  // Orange
    
    public CustomChart(Type type) {
        this.type = type;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 200));
    }

    public void setData(double[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        repaint();
    }

    public void setDonutData(double[] values, String[] labels, Color[] colors) {
        this.values = values;
        this.labels = labels;
        this.customColors = colors;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Safe check
        if (values == null || values.length == 0) {
            g2d.setColor(UIManager.getColor("Label.disabledForeground"));
            g2d.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "No Data Available";
            int tx = (width - fm.stringWidth(text)) / 2;
            int ty = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(text, tx, ty);
            g2d.dispose();
            return;
        }

        switch (type) {
            case LINE_CHART:
                paintLineChart(g2d, width, height);
                break;
            case BAR_CHART:
                paintBarChart(g2d, width, height);
                break;
            case DONUT_CHART:
                paintDonutChart(g2d, width, height);
                break;
        }

        g2d.dispose();
    }

    private void paintLineChart(Graphics2D g2d, int w, int h) {
        int padding = 35;
        int chartW = w - (2 * padding);
        int chartH = h - (2 * padding);

        double maxVal = Arrays.stream(values).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;
        
        // Find Y scale factor
        double scaleY = chartH / maxVal;

        // Draw background grid lines (3 lines)
        g2d.setColor(UIManager.getColor("Component.borderColor"));
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5f}, 0.0f));
        for (int i = 0; i <= 3; i++) {
            int gy = padding + (chartH * i / 3);
            g2d.drawLine(padding, gy, padding + chartW, gy);
            
            // Draw grid labels
            double labelVal = maxVal - (maxVal * i / 3);
            g2d.setColor(UIManager.getColor("Label.disabledForeground"));
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2d.drawString(String.format("%.0f", labelVal), 5, gy + 4);
        }

        // Draw axis line
        g2d.setColor(UIManager.getColor("Label.foreground"));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(padding, padding + chartH, padding + chartW, padding + chartH);

        // Map points to pixels
        Point2D.Double[] points = new Point2D.Double[values.length];
        double stepX = values.length > 1 ? (double) chartW / (values.length - 1) : chartW;

        for (int i = 0; i < values.length; i++) {
            double px = padding + (i * stepX);
            double py = padding + chartH - (values[i] * scaleY);
            points[i] = new Point2D.Double(px, py);
        }

        // 1. Draw Gradient Fill Under the Line
        if (points.length > 0) {
            Path2D.Double fillPath = new Path2D.Double();
            fillPath.moveTo(points[0].x, padding + chartH);
            for (Point2D.Double pt : points) {
                fillPath.lineTo(pt.x, pt.y);
            }
            fillPath.lineTo(points[points.length - 1].x, padding + chartH);
            fillPath.closePath();

            // Gradient from primary color to transparent
            Color startColor = new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 100);
            Color endColor = new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 0);
            GradientPaint gp = new GradientPaint(0, padding, startColor, 0, padding + chartH, endColor);
            g2d.setPaint(gp);
            g2d.fill(fillPath);
        }

        // 2. Draw Thick Stroke Line
        g2d.setColor(primaryColor);
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double strokePath = new Path2D.Double();
        if (points.length > 0) {
            strokePath.moveTo(points[0].x, points[0].y);
            for (int i = 1; i < points.length; i++) {
                strokePath.lineTo(points[i].x, points[i].y);
            }
            g2d.draw(strokePath);
        }

        // 3. Draw Point Dots and Labels
        for (int i = 0; i < points.length; i++) {
            Point2D.Double pt = points[i];
            
            // Draw hover/point circle
            g2d.setColor(UIManager.getColor("Panel.background"));
            g2d.fill(new Ellipse2D.Double(pt.x - 5, pt.y - 5, 10, 10));
            g2d.setColor(primaryColor);
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(new Ellipse2D.Double(pt.x - 5, pt.y - 5, 10, 10));

            // Draw X-axis label
            if (labels != null && i < labels.length) {
                g2d.setColor(UIManager.getColor("Label.foreground"));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fm = g2d.getFontMetrics();
                int labelX = (int) (pt.x - fm.stringWidth(labels[i]) / 2);
                g2d.drawString(labels[i], labelX, padding + chartH + 18);
            }
        }
    }

    private void paintBarChart(Graphics2D g2d, int w, int h) {
        int padding = 35;
        int chartW = w - (2 * padding);
        int chartH = h - (2 * padding);

        double maxVal = Arrays.stream(values).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;
        
        // Find Y scale factor
        double scaleY = chartH / maxVal;

        // Draw background grid lines (3 lines)
        g2d.setColor(UIManager.getColor("Component.borderColor"));
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5f}, 0.0f));
        for (int i = 0; i <= 3; i++) {
            int gy = padding + (chartH * i / 3);
            g2d.drawLine(padding, gy, padding + chartW, gy);
            
            // Draw grid labels
            double labelVal = maxVal - (maxVal * i / 3);
            g2d.setColor(UIManager.getColor("Label.disabledForeground"));
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2d.drawString(String.format("%.0f", labelVal), 5, gy + 4);
        }

        // Draw axis line
        g2d.setColor(UIManager.getColor("Label.foreground"));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(padding, padding + chartH, padding + chartW, padding + chartH);

        // Bar layout properties
        int numBars = values.length;
        double gapFraction = 0.3; // 30% gap
        double totalBarW = (double) chartW / numBars;
        double gapW = totalBarW * gapFraction;
        double barW = totalBarW - gapW;

        for (int i = 0; i < numBars; i++) {
            double barH = values[i] * scaleY;
            double bx = padding + (i * totalBarW) + (gapW / 2);
            double by = padding + chartH - barH;

            // Paint bar with beautiful rounded rectangle and gradient
            RoundRectangle2D.Double barRect = new RoundRectangle2D.Double(bx, by, barW, barH, 6, 6);
            GradientPaint gp = new GradientPaint(
                    (float) bx, (float) by, accentColor, 
                    (float) bx, (float) (by + barH), new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120)
            );
            g2d.setPaint(gp);
            g2d.fill(barRect);

            // Draw thin outline
            g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 220));
            g2d.setStroke(new BasicStroke(1f));
            g2d.draw(barRect);

            // Draw label value above bar
            g2d.setColor(UIManager.getColor("Label.foreground"));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            String valStr = String.format("%.0f", values[i]);
            int vx = (int) (bx + (barW - fm.stringWidth(valStr)) / 2);
            g2d.drawString(valStr, vx, (int) (by - 6));

            // Draw X-axis label
            if (labels != null && i < labels.length) {
                int lx = (int) (bx + (barW - fm.stringWidth(labels[i])) / 2);
                g2d.drawString(labels[i], lx, padding + chartH + 18);
            }
        }
    }

    private void paintDonutChart(Graphics2D g2d, int w, int h) {
        double total = Arrays.stream(values).sum();
        if (total <= 0) total = 1.0;

        int size = Math.min(w, h) - 50;
        int cx = (w - size) / 2;
        int cy = (h - size) / 2;

        double currentAngle = 90; // Start at top
        
        // Draw slices
        for (int i = 0; i < values.length; i++) {
            double angle = (values[i] / total) * 360;
            
            Color sliceColor = (customColors != null && i < customColors.length) 
                    ? customColors[i] 
                    : (i % 2 == 0 ? primaryColor : accentColor);
            
            g2d.setColor(sliceColor);
            g2d.fill(new Arc2D.Double(cx, cy, size, size, currentAngle, -angle, Arc2D.PIE));
            
            currentAngle -= angle;
        }

        // Draw inner cutout circle to make it a DONUT
        int donutSize = (int) (size * 0.6);
        int dcx = cx + (size - donutSize) / 2;
        int dcy = cy + (size - donutSize) / 2;
        
        g2d.setColor(UIManager.getColor("Panel.background") != null ? UIManager.getColor("Panel.background") : new Color(30, 30, 30));
        g2d.fill(new Ellipse2D.Double(dcx, dcy, donutSize, donutSize));

        // Draw donut border line
        g2d.setColor(UIManager.getColor("Component.borderColor"));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new Ellipse2D.Double(dcx, dcy, donutSize, donutSize));
        g2d.draw(new Ellipse2D.Double(cx, cy, size, size));

        // Draw legends
        if (labels != null) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2d.getFontMetrics();
            int legendY = cy + size + 18;
            int currentX = 15;
            int gap = 20;

            for (int i = 0; i < labels.length; i++) {
                Color legendColor = (customColors != null && i < customColors.length) 
                        ? customColors[i] 
                        : (i % 2 == 0 ? primaryColor : accentColor);

                // Percentage calculate
                double pct = (values[i] / total) * 100;
                String text = String.format("%s (%.1f%%)", labels[i], pct);

                // Draw legend square box
                g2d.setColor(legendColor);
                g2d.fillRect(currentX, legendY - 8, 10, 10);
                
                g2d.setColor(UIManager.getColor("Label.foreground"));
                g2d.drawString(text, currentX + 15, legendY);
                
                currentX += 15 + fm.stringWidth(text) + gap;
            }
        }
    }
}
