package core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

import ui.SpectogramWindow;
import ui.WeightWindow;
import core.WavFile.WavFileException;

/**
 * Entry point for F0 estimation.
 */
public class F0Estimator
{
  /**
   * Descriptor of audio file and properties.
   */
  public static class AudioDescriptor
  {
    /**
     * The frame size (in samples) for estimates.
     *
     * Assuming a sample rate of 44,100 samples/seconds...
     *
     * - Frame size 2048 = 46ms
     * - Frame size 4096 = 92ms
     */
    public static final int FRAME_SIZE = 2048;

    /**
     * The minimum F0 candidate.
     */
    public static final int FREQ_MIN = 50;

    /**
     * The maximum F0 candidate (and maximum frequency that will be considered
     * as a possible harmonic of a lower F0).
     */
    public static final int FREQ_MAX = 6000;

    /**
     * Sample rate (per second).
     */
    public final int mSampleRate;

    /**
     * Size a single FFT bucket (in Hz).
     */
    public final double mBucketSizeHz;

    /**
     * Bucket index for the minimum frequency.
     */
    public final int mMinFreqIndex;

    /**
     * Bucket index for the maximum frequency.
     */
    public final int mMaxFreqIndex;

    /**
     * Create an audio descriptor.
     *
     * @param xiSampleRate - the sample rate (per second) of the audio.
     * @param xiBucketSizeHz - the Fourier transform bucket size (in Hz).
     */
    public AudioDescriptor(int xiSampleRate, int xiBucketSizeHz)
    {
      mSampleRate = xiSampleRate;
      mBucketSizeHz = xiBucketSizeHz;

      // Find the FFT buckets containing the lowest (50Hz) and highest (6kHz)
      // frequencies that we'll be dealing with.
      mMinFreqIndex = (int)(FREQ_MIN / mBucketSizeHz);
      mMaxFreqIndex = (int)Math.ceil(FREQ_MAX / mBucketSizeHz);
    }
  }

  // The audio file being transformed.
  private final WavFile mWaveFile;

  // Parameters of the audio.
  private final AudioDescriptor mAudioDescriptor;

  // Core objects for doing F0 estimation
  private final Transformer mTransformer;
  private final Whitener mWhitener;
  private final KlapuriWeightCalculator mWeightCalculator;

  // UI objects for visualisation
  private final SpectogramWindow mSpecWindow;
  private final WeightWindow mWeightWindow;

  /**
   * Run the multiple F0 estimator.
   *
   * @param xiArgs - First arg (mandatory) is file to transform.
   * @throws Exception if anything goes wrong.
   */
  public static void main(String[] xiArgs) throws Exception
  {
    final F0Estimator lEstimator = new F0Estimator(xiArgs[0]);
    lEstimator.processFile();
  }

  /**
   *
   * @param xiFilename
   * @throws WavFileException
   * @throws IOException
   */
  public F0Estimator(String xiFilename) throws WavFileException, IOException
  {
    mWaveFile = WavFile.openWavFile(new File(xiFilename));
    mAudioDescriptor = new AudioDescriptor((int)mWaveFile.getSampleRate(),
                                           (int)(mWaveFile.getSampleRate() /
                                           AudioDescriptor.FRAME_SIZE));

    mTransformer = new Transformer();
    mWhitener = new Whitener(mAudioDescriptor);
    mWeightCalculator = new KlapuriWeightCalculator(mAudioDescriptor);

    mSpecWindow = new SpectogramWindow();
    mWeightWindow = new WeightWindow();
  }

  /**
   * Process the audio file.
   */
  public void processFile()
  {
    try
    {
      final long lStartTime = System.currentTimeMillis();

      // Read the wave file
      mWaveFile.display();

      // !! ARR ... Check that wavFile.getNumChannels() == 1;
      final double[] lWholeFile = new double[(int)mWaveFile.getNumFrames()];
      mWaveFile.readFrames(lWholeFile, (int)mWaveFile.getNumFrames());

      // Step through the file, looking at overlapping ~50ms slices.
      int lCount = 0;
      for (int lOffset = 0;
           (lOffset + AudioDescriptor.FRAME_SIZE) < mWaveFile.getNumFrames();
           lOffset += (AudioDescriptor.FRAME_SIZE / 8))
      {
        // Get a slice of the data
        final double[] lBuffer = Arrays.copyOfRange(lWholeFile,
                                                    lOffset,
                                                    lOffset + AudioDescriptor.FRAME_SIZE);

        // Perform a Hamming-windowed FFT.
        final Complex[] lFreq = mTransformer.transform(lBuffer);

        // Whiten the data and add to the UI.
        final double[] lWhitened = mWhitener.whiten(lFreq);
        mSpecWindow.addSamples(lWhitened);

        // Calculate the weights.
        if (++lCount == 100)
        {
          final double[][] lWeights = mWeightCalculator.calculateWeights(
                                                                    lWhitened);
          mWeightWindow.addWeights(lWeights);
        }
      }

      // Close the wavFile
      mWaveFile.close();

      final long lEndTime = System.currentTimeMillis();
      System.out.println("Took: " + (lEndTime - lStartTime) + "ms to " +
                         "transform " + ((mWaveFile.getNumFrames() * 1000) / mWaveFile.getSampleRate()) +
                         "ms of audio");
    }
    catch (final Exception e)
    {
      System.err.println(e);
    }
  }
}
