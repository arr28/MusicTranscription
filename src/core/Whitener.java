package core;

import org.apache.commons.math3.complex.Complex;

import core.F0Estimator.AudioDescriptor;

/**
 * Spectrum whitener.
 */
public class Whitener
{

  private final AudioDescriptor mDescriptor;

  /**
   * Create a spectrum whitener.
   *
   * @param xiDescriptor - description of the audio data.
   */
  public Whitener(AudioDescriptor xiDescriptor)
  {
    mDescriptor = xiDescriptor;
  }

  /**
   * Whiten the supplied spectrum.
   *
   * @param xiFreq - the raw spectrum.
   *
   * @return the whitened spectrum.
   */
  public double[] whiten(Complex[] xiFreq)
  {
    final double[] lScaled = warpMagnitudes(xiFreq);
    removeNoise(lScaled);

    // Remove the noise.  See (4) in [Klapuri2005].

    return lScaled;
  }

  /**
   * Perform "magnitude warping" to compensate for noise and the environmental
   * response to the signal.  See (2) and (3) in [Klapuri2005].
   */
  private double[] warpMagnitudes(Complex[] xiFreq)
  {
    // Compute the scaling factor (g).
    final double[] lScaled = new double[xiFreq.length];
    double lScalingFactor = 0;
    for (int lFSample = mDescriptor.mMinFreqIndex;
         lFSample <= mDescriptor.mMaxFreqIndex;
         lFSample++)
    {
      lScalingFactor += Math.pow(Math.abs(xiFreq[lFSample].abs()), 1.0 / 3.0);
    }
    lScalingFactor /= (mDescriptor.mMaxFreqIndex + 1) - mDescriptor.mMinFreqIndex;
    lScalingFactor = Math.pow(lScalingFactor, 3);

    // Perform the magnitude warping.
    for (int lFSample = 0; lFSample < xiFreq.length; lFSample++)
    {
      lScaled[lFSample] =
          Math.log1p(Math.abs(xiFreq[lFSample].abs()) / lScalingFactor);
    }

    return lScaled;
  }

  private void removeNoise(double[] xiScaledFreq)
  {
    int lStartIndex = mDescriptor.mMinFreqIndex;
    while (lStartIndex <= mDescriptor.mMaxFreqIndex)
    {
      // Calculate the end of the band
      int lEndIndex = (int)Math.pow(lStartIndex, 4.0 / 3.0);
      if (lEndIndex < (lStartIndex + 5))
      {
        lEndIndex = lStartIndex + 5;
      }

      double lAvgMagnitude = 0;
      for (int lFSample = lStartIndex; lFSample < lEndIndex; lFSample++)
      {
        lAvgMagnitude += xiScaledFreq[lFSample];
      }
      lAvgMagnitude /= ((mDescriptor.mMaxFreqIndex + 1)- mDescriptor.mMinFreqIndex);
      for (int lFSample = lStartIndex; lFSample < lEndIndex; lFSample++)
      {
        xiScaledFreq[lFSample] = Math.max(0, xiScaledFreq[lFSample] - lAvgMagnitude);
      }

      lStartIndex = lEndIndex;
    }
  }
}
