/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.gsf.testrunner.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * <strong>This is a copy of <code>CoverageBar</code> from the gsf.codecoverage</code>
 * module with minor changes only. TODO is to look at to which API the class could be put.</strong>.
 * <p/>
 *
 * Custom component for painting code coverage.
 * I was initially using a JProgressBar, with the BasicProgressBarUI associated with it
 * (to get red/green colors set correctly even on OSX), but it was pretty plain
 * and ugly looking - no nice gradients etc. Hence this component.
 * @todo Add a getBaseline
 *
 * @author Tor Norbye
 */
public final class ResultBar extends JComponent implements ActionListener{
    private static final Color NOT_COVERED_LIGHT = new Color(255, 160, 160);
    private static final Color NOT_COVERED_DARK = new Color(180, 50, 50);
    private static final Color COVERED_LIGHT = new Color(160, 255, 160);
    private static final Color COVERED_DARK = new Color(30, 180, 30);
    private static final Color NO_TESTS_LIGHT = new Color(200, 200, 200);
    private static final Color NO_TESTS_DARK = new Color(110, 110, 110);
    private static final Color ABORTED_TESTS_LIGHT = new Color(246, 232, 206);
    private static final Color ABORTED_TESTS_DARK = new Color(214, 157, 41);
    private boolean emphasize;
    private boolean selected;
    /** Passed tests percentage:  0.0f <= x <= 100f */
    private float passedPercentage = 0.0f;
    /** Skipped tests percentage:  0.0f <= x <= 100f */
    private float skippedPercentage = 0.0f;
    /** Aborted tests percentage:  0.0f <= x <= 100f */
    private float abortedPercentage = 0.0f;

    private Timer timer = new Timer(100, this);
    private int phase = 1;
    private boolean passedReported = false;
    private boolean skippedReported = false;
    private boolean abortedReported = false;

    public ResultBar() {
        updateUI();
        timer.start();
    }

    public void stop(){
        timer.stop();
    }

    public void actionPerformed(ActionEvent e) {
        phase = (phase < getHeight()-1) ? phase + 1 : 1;
        repaint();
    }

    public float getPassedPercentage() {
        return passedPercentage;
    }

    public void setPassedPercentage(float passedPercentage) {
        if(Float.isNaN(passedPercentage)) { // #167230
            passedPercentage = 0.0f;
        }
        this.passedPercentage = passedPercentage;
        this.passedReported = true;
        repaint();
    }

    public void setSkippedPercentage(float skippedPercentage) {
        if(Float.isNaN(skippedPercentage)) { // #167230
            skippedPercentage = 0.0f;
        }
        this.skippedPercentage = skippedPercentage;
        this.skippedReported = true;
        repaint();
    }

    public void setAbortedPercentage(float abortedPercentage) {
        if(Float.isNaN(abortedPercentage)) { // #167230
            abortedPercentage = 0.0f;
        }
        this.abortedPercentage = abortedPercentage;
        this.abortedReported = true;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEmphasize() {
        return emphasize;
    }

    public void setEmphasize(boolean emphasize) {
        this.emphasize = emphasize;
    }

    private String getString() {
        // #183996 (PHP project) requires to use the format "%.2f".
        // It lets to have not rounding a value if number of tests <= 10000
        
        // make it clearer what the shown percentage stands for, since now
        // the color bar schema has been changed/simplyfied
        return "Tests passed: ".concat(String.format("%.2f %%", passedPercentage)); // NOI18N
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public void updateUI() {
        Font f = new JLabel().getFont();
        f = new Font(f.getName(), Font.BOLD, f.getSize());
        setFont(f);
        revalidate();
        repaint();
    }

    public
    @Override
    void paint(Graphics g) {
        // Antialiasing if necessary
        Object value = (Map) (Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")); //NOI18N
        Map renderingHints = (value instanceof Map) ? (java.util.Map) value : null;
        if (renderingHints != null && g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            RenderingHints oldHints = g2d.getRenderingHints();
            g2d.setRenderingHints(renderingHints);
            try {
                super.paint(g2d);
            } finally {
                g2d.setRenderingHints(oldHints);
            }
        } else {
            super.paint(g);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        if (!(g instanceof Graphics2D)) {
            return;
        }

        int width = getWidth();
        int barRectWidth = width;
        int height = getHeight();
        int barRectHeight = height;

        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }

        int amountFull = (int) (barRectWidth * passedPercentage / 100.0f);
	int amountSkip = (int) (barRectWidth * skippedPercentage / 100.0f);
	int amountAbort = (int) (barRectWidth * abortedPercentage / 100.0f);
	int amountFail = Math.abs(barRectWidth - amountFull - amountSkip - amountAbort);
	if(amountFail <= 1) {
	    amountFail = 0;
	}

        Color notCoveredLight = NOT_COVERED_LIGHT;
        Color notCoveredDark = NOT_COVERED_DARK;
        Color coveredLight = COVERED_LIGHT;
        Color coveredDark = COVERED_DARK;
        Color noTestsLight = NO_TESTS_LIGHT;
        Color noTestsDark = NO_TESTS_DARK;
        Color abortedTestsLight = ABORTED_TESTS_LIGHT;
        Color abortedTestsDark = ABORTED_TESTS_DARK;
        if (emphasize) {
            coveredDark = coveredDark.darker();
            notCoveredDark = notCoveredDark.darker();
            noTestsDark = noTestsDark.darker();
            abortedTestsDark = abortedTestsDark.darker();
        } else if (selected) {
            coveredLight = coveredLight.brighter();
            coveredDark = coveredDark.darker();
            notCoveredLight = notCoveredLight.brighter();
            notCoveredDark = notCoveredDark.darker();
            noTestsLight = noTestsLight.brighter();
            noTestsDark = noTestsDark.darker();
            abortedTestsLight = abortedTestsLight.brighter();
            abortedTestsDark = abortedTestsDark.darker();
        }
        Graphics2D g2 = (Graphics2D) g;
        // running with no results yet -> gray
        Color light = noTestsLight;
        Color dark = noTestsDark;

        if (abortedReported || skippedReported || passedReported) {
            // running with at least one result or finished
            if (passedPercentage == 100.0) {
                // contains only successful tests -> green
                light = coveredLight;
                dark = coveredDark;
            } else if (abortedPercentage > 0.0) {
                // contains aborted tests -> abort color
                light = abortedTestsLight;
                dark = abortedTestsDark;
            } else if(100.0f - passedPercentage - abortedPercentage - skippedPercentage > 0.0001) {
                // contains failed tests -> red
                light = notCoveredLight;
                dark = notCoveredDark;
            } else if (skippedPercentage > 0.0) {
                // contains ignored tests -> gray
                light = noTestsLight;
                dark = noTestsDark;
            }
        }
        g2.setPaint(new GradientPaint(0, phase, light, 0, phase + height / 2, dark, true));
        g2.fillRect(1, 1, barRectWidth, height);

        g2.setFont(getFont());
        paintDropShadowText(g2, barRectWidth, barRectHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size;
        Insets border = getInsets();
        FontMetrics fontSizer = getFontMetrics(getFont());
        Container parent = getParent(); // JToolBar registered in ResultPanelTree
        Insets insets = parent.getInsets();
        size = new Dimension(parent.getWidth() - insets.left - insets.right, parent.getHeight()- insets.top - insets.bottom);
        String string = getString();
        int stringWidth = fontSizer.stringWidth(string);
        if (stringWidth > size.width) {
            size.width = stringWidth;
        }
        int stringHeight = fontSizer.getHeight() +
                fontSizer.getDescent();
        if (stringHeight > size.height) {
            size.height = stringHeight;
        }
        size.width += border.left + border.right;
        size.height += border.top + border.bottom;
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension pref = getPreferredSize();
        pref.width = 40;
        return pref;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        pref.width = Short.MAX_VALUE;
        return pref;
    }

    //@Override JDK6
    public int getBaseline(int w, int h) {
        FontMetrics fm = getFontMetrics(getFont());
        return h - fm.getDescent() - ((h - fm.getHeight()) / 2);
    }

    ///////////////////////////////////////////////////////////////////////////////
    // The following code is related to painting drop-shadow text. It is
    // directly based on code in openide.actions/**/HeapView.java by Scott Violet.
    ///////////////////////////////////////////////////////////////////////////////
    /**
     * Image containing text.
     */
    private BufferedImage textImage;
    /**
     * Image containing the drop shadow.
     */
    private BufferedImage dropShadowImage;
    /**
     * Color for the text before blurred.
     */
    private static final Color TEXT_BLUR_COLOR = Color.WHITE;
    /**
     * Color for text drawn on top of blurred text.
     */
    private static final Color TEXT_COLOR = Color.WHITE;
    /**
     * Size used for Kernel used to generate drop shadow.
     */
    private static final int KERNEL_SIZE = 3;
    /**
     * Factor used for Kernel used to generate drop shadow.
     */
    private static final float BLUR_FACTOR = 0.1f;
    /**
     * How far to shift the drop shadow along the horizontal axis.
     */
    private static final int SHIFT_X = 0;
    /**
     * How far to shift the drop shadow along the vertical axis.
     */
    private static final int SHIFT_Y = 1;
    /**
     * Used to generate drop shadown.
     */
    private ConvolveOp blur;

    /**
     * Renders the text using a drop shadow.
     */
    private void paintDropShadowText(Graphics g, int w, int h) {
        if (textImage == null) {
            textImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            dropShadowImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        // Step 1: render the text.
        Graphics2D textImageG = textImage.createGraphics();
        textImageG.setComposite(AlphaComposite.Clear);
        textImageG.fillRect(0, 0, w, h);
        textImageG.setComposite(AlphaComposite.SrcOver);
        textImageG.setColor(TEXT_BLUR_COLOR);
        paintText(textImageG, w, h);
        textImageG.dispose();

        // Step 2: copy the image containing the text to dropShadowImage using
        // the blur effect, which generates a nice drop shadow.
        Graphics2D blurryImageG = dropShadowImage.createGraphics();
        blurryImageG.setComposite(AlphaComposite.Clear);
        blurryImageG.fillRect(0, 0, w, h);
        blurryImageG.setComposite(AlphaComposite.SrcOver);
        if (blur == null) {
            // Configure structures needed for rendering drop shadow.
            int kw = KERNEL_SIZE, kh = KERNEL_SIZE;
            float blurFactor = BLUR_FACTOR;
            float[] kernelData = new float[kw * kh];
            for (int i = 0; i < kernelData.length; i++) {
                kernelData[i] = blurFactor;
            }
            blur = new ConvolveOp(new Kernel(kw, kh, kernelData));
        }
        blurryImageG.drawImage(textImage, blur, SHIFT_X, SHIFT_Y);
        if (emphasize) {
            blurryImageG.setColor(Color.YELLOW);
        } else {
            blurryImageG.setColor(TEXT_COLOR);
        }
        blurryImageG.setFont(getFont());

        // Step 3: render the text again on top.
        paintText(blurryImageG, w, h);
        blurryImageG.dispose();

        // And finally copy it.
        g.drawImage(dropShadowImage, 0, 0, null);
    }

    private void paintText(Graphics g, int w, int h) {
        g.setFont(getFont());
        String text = getString();
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, (w - textWidth) / 2,
                h - fm.getDescent() - ((h - fm.getHeight()) / 2));
    }

}
