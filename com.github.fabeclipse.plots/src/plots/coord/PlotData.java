package plots.coord;

public class PlotData {
	// domain assumed to be monotonically increasing
	double[] domain;
	double[] value;
	
	// range includes all elements i for which domainRangeStart <= domain[i] < domainRangeEnd
	int domainRangeStart;
	int domainRangeEnd;
	private double min;
	private double max;

	public PlotData(double[] domain, double[] value) {
		this.domain = domain;
		this.value = value;
		min = Double.MAX_VALUE;
		max = Double.MIN_VALUE;
		for (int i = 0; i < domain.length; i++) {
			double v = value[i];
			if (v > max) max = v;
			if (v < min) min = v;
		}
	}

	double valueFor(double domainPoint) {
		for (int i = 1; i < domain.length; i++) {
			double d = domain[i];
			if (d > domainPoint)
				return value[i-1];
		}
		return 0; // TODO
	}

	double valueForRange(double start, double end) {
		double val = 0;
		double span = 0;
		double ld = domain[0];
		for (int i = 1; i < domain.length; i++) {
			double d = domain[i];
			if (d < start) {
				// before the range, skip
			} else if (start > ld && start < d) {
				val += (value[i] + value[i-1]) / 2; // begins here
				span += 1;
			} else if (d < end) {
				val += value[i]; // completely inside the range
				span += 1;
			} else if (ld < end && end < d) {
				val += (value[i] + value[i-1]) / 2; // ends here
				span += 1;
			} else {
				break; // done
			}
			ld = d;
		}
		return val / (span > 0 ? span : 1);
	}

	double getMin() {
		return min;
	}

	double getMax() {
		return max;
	}
}
