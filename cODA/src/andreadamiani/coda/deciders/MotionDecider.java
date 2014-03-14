package andreadamiani.coda.deciders;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.data.FileHandler;
import Jama.Matrix;
import andreadamiani.coda.Application;
import andreadamiani.coda.LogProvider;
import andreadamiani.coda.R;
import andreadamiani.coda.observers.accelerometer.AccelerometerLogger;
import andreadamiani.coda.observers.gyroscope.GyroscopeLogger;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

public class MotionDecider extends IntentService {

	private static final String DEBUG_TAG = "[cODA] MOTION DECIDER";
	private Dataset trainingData = null;
	private final Classifier knn;

	public MotionDecider() {
		super(DEBUG_TAG);
		try {
			File trainingDataFile = new File(URI.create("file:///android_asset/motion_trainingdata.data"));
			trainingData = FileHandler.loadDataset(trainingDataFile, 4, ",");
		} catch (IOException e) {
			Log.d(DEBUG_TAG, "Training dataset not available - no classification will be available.");
		}
		if(trainingData != null){
			knn = new KNearestNeighbors(getResources().getInteger(
					R.integer.motion_knn_k));
			knn.buildClassifier(trainingData);
		} else {
			knn = null;
		}
	}

	@Override
	protected void onHandleIntent(Intent arg0) {

		if(knn==null){
			Log.d(DEBUG_TAG, "Classifier not available - aborting.");
			return;
		}
		
		SparseArray<float[]> accelerometerData = new SparseArray<float[]>();
		int[] accelerometerInterval = { 0, 0 };
		SparseArray<float[]> gyroscopeData = new SparseArray<float[]>();
		int[] gyroscopeInterval = { 0, 0 };

		Uri uri = Uri.parse(LogProvider.CONTENT_URI + "/"
				+ AccelerometerLogger.NAME);
		String[] projection = { LogProvider.TIMESTAMP, LogProvider.LOG_VALUE };
		String selection = "(" + LogProvider.TIMESTAMP + " > ?)";
		Long relevantPeriod = System.currentTimeMillis()
				- getResources().getInteger(R.integer.motion_relevant_period);
		String[] selectionArgs = { Long.toString(relevantPeriod) };
		String sortOrder = LogProvider.TIMESTAMP + " ASC";

		Cursor c = getContentResolver().query(uri, projection, selection,
				selectionArgs, sortOrder);

		while (c.moveToNext()) {
			float[] entry = AccelerometerLogger.parseValue(c.getString(c
					.getColumnIndex(LogProvider.LOG_VALUE)));
			int key = (int) (c.getLong(c.getColumnIndex(LogProvider.TIMESTAMP)) - relevantPeriod);
			accelerometerData.put(key, entry);
			if (c.isFirst()) {
				accelerometerInterval[0] = key;
			} else if (c.isLast()) {
				accelerometerInterval[1] = key;
			}
		}

		c.close();

		uri = Uri.parse(LogProvider.CONTENT_URI + "/" + GyroscopeLogger.NAME);

		c = getContentResolver().query(uri, projection, selection,
				selectionArgs, sortOrder);

		while (c.moveToNext()) {
			float[] entry = GyroscopeLogger.parseValue(c.getString(c
					.getColumnIndex(LogProvider.LOG_VALUE)));
			int key = (int) (c.getLong(c.getColumnIndex(LogProvider.TIMESTAMP)) - relevantPeriod);
			gyroscopeData.put(key, entry);
			if (c.isFirst()) {
				gyroscopeInterval[0] = key;
			} else if (c.isLast()) {
				gyroscopeInterval[1] = key;
			}
		}

		c.close();

		int[] interval = {
				accelerometerInterval[0] >= gyroscopeInterval[0] ? accelerometerInterval[0]
						: gyroscopeInterval[0],
				accelerometerInterval[1] < gyroscopeInterval[1] ? accelerometerInterval[1]
						: gyroscopeInterval[1] };

		int resolution = getResources().getInteger(R.integer.motion_rate);
		int windowDuration = getResources().getInteger(R.integer.motion_window);
		int windowSize = (windowDuration / resolution);

		ArrayList<float[][]> imuValues = new ArrayList<float[][]>();
		for (int i = interval[0]; i <= interval[1]; i += resolution) {
			float[][] entry = { getNearestValid(accelerometerData, i),
					getNearestValid(gyroscopeData, i) };
			imuValues.add(entry);
		}

		accelerometerData = null;
		gyroscopeData = null;

		Matrix dataMatrix = new Matrix(WindowFeatures.featuresCount,
				((imuValues.size() / windowSize) * 2) - 1);
		for (int start = 0, j = 0; start < imuValues.size() - windowSize; start += windowSize / 2) {
			WindowFeatures feats = new WindowFeatures(imuValues.subList(start,
					start + windowSize));
			assert (j < dataMatrix.getColumnDimension());
			for (int i = 0; i < dataMatrix.getRowDimension(); i++) {
				dataMatrix.set(i, j, feats.getFeature(i));
			}
			j++;
		}

		imuValues = null;

		Matrix featuresMeans = new Matrix(dataMatrix.getRowDimension(), 1);
		for (int i = 0; i < dataMatrix.getRowDimension(); i++) {
			float accumulator = 0;
			int j;
			for (j = 0; j < dataMatrix.getColumnDimension(); j++) {
				accumulator += dataMatrix.get(i, j);
			}
			featuresMeans.set(i, 0, accumulator / j);
		}

		for (int j = 0; j < dataMatrix.getColumnDimension(); j++) {
			Matrix column = dataMatrix.getMatrix(0,
					dataMatrix.getRowDimension() - 1, j, j);
			column.minusEquals(featuresMeans);
			dataMatrix.setMatrix(0, dataMatrix.getRowDimension() - 1, j, j,
					column);
		}

		featuresMeans = null;

		Matrix principalComponents = dataMatrix.svd().getV().transpose();

		double[][] principalComponentsArray = principalComponents.getArray();
		principalComponents = null;
		dataMatrix = null;

		int relevant_components = getResources().getInteger(
				R.integer.motion_relevant_components);
		int isRunning = 0;
		for (int i = 0; i < (principalComponentsArray.length < relevant_components ? principalComponentsArray.length
				: relevant_components); i++) {
			if (trainingData.classIndex(knn.classify(new DenseInstance(
					principalComponentsArray[i]))) == getResources().getInteger(R.integer.motion_running_class_id)) {
				isRunning++;
			} else {
				isRunning--;
			}
		}

		if(isRunning > getResources().getInteger(R.integer.motion_treshold)){
			Application.getInstance().sendBroadcast(
					new Intent(Application.formatIntentAction("RUNNING")));
		}
	}

	private class WindowFeatures {

		public static final int featuresCount = 116;

		public final float[] features = new float[featuresCount];
		public static final int MEAN_ACC_X = 0;
		public static final int MEAN_ACC_Y = 1;
		public static final int MEAN_ACC_Z = 2;
		public static final int MEAN_ACC_ALL = 3;
		public static final int MEAN_ROT_X = 4;
		public static final int MEAN_ROT_Y = 5;
		public static final int MEAN_ROT_Z = 6;
		public static final int MEAN_ROT_ALL = 7;
		public static final int SD_ACC_X = 8;
		public static final int SD_ACC_Y = 9;
		public static final int SD_ACC_Z = 10;
		public static final int SD_ACC_ALL = 11;
		public static final int SD_ROT_X = 12;
		public static final int SD_ROT_Y = 13;
		public static final int SD_ROT_Z = 14;
		public static final int SD_ROT_ALL = 15;
		public static final int VAR_ACC_X = 16;
		public static final int VAR_ACC_Y = 17;
		public static final int VAR_ACC_Z = 18;
		public static final int VAR_ACC_ALL = 19;
		public static final int VAR_ROT_X = 20;
		public static final int VAR_ROT_Y = 21;
		public static final int VAR_ROT_Z = 22;
		public static final int VAR_ROT_ALL = 23;
		public static final int RMS_ACC_X = 24;
		public static final int RMS_ACC_Y = 25;
		public static final int RMS_ACC_Z = 26;
		public static final int RMS_ACC_ALL = 37;
		public static final int RMS_ROT_X = 28;
		public static final int RMS_ROT_Y = 29;
		public static final int RMS_ROT_Z = 30;
		public static final int RMS_ROT_ALL = 31;
		public static final int MAD_ACC_X = 32;
		public static final int MAD_ACC_Y = 33;
		public static final int MAD_ACC_Z = 34;
		public static final int MAD_ACC_ALL = 35;
		public static final int MAD_ROT_X = 36;
		public static final int MAD_ROT_Y = 37;
		public static final int MAD_ROT_Z = 38;
		public static final int MAD_ROT_ALL = 39;
		public static final int IQR_ACC_X = 40;
		public static final int IQR_ACC_Y = 41;
		public static final int IQR_ACC_Z = 42;
		public static final int IQR_ACC_ALL = 43;
		public static final int IQR_ROT_X = 44;
		public static final int IQR_ROT_Y = 45;
		public static final int IQR_ROT_Z = 46;
		public static final int IQR_ROT_ALL = 47;
		public static final int P10_ACC_X = 48;
		public static final int P10_ACC_Y = 49;
		public static final int P10_ACC_Z = 50;
		public static final int P10_ACC_ALL = 51;
		public static final int P10_ROT_X = 52;
		public static final int P10_ROT_Y = 53;
		public static final int P10_ROT_Z = 54;
		public static final int P10_ROT_ALL = 55;
		public static final int P25_ACC_X = 56;
		public static final int P25_ACC_Y = 57;
		public static final int P25_ACC_Z = 58;
		public static final int P25_ACC_ALL = 59;
		public static final int P25_ROT_X = 60;
		public static final int P25_ROT_Y = 61;
		public static final int P25_ROT_Z = 62;
		public static final int P25_ROT_ALL = 63;
		public static final int P50_ACC_X = 64;
		public static final int P50_ACC_Y = 65;
		public static final int P50_ACC_Z = 66;
		public static final int P50_ACC_ALL = 67;
		public static final int P50_ROT_X = 68;
		public static final int P50_ROT_Y = 69;
		public static final int P50_ROT_Z = 70;
		public static final int P50_ROT_ALL = 71;
		public static final int P75_ACC_X = 72;
		public static final int P75_ACC_Y = 73;
		public static final int P75_ACC_Z = 74;
		public static final int P75_ACC_ALL = 75;
		public static final int P75_ROT_X = 76;
		public static final int P75_ROT_Y = 77;
		public static final int P75_ROT_Z = 78;
		public static final int P75_ROT_ALL = 79;
		public static final int P90_ACC_X = 80;
		public static final int P90_ACC_Y = 81;
		public static final int P90_ACC_Z = 82;
		public static final int P90_ACC_ALL = 83;
		public static final int P90_ROT_X = 84;
		public static final int P90_ROT_Y = 85;
		public static final int P90_ROT_Z = 86;
		public static final int P90_ROT_ALL = 87;
		public static final int MIN_ACC_X = 88;
		public static final int MIN_ACC_Y = 89;
		public static final int MIN_ACC_Z = 90;
		public static final int MIN_ACC_ALL = 91;
		public static final int MIN_ROT_X = 92;
		public static final int MIN_ROT_Y = 93;
		public static final int MIN_ROT_Z = 94;
		public static final int MIN_ROT_ALL = 95;
		public static final int MAX_ACC_X = 96;
		public static final int MAX_ACC_Y = 97;
		public static final int MAX_ACC_Z = 98;
		public static final int MAX_ACC_ALL = 99;
		public static final int MAX_ROT_X = 100;
		public static final int MAX_ROT_Y = 101;
		public static final int MAX_ROT_Z = 102;
		public static final int MAX_ROT_ALL = 103;
		public static final int ZCR_ACC_X = 104;
		public static final int ZCR_ACC_Y = 105;
		public static final int ZCR_ACC_Z = 106;
		public static final int ZCR_ROT_X = 107;
		public static final int ZCR_ROT_Y = 108;
		public static final int ZCR_ROT_Z = 109;
		public static final int MCR_ACC_X = 110;
		public static final int MCR_ACC_Y = 111;
		public static final int MCR_ACC_Z = 112;
		public static final int MCR_ROT_X = 113;
		public static final int MCR_ROT_Y = 114;
		public static final int MCR_ROT_Z = 115;

		public WindowFeatures(List<float[][]> entries) {
			computeMeans(entries);
			computeStandardDeviationsAndVariances(entries);
			computePercentiles(entries);
			computeCrossingRate(entries);
		}

		public float getFeature(int i) {
			return features[i];
		}

		private void computeCrossingRate(List<float[][]> entries) {
			int[][] semiplanZCR = { { 0, 0, 0 }, { 0, 0, 0 } };
			int[][] semiplanMCR = { { 0, 0, 0 }, { 0, 0, 0 } };

			features[ZCR_ACC_X] = 0;
			features[ZCR_ACC_Y] = 0;
			features[ZCR_ACC_Z] = 0;
			features[ZCR_ROT_X] = 0;
			features[ZCR_ROT_Y] = 0;
			features[ZCR_ROT_Z] = 0;
			features[MCR_ACC_X] = 0;
			features[MCR_ACC_Y] = 0;
			features[MCR_ACC_Z] = 0;
			features[MCR_ROT_X] = 0;
			features[MCR_ROT_Y] = 0;
			features[MCR_ROT_Z] = 0;

			int entriesNum = entries.size();
			for (int i = 0; i < entriesNum; i++) {
				float[][] entry = entries.get(i);
				for (int j = 0; j < 2; j++) {
					for (int k = 0; k < 3; k++) {
						int featureIndex = -1;
						int featureZCR = -1;
						int featureMCR = -1;
						switch (j) {
						case 0:
							switch (k) {
							case 0:
								featureIndex = MEAN_ACC_X;
								featureZCR = ZCR_ACC_X;
								featureMCR = MCR_ACC_X;
								break;
							case 1:
								featureIndex = MEAN_ACC_Y;
								featureZCR = ZCR_ACC_Y;
								featureMCR = MCR_ACC_Y;
								break;
							case 2:
								featureIndex = MEAN_ACC_Z;
								featureZCR = ZCR_ACC_Z;
								featureMCR = MCR_ACC_Z;
								break;
							}
							break;
						case 1:
							switch (k) {
							case 0:
								featureIndex = MEAN_ROT_X;
								featureZCR = ZCR_ROT_X;
								featureMCR = MCR_ROT_X;
								break;
							case 1:
								featureIndex = MEAN_ROT_Y;
								featureZCR = ZCR_ROT_Y;
								featureMCR = MCR_ROT_Y;
								break;
							case 2:
								featureIndex = MEAN_ROT_Z;
								featureZCR = ZCR_ROT_Z;
								featureMCR = MCR_ROT_Z;
								break;
							}
							break;
						}
						if (Math.signum(entry[j][k]) != Math
								.signum(semiplanZCR[j][k])) {
							semiplanZCR[j][k] = (int) Math.signum(entry[j][k]);
							features[featureZCR]++;
						}
						if (Math.signum(entry[j][k] - features[featureIndex]) != Math
								.signum(semiplanMCR[j][k])) {
							semiplanMCR[j][k] = (int) Math.signum(entry[j][k]
									- features[featureIndex]);
							features[featureMCR]++;
						}
					}
				}
			}

			features[ZCR_ACC_X] /= entriesNum;
			features[ZCR_ACC_Y] /= entriesNum;
			features[ZCR_ACC_Z] /= entriesNum;
			features[ZCR_ROT_X] /= entriesNum;
			features[ZCR_ROT_Y] /= entriesNum;
			features[ZCR_ROT_Z] /= entriesNum;
			features[MCR_ACC_X] /= entriesNum;
			features[MCR_ACC_Y] /= entriesNum;
			features[MCR_ACC_Z] /= entriesNum;
			features[MCR_ROT_X] /= entriesNum;
			features[MCR_ROT_Y] /= entriesNum;
			features[MCR_ROT_Z] /= entriesNum;
		}

		private void computePercentiles(List<float[][]> entries) {
			int entriesNum = entries.size();
			SortedArrayList<Float> accX = new SortedArrayList<Float>(entriesNum);
			SortedArrayList<Float> accY = new SortedArrayList<Float>(entriesNum);
			SortedArrayList<Float> accZ = new SortedArrayList<Float>(entriesNum);
			SortedArrayList<Float> rotX = new SortedArrayList<Float>(entriesNum);
			SortedArrayList<Float> rotY = new SortedArrayList<Float>(entriesNum);
			SortedArrayList<Float> rotZ = new SortedArrayList<Float>(entriesNum);
			for (int i = 0; i < entriesNum; i++) {
				float[][] entry = entries.get(i);
				accX.insertSorted(entry[0][0]);
				accY.insertSorted(entry[0][1]);
				accZ.insertSorted(entry[0][2]);
				rotX.insertSorted(entry[1][0]);
				rotY.insertSorted(entry[1][1]);
				rotZ.insertSorted(entry[1][2]);
			}

			features[P10_ACC_X] = accX.get(entriesNum / 10);
			features[P10_ACC_Y] = accY.get(entriesNum / 10);
			features[P10_ACC_Z] = accZ.get(entriesNum / 10);
			features[P10_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[P10_ACC_X], 2)
					+ Math.pow(features[P10_ACC_Y], 2)
					+ Math.pow(features[P10_ACC_Z], 2));
			features[P10_ROT_X] = rotX.get(entriesNum / 10);
			features[P10_ROT_Y] = rotY.get(entriesNum / 10);
			features[P10_ROT_Z] = rotZ.get(entriesNum / 10);
			features[P10_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[P10_ROT_X], 2)
					+ Math.pow(features[P10_ROT_Y], 2)
					+ Math.pow(features[P10_ROT_Z], 2));
			features[P25_ACC_X] = accX.get(entriesNum / 4);
			features[P25_ACC_Y] = accY.get(entriesNum / 4);
			features[P25_ACC_Z] = accZ.get(entriesNum / 4);
			features[P25_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[P25_ACC_X], 2)
					+ Math.pow(features[P25_ACC_Y], 2)
					+ Math.pow(features[P25_ACC_Z], 2));
			features[P25_ROT_X] = rotX.get(entriesNum / 4);
			features[P25_ROT_Y] = rotY.get(entriesNum / 4);
			features[P25_ROT_Z] = rotZ.get(entriesNum / 4);
			features[P25_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[P25_ROT_X], 2)
					+ Math.pow(features[P25_ROT_Y], 2)
					+ Math.pow(features[P25_ROT_Z], 2));
			features[P50_ACC_X] = accX.get(entriesNum / 2);
			features[P50_ACC_Y] = accY.get(entriesNum / 2);
			features[P50_ACC_Z] = accZ.get(entriesNum / 2);
			features[P50_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[P50_ACC_X], 2)
					+ Math.pow(features[P50_ACC_Y], 2)
					+ Math.pow(features[P50_ACC_Z], 2));
			features[P50_ROT_X] = rotX.get(entriesNum / 2);
			features[P50_ROT_Y] = rotY.get(entriesNum / 2);
			features[P50_ROT_Z] = rotZ.get(entriesNum / 2);
			features[P50_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[P50_ROT_X], 2)
					+ Math.pow(features[P50_ROT_Y], 2)
					+ Math.pow(features[P50_ROT_Z], 2));
			features[P75_ACC_X] = accX.get(3 * (entriesNum / 4));
			features[P75_ACC_Y] = accY.get(3 * (entriesNum / 4));
			features[P75_ACC_Z] = accZ.get(3 * (entriesNum / 4));
			features[P75_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[P75_ACC_X], 2)
					+ Math.pow(features[P75_ACC_Y], 2)
					+ Math.pow(features[P75_ACC_Z], 2));
			features[P75_ROT_X] = rotX.get(3 * (entriesNum / 4));
			features[P75_ROT_Y] = rotY.get(3 * (entriesNum / 4));
			features[P75_ROT_Z] = rotZ.get(3 * (entriesNum / 4));
			features[P75_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[P75_ROT_X], 2)
					+ Math.pow(features[P75_ROT_Y], 2)
					+ Math.pow(features[P75_ROT_Z], 2));
			features[P90_ACC_X] = accX.get(9 * (entriesNum / 10));
			features[P90_ACC_Y] = accY.get(9 * (entriesNum / 10));
			features[P90_ACC_Z] = accZ.get(9 * (entriesNum / 10));
			features[P90_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[P90_ACC_X], 2)
					+ Math.pow(features[P90_ACC_Y], 2)
					+ Math.pow(features[P90_ACC_Z], 2));
			features[P90_ROT_X] = rotX.get(9 * (entriesNum / 10));
			features[P90_ROT_Y] = rotY.get(9 * (entriesNum / 10));
			features[P90_ROT_Z] = rotZ.get(9 * (entriesNum / 10));
			features[P90_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[P90_ROT_X], 2)
					+ Math.pow(features[P90_ROT_Y], 2)
					+ Math.pow(features[P90_ROT_Z], 2));
			features[IQR_ACC_X] = features[P75_ACC_X] - features[P25_ACC_X];
			features[IQR_ACC_Y] = features[P75_ACC_Y] - features[P25_ACC_Y];
			features[IQR_ACC_Z] = features[P75_ACC_Z] - features[P25_ACC_Z];
			features[IQR_ACC_ALL] = features[P75_ACC_ALL]
					- features[P25_ACC_ALL];
			features[IQR_ROT_X] = features[P75_ROT_X] - features[P25_ROT_X];
			features[IQR_ROT_Y] = features[P75_ROT_Y] - features[P25_ROT_Y];
			features[IQR_ROT_Z] = features[P75_ROT_Z] - features[P25_ROT_Z];
			features[IQR_ROT_ALL] = features[P75_ROT_ALL]
					- features[P25_ROT_ALL];
			features[MIN_ACC_X] = accX.get(0);
			features[MIN_ACC_Y] = accY.get(0);
			features[MIN_ACC_Z] = accZ.get(0);
			features[MIN_ACC_ALL] = features[MIN_ACC_X] < features[MIN_ACC_Y] ? (features[MIN_ACC_X] < features[MIN_ACC_Z] ? features[MIN_ACC_X]
					: features[MIN_ACC_Z])
					: features[MIN_ACC_Y];
			features[MIN_ROT_X] = rotX.get(0);
			features[MIN_ROT_Y] = rotY.get(0);
			features[MIN_ROT_Z] = rotZ.get(0);
			features[MIN_ROT_ALL] = features[MIN_ROT_X] < features[MIN_ROT_Y] ? (features[MIN_ROT_X] < features[MIN_ROT_Z] ? features[MIN_ROT_X]
					: features[MIN_ROT_Z])
					: features[MIN_ROT_Y];
			features[MAX_ACC_X] = accX.get(0);
			features[MAX_ACC_Y] = accY.get(0);
			features[MAX_ACC_Z] = accZ.get(0);
			features[MAX_ACC_ALL] = features[MAX_ACC_X] < features[MAX_ACC_Y] ? (features[MAX_ACC_X] < features[MAX_ACC_Z] ? features[MAX_ACC_X]
					: features[MAX_ACC_Z])
					: features[MAX_ACC_Y];
			features[MAX_ROT_X] = rotX.get(0);
			features[MAX_ROT_Y] = rotY.get(0);
			features[MAX_ROT_Z] = rotZ.get(0);
			features[MAX_ROT_ALL] = features[MAX_ROT_X] > features[MAX_ROT_Y] ? (features[MAX_ROT_X] > features[MAX_ROT_Z] ? features[MAX_ROT_X]
					: features[MAX_ROT_Z])
					: features[MAX_ROT_Y];
		}

		private class SortedArrayList<E extends Comparable<? super E>> extends
				ArrayList<E> {

			private static final long serialVersionUID = -2681345619383007275L;

			public SortedArrayList(int capacity) {
				super(capacity);
			}

			public void insertSorted(E value) {
				add(value);
				for (int i = size() - 1; i > 0
						&& value.compareTo(get(i - 1)) < 0; i--)
					Collections.swap(this, i, i - 1);
			}
		}

		private void computeMeans(List<float[][]> entries) {
			float[][] accumulators = { { 0, 0, 0 }, { 0, 0, 0 } };
			float[][] squareAccumulators = { { 0, 0, 0 }, { 0, 0, 0 } };
			for (float[][] i : entries) {
				for (int j = 0; j < 2; j++) {
					for (int k = 0; k < 3; k++) {
						accumulators[j][k] += i[j][k];
						squareAccumulators[j][k] += Math.pow(i[j][k], 2);
					}
				}
			}

			int entryNum = entries.size();

			features[MEAN_ACC_X] = accumulators[0][0] / entryNum;
			features[MEAN_ACC_Y] = accumulators[0][1] / entryNum;
			features[MEAN_ACC_Z] = accumulators[0][2] / entryNum;
			features[MEAN_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[MEAN_ACC_X], 2)
					+ Math.pow(features[MEAN_ACC_Y], 2)
					+ Math.pow(features[MEAN_ACC_Z], 2));
			features[MEAN_ROT_X] = accumulators[0][0] / entryNum;
			features[MEAN_ROT_Y] = accumulators[0][1] / entryNum;
			features[MEAN_ROT_Z] = accumulators[0][2] / entryNum;
			features[MEAN_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[MEAN_ROT_X], 2)
					+ Math.pow(features[MEAN_ROT_Y], 2)
					+ Math.pow(features[MEAN_ROT_Z], 2));
			features[RMS_ACC_X] = (float) Math.sqrt(squareAccumulators[0][0]
					/ entryNum);
			features[RMS_ACC_Y] = (float) Math.sqrt(squareAccumulators[0][1]
					/ entryNum);
			features[RMS_ACC_Z] = (float) Math.sqrt(squareAccumulators[0][2]
					/ entryNum);
			features[RMS_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[RMS_ACC_X], 2)
					+ Math.pow(features[RMS_ACC_Y], 2)
					+ Math.pow(features[RMS_ACC_Z], 2));
			features[RMS_ROT_X] = (float) Math.sqrt(squareAccumulators[0][0]
					/ entryNum);
			features[RMS_ROT_Y] = (float) Math.sqrt(squareAccumulators[0][1]
					/ entryNum);
			features[RMS_ROT_Z] = (float) Math.sqrt(squareAccumulators[0][2]
					/ entryNum);
			features[RMS_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[RMS_ROT_X], 2)
					+ Math.pow(features[RMS_ROT_Y], 2)
					+ Math.pow(features[RMS_ROT_Z], 2));
		}

		private void computeStandardDeviationsAndVariances(
				List<float[][]> entries) {
			float[][] accumulators = { { 0, 0, 0 }, { 0, 0, 0 } };
			float[][] squareAccumulators = { { 0, 0, 0 }, { 0, 0, 0 } };
			for (float[][] i : entries) {
				for (int j = 0; j < 2; j++) {
					for (int k = 0; k < 3; k++) {
						int featureIndex = -1;
						switch (j) {
						case 0:
							switch (k) {
							case 0:
								featureIndex = MEAN_ACC_X;
								break;
							case 1:
								featureIndex = MEAN_ACC_Y;
								break;
							case 2:
								featureIndex = MEAN_ACC_Z;
								break;
							}
							break;
						case 1:
							switch (k) {
							case 0:
								featureIndex = MEAN_ROT_X;
								break;
							case 1:
								featureIndex = MEAN_ROT_Y;
								break;
							case 2:
								featureIndex = MEAN_ROT_Z;
								break;
							}
							break;
						}
						accumulators[j][k] += i[j][k] - features[featureIndex];
						squareAccumulators[j][k] += Math.pow(i[j][k]
								- features[featureIndex], 2);
					}
				}
			}

			int entryNum = entries.size();

			features[SD_ACC_X] = (float) Math.sqrt(squareAccumulators[0][0]
					/ entryNum);
			features[SD_ACC_Y] = (float) Math.sqrt(squareAccumulators[0][1]
					/ entryNum);
			features[SD_ACC_Z] = (float) Math.sqrt(squareAccumulators[0][2]
					/ entryNum);
			features[SD_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[SD_ACC_X], 2)
					+ Math.pow(features[SD_ACC_Y], 2)
					+ Math.pow(features[SD_ACC_Z], 2));
			features[SD_ROT_X] = (float) Math.sqrt(squareAccumulators[0][0]
					/ entryNum);
			features[SD_ROT_Y] = (float) Math.sqrt(squareAccumulators[0][1]
					/ entryNum);
			features[SD_ROT_Z] = (float) Math.sqrt(squareAccumulators[0][2]
					/ entryNum);
			features[SD_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[SD_ROT_X], 2)
					+ Math.pow(features[SD_ROT_Y], 2)
					+ Math.pow(features[SD_ROT_Z], 2));
			features[VAR_ACC_X] = squareAccumulators[0][0] / (entryNum - 1);
			features[VAR_ACC_Y] = squareAccumulators[0][1] / (entryNum - 1);
			features[VAR_ACC_Z] = squareAccumulators[0][2] / (entryNum - 1);
			features[VAR_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[VAR_ACC_X], 2)
					+ Math.pow(features[VAR_ACC_Y], 2)
					+ Math.pow(features[VAR_ACC_Z], 2));
			features[VAR_ROT_X] = squareAccumulators[0][0] / (entryNum - 1);
			features[VAR_ROT_Y] = squareAccumulators[0][1] / (entryNum - 1);
			features[VAR_ROT_Z] = squareAccumulators[0][2] / (entryNum - 1);
			features[VAR_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[VAR_ROT_X], 2)
					+ Math.pow(features[VAR_ROT_Y], 2)
					+ Math.pow(features[VAR_ROT_Z], 2));
			features[MAD_ACC_X] = accumulators[0][0] / (entryNum - 1);
			features[MAD_ACC_Y] = accumulators[0][1] / (entryNum - 1);
			features[MAD_ACC_Z] = accumulators[0][2] / (entryNum - 1);
			features[MAD_ACC_ALL] = (float) Math.sqrt(Math.pow(
					features[MAD_ACC_X], 2)
					+ Math.pow(features[MAD_ACC_Y], 2)
					+ Math.pow(features[MAD_ACC_Z], 2));
			features[MAD_ROT_X] = accumulators[0][0] / (entryNum - 1);
			features[MAD_ROT_Y] = accumulators[0][1] / (entryNum - 1);
			features[MAD_ROT_Z] = accumulators[0][2] / (entryNum - 1);
			features[MAD_ROT_ALL] = (float) Math.sqrt(Math.pow(
					features[MAD_ROT_X], 2)
					+ Math.pow(features[MAD_ROT_Y], 2)
					+ Math.pow(features[MAD_ROT_Z], 2));
		}
	}

	private float[] getNearestValid(SparseArray<float[]> array, int key) {
		float[] entry = array.get(key);
		if (entry == null) {
			for (int j = key - 1; j >= 0; j--) {
				float[] cached = array.get(j);
				if (cached != null) {
					entry = cached;
					break;
				}
			}
		}
		return entry;
	}
}
