package aurelienribon.tweenengine;

import aurelienribon.tweenengine.callbacks.CompleteCallback;
import aurelienribon.tweenengine.callbacks.IterationCompleteCallback;
import aurelienribon.tweenengine.callbacks.KillCallback;
import aurelienribon.tweenengine.callbacks.PoolCallback;
import aurelienribon.tweenengine.utils.Pool;
import java.util.ArrayList;

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
 * Tween.to(myObject, POSITION_XY, 500, Quad.INOUT)
 *      .target(200, 300).delay(1000).repeat(2).start();
 * </pre>
 *
 * You need to periodicaly update the tween engine, in order to compute the new
 * values. First way is to directly use the update() method of each tween.
 * However, take care that if you enabled pooling (with Tween.setPoolEnabled()),
 * you need to stop mess with a tween as soon as it is freed and returned to
 * the pool (you may add a PoolCallback to your tweens). <b>If you do want to
 * use tween pooling without having to take care of everything, use a
 * TweenManager !</b>.
 *
 * @see TweenManager
 * @see TweenGroup
 * @author Aurelien Ribon (aurelien.ribon@gmail.com)
 */
public class Tween {

	// -------------------------------------------------------------------------
	// Static
	// -------------------------------------------------------------------------

	/** If you need to repeat your tween for infinity, use this. */
	public static final int INFINITY = -1;
	/** The maximum number of attributes that can be tweened in a single tween. */
	public static final int MAX_COMBINED_TWEENS = 10;

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
	 * Used for debug purpose. Gets the current number of objects that are
	 * waiting in the pool.
	 * @return The current size of the pool.
	 */
	public static int getPoolSize() {
		return pool.size();
	}

	/**
	 * Clears every static resources and resets the static instance.
	 */
	public static void dispose() {
		isPoolEnabled = false;
		pool.clear();
	}

	// -------------------------------------------------------------------------

	private static boolean isPoolEnabled = false;
	private static final Pool<Tween> pool;

	static {
		pool = new Pool<Tween>(20) {
			@Override protected Tween getNew() {
				return new Tween(null, -1, 0, null);
			}
		};
	}

	private static Tween getNewTween() {
		if (isPoolEnabled) {
			Tween tween = pool.get();
			tween.__reset();
			tween.isPooled = true;
			return tween;
		}
		return new Tween(null, -1, 0, null);
	}

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
	 * The following lines are equivalent (if pooling has been disabled):
	 *
	 * <br/><br/>
	 * <pre>
	 * Tween.to(myObject, Types.POSITION, 1000, Quad.INOUT).target(50, 70);
	 * new Tween(myObject, Types.POSITION, 1000, Quad.INOUT).target(50, 70);
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience. However, you can control the creation of the
	 * tween by using the classic constructor.
	 * 
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 * @param equation The easing equation used during the interpolation.
	 * @return The generated Tween.
	 */
	public static Tween to(Tweenable target, int tweenType, int durationMillis, TweenEquation equation) {
		Tween tween = getNewTween();
		tween.__build(target, tweenType, durationMillis, equation);
		return tween;
	}

	/**
	 * Convenience method to create a new reversed interpolation.
	 *
	 * <br/><br/>
	 * You need to set the target values of the interpolation by using one
	 * of the ".target()" methods. The interpolation will run from these target
	 * values to the current values (retrieved after the delay, if any).
	 *
	 * <br/><br/>
	 * The following lines are equivalent (if pooling has been disabled):
	 *
	 * <br/><br/>
	 * <pre>
	 * Tween.from(myObject, Types.POSITION, 1000, Quad.INOUT).target(50, 70);
	 * new Tween(myObject, Types.POSITION, 1000, Quad.INOUT).target(50, 70).reverse();
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience. However, you can control the creation of the
	 * tween by using the classic constructor.
	 *
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 * @param equation The easing equation used during the interpolation.
	 * @return The generated Tween.
	 */
	public static Tween from(Tweenable target, int tweenType, int durationMillis, TweenEquation equation) {
		Tween tween = getNewTween();
		tween.__build(target, tweenType, durationMillis, equation);
		tween.reverse();
		return tween;
	}

	/**
	 * Convenience method to create a new instantaneous interpolation (as a
	 * result, this is not really an "interpolation").
	 *
	 * <br/><br/>
	 * You need to set the target values of the interpolation by using one
	 * of the ".target()" methods. The interpolation will directly apply these
	 * target values. Of course, a delay can be specified, like in every tween.
	 *
	 * <br/><br/>
	 * The following lines are equivalent (if pooling has been disabled):
	 *
	 * <br/><br/>
	 * <pre>
	 * Tween.set(myObject, Types.POSITION).target(50, 70);
	 * new Tween(myObject, Types.POSITION, 0, null).target(50, 70);
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience. However, you can control the creation of the
	 * tween by using the classic constructor.
	 *
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @return The generated Tween.
	 */
	public static Tween set(Tweenable target, int tweenType) {
		Tween tween = getNewTween();
		tween.__build(target, tweenType, 0, null);
		return tween;
	}

	/**
	 * Convenience method to create a new simple timer.
	 *
	 * <br/><br/>
	 * You need to set the target values of the interpolation by using one
	 * of the ".target()" methods. The interpolation will run from the current
	 * values (retrieved after the delay, if any) to these target values.
	 *
	 * <br/><br/>
	 * The following lines are equivalent (if pooling has been disabled):
	 *
	 * <br/><br/>
	 * <pre>
	 * Tween.call(myCallback).delay(1000);
	 * new Tween(null, -1, 0, null).addCallback(mycallback).delay(1000);
	 * </pre>
	 *
	 * Several options such as delays and callbacks can be added to the tween.
	 * This method hides some of the internal optimizations such as object
	 * reuse for convenience. However, you can control the creation of the
	 * tween by using the classic constructor.
	 *
	 * @param callback The callback that will be triggered at the end of the
	 * delay (if any specified).
	 * @return The generated Tween.
	 */
	public static Tween call(IterationCompleteCallback callback) {
		Tween tween = getNewTween();
		tween.__build(null, -1, 0, null);
		tween.addCallback(callback);
		return tween;
	}

	// -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	// Main
	private Tweenable target;
	private int tweenType;
	private TweenEquation equation;

	// General
	private boolean isReversed;
	private boolean isInitialized;
	private boolean isDirty;
	private boolean isPooled;

	// Values
	private int combinedTweenCount;
	private final float[] startValues;
	private final float[] targetValues;
	private final float[] targetMinusStartValues;

	// Timings
	private long startMillis;
	private int durationMillis;
	private int delayMillis;
	private long endDelayMillis;
	private long endMillis;
	private boolean isStarted;
	private boolean isDelayEnded;
	private boolean isEnded;
	private boolean isKilled;

	// Callbacks
	private final ArrayList<CompleteCallback> completeCallbacks;
	private final ArrayList<IterationCompleteCallback> iterationCompleteCallbacks;
	private final ArrayList<KillCallback> killCallbacks;
	private final ArrayList<PoolCallback> poolCallbacks;

	// Repeat
	private int repeatCnt;
	private int iteration;
	private int repeatDelayMillis;
	private long endRepeatDelayMillis;

	// Misc
	private final float[] localTmp = new float[MAX_COMBINED_TWEENS];

	// -------------------------------------------------------------------------
	// Ctor
	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new Tween from scratch.
	 * @param target The target of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param durationMillis The duration of the interpolation, in milliseconds.
	 * @param equation The easing equation used during the interpolation.
	 */
	public Tween(Tweenable target, int tweenType, int durationMillis, TweenEquation equation) {
		startValues = new float[MAX_COMBINED_TWEENS];
		targetValues = new float[MAX_COMBINED_TWEENS];
		targetMinusStartValues = new float[MAX_COMBINED_TWEENS];

		completeCallbacks = new ArrayList<CompleteCallback>(3);
		iterationCompleteCallbacks = new ArrayList<IterationCompleteCallback>(3);
		killCallbacks = new ArrayList<KillCallback>(3);
		poolCallbacks = new ArrayList<PoolCallback>(3);

		__reset();
		__build(target, tweenType, durationMillis, equation);
	}

	/**
	 * Starts or restart the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween start() {
		startMillis = System.currentTimeMillis();
		endDelayMillis = startMillis + delayMillis;

		if (iteration > 0 && repeatDelayMillis < 0)
			endDelayMillis = Math.max(endDelayMillis + repeatDelayMillis, startMillis);

		endMillis = endDelayMillis + durationMillis;
		endRepeatDelayMillis = Math.max(endMillis, endMillis + repeatDelayMillis);

		isStarted = true;
		isDelayEnded = false;
		isEnded = false;
		isKilled = false;

		return this;
	}

	/**
	 * Kills the interpolation. If pooling was enabled when this tween was
	 * created, the tween will be freed, cleared, and returned to the pool. As
	 * a result, you shouldn't use it anymore.
	 */
	public void kill() {
		isKilled = true;
		callKillCallbacks();
	}

	/**
	 * Adds a delay to the tween.
	 * @param millis The delay, in milliseconds.
	 * @return The current tween for chaining instructions.
	 */
	public Tween delay(int millis) {
		this.delayMillis += millis;
		return this;
	}

	/**
	 * Sets the target value of the interpolation. If not reversed, the
	 * interpolation will run from the current value to this target value.
	 * @param targetValue The target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float targetValue) {
		targetValues[0] = targetValue;
		return this;
	}

	/**
	 * Sets the target values of the interpolation. If not reversed, the
	 * interpolation will run from the current values to these target values.
	 * @param targetValue1 The 1st target value of the interpolation.
	 * @param targetValue2 The 2nd target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float targetValue1, float targetValue2) {
		targetValues[0] = targetValue1;
		targetValues[1] = targetValue2;
		return this;
	}

	/**
	 * Sets the target values of the interpolation. If not reversed, the
	 * interpolation will run from the current values to these target values.
	 * @param targetValue1 The 1st target value of the interpolation.
	 * @param targetValue2 The 2nd target value of the interpolation.
	 * @param targetValue3 The 3rd target value of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float targetValue1, float targetValue2, float targetValue3) {
		targetValues[0] = targetValue1;
		targetValues[1] = targetValue2;
		targetValues[2] = targetValue3;
		return this;
	}

	/**
	 * Sets the target values of the interpolation. If not reversed, the
	 * interpolation will run from the current values to these target values.
	 * <br/><br/>
	 * The other methods are convenience to avoid the allocation of an array.
	 * @param targetValues The target values of the interpolation.
	 * @return The current tween for chaining instructions.
	 */
	public Tween target(float... targetValues) {
		if (targetValues.length > MAX_COMBINED_TWEENS)
			throw new RuntimeException("You cannot set more than " + MAX_COMBINED_TWEENS + " targets.");
		System.arraycopy(targetValues, 0, this.targetValues, 0, targetValues.length);
		return this;
	}

	/**
	 * Adds a callback to the tween.
	 * @param callback A callback, see tweenengine.callbacks package for the
	 * available callbacks.
	 * @return The current tween for chaining instructions.
	 */
	public Tween addCallback(TweenCallback callback) {
		if (callback instanceof CompleteCallback)
			completeCallbacks.add((CompleteCallback) callback);
		else if (callback instanceof IterationCompleteCallback)
			iterationCompleteCallbacks.add((IterationCompleteCallback) callback);
		else if (callback instanceof KillCallback)
			killCallbacks.add((KillCallback) callback);
		return this;
	}

	/**
	 * Repeats the tween for a given number of times. 
	 * @param count The number of desired repetition. For infinite repetition,
	 * use Tween.INFINITY, or a negative number.
	 * @param millis A delay before each repetition.
	 * @return The current tween for chaining instructions.
	 */
	public Tween repeat(int count, int delayMillis) {
		repeatCnt = count;
		repeatDelayMillis = delayMillis;
		return this;
	}

	/**
	 * Reverse the tween. Will interpolate from target values to the
	 * current values if not already reversed.
	 * @return The current tween for chaining instructions.
	 */
	public Tween reverse() {
		isReversed = !isReversed;
		return this;
	}

	/**
	 * Gets the tween target.
	 * @return The tween target.
	 */
	public Tweenable getTarget() {
		return target;
	}

	/**
	 * Gets the tween type.
	 * @return The tween type.
	 */
	public int getTweenType() {
		return tweenType;
	}

	/**
	 * Gets the tween easing equation.
	 * @return The tween easing equation.
	 */
	public TweenEquation getEquation() {
		return equation;
	}

	/**
	 * Gets the tween target values.
	 * @return The tween target values.
	 */
	public float[] getTargetValues() {
		return targetValues;
	}

	/**
	 * Gets the tween duration.
	 * @return The tween duration.
	 */
	public int getDuration() {
		return durationMillis;
	}

	/**
	 * Gets the tween delay.
	 * @return The tween delay.
	 */
	public int getDelay() {
		return delayMillis;
	}

	/**
	 * Gets the number of combined tweens.
	 * @return The number of combined tweens.
	 */
	public int getCombinedTweenCount() {
		return combinedTweenCount;
	}

	/**
	 * Getsthe total number of repetitions.
	 * @return The total number of repetitions.
	 */
	public int getRepeatCount() {
		return repeatCnt;
	}

	/**
	 * Gets the delay before each repetition.
	 * @return The delay before each repetition.
	 */
	public int getRepeatDelay() {
		return repeatDelayMillis;
	}

	/**
	 * Gets the number of remaining iterations.
	 * @return The number of remaining iterations.
	 */
	public int getRemainingIterationCount() {
		return repeatCnt - iteration;
	}

	/**
	 * Returns true if the tween is dirty (i.e. if tween pooling is enabled and
	 * the tween has reached its end or has been killed). If this is the case,
	 * the tween should no longer been used, since it will be reset and freed.
	 * @return True if the tween should no longer be used.
	 */
	public boolean isDirty() {
		return isDirty;
	}

	// -------------------------------------------------------------------------
	// Update engine
	// -------------------------------------------------------------------------

	/**
	 * Updates the tween current state.
	 * @param currentMillis The current time, in milliseconds.
	 */
	public final void update(long currentMillis) {
		// Is the tween dirty ?
		checkForValidity();

		// Are we started ?
		if (isKilled || !isStarted)
			return;

		// Shall we repeat ?
		if (checkForRepetition(currentMillis))
			return;

		// Is the tween ended ?
		if (isEnded)
			return;

		// Wait for the end of the delay then either grab the start or end
		// values if it is the first iteration, or restart from those values
		// if the animation is replaying.
		if (checkForEndOfDelay(currentMillis))
			return;

		// Test for the end of the tween. If true, set the target values to
		// their final values (to avoid precision loss when moving fast), and
		// call the callbacks.
		if (checkForEndOfTween(currentMillis))
			return;

		// New values computation
		updateTarget(currentMillis);
	}

	private boolean checkForValidity() {
		if (isDirty && isPooled && isInitialized) {
			callPoolCallbacks();
			__reset();
			pool.free(this);
			return true;
		} else if (isDirty) {
			return true;
		}
		return false;
	}

	private boolean checkForRepetition(long currentMillis) {
		if (shouldRepeat() && currentMillis >= endRepeatDelayMillis) {
			iteration += 1;
			start();
			return true;
		}
		return false;
	}
	
	private boolean checkForEndOfDelay(long currentMillis) {
		if (!isDelayEnded && currentMillis >= endDelayMillis) {
			isDelayEnded = true;

			if (iteration > 0 && target != null) {
				target.onTweenUpdated(tweenType, startValues);
			} else if (target != null) {
				target.getTweenValues(tweenType, startValues);
				for (int i=0; i<combinedTweenCount; i++)
					targetMinusStartValues[i] = targetValues[i] - startValues[i];
			}

		} else if (!isDelayEnded) {
			return true;
		}
		return false;
	}

	private boolean checkForEndOfTween(long currentMillis) {
		if (!isEnded && currentMillis >= endMillis) {
			isEnded = true;

			if (target != null) {
				for (int i=0; i<combinedTweenCount; i++)
					localTmp[i] = startValues[i] + targetMinusStartValues[i];
				target.onTweenUpdated(tweenType, localTmp);
			}

			if (shouldRepeat()) {
				callIterationCompleteCallbacks();
			} else {
				callIterationCompleteCallbacks();
				callCompleteCallbacks();
			}

			return true;
		}
		return false;
	}

	private void updateTarget(long currentMillis) {
		if (target != null) {
			for (int i=0; i<combinedTweenCount; i++)
				localTmp[i] = equation.compute(
					currentMillis - endDelayMillis,
					isReversed ? targetValues[i] : startValues[i],
					isReversed ? -targetMinusStartValues[i] : +targetMinusStartValues[i],
					durationMillis);
			target.onTweenUpdated(tweenType, localTmp);
		}
	}

	// -------------------------------------------------------------------------
	// Hidden methods
	// -------------------------------------------------------------------------

	/**
	 * <b>Advanced use.</b>
	 * <br/>Resets every attribute of the tween. May be used if you want to
	 * build your own pool system. 
	 */
	public final void __reset() {
		this.target = null;
		this.tweenType = -1;
		this.equation = null;

		this.isReversed = false;
		this.isInitialized = false;
		this.isDirty = true;
		this.isPooled = false;

		this.combinedTweenCount = 0;

		this.delayMillis = 0;
		this.isStarted = false;
		this.isDelayEnded = false;
		this.isEnded = false;
		this.isKilled = false;

		this.completeCallbacks.clear();
		this.iterationCompleteCallbacks.clear();
		this.killCallbacks.clear();
		this.poolCallbacks.clear();

		this.repeatCnt = 0;
		this.iteration = 0;
		this.repeatDelayMillis = 0;
	}

	/**
	 * <b>Advanced use.</b>
	 * <br/>Rebuilds a tween from the current one. May be used if you want to
	 * build your own pool system. You should call __reset() before.
	 */
	public final void __build(Tweenable target, int tweenType, int durationMillis, TweenEquation equation) {
		this.isDirty = false;
		this.isInitialized = true;
		
		this.target = target;
		this.tweenType = tweenType;
		this.durationMillis = durationMillis;
		this.equation = equation;

		if (target != null) {
			this.combinedTweenCount = target.getTweenValues(tweenType, localTmp);
			if (this.combinedTweenCount < 1 || this.combinedTweenCount > MAX_COMBINED_TWEENS)
				throw new RuntimeException("Min combined tweens = 1, max = " + MAX_COMBINED_TWEENS);
		}
	}

	private boolean shouldRepeat() {
		return (repeatCnt < 0) || (iteration < repeatCnt);
	}

	private void callCompleteCallbacks() {
		if (isPooled)
			isDirty = true;

		for (int i=completeCallbacks.size()-1; i>=0; i--)
			completeCallbacks.get(i).onComplete(this);
	}

	private void callIterationCompleteCallbacks() {
		for (int i=iterationCompleteCallbacks.size()-1; i>=0; i--)
			iterationCompleteCallbacks.get(i).onIterationComplete(this);
	}

	private void callKillCallbacks() {
		if (isPooled)
			isDirty = true;
		
		for (int i=killCallbacks.size()-1; i>=0; i--)
			killCallbacks.get(i).onKill(this);
	}

	private void callPoolCallbacks() {
		for (int i=poolCallbacks.size()-1; i>=0; i--)
			poolCallbacks.get(i).onPool(this);
	}
}
