package core;

/**
 * Implementation of an in-place Hamming window.
 */
public class HammingWindow
{
  private final double[] mCoefficients;

  /**
   * Create a Hamming window of the specified size.
   *
   * @param xiSize - size (in samples) of the requried window.
   */
  public HammingWindow(int xiSize)
  {
    mCoefficients = new double[xiSize];
    for (int lii = 0; lii < mCoefficients.length; lii++)
    {
      mCoefficients[lii] =
                    0.54 - (0.46 * Math.cos((2 * Math.PI * lii) / (xiSize - 1)));
    }
  }

  /**
   * Apply the window to the specified data.
   *
   * Note that the data length must equal the window size used when
   * constructing this window.
   *
   * @param xiData - the data.
   */
  public void apply(double[] xiData)
  {
    for (int lii = 0; lii < mCoefficients.length; lii++)
    {
      xiData[lii] *= mCoefficients[lii];
    }
  }
}
