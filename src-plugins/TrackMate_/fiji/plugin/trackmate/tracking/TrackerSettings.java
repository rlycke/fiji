package fiji.plugin.trackmate.tracking;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Feature;

public class TrackerSettings {
	
	private static final double 	DEFAULT_LINKING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<Feature, Double> DEFAULT_LINKING_FEATURE_CUTOFFS = new HashMap<Feature, Double>();
	
	private static final boolean 	DEFAULT_ALLOW_GAP_CLOSING 				= true;
	private static final double 	DEFAULT_GAP_CLOSING_TIME_CUTOFF 		= 4;
	private static final double 	DEFAULT_GAP_CLOSING_DISTANCE_CUTOFF 	= 15.0;
	private static final HashMap<Feature, Double> DEFAULT_GAP_CLOSING_FEATURE_CUTOFFS = new HashMap<Feature, Double>();
	static {
		DEFAULT_GAP_CLOSING_FEATURE_CUTOFFS.put(Feature.MEAN_INTENSITY, 4d);
	}
	
	private static final boolean 	DEFAULT_ALLOW_MERGING 					= false;
	private static final double 	DEFAULT_MERGING_TIME_CUTOFF 			= 1;
	private static final double 	DEFAULT_MERGING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<Feature, Double> DEFAULT_MERGING_FEATURE_CUTOFFS = new HashMap<Feature, Double>();
	static {
		DEFAULT_MERGING_FEATURE_CUTOFFS.put(Feature.MEAN_INTENSITY, 4d);
	}

	private static final boolean 	DEFAULT_ALLOW_SPLITTING 				= false;
	private static final double 	DEFAULT_SPLITTING_TIME_CUTOFF 			= 1;
	private static final double 	DEFAULT_SPLITTING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<Feature, Double> DEFAULT_SPLITTING_FEATURE_CUTOFFS = new HashMap<Feature, Double>();
	static {
		DEFAULT_SPLITTING_FEATURE_CUTOFFS.put(Feature.MEAN_INTENSITY, 4d);
	}

	private static final double 	DEFAULT_ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;
	private static final double 	DEFAULT_CUTOFF_PERCENTILE 				= 0.9d;
	
	/** The tracker to use. */
	public TrackerType trackerType				= TrackerType.LAP_TRACKER;
	
	/** Max time difference over which particle linking is allowed.	 */
	public double linkingDistanceCutOff 		= DEFAULT_LINKING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for linking. */
	public Map<Feature, Double> linkingFeatureCutoffs = DEFAULT_LINKING_FEATURE_CUTOFFS; 
	
	/** Allow track segment gap closing? */
	public boolean allowGapClosing 				= DEFAULT_ALLOW_GAP_CLOSING;
	/** Max time difference over which segment gap closing is allowed.	 */
	public double gapClosingTimeCutoff 			= DEFAULT_GAP_CLOSING_TIME_CUTOFF;
	/** Max distance over which segment gap closing is allowed. */
	public double gapClosingDistanceCutoff 		= DEFAULT_GAP_CLOSING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for gap closing. */
	public Map<Feature, Double> gapClosingFeatureCutoffs = DEFAULT_GAP_CLOSING_FEATURE_CUTOFFS; 

	/** Allow track segment merging? */
	public boolean allowMerging 				= DEFAULT_ALLOW_MERGING;
	/** Max time difference over which segment gap closing is allowed.	 */
	public double mergingTimeCutoff 			= DEFAULT_MERGING_TIME_CUTOFF;
	/** Max distance over which segment gap closing is allowed. */
	public double mergingDistanceCutoff 		= DEFAULT_MERGING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for merging. */
	public Map<Feature, Double> mergingFeatureCutoffs = DEFAULT_MERGING_FEATURE_CUTOFFS; 

	/** Allow track segment splitting? */
	public boolean allowSplitting				= DEFAULT_ALLOW_SPLITTING;
	/** Max time difference over which segment splitting is allowed.	 */
	public double splittingTimeCutoff 			= DEFAULT_SPLITTING_TIME_CUTOFF;
	/** Max distance over which segment splitting is allowed. */
	public double splittingDistanceCutoff 		= DEFAULT_SPLITTING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for splitting. */
	public Map<Feature, Double> splittingFeatureCutoffs = DEFAULT_SPLITTING_FEATURE_CUTOFFS; 

	/** The factor used to create d and b in the paper, the alternative costs to linking objects. */
	public double alternativeObjectLinkingCostFactor = DEFAULT_ALTERNATIVE_OBJECT_LINKING_COST_FACTOR;
	/** The percentile used to calculate d and b cutoffs in the paper. */
	public double cutoffPercentile 				= DEFAULT_CUTOFF_PERCENTILE;
	
	/** Value used to block assignments when physically meaningless. */
	public double blockingValue 				= Double.MAX_VALUE;
	
	public String timeUnits 					= "frames";
	public String spaceUnits 					= "pixels";
	

	/*
	 * METHODS
	 */
	
	@Override
	public String toString() {
		String 	str = "Tracker: "+ trackerType.toString()+'\n';
		
		str += "  Linking costs:\n";
		str += String.format("    distance cutoff: %.1f", linkingDistanceCutOff);
		return str;
	}
	
	

	public enum TrackerType {
		LAP_TRACKER;
		
		@Override
		public String toString() {
			switch(this) {
			case LAP_TRACKER:
				return "LAP tracker";
			}
			return null;
		}

		public TrackerSettings createSettings() {
			switch(this) {
			case LAP_TRACKER:
				return new TrackerSettings();			
			}
			return null;
		}

		public String getInfoText() {
			switch(this) {
			case LAP_TRACKER:
				return "<html>" +
						"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
						"Its implementation is derived from the following paper: <br>" +
						"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
						"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
						" </html>";
			}
			
			return null;
		}
	}
	
}
