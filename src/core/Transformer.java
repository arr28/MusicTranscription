package core;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import core.F0Estimator.AudioDescriptor;

/**
 * Fourier transformer (with Hamming window).
 */
public class Transformer
{
  private final FastFourierTransformer mFreqTransformer;
  private final HammingWindow mHammingWindow;

  /**
   * Create a Fourier transformer.
   */
  public Transformer()
  {
    mFreqTransformer = new FastFourierTransformer(DftNormalization.STANDARD);
    mHammingWindow = new HammingWindow(AudioDescriptor.FRAME_SIZE);
  }

  /**
   * Transform the real-valued samples into the frequency domain.
   *
   * @param xiSamples - input samples.
   * @return frequency spectrum.
   */
  public Complex[] transform(double[] xiSamples)
  {
    // Apply a Hamming window
    mHammingWindow.apply(xiSamples);

    // Perform an FFT.
    return mFreqTransformer.transform(xiSamples, TransformType.FORWARD);
  }
}
