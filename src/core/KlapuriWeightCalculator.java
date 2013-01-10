package core;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import core.F0Estimator.AudioDescriptor;

/**
 * Calculate banded weights according to Klapuri2005.
 */
public class KlapuriWeightCalculator
{

  private class Band
  {
    public final int mMinIndex;
    public final int mMaxIndex;
    public final int mNumBuckets;
    public final double[] mWindowCoefficients;

    public Band(int xiLowIndex)
    {
      mMinIndex = xiLowIndex;
      final double lMinFreq = mMinIndex * mDescriptor.mBucketSizeHz;
      final double lMaxFreq = Math.max(lMinFreq + 100,
                                 lMinFreq * Math.pow(2, 2.0 / 3.0));
      mMaxIndex = (int)Math.ceil(lMaxFreq / mDescriptor.mBucketSizeHz);
      mNumBuckets = (mMaxIndex + 1) - mMinIndex;
      mWindowCoefficients = new double[AudioDescriptor.FRAME_SIZE];

      final double lCentreIndex = (mMinIndex + mMaxIndex) / 2.0;
      for (int lii = 0; lii < AudioDescriptor.FRAME_SIZE; lii++)
      {
        if ((lii >= mMinIndex) && (lii <= mMaxIndex))
        {
          mWindowCoefficients[lii] = 1.0 - ((Math.abs(lCentreIndex - lii) * 2.0) / mNumBuckets);
        }
      }

      System.out.println("");
    }
  }

  private final AudioDescriptor mDescriptor;
  private final Band[] mBands;

  /**
   * Create a Klapuri 2005 Weight Calculator.
   *
   * @param xiDescriptor - description of the audio parameters.
   */
  public KlapuriWeightCalculator(AudioDescriptor xiDescriptor)
  {
    // Save off the audio descriptor.
    mDescriptor = xiDescriptor;

    // Set up frequency band details.
    final List<Band> lBands = new LinkedList<Band>();
    Band lBand = new Band(mDescriptor.mMinFreqIndex);
    while (lBand.mMaxIndex < mDescriptor.mMaxFreqIndex)
    {
      lBands.add(lBand);
      lBand = new Band((lBand.mMinIndex + lBand.mMaxIndex) / 2);
    }
    lBands.add(lBand);

    mBands = lBands.toArray(new Band[lBands.size()]);
  }

  /**
   * Calculate the bandwise Klapuri weights for the given whitened spectrum.
   *
   * @param xiWhitened - whitened spectrum.
   * @return the weights across all bands.
   */
  public double[][] calculateBandwiseWeights(double[] xiWhitened)
  {
    final double[][] lBandWeights = new double[mBands.length][];
    for (int lii = 0; lii < mBands.length; lii++)
    {
      lBandWeights[lii] = calculateBandWeights(xiWhitened, mBands[lii]);
    }
    return lBandWeights;
  }

  /**
   * Calculate global weights from bandwise weights.  See
   * {@link #calculateBandwiseWeights(double[])}.
   *
   * @param xiBandwiseWeights - the bandwise weights.
   * @return the global weights.
   */
  public double[] calculateGlobalWeights(double[][] xiBandwiseWeights)
  {
    // Iterate over the bands, looking for F0 candidates
    final Set<Integer> lTopF0Indicies = new TreeSet<Integer>();

    for (final double[] lBandWeights : xiBandwiseWeights)
    {
      // In each band, find the top 3 F0 candidates.
      int lFirstIndex = 0;
      double lFirstValue = 0;
      int lSecondIndex = 0;
      double lSecondValue = 0;
      int lThirdIndex = 0;
      double lThirdValue = 0;

      for (int lii = 0; lii < lBandWeights.length; lii++)
      {
        // See if this value make it into the top 3 (kept sorted).
        if (lBandWeights[lii] > lFirstValue)
        {
          lThirdIndex = lSecondIndex;
          lSecondIndex = lFirstIndex;
          lFirstIndex = lii;

          lThirdValue = lSecondValue;
          lSecondValue = lFirstValue;
          lFirstValue = lBandWeights[lii];
        }
        else if (lBandWeights[lii] > lSecondValue)
        {
          lThirdIndex = lSecondIndex;
          lSecondIndex = lii;

          lThirdValue = lSecondValue;
          lSecondValue = lBandWeights[lii];
        }
        else if (lBandWeights[lii] > lThirdValue)
        {
          lThirdIndex = lii;

          lThirdValue = lBandWeights[lii];
        }
      }

      // Add the top 3 frequencies from this band into the overall set
      lTopF0Indicies.add(lFirstIndex);
      lTopF0Indicies.add(lSecondIndex);
      lTopF0Indicies.add(lThirdIndex);
    }

    // For each of the F0 candidates, sum the square of the bandwise weights,
    // taking the maximum weight within the permitted inharmonicity.
    final double[] lGlobalWeights = new double[xiBandwiseWeights[16].length];
    for (final Integer lFIndex : lTopF0Indicies)
    {
      if ((lFIndex <= mDescriptor.mMaxFreqIndex) &&
          (lFIndex > 5)) // !! ARR We seem very biased towards low frequencies.  Ignore them for now.
      {
        // !! ARR For now, don't take inharmonicity into account
        for (final double[] lBandWeights : xiBandwiseWeights)
        {
          lGlobalWeights[lFIndex] += Math.pow(lBandWeights[lFIndex], 2.0);
        }
      }
    }

    return lGlobalWeights;
  }

  /**
   * Calculate the Klapuri weights for the given whitened spectrum in a
   * particular band.
   *
   * @param xiWhitened - the whitened spectrum.
   * @param xiBand - the band at which weights should be calculated.
   */
  private double[] calculateBandWeights(double[] xiWhitened, Band xiBand)
  {
    final double lWeights[] = new double[Math.max(mDescriptor.mMaxFreqIndex, xiBand.mMaxIndex + 1)];

    // Look for frequencies that have more than 1 harmonic in the band.
    for (int lIndex = mDescriptor.mMinFreqIndex;
         lIndex <= (xiBand.mNumBuckets - 1);
         lIndex++)
    {
      // Calculate the range of offsets at which we'll look for this frequency.
      // This allows for inharmonicites as per (5) in [Klapuri2005].
      int lMinOffset = (int)Math.round(Math.ceil((double)xiBand.mMinIndex / (double)lIndex) * lIndex) - xiBand.mMinIndex;
      final int h = xiBand.mMaxIndex / lIndex;
      final double lDelta = xiBand.mMaxIndex * (Math.sqrt(1.0 + (0.01 * (Math.pow(h, 2.0) - 1.0))) - 1.0);
      int lMaxOffset = (int)(lMinOffset + lDelta);
      if (lMaxOffset > ((lMinOffset + lIndex) - 1))
      {
        // The spread of positions in which we expect to find the frequency is
        // greater than the frequency itself.  Search everywhere.
        lMinOffset = 0;
        lMaxOffset = lIndex - 1;
      }

      // Within the range of allowed offsets, sum the power of appropriately
      // separated frequencies.  Find the maximum value for that sum over all
      // allowed offsets.
      double lMaxSum = 0;
      for (int lOffset = lMinOffset;
           lOffset <= lMaxOffset;
           lOffset++)
      {
        double lSum = 0;
        int lNumPartialsIncluded = 0;
        for (int lGlobalIndex = xiBand.mMinIndex + lOffset;
             lGlobalIndex <= xiBand.mMaxIndex;
             lGlobalIndex += lIndex)
        {
          lSum += xiWhitened[lGlobalIndex] * xiBand.mWindowCoefficients[lGlobalIndex];
          lNumPartialsIncluded++;
        }
        lSum *= (0.75 / lNumPartialsIncluded) + 0.25;
        lMaxSum = Math.max(lSum,  lMaxSum);
      }

      lWeights[lIndex] = lMaxSum;
    }

    // Look for frequencies that have 1 harmonic in the band.
    int h = 1;
    int k0 = (int)Math.floor((xiBand.mMinIndex + xiBand.mNumBuckets) / (h + 1));
    if (k0 < xiBand.mMinIndex)
    {
      k0 = xiBand.mMinIndex;
    }
    int k1 = xiBand.mMaxIndex;
    while (k0 <= k1)
    {
      for (int k = k0; k <= k1; k++)
      {
        final int n = Math.round(k / h);
        lWeights[n] = Math.max(lWeights[n], xiWhitened[k] * xiBand.mWindowCoefficients[k]);
      }

      h++;
      k0 = (int)Math.ceil(((xiBand.mMinIndex + xiBand.mNumBuckets) * h) / (h + 1));
      if (k0 < xiBand.mMinIndex)
      {
        k0 = xiBand.mMinIndex;
      }
      k1 = (int)Math.floor(((xiBand.mMinIndex - 1) * h) / (h - 1));
      if (k1 > (xiBand.mMinIndex + xiBand.mNumBuckets))
      {
        k1 = xiBand.mMinIndex + xiBand.mMaxIndex;
      }
    }

    return lWeights;
  }
}
