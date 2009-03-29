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

import ij.ImagePlus;
import ij.io.Opener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix3D;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Benchmark for Parallel Spectral Deconvolution 3D
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class Benchmark_3D {

	private static final String path = "/home/pwendyk/images/spectral/";

	private static final String blur_image = "head-blur.tif";

	private static final String psf_image = "head-psf.tif";

	private static final DoubleMatrix3D doubleStencil = new DenseDoubleMatrix3D(3, 3, 3).assign(new double[] { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, -6, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 });

	private static final FloatMatrix3D floatStencil = new DenseFloatMatrix3D(3, 3, 3).assign(new float[] { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, -6, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 });

	private static final ResizingType resizing = ResizingType.NONE;

	private static final OutputType output = OutputType.FLOAT;

	private static final int NITER = 20;
	
	private static final int threshold = 0;

	private static final double double_alpha_deblur = 0.01;

	private static final double double_alpha_update = 0.02;

	private static final float float_alpha_deblur = 0.01f;

	private static final float float_alpha_update = 0.02f;

	private static final String format = "%.1f";

	public static void benchmarkDoubleTSVD_Periodic_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking DoubleTSVD_Periodic_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleTsvdFFT_3D tsvd = new DoubleTsvdFFT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new DoubleTsvdFFT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(double_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(double_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleTSVD_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoubleTSVD_Reflexive_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking DoubleTSVD_Reflexive_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleTsvdDCT_3D tsvd = new DoubleTsvdDCT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new DoubleTsvdDCT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(double_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(double_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleTSVD_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoubleTikhonov_Periodic_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking DoubleTikhonov_Periodic_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleTikFFT_3D tsvd = new DoubleTikFFT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new DoubleTikFFT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(double_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(double_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoubleTikhonov_Reflexive_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking DoubleTikhonov_Reflexive_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleTikDCT_3D tsvd = new DoubleTikDCT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new DoubleTikDCT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(double_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(double_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoubleGTikhonov_Periodic_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking DoubleGTikhonov_Periodic_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleGTikFFT_3D tsvd = new DoubleGTikFFT_3D(blurImage, psfImage, doubleStencil, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new DoubleGTikFFT_3D(blurImage, psfImage, doubleStencil, resizing, output, false);
			imX = tsvd.deblur(double_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(double_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleGTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoubleGTikhonov_Reflexive_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking DoubleGTikhonov_Reflexive_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleGTikDCT_3D tsvd = new DoubleGTikDCT_3D(blurImage, psfImage, doubleStencil, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new DoubleGTikDCT_3D(blurImage, psfImage, doubleStencil, resizing, output, false);
			imX = tsvd.deblur(double_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(double_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleGTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatTSVD_Periodic_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking FloatTSVD_Periodic_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatTsvdFFT_3D tsvd = new FloatTsvdFFT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new FloatTsvdFFT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(float_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(float_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatTSVD_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatTSVD_Reflexive_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking FloatTSVD_Reflexive_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatTsvdDCT_3D tsvd = new FloatTsvdDCT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new FloatTsvdDCT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(float_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(float_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatTSVD_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatTikhonov_Periodic_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking FloatTikhonov_Periodic_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatTikFFT_3D tsvd = new FloatTikFFT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new FloatTikFFT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(float_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(float_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatTikhonov_Reflexive_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking FloatTikhonov_Reflexive_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatTikDCT_3D tsvd = new FloatTikDCT_3D(blurImage, psfImage, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new FloatTikDCT_3D(blurImage, psfImage, resizing, output, false);
			imX = tsvd.deblur(float_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(float_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatGTikhonov_Periodic_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking FloatGTikhonov_Periodic_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatGTikFFT_3D tsvd = new FloatGTikFFT_3D(blurImage, psfImage, floatStencil, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new FloatGTikFFT_3D(blurImage, psfImage, floatStencil, resizing, output, false);
			imX = tsvd.deblur(float_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(float_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatGTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatGTikhonov_Reflexive_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus psfImage = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_alpha = 0;
		double av_time_update = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_alpha = 0;
		long elapsedTime_update = 0;
		System.out.println("Benchmarking FloatGTikhonov_Reflexive_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatGTikDCT_3D tsvd = new FloatGTikDCT_3D(blurImage, psfImage, floatStencil, resizing, output, false);
			ImagePlus imX = tsvd.deblur(threshold);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_alpha = System.nanoTime();
			tsvd = new FloatGTikDCT_3D(blurImage, psfImage, floatStencil, resizing, output, false);
			imX = tsvd.deblur(float_alpha_deblur, threshold);
			elapsedTime_deblur_alpha = System.nanoTime() - elapsedTime_deblur_alpha;
			av_time_deblur_alpha = av_time_deblur_alpha + elapsedTime_deblur_alpha;

			elapsedTime_update = System.nanoTime();
			tsvd.update(float_alpha_update, threshold, imX);
			elapsedTime_update = System.nanoTime() - elapsedTime_update;
			av_time_update = av_time_update + elapsedTime_update;

			imX = null;
			tsvd = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (deblur(alpha)): " + String.format(format, av_time_deblur_alpha / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatGTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_alpha / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
	}

	public static void writeResultsToFile(String filename, double time_deblur, double time_deblur_alpha, double time_update) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			out.write(new Date().toString());
			out.newLine();
			out.write("Number of processors: " + ConcurrencyUtils.getNumberOfProcessors());
			out.newLine();
			out.write("deblur() time=");
			out.write(String.format(format, time_deblur));
			out.write(" seconds");
			out.newLine();
			out.write("deblur(alpha) time=");
			out.write(String.format(format, time_deblur_alpha));
			out.write(" seconds");
			out.newLine();
			out.write("update() time=");
			out.write(String.format(format, time_update));
			out.write(" seconds");
			out.newLine();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
//		benchmarkDoubleTSVD_Periodic_3D(1);
//		System.gc();
//		benchmarkDoubleTSVD_Periodic_3D(2);
//		System.gc();
//		benchmarkDoubleTSVD_Periodic_3D(4);
//		System.gc();
//		benchmarkDoubleTSVD_Periodic_3D(8);
//		System.gc();
//		benchmarkDoubleTSVD_Reflexive_3D(1);
//		System.gc();
//		benchmarkDoubleTSVD_Reflexive_3D(2);
//		System.gc();
//		benchmarkDoubleTSVD_Reflexive_3D(4);
//		System.gc();
//		benchmarkDoubleTSVD_Reflexive_3D(8);
//		System.gc();
//		benchmarkDoubleTikhonov_Periodic_3D(1);
//		System.gc();
//		benchmarkDoubleTikhonov_Periodic_3D(2);
//		System.gc();
//		benchmarkDoubleTikhonov_Periodic_3D(4);
//		System.gc();
//		benchmarkDoubleTikhonov_Periodic_3D(8);
//		System.gc();
//		benchmarkDoubleTikhonov_Reflexive_3D(1);
//		System.gc();
//		benchmarkDoubleTikhonov_Reflexive_3D(2);
//		System.gc();
//		benchmarkDoubleTikhonov_Reflexive_3D(4);
//		System.gc();
//		benchmarkDoubleTikhonov_Reflexive_3D(8);
//		System.gc();
//		benchmarkDoubleGTikhonov_Periodic_3D(1);
//		System.gc();
//		benchmarkDoubleGTikhonov_Periodic_3D(2);
//		System.gc();
//		benchmarkDoubleGTikhonov_Periodic_3D(4);
//		System.gc();
//		benchmarkDoubleGTikhonov_Periodic_3D(8);
//		System.gc();
//		benchmarkDoubleGTikhonov_Reflexive_3D(1);
//		System.gc();
//		benchmarkDoubleGTikhonov_Reflexive_3D(2);
//		System.gc();
//		benchmarkDoubleGTikhonov_Reflexive_3D(4);
//		System.gc();
//		benchmarkDoubleGTikhonov_Reflexive_3D(8);
//		System.gc();

		benchmarkFloatTSVD_Periodic_3D(1);
		System.gc();
		benchmarkFloatTSVD_Periodic_3D(2);
		System.gc();
		benchmarkFloatTSVD_Periodic_3D(4);
		System.gc();
		benchmarkFloatTSVD_Periodic_3D(8);
		System.gc();
		benchmarkFloatTSVD_Reflexive_3D(1);
		System.gc();
		benchmarkFloatTSVD_Reflexive_3D(2);
		System.gc();
		benchmarkFloatTSVD_Reflexive_3D(4);
		System.gc();
		benchmarkFloatTSVD_Reflexive_3D(8);
		System.gc();
		benchmarkFloatTikhonov_Periodic_3D(1);
		System.gc();
		benchmarkFloatTikhonov_Periodic_3D(2);
		System.gc();
		benchmarkFloatTikhonov_Periodic_3D(4);
		System.gc();
		benchmarkFloatTikhonov_Periodic_3D(8);
		System.gc();
		benchmarkFloatTikhonov_Reflexive_3D(1);
		System.gc();
		benchmarkFloatTikhonov_Reflexive_3D(2);
		System.gc();
		benchmarkFloatTikhonov_Reflexive_3D(4);
		System.gc();
		benchmarkFloatTikhonov_Reflexive_3D(8);
		System.gc();
		benchmarkFloatGTikhonov_Periodic_3D(1);
		System.gc();
		benchmarkFloatGTikhonov_Periodic_3D(2);
		System.gc();
		benchmarkFloatGTikhonov_Periodic_3D(4);
		System.gc();
		benchmarkFloatGTikhonov_Periodic_3D(8);
		System.gc();
		benchmarkFloatGTikhonov_Reflexive_3D(1);
		System.gc();
		benchmarkFloatGTikhonov_Reflexive_3D(2);
		System.gc();
		benchmarkFloatGTikhonov_Reflexive_3D(4);
		System.gc();
		benchmarkFloatGTikhonov_Reflexive_3D(8);
		System.gc();

		System.exit(0);

	}
}
