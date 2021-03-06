package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Spectogram window.
 */
public class SpectogramWindow
{
  private static final int BLOCK_WIDTH = 1;
  private static final int BLOCK_HEIGHT = 5;

  private final SpectogramCanvas mCanvas = new SpectogramCanvas();

  /**
   * Create a spectogram window.
   *
   * @param xiTitle - window title.
   */
  public SpectogramWindow(String xiTitle)
  {
    //Create and set up the window.
    final JFrame lFrame = new JFrame(xiTitle);
    lFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Set the canvas size and add it to the window.
    mCanvas.setPreferredSize(new Dimension(912 * BLOCK_WIDTH, 678));
    lFrame.getContentPane().add(mCanvas, BorderLayout.CENTER);

    //Display the window.
    lFrame.setLocationRelativeTo(null);
    lFrame.pack();
    lFrame.setVisible(true);
  }

  /**
   * Add a set of frequency samples to the spectogram.
   *
   * @param xiSamples - the frequency samples.
   */
  public void addSamples(double[] xiSamples)
  {
    mCanvas.addSamples(xiSamples);
  }

  @SuppressWarnings("serial")
  private static class SpectogramCanvas extends JPanel
  {
    private final double[][] mColData;
    private double mMaxSeen = 0.01;
    private int mCurrentCol = 0;

    public SpectogramCanvas()
    {
      mColData = new double[912][];
    }

    public void addSamples(double[] xiSamples)
    {
      mColData[mCurrentCol++] = Arrays.copyOf(xiSamples, xiSamples.length);
      for (final double lSample : xiSamples)
      {
        mMaxSeen = Math.max(lSample, mMaxSeen);
      }
    }

    @Override
    public void paintComponent(Graphics xiGraphics)
    {
      for (int lCol = 0; lCol < mCurrentCol; lCol++)
      {
        float lMaxValue = 1;
        int lMaxIndex = 0;
        for (int lRow = 0; lRow < (mColData[lCol].length / 7); lRow++)
        {
          final float lValue = (float)(1.0 - (mColData[lCol][lRow] / mMaxSeen));
          xiGraphics.setColor(new Color(1, lValue, lValue));
          xiGraphics.fillRect(lCol * BLOCK_WIDTH,
                              678 - (lRow * BLOCK_HEIGHT),
                              BLOCK_WIDTH,
                              BLOCK_HEIGHT);
          if (lValue < lMaxValue)
          {
            lMaxValue = lValue;
            lMaxIndex = lRow;
          }
        }

        // Highlight the maximum - if it exceeds the threshold.
        if (lMaxValue < 0.6) // !! ARR Threshold
        {
          xiGraphics.setColor(new Color(0, 0, 0));
          xiGraphics.fillRect(lCol * BLOCK_WIDTH,
                              678 - (lMaxIndex * BLOCK_HEIGHT),
                              BLOCK_WIDTH,
                              BLOCK_HEIGHT);
        }

        // Show the power in the maximum.
        xiGraphics.setColor(new Color(lMaxValue, lMaxValue, lMaxValue));
        xiGraphics.fillRect(lCol * BLOCK_WIDTH,
                            678 - (-5 * BLOCK_HEIGHT),
                            BLOCK_WIDTH,
                            BLOCK_HEIGHT);
      }
    }
  }
}