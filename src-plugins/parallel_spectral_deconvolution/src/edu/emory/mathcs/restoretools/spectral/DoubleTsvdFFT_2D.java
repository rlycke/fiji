/*
 *  Copyright 2008 Piotr Wendykier
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.emory.mathcs.restoretools.spectral;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import cern.colt.function.tint.IntComparator;
import cern.colt.matrix.AbstractMatrix1D;
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tdcomplex.DComplexMatrix1D;
import cern.colt.matrix.tdcomplex.DComplexMatrix2D;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix1D;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DoubleSorting;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdcomplex.DComplexFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Truncated SVD image deblurring 2D using the FFT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleTsvdFFT_2D {

	private AbstractMatrix2D B;

	private AbstractMatrix2D PSF;

	private AbstractMatrix2D S;

	private java.awt.image.ColorModel cmY;

	private int bColsPad;

	private int bRowsPad;

	private int bCols;

	private int bRows;

	private int bColsOff;

	private int bRowsOff;

	private int[] psfCenter;

	private boolean isPadded = false;

	private OutputType output;

	/**
	 * Constructs new DoubleTsvdFFT_2D.
	 * 
	 * @param imB
	 *            blurred image
	 * @param imPSF
	 *            point spread function
	 * @param resizing
	 *            type of resizing
	 * @param output
	 *            type of an output
	 * @param showPadded
	 *            if true then padded image is displayed
	 */
	public DoubleTsvdFFT_2D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
		ImageProcessor ipB = imB.getProcessor();
		ImageProcessor ipPSF = imPSF.getProcessor();
		if (output == OutputType.SAME_AS_SOURCE) {
			if (ipB instanceof ByteProcessor) {
				this.output = OutputType.BYTE;
			} else if (ipB instanceof ShortProcessor) {
				this.output = OutputType.SHORT;
			} else if (ipB instanceof FloatProcessor) {
				this.output = OutputType.FLOAT;
			} else {
				throw new IllegalArgumentException("Unsupported image type.");
			}
		} else {
			this.output = output;
		}
		cmY = ipB.getColorModel();
		int kCols = ipPSF.getWidth();
		int kRows = ipPSF.getHeight();
		bCols = ipB.getWidth();
		bRows = ipB.getHeight();
		if ((kRows > bRows) || (kCols > bCols)) {
			throw new IllegalArgumentException("The PSF image cannot be larger than the blurred image.");
		}
		IJ.showStatus("TSVD: initializing");
		if (resizing == ResizingType.NEXT_POWER_OF_TWO) {
			if (ConcurrencyUtils.isPowerOf2(bRows)) {
				bRowsPad = bRows;
			} else {
				isPadded = true;
				bRowsPad = ConcurrencyUtils.nextPow2(bRows);
			}
			if (ConcurrencyUtils.isPowerOf2(bCols)) {
				bColsPad = bCols;
			} else {
				isPadded = true;
				bColsPad = ConcurrencyUtils.nextPow2(bCols);
			}
		} else {
			bColsPad = bCols;
			bRowsPad = bRows;
		}
		B = new DenseDoubleMatrix2D(bRows, bCols);
		DoubleCommon_2D.assignPixelsToMatrix_2D((DoubleMatrix2D) B, ipB);
		if (isPadded) {
			B = DoubleCommon_2D.padPeriodic_2D((DoubleMatrix2D) B, bColsPad, bRowsPad);
			bColsOff = (bColsPad - bCols + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
		}
		PSF = new DenseDoubleMatrix2D(kRows, kCols);
		DoubleCommon_2D.assignPixelsToMatrix_2D((DoubleMatrix2D) PSF, ipPSF);
		double[] maxAndLoc = ((DoubleMatrix2D) PSF).getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2] };
		((DoubleMatrix2D) PSF).normalize();
		if ((kCols != bColsPad) || (kRows != bRowsPad)) {
			PSF = DoubleCommon_2D.padZero_2D((DoubleMatrix2D) PSF, bColsPad, bRowsPad);
		}
		psfCenter[0] += (bRowsPad - kRows + 1) / 2;
		psfCenter[1] += (bColsPad - kCols + 1) / 2;
		if (showPadded && isPadded) {
			FloatProcessor ipTemp = new FloatProcessor(bColsPad, bRowsPad);
			DoubleCommon_2D.assignPixelsToProcessor(ipTemp, (DoubleMatrix2D) B, cmY);
			ImagePlus imTemp = new ImagePlus("", ipTemp);
			imTemp.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + " (padded)"));
			imTemp.show();
			imTemp.setRoi(bColsOff, bRowsOff, bCols, bRows);
		}
	}

	/**
	 * Returns deblurred image. Uses Generalized Cross-Validation (GCV) to
	 * compute regularization parameter.
	 * 
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @return deblurred image
	 */
	public ImagePlus deblur(double threshold) {
		IJ.showStatus("TSVD: deblurring");
		S = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) PSF, psfCenter);
		S = ((DoubleMatrix2D) S).getFft2();
		B = ((DoubleMatrix2D) B).getFft2();
		IJ.showStatus("TSVD: computing regularization parameter");
		double alpha = gcvTsvdFFT_2D((DComplexMatrix2D) S, (DComplexMatrix2D) B);
		IJ.showStatus("TSVD: deblurring");
		DComplexMatrix2D Sfilt = DoubleCommon_2D.createFilter_2D((DComplexMatrix2D) S, alpha);
		PSF = ((DComplexMatrix2D) B).copy();
		((DComplexMatrix2D) PSF).assign(Sfilt, DComplexFunctions.mult);
		((DComplexMatrix2D) PSF).ifft2(true);
		IJ.showStatus("TSVD: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) PSF, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) PSF, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) PSF, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) PSF, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", ip);
		DoubleCommon_2D.convertImage(imX, output);
		imX.setProperty("alpha", alpha);
		return imX;
	}

	/**
	 * Returns deblurred image.
	 * 
	 * @param alpha
	 *            regularization parameter
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @return deblurred image
	 */
	public ImagePlus deblur(double alpha, double threshold) {
		IJ.showStatus("TSVD: deblurring");
		S = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) PSF, psfCenter);
		S = ((DoubleMatrix2D) S).getFft2();
		B = ((DoubleMatrix2D) B).getFft2();
		DComplexMatrix2D Sfilt = DoubleCommon_2D.createFilter_2D((DComplexMatrix2D) S, alpha);
		PSF = ((DComplexMatrix2D) B).copy();
		((DComplexMatrix2D) PSF).assign(Sfilt, DComplexFunctions.mult);
		((DComplexMatrix2D) PSF).ifft2(true);
		IJ.showStatus("TSVD: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) PSF, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) PSF, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) PSF, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) PSF, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", ip);
		DoubleCommon_2D.convertImage(imX, output);
		return imX;
	}

	/**
	 * Updates deblurred image <code>imX</code> with new regularization
	 * parameter <code>alpha</code>.
	 * 
	 * @param alpha
	 *            regularization parameter
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @param imX
	 *            deblurred image
	 */
	public void update(double alpha, double threshold, ImagePlus imX) {
		IJ.showStatus("TSVD: updating");
		DComplexMatrix2D Sfilt = DoubleCommon_2D.createFilter_2D((DComplexMatrix2D) S, alpha);
		PSF = ((DComplexMatrix2D) B).copy();
		((DComplexMatrix2D) PSF).assign(Sfilt, DComplexFunctions.mult);
		((DComplexMatrix2D) PSF).ifft2(true);
		IJ.showStatus("TSVD: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) PSF, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) PSF, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) PSF, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) PSF, cmY, threshold);
			}
		}
		imX.setProcessor(imX.getTitle(), ip);
		DoubleCommon_2D.convertImage(imX, output);
	}

	private double gcvTsvdFFT_2D(DComplexMatrix2D S, DComplexMatrix2D Bhat) {
		int length = S.rows() * S.columns();
		AbstractMatrix1D s = new DenseDComplexMatrix1D(length);
		AbstractMatrix1D bhat = new DenseDComplexMatrix1D(length);
		System.arraycopy(((DenseDComplexMatrix2D) S).elements(), 0, ((DenseDComplexMatrix1D) s).elements(), 0, 2 * length);
		System.arraycopy(((DenseDComplexMatrix2D) Bhat).elements(), 0, ((DenseDComplexMatrix1D) bhat).elements(), 0, 2 * length);
		s = ((DComplexMatrix1D) s).assign(DComplexFunctions.abs).getRealPart();
		bhat = ((DComplexMatrix1D) bhat).assign(DComplexFunctions.abs).getRealPart();
		final double[] svalues = (double[]) ((DenseDoubleMatrix1D) s).elements();
		IntComparator compDec = new IntComparator() {
			public int compare(int a, int b) {
				if (svalues[a] != svalues[a] || svalues[b] != svalues[b])
					return compareNaN(svalues[a], svalues[b]); // swap NaNs to
				// the end
				return svalues[a] < svalues[b] ? 1 : (svalues[a] == svalues[b] ? 0 : -1);
			}
		};
		int[] indices = DoubleSorting.quickSort.sortIndex((DoubleMatrix1D) s, compDec);
		s = ((DoubleMatrix1D) s).viewSelection(indices);
		bhat = ((DoubleMatrix1D) bhat).viewSelection(indices);
		int n = s.size();
		double[] rho = new double[n - 1];
		rho[n - 2] = ((DoubleMatrix1D) bhat).getQuick(n - 1) * ((DoubleMatrix1D) bhat).getQuick(n - 1);
		DoubleMatrix1D G = new DenseDoubleMatrix1D(n - 1);
		double[] elemsG = (double[]) G.elements();
		elemsG[n - 2] = rho[n - 2];
		double bhatel, temp1;
		for (int k = n - 2; k > 0; k--) {
			bhatel = ((DoubleMatrix1D) bhat).getQuick(k);
			rho[k - 1] = rho[k] + bhatel * bhatel;
			temp1 = n - k;
			temp1 = temp1 * temp1;
			elemsG[k - 1] = rho[k - 1] / temp1;
		}
		for (int k = 0; k < n - 3; k++) {
			if (((DoubleMatrix1D) s).getQuick(k) == ((DoubleMatrix1D) s).getQuick(k + 1)) {
				elemsG[k] = Double.POSITIVE_INFINITY;
			}
		}
		return ((DoubleMatrix1D) s).getQuick((int) G.getMinLocation()[1]);
	}

	private final int compareNaN(double a, double b) {
		if (a != a) {
			if (b != b)
				return 0; // NaN equals NaN
			else
				return 1; // e.g. NaN > 5
		}
		return -1; // e.g. 5 < NaN
	}

}
