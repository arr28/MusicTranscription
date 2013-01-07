package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Window to display Klapuri weights.
 */
public class WeightWindow
{
  private static final int BUCKET_WIDTH_PX = 5;
  private static final int HEIGHT_AMPLIFIER = 40;

  private final WeightCanvas mCanvas = new WeightCanvas();

  /**
   * Create a new weight window.
   */
  public WeightWindow()
  {
    createWindow();
  }

  private void createWindow()
  {
    //Create and set up the window.
    final JFrame lFrame = new JFrame("Weights");

    // Set the canvas size and add it to the window.
    mCanvas.setPreferredSize(new Dimension(BUCKET_WIDTH_PX * 300, 800));
    lFrame.getContentPane().add(mCanvas, BorderLayout.CENTER);

    //Display the window.
    lFrame.setLocationRelativeTo(null);
    lFrame.pack();
    lFrame.setVisible(true);
  }

  /**
   * Add a set of weights to display.
   *
   * @param xiWeights - the weights (across all bands).
   */
  public void addWeights(double[][] xiWeights)
  {
    mCanvas.addWeights(xiWeights);
  }

  @SuppressWarnings("serial")
  private static class WeightCanvas extends JPanel
  {
    private double[][] mWeightData;
    private double mMaxSeen;

    public void addWeights(double[][] xiWeights)
    {
      mWeightData = Arrays.copyOf(xiWeights, xiWeights.length);
      mMaxSeen = 1.0;
      for (double[] lBandWeights : xiWeights)
      {
        for (double lBandWeight : lBandWeights)
        {
          mMaxSeen = Math.max(mMaxSeen, lBandWeight);
        }
      }
    }

    @Override
    public void paintComponent(Graphics xiGraphics)
    {
      if (mWeightData == null) return;

      for (int lBandIndex = 0; lBandIndex < mWeightData.length; lBandIndex++)
      {
        xiGraphics.setColor(new Color(((lBandIndex % 3) == 0 ? 1 : 0) * 255,
                                      ((lBandIndex % 3) == 1 ? 1 : 0) * 255,
                                      ((lBandIndex % 3) == 2 ? 1 : 0) * 255));

        int lYOffset = lBandIndex * (HEIGHT_AMPLIFIER + 5);
        double[] lBandSamples = mWeightData[lBandIndex];

        int lLastPosX = 0;
        int lLastPosY = lYOffset;

        for (int lSampleIndex = 0;
             lSampleIndex < lBandSamples.length;
             lSampleIndex++)
        {
          int lPosX = lSampleIndex * BUCKET_WIDTH_PX;
          int lPosY = 800 - (int)(lYOffset + (lBandSamples[lSampleIndex] * HEIGHT_AMPLIFIER / mMaxSeen));
          xiGraphics.drawLine(lLastPosX, lLastPosY, lPosX, lPosY);
          lLastPosX = lPosX;
          lLastPosY = lPosY;
        }
      }
    }
  }
}
