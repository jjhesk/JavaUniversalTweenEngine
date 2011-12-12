package aurelienribon.tweenengine;

import aurelienribon.tweenengine.TweenCallback.Types;
import aurelienribon.tweenengine.equations.Linear;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core class of the Tween Engine. It contains many static factory methods to
 * create and instantiate new interpolations.
 *
 * <br/><br/>
 * The common way to create a Tween is by using one of the static constructor,
 * like:
 *
 * <br/><br/>
 * -- Tween.to(...);<br/>
 * -- Tween.from(...);<br/>
 * -- Tween.set(...);<br/>
 * -- Tween.call(...);
 *
 * <br/><br/>
 * The following example will move the target horizontal position from its
 * current value to x=200 and y=300, during 500ms, but only after a delay of
 * 1000ms. The transition will also be repeated 2 times (the starting position
 * is registered at the end of the delay, so the animation will automatically
 * restart from this registered position).
 *
 * <br/><br/>
 * <pre>
 * Tween.to(myObject, POSITION_XY, 500)
 *      .target(200, 300)
 *      .ease(Quad.INOUT)
 *      .delay(1000)
 *      .repeat(2)
 *      .addToManager(myManager);
 * </pre>
 *
 * You need to periodicaly update the tween engine, in order to compute the new
 * values. Add it to a TweenManager, it will take care of the tween life-cycle
 * for you!
 *
 * @see TweenAccessor
 * @see TweenManager
 * @see TweenGroup
 * @see TweenEquation
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class Tween implements Groupable {

	// -------------------------------------------------------------------------
	// Static
	// -------------------------------------------------------------------------

	private static boolean isPoolEnabled = false;

	/** If you need to repeat your tween for infinity, use this. */
	public static final int INFINITY = -1;
	/** The maximum number of attributes that can be tweened in a single tween. */
	public static final int MAX_COMBINED_TWEENS = 10;

	/**
	 * Gets the version number of the library.
	 */
	public static String getVersion() {
		return "6.0.0";
	}

	/**
	 * Enables or disables the automatic reuse of ended tweens. Pooling prevents
	 * the allocation of a new tween object when using the static constructors,
	 * thus removing the need for garbage collection. Can be quite helpful on
	 * slow or embedded devices.
	 * <br/><br/>
	 * Defaults to false.
	 */
	public static void setPoolEnabled(boolean value) {
		isPoolEnabled = value;
	}

	/**
	 * Returns true if object pooling is enabled.
	 */
	public static boolean isPoolEnabled() {
		return isPoolEnabled;
	}

	/**
	 * Used for debug purpose. Gets the current number of objects that are
	 * waiting in the pool.
	 * @return The current size of the pool.
	 */
	public static int getPoolSize() {
		return pool.size();
	}

	/**
	 * Increases the pool capacity directly. Capacity defaults to 20.
	 * @param minCapacity The minimum capacity of the pool.
	 */
	public static void ensurePoolCapacity(int minCapacity) {
		pool.ensureCapacity(minCapacity);
	}

	/**
	 * Clears every static resources and resets the static instance.
	 */
	public static void dispose() {
		isPoolEnabled = false;
		pool.clear();
	}

	/**
	 * Registers an engine with the class of an object. This engine will be used
	 * with interpolations applied to every objects of the registered class.
	 * @param someClass An object class.
	 * @param defaultAccessor The accessor that will be used to tween any object
	 * of class "someClass".
	 */
	public static void registerAccessor(Class someClass, TweenAccessor defaultAccessor) {
		registeredAccessors.put(someClass, defaultAccessor);
	}

	/**
	 * Gets the registered TweenAccessor associated with the given object class.
	 * @param someClass An object class.
	 */
	public static TweenAccessor getRegisteredAccessor(Class someClass) {
		return registeredAccessors.get(someClass);
	}

	// -------------------------------------------------------------------------

	private static final Map<Class, TweenAccessor> registeredAccessors = new HashMap<Class, TweenAccessor>();
	private static final float[] buffer = new float[MAX_COMBINED_TWEENS];

	private static final Pool.Callback<Tween> poolCallback = new Pool.Callback<Tween>() {
		@Override public void onPool(Tween obj) {obj.reset();}
		@Override public void onUnpool(Tween obj) {obj.isPooled = Tween.isPoolEnabled();}
	};

	private static final Pool<Tween> pool = new Pool<Tween>(20, poolCallback) {
		@Override protected Tween create() {
			Tween tween = new Tween(null, -1, 0);
			tween.reset();
			return tween;
		}
	};

	// -------------------------------------------------------------------------
	// Factories
	// -------------------------------------------------------------------------

	/**
	 * Convenience method to create a new interpolation.
	 *
	 * <br/><br/>
	 * You need to set the target values of the interpolation by using one
	 * of the ".target()" methods. The interpolation will run from the current
	 * values (retrieved after the delay, if any) to these target values.
	 *
	 * <br/><br/>
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/>
	 * <pre>
	 * Tween.to(myObject, POSITION, 1000)
	 *      .target(50, 70)
	 *      .ease(Quad.INOUT)
	 *      .addToManager(myManager);
	 * </pre>
	 * 
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience.
	 *
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 * @return The generated Tween.
	 */
	public static Tween to(Object target, int tweenType, int durationMillis) {
		Tween tween = pool.get();
		tween.setup(target, tweenType, durationMillis);
		tween.ease(Linear.INOUT);
		return tween;
	}

	/**
	 * Convenience method to create a new reversed interpolation.
	 *
	 * <br/><br/>
	 * You need to set the target values of the interpolation by using one
	 * of the ".target()" methods. The interpolation will run from these
	 * values (retrieved after the delay, if any) to the current values.
	 *
	 * <br/><br/>
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/>
	 * <pre>
	 * Tween.from(myObject, POSITION, 1000)
	 *      .target(50, 70)
	 *      .ease(Quad.INOUT)
	 *      .addToManager(myManager);
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience.
	 *
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 * @return The generated Tween.
	 */
	public static Tween from(Object target, int tweenType, int durationMillis) {
		Tween tween = pool.get();
		tween.setup(target, tweenType, durationMillis);
		tween.ease(Linear.INOUT);
		tween.isReversed = true;
		return tween;
	}

	/**
	 * Convenience method to create a new instantaneous interpolation (as a
	 * result, this is not really an interpolation).
	 *
	 * <br/><br/>
	 * You need to set the target values of the interpolation by using one
	 * of the ".target()" methods. The interpolation will directly set the target
	 * to these values.
	 *
	 * <br/><br/>
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/>
	 * <pre>
	 * Tween.set(myObject, POSITION)
	 *      .target(50, 70)
	 *      .addToManager(myManager);
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience.
	 *
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 * @return The generated Tween.
	 */
	public static Tween set(Object target, int tweenType) {
		Tween tween = pool.get();
		tween.setup(target, tweenType, 0);
		return tween;
	}

	/**
	 * Convenience method to create a new simple timer.
	 *
	 * <br/><br/>
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/>
	 * <pre>
	 * Tween.call(myCallback)
	 *      .addToManager(myManager);
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience.
	 *
	 * @param callback The callback that will be triggered at the end of the
	 * delay (if specified). A repeat behavior can be set to the tween to
	 * trigger it more than once.
	 * @return The generated Tween.
	 * @see TweenCallback
	 */
	public static Tween call(TweenCallback callback) {
		Tween tween = pool.get();
		tween.setup(null, -1, 0);
		tween.addStartCallback(callback);
		return tween;
	}

	/**
	 * Convenience method to create an empty tween. Such object is only useful
	 * when placed inside animation sequences (see TweenGroup), in which it
	 * may act as a beacon, so you can set callbacks on it in order to trigger
	 * then at the moment you need.
	 * @see TweenGroup
	 */
	public static Tween mark() {
		Tween tween = pool.get();
		tween.setup(null, -1, 0);
		return tween;
	}

	// -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	// Main
	private Object target;
	private TweenAccessor accessor;
	private int type;
	private TweenEquation equation;

	// General
	private boolean isPooled;
	private boolean isReversed;
	private boolean isRelative;
	private int iteration;
	private int repeatCnt;
	private int combinedTweenCnt;

	// Values
	private final float[] startValues = new float[MAX_COMBINED_TWEENS];
	private final float[] targetValues = new float[MAX_COMBINED_TWEENS];
	private final float[] targetMinusStartValues = new float[MAX_COMBINED_TWEENS];

	// Timings
	private int delayMillis;
	private int durationMillis;
	private int repeatDelayMillis;
	private int currentMillis;
	private boolean isStarted; // true when the tween is started
	private boolean isInitialized; // true when starting values have been retrieved (after first delay)
	private boolean isFinished; // true when all repetitions are done or the tween has been killed

	// Callbacks
	private List<TweenCallback> beginCallbacks;
	private List<TweenCallback> startCallbacks;
	private List<TweenCallback> endCallbacks;
	private List<TweenCallback> completeCallbacks;
	private List<TweenCallback> backStartCallbacks;
	private List<TweenCallback> backEndCallbacks;
	private List<TweenCallback> backCompleteCallbacks;

	// UserData
	private Object userData;

	// -------------------------------------------------------------------------
	// Ctor
	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new Tween from scratch.
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 */
	public Tween(Object target, int tweenType, int durationMillis) {
		reset();
		setup(target, tweenType, durationMillis);
	}

	private void reset() {
		target = null;
		accessor = null;
		type = -1;
		equation = null;

		isReversed = isRelative = false;
		iteration = repeatCnt = combinedTweenCnt = 0;

		currentMillis = delayMillis = durationMillis = repeatDelayMillis = 0;
		isStarted = isInitialized = isFinished = false;

		if (beginCallbacks != null) beginCallbacks.clear();
		if (startCallbacks != null) startCallbacks.clear();
		if (endCallbacks != null) endCallbacks.clear();
		if (completeCallbacks != null) completeCallbacks.clear();
		if (backStartCallbacks != null) backStartCallbacks.clear();
		if (backEndCallbacks != null) backEndCallbacks.clear();
		if (backCompleteCallbacks != null) backCompleteCallbacks.clear();

		userData = null;
	}

	private void setup(Object target, int tweenType, int durationMillis) {
		this.target = target;
		this.type = tweenType;
		this.durationMillis = durationMillis;

		if (target != null) {
			if (!registeredAccessors.containsKey(target.getClass()) && !(target instanceof TweenAccessor)) {
				TweenAccessor parentAccessor = getParentAccessor(target.getClass());
				if (parentAccessor != null) {
					registerAccessor(target.getClass(), parentAccessor);
				} else {
					throw new RuntimeException("No TweenAccessor was found for the target class");
				}
			}

			accessor = registeredAccessors.get(target.getClass());
			if (accessor == null) accessor = (TweenAccessor) target;

			combinedTweenCnt = accessor.getValues(target, tweenType, buffer);
			if (combinedTweenCnt < 1 || combinedTweenCnt > MAX_COMBINED_TWEENS)
				throw new RuntimeException("Min combined tweens = 1, max = " + MAX_COMBINED_TWEENS);
		}
	}

	private TweenAccessor getParentAccessor(Class clazz) {
		Class parentClass = clazz.getSuperclass();
		while (parentClass != null && !registeredAccessors.containsKey(parentClass))
			parentClass = parentClass.getSuperclass();
		return parentClass != null ? registeredAccessors.get(parentClass) : null;
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Starts or restars the interpolation. Using this method can lead to some
	 * side-effects if you call it multiple times. <b>The recommanded behavior
	 * is to add the tween to a Tween Manager instead.</b>
	 * @return The current tween for chaining instructions.
	 */
	public Tween start() {
		currentMillis = 0;
		isStarted = true;
		return this;
	}

	/**
	 * Sets the target value of the interpolation. The interpolation will run
	 * from the <b>value at start time (after the delay, if any)</b> to this
	 * target value.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start value: value at start time, after delay<br/>
	 * - end value: param
	 * @param targetValue The target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float targetValue) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		targetValues[0] = targetValue;
		return this;
	}

	/**
	 * Sets the target values of the interpolation. The interpolation will run
	 * from the <b>values at start time (after the delay, if any)</b> to these
	 * target values.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params
	 * @param targetValue1 The 1st target value of the interpolation.
	 * @param targetValue2 The 2nd target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float targetValue1, float targetValue2) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		targetValues[0] = targetValue1;
		targetValues[1] = targetValue2;
		return this;
	}

	/**
	 * Sets the target values of the interpolation. The interpolation will run
	 * from the <b>values at start time (after the delay, if any)</b> to these
	 * target values.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params
	 * @param targetValue1 The 1st target value of the interpolation.
	 * @param targetValue2 The 2nd target value of the interpolation.
	 * @param targetValue3 The 3rd target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float targetValue1, float targetValue2, float targetValue3) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		targetValues[0] = targetValue1;
		targetValues[1] = targetValue2;
		targetValues[2] = targetValue3;
		return this;
	}

	/**
	 * Sets the target values of the interpolation. The interpolation will run
	 * from the <b>values at start time (after the delay, if any)</b> to these
	 * target values.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params
	 * @param targetValues The target values of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float... targetValues) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		if (targetValues.length > MAX_COMBINED_TWEENS)
			throw new RuntimeException("You cannot set more than " + MAX_COMBINED_TWEENS + " targets.");
		System.arraycopy(targetValues, 0, this.targetValues, 0, targetValues.length);
		return this;
	}

	/**
	 * Sets the target value of the interpolation, relatively to the <b>value
	 * at start time (after the delay, if any)</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start value: value at start time, after delay<br/>
	 * - end value: param + value at start time, after delay
	 * @param targetValue The relative target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetRelative(float targetValue) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		isRelative = true;
		targetValues[0] = targetValue;
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>values
	 * at start time (after the delay, if any)</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params + values at start time, after delay
	 * @param targetValue1 The 1st relative target value of the interpolation.
	 * @param targetValue2 The 2nd relative target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetRelative(float targetValue1, float targetValue2) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		isRelative = true;
		targetValues[0] = targetValue1;
		targetValues[1] = targetValue2;
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>values
	 * at start time (after the delay, if any)</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params + values at start time, after delay
	 * @param targetValue1 The 1st relative target value of the interpolation.
	 * @param targetValue2 The 2nd relative target value of the interpolation.
	 * @param targetValue3 The 3rd relative target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetRelative(float targetValue1, float targetValue2, float targetValue3) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		isRelative = true;
		targetValues[0] = targetValue1;
		targetValues[1] = targetValue2;
		targetValues[2] = targetValue3;
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>values
	 * at start time (after the delay, if any)</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params + values at start time, after delay
	 * @param targetValues The relative target values of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetRelative(float... targetValues) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		if (targetValues.length > MAX_COMBINED_TWEENS)
			throw new RuntimeException("You cannot set more than " + MAX_COMBINED_TWEENS + " targets.");
		System.arraycopy(targetValues, 0, this.targetValues, 0, targetValues.length);
		isRelative = true;
		return this;
	}

	/**
	 * Sets the target value(s) of the interpolation as <b>the current value(s),
	 * the one(s) present when this call is made</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start value: value at start time, after delay<br/>
	 * - end value: value at current time
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetCurrent() {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		accessor.getValues(target, type, targetValues);
		return this;
	}

	/**
	 * Sets the target value of the interpolation, relatively to the <b>the
	 * current value, the one present when this call is made</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start value: value at start time, after delay<br/>
	 * - end value: param + value at current time
	 * @param targetValue The relative target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetCurrentRelative(float targetValue) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		accessor.getValues(target, type, targetValues);
		targetValues[0] += targetValue;
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>the
	 * current values, the ones present when this call is made</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params + values at current time
	 * @param targetValue1 The 1st relative target value of the interpolation.
	 * @param targetValue2 The 2nd relative target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetCurrentRelative(float targetValue1, float targetValue2) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		accessor.getValues(target, type, targetValues);
		targetValues[0] += targetValue1;
		targetValues[1] += targetValue2;
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>the
	 * current values, the ones present when this call is made</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params + values at current time
	 * @param targetValue1 The 1st relative target value of the interpolation.
	 * @param targetValue2 The 2nd relative target value of the interpolation.
	 * @param targetValue3 The 3rd relative target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetCurrentRelative(float targetValue1, float targetValue2, float targetValue3) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		accessor.getValues(target, type, targetValues);
		targetValues[0] += targetValue1;
		targetValues[1] += targetValue2;
		targetValues[2] += targetValue3;
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>the
	 * current values, the ones present when this call is made</b>.
	 * <br/><br/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params + values at current time
	 * @param targetValues The relative target values of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween targetCurrentRelative(float... targetValues) {
		if (isStarted) throw new RuntimeException("Cannot change the targets of a running tween");
		if (targetValues.length > MAX_COMBINED_TWEENS)
			throw new RuntimeException("You cannot set more than " + MAX_COMBINED_TWEENS + " targets.");
		accessor.getValues(target, type, targetValues);
		for (int i=0, n=targetValues.length; i<n; i++)
			this.targetValues[i] += targetValues[i];
		return this;
	}

	/**
	 * Sets the easing equation of the tween. Existing equations are located in
	 * aurelienribon.tweenengine.equations, but you can of course implement
	 * your own, see TweenEquation.
	 * @return The current tween for chaining instructions.
	 * @see TweenEquation
	 */
	public Tween ease(TweenEquation easeEquation) {
		if (isStarted) throw new RuntimeException("Cannot change the easing of a running tween");
		this.equation = easeEquation;
		return this;
	}

	/**
	 * Kills the interpolation. If pooling was enabled when this tween was
	 * created, the tween will be freed, cleared, and returned to the pool. As
	 * a result, you shouldn't use it anymore.
	 */
	public void kill() {
		isFinished = true;
	}

	/**
	 * Adds a delay to the tween.
	 * @param millis The delay, in milliseconds.
	 * @return The current tween for chaining instructions.
	 */
	@Override
	public Tween delay(int millis) {
		if (isStarted) throw new RuntimeException("Cannot change the delay of a running tween");
		delayMillis += millis;
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the beginning
	 * of each iteration (before the delay), if the tween is running forwards
	 * (with a positive speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addBeginCallback(TweenCallback callback) {
		if (beginCallbacks == null) beginCallbacks = new ArrayList<TweenCallback>(1);
		beginCallbacks.add(callback);
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the start
	 * of each iteration (after the delay), if the tween is running forwards
	 * (with a positive speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addStartCallback(TweenCallback callback) {
		if (startCallbacks == null) startCallbacks = new ArrayList<TweenCallback>(1);
		startCallbacks.add(callback);
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the end
	 * of each iteration (after delay + duration), if the tween is running
	 * forwards (with a positive speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addEndCallback(TweenCallback callback) {
		if (endCallbacks == null) endCallbacks = new ArrayList<TweenCallback>(1);
		endCallbacks.add(callback);
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the completion
	 * of each iteration (after delay + duration + repeatDelay), if the tween is
	 * running forwards (with a positive speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addCompleteCallback(TweenCallback callback) {
		if (completeCallbacks == null) completeCallbacks = new ArrayList<TweenCallback>(1);
		completeCallbacks.add(callback);
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the start
	 * of each iteration (after the delay), if the tween is running backwards
	 * (with a negative speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addBackwardsStartCallback(TweenCallback callback) {
		if (backStartCallbacks == null) backStartCallbacks = new ArrayList<TweenCallback>(1);
		backStartCallbacks.add(callback);
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the end
	 * of each iteration (after delay + duration), if the tween is running
	 * backwards (with a negative speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addBackwardsEndCallback(TweenCallback callback) {
		if (backEndCallbacks == null) backEndCallbacks = new ArrayList<TweenCallback>(1);
		backEndCallbacks.add(callback);
		return this;
	}

	/**
	 * Adds a callback to the tween. The callback is triggered at the completion
	 * of each iteration (after delay + duration + repeatDelay), if the tween is
	 * running backwards (with a negative speed).
	 * @param callback A tween callback.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addBackwardsCompleteCallback(TweenCallback callback) {
		if (backCompleteCallbacks == null) backCompleteCallbacks = new ArrayList<TweenCallback>(1);
		backCompleteCallbacks.add(callback);
		return this;
	}

	/**
	 * Repeats the tween for a given number of times.
	 * @param count The number of desired repetition. For infinite repetition,
	 * use Tween.INFINITY, or a negative number.
	 * @param millis A delay before each repetition.
	 * @return The current tween for chaining instructions.
	 */
	@Override
	public Tween repeat(int count, int delayMillis) {
		repeatCnt = count;
		repeatDelayMillis = delayMillis >= 0 ? delayMillis : 0;
		return this;
	}

	/**
	 * Sets an object attached to this tween. It can be useful in order to
	 * retrieve some data from a TweenCallback.
	 * @param data Any kind of object.
	 * @return The current tween for chaining instructions.
	 */
	public Tween setUserData(Object data) {
		userData = data;
		return this;
	}

	/**
	 * Convenience method to add a single tween to a manager. The tween is
	 * automatically started. Both following usages are equivalent:<br/>
	 * <pre>
	 * myManager.add(Tween.to(...));
	 * Tween.to(...).addToManager(myManager);
	 * </pre>
	 * @param manager A TweenManager.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addToManager(TweenManager manager) {
		if (!manager.tweens.contains(this)) manager.tweens.add(this);
		start();
		return this;
	}

	/**
	 * Gets the tween target.
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * Gets the tween type.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Gets the tween easing equation.
	 */
	public TweenEquation getEquation() {
		return equation;
	}

	/**
	 * Gets the tween target values.
	 */
	public float[] getTargetValues() {
		return targetValues;
	}

	/**
	 * Gets the tween duration.
	 */
	@Override
	public int getDuration() {
		return durationMillis;
	}

	/**
	 * Gets the tween delay.
	 */
	@Override
	public int getDelay() {
		return delayMillis;
	}

	/**
	 * Gets the number of combined tweens.
	 */
	public int getCombinedTweenCount() {
		return combinedTweenCnt;
	}

	/**
	 * Gets the total number of repetitions.
	 */
	@Override
	public int getRepeatCount() {
		return repeatCnt;
	}

	/**
	 * Gets the delay before each repetition.
	 */
	@Override
	public int getRepeatDelay() {
		return repeatDelayMillis;
	}

	/**
	 * Returns true if the tween is finished (i.e. if the tween has reached
	 * its end or has been killed). If this is the case and tween pooling is
	 * enabled, the tween should no longer been used, since it will be reset
	 * and returned to the pool.
	 */
	public boolean isFinished() {
		return isFinished;
	}

	/**
	 * Gets the attached user data, or null if none.
	 */
	public Object getUserData() {
		return userData;
	}

	// -------------------------------------------------------------------------
	// Update engine
	// -------------------------------------------------------------------------

	/**
	 * <b>Advanced use.</b><br/>
	 * Updates the tween state. Using this method can be unsafe if tween
	 * pooling was first enabled. <b>The recommanded behavior is to use a
	 * TweenManager instead.</b> Of course, you can give any delta time you
	 * want. Therefore, slow or fast motion can be easily achieved.
	 * @param deltaMillis A delta time, in milliseconds, between now and the
	 * last call.
	 */
	public final void update(int deltaMillis) {
		if (isFinished && isPooled) pool.free(this);
		if (isFinished || !isStarted) return;

		currentMillis += deltaMillis;

		initialize();
		if (!isInitialized) return;

		int lastMillis = currentMillis - deltaMillis;
		int lastIteration = iteration;

		while (currentMillis > durationMillis + repeatDelayMillis) {
			currentMillis -= durationMillis + repeatDelayMillis;
			iteration += 1;
		}
		
		while (currentMillis < 0) {
			currentMillis += durationMillis + repeatDelayMillis;
			iteration -= 1;
		}

		testCallbacks(iteration, lastIteration, currentMillis, lastMillis);
		testIteration(iteration);
		updateTarget(currentMillis);
	}

	private void initialize() {
		if (!isInitialized && currentMillis >= delayMillis) {
			isInitialized = true;
			currentMillis -= delayMillis;

			if (target != null) {
				accessor.getValues(target, type, startValues);
				for (int i=0; i<combinedTweenCnt; i++) {
					targetValues[i] += isRelative ? startValues[i] : 0;
					targetMinusStartValues[i] = targetValues[i] - startValues[i];
				}
			}
		}
	}

	private void testCallbacks(int iteration, int lastIteration, int millis, int lastMillis) {
		if (iteration > lastIteration) {
			if (isValid(lastIteration) && lastMillis <= durationMillis) callCallbacks(Types.END);
			if (isValid(iteration)) callCallbacks(Types.START);

		} else if (iteration < lastIteration) {
			if (isValid(lastIteration)) callCallbacks(Types.BACK_END);
			if (isValid(iteration) && millis < durationMillis) callCallbacks(Types.BACK_START);

		} else {
			if (isValid(iteration) && millis > durationMillis && lastMillis <= durationMillis) callCallbacks(Types.END);
			if (isValid(iteration) && millis < durationMillis && lastMillis >= durationMillis) callCallbacks(Types.BACK_START);
		}
	}

	private void testIteration(int iteration) {
		if (iteration > repeatCnt && repeatCnt >= 0) {
			forceEndValues();
			callCallbacks(TweenCallback.Types.COMPLETE);
			kill();

		} else if (iteration < 0 && repeatCnt >= 0) {
			forceStartValues();
			callCallbacks(TweenCallback.Types.BACK_COMPLETE);
			kill();
		}
	}

	private void updateTarget(int itMillis) {
		assert itMillis >= 0;
		assert itMillis <= durationMillis + repeatDelayMillis;

		if (target == null || equation == null || !isInitialized || isFinished) return;

		if (itMillis >= durationMillis) {
			forceEndValues();
			return;
		}

		for (int i=0; i<combinedTweenCnt; i++) {
			buffer[i] = equation.compute(
				itMillis,
				isReversed ? targetValues[i] : startValues[i],
				isReversed ? -targetMinusStartValues[i] : +targetMinusStartValues[i],
				durationMillis);
		}

		accessor.setValues(target, type, buffer);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private void forceStartValues() {
		if (!isInitialized || target == null) return;
		accessor.setValues(target, type, isReversed ? targetValues : startValues);
	}

	private void forceEndValues() {
		if (!isInitialized || target == null) return;
		accessor.setValues(target, type, isReversed ? startValues : targetValues);
	}

	private boolean isValid(int iteration) {
		return (iteration >= 0 && iteration <= repeatCnt) || repeatCnt < 0;
	}

	private void callCallbacks(TweenCallback.Types type) {
		List<TweenCallback> callbacks = null;

		switch (type) {
			case BEGIN: callbacks = startCallbacks; break;
			case START: callbacks = startCallbacks; break;
			case END: callbacks = endCallbacks; break;
			case COMPLETE: callbacks = endCallbacks; break;
			case BACK_START: callbacks = backStartCallbacks; break;
			case BACK_END: callbacks = backEndCallbacks; break;
			case BACK_COMPLETE: callbacks = backEndCallbacks; break;
		}

		if (callbacks != null)
			for (int i=0, n=callbacks.size(); i<n; i++)
				callbacks.get(i).tweenEventOccured(type, this);
	}
}
