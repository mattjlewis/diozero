package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;

import com.diozero.api.easing.EasingFunction;
import com.diozero.api.easing.Linear;
import com.diozero.util.DioZeroScheduler;

/**
 * The Animation class constructs objects that represent a single Animation. An
 * Animation consists of a target and an array of segments. A target is the
 * device or list of devices that are being animated. A segment is a short
 * modular animation sequence (i.e. sit, stand, walk, etc). Segments are
 * synchronous and run first-in, first-out.
 */
public class Animation implements Runnable {
	private static final int DEFAULT_FPS = 60;

	private Collection<OutputDeviceInterface> targets;
	/**
	 * An easing function from ease-component to apply to the playback head on
	 * the timeline. See {@link com.diozero.api.easing.Easing Easing} docs for a
	 * list of available easing functions (default: "linear")
	 */
	private EasingFunction easing;
	/**
	 * When true, segment will loop until animation.next() or animation.stop()
	 * is called (default: false)
	 */
	private boolean loop;
	/**
	 * The cuePoint that the animation will loop back to. If the animation is
	 * playing in reverse, this is the point at which the animation will "loop
	 * back" to 1.0 (default: 0.0)
	 */
	private float loopback;
	/**
	 * Controls the speed of the playback head and scales the calculated
	 * duration of this and all subsequent segments until it is changed by
	 * another segment or a call to the speed() method (default: 1.0)
	 */
	private float speed;
	/**
	 * fps: The maximum frames per second for the segment (default: 60)
	 */
	private int fps;
	private int periodMs;
	/**
	 * function to execute when segment is started (default: none)
	 */
	private Action onStart;
	/**
	 * function to execute when segment is paused (default: none)
	 */
	private Action onPause;
	/**
	 * function to execute when animation is stopped (default: none)
	 */
	private Action onStop;
	/**
	 * function to execute when segment is completed (default: none)
	 */
	private Action onSegmentComplete;
	/**
	 * function to execute when segment loops (default: none)
	 */
	private Action onLoop;

	private Future<?> future;
	private int runSegment;
	private int runStep;
	private Collection<AnimationObject> animationList;

	private AnimationObject currentAnimationObject;

	public Animation(Collection<OutputDeviceInterface> targets, int fps, EasingFunction easing, float speed) {
		animationList = new ArrayList<>();
		this.targets = targets;
		if (fps <= 0) {
			fps = DEFAULT_FPS;
		}

		this.fps = fps;
		periodMs = 1000 / fps;

		// Default to Linear
		if (easing == null) {
			easing = Linear::ease;
		}
		this.easing = easing;

		this.speed = speed;
	}

	public int getFps() {
		return fps;
	}

	public boolean getLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	/**
	 * Play the animation. Animation's are set to play by default and this only
	 * needs to be called if the animation has been paused or a segment's speed
	 * property was set to 0.
	 * 
	 * @return Future instance for the background animation thread
	 */
	public Future<?> play() {
		runSegment = 0;
		runStep = 0;
		future = DioZeroScheduler.getNonDaemonInstance().scheduleAtFixedRate(this, 0, periodMs, TimeUnit.MILLISECONDS);
		if (onStart != null) {
			onStart.action();
		}
		return future;
	}

	/**
	 * Pause an animation while retaining the current progress and segment
	 * queue.
	 */
	public void pause() {
		// TODO
	}

	/**
	 * Immediately stop the animation and flush the segment queue.
	 */
	public void stop() {
		if (future != null) {
			future.cancel(true);
			future = null;

			if (onStop != null) {
				onStop.action();
			}
		}
	}

	/**
	 * Jump to the next segment in the queue. This is called automatically when
	 * a segment completes and in most cases should not be called by the user.
	 */
	public void next() {
		// TODO
	}

	/**
	 * Get the current speed
	 * 
	 * @return current speed factor
	 */
	public float getSpeed() {
		return speed;
	}
	
	public int getPeriodMs() {
		return periodMs;
	}
	
	public EasingFunction getEasingFunction() {
		return easing;
	}
	
	public Collection<OutputDeviceInterface> getTargets() {
		return targets;
	}
	
	/**
	 * Add a segment to the animation's queue.
	 * 
	 * @param durationMillis
	 *            Time in milliseconds for the entire animation
	 * @param cuePoints
	 *            List of relative time points at which to change to the next
	 *            segment
	 * @param keyFrames
	 *            List of segment values for target
	 */
	public void enqueue(int durationMillis, float[] cuePoints, List<AnimationObject.KeyFrame[]> keyFrames) {
		enqueue(new AnimationObject(durationMillis, cuePoints, keyFrames));
	}

	public void enqueue(AnimationObject animationObject) {
		animationObject.prepare(this);
		animationList.add(animationObject);
		// TODO Go through the list of animation objects
		currentAnimationObject = animationObject;
	}

	@Override
	public void run() {
		List<List<float[]>> segment_values = currentAnimationObject.getSegmentValues();
		float[] tgt_values = segment_values.get(runSegment).get(runStep);
		int index = 0;
		for (OutputDeviceInterface target : targets) {
			// TODO Handle delta as well as value
			target.setValue(tgt_values[index++]);
		}
		runStep++;
		if (runStep == segment_values.get(runSegment).size()) {
			runStep = 0;
			runSegment++;
			Logger.info("New segment {}, segmentValues.size() {}", Integer.valueOf(runSegment),
					Integer.valueOf(segment_values.size()));
			if (onSegmentComplete != null) {
				onSegmentComplete.action();
			}
			if (runSegment == segment_values.size()) {
				runSegment = 0;
				Logger.info("Finished");
				if (loop) {
					if (onLoop != null) {
						onLoop.action();
					}
				} else {
					future.cancel(false);
					future = null;
					if (onStop != null) {
						onStop.action();
					}
				}
			}
		}
	}
}
