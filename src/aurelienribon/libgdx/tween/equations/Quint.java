package aurelienribon.libgdx.tween.equations;

import aurelienribon.libgdx.tween.TweenEquation;

public class Quint {
	public static final TweenEquation IN = new TweenEquation() {
		@Override
		public float compute(float t, float b, float c, float d) {
			return c*(t/=d)*t*t*t*t + b;
		}

		@Override
		public String toString() {
			return "Quint.IN";
		}
	};

	public static final TweenEquation OUT = new TweenEquation() {
		@Override
		public float compute(float t, float b, float c, float d) {
			return c*((t=t/d-1)*t*t*t*t + 1) + b;
		}

		@Override
		public String toString() {
			return "Quint.OUT";
		}
	};

	public static final TweenEquation INOUT = new TweenEquation() {
		@Override
		public float compute(float t, float b, float c, float d) {
			if ((t/=d/2) < 1) return c/2*t*t*t*t*t + b;
			return c/2*((t-=2)*t*t*t*t + 2) + b;
		}

		@Override
		public String toString() {
			return "Quint.INOUT";
		}
	};
}